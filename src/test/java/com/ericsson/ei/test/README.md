# OpenAPI Generated Client Tests

This directory contains tests for the OpenAPI generated client based on the Eiffel Event Repository API specification.

## Overview

The OpenAPI generator Maven plugin has been configured to generate a Java client from the OpenAPI specification located at:
- `src/main/resources/openapi-spec.yaml`

The generated client code is placed in:
- `target/generated-test-sources/openapi/src/main/java/com/ericsson/ei/test/generated/`

## Generated Components

### API Client
- **ApiApi**: Main API interface with methods for:
  - `getEventsUsingGET()` - Get all events with optional filtering
  - `getEventUsingGET()` - Get a single event by ID
  - `searchUsingPOST()` - Search for upstream/downstream events

### Models
- **EiffelEvent**: Represents an Eiffel event with meta, data, and links
- **SearchParameters**: Parameters for upstream/downstream search
- **GetEventsUsingGET200Response**: Response model for events listing
- **SearchUsingPOST200Response**: Response model for search operations

### Client Configuration
- **ApiClient**: HTTP client configuration and base path management

## Test Classes

### EventRepositoryApiTest
Unit tests that verify:
- API client configuration
- Model object creation and validation
- Search parameters with all supported link types
- Parameter validation for event queries

### EventRepositoryIntegrationTest
Integration tests that demonstrate:
- Real API client usage patterns
- Exception handling
- Configuration validation
- Link type validation

## Usage Example

```java
// Create and configure the API client
ApiClient apiClient = new ApiClient();
apiClient.setBasePath("http://your-event-repository-server:8080");

// Create the API instance
ApiApi api = new ApiApi(apiClient);

// Get events with filtering
Map<String, String> params = new HashMap<>();
params.put("meta.type", "EiffelArtifactCreatedEvent");
GetEventsUsingGET200Response events = api.getEventsUsingGET(100, params);

// Get a specific event
EiffelEvent event = api.getEventUsingGET("event-id");

// Search for related events
SearchParameters searchParams = new SearchParameters();
searchParams.setDlt(Arrays.asList("CAUSE", "CONTEXT"));
searchParams.setUlt(Arrays.asList("ARTIFACT"));
SearchUsingPOST200Response results = api.searchUsingPOST("event-id", -1, -1, searchParams);
```

## Running Tests

To run only the OpenAPI client tests:
```bash
mvn test -Dtest=EventRepositoryApiTest,EventRepositoryIntegrationTest
```

To regenerate the client code:
```bash
mvn generate-test-sources
```

## Supported Link Types

The API supports the following link types for upstream/downstream searches:
- CAUSE, CONTEXT, FLOW_CONTEXT
- ACTIVITY_EXECUTION, PREVIOUS_ACTIVITY_EXECUTION, PREVIOUS_VERSION
- COMPOSITION, ENVIRONMENT, ARTIFACT, SUBJECT, ELEMENT, BASE
- CHANGE, TEST_SUITE_EXECUTION, TEST_CASE_EXECUTION
- IUT, TERC, MODIFIED_ANNOUNCEMENT, SUB_CONFIDENCE_LEVEL
- REUSED_ARTIFACT, VERIFICATION_BASIS, PRECURSOR
- ORIGINAL_TRIGGER, CONFIGURATION, ALL