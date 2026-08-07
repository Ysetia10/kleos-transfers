package com.kleos.transfers.manager.controller;

import com.kleos.transfers.common.test.AbstractPostgresIntegrationTest;
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

class ManagerControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String MANAGERS_PATH = "/api/v1/managers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearManagers() {
        DatabaseCleaner.clearAll(jdbcTemplate);
    }

    @Test
    void createsReadsAndUpdatesManagerIdentity() throws Exception {
        MvcResult createResult = mockMvc.perform(post(MANAGERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validManagerRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Mikel Arteta"))
                .andExpect(jsonPath("$.nationality").value("ESP"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(MANAGERS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.dateOfBirth").value("1982-03-26"));

        mockMvc.perform(put(MANAGERS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Mikel Arteta Amatriain",
                                  "dateOfBirth": "1982-03-26",
                                  "nationality": "ESP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Mikel Arteta Amatriain"));

        mockMvc.perform(get(MANAGERS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void normalizesLowercaseNationality() throws Exception {
        mockMvc.perform(post(MANAGERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Jurgen Klopp",
                                  "dateOfBirth": "1967-06-16",
                                  "nationality": "ger"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nationality").value("GER"));
    }

    @Test
    void rejectsInvalidManagerRequest() throws Exception {
        mockMvc.perform(post(MANAGERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "A",
                                  "dateOfBirth": "2999-01-01",
                                  "nationality": "XXX"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(3));
    }

    @Test
    void softDeletesManagerAndHidesItFromReads() throws Exception {
        MvcResult createResult = mockMvc.perform(post(MANAGERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validManagerRequest()))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(delete(MANAGERS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(MANAGERS_PATH + "/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(MANAGERS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void returnsNotFoundForUnknownManager() throws Exception {
        mockMvc.perform(get(MANAGERS_PATH + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }

    private String validManagerRequest() {
        return """
                {
                  "fullName": "Mikel Arteta",
                  "dateOfBirth": "1982-03-26",
                  "nationality": "ESP"
                }
                """;
    }
}
