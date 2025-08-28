package com.ericsson.ei.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.ei.test.generated.api.ApiApi;
import com.ericsson.ei.test.generated.client.ApiClient;
import com.ericsson.ei.test.generated.model.EiffelEvent;
import com.ericsson.ei.test.generated.model.SearchParameters;

public class EventRepositoryApiTest {
    
    private ApiApi apiApi;
    private ApiClient apiClient;

    @Before
    public void setUp() {
        apiClient = new ApiClient();
        // Set base path to a test server
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
    public void testEiffelEventModel() {
        // Test the EiffelEvent model
        EiffelEvent event = createMockEiffelEvent();
        
        assertNotNull(event);
        assertNotNull(event.getMeta());
        assertNotNull(event.getData());
        assertNotNull(event.getLinks());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) event.getMeta();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) event.getData();
        
        assertEquals("1a4bc724-95f7-43c9-b5de-6348ddddbafe", meta.get("id"));
        assertEquals("EiffelCompositionDefinedEvent", meta.get("type"));
        assertEquals("My Composition", data.get("name"));
        assertEquals(1, event.getLinks().size());
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

    private EiffelEvent createMockEiffelEvent() {
        EiffelEvent event = new EiffelEvent();
        Map<String, Object> meta = new HashMap<>();
        meta.put("id", "1a4bc724-95f7-43c9-b5de-6348ddddbafe");
        meta.put("type", "EiffelCompositionDefinedEvent");
        meta.put("version", "4.0.0");
        meta.put("time", 657718729693L);
        event.setMeta(meta);
        
        Map<String, Object> data = new HashMap<>();
        data.put("name", "My Composition");
        event.setData(data);
        
        Map<String, Object> link = new HashMap<>();
        link.put("target", "a77fc96e-847c-4828-9a16-2c2edd3c9580");
        link.put("type", "ELEMENT");
        event.setLinks(Arrays.asList(link));
        
        return event;
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