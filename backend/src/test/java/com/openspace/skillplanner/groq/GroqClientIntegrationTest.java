package com.openspace.skillplanner.groq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspace.skillplanner.config.GroqProperties;
import com.openspace.skillplanner.exception.ApiExceptions;
import java.math.BigDecimal;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

class GroqClientIntegrationTest {
    private static MockWebServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.shutdown();
    }

    @Test
    void sendsStructuredPlanRequestAndParsesValidPlan() throws Exception {
        enqueueContent("""
                {"status":"OK","reason":"","days":[{"dayNumber":1,"title":"Basics","goal":"Learn core ideas.","tasks":[{"title":"Build a small exercise","description":"Create and test an example.","estimatedMinutes":60}]}]}
                """);

        GroqPlan result = client().generate("Java", 1, new BigDecimal("1"), null);
        RecordedRequest request = server.takeRequest();
        String requestBody = request.getBody().readUtf8();
        var requestJson = objectMapper.readTree(requestBody);
        String userContent = requestJson.path("messages").path(1).path("content").asText();

        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.days()).hasSize(1);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/");
        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-key");
        assertThat(requestJson.path("response_format").path("type").asText()).isEqualTo("json_schema");
        assertThat(userContent).contains("BEGIN_SKILL_PLAN_JSON", "\"skill\":\"weather\"");
        assertThat(requestJson.path("messages").path(0).path("content").asText())
                .contains("You are the OpenSpace Learning Planner.");
    }

    @Test
    void parsesRejectedPlanStatus() throws Exception {
        enqueueContent("""
                {"status":"REJECTED","reason":"Please submit a learnable skill.","days":[]}
                """);

        GroqPlan result = client().generate("weather", 1, new BigDecimal("1"), null);

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.reason()).contains("learnable skill");
    }

    @Test
    void rejectsMalformedModelJson() throws Exception {
        enqueueContent("{not-json");

        assertThatThrownBy(() -> client().generate("Java", 1, new BigDecimal("1"), null))
                .isInstanceOf(GroqClient.InvalidGroqResponseException.class);
    }

    @Test
    void mapsProvider429AndCarriesRetryAfter() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .addHeader(HttpHeaders.RETRY_AFTER, "17")
                .setBody("{\"error\":{\"message\":\"rate limited\"}}"));

        assertThatThrownBy(() -> client().generate("Java", 1, new BigDecimal("1"), null))
                .isInstanceOf(ApiExceptions.UpstreamUnavailableException.class)
                .extracting(exception -> ((ApiExceptions.UpstreamUnavailableException) exception)
                        .getRetryAfterSeconds())
                .isEqualTo(17L);
    }

    private GroqClient client() {
        GroqProperties properties = new GroqProperties(
                server.url("/").toString(),
                "test-key",
                "test-model",
                0.2,
                8192,
                Duration.ofSeconds(2));
        return new GroqClient(
                RestClient.builder().baseUrl(server.url("/").toString()).build(),
                properties,
                objectMapper);
    }

    private void enqueueContent(String content) throws Exception {
        String response = "{\"choices\":[{\"message\":{\"content\":"
                + objectMapper.writeValueAsString(content) + "}}]}";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setBody(response));
    }
}
