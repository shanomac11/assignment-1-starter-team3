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
 * - AI Tool Used: ChatGPT
 * - Most Helpful Prompt: Do it now please
 * - AI Mistake We Fixed: no mistakes
 * - Time Saved: an hour
 * - Team Members: Shane Ethan Quinn Ralston
 */
@RestController
@RequestMapping(value = "/api/habit", produces = MediaType.APPLICATION_JSON_VALUE) // TODO: Rename to match your domain (e.g., /api/bookmarks, /api/recipes)
public class HabitController {

    // Simple in-memory store (will be replaced by a database later)
    private static final Map<Long, Habit> STORE = new ConcurrentHashMap<>();
    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    /**
     * GET /api/habit
     * Returns all habits in the system
     */
    @GetMapping
    public ResponseEntity<List<Habit>> getAll() {
        List<Habit> habits = STORE.values()
                .stream()
                .sorted(Comparator.comparing(Habit::getId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(habits);
    }

    /**
     * GET /api/habits/{id}
     * Returns a specific habit by ID
     * Return 404 if habit doesn't exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<Habit> getById(@PathVariable Long id) {
        Habit habit = STORE.get(id);
        if (habit == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(habit);
    }

    /**
     * POST /api/habits
     * Creates a new habit
     * - Validate required fields (name)
     * - Reject duplicates by name (409 Conflict)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Habit> create(@RequestBody Habit habit) {
        // Validate name
        if (habit.getName() == null || habit.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // Enforce uniqueness by name
        boolean duplicate = STORE.values().stream()
                .anyMatch(existing -> Objects.equals(existing.getName(), habit.getName()));
        if (duplicate) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Assign new ID (ignore any provided id)
        long id = ID_SEQ.getAndIncrement();
        Habit toSave = new Habit();
        toSave.setId(id);
        toSave.setName(habit.getName());
        toSave.setDescription(habit.getDescription());
        toSave.setCompleted(habit.isCompleted());
        // Keep server-controlled createdAt from constructor; do not override from client

        STORE.put(id, toSave);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSave);
    }

    /**
     * PUT /api/habits/{id}
     * Updates an existing habit
     * - Validate required fields (name)
     * - Return 404 if habit doesn't exist
     * - Reject duplicates by name (409 Conflict) if changing to an existing name
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Habit> update(@PathVariable Long id, @RequestBody Habit update) {
        Habit existing = STORE.get(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (update.getName() == null || update.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // Prevent changing to a name that duplicates another habit's name
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
     * DELETE /api/habits/{id}
     * Deletes a habit
     * - Return 204 No Content on successful delete
     * - Return 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Habit removed = STORE.remove(id);
        if (removed == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/habits/search?name=value
     * Searches habits by name (case-insensitive contains)
     * BONUS endpoint
     */
    @GetMapping("/search")
    public ResponseEntity<List<Habit>> searchByName(@RequestParam("name") String name) {
        if (name == null) {
            return ResponseEntity.badRequest().build();
        }
        String query = name.toLowerCase(Locale.ROOT);
        List<Habit> results = STORE.values().stream()
                .filter(it -> it.getName() != null && it.getName().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Habit::getId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    // Test helper method - only for testing purposes
    static void clearStore() {
        STORE.clear();
        ID_SEQ.set(1);
    }
}