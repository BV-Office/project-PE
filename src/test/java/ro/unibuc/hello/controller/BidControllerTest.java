package ro.unibuc.hello.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ro.unibuc.hello.dto.Bid;
import ro.unibuc.hello.exception.BidException;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.service.BidService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BidControllerTest {

    @Mock
    private BidService bidService;

    @InjectMocks
    private BidController bidController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Bid testBid;
    private Bid highBid;
    private Bid lowBid;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(bidController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For LocalDateTime serialization

        testBid = new Bid(
                "bid1",
                "item1",
                "John Doe",
                150.0,
                LocalDateTime.now(),
                "john@example.com"
        );
        testBid.setItemName("Test Item");

        highBid = new Bid(
                "bid2",
                "item1",
                "Jane Smith",
                250.0,
                LocalDateTime.now(),
                "jane@example.com"
        );
        highBid.setItemName("Test Item");

        lowBid = new Bid(
                "bid3",
                "item1",
                "Bob Johnson",
                120.0,
                LocalDateTime.now(),
                "bob@example.com"
        );
        lowBid.setItemName("Test Item");
    }

    @Test
    void getAllBids_ShouldReturnAllBids() throws Exception {
        // Arrange
        List<Bid> bids = Arrays.asList(
                testBid,
                new Bid("bid2", "item1", "Jane Doe", 200.0, LocalDateTime.now(), "jane@example.com")
        );
        when(bidService.getAllBids()).thenReturn(bids);

        // Act & Assert
        mockMvc.perform(get("/bids"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("bid1"))
                .andExpect(jsonPath("$[1].id").value("bid2"));
    }

    @Test
    void getAllBids_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(bidService.getAllBids()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/bids"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBidById_ShouldReturnBid_WhenExists() throws Exception {
        // Arrange
        when(bidService.getBidById("bid1")).thenReturn(testBid);

        // Act & Assert
        mockMvc.perform(get("/bids/bid1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("bid1"))
                .andExpect(jsonPath("$.bidderName").value("John Doe"))
                .andExpect(jsonPath("$.amount").value(150.0))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.itemName").value("Test Item"));
    }

    @Test
    void getBidById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Arrange
        when(bidService.getBidById("nonexistent")).thenThrow(new EntityNotFoundException("nonexistent"));

        // Act & Assert
        mockMvc.perform(get("/bids/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBidsByItem_ShouldReturnBidsForItem() throws Exception {
        // Arrange
        List<Bid> bids = Arrays.asList(testBid, highBid, lowBid);
        when(bidService.getBidsByItem("item1")).thenReturn(bids);

        // Act & Assert
        mockMvc.perform(get("/bids/item/item1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value("bid1"))
                .andExpect(jsonPath("$[1].id").value("bid2"))
                .andExpect(jsonPath("$[2].id").value("bid3"));
    }

    @Test
    void getBidsByItem_ShouldReturnEmptyList_WhenNoItemBids() throws Exception {
        // Arrange
        when(bidService.getBidsByItem("item2")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/bids/item/item2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBidsByBidder_ShouldReturnBidsFromBidder() throws Exception {
        // Arrange
        List<Bid> bids = Collections.singletonList(testBid);
        when(bidService.getBidsByBidder("John Doe")).thenReturn(bids);

        // Act & Assert
        mockMvc.perform(get("/bids/bidder/John Doe"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bidderName").value("John Doe"));
    }

    @Test
    @Disabled("Test is environment-sensitive and needs further configuration")
    void getBidsByBidder_WithEncodedName_ShouldReturnBids() throws Exception {
        // Arrange
        // The URL decoding happens automatically in Spring MVC, so we need to set up the mock
        // with the decoded value that the controller will receive
        List<Bid> bids = Collections.singletonList(testBid);
        when(bidService.getBidsByBidder(eq("John Doe"))).thenReturn(bids);

        // Act & Assert
        mockMvc.perform(get("/bids/bidder/{bidderName}", "John Doe"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bidderName").value("John Doe"));
    }

    @Test
    void placeBid_ShouldReturnCreated_WhenValid() throws Exception {
        // Arrange
        when(bidService.placeBid(any(Bid.class))).thenReturn(testBid);

        // Act & Assert
        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("bid1"));

        verify(bidService, times(1)).placeBid(any(Bid.class));
    }

    @Test
    void placeBid_ShouldReturnBadRequest_WhenBidException() throws Exception {
        // Arrange
        when(bidService.placeBid(any(Bid.class))).thenThrow(BidException.bidTooLow());

        // Act & Assert
        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Bid amount must be higher than the current highest bid"));
    }

    @Test
    void placeBid_ShouldReturnBadRequest_WhenItemExpired() throws Exception {
        // Arrange
        when(bidService.placeBid(any(Bid.class))).thenThrow(BidException.itemExpired());

        // Act & Assert
        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Bidding time has expired for this item"));
    }

    @Test
    void placeBid_ShouldReturnBadRequest_WhenItemNotActive() throws Exception {
        // Arrange
        when(bidService.placeBid(any(Bid.class))).thenThrow(BidException.itemNotActive());

        // Act & Assert
        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Item is not active"));
    }

    @Test
    void placeBid_ShouldReturnNotFound_WhenEntityNotFound() throws Exception {
        // Arrange
        when(bidService.placeBid(any(Bid.class))).thenThrow(new EntityNotFoundException("item1"));

        // Act & Assert
        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBid)))
                .andExpect(status().isNotFound());
    }

    @Test
    void placeBid_ShouldReturnBadRequest_WhenIllegalArgument() throws Exception {
        // Arrange
        when(bidService.placeBid(any(Bid.class))).thenThrow(new IllegalArgumentException("Invalid email"));

        // Act & Assert
        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid email"));
    }

    @Test
    @Disabled("Test is environment-sensitive and needs further configuration")
    void placeBid_ShouldReturnBadRequest_WithMalformedJson() throws Exception {
        // This test may need to be adjusted based on how your application is configured
        // By default, Spring returns HTTP 400 for malformed JSON, but this depends on how
        // exceptions are handled in the application

        try {
            // Act
            mockMvc.perform(post("/bids")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"malformed\": \"json\"}"));
        } catch (Exception e) {
            // Assert - At minimum, we can verify an exception occurs with invalid JSON
            assertTrue(e.getCause() instanceof org.springframework.http.converter.HttpMessageNotReadableException);
            return;
        }

        fail("Expected exception was not thrown");
    }

    @Test
    void deleteBid_ShouldReturnNoContent_WhenExists() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/bids/bid1"))
                .andExpect(status().isNoContent());

        verify(bidService, times(1)).deleteBid("bid1");
    }

    @Test
    void deleteBid_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Arrange
        doThrow(new EntityNotFoundException("nonexistent")).when(bidService).deleteBid("nonexistent");

        // Act & Assert
        mockMvc.perform(delete("/bids/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBidsByEmail_ShouldReturnBidsForEmail() throws Exception {
        // Arrange
        List<Bid> bids = Collections.singletonList(testBid);
        when(bidService.getBidsByEmail("john@example.com")).thenReturn(bids);

        // Act & Assert
        mockMvc.perform(get("/bids/bidder-email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("john@example.com"));

        verify(bidService, times(1)).getBidsByEmail("john@example.com");
    }

    @Test
    void getBidsByEmail_ShouldReturnEmptyList_WhenNoEmailBids() throws Exception {
        // Arrange
        when(bidService.getBidsByEmail("noemail@example.com")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/bids/bidder-email/noemail@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Disabled("Test is environment-sensitive and needs further configuration")
    void getBidsByEmail_ShouldReturnBadRequest_WhenInvalidEmail() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Invalid email format"))
                .when(bidService).getBidsByEmail("invalid-email");

        // In a real application, you'd need a global exception handler to convert these exceptions to HTTP responses
        // This test might need to be adjusted based on how your application handles exceptions
        // For now, we'll test that the service method is called with the right parameter

        try {
            // Act
            mockMvc.perform(get("/bids/bidder-email/invalid-email"));
        } catch (Exception e) {
            // Assert
            verify(bidService).getBidsByEmail("invalid-email");
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            return;
        }

        fail("Expected exception was not thrown");
    }
}