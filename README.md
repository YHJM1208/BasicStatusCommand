# BasicStatusCommand

服务器状态查询插件：**延迟、流量、TPS、CPU/内存/GC/线程、实体、区块、磁盘、一键巡检**。
**14 条直接命令**（`/ping`、`/cpu`、`/check`……），不用打插件前缀；`/basicstatuscmd` 只负责命令索引与 `reload`。丢一个 jar 进 `plugins/` 就能用，不需要前置插件。

- **14 条直接命令**：网络 / 性能 / 生态 / 系统 / 巡检五组状态一网打尽
- **零依赖**：JVM 读数用 `java.lang.management`，流量用服务端自带 Netty
- **两个包**：bukkit 线覆盖 1.8.8 – 1.18.1，paper 线用原生 TPS / ping 接口
- **中英双语**：`zh_CN` / `en_US` 现成可用
- **巡检可配阈值**：TPS、内存、磁盘、区块、实体、线程、GC 七项，异常单独标出

## 支持范围

| 服务端核心 | MC 版本 | Java | 用哪个包 |
|---|---|---|---|
| CraftBukkit / Spigot | 1.8.8 – 1.18.1 | 8+ | `BasicStatusCommand-1.1.0-bukkit-1.8.8-1.18.1.jar` |
| Paper | 1.18.2 – 26.3 | 17+ | `BasicStatusCommand-1.1.0-paper-1.18.2.jar` |

Purpur / Pufferfish 等 Paper 分支按对应版本线装。**同一服只装一个包**，两个包同时放会因同名插件冲突。

## 安装

1. 按上表选一个 jar
2. 放进服务端 `plugins/` 目录
3. 重启服务器
4. 控制台或游戏内执行 `/basicstatuscmd`，看到命令列表即安装成功

## 命令与权限

所有命令都直接触发，不带插件前缀；`/basicstatuscmd` 只做命令索引与热重载。

| 命令 | 说明 | 权限节点 | 默认 |
|---|---|---|---|
| `/basicstatuscmd` | 显示状态命令列表 | — | 所有人 |
| `/basicstatuscmd reload` | 重载配置与语言文件 | `basicstatuscmd.reload` | 仅 OP |
| `/ping [玩家]` | 查看自己或他人的延迟 | `basicstatuscmd.ping` | 所有人 |
| `/traffic` | 采样玩家通道收发速率 | `basicstatuscmd.admin` | 仅 OP |
| `/top` | 延迟最高的前 N 名（默认 5） | `basicstatuscmd.top` | 所有人 |
| `/tps` | 1/5/15 分钟 TPS（paper 含 MSPT） | `basicstatuscmd.tps` | 所有人 |
| `/cpu` | JVM 进程 CPU / 系统 CPU / 核心数 | `basicstatuscmd.admin` | 仅 OP |
| `/mem` | 堆、非堆、物理内存 | `basicstatuscmd.admin` | 仅 OP |
| `/gc` | 各收集器次数与耗时 | `basicstatuscmd.admin` | 仅 OP |
| `/thread` | 活跃 / 峰值 / 守护 / 累计线程 | `basicstatuscmd.admin` | 仅 OP |
| `/entity` | 按怪物 / 动物 / 水生 / 村民 / 其他分类 | `basicstatuscmd.admin` | 仅 OP |
| `/chunk` | 全服已加载区块数，按世界分行 | `basicstatuscmd.admin` | 仅 OP |
| `/info` | 服务端、版本、在线、世界、插件、运行时长 | `basicstatuscmd.info` | 所有人 |
| `/env` | Java / 虚拟机 / 系统 / 编码 / 时区 / 目录 | `basicstatuscmd.admin` | 仅 OP |
| `/disk` | 各磁盘可用空间 | `basicstatuscmd.admin` | 仅 OP |
| `/check` | 一键巡检，按阈值标异常 | `basicstatuscmd.admin` | 仅 OP |

`basicstatuscmd.admin` 是管理类命令的总节点（默认 OP），`children` 已包含其余管理节点；只给某个管理命令授权时也可单独给对应节点。

## 配置项

配置文件 `plugins/BasicStatusCommand/config.yml`，改完执行 `/basicstatuscmd reload` 生效。

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `language` | `zh_CN` | 语言，可选 `zh_CN` / `en_US` |
| `ping.good-ms` / `medium-ms` | `80` / `200` | 延迟分档：良好 / 一般 / 较差 |
| `top.size` | `5` | `/top` 显示人数 |
| `traffic.sample-seconds` | `3` | 流量采样窗口秒数 |
| `entity.max-worlds` | `8` | 实体统计最多显示几个世界 |
| `check.tps-warn` / `tps-fail` | `18.0` / `10.0` | TPS 关注 / 危险阈值 |
| `check.memory-warn-percent` / `-fail-percent` | `85` / `95` | 堆内存占用阈值 |
| `check.disk-free-warn-mb` | `2048` | 磁盘剩余空间阈值（MB） |
| `check.chunk-warn` | `10000` | 已加载区块阈值 |
| `check.entity-warn` | `5000` | 实体总数阈值 |
| `check.thread-warn` | `400` | 活跃线程阈值 |
| `check.gc-warn-percent` | `20` | GC 时间占运行时长阈值 |

## 兼容说明

- **语言文件分两套**：paper 包用 MiniMessage（与 BasicTpCmd 现代线同格式），bukkit 包用 `&` 色码（与 BasicTpCmd legacy 线同格式）；升级只补缺失键，不覆盖你改过的文案。
- **1.0.0 → 1.1.0 的 paper 语言迁移**：旧版 paper 语言文件是 `&` 色码，1.1.0 会在内存里转换成 MiniMessage 再渲染，不覆盖原文件；要彻底换成新模板，删掉 `plugins/BasicStatusCommand/lang/` 后重启即可。
- **直接命令可能撞名**：`/ping`、`/top`、`/tps`、`/info` 比较通用，若已被其他插件注册，Bukkit 会跳过该命令并在控制台提示；先卸掉同名命令的插件。
- **流量只统计玩家连接**：在玩家 Netty 通道挂计数 handler，列表 ping、控制台连接不计入；个别改过网络层的服务端可能提示「未接入通道」。
- **bukkit 线 TPS 自采样**：逐 tick 记录时间戳回算窗口，刚起服时 5/15 分钟会显示「采样中」；paper 线直接读服务端 TPS，无暖机。
- **bukkit 线 ping**：1.8.8 没有 `Player#getPing()`，运行期先反射该方法，老版本回退到 `EntityPlayer` 的 `ping` 字段。
- **数据**：不写数据库，只有 `config.yml` 与 `lang/*.yml`；删掉配置目录会恢复默认。

## 更新日志

1.1.0：命令改为 `/ping`、`/cpu`、`/check` 等 14 条直接触发；`/basicstatuscmd` 只保留命令索引与 `reload`；paper 语言文件改 MiniMessage、bukkit 保持 `&` 色码，与 BasicTpCmd 两条线对齐。历史见 [CHANGELOG.md](CHANGELOG.md)。

## 许可与来源

MIT License，Copyright (c) 2026 YHJM1208。
源码仓库：https://github.com/YHJM1208/BasicStatusCommand
