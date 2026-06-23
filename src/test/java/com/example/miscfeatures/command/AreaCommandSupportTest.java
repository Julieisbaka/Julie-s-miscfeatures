package com.example.miscfeatures.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AreaCommandSupportTest {

    @Test
    void tokenizeTargets_splitsOnCommaAndWhitespace() {
        List<String> tokens = AreaCommandSupport.tokenizeTargets(" stone, minecraft:dirt   #minecraft:logs");

        assertEquals(List.of("stone", "minecraft:dirt", "#minecraft:logs"), tokens);
    }

    @Test
    void tokenizeTargets_ignoresEmptySegments() {
        List<String> tokens = AreaCommandSupport.tokenizeTargets("stone,,,   dirt  ");

        assertEquals(List.of("stone", "dirt"), tokens);
    }

    @Test
    void parsePositiveIntFlag_parsesValidPositiveValue() {
        Integer parsed = AreaCommandSupport.parsePositiveIntFlag("--page=3", "--page=");

        assertEquals(3, parsed);
    }

    @Test
    void parsePositiveIntFlag_rejectsZeroOrNegativeOrInvalid() {
        assertNull(AreaCommandSupport.parsePositiveIntFlag("--limit=0", "--limit="));
        assertNull(AreaCommandSupport.parsePositiveIntFlag("--limit=-4", "--limit="));
        assertNull(AreaCommandSupport.parsePositiveIntFlag("--limit=abc", "--limit="));
        assertNull(AreaCommandSupport.parsePositiveIntFlag("--page=2", "--limit="));
    }

    @Test
    void cubeWidth_returnsExpectedValue() {
        assertEquals(1, AreaCommandSupport.cubeWidth(0));
        assertEquals(3, AreaCommandSupport.cubeWidth(1));
        assertEquals(11, AreaCommandSupport.cubeWidth(5));
    }
}
