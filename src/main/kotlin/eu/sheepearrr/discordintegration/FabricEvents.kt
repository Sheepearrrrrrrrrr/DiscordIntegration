package eu.sheepearrr.discordintegration

import dev.kord.common.Color
import dev.kord.common.entity.DiscordRole
import dev.kord.common.entity.PresenceStatus
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
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun initFabricEvents() {
    ServerLifecycleEvents.SERVER_STOPPED.register { server ->
        COROUTINE_SCOPE.launch {
            BOT?.let { bot ->
                bot.rest.channel.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                    content =
                        "<:reconsider:1437046622788911194> The server has stopped <:reconsider:1437046622788911194>"
                }
                if (CONFIG.updateTopic) bot.rest.channel.patchChannel(
                    CONFIG.yapChannel.get().toSnowflake(),
                    ChannelModifyPatchRequest(topic = Optional("<:reconsider:1437046622788911194> Server Offline <:reconsider:1437046622788911194>"))
                )
                bot.shutdown()
            }
        }
        COROUTINE_SCOPE.launch {
            delay(1500)
            exitProcess(0)
        }
    }
    ServerLifecycleEvents.SERVER_STARTED.register {
        COROUTINE_SCOPE = CoroutineScope(SupervisorJob() + it.asCoroutineDispatcher())
        runBlocking {
            DiscordIntegration.mainBot()
        }
        DiscordIntegration.PLAYER_MANAGER = it.playerManager
        COROUTINE_SCOPE.launch {
            BOT?.rest?.channel?.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                content = "<:fooftrue:1435036912778874930> The server has started <:fooftrue:1435036912778874930>"
            }
            if (CONFIG.updateTopic) BOT?.rest?.channel?.patchChannel(
                CONFIG.yapChannel.get().toSnowflake(),
                ChannelModifyPatchRequest(topic = Optional("<:fooftrue:1435036912778874930> Server Online <:fooftrue:1435036912778874930>"))
            )
        }
    }
    ServerPlayerEvents.JOIN.register {
        val embed = EmbedBuilder()
        embed.color = Color(0x55FF55)
        embed.author {
            name = it.name.string + " joined the game"
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
        embed.color = Color(0xFF5555)
        embed.author {
            name = it.name.string + " left the game"
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
                var currentHighest: DiscordRole? = null
                BOT?.rest?.guild?.getGuildRoles(message.guildId.value!!)?.let { roles ->
                    for (role in roles.filter { role -> sender?.roles?.contains(role.id) == true && role.color != 0 }) {
                        currentHighest?.position?.let {
                            if (role.position > it)
                                currentHighest = role
                            continue
                        }
                        currentHighest = role
                    }
                }
                var mcMessage = Text.empty()
                    .append(Text.literal("<"))
                    .append(Text.literal(sender?.nick?.value ?: message.author.globalName.value).withColor(currentHighest?.color ?: Colors.WHITE))
                    .append(Text.literal("> ${DiscordIntegration.theGassyLighty(message.content)}"))
                if (message.attachments.isNotEmpty()) {
                    mcMessage = mcMessage.append(Text.literal("\n    <Posted an attachment>").withColor(0x55FFFF))
                }
                server.sendMessage(mcMessage)
                for (player in server.playerManager.playerList) {
                    player.sendMessageToClient(mcMessage, false)
                }
            }
        }
        if (server.ticks % 600 == 0) {
            COROUTINE_SCOPE.launch {
                BOT?.editPresence {
                    status = PresenceStatus.Online
                    state = "Currently Online: ${server.currentPlayerCount}/${server.maxPlayerCount}"
                }
            }
        }
    }
}