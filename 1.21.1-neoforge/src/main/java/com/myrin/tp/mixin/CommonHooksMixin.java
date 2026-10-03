package com.myrin.tp.mixin;

import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 选择器权限放行：对 forge:use_entity_selectors 权限查询恒返回可用，
 * 使生存模式无作弊环境下也能正常使用 @ 实体选择器。
 */
@Mixin(CommonHooks.class)
public abstract class CommonHooksMixin {

    @Inject(method = "canUseEntitySelectors", at = @At("HEAD"), cancellable = true, remap = false)
    private static void myrintp$allowEntitySelectors(SharedSuggestionProvider provider, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
