package eu.sheepearrr.discordintegration

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.cache.data.MessageData
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.json.request.ChannelModifyPatchRequest
import kotlinx.coroutines.*
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object DiscordIntegration : ModInitializer {
    const val MOD_ID = "discordintegration"

    @JvmField
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    @JvmField
    val CONFIG = ConfigApi.registerAndLoadConfig(::DiscordIntegrationConfig)

    @JvmStatic
    var BOT: Kord? = null
    var MESSAGE_QUEUE: MutableList<MessageData> = mutableListOf()
    lateinit var COROUTINE_SCOPE: CoroutineScope

    @JvmStatic
    fun id(path: String): Identifier = Identifier.of(MOD_ID, path)

    fun Long.toSnowflake(): Snowflake = Snowflake(this)

    @JvmStatic
    fun handleMessage(message: String) {
        COROUTINE_SCOPE.launch {
            BOT?.rest?.channel?.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                content = message
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun onInitialize() {
        runBlocking {
            mainBot()
        }
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
            COROUTINE_SCOPE.launch {
                for (message in MESSAGE_QUEUE) {
                    val sender = BOT?.rest?.guild?.getGuildMember(message.guildId.value!!, message.author.id)
                    server.playerManager.broadcast(
                        Text.literal("Á${sender?.nick}: ${message.content}"),
                        false
                    )
                }
                MESSAGE_QUEUE.clear()
                if (server.ticks % 6000 == 0) {
                    BOT?.rest?.channel?.patchChannel(
                        CONFIG.yapChannel.get().toSnowflake(),
                        ChannelModifyPatchRequest(topic = Optional("Currently Online: ${server.currentPlayerCount}/${server.maxPlayerCount}"))
                    )
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun mainBot() {
        if (CONFIG.token.get().trim().isEmpty()) {
            LOGGER.warn("Discord bot didn't start - The token hasn't been changed. Please change it so the bot can actually work. ")
            return
        }
        BOT = Kord(CONFIG.token.get().trim())
        BOT!!.on<MessageCreateEvent> {
            if (message.channelId.value.toLong() != CONFIG.yapChannel.get() || message.author?.isSelf == true)
                return@on
            MESSAGE_QUEUE.add(message.data)
        }
        GlobalScope.launch {
            BOT!!.login {
                intents += Intent.GuildMessages
                intents += Intent.DirectMessages
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        }
    }
}