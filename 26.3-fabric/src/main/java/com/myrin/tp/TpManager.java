package com.myrin.tp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 传送逻辑：请求、冷却、家点、随机传送。
 */
public final class TpManager {

    private static final class TpRequest {
        final UUID from;
        final UUID to;
        final boolean here; // true = tpahere
        final long created;

        TpRequest(UUID from, UUID to, boolean here) {
            this.from = from;
            this.to = to;
            this.here = here;
            this.created = System.currentTimeMillis();
        }
    }

    private final MinecraftServer server;
    private final Config config;
    private final DataStore data;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "myrintp-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final Map<UUID, List<TpRequest>> incoming = new HashMap<>();
    private final Map<UUID, List<TpRequest>> outgoing = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldownReadyAt = new HashMap<>();

    public TpManager(MinecraftServer server, Config config, DataStore data) {
        this.server = server;
        this.config = config;
        this.data = data;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }


    private static void ok(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.GREEN), false);
    }

    private static void err(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED), false);
    }

    private static void info(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.YELLOW), false);
    }


    private void setCooldown(ServerPlayer p, String key, int seconds) {
        cooldownReadyAt.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(key, System.currentTimeMillis() + seconds * 1000L);
    }

    private long remainingCooldown(ServerPlayer p, String key, int configuredSeconds) {
        Long ready = cooldownReadyAt.getOrDefault(p.getUUID(), Map.of()).get(key);
        if (ready == null) {
            return 0;
        }
        long remain = ready - System.currentTimeMillis();
        if (remain <= 0) {
            return 0;
        }
        // 配置改短：剩余超过新配置时长时视为已冷却，下次使用按新配置重新计时
        if (configuredSeconds >= 0 && remain > configuredSeconds * 1000L) {
            return 0;
        }
        return remain;
    }

    private boolean cooldownCheck(ServerPlayer p, String key, int seconds) {
        long remain = remainingCooldown(p, key, seconds);
        if (remain > 0) {
            err(p, "操作冷却中，剩余 " + (remain / 1000 + 1) + " 秒。");
            return false;
        }
        setCooldown(p, key, seconds);
        return true;
    }


    public int tpa(ServerPlayer src, ServerPlayer target, boolean here) {
        if (src == target) {
            err(src, "不能向自己发送传送请求。");
            return 1;
        }
        if (!cooldownCheck(src, "tpa", config.tpaRequestCooldownSeconds)) {
            return 1;
        }
        List<TpRequest> out = outgoing.computeIfAbsent(src.getUUID(), k -> new ArrayList<>());
        for (TpRequest r : out) {
            if (r.to.equals(target.getUUID())) {
                err(src, "你已向 " + target.getName().getString() + " 发送过请求，请等待回复。");
                return 1;
            }
        }
        TpRequest req = new TpRequest(src.getUUID(), target.getUUID(), here);
        outgoing.computeIfAbsent(src.getUUID(), k -> new ArrayList<>()).add(req);
        incoming.computeIfAbsent(target.getUUID(), k -> new ArrayList<>()).add(req);
        ok(src, "已向 " + target.getName().getString() + " 发送传送请求。");
        info(target, src.getName().getString() + " 请求" + (here ? "你传送到他身边" : "传送到你身边") + "，使用 /tpyes 接受，/tpno 拒绝。");
        return 1;
    }

    public int tpAccept(ServerPlayer player, ServerPlayer from) {
        List<TpRequest> list = incoming.getOrDefault(player.getUUID(), new ArrayList<>());
        TpRequest pick = null;
        if (from != null) {
            for (TpRequest r : list) {
                if (r.from.equals(from.getUUID())) {
                    pick = r;
                    break;
                }
            }
            if (pick == null) {
                err(player, from.getName().getString() + " 没有向你发送过传送请求。");
                return 1;
            }
        } else if (!list.isEmpty()) {
            pick = list.get(list.size() - 1);
        } else {
            err(player, "当前没有待处理的传送请求。");
            return 1;
        }
        if (System.currentTimeMillis() - pick.created > config.tpaRequestTimeoutSeconds * 1000L) {
            removeRequest(pick);
            err(player, "该传送请求已过期。");
            return 1;
        }
        ServerPlayer requester = server.getPlayerList().getPlayer(pick.from);
        if (requester == null) {
            removeRequest(pick);
            err(player, "请求方已离线，请求已取消。");
            return 1;
        }
        removeRequest(pick);
        if (pick.here) {
            // tpahere：接受者传送到请求者身边
            teleportWithDelay(player, (ServerLevel) requester.level(), requester.getX(), requester.getY(), requester.getZ(), requester.getYRot(), requester.getXRot());
            ok(player, "正在传送到 " + requester.getName().getString() + " 身边……");
        } else {
            // tpa：请求者传送到接受者身边
            teleportWithDelay(requester, (ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            ok(requester, player.getName().getString() + " 已接受你的传送请求，正在传送……");
            ok(player, "已接受 " + requester.getName().getString() + " 的传送请求。");
        }
        return 1;
    }

    public int tpDeny(ServerPlayer player, ServerPlayer from) {
        List<TpRequest> list = incoming.getOrDefault(player.getUUID(), new ArrayList<>());
        TpRequest pick = null;
        if (from != null) {
            for (TpRequest r : list) {
                if (r.from.equals(from.getUUID())) {
                    pick = r;
                    break;
                }
            }
        } else if (!list.isEmpty()) {
            pick = list.get(list.size() - 1);
        }
        if (pick == null) {
            err(player, "当前没有可拒绝的传送请求。");
            return 1;
        }
        removeRequest(pick);
        ok(player, "已拒绝传送请求。");
        ServerPlayer requester = server.getPlayerList().getPlayer(pick.from);
        if (requester != null) {
            info(requester, player.getName().getString() + " 拒绝了你的传送请求。");
        }
        return 1;
    }

    public int tpCancel(ServerPlayer player) {
        List<TpRequest> list = outgoing.remove(player.getUUID());
        if (list == null || list.isEmpty()) {
            err(player, "你没有发出过待处理的传送请求。");
            return 1;
        }
        for (TpRequest r : list) {
            List<TpRequest> inc = incoming.get(r.to);
            if (inc != null) {
                inc.remove(r);
            }
        }
        ok(player, "已取消全部 " + list.size() + " 个传送请求。");
        return 1;
    }

    public int tpList(ServerPlayer player) {
        List<TpRequest> inc = incoming.getOrDefault(player.getUUID(), new ArrayList<>());
        List<TpRequest> out = outgoing.getOrDefault(player.getUUID(), new ArrayList<>());
        StringBuilder sb = new StringBuilder();
        sb.append("传入请求 (").append(inc.size()).append(")：");
        for (TpRequest r : inc) {
            ServerPlayer p = server.getPlayerList().getPlayer(r.from);
            sb.append(" ").append(p != null ? p.getName().getString() : r.from.toString().substring(0, 8)).append(",");
        }
        if (inc.isEmpty()) {
            sb.append(" 无");
        }
        info(player, sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("发出请求 (").append(out.size()).append(")：");
        for (TpRequest r : out) {
            ServerPlayer p = server.getPlayerList().getPlayer(r.to);
            sb2.append(" ").append(p != null ? p.getName().getString() : r.to.toString().substring(0, 8)).append(",");
        }
        if (out.isEmpty()) {
            sb2.append(" 无");
        }
        info(player, sb2.toString());
        return 1;
    }

    private void removeRequest(TpRequest r) {
        List<TpRequest> out = outgoing.get(r.from);
        if (out != null) {
            out.remove(r);
        }
        List<TpRequest> inc = incoming.get(r.to);
        if (inc != null) {
            inc.remove(r);
        }
    }


    public int setHome(ServerPlayer p, String name) {
        String key = name == null || name.isBlank() ? "home" : name.trim();
        Map<String, DataStore.HomePoint> homes = data.homesOf(p.getUUID());
        if (!homes.containsKey(key) && homes.size() >= config.maxHomes) {
            err(p, "家点数量已达上限（" + config.maxHomes + "），请先删除多余家点。");
            return 1;
        }
        homes.put(key, pointOf(p));
        data.save(dataFile());
        ok(p, "已设置家点 " + key + "。");
        return 1;
    }

    public int goHome(ServerPlayer p, String name) {
        String key = name == null || name.isBlank() ? "home" : name.trim();
        DataStore.HomePoint hp = data.homesOf(p.getUUID()).get(key);
        if (hp == null) {
            err(p, "家点 " + key + " 不存在。");
            return 1;
        }
        if (!cooldownCheck(p, "home", config.homeCooldownSeconds)) {
            return 1;
        }
        ServerLevel target = SafeTeleport.levelByKey(server, hp.dim);
        BlockPos safe = SafeTeleport.adjustUp(target, new BlockPos((int) hp.x, (int) hp.y, (int) hp.z));
        teleportWithDelay(p, target, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, hp.yaw, hp.pitch);
        ok(p, "正在传送回家点 " + key + "……");
        return 1;
    }

    public int homes(ServerPlayer p) {
        Map<String, DataStore.HomePoint> homes = data.homesOf(p.getUUID());
        if (homes.isEmpty()) {
            info(p, "你还没有设置家点，使用 /sethome [名称] 设置。");
            return 1;
        }
        info(p, "你的家点（" + homes.size() + "/" + config.maxHomes + "）：" + String.join(", ", homes.keySet()));
        return 1;
    }

    public int delHome(ServerPlayer p, String name) {
        DataStore.HomePoint hp = data.homesOf(p.getUUID()).remove(name);
        if (hp == null) {
            err(p, "家点 " + name + " 不存在。");
            return 1;
        }
        data.save(dataFile());
        ok(p, "已删除家点 " + name + "。");
        return 1;
    }

    public int renameHome(ServerPlayer p, String oldName, String newName) {
        Map<String, DataStore.HomePoint> homes = data.homesOf(p.getUUID());
        if (!homes.containsKey(oldName)) {
            err(p, "家点 " + oldName + " 不存在。");
            return 1;
        }
        if (homes.containsKey(newName)) {
            err(p, "家点 " + newName + " 已存在。");
            return 1;
        }
        homes.put(newName, homes.remove(oldName));
        data.save(dataFile());
        ok(p, "已将家点 " + oldName + " 重命名为 " + newName + "。");
        return 1;
    }


    public int back(ServerPlayer p) {
        if (!cooldownCheck(p, "back", config.backCooldownSeconds)) {
            return 1;
        }
        DataStore.HomePoint pt = data.backPoints.get(p.getUUID());
        if (pt == null) {
            err(p, "没有可返回的位置。");
            return 1;
        }
        if (config.deathBack && data.diedFlags.getOrDefault(p.getUUID(), false)) {
            data.diedFlags.put(p.getUUID(), false);
        }
        ServerLevel target = SafeTeleport.levelByKey(server, pt.dim);
        BlockPos safe = SafeTeleport.adjustUp(target, new BlockPos((int) pt.x, (int) pt.y, (int) pt.z));
        teleportWithDelay(p, target, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, pt.yaw, pt.pitch);
        ok(p, "正在返回上一个位置……");
        return 1;
    }

    /** 玩家死亡时记录死亡点（供 /back 使用）。 */
    public void onPlayerDeath(ServerPlayer p) {
        data.backPoints.put(p.getUUID(), pointOf(p));
        data.diedFlags.put(p.getUUID(), true);
        data.save(dataFile());
    }


    public int tpr(ServerPlayer p) {
        if (!cooldownCheck(p, "tpr", config.tprCooldownSeconds)) {
            return 1;
        }
        ServerLevel target = (config.tprDimension == null || config.tprDimension.isEmpty())
                ? (ServerLevel) p.level()
                : SafeTeleport.levelByKey(server, config.tprDimension);
        BlockPos center = new BlockPos((int) p.getX(), (int) p.getY(), (int) p.getZ());
        BlockPos pos = SafeTeleport.randomSafe(target, center, config.tprRange, Math.max(50, config.tprAttempts));
        if (pos.distSqr(center) < 25) {
            err(p, "随机传送失败：未找到安全落点，请在配置中缩小「随机传送范围」。");
            return 1;
        }
        teleportWithDelay(p, target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        ok(p, "正在随机传送……");
        return 1;
    }


    private DataStore.HomePoint pointOf(ServerPlayer p) {
        return new DataStore.HomePoint(p.level().dimension().identifier().toString(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
    }

    private java.nio.file.Path dataFile() {
        return MyrinTPMod.CONFIG_DIR.resolve("data.json");
    }

    /**
     * 带倒计时的安全传送：记录返回点 -> 延迟 -> 校验在线/移动 -> 执行。
     */
    public void teleportWithDelay(ServerPlayer who, ServerLevel target, double x, double y, double z, float yaw, float pitch) {
        // 记录 /back 返回点（传送前位置）
        data.backPoints.put(who.getUUID(), pointOf(who));
        data.diedFlags.put(who.getUUID(), false);
        data.save(dataFile());

        ServerLevel startLevel = (ServerLevel) who.level();
        double startX = who.getX();
        double startY = who.getY();
        double startZ = who.getZ();
        UUID id = who.getUUID();
        int delayMs = Math.max(0, config.teleportDelayTicks) * 50;

        if (delayMs == 0) {
            SafeTeleport.teleportPlayer(who, target, x, y, z, yaw, pitch);
            return;
        }

        scheduler.schedule(() -> server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                return;
            }
            if (p.level() != startLevel) {
                info(p, "传送已取消（传送期间改变了维度）。");
                return;
            }
            if (config.cancelOnMove) {
                double dx = p.getX() - startX;
                double dy = p.getY() - startY;
                double dz = p.getZ() - startZ;
                if (dx * dx + dy * dy + dz * dz > 1.0) {
                    info(p, "传送已取消（移动打断了倒计时）。");
                    return;
                }
            }
            SafeTeleport.teleportPlayer(p, target, x, y, z, yaw, pitch);
        }), delayMs, TimeUnit.MILLISECONDS);
    }
}
