package com.kleos.transfers.common.bulk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kleos.transfers.manager.repository.ManagerRepository;
import com.kleos.transfers.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BulkImportIntegrationTest {

    private static final String PLAYERS_BULK_PATH = "/api/v1/players/bulk";
    private static final String MANAGERS_BULK_PATH = "/api/v1/managers/bulk";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @BeforeEach
    void clearIdentities() {
        playerRepository.deleteAllInBatch();
        managerRepository.deleteAllInBatch();
    }

    @Test
    void importsValidPlayersInOneRequest() throws Exception {
        mockMvc.perform(post(PLAYERS_BULK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    %s,
                                    {
                                      "fullName": "Jude Bellingham",
                                      "dateOfBirth": "2003-06-29",
                                      "nationality": "eng",
                                      "heightCm": 186,
                                      "preferredFoot": "RIGHT",
                                      "primaryPosition": "CM"
                                    }
                                  ]
                                }
                                """.formatted(bukayoSaka())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(2))
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.created[1].nationality").value("ENG"));

        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void skipsRowsThatAlreadyExistOrRepeatWithinTheRequest() throws Exception {
        mockMvc.perform(post(PLAYERS_BULK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [%s]}
                                """.formatted(bukayoSaka())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1));

        // Same player again, twice, with different casing on the name.
        mockMvc.perform(post(PLAYERS_BULK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [%s, %s]}
                                """.formatted(bukayoSaka().replace("Bukayo Saka", "bukayo saka"), bukayoSaka())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.skippedCount").value(2))
                .andExpect(jsonPath("$.skipped[0].index").value(0))
                .andExpect(jsonPath("$.skipped[0].reason").value("already exists"))
                .andExpect(jsonPath("$.skipped[1].reason").value("already exists"));

        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void reportsInvalidRowsWithoutRejectingTheBatch() throws Exception {
        mockMvc.perform(post(PLAYERS_BULK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    %s,
                                    {
                                      "fullName": "X",
                                      "dateOfBirth": "2003-06-29",
                                      "nationality": "XXX",
                                      "heightCm": 186,
                                      "preferredFoot": "RIGHT",
                                      "primaryPosition": "RW"
                                    }
                                  ]
                                }
                                """.formatted(bukayoSaka())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.failed[0].index").value(1))
                .andExpect(jsonPath("$.failed[0].reference").value("X"))
                .andExpect(jsonPath("$.failed[0].reason").isNotEmpty());
    }

    @Test
    void rejectsEmptyBatch() throws Exception {
        mockMvc.perform(post(PLAYERS_BULK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("items"));
    }

    @Test
    void importsManagersThroughTheSameContract() throws Exception {
        mockMvc.perform(post(MANAGERS_BULK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"fullName": "Mikel Arteta", "dateOfBirth": "1982-03-26", "nationality": "ESP"},
                                    {"fullName": "Mikel Arteta", "dateOfBirth": "1982-03-26", "nationality": "ESP"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.skipped[0].reason").value("duplicate within request"));
    }

    private String bukayoSaka() {
        return """
                {
                  "fullName": "Bukayo Saka",
                  "dateOfBirth": "2001-09-05",
                  "nationality": "ENG",
                  "heightCm": 178,
                  "preferredFoot": "LEFT",
                  "primaryPosition": "RW"
                }
                """;
    }
}
