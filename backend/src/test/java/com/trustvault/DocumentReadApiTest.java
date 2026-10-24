package com.trustvault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentReadApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String token(String email, String password) throws Exception {
        String body = mapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(response);
        return json.get("token").asText();
    }

    @Test
    void ownerCanListDocumentsAndAdminCanReadAuditWithoutLazyLoadingFailure() throws Exception {
        String ownerToken = token("owner@trustvault.local", "Owner@12345");
        mvc.perform(get("/api/documents").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].owner").value("Document Owner"));

        String adminToken = token("admin@trustvault.local", "Admin@12345");
        mvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chainValid").value(true))
                .andExpect(jsonPath("$.events[0].actor").exists());
    }
}
