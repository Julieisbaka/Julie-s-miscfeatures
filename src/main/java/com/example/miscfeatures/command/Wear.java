package com.example.miscfeatures.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class Wear {

    private Wear() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("wear")
                        .then(Commands.argument("slot", StringArgumentType.word())
                                .suggests(Wear::suggestSlots)
                                .executes(Wear::execute))
        );
    }

    private static CompletableFuture<Suggestions> suggestSlots(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(List.of("head", "chest", "legs", "feet"), builder);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        String rawSlot = StringArgumentType.getString(context, "slot");
        EquipmentSlot slot = parseArmorSlot(rawSlot);
        if (slot == null) {
            source.sendFailure(Component.literal("Invalid armor slot: " + rawSlot + ". Use head, chest, legs, or feet."));
            return 0;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            source.sendFailure(Component.literal("You must hold an item in your main hand."));
            return 0;
        }

        ItemStack equipStack = mainHand.copyWithCount(1);
        ItemStack previous = player.getItemBySlot(slot);

        player.setItemSlot(slot, equipStack);

        if (player.hasInfiniteMaterials()) {
            // Creative: keep original stack unchanged in hand
        } else {
            mainHand.shrink(1);
        }

        if (!previous.isEmpty()) {
            if (!player.getInventory().add(previous)) {
                player.drop(previous, false);
            }
        }

        source.sendSuccess(() -> Component.literal(
                "Wore " + equipStack.getHoverName().getString() + " in " + slot.getName() + " slot."
        ), true);
        return 1;
    }

    private static EquipmentSlot parseArmorSlot(String rawSlot) {
        return switch (rawSlot.toLowerCase(Locale.ROOT)) {
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> null;
        };
    }
}
