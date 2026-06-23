package com.example.miscfeatures.command;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and matches block state filters like "minecraft:stone[variant=polished,axis=y]"
 * Supports filtering by block properties.
 */
public class BlockStateFilter {
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("([a-z_]+)=([a-z0-9_]+)", Pattern.CASE_INSENSITIVE);
    private final Map<String, String> propertyFilters;

    public BlockStateFilter(String filterString) {
        this.propertyFilters = new HashMap<>();
        parseProperties(filterString);
    }

    private void parseProperties(String filterString) {
        if (!filterString.contains("[")) {
            return;
        }

        int bracketStart = filterString.indexOf('[');
        int bracketEnd = filterString.lastIndexOf(']');
        if (bracketStart < 0 || bracketEnd < 0 || bracketStart >= bracketEnd) {
            return;
        }

        String propertiesStr = filterString.substring(bracketStart + 1, bracketEnd);
        Matcher matcher = PROPERTY_PATTERN.matcher(propertiesStr);
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String propertyValue = matcher.group(2);
            propertyFilters.put(propertyName, propertyValue);
        }
    }

    /**
     * Check if a block state matches all the property filters
     */
    public boolean matches(BlockState state) {
        if (propertyFilters.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> filter : propertyFilters.entrySet()) {
            String propertyName = filter.getKey();
            String expectedValue = filter.getValue();

            Property<?> property = state.getProperties().stream()
                    .filter(p -> p.getName().equals(propertyName))
                    .findFirst()
                    .orElse(null);

            if (property == null) {
                return false;
            }

            String actualValue = state.getValue(property).toString();
            if (!actualValue.equals(expectedValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extract the block ID without the properties part
     */
    public static String extractBlockId(String input) {
        int bracketIndex = input.indexOf('[');
        if (bracketIndex < 0) {
            return input;
        }
        return input.substring(0, bracketIndex);
    }

    /**
     * Check if the input has property filters
     */
    public boolean hasFilters() {
        return !propertyFilters.isEmpty();
    }

    @Override
    public String toString() {
        if (propertyFilters.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[");
        for (var entry : propertyFilters.entrySet()) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("]");
        return sb.toString();
    }
}
