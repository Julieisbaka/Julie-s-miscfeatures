package com.example.miscfeatures.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStateFilterTest {

    @Test
    void extractBlockId_returnsWholeInputWhenNoProperties() {
        assertEquals("minecraft:stone", BlockStateFilter.extractBlockId("minecraft:stone"));
    }

    @Test
    void extractBlockId_stripsPropertySuffix() {
        assertEquals("minecraft:oak_log", BlockStateFilter.extractBlockId("minecraft:oak_log[axis=y]"));
    }

    @Test
    void hasFilters_detectsPropertySection() {
        assertTrue(new BlockStateFilter("minecraft:oak_log[axis=y]").hasFilters());
        assertFalse(new BlockStateFilter("minecraft:oak_log").hasFilters());
    }
}
