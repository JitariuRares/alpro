package com.placute.ocrbackend;

import com.placute.ocrbackend.model.AuditLog;
import com.placute.ocrbackend.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InsuranceLookupAuditIntegrationTest extends BaseIntegrationTest {

    @Test
    void insuranceCreateAndUpdateAreVisibleInPoliceLookupAndAudited() throws Exception {
        createPlate("B444LIC");
        String insuranceToken = createUserAndGetToken("insurance", UserRole.INSURANCE);
        String policeToken = createUserAndGetToken("police", UserRole.POLICE);

        String createBody = """
                {
                  "company": "Omniasig",
                  "validFrom": "2026-01-01",
                  "validTo": "2026-12-31",
                  "licensePlate": {
                    "plateNumber": "B444LIC"
                  }
                }
                """;

        String createResponse = mockMvc.perform(post("/api/insurance")
                        .header("Authorization", bearer(insuranceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Omniasig"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long insuranceId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = """
                {
                  "company": "Groupama",
                  "validFrom": "2026-02-01",
                  "validTo": "2026-12-31"
                }
                """;

        mockMvc.perform(put("/api/insurance/{insuranceId}", insuranceId)
                        .header("Authorization", bearer(insuranceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Groupama"));

        mockMvc.perform(get("/api/police/lookup/B444LIC")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("B444LIC"))
                .andExpect(jsonPath("$.insurances[0].company").value("Groupama"))
                .andExpect(jsonPath("$.auditEvents[0].action").value("INSURANCE_UPDATE"))
                .andExpect(jsonPath("$.auditEvents[0].actorUsername").exists());

        mockMvc.perform(get("/api/audit")
                        .param("plate", "B444LIC")
                        .param("limit", "10")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetPlateNumber").value("B444LIC"));

        mockMvc.perform(get("/api/audit")
                        .param("action", "INSURANCE_UPDATE")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("INSURANCE_UPDATE"));

        List<AuditLog> auditLogs = auditLogRepository.findAll();
        List<String> actions = auditLogs.stream()
                .map(AuditLog::getAction)
                .collect(Collectors.toList());

        assertThat(actions).contains("INSURANCE_CREATE", "INSURANCE_UPDATE", "POLICE_LOOKUP");

        List<AuditLog> plateAuditLogs = auditLogs.stream()
                .filter(log -> "B444LIC".equals(log.getTargetPlateNumber()))
                .toList();
        assertThat(plateAuditLogs).hasSizeGreaterThanOrEqualTo(3);
    }
}
