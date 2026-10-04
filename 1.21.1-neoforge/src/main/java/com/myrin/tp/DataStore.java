package com.myrin.tp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 家点和返回点的存档。
 * 存储于 config/myrintp/data.json。
 */
public final class DataStore {

    private static final Gson GSON = new Gson();
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();

    /** 家点 */
    public final Map<UUID, Map<String, HomePoint>> homes = new HashMap<>();
    /** /back 返回点（传送前位置） */
    public final Map<UUID, HomePoint> backPoints = new HashMap<>();
    /** 玩家是否死亡（死亡点优先于传送前位置） */
    public final Map<UUID, Boolean> diedFlags = new HashMap<>();

    public static final class HomePoint {
        public String dim;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;

        public HomePoint() {
        }

        public HomePoint(String dim, double x, double y, double z, float yaw, float pitch) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public HomePoint copy() {
            return new HomePoint(dim, x, y, z, yaw, pitch);
        }
    }

    public static DataStore load(Path file) {
        DataStore ds = new DataStore();
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                if (root != null) {
                    if (root.has("homes") && root.get("homes").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("homes").entrySet()) {
                            UUID uuid = UUID.fromString(e.getKey());
                            JsonObject hm = e.getValue().getAsJsonObject();
                            Map<String, HomePoint> map = new HashMap<>();
                            for (Map.Entry<String, JsonElement> h : hm.entrySet()) {
                                map.put(h.getKey(), GSON.fromJson(h.getValue(), HomePoint.class));
                            }
                            ds.homes.put(uuid, map);
                        }
                    }
                    if (root.has("back") && root.get("back").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("back").entrySet()) {
                            UUID uuid = UUID.fromString(e.getKey());
                            ds.backPoints.put(uuid, GSON.fromJson(e.getValue(), HomePoint.class));
                        }
                    }
                    if (root.has("died") && root.get("died").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("died").entrySet()) {
                            UUID uuid = UUID.fromString(e.getKey());
                            ds.diedFlags.put(uuid, e.getValue().getAsBoolean());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ds;
    }

    public void save(Path file) {
        JsonObject root = new JsonObject();
        JsonObject homesObj = new JsonObject();
        for (Map.Entry<UUID, Map<String, HomePoint>> e : homes.entrySet()) {
            JsonObject hm = new JsonObject();
            for (Map.Entry<String, HomePoint> h : e.getValue().entrySet()) {
                hm.add(h.getKey(), GSON.toJsonTree(h.getValue()));
            }
            homesObj.add(e.getKey().toString(), hm);
        }
        JsonObject backObj = new JsonObject();
        for (Map.Entry<UUID, HomePoint> e : backPoints.entrySet()) {
            backObj.add(e.getKey().toString(), GSON.toJsonTree(e.getValue()));
        }
        JsonObject diedObj = new JsonObject();
        for (Map.Entry<UUID, Boolean> e : diedFlags.entrySet()) {
            diedObj.addProperty(e.getKey().toString(), e.getValue());
        }
        root.add("homes", homesObj);
        root.add("back", backObj);
        root.add("died", diedObj);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, PRETTY.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 获取/创建某玩家的家点表 */
    public Map<String, HomePoint> homesOf(UUID uuid) {
        return homes.computeIfAbsent(uuid, k -> new HashMap<>());
    }
}
