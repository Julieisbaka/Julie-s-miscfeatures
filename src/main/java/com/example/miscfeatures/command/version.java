package com.example.miscfeatures.command;

import org.jspecify.annotations.NonNull;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class version {

    private version() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildRootLiteral("miscfeatures"));
        dispatcher.register(buildRootLiteral("mf"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildRootLiteral(@NonNull String rootLiteral) {
        return Commands.literal(rootLiteral)
                .then(Commands.literal("version")
                        .executes(version::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        // TODO: Fetch the version from the mod metadata instead of hardcoding it here.
        source.sendSuccess(() -> Component.literal(
                "JulieISBaka Misc Features version: 0.0.5"
        ), true);
        return 1;
    }
}
