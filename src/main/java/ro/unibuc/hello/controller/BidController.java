package ro.unibuc.hello.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.hello.dto.Bid;
import ro.unibuc.hello.exception.BidException;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.service.BidService;

import java.util.List;

@RestController
@RequestMapping("/bids")
public class BidController {

    @Autowired
    private BidService bidService;

    @Autowired
    private Counter httpRequestsTotal;

    @Autowired
    private Counter successfulOperations;

    @Autowired
    private Counter failedOperations;

    @Autowired
    private Timer operationLatency;

    @Autowired
    private Counter activeUsers;

    @GetMapping
    public ResponseEntity<List<Bid>> getAllBids() {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            List<Bid> bids = bidService.getAllBids();
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(bids, HttpStatus.OK);
        } catch (Exception e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bid> getBidById(@PathVariable String id) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            Bid bid = bidService.getBidById(id);
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(bid, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<Bid>> getBidsByItem(@PathVariable String itemId) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            List<Bid> bids = bidService.getBidsByItem(itemId);
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(bids, HttpStatus.OK);
        } catch (Exception e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            throw e;
        }
    }

    @GetMapping("/bidder/{bidderName}")
    public ResponseEntity<List<Bid>> getBidsByBidder(@PathVariable String bidderName) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            List<Bid> bids = bidService.getBidsByBidder(bidderName);
            successfulOperations.increment();
            activeUsers.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(bids, HttpStatus.OK);
        } catch (Exception e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody Bid bid) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            Bid placedBid = bidService.placeBid(bid);
            successfulOperations.increment();
            activeUsers.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(placedBid, HttpStatus.CREATED);
        } catch (BidException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (EntityNotFoundException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBid(@PathVariable String id) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            bidService.deleteBid(id);
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (EntityNotFoundException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/bidder-email/{email}")
    public ResponseEntity<List<Bid>> getBidsByEmail(@PathVariable String email) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            List<Bid> bids = bidService.getBidsByEmail(email);
            successfulOperations.increment();
            activeUsers.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(bids, HttpStatus.OK);
        } catch (Exception e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            throw e;
        }
    }
}