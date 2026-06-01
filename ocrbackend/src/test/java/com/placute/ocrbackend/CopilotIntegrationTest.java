package com.placute.ocrbackend;

import com.placute.ocrbackend.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CopilotIntegrationTest extends BaseIntegrationTest {

    @Test
    void policeCanUseCopilotForVehicleLookup() throws Exception {
        String policeToken = createUserAndGetToken("police", UserRole.POLICE);
        createPlate("B123ABC");

        String body = """
                {
                  "message": "Arata-mi dosarul pentru B123ABC"
                }
                """;

        mockMvc.perform(post("/api/copilot/chat")
                        .header("Authorization", bearer(policeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("POLICE_LOOKUP"))
                .andExpect(jsonPath("$.tool").value("police_lookup"))
                .andExpect(jsonPath("$.deepLink").value("/vehicule?tab=cautare&plate=B123ABC"));
    }

    @Test
    void parkingRoleGetsForbiddenIntentForPoliceLookup() throws Exception {
        String parkingToken = createUserAndGetToken("parking", UserRole.PARKING);
        createPlate("B123ABC");

        String body = """
                {
                  "message": "dosar B123ABC"
                }
                """;

        mockMvc.perform(post("/api/copilot/chat")
                        .header("Authorization", bearer(parkingToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool").value("forbidden_intent"))
                .andExpect(jsonPath("$.intent").value("HELP"));
    }
}
