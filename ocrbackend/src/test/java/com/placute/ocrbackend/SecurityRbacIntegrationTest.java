package com.placute.ocrbackend;

import com.placute.ocrbackend.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityRbacIntegrationTest extends BaseIntegrationTest {

    @Test
    void parkingRoleCannotAccessPoliceLookup() throws Exception {
        String parkingToken = createUserAndGetToken("parking", UserRole.PARKING);

        mockMvc.perform(get("/api/police/lookup/B123ABC")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void insuranceRoleCannotAccessVideoJobs() throws Exception {
        String insuranceToken = createUserAndGetToken("insurance", UserRole.INSURANCE);

        mockMvc.perform(get("/api/video-jobs")
                        .header("Authorization", bearer(insuranceToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void parkingRoleCannotAccessAuditLogs() throws Exception {
        String parkingToken = createUserAndGetToken("parking", UserRole.PARKING);

        mockMvc.perform(get("/api/audit")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void parkingRoleCannotAccessDetectionReview() throws Exception {
        String parkingToken = createUserAndGetToken("parking", UserRole.PARKING);

        mockMvc.perform(get("/api/detection-review")
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void policeRoleCannotCreateInsurance() throws Exception {
        String policeToken = createUserAndGetToken("police", UserRole.POLICE);
        createPlate("B123ABC");

        String insuranceBody = """
                {
                  "company": "Allianz",
                  "validFrom": "2026-01-01",
                  "validTo": "2026-12-31",
                  "licensePlate": {
                    "plateNumber": "B123ABC"
                  }
                }
                """;

        mockMvc.perform(post("/api/insurance")
                        .header("Authorization", bearer(policeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(insuranceBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void insuranceRoleCannotRegisterParkingEntry() throws Exception {
        String insuranceToken = createUserAndGetToken("insurance", UserRole.INSURANCE);
        createPlate("B777XYZ");

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "entry.jpg",
                "image/jpeg",
                "entry-proof".getBytes()
        );

        mockMvc.perform(multipart("/api/parking/entry")
                        .file(image)
                        .param("plateNumber", "B777XYZ")
                        .param("parkingZone", "Zona 1")
                        .header("Authorization", bearer(insuranceToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void policeRoleCannotRegisterParkingEntryOrExit() throws Exception {
        String policeToken = createUserAndGetToken("police", UserRole.POLICE);
        createPlate("B777POL");

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "parking.jpg",
                "image/jpeg",
                "parking-proof".getBytes()
        );

        mockMvc.perform(multipart("/api/parking/entry")
                        .file(image)
                        .param("plateNumber", "B777POL")
                        .param("parkingZone", "Zona 1")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/parking/exit")
                        .file(image)
                        .param("plateNumber", "B777POL")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void policeRoleCanReadParkingSessionsByPlate() throws Exception {
        String policeToken = createUserAndGetToken("police", UserRole.POLICE);
        createPlate("B778POL");

        mockMvc.perform(get("/api/parking/B778POL")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isOk());
    }

    @Test
    void parkingRoleCannotUseOcrFullEndpoint() throws Exception {
        String parkingToken = createUserAndGetToken("parking", UserRole.PARKING);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "car.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/ocr/full")
                        .file(image)
                        .header("Authorization", bearer(parkingToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void policeRoleCanAccessPoliceLookup() throws Exception {
        String policeToken = createUserAndGetToken("police", UserRole.POLICE);
        createPlate("B321POL");

        mockMvc.perform(get("/api/police/lookup/B321POL")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isOk());
    }
}
