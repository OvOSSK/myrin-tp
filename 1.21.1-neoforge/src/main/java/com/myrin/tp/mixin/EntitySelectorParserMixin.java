package com.myrin.tp.mixin;

import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.1 的实体选择器检查点在 EntitySelectorParser.allowSelectors（仅校验 OP 权限）。
 * 此处恒返回可用，使生存无作弊环境下也能正常解析 @ 实体选择器。
 */
@Mixin(EntitySelectorParser.class)
public abstract class EntitySelectorParserMixin {

    @Inject(method = "allowSelectors", at = @At("HEAD"), cancellable = true, remap = false)
    private static void myrintp$allowSelectors(Object source, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
