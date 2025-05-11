package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    private static ExecutorService executor = Executors.newFixedThreadPool(10);

    private List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());

    private AtomicInteger processedCount = new AtomicInteger(0);

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Long id : itemIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100);
                    Item item = itemRepository.findById(id).orElse(null);
                    if (item != null) {
                        processedCount.incrementAndGet();
                        item.setStatus("PROCESSED");
                        itemRepository.save(item);
                        processedItems.add(item);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, executor);
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processedItems);
    }
}
