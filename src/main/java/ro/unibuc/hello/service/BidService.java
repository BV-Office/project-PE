package ro.unibuc.hello.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.unibuc.hello.data.BidEntity;
import ro.unibuc.hello.data.BidRepository;
import ro.unibuc.hello.data.ItemEntity;
import ro.unibuc.hello.data.ItemRepository;
import ro.unibuc.hello.dto.Bid;
import ro.unibuc.hello.exception.BidException;
import ro.unibuc.hello.exception.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private MetricsService metricsService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    public List<Bid> getAllBids() {
        metricsService.incrementApiCalls();
        List<BidEntity> bids = bidRepository.findAll();
        return bids.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<Bid> getBidsByItem(String itemId) {
        metricsService.incrementApiCalls();
        List<BidEntity> bids = bidRepository.findByItemId(itemId);
        return bids.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<Bid> getBidsByBidder(String bidderName) {
        metricsService.incrementApiCalls();
        List<BidEntity> bids = bidRepository.findByBidderName(bidderName);
        return bids.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Bid getBidById(String id) {
        metricsService.incrementApiCalls();
        BidEntity bid = bidRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
        return convertToDto(bid);
    }

    public Bid placeBid(Bid bidDto) {
        metricsService.incrementApiCalls();
        
        // Use timer to measure bid placement duration
        return metricsService.recordBidPlacementTime(() -> {
            try {
                // Validate item exists
                ItemEntity item = itemRepository.findById(bidDto.getItemId())
                        .orElseThrow(() -> {
                            metricsService.incrementFailedBids();
                            metricsService.recordFailureReason("item_not_found");
                            return BidException.itemNotFound();
                        });

                // Check if item is active
                if (!item.isActive()) {
                    metricsService.incrementFailedBids();
                    metricsService.recordFailureReason("item_not_active");
                    throw BidException.itemNotActive();
                }

                // Check if bidding has ended
                if (item.getEndTime().isBefore(LocalDateTime.now())) {
                    metricsService.incrementFailedBids();
                    metricsService.recordFailureReason("item_expired");
                    throw BidException.itemExpired();
                }

                // Validate email format
                if (!EMAIL_PATTERN.matcher(bidDto.getEmail()).matches()) {
                    metricsService.incrementFailedBids();
                    metricsService.recordFailureReason("invalid_email");
                    throw new IllegalArgumentException("Invalid email format");
                }

                // Check if bid amount is valid
                double minimumBid = item.getInitialPrice();
                List<BidEntity> existingBids = bidRepository.findByItemIdOrderByAmountDesc(item.getId());
                if (!existingBids.isEmpty()) {
                    minimumBid = existingBids.get(0).getAmount();
                }

                if (bidDto.getAmount() <= minimumBid) {
                    metricsService.incrementFailedBids();
                    metricsService.recordFailureReason("bid_too_low");
                    throw BidException.bidTooLow();
                }

                // Check if the bid is higher than the last one from the same user email
                List<BidEntity> userBids = bidRepository.findByItemIdAndEmailOrderByAmountDesc(item.getId(), bidDto.getEmail());
                if (!userBids.isEmpty()) {
                    double lastUserBidAmount = userBids.get(0).getAmount();
                    if (bidDto.getAmount() <= lastUserBidAmount) {
                        metricsService.incrementFailedBids();
                        metricsService.recordFailureReason("user_bid_not_higher");
                        throw new IllegalArgumentException("Bid amount must be higher than your last bid");
                    }
                }

                // Save the bid
                BidEntity bid = new BidEntity(
                        bidDto.getItemId(),
                        bidDto.getBidderName(),
                        bidDto.getAmount(),
                        bidDto.getEmail()
                );

                BidEntity savedBid = bidRepository.save(bid);
                
                // Record successful bid
                metricsService.incrementSuccessfulBids();
                
                return convertToDto(savedBid);
            } catch (Exception e) {
                // If not already counted as a failed bid
                if (!(e instanceof BidException) && !(e instanceof IllegalArgumentException)) {
                    metricsService.incrementFailedBids();
                    metricsService.recordFailureReason("unexpected_error");
                }
                throw e;
            }
        });
    }

    public void deleteBid(String id) {
        metricsService.incrementApiCalls();
        BidEntity bid = bidRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
        bidRepository.delete(bid);
    }

    private Bid convertToDto(BidEntity bidEntity) {
        Bid bidDto = new Bid(
                bidEntity.getId(),
                bidEntity.getItemId(),
                bidEntity.getBidderName(),
                bidEntity.getAmount(),
                bidEntity.getCreatedAt(),
                bidEntity.getEmail()
        );

        // Add item name if available
        itemRepository.findById(bidEntity.getItemId()).ifPresent(item -> {
            bidDto.setItemName(item.getName());
        });

        return bidDto;
    }

    public List<Bid> getBidsByEmail(String email) {
        metricsService.incrementApiCalls();
        // Validate email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        List<BidEntity> bids = bidRepository.findByEmail(email);
        return bids.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}