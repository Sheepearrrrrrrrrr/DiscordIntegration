package eu.sheepearrr.discordintegration.mixin;

import eu.sheepearrr.discordintegration.DiscordIntegration;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Inject(
            method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Z)V",
            at = @At("HEAD")
    )
    private void discordIntegration$sendToChannels(Text message, Function<ServerPlayerEntity, Text> playerMessageFactory, boolean overlay, CallbackInfo ci) {
        if (message.getContent() instanceof TranslatableTextContent translatable && DiscordIntegration.handleCustomEmbeds(translatable, translatable.getArgs(), message)) {
            return;
        }
        DiscordIntegration.handleMessage(message.getString());
    }

    @Inject(
            method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At("TAIL")
    )
    private void discordIntegration$sendChatMessageToChannels(SignedMessage message, Predicate<ServerPlayerEntity> shouldSendFiltered, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci) {
        DiscordIntegration.handleMessage(sender.getName().getString() + ": " + message.getContent().getString());
    }
}
