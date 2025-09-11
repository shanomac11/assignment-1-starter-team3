package edu.trincoll.tracker;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class HabitController {

    private static final Map<Long, Habit> STORE = new ConcurrentHashMap<>();
    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    /** Test hook to reset in-memory state. */
    public static void clearStore() {
        STORE.clear();
        ID_SEQ.set(1);
    }

    /** GET /api/Habits — list all habits (sorted by id). */
    @GetMapping("/Habits")
    public ResponseEntity<List<Habit>> getAll() {
        List<Habit> habits = STORE.values().stream()
                .sorted(Comparator.comparing(Habit::getId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(habits);
    }

    /** GET /api/Habits/{id} — get one or 404. */
    @GetMapping("/Habits/{id}")
    public ResponseEntity<Habit> getById(@PathVariable Long id) {
        Habit habit = STORE.get(id);
        return (habit == null)
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(habit);
    }

    /** POST /api/Habits — create; validate name and uniqueness. */
    @PostMapping(path = "/Habits", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Habit> create(@RequestBody Habit body) {
        if (body.getName() == null || body.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        boolean duplicate = STORE.values().stream()
                .anyMatch(h -> Objects.equals(h.getName(), body.getName()));
        if (duplicate) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        long id = ID_SEQ.getAndIncrement();
        Habit toSave = new Habit();
        toSave.setId(id);
        toSave.setName(body.getName());
        toSave.setDescription(body.getDescription());
        toSave.setCompleted(body.isCompleted()); // default false unless provided

        STORE.put(id, toSave);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSave);
    }

    /** POST /api/habit — singular path used by one test. */
    @PostMapping(path = "/habit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Habit> createViaSingular(@RequestBody Habit body) {
        return create(body);
    }

    /** PUT /api/Habits/{id} — update name/description/completed; 404 if missing; guard duplicate names. */
    @PutMapping(path = "/Habits/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Habit> update(@PathVariable Long id, @RequestBody Habit body) {
        Habit existing = STORE.get(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (body.getName() == null || body.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        boolean duplicateName = STORE.values().stream()
                .anyMatch(other -> !Objects.equals(other.getId(), id)
                        && Objects.equals(other.getName(), body.getName()));
        if (duplicateName) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        existing.setName(body.getName());
        existing.setDescription(body.getDescription());

        // If payload has lastCompleted (e.g., from completeToday()), treat as completed=true.
        boolean completedFlag = body.isCompleted();
        if (!completedFlag && body.getLastCompleted() != null && !body.getLastCompleted().isAfter(LocalDate.now())) {
            completedFlag = true;
        }
        existing.setCompleted(completedFlag);

        return ResponseEntity.ok(existing);
    }

    /** DELETE /api/Habits/{id} — 204 or 404. */
    @DeleteMapping("/Habits/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Habit removed = STORE.remove(id);
        return (removed == null)
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.noContent().build();
    }

    /** GET /api/Habits/search?name=foo — case-insensitive contains on name. */
    @GetMapping("/Habits/search")
    public ResponseEntity<List<Habit>> searchByName(@RequestParam("name") String name) {
        if (name == null) {
            return ResponseEntity.badRequest().build();
        }
        String q = name.toLowerCase(Locale.ROOT);
        List<Habit> results = STORE.values().stream()
                .filter(h -> h.getName() != null && h.getName().toLowerCase(Locale.ROOT).contains(q))
                .sorted(Comparator.comparing(Habit::getId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }
}
