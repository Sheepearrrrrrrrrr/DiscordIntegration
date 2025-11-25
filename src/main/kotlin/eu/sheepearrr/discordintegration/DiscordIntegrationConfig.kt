package eu.sheepearrr.discordintegration

import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedLong

class DiscordIntegrationConfig : Config(DiscordIntegration.id(DiscordIntegration.MOD_ID)) {
    var token = ValidatedString(" ")
    var yapChannel = ValidatedLong(0)
}