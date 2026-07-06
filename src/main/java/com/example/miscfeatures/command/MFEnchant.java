package com.example.miscfeatures.command;

import com.example.miscfeatures.MiscFeatures;
import com.example.miscfeatures.config.Config;
import com.example.miscfeatures.mixin.ItemEnchantmentsMutableAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MFEnchant {

        private static final Map<String, PendingUnsafeEnchant> PENDING_UNSAFE_CONFIRMATIONS = new ConcurrentHashMap<>();
        private static volatile boolean cleanupHookRegistered;

    private MFEnchant() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        registerCleanupHookIfNeeded();
        dispatcher.register(buildRootLiteral("miscfeatures", buildContext));
        dispatcher.register(buildRootLiteral("mf", buildContext));
    }

        private static void registerCleanupHookIfNeeded() {
                if (cleanupHookRegistered) {
                        return;
                }

                synchronized (MFEnchant.class) {
                        if (cleanupHookRegistered) {
                                return;
                        }

                        ServerTickEvents.END_SERVER_TICK.register(server -> cleanupExpiredConfirmations(System.currentTimeMillis()));
                        cleanupHookRegistered = true;
                }
        }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRootLiteral(String rootLiteral, CommandBuildContext buildContext) {
        return Commands.literal(rootLiteral)
                .then(Commands.literal("enchant")
                        .requires(MFEnchant::canUseEnchantCommand)
                        .then(Commands.literal("dry-run")
                                .then(buildEnchantArguments(buildContext, true)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                                        .then(Commands.argument("level", EnchantLevelArgument.enchantLevel())
                                                .executes(context -> execute(context, false))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, net.minecraft.commands.arguments.selector.EntitySelector> buildEnchantArguments(
            CommandBuildContext buildContext,
            boolean dryRun
    ) {
        return Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                        .then(Commands.argument("level", EnchantLevelArgument.enchantLevel())
                                .executes(context -> execute(context, dryRun))));
    }

        private static boolean canUseEnchantCommand(CommandSourceStack source) {
                Config config = Config.getInstance();
                if (!config.shouldRequirePermissionForEnchant()) {
                        return true;
                }

                return hasRequiredPermission(source, config.getEnchantPermissionLevel());
        }

        private static int execute(CommandContext<CommandSourceStack> context, boolean dryRun) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Config config = Config.getInstance();

        if (config.shouldRequirePermissionForEnchant() && !hasRequiredPermission(source, config.getEnchantPermissionLevel())) {
            source.sendFailure(Component.literal(
                    "You do not have permission to use /miscfeatures enchant. Required permission level: " + config.getEnchantPermissionLevel()
            ));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        Holder.Reference<Enchantment> enchantmentHolder = ResourceArgument.getEnchantment(context, "enchantment");
        Identifier enchantmentId = enchantmentHolder.key().identifier();
        int requestedLevel = EnchantLevelArgument.getLevel(context, "level");

                boolean allowNegative = config.isDeveloperMode() && config.shouldAllowNegativeEnchants();
                boolean allowHighLevel = config.isDeveloperMode() && config.shouldAllowHighLevelEnchants();

                if (requestedLevel == 0) {
                        source.sendFailure(Component.literal("Level 0 is not allowed for /miscfeatures enchant."));
                        return 0;
                }

                if (!allowNegative && requestedLevel < 0) {
            source.sendFailure(Component.literal(
                                        "Negative levels are disabled. Enable developerMode + allowNegativeEnchants to use them."
            ));
            return 0;
        }

                if (!allowHighLevel && requestedLevel > 255) {
                        source.sendFailure(Component.literal(
                                        "Enchant levels above 255 are disabled. Enable developerMode + allowHighLevelEnchants to use them."
                        ));
            return 0;
        }

        ItemStack mainHand = target.getMainHandItem();
        if (mainHand.isEmpty()) {
            source.sendFailure(Component.literal("Target has no item in main hand."));
            return 0;
        }

        int finalLevel = requestedLevel;
        boolean unsafeLevel = isUnsafeLevel(finalLevel);

        if (dryRun) {
            source.sendSuccess(() -> Component.literal(
                    "Dry-run: would apply §e" + enchantmentId + "§a level §e" + finalLevel
                            + "§a to §e" + target.getName().getString() + "§a main hand."
            ), false);

            if (unsafeLevel) {
                source.sendSuccess(() -> Component.literal(getUnsafeEnchantWarning()), false);
            }
        }

        if (unsafeLevel
                && config.shouldPreventCreativePacketCrashOnUnsafeEnchants()
                && target.hasInfiniteMaterials()) {
            source.sendFailure(Component.literal(
                    "Unsafe enchant level blocked for creative target to prevent known creative-slot packet crashes. "
                            + "Disable preventCreativePacketCrashOnUnsafeEnchants to force it anyway."
            ));
            return 0;
        }

        if (unsafeLevel
                && config.shouldRequireConfirmForUnsafeEnchants()
                && !dryRun
                && !consumeUnsafeConfirm(source, target, enchantmentId, finalLevel, config.getNegativeEnchantConfirmWindowSeconds())) {
            source.sendFailure(Component.literal(
                    "Unsafe enchant level requested. Run the exact same command again within "
                            + config.getNegativeEnchantConfirmWindowSeconds()
                            + "s to confirm."
            ));
            source.sendSuccess(() -> Component.literal(
                    getUnsafeEnchantWarning()
            ), false);
            return 0;
        }

                if (dryRun) {
                        if (unsafeLevel && config.shouldRequireConfirmForUnsafeEnchants()) {
                                source.sendSuccess(() -> Component.literal(
                                                "Dry-run note: this would require confirmation within "
                                                                + config.getNegativeEnchantConfirmWindowSeconds() + "s on real execution."
                                ), false);
                        }

                        return 1;
                }

                try {
                        if (mainHand.is(Items.ENCHANTED_BOOK)) {
                                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                                                mainHand.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
                                );
                                setExactEnchantLevel(mutable, enchantmentHolder, finalLevel);
                                mainHand.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                        } else {
                                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                                                mainHand.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                                );
                                setExactEnchantLevel(mutable, enchantmentHolder, finalLevel);
                                mainHand.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
                        }
                } catch (RuntimeException exception) {
                        source.sendFailure(Component.literal(
                                        "Failed to apply enchant level " + finalLevel + ": " + exception.getMessage()
                        ));
                        return 0;
        }

        MiscFeatures.verbose(
                "Applied unsafe enchant {} level {} to {} main hand (requested={}, allowNegative={}, allowHighLevel={})",
                enchantmentId,
                finalLevel,
                target.getName().getString(),
                requestedLevel,
                allowNegative,
                allowHighLevel
        );

        source.sendSuccess(() -> Component.literal(
                "Applied §e" + enchantmentId + "§a level §e" + finalLevel +
                        "§a to §e" + target.getName().getString() + "§a main hand."
        ), true);

        if (unsafeLevel) {
            source.sendSuccess(() -> Component.literal(
                    getUnsafeEnchantWarning()
            ), false);
        }

        return 1;
    }

        private static void setExactEnchantLevel(
                        ItemEnchantments.Mutable mutable,
                        Holder.Reference<Enchantment> enchantmentHolder,
                        int level
        ) {
                var backingMap = ((ItemEnchantmentsMutableAccessor) (Object) mutable).miscFeatures$getEnchantments();
                backingMap.put(enchantmentHolder, level);
        }

        private static boolean isUnsafeLevel(int level) {
                return level < 0 || level > 255;
        }

        private static String getUnsafeEnchantWarning() {
                return "§6Warning: unsafe enchant levels are unstable developer behavior and can crash server/client or corrupt inventory data.";
        }

        private static boolean consumeUnsafeConfirm(
                        CommandSourceStack source,
                        ServerPlayer target,
                        Identifier enchantmentId,
                        int level,
                        int confirmWindowSeconds
        ) {
                long now = System.currentTimeMillis();
                cleanupExpiredConfirmations(now);
                String actorKey = getActorKey(source);
                PendingUnsafeEnchant pending = PENDING_UNSAFE_CONFIRMATIONS.get(actorKey);
                if (pending != null && pending.matches(target.getUUID().toString(), enchantmentId.toString(), level, now)) {
                        PENDING_UNSAFE_CONFIRMATIONS.remove(actorKey);
                        return true;
                }

                PENDING_UNSAFE_CONFIRMATIONS.put(
                                actorKey,
                                new PendingUnsafeEnchant(
                                                target.getUUID().toString(),
                                                enchantmentId.toString(),
                                                level,
                                                now + (confirmWindowSeconds * 1000L)
                                )
                );
                return false;
        }

        private static void cleanupExpiredConfirmations(long nowMillis) {
                if (PENDING_UNSAFE_CONFIRMATIONS.isEmpty()) {
                        return;
                }

                PENDING_UNSAFE_CONFIRMATIONS.entrySet().removeIf(entry -> entry.getValue().isExpired(nowMillis));
        }

        private static String getActorKey(CommandSourceStack source) {
                Entity entity = source.getEntity();
                if (entity != null) {
                        return entity.getUUID().toString();
                }

                return source.getTextName();
        }

        private record PendingUnsafeEnchant(
                        String targetUuid,
                        String enchantmentId,
                        int level,
                        long expiresAtMillis
        ) {
                private boolean isExpired(long nowMillis) {
                        return nowMillis > expiresAtMillis;
                }

                private boolean matches(String otherTargetUuid, String otherEnchantmentId, int otherLevel, long nowMillis) {
                        return nowMillis <= expiresAtMillis
                                        && targetUuid.equals(otherTargetUuid)
                                        && enchantmentId.equals(otherEnchantmentId)
                                        && level == otherLevel;
                }
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
}
