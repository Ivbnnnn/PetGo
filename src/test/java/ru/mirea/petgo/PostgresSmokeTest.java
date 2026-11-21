package ru.mirea.petgo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "PETGO_TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${PETGO_TEST_DB_URL}",
        "spring.datasource.username=${PETGO_TEST_DB_USER:petgo}",
        "spring.datasource.password=${PETGO_TEST_DB_PASSWORD:petgo}",
        "app.bootstrap.email=",
        "app.bootstrap.password="
})
@AutoConfigureMockMvc
class PostgresSmokeTest {
    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void jwtKey(DynamicPropertyRegistry properties) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String encoded = Base64.getEncoder().encodeToString(key);
        properties.add("app.jwt.secret-base64", () -> encoded);
    }

    @Test
    void applicationStartsWithFlywayAndProtectsAudit() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mvc.perform(get("/api/admin/audit").header("X-Correlation-ID", "smoke-audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", "smoke-audit"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
