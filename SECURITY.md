# 安全说明

> 适用范围：MiniCat v1.2.0（单体 JAR / Docker 部署）。
> 本文是**安全模型与威胁边界**的权威说明；使用/部署前请务必读第 1、6 节。

---

## 1. 安全模型（重要）

MiniCat 的定位是**单用户、本机运行**的数据库管理工具，不是多租户在线服务：

- 默认只监听回环地址（`server.address=127.0.0.1`），局域网/公网无法直接访问；
- **没有登录认证、没有角色权限、没有操作审计**；
- 应用能解密所有已保存的连接凭据，并用这些凭据连接目标数据库；
- 任何能访问到本应用端口的人，等价于**拥有这些数据库账号的权限**。

因此：

1. **不要直接部署到公网/公司内网**。远程访问至少叠加：
   - 反向代理 + HTTPS；
   - 代理层 Basic Auth（或 VPN / IP 白名单）；
   - 只允许可信网络访问。
2. 不要分享/提交 `data/` 目录（含 H2 元数据与加密密钥）。
3. 迁移机器时把 `data/` 一并迁移可保留连接；密钥丢失则历史密码无法解密。

> 早期版本曾有一份 “BCrypt 密码哈希 / 全面注入防护” 的安全增强说明（已随文档重构删除，可在 Git 历史中查看）。
> 该说明与实现不符：数据库密码必须可逆才能连接，本项目使用 AES-256-GCM；SQL 校验是黑名单提醒，不是安全边界。

---

## 2. 凭据存储

- 连接密码使用 **AES-256-GCM** 加密后存入 H2（`./data/minicat_db`），密文格式 `v2:Base64(IV+ciphertext)`；
- 加密密钥来源（优先级从高到低）：
  1. 环境变量 `MINICAT_SECRET_KEY`（容器化/CI 推荐）；
  2. 密钥文件 `minicat.security.key-file`（默认 `./data/secret.key`，首次启动随机生成，权限尽力收紧）；
- 旧版本（v1.0.0 及之前）固定种子派生的密钥仍可解密历史密文，并在下次保存连接时升级为 `v2:` 格式；
- **对称加密的边界**：拿到 `data/secret.key` + H2 文件 = 拿到全部连接密码明文，请像保护密码一样保护它们；
- API 响应不回传明文密码（GET 接口 `password` 恒为 `null`；POST/PUT 的响应可能含密文，请勿依赖/记录）；
- 密码/密钥永不写入日志（日志中只出现连接 ID、库名、表名、耗时等）。

---

## 3. 网络与访问控制

| 措施 | 说明 |
|---|---|
| 默认回环监听 | `server.address=127.0.0.1`；容器内由 `SERVER_ADDRESS=0.0.0.0` 覆盖，对外暴露仍由 compose `ports` 决定 |
| 同源访问 | 已移除 Controller 上的 `@CrossOrigin(origins="*")`，前端与 API 同源部署，不开启跨域 |
| 无状态认证 | 当前无认证；反向代理层负责 Basic Auth / VPN / IP 白名单 |
| 上传限制 | `spring.servlet.multipart.max-file-size=50MB`（防超大文件耗尽内存/磁盘） |

---

## 4. SQL 执行与数据修改

### 4.1 SQL 校验的定位

`SqlInjectionValidator` 是基于正则黑名单的**危险操作提醒**，不是安全边界：

- 拦截常见注入特征（`UNION SELECT`、系统表、文件读写、时间盲注等）；
- 会**误伤**合法 SQL（如 `information_schema` 查询、优化器 hint）；
- **不能阻止**所有注入手法（行注释、多语句、布尔盲注等）。

执行通道约束：

- `POST .../query` 仅允许 `SELECT`；
- `POST .../update` 仅允许 `INSERT / UPDATE / DELETE / DROP TABLE`；
- `UPDATE / DELETE` 必须包含 `WHERE`，`DROP TABLE` 禁止多语句。

### 4.2 单元格编辑（P1-1）

`PUT .../table-row` 不拼接用户输入：

- 表名/列名通过白名单正则（仅字母/数字/`_`/`$`）并加反引号；
- 所有值使用 `PreparedStatement` 参数绑定（含 `NULL`）；
- 服务端按列元数据校验列存在、类型可编辑、不允许改行标识列/自增列/二进制与 JSON 类型；
- 支持可选 `expected` 旧值 CAS；冲突返回 `40902`，不静默覆盖。

### 4.3 前端内联编辑开关

`minicat.ui.inline-edit-enabled=false` 可把表格降级为只读（`GET /settings` 下发）。
在共享库/生产库上使用建议关闭或至少保持 CAS 开启。

---

## 5. 其他已实现防护

| 项 | 说明 |
|---|---|
| 全局异常处理 | 业务异常按 `ErrorCode` 返回业务码 + 可读消息；SQL 异常按 SQLState 归类；未预期异常不向前端暴露堆栈 |
| 日志 | 统一 slf4j；密码/密钥不记录；5xx 记录堆栈，4xx 只记录 warn |
| H2 控制台 | 生产 profile 关闭 |
| DevTools | 生产 profile 关闭 |
| Docker | 非 root 用户运行；健康检查探测首页（无 actuator） |
| 导入事务 | 批量插入失败整体回滚（避免半批数据） |

---

## 6. 部署加固清单（远程/共享环境）

- [ ] 反向代理 + HTTPS，并启用 Basic Auth / VPN / IP 白名单；
- [ ] 不要直接映射 8888 到公网；
- [ ] 为 `data/` 设置最小权限（仅运行用户可读）；
- [ ] 优先用 `MINICAT_SECRET_KEY`（Secret 注入）而不是落盘密钥；
- [ ] 关闭内联编辑（`minicat.ui.inline-edit-enabled=false`）或限制目标库账号权限（只读账号）；
- [ ] 目标数据库使用最小权限账号（不要用 root 管理账号日常登录）；
- [ ] 定期备份 `data/`，并妥善保管密钥；
- [ ] 关注日志中的异常登录/失败操作（本项目自身无审计，需依赖代理/数据库审计）。

---

## 7. 已知限制

- 无认证 / 审计 / 多用户权限体系；
- PostgreSQL 目前仅“测试连接”可用，浏览/编辑能力在 Roadmap；
- 查询结果一次性加载内存，无行数上限/执行超时（大表查询请加 `LIMIT`）；
- SQL 黑名单校验会误伤合法 SQL，也不是安全边界；
- 无 CSRF 防护（无认证 + 仅本机访问的权衡，远程部署需在反代层补）；
- 单元格编辑目前针对 MySQL 方言（反引号）。

---

## 8. 报告安全问题

请优先通过仓库 Issue 报告（不要公开可复现的攻击细节），或直接联系作者；修复前请勿公开漏洞利用方式。

- GitHub Issues：<https://github.com/zhuyuebin/mini-cat/issues>
- Gitee Issues（国内镜像）：<https://gitee.com/zhuyuebin/mini-cat/issues>

---

## 9. 相关文档

- [README.md](README.md) — 项目概览与快速开始
- [docs/CONFIGURATION.md](docs/CONFIGURATION.md) — 密钥/环境变量/数据目录
- [docs/API.md](docs/API.md) — 接口与错误码
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) — 部署方式
