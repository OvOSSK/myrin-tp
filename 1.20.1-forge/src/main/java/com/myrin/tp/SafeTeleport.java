package com.myrin.tp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 落点检查、随机安全落点、传送执行。
 */
public final class SafeTeleport {

    private SafeTeleport() {
    }

    /**
     * 判断某个方块位置是否适合站立（不淹水、不踩岩浆、脚下有实心方块、头顶不窒息）。
     */
    public static boolean isSafe(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() + 2) {
            return false;
        }
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        BlockPos below = pos.below();
        if (level.getBlockState(below).isAir()) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(below).isEmpty()) {
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
     * 在目标维度以出生点为中心随机寻找安全落点。
     */
    public static BlockPos randomSafe(ServerLevel level, int range, int attempts) {
        BlockPos spawn = level.getSharedSpawnPos();
        for (int i = 0; i < attempts; i++) {
            int x = spawn.getX() + level.getRandom().nextInt(range * 2 + 1) - range;
            int z = spawn.getZ() + level.getRandom().nextInt(range * 2 + 1) - range;
            int y = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).getY() + 1;
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafe(level, pos)) {
                return pos;
            }
        }
        // 尝试失败，回退到出生点上方
        BlockPos fallback = spawn.above(2);
        return isSafe(level, fallback) ? fallback : spawn;
    }

    /** 按维度 key 获取服务端世界。 */
    public static ServerLevel levelByKey(net.minecraft.server.MinecraftServer server, String dimension) {
        try {
            ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimension));
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
        player.teleportTo(target, x, y, z, yaw, pitch);
        old.playSound(null, old.getSharedSpawnPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        old.sendParticles(ParticleTypes.PORTAL, x, y + 1, z, 32, 0.5, 0.5, 0.5, 0.2);
        target.playSound(null, new BlockPos((int) x, (int) y, (int) z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        target.sendParticles(ParticleTypes.PORTAL, x, y + 1, z, 32, 0.5, 0.5, 0.5, 0.2);
    }
}
