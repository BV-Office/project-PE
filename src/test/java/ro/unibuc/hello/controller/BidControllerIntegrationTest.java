package ro.unibuc.hello.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.hello.dto.Bid;
import ro.unibuc.hello.dto.Item;
import ro.unibuc.hello.data.Category;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ro.unibuc.hello.service.BidService;
import ro.unibuc.hello.service.ItemService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class BidControllerIntegrationTest {

    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            .withExposedPorts(27017)
            .withSharding();

    @BeforeAll
    public static void setUp() {
        mongoDBContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        mongoDBContainer.stop();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        final String MONGO_URL = "mongodb://host.docker.internal:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));

        registry.add("mongodb.connection.url", () -> MONGO_URL + PORT);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BidService bidService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    private Item testItem1;
    private Item testItem2;
    private Bid testBid1;
    private Bid testBid2;

    @BeforeEach
    public void cleanUpAndAddTestData() {
        // Clear existing data
        List<Item> allItems = itemService.getAllItems();
        for (Item item : allItems) {
            itemService.deleteItem(item.getId());
        }

        List<Bid> allBids = bidService.getAllBids();
        for (Bid bid : allBids) {
            bidService.deleteBid(bid.getId());
        }

        // Create test items (needed for bids)
        Item item1 = new Item();
        item1.setName("Test Item 1");
        item1.setDescription("This is a test item 1");
        item1.setInitialPrice(100.0);
        item1.setEndTime(LocalDateTime.now().plusDays(7)); // Active for 7 days
        item1.setActive(true);
        item1.setCreator("seller1@example.com");
        item1.setCategory(Category.ELECTRONICS);

        Item item2 = new Item();
        item2.setName("Test Item 2");
        item2.setDescription("This is a test item 2");
        item2.setInitialPrice(200.0);
        item2.setEndTime(LocalDateTime.now().plusDays(5)); // Active for 5 days
        item2.setActive(true);
        item2.setCreator("seller2@example.com");
        item2.setCategory(Category.BOOKS);

        // Save test items
        testItem1 = itemService.createItem(item1);
        testItem2 = itemService.createItem(item2);

        // Create test bids
        Bid bid1 = new Bid();
        bid1.setItemId(testItem1.getId());
        bid1.setBidderName("Bidder One");
        bid1.setAmount(120.0);
        bid1.setEmail("bidder1@example.com");

        Bid bid2 = new Bid();
        bid2.setItemId(testItem2.getId());
        bid2.setBidderName("Bidder Two");
        bid2.setAmount(220.0);
        bid2.setEmail("bidder2@example.com");

        // Save test bids
        testBid1 = bidService.placeBid(bid1);
        testBid2 = bidService.placeBid(bid2);
    }

    @Test
    public void testGetAllBids() throws Exception {
        mockMvc.perform(get("/bids"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bidderName").exists())
                .andExpect(jsonPath("$[1].bidderName").exists());
    }

    @Test
    public void testGetBidById() throws Exception {
        mockMvc.perform(get("/bids/" + testBid1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testBid1.getId()))
                .andExpect(jsonPath("$.bidderName").value("Bidder One"))
                .andExpect(jsonPath("$.amount").value(120.0))
                .andExpect(jsonPath("$.email").value("bidder1@example.com"));
    }

    @Test
    public void testGetBidByIdNotFound() throws Exception {
        mockMvc.perform(get("/bids/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetBidsByItem() throws Exception {
        mockMvc.perform(get("/bids/item/" + testItem1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].itemId").value(testItem1.getId()))
                .andExpect(jsonPath("$[0].bidderName").value("Bidder One"));
    }

    @Test
    public void testGetBidsByBidder() throws Exception {
        mockMvc.perform(get("/bids/bidder/Bidder One"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bidderName").value("Bidder One"));
    }

    @Test
    public void testGetBidsByEmail() throws Exception {
        mockMvc.perform(get("/bids/bidder-email/bidder1@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("bidder1@example.com"));
    }

    @Test
    public void testPlaceBid() throws Exception {
        Bid newBid = new Bid();
        newBid.setItemId(testItem1.getId());
        newBid.setBidderName("Bidder Three");
        newBid.setAmount(150.0);
        newBid.setEmail("bidder3@example.com");

        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBid)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bidderName").value("Bidder Three"))
                .andExpect(jsonPath("$.amount").value(150.0))
                .andExpect(jsonPath("$.email").value("bidder3@example.com"));

        // Verify bid was added
        mockMvc.perform(get("/bids"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    public void testPlaceBidWithLowAmount() throws Exception {
        Bid lowBid = new Bid();
        lowBid.setItemId(testItem1.getId());
        lowBid.setBidderName("Low Bidder");
        lowBid.setAmount(110.0); // Lower than existing bid of 120.0
        lowBid.setEmail("lowbidder@example.com");

        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowBid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPlaceBidWithInvalidEmail() throws Exception {
        Bid invalidBid = new Bid();
        invalidBid.setItemId(testItem1.getId());
        invalidBid.setBidderName("Invalid Bidder");
        invalidBid.setAmount(200.0);
        invalidBid.setEmail("invalid-email"); // Invalid email format

        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPlaceBidWithNonExistentItem() throws Exception {
        Bid nonexistentItemBid = new Bid();
        nonexistentItemBid.setItemId("nonexistent-item-id");
        nonexistentItemBid.setBidderName("Nonexistent Item Bidder");
        nonexistentItemBid.setAmount(300.0);
        nonexistentItemBid.setEmail("nonexistent@example.com");

        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonexistentItemBid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPlaceBidFromSameBidderWithLowerAmount() throws Exception {
        // First place a valid bid
        Bid validBid = new Bid();
        validBid.setItemId(testItem2.getId());
        validBid.setBidderName("Same Bidder");
        validBid.setAmount(250.0);
        validBid.setEmail("samebidder@example.com");

        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBid)))
                .andExpect(status().isCreated());

        // Then try to place a lower bid from the same bidder
        Bid lowerBid = new Bid();
        lowerBid.setItemId(testItem2.getId());
        lowerBid.setBidderName("Same Bidder");
        lowerBid.setAmount(240.0); // Lower than previous bid of 250.0
        lowerBid.setEmail("samebidder@example.com");

        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowerBid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteBid() throws Exception {
        mockMvc.perform(delete("/bids/" + testBid1.getId()))
                .andExpect(status().isNoContent());

        // Verify bid was deleted
        mockMvc.perform(get("/bids"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(testBid2.getId()));
    }

    @Test
    public void testDeleteBidNotFound() throws Exception {
        mockMvc.perform(delete("/bids/nonexistent-bid-id"))
                .andExpect(status().isNotFound());
    }
}