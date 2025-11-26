package eu.sheepearrr.discordintegration

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.MessageData
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.*
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object DiscordIntegration : ModInitializer {
    const val MOD_ID = "discordintegration"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

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

    override fun onInitialize() {
        runBlocking {
            mainBot()
        }
        initFabricEvents()
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun mainBot() {
        if (CONFIG.token.get().trim().isEmpty()) {
            LOGGER.warn("Discord bot didn't start - The token hasn't been changed. Please change it so the bot can actually work. ")
            return
        }
        BOT = Kord(CONFIG.token.get().trim())
        BOT!!.on<MessageCreateEvent> {
            if (message.channelId.value.toLong() != CONFIG.yapChannel.get() || message.author?.isSelf == true || MESSAGE_QUEUE.contains(message.data))
                return@on
            MESSAGE_QUEUE.add(message.data)
            println("Message received on discord, ${message.data.content}")
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