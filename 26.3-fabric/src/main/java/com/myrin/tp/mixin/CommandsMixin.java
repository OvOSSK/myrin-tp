package com.myrin.tp.mixin;

import com.mojang.brigadier.ParseResults;
import com.myrin.tp.CommandBlocker;
import com.myrin.tp.MyrinTPMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 全局命令执行点拦截：命令真正执行前按管控模式判定，
 * 被禁指令取消执行并红字提示，指令补全不受影响。
 */
@Mixin(Commands.class)
public class CommandsMixin {

    @Inject(method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true)
    private void myrintp$block(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
        if (CommandBlocker.shouldCancel(parseResults, MyrinTPMod.GUARD)) {
            ci.cancel();
        }
    }
}
