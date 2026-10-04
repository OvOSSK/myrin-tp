package com.myrin.tp.mixin;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 选择器补全权限放行：非 OP 输入 @ 时仍显示实体选择器建议。 */
@Mixin(EntityArgument.class)
public class EntityArgumentMixin {

    @Redirect(
            method = "listSuggestions",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeHooks;canUseEntitySelectors(Lnet/minecraft/commands/SharedSuggestionProvider;)Z"),
            remap = false
    )
    private boolean myrintp$allowSelectorSuggestions(SharedSuggestionProvider source) {
        return true;
    }
}
