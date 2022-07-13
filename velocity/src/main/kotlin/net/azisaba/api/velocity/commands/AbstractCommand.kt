package net.azisaba.api.velocity.commands

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource

abstract class AbstractCommand {
    companion object {
        @JvmStatic
        fun literal(name: String): LiteralArgumentBuilder<CommandSource> =
            LiteralArgumentBuilder.literal(name)

        @JvmStatic
        fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSource, T> =
            RequiredArgumentBuilder.argument(name, type)
    }

    protected abstract fun createBuilder(): LiteralArgumentBuilder<CommandSource>

    fun createCommand() = BrigadierCommand(createBuilder())
}
