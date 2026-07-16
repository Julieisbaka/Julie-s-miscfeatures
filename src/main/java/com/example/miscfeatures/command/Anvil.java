package com.example.miscfeatures.command;

import com.example.miscfeatures.config.Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Anvil {

    private Anvil() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("anvil")
                        .requires(Anvil::canUseAnvilCommand)
                        .executes(Anvil::execute)
                .then(Commands.literal("rename")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(Anvil::renameMainHandItem)))
                .then(Commands.literal("clearname")
                    .executes(Anvil::clearMainHandItemName))
        );
    }

    private static boolean canUseAnvilCommand(CommandSourceStack source) {
        Config config = Config.getInstance();
        if (!config.shouldRequirePermissionForAnvil()) {
            return true;
        }

        return hasRequiredPermission(source, config.getAnvilPermissionLevel());
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getCreativePlayerOrFail(source);
        if (player == null) {
            return 0;
        }

        Config config = Config.getInstance();

        ItemStack mainHand = player.getMainHandItem();

        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, ignored) -> new SafeCreativeAnvilMenu(
                        containerId,
                        inventory
                ),
                Component.translatable("container.repair")
        ));

        if (!mainHand.isEmpty()
                && shouldAutoInsert(config, mainHand)
                && player.containerMenu instanceof AnvilMenu anvilMenu
                && !anvilMenu.getSlot(0).hasItem()) {
            anvilMenu.getSlot(0).set(mainHand.copyWithCount(1));
            anvilMenu.broadcastChanges();
        }

        source.sendSuccess(() -> Component.literal("Opened creative anvil."), false);
        return 1;
    }

    private static int renameMainHandItem(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getCreativePlayerOrFail(source);
        if (player == null) {
            return 0;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            source.sendFailure(Component.literal("You must hold an item in your main hand to rename it."));
            return 0;
        }

        String rawName = StringArgumentType.getString(context, "name");
        String parsedName = rawName.replace('&', '§').trim();
        if (parsedName.isEmpty()) {
            source.sendFailure(Component.literal("Name cannot be empty."));
            return 0;
        }

        if (parsedName.length() > 50) {
            source.sendFailure(Component.literal("Name is too long (max 50 characters)."));
            return 0;
        }

        mainHand.set(DataComponents.CUSTOM_NAME, Component.literal(parsedName));
        source.sendSuccess(() -> Component.literal("Renamed held item to: ").append(mainHand.getHoverName()), true);
        return 1;
    }

    private static int clearMainHandItemName(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getCreativePlayerOrFail(source);
        if (player == null) {
            return 0;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            source.sendFailure(Component.literal("You must hold an item in your main hand to clear its name."));
            return 0;
        }

        if (!mainHand.has(DataComponents.CUSTOM_NAME)) {
            source.sendFailure(Component.literal("Held item does not have a custom name."));
            return 0;
        }

        mainHand.remove(DataComponents.CUSTOM_NAME);
        source.sendSuccess(() -> Component.literal("Cleared custom name from held item."), true);
        return 1;
    }

    private static ServerPlayer getCreativePlayerOrFail(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return null;
        }

        if (!player.hasInfiniteMaterials()) {
            source.sendFailure(Component.literal("You must be in creative mode to use /anvil."));
            return null;
        }

        Config config = Config.getInstance();
        if (config.shouldRequirePermissionForAnvil() && !hasRequiredPermission(source, config.getAnvilPermissionLevel())) {
            source.sendFailure(Component.literal(
                    "You do not have permission to use /anvil. Required permission level: " + config.getAnvilPermissionLevel()
            ));
            return null;
        }

        return player;
    }

    private static boolean shouldAutoInsert(Config config, ItemStack stack) {
        boolean isArmorLike = stack.has(DataComponents.EQUIPPABLE);
        boolean isWeaponOrToolLike = stack.isDamageableItem()
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT);

        if (config.shouldAnvilAutoInsertArmor() && isArmorLike) {
            return true;
        }

        if (config.shouldAnvilAutoInsertNameTags() && stack.is(Items.NAME_TAG)) {
            return true;
        }

        if (config.shouldAnvilAutoInsertWeapons()
                && isWeaponOrToolLike
                && !isArmorLike) {
            return true;
        }

        return false;
    }

    private static boolean hasRequiredPermission(CommandSourceStack source, int level) {
        return switch (Math.max(0, Math.min(4, level))) {
            case 0, 1 -> true;
            case 2 -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
            case 3 -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
            case 4 -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER);
            default -> false;
        };
    }

    private static final class SafeCreativeAnvilMenu extends AnvilMenu {

        private SafeCreativeAnvilMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory) {
            super(containerId, playerInventory);
        }

        @Override
        public void removed(Player player) {
            returnSlotToPlayerInventory(player, 0);
            returnSlotToPlayerInventory(player, 1);
            super.removed(player);
        }

        private void returnSlotToPlayerInventory(Player player, int slotIndex) {
            ItemStack stack = this.getSlot(slotIndex).getItem();
            if (stack.isEmpty()) {
                return;
            }

            this.getSlot(slotIndex).set(ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }
}
