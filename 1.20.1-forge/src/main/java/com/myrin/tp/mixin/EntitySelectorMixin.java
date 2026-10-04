package com.myrin.tp.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 选择器执行权限放行：非 OP 也可使用 @实体选择器（1.20.1 运行时方法名为 srg m_121168_）。 */
@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    @Inject(method = "m_121168_", at = @At("HEAD"), cancellable = true, remap = false)
    private void myrintp$allowSelectorUse(CommandSourceStack source, CallbackInfo ci) {
        ci.cancel();
    }
}
