package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class NewsSourceServiceTests {
    private final ObjectMapper json=new ObjectMapper();
    private JsonNode meta(String generation,String path) throws Exception {
        return json.readTree("{\"meta\":{\"generatedAt\":\""+generation+"\"},\"articleFileByPressId\":{\"1\":\""+path+"\"}}");
    }
    @Test void resolvesOnlyKnownShardAndRejectsGenerationChange() throws Exception {
        Queue<JsonNode> responses=new ArrayDeque<>(List.of(meta("a","articles/press-1.json"),meta("a",""),meta("b","")));
        NewsSourceService source=new NewsSourceService(json) {
            @Override protected JsonNode fetch(String relative) { return responses.remove(); }
        };
        assertThatThrownBy(() -> source.read("1","a")).isInstanceOf(NewsDailyConflictException.class);
    }
    @Test void refusesExternalOrTraversalPath() throws Exception {
        JsonNode bad=meta("a","https://other.invalid/a.json");
        NewsSourceService source=new NewsSourceService(json) {
            @Override protected JsonNode fetch(String relative) {
                assertThat(relative).isEqualTo("meta.json"); return bad;
            }
        };
        assertThatThrownBy(() -> source.read("1","a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> source.read("../1","a")).isInstanceOf(IllegalArgumentException.class);
    }
}

