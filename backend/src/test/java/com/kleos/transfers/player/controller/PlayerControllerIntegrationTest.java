package com.kleos.transfers.player.controller;

import com.kleos.transfers.common.test.AbstractPostgresIntegrationTest;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.kleos.transfers.common.test.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class PlayerControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PLAYERS_PATH = "/api/v1/players";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearPlayers() {
        DatabaseCleaner.clearAll(jdbcTemplate);
    }

    @Test
    void createsReadsAndUpdatesPlayerIdentity() throws Exception {
        MvcResult createResult = mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Test Player"))
                .andExpect(jsonPath("$.nationality").value("ENG"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(PLAYERS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.primaryPosition").value("CM"));

        mockMvc.perform(put(PLAYERS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedPlayerRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Player"))
                .andExpect(jsonPath("$.heightCm").value(181))
                .andExpect(jsonPath("$.preferredFoot").value("LEFT"))
                .andExpect(jsonPath("$.nationality").value("NED"));

        mockMvc.perform(get(PLAYERS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Updated Player"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchesPlayersByNameSubstring() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Kylian Mbappé",
                                  "dateOfBirth": "1998-12-20",
                                  "nationality": "FRA",
                                  "heightCm": 178,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "ST"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get(PLAYERS_PATH).param("q", "mbappe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Kylian Mbappé"));
    }

    @Test
    void filtersPlayersByPositionAgeAndLeague() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Young Midfielder",
                                  "dateOfBirth": "2005-01-01",
                                  "nationality": "ENG",
                                  "heightCm": 180,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "CM"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult forwardResult = mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Veteran Forward",
                                  "dateOfBirth": "1990-01-01",
                                  "nationality": "ESP",
                                  "heightCm": 185,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "ST"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID forwardId = UUID.fromString(readId(forwardResult));

        UUID clubId = createClub("Barcelona", "BAR", "ESP");
        UUID seasonId = createSeason("2024/25");
        UUID tournamentId = createTournament("La Liga", "LL", "ESP");
        createClubSeason(clubId, seasonId, tournamentId);
        createPlayerSeason(forwardId, clubId, seasonId);

        mockMvc.perform(get(PLAYERS_PATH).param("position", "MID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Young Midfielder"));

        mockMvc.perform(get(PLAYERS_PATH).param("minAge", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Veteran Forward"));

        mockMvc.perform(get(PLAYERS_PATH).param("league", "La Liga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Veteran Forward"));

        mockMvc.perform(get(PLAYERS_PATH)
                        .param("position", "FWD")
                        .param("league", "LL")
                        .param("minAge", "30")
                        .param("maxAge", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Veteran Forward"));
    }

    @Test
    void searchesPlayersByNationalityCodeAndCountryName() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Kylian Mbappé",
                                  "dateOfBirth": "1998-12-20",
                                  "nationality": "FRA",
                                  "heightCm": 178,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "ST"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Harry Kane",
                                  "dateOfBirth": "1993-07-28",
                                  "nationality": "ENG",
                                  "heightCm": 188,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "ST"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get(PLAYERS_PATH).param("q", "FRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Kylian Mbappé"));

        mockMvc.perform(get(PLAYERS_PATH).param("q", "France"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nationality").value("FRA"));
    }

    @Test
    void acceptsLowercaseNationalityAndNormalizesIt() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Case Player",
                                  "dateOfBirth": "2000-01-01",
                                  "nationality": "ger",
                                  "heightCm": 180,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "ST"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nationality").value("GER"));
    }

    @Test
    void rejectsInvalidPlayerRequest() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "A",
                                  "dateOfBirth": "2999-01-01",
                                  "nationality": "XXX",
                                  "heightCm": 139,
                                  "preferredFoot": null,
                                  "primaryPosition": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(5));
    }

    @Test
    void rejectsDuplicatePlayerNaturalKey() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    void softDeleteFreesNaturalKeyForRecreate() throws Exception {
        MvcResult createResult = mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(delete(PLAYERS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsMalformedJsonWithBadRequest() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Broken Player",
                                  "preferredFoot": "NOT_A_FOOT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void rejectsInvalidUuidPathWithBadRequest() throws Exception {
        mockMvc.perform(get(PLAYERS_PATH + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsNotFoundForUnknownPlayer() throws Exception {
        mockMvc.perform(get(PLAYERS_PATH + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void softDeletesPlayerAndHidesItFromReads() throws Exception {
        MvcResult createResult = mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(delete(PLAYERS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PLAYERS_PATH + "/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(PLAYERS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(delete(PLAYERS_PATH + "/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesAndClearsPlayerPhotoMedia() throws Exception {
        MvcResult createResult = mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(put(PLAYERS_PATH + "/{id}/media", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/Example.jpg/200px-Example.jpg",
                                  "attribution": "Example Author via Wikimedia",
                                  "license": "CC BY-SA 4.0",
                                  "source": "wikimedia"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value(
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/Example.jpg/200px-Example.jpg"))
                .andExpect(jsonPath("$.photoAttribution").value("Example Author via Wikimedia"))
                .andExpect(jsonPath("$.photoLicense").value("CC BY-SA 4.0"))
                .andExpect(jsonPath("$.photoSource").value("wikimedia"));

        mockMvc.perform(put(PLAYERS_PATH + "/{id}/media", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageUrl": null,
                                  "attribution": null,
                                  "license": null,
                                  "source": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value(nullValue()))
                .andExpect(jsonPath("$.photoSource").value(nullValue()));
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }

    private UUID createClub(String name, String shortName, String countryCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "shortName": "%s",
                                  "countryCode": "%s"
                                }
                                """.formatted(name, shortName, countryCode)))
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

    private UUID createTournament(String name, String shortName, String countryCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "shortName": "%s",
                                  "confederation": "UEFA",
                                  "type": "LEAGUE",
                                  "countryCode": "%s"
                                }
                                """.formatted(name, shortName, countryCode)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private void createClubSeason(UUID clubId, UUID seasonId, UUID tournamentId) throws Exception {
        mockMvc.perform(post("/api/v1/club-seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clubId": "%s",
                                  "seasonId": "%s",
                                  "tournamentId": "%s"
                                }
                                """.formatted(clubId, seasonId, tournamentId)))
                .andExpect(status().isCreated());
    }

    private void createPlayerSeason(UUID playerId, UUID clubId, UUID seasonId) throws Exception {
        mockMvc.perform(post("/api/v1/player-seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "clubId": "%s",
                                  "seasonId": "%s",
                                  "appearances": 20,
                                  "minutesPlayed": 1600,
                                  "goals": 5,
                                  "assists": 2,
                                  "xg": 4.5,
                                  "xa": 1.8,
                                  "primaryPosition": "ST"
                                }
                                """.formatted(playerId, clubId, seasonId)))
                .andExpect(status().isCreated());
    }

    private String validPlayerRequest() {
        return """
                {
                  "fullName": "Test Player",
                  "dateOfBirth": "2000-01-01",
                  "nationality": "ENG",
                  "heightCm": 180,
                  "preferredFoot": "RIGHT",
                  "primaryPosition": "CM"
                }
                """;
    }

    private String updatedPlayerRequest() {
        return """
                {
                  "fullName": "Updated Player",
                  "dateOfBirth": "2000-01-01",
                  "nationality": "NED",
                  "heightCm": 181,
                  "preferredFoot": "LEFT",
                  "primaryPosition": "CAM"
                }
                """;
    }
}
