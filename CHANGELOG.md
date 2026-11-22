# 更新日志

## 1.1.0 — 2026-11-22

### 变更

- 命令改为**直接触发**：`/ping`、`/traffic`、`/top`、`/tps`、`/cpu`、`/mem`、`/gc`、`/thread`、`/entity`、`/chunk`、`/info`、`/env`、`/disk`、`/check`，不再需要 `/basicstatuscmd <子命令>` 前缀。
- `/basicstatuscmd` 改为命令索引，列出全部直接命令；新增 `/basicstatuscmd reload` 热重载配置与语言文件（权限 `basicstatuscmd.reload`，默认 OP）。
- 语言文件按平台拆分：paper 包用 MiniMessage（与 BasicTpCmd 现代线同格式），bukkit 包用 `&` 色码（与 BasicTpCmd legacy 线同格式）。
- paper 包检测到 1.0.0 的 `&` 色码语言文件时，在内存中转换为 MiniMessage，不覆盖玩家文件。

### 权限

- 新增 `basicstatuscmd.reload`；`basicstatuscmd.admin` 的 children 同步包含它。
- 其余权限节点保持 `basicstatuscmd.<命令>` 不变，升级不用改权限组。

## 1.0.0

- 首个版本：`/basicstatuscmd` 单根命令，14 个子命令
- 网络：`ping`、`traffic`、`top`
- 性能：`tps`、`cpu`、`mem`、`gc`、`thread`
- 生态：`entity`、`chunk`
- 系统：`info`、`env`、`disk`
- 巡检：`check`，7 项阈值可配
- 双平台产物：bukkit（1.8.8 – 1.18.1，Java 8）、paper（1.18.2+，Java 17）
- 中英双语 `zh_CN` / `en_US`
