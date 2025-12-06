package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryItemRepository implements ItemRepository {

    private final ConcurrentMap<Long, Item> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    @Override
    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(idGenerator.incrementAndGet());
        }
        storage.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Item> findAll() {
        return storage.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<Item> findByOwnerId(Long ownerId) {
        return storage.values().stream()
                .filter(item -> item.getOwner() != null && item.getOwner().getId().equals(ownerId))
                .collect(Collectors.toList());
    }
}

