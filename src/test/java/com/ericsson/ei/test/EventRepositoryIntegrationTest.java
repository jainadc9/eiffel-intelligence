package com.ericsson.ei.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestClientException;

import com.ericsson.ei.test.generated.api.ApiApi;
import com.ericsson.ei.test.generated.client.ApiClient;
import com.ericsson.ei.test.generated.model.SearchParameters;

public class EventRepositoryIntegrationTest {

    private ApiApi apiApi;
    private ApiClient apiClient;

    @Before
    public void setUp() {
        apiClient = new ApiClient();
        // Set base path to a test server or mock server
        apiClient.setBasePath("http://localhost:8080");
        apiApi = new ApiApi(apiClient);
    }

    @Test
    public void testApiClientConfiguration() {
        // Test that the API client is properly configured
        assertNotNull(apiClient);
        assertNotNull(apiApi);
        assertEquals("http://localhost:8080", apiClient.getBasePath());
    }

    @Test
    public void testSearchParametersModel() {
        // Test the SearchParameters model
        SearchParameters searchParams = new SearchParameters();
        searchParams.setDlt(Arrays.asList("CAUSE", "CONTEXT"));
        searchParams.setUlt(Arrays.asList("ARTIFACT", "SUBJECT"));

        assertNotNull(searchParams.getDlt());
        assertNotNull(searchParams.getUlt());
        assertEquals(2, searchParams.getDlt().size());
        assertEquals(2, searchParams.getUlt().size());
        assertTrue(searchParams.getDlt().contains("CAUSE"));
        assertTrue(searchParams.getDlt().contains("CONTEXT"));
        assertTrue(searchParams.getUlt().contains("ARTIFACT"));
        assertTrue(searchParams.getUlt().contains("SUBJECT"));
    }

    @Test
    public void testApiExceptionHandling() {
        // Test that API exceptions are properly handled
        try {
            // This should fail since we don't have a real server running
            apiApi.getEventUsingGET("non-existent-id");
            fail("Expected RestClientException");
        } catch (RestClientException e) {
            // Expected behavior - the client should throw an exception
            // when trying to connect to a non-existent server
            assertNotNull(e);
        }
    }

    @Test
    public void testParameterValidation() {
        // Test parameter validation
        Map<String, String> params = new HashMap<>();
        params.put("meta.type", "EiffelArtifactCreatedEvent");
        params.put("data.identity", "pkg:maven/my.namespace/my-name@1.0.0");

        // Verify parameters are properly formatted
        assertNotNull(params);
        assertEquals("EiffelArtifactCreatedEvent", params.get("meta.type"));
        assertEquals("pkg:maven/my.namespace/my-name@1.0.0", params.get("data.identity"));
    }

    @Test
    public void testSearchParametersWithAllLinkTypes() {
        // Test all supported link types
        SearchParameters searchParams = new SearchParameters();
        searchParams.setDlt(Arrays.asList(
            "CAUSE", "CONTEXT", "FLOW_CONTEXT", "ACTIVITY_EXECUTION",
            "PREVIOUS_ACTIVITY_EXECUTION", "PREVIOUS_VERSION", "COMPOSITION",
            "ENVIRONMENT", "ARTIFACT", "SUBJECT", "ELEMENT", "BASE",
            "CHANGE", "TEST_SUITE_EXECUTION", "TEST_CASE_EXECUTION",
            "IUT", "TERC", "MODIFIED_ANNOUNCEMENT", "SUB_CONFIDENCE_LEVEL",
            "REUSED_ARTIFACT", "VERIFICATION_BASIS", "PRECURSOR",
            "ORIGINAL_TRIGGER", "CONFIGURATION", "ALL"
        ));

        assertNotNull(searchParams.getDlt());
        assertEquals(25, searchParams.getDlt().size());
        assertTrue(searchParams.getDlt().contains("ALL"));
        assertTrue(searchParams.getDlt().contains("CAUSE"));
        assertTrue(searchParams.getDlt().contains("ARTIFACT"));
    }
}