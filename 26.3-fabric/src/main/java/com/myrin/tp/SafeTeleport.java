package com.myrin.tp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 落点检查、随机安全落点、传送执行。
 */
public final class SafeTeleport {

    private SafeTeleport() {
    }

    /**
     * 判断某个方块位置是否适合站立：
     * 世界边界内、脚下有实心方块、站立点与头顶为空、无流体、无危险方块（液体/仙人掌/火）。
     */
    public static boolean isSafe(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinY() + 2) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockPos head = pos.above();
        BlockPos below = pos.below();
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(head).isAir()) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(head).isEmpty()) {
            return false;
        }
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir()) {
            return false;
        }
        if (!level.getFluidState(below).isEmpty()) {
            return false;
        }
        if (isDangerous(belowState) || isDangerous(level.getBlockState(pos)) || isDangerous(level.getBlockState(head))) {
            return false;
        }
        return true;
    }

    /** 从给定坐标向上修正到安全站立高度（向上最多找 10 格），找不到返回原位置。 */
    public static BlockPos adjustUp(ServerLevel level, BlockPos pos) {
        BlockPos cur = pos;
        for (int i = 0; i < 10; i++) {
            if (isSafe(level, cur)) {
                return cur;
            }
            cur = cur.above();
        }
        return pos;
    }

    /**
     * 在目标维度以给定坐标为中心随机寻找安全落点：
     * 随机 x/z（范围内且在世界边界内），从地表高度向下扫描 16 格找安全站立点。
     */
    public static BlockPos randomSafe(ServerLevel level, BlockPos center, int range, int attempts) {
        for (int i = 0; i < attempts; i++) {
            int x = center.getX() + level.getRandom().nextInt(range * 2 + 1) - range;
            int z = center.getZ() + level.getRandom().nextInt(range * 2 + 1) - range;
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) {
                continue;
            }
            // 强制加载（必要时生成）目标区块，保证高度图与方块状态可用
            level.getChunk(x >> 4, z >> 4);
            int topY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).getY();
            for (int dy = 0; dy <= 16; dy++) {
                int y = topY - dy;
                BlockPos pos = new BlockPos(x, y, z);
                if (isSafe(level, pos)) {
                    return pos;
                }
            }
        }
        // 全部尝试失败：回退到中心附近的安全位置
        BlockPos fallback = adjustUp(level, center.above(2));
        return isSafe(level, fallback) ? fallback : center;
    }

    private static boolean isDangerous(BlockState state) {
        return state.getBlock() instanceof LiquidBlock
                || state.getBlock() instanceof CactusBlock
                || state.getBlock() instanceof FireBlock;
    }

    /** 按维度 key 获取服务端世界。 */
    public static ServerLevel levelByKey(MinecraftServer server, String dimension) {
        try {
            ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(dimension));
            ServerLevel level = server.getLevel(key);
            return level != null ? level : server.overworld();
        } catch (Exception e) {
            return server.overworld();
        }
    }

    /**
     * 执行跨维度/同维度传送，并附带原版风格传送音效与粒子。
     */
    public static void teleportPlayer(ServerPlayer player, ServerLevel target, double x, double y, double z, float yaw, float pitch) {
        ServerLevel old = (ServerLevel) player.level();
        player.teleportTo(target, x, y, z, java.util.Set.of(), yaw, pitch, false);
        old.playSound(null, old.getRespawnData().pos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        old.sendParticles(ParticleTypes.PORTAL, x, y + 1, z, 32, 0.5, 0.5, 0.5, 0.2);
        target.playSound(null, new BlockPos((int) x, (int) y, (int) z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        target.sendParticles(ParticleTypes.PORTAL, x, y + 1, z, 32, 0.5, 0.5, 0.5, 0.2);
    }
}
