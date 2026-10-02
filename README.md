# Myrin TP

Minecraft 服务端的传送 + 指令限制可配置模组。纯服务端逻辑、无 Mixin、无作弊，不需要额外前置（各平台的官方加载器除外）。

## 版本

| 平台 | 版本 | 加载器 | 目录 |
|---|---|---|---|
| Forge | 1.20.1 | Forge 47.4.23 | `1.20.1-forge/` |
| NeoForge | 1.21.1 | NeoForge 21.1.252 | `1.21.1-neoforge/` |
| Fabric | 26.3 | Fabric Loader 0.19.5 + Fabric API 0.161.0 | `26.3-fabric/` |

三个版本逻辑一致，各自是独立的 Gradle 工程。

## 功能

### 便携传送指令，玩家可用，无需作弊

`/tpa` `/tpahere` `/tpyes`(`/tpaccept`) `/tpno`(`/tpdeny`) `/tpcancel` `/tplist` `/sethome` `/home` `/homes` `/delhome` `/renamehome` `/back` `/tpr`(`/rtp`)

- 传送带安全落点检查、倒计时（可配移动取消）、冷却，跨维度也能传
- 家点可以命名，默认上限 20 个
- /back 返回上一个位置，死了优先回死亡点

### 指令守卫（服主配置）

| mode | 效果 |
|---|---|
| 0 | 关闭 |
| 1 | 非 OP 只能用 TP 类指令，OP 不受限 |
| 2 | OP 只能用 TP 类指令（白名单里的除外） |
| 3 | 模式 1、2 同时开 |

- 模式一、二各有独立黑名单，名单内玩家在对应模式豁免
- 模式二带指令白名单，默认放行 seed/msg/tell/w/help/list/me
- 游戏内配置：`/mtp mode <0-3>`、`/mtp blacklist1|blacklist2 add|remove|list <玩家>`、`/mtp whitelist add|remove|list <指令>`、`/mtp status`、`/mtp reload`
- Fabric 端包装指令节点 requires，Forge/NeoForge 端拦 CommandEvent，都没用 Mixin

## 配置

首次启动在 `config/myrintp/` 生成两个文件：

- `config.json`：模式、黑名单、白名单、冷却、随机传送范围等
- `data.json`：家点和返回点的存档

## 构建

各子目录独立构建：

```bash
cd 1.20.1-forge    # 或 1.21.1-neoforge / 26.3-fabric
./gradlew build
```

产物在 `build/libs/`。
