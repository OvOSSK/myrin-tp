package com.myrin.tp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件（JSON），字段都有默认值。
 * 全部字段带默认值，缺失字段自动补默认值。
 */
public final class Config {

    /** 0=关闭 1=限制非OP仅TP类 2=限制OP仅TP类 3=两者同时 */
    public int mode = 0;
    /** 模式一黑名单：名单内玩家豁免"非OP仅TP类"限制（玩家名） */
    public List<String> blacklistMode1 = new ArrayList<>();
    /** 模式二黑名单：名单内玩家豁免"OP仅TP类"限制（玩家名） */
    public List<String> blacklistMode2 = new ArrayList<>();
    /** 模式二指令白名单：默认放行基础指令 */
    public List<String> whitelistMode2 = defaultWhitelist();

    public int tpaRequestCooldownSeconds = 5;      // /tpa /tpahere 发送冷却
    public int tpaRequestTimeoutSeconds = 60;      // 传送请求有效期
    public int homeCooldownSeconds = 60;           // /home 冷却
    public int backCooldownSeconds = 120;          // /back 冷却
    public int tprCooldownSeconds = 300;           // /tpr /rtp 冷却
    public int teleportDelayTicks = 40;            // 传送倒计时（tick）
    public boolean cancelOnMove = true;            // 倒计时期间移动则取消
    public int tprRange = 10000;                   // 随机传送范围（以主城出生点为中心）
    public int tprAttempts = 12;                   // 随机落点尝试次数
    public String tprDimension = "minecraft:overworld"; // 随机传送目标维度
    public int maxHomes = 20;                      // 每个玩家最大家点数量
    public boolean deathBack = true;               // /back 优先返回死亡点

    private static List<String> defaultWhitelist() {
        List<String> l = new ArrayList<>();
        l.add("seed");
        l.add("msg");
        l.add("tell");
        l.add("w");
        l.add("help");
        l.add("list");
        l.add("me");
        return l;
    }

    public static Config load(Path file) {
        Config cfg = new Config();
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject obj = new Gson().fromJson(json, JsonObject.class);
                if (obj != null) {
                    if (obj.has("mode")) cfg.mode = obj.get("mode").getAsInt();
                    if (obj.has("blacklistMode1")) cfg.blacklistMode1 = strList(obj.getAsJsonArray("blacklistMode1"));
                    if (obj.has("blacklistMode2")) cfg.blacklistMode2 = strList(obj.getAsJsonArray("blacklistMode2"));
                    if (obj.has("whitelistMode2")) cfg.whitelistMode2 = strList(obj.getAsJsonArray("whitelistMode2"));
                    if (obj.has("tpaRequestCooldownSeconds")) cfg.tpaRequestCooldownSeconds = obj.get("tpaRequestCooldownSeconds").getAsInt();
                    if (obj.has("tpaRequestTimeoutSeconds")) cfg.tpaRequestTimeoutSeconds = obj.get("tpaRequestTimeoutSeconds").getAsInt();
                    if (obj.has("homeCooldownSeconds")) cfg.homeCooldownSeconds = obj.get("homeCooldownSeconds").getAsInt();
                    if (obj.has("backCooldownSeconds")) cfg.backCooldownSeconds = obj.get("backCooldownSeconds").getAsInt();
                    if (obj.has("tprCooldownSeconds")) cfg.tprCooldownSeconds = obj.get("tprCooldownSeconds").getAsInt();
                    if (obj.has("teleportDelayTicks")) cfg.teleportDelayTicks = obj.get("teleportDelayTicks").getAsInt();
                    if (obj.has("cancelOnMove")) cfg.cancelOnMove = obj.get("cancelOnMove").getAsBoolean();
                    if (obj.has("tprRange")) cfg.tprRange = obj.get("tprRange").getAsInt();
                    if (obj.has("tprAttempts")) cfg.tprAttempts = obj.get("tprAttempts").getAsInt();
                    if (obj.has("tprDimension")) cfg.tprDimension = obj.get("tprDimension").getAsString();
                    if (obj.has("maxHomes")) cfg.maxHomes = obj.get("maxHomes").getAsInt();
                    if (obj.has("deathBack")) cfg.deathBack = obj.get("deathBack").getAsBoolean();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cfg;
    }

    private static List<String> strList(com.google.gson.JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr == null) return out;
        arr.forEach(e -> out.add(e.getAsString()));
        return out;
    }

    public void save(Path file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(toJson());
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("mode", mode);
        o.add("blacklistMode1", new Gson().toJsonTree(blacklistMode1));
        o.add("blacklistMode2", new Gson().toJsonTree(blacklistMode2));
        o.add("whitelistMode2", new Gson().toJsonTree(whitelistMode2));
        o.addProperty("tpaRequestCooldownSeconds", tpaRequestCooldownSeconds);
        o.addProperty("tpaRequestTimeoutSeconds", tpaRequestTimeoutSeconds);
        o.addProperty("homeCooldownSeconds", homeCooldownSeconds);
        o.addProperty("backCooldownSeconds", backCooldownSeconds);
        o.addProperty("tprCooldownSeconds", tprCooldownSeconds);
        o.addProperty("teleportDelayTicks", teleportDelayTicks);
        o.addProperty("cancelOnMove", cancelOnMove);
        o.addProperty("tprRange", tprRange);
        o.addProperty("tprAttempts", tprAttempts);
        o.addProperty("tprDimension", tprDimension);
        o.addProperty("maxHomes", maxHomes);
        o.addProperty("deathBack", deathBack);
        return o;
    }
}
