package eu.sheepearrr.discordintegration

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.MessageData
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.nullCast
import net.fabricmc.api.ModInitializer
import net.minecraft.server.PlayerManager
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.TranslatableTextContent
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
    var PLAYER_MANAGER: PlayerManager? = null

    @JvmStatic
    fun id(path: String): Identifier = Identifier.of(MOD_ID, path)

    fun Long.toSnowflake(): Snowflake = Snowflake(this)

    @JvmStatic
    fun handleMessage(message: String) {
        COROUTINE_SCOPE.launch {
            BOT?.rest?.channel?.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                content = theGassyLighty(message)
            }
        }
    }

    @JvmStatic
    fun theGassyLighty(valueArg: String): String {
        var value = valueArg
        if (CONFIG.squeakingAround.get()) {
            value = value.replace("peak", "squeak", ignoreCase = true)
        }
        if (CONFIG.doTheEnderFunny.get()) {
            val random = "öüóú\$űőéáí%"
            value = value
                .replace("yes", "n${random}o", ignoreCase = true)
                .replace("false", "tr${random}ue", ignoreCase = true)
                .replace("lie", "tr${random}uth", ignoreCase = true)
                .replace("fake", "re${random}al", ignoreCase = true)
                .replace("no", "y${random}es", ignoreCase = true)
                .replace("true", "fa${random}lse")
                .replace("truth", "l${random}ie", ignoreCase = true)
                .replace("real", "fa${random}ke")
                .replace(random, "")
        }
        if (CONFIG.foodTheFoof.get()) {
            value = value.replace("foof", "food", ignoreCase = true)
        }
        return value
    }

    @JvmStatic
    fun handleCustomEmbeds(translatable: TranslatableTextContent, args: Array<Any>, mcText: Text): Boolean {
        translatable.key.let {
            if (it.startsWith("multiplayer.player") || it == "multiplayer.player.left")
                return true
            else if (it.startsWith("death.")) {
                /* DO THE FUNNY DEATH EMBED */
                val embed = EmbedBuilder()
                val player = PLAYER_MANAGER?.getPlayer((args[0] as Text).string)
                player?.let { player ->
                    embed.author {
                        name = mcText.string //player.name.string
                        icon = "https://api.mineatar.io/face/${player.uuid}"
                    }
                }
                embed.color = Color(0x8C1D25)
                COROUTINE_SCOPE.launch {
                    BOT?.rest?.channel?.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                        embeds = mutableListOf(embed)
                    }
                }
                return true
            } else if (it.startsWith("chat.type.advancement.")) {
                val player = PLAYER_MANAGER?.getPlayer((args[0] as Text).string)
                val embed = EmbedBuilder()
                embed.author {
                    name = player?.name?.string
                    icon = "https://api.mineatar.io/face/${player?.uuid}"
                }
                embed.title = mcText.string
                embed.color = Color(
                    when (it.substring(22)) {
                        "task" -> 0x55FF55
                        "goal" -> 16755200
                        "challenge" -> 0xAA00AA
                        else -> 0xFFFFFF
                    }
                )

                val textObj = args[1].nullCast<Text>()?.content?.nullCast<TranslatableTextContent>()?.args[0]
                if (textObj is Text) {
                    embed.footer {
                        text = textObj.style?.hoverEvent
                            ?.getValue(HoverEvent.Action.SHOW_TEXT)?.siblings?.get(1)?.string ?: "..."
                    }
                }

                COROUTINE_SCOPE.launch {
                    BOT?.rest?.channel?.createMessage(CONFIG.yapChannel.get().toSnowflake()) {
                        embeds = mutableListOf(embed)
                    }
                }
                return true
            }
        }
        return false
    }

    override fun onInitialize() {
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
            if (message.channelId.value.toLong() != CONFIG.yapChannel.get()
                || message.author?.isSelf == true || MESSAGE_QUEUE.contains(message.data)
            ) return@on
            MESSAGE_QUEUE.add(message.data)
        }
        COROUTINE_SCOPE.launch {
            LOGGER.info("Starting Discord bot.")
            BOT!!.login {
                intents += Intent.GuildMessages
                intents += Intent.DirectMessages
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        }
    }
}