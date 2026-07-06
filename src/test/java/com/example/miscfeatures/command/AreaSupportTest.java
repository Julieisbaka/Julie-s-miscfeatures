package com.example.miscfeatures.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AreaSupportTest {

    @Test
    void tokenizeTargets_splitsOnCommaAndWhitespace() {
        List<String> tokens = AreaSupport.tokenizeTargets(" stone, minecraft:dirt   #minecraft:logs");

        assertEquals(List.of("stone", "minecraft:dirt", "#minecraft:logs"), tokens);
    }

    @Test
    void tokenizeTargets_ignoresEmptySegments() {
        List<String> tokens = AreaSupport.tokenizeTargets("stone,,,   dirt  ");

        assertEquals(List.of("stone", "dirt"), tokens);
    }

    @Test
    void parsePositiveIntFlag_parsesValidPositiveValue() {
        Integer parsed = AreaSupport.parsePositiveIntFlag("--page=3", "--page=");

        assertEquals(3, parsed);
    }

    @Test
    void parsePositiveIntFlag_rejectsZeroOrNegativeOrInvalid() {
        assertNull(AreaSupport.parsePositiveIntFlag("--limit=0", "--limit="));
        assertNull(AreaSupport.parsePositiveIntFlag("--limit=-4", "--limit="));
        assertNull(AreaSupport.parsePositiveIntFlag("--limit=abc", "--limit="));
        assertNull(AreaSupport.parsePositiveIntFlag("--page=2", "--limit="));
    }

    @Test
    void cubeWidth_returnsExpectedValue() {
        assertEquals(1, AreaSupport.cubeWidth(0));
        assertEquals(3, AreaSupport.cubeWidth(1));
        assertEquals(11, AreaSupport.cubeWidth(5));
    }
}
