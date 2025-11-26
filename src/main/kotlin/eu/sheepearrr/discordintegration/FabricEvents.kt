package eu.sheepearrr.discordintegration

import dev.kord.common.Color
import dev.kord.common.entity.optional.Optional
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.json.request.ChannelModifyPatchRequest
import eu.sheepearrr.discordintegration.DiscordIntegration.BOT
import eu.sheepearrr.discordintegration.DiscordIntegration.CONFIG
import eu.sheepearrr.discordintegration.DiscordIntegration.COROUTINE_SCOPE
import eu.sheepearrr.discordintegration.DiscordIntegration.MESSAGE_QUEUE
import eu.sheepearrr.discordintegration.DiscordIntegration.toSnowflake
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.text.Text
import net.minecraft.util.Colors
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun initFabricEvents() {
    ServerLifecycleEvents.SERVER_STOPPED.register {
        runBlocking {
            BOT?.let { bot ->
                bot.rest.channel.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                    content = ":reconsider: The server has stopped :reconsider:"
                }
                bot.rest.channel.patchChannel(
                    CONFIG.yapChannel.get().toSnowflake(),
                    ChannelModifyPatchRequest(topic = Optional(":reconsider: Server Offline :reconsider:"))
                )
                bot.logout()
            }
        }
    }
    ServerLifecycleEvents.SERVER_STARTED.register {
        COROUTINE_SCOPE = CoroutineScope(SupervisorJob() + it.asCoroutineDispatcher())
        COROUTINE_SCOPE.launch {
            BOT?.rest?.channel?.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                content = ":fooftrue: The server has started :fooftrue:"
            }
        }
    }
    ServerPlayerEvents.JOIN.register {
        val embed = EmbedBuilder()
        embed.title = "Joined the game"
        embed.color = Color(0x55FF55)
        embed.author {
            name = it.name.string
            icon = "https://api.mineatar.io/face/${it.uuid}"
        }
        embed.timestamp = Clock.System.now()
        COROUTINE_SCOPE.launch {
            BOT?.rest?.channel?.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                embeds = mutableListOf(embed)
            }
        }
    }
    ServerPlayerEvents.LEAVE.register {
        val embed = EmbedBuilder()
        embed.title = "Left the game"
        embed.color = Color(0xFF5555)
        embed.author {
            name = it.name.string
            icon = "https://api.mineatar.io/face/${it.uuid}"
        }
        embed.timestamp = Clock.System.now()
        COROUTINE_SCOPE.launch {
            BOT?.rest?.channel?.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                embeds = mutableListOf(embed)
            }
        }
    }
    ServerTickEvents.END_SERVER_TICK.register { server ->
        val message = MESSAGE_QUEUE.firstOrNull()
        if (message != null) {
            MESSAGE_QUEUE.remove(message)
            COROUTINE_SCOPE.launch {
                val sender = BOT?.rest?.guild?.getGuildMember(message.guildId.value!!, message.author.id)
                val color = BOT?.rest?.guild?.getGuildRoles(message.guildId.value!!)?.first { it.id == sender?.roles[0] }?.color ?: Colors.WHITE
                server.playerManager.broadcast(
                    Text.empty()
                        .append(Text.literal("<"))
                        .append(Text.literal(sender?.nick?.value ?: message.author.globalName.value).withColor(color))
                        .append(Text.literal("> ${message.content}")),
                    false
                )
            }
        }
        if (server.ticks % 6000 == 0) {
            COROUTINE_SCOPE.launch {
                BOT?.rest?.channel?.patchChannel(
                    CONFIG.yapChannel.get().toSnowflake(),
                    ChannelModifyPatchRequest(topic = Optional("Currently Online: ${server.currentPlayerCount}/${server.maxPlayerCount}"))
                )
            }
        }
    }
}