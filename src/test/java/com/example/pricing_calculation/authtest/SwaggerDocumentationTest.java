package com.example.pricing_calculation.authtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:authbot;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false"
        }
)
class SwaggerDocumentationTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void openApiJsonContainsCoreEndpointsAndBearerScheme() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");

        assertEquals(200, response.statusCode());
        JsonNode document = objectMapper.readTree(response.body());
        assertEquals("Parking Payment System API", document.path("info").path("title").stringValue(""));
        assertFalse(document.path("paths").path("/api/auth/register").isMissingNode());
        assertFalse(document.path("paths").path("/api/auth/login").isMissingNode());
        assertFalse(document.path("paths").path("/api/auth/logout").isMissingNode());
        assertFalse(document.path("paths").path("/api/roles").isMissingNode());
        assertFalse(document.path("paths").path("/api/roles/parking-user").isMissingNode());
        assertFalse(document.path("paths").path("/api/reservations").isMissingNode());
        assertFalse(document.path("paths").path("/api/dashboard/overview").isMissingNode());
        assertEquals(
                "bearer",
                document.path("components").path("securitySchemes").path("bearerAuth").path("scheme")
                        .stringValue("")
        );
    }

    @Test
    void swaggerUiIsAvailable() throws Exception {
        HttpResponse<String> response = get("/swagger-ui/index.html");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().toLowerCase().contains("swagger ui"));
    }

    @Test
    void parkingUserRoleEndpointDocumentsDriverCapabilities() throws Exception {
        HttpResponse<String> response = get("/api/roles/parking-user");

        assertEquals(200, response.statusCode());
        JsonNode role = objectMapper.readTree(response.body());
        assertEquals("PARKING_USER", role.path("code").stringValue(""));
        assertEquals("Parking User / Driver", role.path("displayName").stringValue(""));
        assertTrue(role.path("capabilities").isArray());
        assertTrue(role.path("capabilities").size() >= 6);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
