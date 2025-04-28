package ro.unibuc.hello.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.unibuc.hello.data.BidEntity;
import ro.unibuc.hello.data.BidRepository;
import ro.unibuc.hello.data.ItemEntity;
import ro.unibuc.hello.data.ItemRepository;
import ro.unibuc.hello.dto.Item;
import ro.unibuc.hello.exception.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private MetricsService metricsService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    public List<Item> getAllItems() {
        metricsService.incrementApiCalls();
        List<ItemEntity> items = itemRepository.findAll();
        return items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<Item> getActiveItems() {
        metricsService.incrementApiCalls();
        List<ItemEntity> items = itemRepository.findByActive(true);
        // Update the active items metric
        metricsService.updateActiveAuctions(items.size());
        return items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Item getItemById(String id) {
        metricsService.incrementApiCalls();
        ItemEntity item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
        return convertToDto(item);
    }

    public Item searchItemByName(String name) {
        metricsService.incrementApiCalls();
        List<ItemEntity> items = itemRepository.findByNameContainingIgnoreCase(name);
        if (items.isEmpty()) {
            throw new EntityNotFoundException(name);
        }
        return convertToDto(items.get(0));
    }

    public Item createItem(Item itemDto) {
        metricsService.incrementApiCalls();
        
        return metricsService.recordItemCreationTime(() -> {
            // Validate email format
            if (!EMAIL_PATTERN.matcher(itemDto.getCreator()).matches()) {
                throw new IllegalArgumentException("Invalid email format for creator");
            }

            // Validate end time is in the future
            if (itemDto.getEndTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("End time must be in the future");
            }

            ItemEntity item = new ItemEntity(
                    itemDto.getName(),
                    itemDto.getDescription(),
                    itemDto.getInitialPrice(),
                    itemDto.getEndTime(),
                    itemDto.getCreator(),
                    itemDto.getCategory()
            );
            ItemEntity savedItem = itemRepository.save(item);
            
            // Update active items count
            metricsService.updateActiveAuctions(itemRepository.findByActive(true).size());
            
            return convertToDto(savedItem);
        });
    }

    public Item updateItem(String id, Item itemDto) {
        metricsService.incrementApiCalls();
        
        ItemEntity existingItem = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        // Validate email format
        if (!EMAIL_PATTERN.matcher(itemDto.getCreator()).matches()) {
            throw new IllegalArgumentException("Invalid email format for creator");
        }

        // Validate end time is in the future for active items
        if (itemDto.isActive() && itemDto.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("End time must be in the future for active items");
        }

        existingItem.setName(itemDto.getName());
        existingItem.setDescription(itemDto.getDescription());
        existingItem.setInitialPrice(itemDto.getInitialPrice());
        existingItem.setEndTime(itemDto.getEndTime());
        existingItem.setActive(itemDto.isActive());
        existingItem.setCreator(itemDto.getCreator());
        existingItem.setCategory(itemDto.getCategory());

        ItemEntity updatedItem = itemRepository.save(existingItem);
        
        // Update active items count if the active status changed
        metricsService.updateActiveAuctions(itemRepository.findByActive(true).size());
        
        return convertToDto(updatedItem);
    }

    public void deleteItem(String id) {
        metricsService.incrementApiCalls();
        
        ItemEntity item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
        itemRepository.delete(item);
        
        // Update active items count if the deleted item was active
        if (item.isActive()) {
            metricsService.updateActiveAuctions(itemRepository.findByActive(true).size());
        }
    }

    public void deactivateExpiredItems() {
        List<ItemEntity> activeItems = itemRepository.findByActive(true);
        LocalDateTime now = LocalDateTime.now();
        int deactivatedCount = 0;

        for (ItemEntity item : activeItems) {
            if (item.getEndTime().isBefore(now)) {
                item.setActive(false);
                itemRepository.save(item);
                deactivatedCount++;
            }
        }
        
        // If any items were deactivated, update the active items count
        if (deactivatedCount > 0) {
            metricsService.updateActiveAuctions(activeItems.size() - deactivatedCount);
        }
    }

    private Item convertToDto(ItemEntity itemEntity) {
        Item itemDto = new Item(
                itemEntity.getId(),
                itemEntity.getName(),
                itemEntity.getDescription(),
                itemEntity.getInitialPrice(),
                itemEntity.getEndTime(),
                itemEntity.isActive(),
                itemEntity.getCreator(),
                itemEntity.getCategory()
        );

        // Add the highest bid information if available
        List<BidEntity> bids = bidRepository.findByItemIdOrderByAmountDesc(itemEntity.getId());
        if (!bids.isEmpty()) {
            BidEntity highestBid = bids.get(0);
            itemDto.setHighestBid(highestBid.getAmount());
            itemDto.setHighestBidder(highestBid.getBidderName());
        } else {
            itemDto.setHighestBid(itemEntity.getInitialPrice());
        }

        return itemDto;
    }
}