package ro.unibuc.hello.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.hello.dto.Item;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.service.ItemService;

import java.util.List;

@RestController
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private Counter httpRequestsTotal;

    @Autowired
    private Counter successfulOperations;

    @Autowired
    private Counter failedOperations;

    @Autowired
    private Timer operationLatency;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            List<Item> items;
            if (activeOnly) {
                items = itemService.getActiveItems();
            } else {
                items = itemService.getAllItems();
            }
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(items, HttpStatus.OK);
        } catch (Exception e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable String id) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            Item item = itemService.getItemById(id);
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(item, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Item> searchItemByName(@RequestParam String name) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            Item item = itemService.searchItemByName(name);
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(item, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            Item createdItem = itemService.createItem(item);
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable String id, @RequestBody Item item) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            Item updatedItem = itemService.updateItem(id, item);
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(updatedItem, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        httpRequestsTotal.increment();
        Timer.Sample sample = Timer.start();
        try {
            itemService.deleteItem(id);
            successfulOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (EntityNotFoundException e) {
            failedOperations.increment();
            sample.stop(operationLatency);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}