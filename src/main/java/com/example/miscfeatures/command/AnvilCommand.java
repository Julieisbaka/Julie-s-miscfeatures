package com.example.miscfeatures.command;

import com.example.miscfeatures.config.MiscFeaturesConfig;
import com.mojang.brigadier.CommandDispatcher;
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

public final class AnvilCommand {

    private AnvilCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("anvil")
                        .requires(AnvilCommand::canUseAnvilCommand)
                        .executes(AnvilCommand::execute)
        );
    }

    private static boolean canUseAnvilCommand(CommandSourceStack source) {
        MiscFeaturesConfig config = MiscFeaturesConfig.getInstance();
        if (!config.shouldRequirePermissionForAnvil()) {
            return true;
        }

        return hasRequiredPermission(source, config.getAnvilPermissionLevel());
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

        if (!player.hasInfiniteMaterials()) {
            source.sendFailure(Component.literal("You must be in creative mode to use /anvil."));
            return 0;
        }

        MiscFeaturesConfig config = MiscFeaturesConfig.getInstance();

        if (config.shouldRequirePermissionForAnvil() && !hasRequiredPermission(source, config.getAnvilPermissionLevel())) {
            source.sendFailure(Component.literal(
                "You do not have permission to use /anvil. Required permission level: " + config.getAnvilPermissionLevel()
            ));
            return 0;
        }

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

    private static boolean shouldAutoInsert(MiscFeaturesConfig config, ItemStack stack) {
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
