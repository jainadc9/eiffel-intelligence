package com.ericsson.ei.erservice;

import com.ericsson.ei.erservice.api.EventsApiDelegate;
import com.ericsson.ei.erservice.api.SearchApiDelegate;
import com.ericsson.ei.erservice.model.EiffelEvent;
import com.ericsson.ei.erservice.model.EventsResponse;
import com.ericsson.ei.erservice.model.SearchParameters;
import com.ericsson.ei.erservice.model.SearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
@ConditionalOnProperty(name = "er.security.permitAll", havingValue = "true")
public class ErApiDelegateImpl implements EventsApiDelegate, SearchApiDelegate {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final List<String> events = new ArrayList<>();
    private static final Map<String, JsonNode> eventsById = new LinkedHashMap<>();

    static {
        loadEventsFromDirectory("src/test/resources/eventrepository/events");
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    public static void loadEventsFromDirectory(String dir) {
        events.clear();
        eventsById.clear();
        File directory = new File(dir);
        if (!directory.exists() || !directory.isDirectory()) return;
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".json")) continue;
            try {
                String content = Files.readString(Paths.get(file.getAbsolutePath()));
                events.add(content);
                JsonNode event = mapper.readTree(content);
                String id = event.at("/meta/id").asText();
                if (!id.isEmpty()) {
                    eventsById.put(id, event);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ResponseEntity<EiffelEvent> getEventUsingGET(String id) {
        for (String event : events) {
            try {
                Object document = Configuration.defaultConfiguration().jsonProvider().parse(event);
                Object value = JsonPath.read(document, "$.meta.id");
                if (id.equals(value)) {
                    return new ResponseEntity<>(mapper.readValue(event, EiffelEvent.class), HttpStatus.OK);
                }
            } catch (PathNotFoundException e) {
                continue;
            } catch (Exception e) {
                continue;
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<EventsResponse> getEventsUsingGET(Integer pageSize, Map<String, String> params) {
        String[] keysToIgnore = {"pageSize", "shallow"};
        List<EiffelEvent> matchedEvents = new ArrayList<>();

        for (String event : events) {
            boolean matches = true;
            try {
                Object document = Configuration.defaultConfiguration().jsonProvider().parse(event);
                for (String key : params.keySet()) {
                    boolean skip = false;
                    for (String ignore : keysToIgnore) {
                        if (key.equals(ignore)) { skip = true; break; }
                    }
                    if (skip) continue;

                    String expected = params.get(key);
                    Object value = JsonPath.read(document, "$." + key);
                    if (expected != null && !expected.isEmpty() && !value.toString().equals(expected)) {
                        matches = false;
                        break;
                    }
                }
            } catch (PathNotFoundException e) {
                matches = false;
            } catch (Exception e) {
                matches = false;
            }

            if (matches) {
                try {
                    matchedEvents.add(mapper.readValue(event, EiffelEvent.class));
                } catch (Exception e) {
                    // skip
                }
            }
        }

        EventsResponse response = new EventsResponse();
        response.pageNo(1).pageSize(pageSize).totalNumberItems(matchedEvents.size()).items(matchedEvents);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SearchResponse> searchUsingPOST(String id, Integer limit, Integer levels,
            Boolean tree, Boolean shallow, SearchParameters searchParameters) {

        JsonNode startEvent = eventsById.get(id);
        if (startEvent == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<String> ult = searchParameters != null && searchParameters.getUlt() != null
                ? searchParameters.getUlt() : Collections.emptyList();
        List<String> dlt = searchParameters != null && searchParameters.getDlt() != null
                ? searchParameters.getDlt() : Collections.emptyList();

        int maxLevels = (levels == null || levels <= 0) ? Integer.MAX_VALUE : levels;
        int maxEvents = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;

        List<Object> upstream = new ArrayList<>();
        List<Object> downstream = new ArrayList<>();

        if (!ult.isEmpty()) {
            Set<String> visited = new HashSet<>();
            visited.add(id);
            if (Boolean.TRUE.equals(tree)) {
                List<Object> upTree = buildUpstreamTree(startEvent, ult, maxLevels, maxEvents, visited);
                if (upTree != null) upstream = upTree;
            } else {
                collectUpstreamFlat(startEvent, ult, maxLevels, maxEvents, visited, upstream);
            }
        }

        if (!dlt.isEmpty()) {
            Set<String> visited = new HashSet<>();
            visited.add(id);
            if (Boolean.TRUE.equals(tree)) {
                List<Object> downTree = buildDownstreamTree(startEvent, dlt, maxLevels, maxEvents, visited);
                if (downTree != null) downstream = downTree;
            } else {
                collectDownstreamFlat(startEvent, dlt, maxLevels, maxEvents, visited, downstream);
            }
        }

        SearchResponse response = new SearchResponse();
        response.setUpstreamLinkObjects(upstream);
        response.setDownstreamLinkObjects(downstream);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private List<Object> buildUpstreamTree(JsonNode event, List<String> linkTypes, int maxLevels, int maxEvents, Set<String> visited) {
        if (maxLevels <= 0) return null;

        List<Object> tree = new ArrayList<>();
        tree.add(mapper.convertValue(event, Map.class));

        JsonNode links = event.get("links");
        if (links != null && links.isArray()) {
            for (JsonNode link : links) {
                String type = link.has("type") ? link.get("type").asText() : "";
                String target = link.has("target") ? link.get("target").asText() : "";
                if (target.isEmpty() || !matchesLinkType(type, linkTypes)) continue;
                if (visited.contains(target)) continue;

                JsonNode targetEvent = eventsById.get(target);
                if (targetEvent == null) continue;

                visited.add(target);
                List<Object> subtree = buildUpstreamTree(targetEvent, linkTypes, maxLevels - 1, maxEvents, visited);
                if (subtree != null) tree.add(subtree);
            }
        }
        return tree;
    }

    private List<Object> buildDownstreamTree(JsonNode event, List<String> linkTypes, int maxLevels, int maxEvents, Set<String> visited) {
        if (maxLevels <= 0) return null;

        String eventId = event.at("/meta/id").asText();
        List<Object> tree = new ArrayList<>();
        tree.add(mapper.convertValue(event, Map.class));

        for (Map.Entry<String, JsonNode> entry : eventsById.entrySet()) {
            String candidateId = entry.getKey();
            if (visited.contains(candidateId)) continue;

            JsonNode candidate = entry.getValue();
            JsonNode links = candidate.get("links");
            if (links == null || !links.isArray()) continue;

            for (JsonNode link : links) {
                String type = link.has("type") ? link.get("type").asText() : "";
                String target = link.has("target") ? link.get("target").asText() : "";
                if (eventId.equals(target) && matchesLinkType(type, linkTypes)) {
                    visited.add(candidateId);
                    List<Object> subtree = buildDownstreamTree(candidate, linkTypes, maxLevels - 1, maxEvents, visited);
                    if (subtree != null) tree.add(subtree);
                    break;
                }
            }
        }
        return tree;
    }

    private void collectUpstreamFlat(JsonNode event, List<String> linkTypes, int maxLevels, int maxEvents, Set<String> visited, List<Object> result) {
        if (maxLevels <= 0 || result.size() >= maxEvents) return;
        JsonNode links = event.get("links");
        if (links == null || !links.isArray()) return;

        for (JsonNode link : links) {
            String type = link.has("type") ? link.get("type").asText() : "";
            String target = link.has("target") ? link.get("target").asText() : "";
            if (target.isEmpty() || !matchesLinkType(type, linkTypes)) continue;
            if (visited.contains(target)) continue;

            JsonNode targetEvent = eventsById.get(target);
            if (targetEvent == null) continue;

            visited.add(target);
            result.add(mapper.convertValue(targetEvent, Map.class));
            collectUpstreamFlat(targetEvent, linkTypes, maxLevels - 1, maxEvents, visited, result);
        }
    }

    private void collectDownstreamFlat(JsonNode event, List<String> linkTypes, int maxLevels, int maxEvents, Set<String> visited, List<Object> result) {
        if (maxLevels <= 0 || result.size() >= maxEvents) return;
        String eventId = event.at("/meta/id").asText();

        for (Map.Entry<String, JsonNode> entry : eventsById.entrySet()) {
            if (result.size() >= maxEvents) break;
            String candidateId = entry.getKey();
            if (visited.contains(candidateId)) continue;

            JsonNode candidate = entry.getValue();
            JsonNode links = candidate.get("links");
            if (links == null || !links.isArray()) continue;

            for (JsonNode link : links) {
                String type = link.has("type") ? link.get("type").asText() : "";
                String target = link.has("target") ? link.get("target").asText() : "";
                if (eventId.equals(target) && matchesLinkType(type, linkTypes)) {
                    visited.add(candidateId);
                    result.add(mapper.convertValue(candidate, Map.class));
                    collectDownstreamFlat(candidate, linkTypes, maxLevels - 1, maxEvents, visited, result);
                    break;
                }
            }
        }
    }

    private boolean matchesLinkType(String type, List<String> allowedTypes) {
        if (allowedTypes.contains("ALL")) return true;
        return allowedTypes.contains(type);
    }
}
