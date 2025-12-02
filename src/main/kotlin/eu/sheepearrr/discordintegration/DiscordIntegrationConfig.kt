package eu.sheepearrr.discordintegration

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedLong

class DiscordIntegrationConfig : Config(DiscordIntegration.id(DiscordIntegration.MOD_ID)) {
    @Comment("This is the token of the bot.")
    var token = ValidatedString(" ")
    @Comment("This is the channel that the bot will talk in, receive messages from and stuff.")
    var yapChannel = ValidatedLong(0)
    @Comment("Do the funny thing that Aug asked for (Replace \"peak\" with \"squeak\".)")
    var squeakingAround = ValidatedBoolean(true)
    @Comment("It does the gassy lighty, the ender thingy.")
    var doTheEnderFunny = ValidatedBoolean(true)
    @Comment("HMMM . . . Yummy! Some roasted Foof.")
    var foodTheFoof = ValidatedBoolean(true)

    @Comment("Updates the channels topic to reflect server status")
    var updateTopic = true
}