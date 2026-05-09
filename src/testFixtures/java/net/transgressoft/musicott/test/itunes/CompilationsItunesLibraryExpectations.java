package net.transgressoft.musicott.test.itunes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Expected counts and search sentinels for the generated Compilations iTunes fixture.
 */
public record CompilationsItunesLibraryExpectations(
        int trackCount,
        int playlistCount,
        List<FilterExpectation> allTracksFilters,
        List<FilterExpectation> artistsFilters) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static CompilationsItunesLibraryExpectations load() {
        try (InputStream input = CompilationsItunesLibraryExpectations.class
                .getResourceAsStream("/itunes/compilations-library-expectations.json")) {
            if (input == null) {
                throw new IllegalStateException("Could not locate /itunes/compilations-library-expectations.json");
            }
            JsonNode root = OBJECT_MAPPER.readTree(input);
            return new CompilationsItunesLibraryExpectations(
                    root.path("trackCount").asInt(),
                    root.path("playlistCount").asInt(),
                    readFilters(root.path("allTracksFilters")),
                    readFilters(root.path("artistsFilters")));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read Compilations iTunes expectations", ex);
        }
    }

    public FilterExpectation allTracksFilter(String name) {
        return filter(allTracksFilters, name);
    }

    public FilterExpectation artistsFilter(String name) {
        return filter(artistsFilters, name);
    }

    private static FilterExpectation filter(List<FilterExpectation> filters, String name) {
        return filters.stream()
                .filter(filter -> Objects.equals(filter.name(), name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No filter expectation named " + name));
    }

    private static List<FilterExpectation> readFilters(JsonNode filtersNode) {
        List<FilterExpectation> filters = new ArrayList<>();
        for (JsonNode filterNode : filtersNode) {
            filters.add(new FilterExpectation(
                    filterNode.path("name").asText(),
                    filterNode.path("query").asText(),
                    filterNode.path("expectedCount").asInt()));
        }
        return List.copyOf(filters);
    }

    /**
     * One search query and its expected visible result count.
     */
    public record FilterExpectation(String name, String query, int expectedCount) {
    }
}
