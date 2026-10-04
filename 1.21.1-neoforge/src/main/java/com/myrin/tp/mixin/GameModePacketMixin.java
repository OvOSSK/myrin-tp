package com.myrin.tp.mixin;

import com.myrin.tp.CommandBlocker;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class GameModePacketMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChangeGameMode", at = @At("HEAD"), cancellable = true)
    private void myrintp$blockChangeGameMode(ServerboundChangeGameModePacket packet, CallbackInfo ci) {
        if (CommandBlocker.shouldBlockGameModeSwitch(this.player)) {
            this.player.sendFailure(Component.literal("该指令已被管理员禁止！仅允许使用 TP 类指令"));
            ci.cancel();
        }
    }
}
