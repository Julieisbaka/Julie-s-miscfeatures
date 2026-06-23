package com.example.miscfeatures.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnchantLevelArgumentTest {

    @Test
    void parse_acceptsNormalPositiveLevel() throws CommandSyntaxException {
        EnchantLevelArgument argument = EnchantLevelArgument.enchantLevel(false, false);

        int parsed = argument.parse(new StringReader("10"));

        assertEquals(10, parsed);
    }

    @Test
    void parse_rejectsZero() {
        EnchantLevelArgument argument = EnchantLevelArgument.enchantLevel(false, false);

        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("0")));
    }

    @Test
    void parse_rejectsNegativeWhenDisabled() {
        EnchantLevelArgument argument = EnchantLevelArgument.enchantLevel(false, false);

        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("-1")));
    }

    @Test
    void parse_acceptsNegativeWhenEnabled() throws CommandSyntaxException {
        EnchantLevelArgument argument = EnchantLevelArgument.enchantLevel(true, false);

        int parsed = argument.parse(new StringReader("-1"));

        assertEquals(-1, parsed);
    }

    @Test
    void parse_rejectsHighLevelWhenDisabled() {
        EnchantLevelArgument argument = EnchantLevelArgument.enchantLevel(false, false);

        assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader("256")));
    }

    @Test
    void parse_acceptsHighLevelWhenEnabled() throws CommandSyntaxException {
        EnchantLevelArgument argument = EnchantLevelArgument.enchantLevel(false, true);

        int parsed = argument.parse(new StringReader("256"));

        assertEquals(256, parsed);
    }
}
