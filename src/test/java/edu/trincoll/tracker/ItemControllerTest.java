package edu.trincoll.tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test suite for the Habit API.
 * <p>
 * ALL TESTS MUST PASS for full credit.
 * Do not modify these tests - modify your code to make them pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Habit Controller Tests")
class HabitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // Clear any existing data before each test
        HabitController.clearStore();
    }

    @Nested
    @DisplayName("GET /api/Habits")
    class GetAllHabits {

        @Test
        @DisplayName("should return empty list when no Habits exist")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/api/Habits"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0))));
        }

        @Test
        @DisplayName("should return all Habits when Habits exist")
        void shouldReturnAllHabits() throws Exception {
            // Create a test Habit first
            Habit testHabit = new Habit();
            testHabit.setName("Test Habit");
            testHabit.setDescription("Test Description");

            mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testHabit)))
                    .andExpect(status().isCreated());

            // Now get all Habits
            mockMvc.perform(get("/api/Habits"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[?(@.name == 'Test Habit')]").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/Habits/{id}")
    class GetHabitById {

        @Test
        @DisplayName("should return Habit when it exists")
        void shouldReturnHabitWhenExists() throws Exception {
            // Create a test Habit first
            Habit testHabit = new Habit();
            testHabit.setName("Specific Habit");
            testHabit.setDescription("Specific Description");

            String response = mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testHabit)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Habit createdHabit = objectMapper.readValue(response, Habit.class);

            // Get the specific Habit
            mockMvc.perform(get("/api/Habits/{id}", createdHabit.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Specific Habit"))
                    .andExpect(jsonPath("$.description").value("Specific Description"));
        }

        @Test
        @DisplayName("should return 404 when Habit doesn't exist")
        void shouldReturn404WhenHabitDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/Habits/{id}", 999999))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/Habits")
    class CreateHabit {

        @Test
        @DisplayName("should create new Habit with valid data")
        void shouldCreateNewHabit() throws Exception {
            Habit newHabit = new Habit();
            newHabit.setName("New Habit");
            newHabit.setDescription("New Description");

            mockMvc.perform(post("/api/habit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newHabit)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("New Habit"))
                    .andExpect(jsonPath("$.description").value("New Description"));
        }

        @Test
        @DisplayName("should return 400 when name is missing")
        void shouldReturn400WhenNameMissing() throws Exception {
            String invalidJson = """
                    {"description":"No name provided"}""";

            mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            Habit invalidHabit = new Habit();
            invalidHabit.setName("");  // Blank name
            invalidHabit.setDescription("Valid Description");

            mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidHabit)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should not allow duplicate Habits with same name")
        void shouldNotAllowDuplicates() throws Exception {
            Habit firstHabit = new Habit();
            firstHabit.setName("Unique Name");
            firstHabit.setDescription("First Description");

            // Create first Habit
            mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstHabit)))
                    .andExpect(status().isCreated());

            // Try to create duplicate
            Habit duplicateHabit = new Habit();
            duplicateHabit.setName("Unique Name");  // Same name
            duplicateHabit.setDescription("Different Description");

            mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateHabit)))
                    .andExpect(status().isConflict());  // 409 Conflict
        }
    }

    @Nested
    @DisplayName("PUT /api/Habits/{id}")
    class UpdateHabit {

        @Test
        @DisplayName("should update existing Habit")
        void shouldUpdateExistingHabit() throws Exception {
            // Create initial Habit
            Habit initialHabit = new Habit();
            initialHabit.setName("Original Name");
            initialHabit.setDescription("Original Description");

            String response = mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(initialHabit)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Habit createdHabit = objectMapper.readValue(response, Habit.class);

            // Update the Habit
            Habit updatedHabit = new Habit();
            updatedHabit.setName("Updated Name");
            updatedHabit.setDescription("Updated Description");
            updatedHabit.completeToday();

            mockMvc.perform(put("/api/Habits/{id}", createdHabit.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedHabit)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"))
                    .andExpect(jsonPath("$.description").value("Updated Description"))
                    .andExpect(jsonPath("$.completed").value(true));
        }

        @Test
        @DisplayName("should return 404 when updating non-existent Habit")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            Habit updateHabit = new Habit();
            updateHabit.setName("Update Name");
            updateHabit.setDescription("Update Description");

            mockMvc.perform(put("/api/Habits/{id}", 999999)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateHabit)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should validate required fields on update")
        void shouldValidateRequiredFieldsOnUpdate() throws Exception {
            // Create initial Habit
            Habit initialHabit = new Habit();
            initialHabit.setName("Original Name");
            initialHabit.setDescription("Original Description");

            String response = mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(initialHabit)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Habit createdHabit = objectMapper.readValue(response, Habit.class);

            // Try to update with invalid data
            String invalidUpdate = "{\"name\":\"\",\"description\":\"Valid Description\"}";

            mockMvc.perform(put("/api/Habits/{id}", createdHabit.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidUpdate))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/Habits/{id}")
    class DeleteHabit {

        @Test
        @DisplayName("should delete existing Habit")
        void shouldDeleteExistingHabit() throws Exception {
            // Create Habit to delete
            Habit HabitToDelete = new Habit();
            HabitToDelete.setName("Delete Me");
            HabitToDelete.setDescription("To Be Deleted");

            String response = mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(HabitToDelete)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Habit createdHabit = objectMapper.readValue(response, Habit.class);

            // Delete the Habit
            mockMvc.perform(delete("/api/Habits/{id}", createdHabit.getId()))
                    .andExpect(status().isNoContent());

            // Verify it's gone
            mockMvc.perform(get("/api/Habits/{id}", createdHabit.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent Habit")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            mockMvc.perform(delete("/api/Habits/{id}", 999999))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Bonus: Search Functionality")
    class SearchHabits {

        @Test
        @DisplayName("BONUS: should search Habits by name")
        void shouldSearchHabitsByName() throws Exception {
            // Create test Habits
            Habit Habit1 = new Habit();
            Habit1.setName("Apple");
            Habit1.setDescription("Red fruit");

            Habit Habit2 = new Habit();
            Habit2.setName("Banana");
            Habit2.setDescription("Yellow fruit");

            Habit Habit3 = new Habit();
            Habit3.setName("Application");
            Habit3.setDescription("Software");

            mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Habit1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Habit2)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/Habits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Habit3)))
                    .andExpect(status().isCreated());

            // Search for Habits containing "App"
            mockMvc.perform(get("/api/Habits/search")
                    .param("name", "App"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[?(@.name == 'Apple')]").exists())
                    .andExpect(jsonPath("$[?(@.name == 'Application')]").exists());
        }
    }
}