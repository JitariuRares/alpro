package com.placute.ocrbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.placute.ocrbackend.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParkingFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void parkingEntryThenExitThenEvidenceWorks() throws Exception {
        String parkingToken = createUserAndGetToken("parking", UserRole.PARKING);
        createPlate("B100AAA");

        MockMultipartFile entryImage = new MockMultipartFile(
                "image",
                "entry.jpg",
                "image/jpeg",
                "entry-proof".getBytes()
        );

        String entryResponse = mockMvc.perform(multipart("/api/parking/entry")
                        .file(entryImage)
                        .param("plateNumber", "B100AAA")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("B100AAA"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.hasEntryImage").value(true))
                .andExpect(jsonPath("$.hasExitImage").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long sessionId = objectMapper.readTree(entryResponse).get("id").asLong();

        MockMultipartFile exitImage = new MockMultipartFile(
                "image",
                "exit.jpg",
                "image/jpeg",
                "exit-proof".getBytes()
        );

        mockMvc.perform(multipart("/api/parking/exit")
                        .file(exitImage)
                        .param("plateNumber", "B100AAA")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.hasEntryImage").value(true))
                .andExpect(jsonPath("$.hasExitImage").value(true));

        String sessionsJson = mockMvc.perform(get("/api/parking/B100AAA")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CLOSED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sessions = objectMapper.readTree(sessionsJson);
        assertThat(sessions.isArray()).isTrue();
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).get("durationMinutes").asLong()).isGreaterThanOrEqualTo(0L);

        mockMvc.perform(get("/api/parking/sessions/{sessionId}/evidence/entry", sessionId)
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));

        mockMvc.perform(get("/api/parking/sessions/{sessionId}/evidence/exit", sessionId)
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void secondEntryWhileOpenSessionExistsIsRejected() throws Exception {
        String parkingToken = createUserAndGetToken("parking", UserRole.PARKING);
        createPlate("CJ55XYZ");

        MockMultipartFile firstImage = new MockMultipartFile(
                "image",
                "entry1.jpg",
                "image/jpeg",
                "first-entry".getBytes()
        );
        MockMultipartFile secondImage = new MockMultipartFile(
                "image",
                "entry2.jpg",
                "image/jpeg",
                "second-entry".getBytes()
        );

        mockMvc.perform(multipart("/api/parking/entry")
                        .file(firstImage)
                        .param("plateNumber", "CJ55XYZ")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/parking/entry")
                        .file(secondImage)
                        .param("plateNumber", "CJ55XYZ")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OPEN")));
    }

    @Test
    void exitWithoutOpenSessionIsRejected() throws Exception {
        String parkingToken = createUserAndGetToken("parking", UserRole.PARKING);
        createPlate("TM10AAA");

        MockMultipartFile exitImage = new MockMultipartFile(
                "image",
                "exit.jpg",
                "image/jpeg",
                "exit-no-open".getBytes()
        );

        mockMvc.perform(multipart("/api/parking/exit")
                        .file(exitImage)
                        .param("plateNumber", "TM10AAA")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nu exista sesiune OPEN")));
    }
}
