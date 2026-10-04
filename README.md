<div align="center">

<img src="myrintp.jpg" width="96" alt="MYRIN TP" />

# MYRIN TP

**无需作弊的传送与作弊指令管控模组**

Forge 1.20.1 ・ NeoForge 1.21.1 ・ Fabric 26.3

[![Build](https://github.com/OvOSSK/myrin-tp/actions/workflows/build-release.yml/badge.svg)](https://github.com/OvOSSK/myrin-tp/actions)
![Version](https://img.shields.io/badge/version-v1.3.2-blue)
![License](https://img.shields.io/badge/license-MIT-green)

</div>

MYRIN TP 是一个**服务端模组**：玩家无需开启作弊即可使用完整的传送功能，管理员可以一键切换**指令管控模式**，精确控制 `/tp` 等指令的使用范围。三个加载器版本共享同一套配置、指令与行为。

---

## 目录

- [功能特性](#功能特性)
- [支持版本](#支持版本)
- [安装](#安装)
- [指令一览](#指令一览)
- [指令管控](#指令管控)
- [配置文件](#配置文件)
- [配置界面](#配置界面)
- [常见问题](#常见问题)
- [从源码构建](#从源码构建)
- [许可证](#许可证)

---

## 功能特性

**传送体系**
- `/tpa` `/tpahere` 请求制传送，支持同意 / 拒绝 / 取消 / 列表
- `/sethome` `/home` 家系统，支持多命名家、重命名与删除
- `/back` 返回死亡点或传送前位置
- `/tpr` `/rtp` 随机传送：安全落点检查、范围与目标维度可配置

**安全与体验**
- 所有传送带**安全落点检查**与自动调整，避免卡进方块或摔落
- 传送**冷却**、**倒计时**与移动取消保护，配置修改后即时生效
- 支持**跨维度**传送
- 原版 `/tp` 的**选择器**（如 `@p`）正常工作

**指令管控**
- 四档管控模式，一条命令即时切换，游戏内改动立即生效
- 黑名单 / 白名单按模式生效，`F3+F4` 快捷切换模式也纳入管控

**三端一致**
- 同一份 `config.json` 字段与行为，Forge / NeoForge / Fabric 无差别
- 游戏内配置界面（Fabric 走 ModMenu，Forge / NeoForge 走模组配置按钮）

---

## 支持版本

| 加载器 | 游戏版本 | 服务端 | 客户端 |
| --- | --- | --- | --- |
| Forge | 1.20.1 | ✅ | ✅（含配置界面） |
| NeoForge | 1.21.1 | ✅ | ✅（含配置界面） |
| Fabric | 26.3 | ✅ | ✅（含 ModMenu 配置界面） |

> 服务端形式模组：服务端安装即可生效，客户端可选安装以使用配置界面。

## 安装

1. 从 [Releases](https://github.com/OvOSSK/myrin-tp/releases) 下载对应加载器的 jar
2. 放入服务器的 `mods` 文件夹（客户端如需配置界面也放入客户端的 `mods`）
3. 启动服务器，首次运行自动生成配置文件
4. 默认模式 0（关闭管控）：玩家不可使用原版 `/tp`（恢复原版权限），模组传送指令全部可用

---

## 指令一览

### 传送指令（所有玩家可用）

| 指令 | 说明 |
| --- | --- |
| `/tpa <玩家>` | 请求传送到对方身边 |
| `/tpahere <玩家>` | 请求对方传送到自己身边 |
| `/tpyes` `/tpaccept` `[玩家]` | 同意传送请求 |
| `/tpno` `/tpdeny` `[玩家]` | 拒绝传送请求 |
| `/tpcancel` | 取消自己发起的请求 |
| `/tplist` | 查看待处理的传送请求 |
| `/sethome [名称]` | 设置家（默认 `home`，最多 `maxHomes` 个） |
| `/home [名称]` | 传送到家 |
| `/homes` | 查看家的列表 |
| `/delhome <名称>` | 删除家 |
| `/renamehome <旧名称> <新名称>` | 重命名家 |
| `/back` | 返回死亡点或传送前位置（受冷却限制） |
| `/tpr` `/rtp` | 随机传送到安全落点（受冷却与范围限制） |

### 原版指令

| 指令 | 说明 |
| --- | --- |
| `/tp` `/teleport` | 原版传送，权限遵循管控模式（模式 0 下仅 OP 可用） |

### 管理指令（需 OP）

| 指令 | 说明 |
| --- | --- |
| `/mtp mode <0-3>` | 切换指令管控模式 |
| `/mtp status` | 查看当前模式与名单 |
| `/mtp reload` `/myrintp reload` | 重载配置文件 |
| `/mtp blacklist1 add\|remove\|list <玩家>` | 模式 1 / 3 黑名单 |
| `/mtp blacklist2 add\|remove\|list <玩家>` | 模式 2 / 3 黑名单 |
| `/mtp whitelist add\|remove\|list <指令>` | 模式 2 / 3 白名单（指令名） |

---

## 指令管控

四种模式可通过配置界面、`/mtp mode <0-3>` 或直接改配置即时切换：

| 模式 | 名称 | 行为 |
| --- | --- | --- |
| 0 | 关闭 | 原版权限：非 OP 不可使用 `/tp` `/teleport`；其余指令不限制；模组传送指令可用 |
| 1 | 玩家 仅 TP | 非管理员只能使用 TP 类指令 |
| 2 | OP 禁非 TP | 管理员只能使用 TP 类指令 |
| 3 | 同时启用 | 任何人只能使用 TP 类指令 |

**规则说明**

- **TP 类指令**：`tp` `teleport`、`tpa` `tpahere` `tpyes` `tpaccept` `tpno` `tpdeny` `tpcancel` `tplist`、`sethome` `home` `homes` `delhome` `renamehome`、`back` `tpr` `rtp`
- **黑名单**：命中名单的玩家在该模式下不受限制（模式 1/3 对应 `blacklist1`，模式 2/3 对应 `blacklist2`）
- **白名单**：仅模式 2/3 生效，名单内的指令额外放行；默认放行 `seed` `msg` `tell` `w` `help` `list` `me`
- 被禁止的指令执行时会收到红字提示，**指令补全保留**（不删除补全，仅执行时拦截）
- `F3+F4` 快捷切换创造 / 生存等模式也按指令管控判定（等价于 `/gamemode`）

---

## 配置文件

首次启动自动生成于 `config/myrintp/`，编辑后执行 `/mtp reload`，或直接在配置界面修改（即时生效）。

`config.json` 字段：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `mode` | `0` | 指令管控模式（0-3） |
| `blacklistMode1` | `[]` | 模式 1/3 黑名单（玩家名） |
| `blacklistMode2` | `[]` | 模式 2/3 黑名单（玩家名） |
| `whitelistMode2` | `["seed","msg","tell","w","help","list","me"]` | 模式 2/3 白名单（指令名） |
| `tpaRequestCooldownSeconds` | `5` | `/tpa` `/tpahere` 发送冷却（秒） |
| `tpaRequestTimeoutSeconds` | `60` | 传送请求有效期（秒） |
| `homeCooldownSeconds` | `60` | `/home` 冷却（秒） |
| `backCooldownSeconds` | `120` | `/back` 冷却（秒） |
| `tprCooldownSeconds` | `300` | `/tpr` `/rtp` 冷却（秒） |
| `teleportDelayTicks` | `40` | 传送倒计时（tick，20 tick = 1 秒） |
| `cancelOnMove` | `true` | 倒计时期间移动是否取消传送 |
| `tprRange` | `10000` | 随机传送范围（以出生点为中心，方块） |
| `tprAttempts` | `50` | 随机落点尝试次数 |
| `tprDimension` | `""` | 随机传送目标维度（留空 = 当前维度） |
| `maxHomes` | `20` | 每名玩家最大家数量 |
| `deathBack` | `true` | `/back` 优先返回死亡点 |

`data.json`：家点与返回点存档（同目录），删除即清空全部家数据。

---

## 配置界面

| 加载器 | 入口 | 能力 |
| --- | --- | --- |
| Fabric | ModMenu → MYRIN TP → 配置 | 模式切换、冷却、随机传送、热重载 |
| Forge / NeoForge | Mods 列表 → myrintp → 配置 | 同上 |

- 修改**立即生效**，无需重启
- 界面自适应不同分辨率
- 所有开关、滑块与模式选项与 `config.json` 字段一一对应

---

## 常见问题

**模式 0 下为什么非 OP 不能使用 `/tp`？**

模式 0 是「关闭管控」，即恢复原版权限：原版 `/tp` 本身就是 OP 指令，非 OP 不可用。需要放开时请切换到模式 1/2/3。

**随机传送为什么落点总是原地或范围很小？**

`tprRange` 控制随机范围（默认 10000 格）。若目标维度不支持或在尝试次数内未找到安全落点，会回退处理；可调大 `tprAttempts` 提升成功率。

**被禁止的指令补全为什么还显示？**

管控只拦截**执行**，不删除补全，执行时会有红字提示，避免破坏原版指令体验。

**配置文件改了没生效？**

改完执行 `/mtp reload`；配置界面内修改则自动即时生效。

**家点数据存在哪里？**

`config/myrintp/data.json`。备份配置时连同该文件一起备份即可保留所有家点。

---

## 从源码构建

三个子工程互相独立，均由 GitHub Actions 自动构建（`build-release.yml`，三端 matrix 并行）：

```text
1.20.1-forge     JDK 17 + Gradle 8.8
1.21.1-neoforge  JDK 21 + Gradle 8.10.2
26.3-fabric      JDK 25 + Gradle 9.8.0
```

推送 `main` 并打 tag（如 `v1.3.2`）即触发构建，产物自动发布到 [Releases](https://github.com/OvOSSK/myrin-tp/releases)，文件名为 `myrin-tp-<版本>-<加载器>-<时间戳>.jar`。

本地构建：进入对应子工程目录执行 `./gradlew build`（需匹配对应 JDK）。

---

## 许可证

[MIT](LICENSE)
