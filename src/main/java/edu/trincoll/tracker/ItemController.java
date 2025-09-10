package edu.trincoll.tracker;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AI Collaboration Report:
 * - AI Tool Used: [Your AI tool here - ChatGPT/Claude/Copilot/Gemini]
 * - Most Helpful Prompt: [Paste the prompt that worked best]
 * - AI Mistake We Fixed: [Describe what the AI got wrong and how you fixed it]
 * - Time Saved: [Estimate hours saved using AI]
 * - Team Members: [List your team members' names]
 */
@RestController
@RequestMapping(value = "/api/habit", produces = MediaType.APPLICATION_JSON_VALUE) // TODO: Rename to match your domain (e.g., /api/bookmarks, /api/recipes)
public class ItemController {

    // Simple in-memory store (will be replaced by a database later)
    private static final Map<Long, Item> STORE = new ConcurrentHashMap<>();
    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    /**
     * GET /api/habit
     * Returns all items in the system
     */
    @GetMapping
    public ResponseEntity<List<Item>> getAll() {
        List<Item> items = STORE.values()
                .stream()
                .sorted(Comparator.comparing(Item::getId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    /**
     * GET /api/items/{id}
     * Returns a specific item by ID
     * Return 404 if item doesn't exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(@PathVariable Long id) {
        Item item = STORE.get(id);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(item);
    }

    /**
     * POST /api/items
     * Creates a new item
     * - Validate required fields (name)
     * - Reject duplicates by name (409 Conflict)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Item> create(@RequestBody Item item) {
        // Validate name
        if (item.getName() == null || item.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // Enforce uniqueness by name
        boolean duplicate = STORE.values().stream()
                .anyMatch(existing -> Objects.equals(existing.getName(), item.getName()));
        if (duplicate) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Assign new ID (ignore any provided id)
        long id = ID_SEQ.getAndIncrement();
        Item toSave = new Item();
        toSave.setId(id);
        toSave.setName(item.getName());
        toSave.setDescription(item.getDescription());
        toSave.setCompleted(item.isCompleted());
        // Keep server-controlled createdAt from constructor; do not override from client

        STORE.put(id, toSave);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSave);
    }

    /**
     * PUT /api/items/{id}
     * Updates an existing item
     * - Validate required fields (name)
     * - Return 404 if item doesn't exist
     * - Reject duplicates by name (409 Conflict) if changing to an existing name
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Item> update(@PathVariable Long id, @RequestBody Item update) {
        Item existing = STORE.get(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (update.getName() == null || update.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // Prevent changing to a name that duplicates another item's name
        boolean duplicateName = STORE.values().stream()
                .anyMatch(other -> !Objects.equals(other.getId(), id)
                        && Objects.equals(other.getName(), update.getName()));
        if (duplicateName) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        existing.setName(update.getName());
        existing.setDescription(update.getDescription());
        existing.setCompleted(update.isCompleted());
        // Keep original createdAt (ignore client-sent value)

        return ResponseEntity.ok(existing);
    }

    /**
     * DELETE /api/items/{id}
     * Deletes an item
     * - Return 204 No Content on successful delete
     * - Return 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Item removed = STORE.remove(id);
        if (removed == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/items/search?name=value
     * Searches items by name (case-insensitive contains)
     * BONUS endpoint
     */
    @GetMapping("/search")
    public ResponseEntity<List<Item>> searchByName(@RequestParam("name") String name) {
        if (name == null) {
            return ResponseEntity.badRequest().build();
        }
        String query = name.toLowerCase(Locale.ROOT);
        List<Item> results = STORE.values().stream()
                .filter(it -> it.getName() != null && it.getName().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Item::getId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    // Test helper method - only for testing purposes
    static void clearStore() {
        STORE.clear();
        ID_SEQ.set(1);
    }
}