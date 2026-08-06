package com.kleos.transfers.managerseason.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kleos.transfers.common.test.DatabaseCleaner;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagerSeasonControllerIntegrationTest {

    private static final String MANAGER_SEASONS_PATH = "/api/v1/manager-seasons";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearData() {
        DatabaseCleaner.clearAll(jdbcTemplate);
    }

    @Test
    void createsReadsAndUpdatesManagerSeason() throws Exception {
        UUID managerId = createManager("Mikel Arteta", "ESP");
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25");

        MvcResult createResult = mockMvc.perform(post(MANAGER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(managerId, clubId, seasonId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.managerId").value(managerId.toString()))
                .andExpect(jsonPath("$.managerName").value("Mikel Arteta"))
                .andExpect(jsonPath("$.clubId").value(clubId.toString()))
                .andExpect(jsonPath("$.clubName").value("Arsenal"))
                .andExpect(jsonPath("$.seasonLabel").value("2024/25"))
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(MANAGER_SEASONS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        UUID otherClubId = createClub("Barcelona", "FCB", "ESP");
        mockMvc.perform(put(MANAGER_SEASONS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(managerId, otherClubId, seasonId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("Barcelona"));
    }

    @Test
    void allowsDifferentManagersAtSameClubSeason() throws Exception {
        UUID artetaId = createManager("Mikel Arteta", "ESP");
        UUID kloppId = createManager("Jurgen Klopp", "GER");
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(MANAGER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(artetaId, clubId, seasonId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(MANAGER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(kloppId, clubId, seasonId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(MANAGER_SEASONS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsDuplicateManagerClubSeason() throws Exception {
        UUID managerId = createManager("Mikel Arteta", "ESP");
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(MANAGER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(managerId, clubId, seasonId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(MANAGER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(managerId, clubId, seasonId)))
                .andExpect(status().isConflict());
    }

    @Test
    void softDeletesAndAllowsRecreate() throws Exception {
        UUID managerId = createManager("Mikel Arteta", "ESP");
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25");

        MvcResult createResult = mockMvc.perform(post(MANAGER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(managerId, clubId, seasonId)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));
        mockMvc.perform(delete(MANAGER_SEASONS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(MANAGER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(managerId, clubId, seasonId)))
                .andExpect(status().isCreated());
    }

    @Test
    void importsManagerSeasonsInBulkAndSkipsDuplicates() throws Exception {
        UUID artetaId = createManager("Mikel Arteta", "ESP");
        UUID kloppId = createManager("Jurgen Klopp", "GER");
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(MANAGER_SEASONS_PATH + "/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    %s,
                                    %s,
                                    %s
                                  ]
                                }
                                """.formatted(
                                managerSeasonJson(artetaId, clubId, seasonId),
                                managerSeasonJson(artetaId, clubId, seasonId),
                                managerSeasonJson(kloppId, clubId, seasonId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(1));
    }

    @Test
    void rejectsUnknownIdentityReferences() throws Exception {
        mockMvc.perform(post(MANAGER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerSeasonJson(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    private UUID createManager(String name, String nationality) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "dateOfBirth": "1980-01-01",
                                  "nationality": "%s"
                                }
                                """.formatted(name, nationality)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private UUID createClub(String name, String shortName, String country) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "shortName": "%s",
                                  "countryCode": "%s"
                                }
                                """.formatted(name, shortName, country)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private UUID createSeason(String label) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "%s",
                                  "startDate": "2024-07-01",
                                  "endDate": "2025-06-30"
                                }
                                """.formatted(label)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private String managerSeasonJson(UUID managerId, UUID clubId, UUID seasonId) {
        return """
                {
                  "managerId": "%s",
                  "clubId": "%s",
                  "seasonId": "%s"
                }
                """.formatted(managerId, clubId, seasonId);
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}
