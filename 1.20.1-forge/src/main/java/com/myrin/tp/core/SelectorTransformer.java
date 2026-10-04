package com.myrin.tp.core;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Set;

/**
 * 选择器权限放行（Forge 1.20.1 无内置 Mixin，采用 modlauncher transformer）。
 * 执行检查 EntitySelector.m_121168_ 清空直接放行；
 * 补全检查 EntityArgument.listSuggestions 中 ForgeHooks.canUseEntitySelectors 调用强制返回 true。
 */
public class SelectorTransformer implements ITransformer<ClassNode> {

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        for (MethodNode m : input.methods) {
            if ("m_121168_".equals(m.name)) {
                m.instructions.clear();
                m.instructions.add(new InsnNode(Opcodes.RETURN));
            } else if ("listSuggestions".equals(m.name)) {
                for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode min
                            && "net/minecraftforge/common/ForgeHooks".equals(min.owner)
                            && "canUseEntitySelectors".equals(min.name)) {
                        m.instructions.insertBefore(min, new InsnNode(Opcodes.POP));
                        m.instructions.insertBefore(min, new InsnNode(Opcodes.ICONST_1));
                        m.instructions.remove(min);
                        break;
                    }
                }
            }
        }
        return input;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<Target> targets() {
        return Set.of(
                Target.targetClass("net.minecraft.commands.arguments.selector.EntitySelector"),
                Target.targetClass("net.minecraft.commands.arguments.EntityArgument")
        );
    }
}
