package com.myrin.tp.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 选择器执行期权限检查放行（1.20.1 srg 名 m_121168_）。
 * 原实现：使用选择器且权限 < 2 时抛「不能使用选择器」；此处直接跳过检查。
 */
@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin {

    @Inject(method = "m_121168_", at = @At("HEAD"), cancellable = true, remap = false)
    private void myrintp$allowSelectors(CommandSourceStack src, CallbackInfo ci) {
        ci.cancel();
    }
}
