# 发布与双远程同步

> 仓库策略：**GitHub 为主仓库**（<https://github.com/zhuyuebin/mini-cat>），
> **Gitee 为国内备份**（<https://gitee.com/zhuyuebin/mini-cat>）。
> 代码、Tag、Release 以 GitHub 为准；Gitee 保持同步，供访问 GitHub 受限时使用。

---

## 1. 版本约定

- 遵循 [语义化版本](https://semver.org/lang/zh-CN/)：`MAJOR.MINOR.PATCH`；
- 当前版本：**v1.2.0**（2026-10-24 发布，Tag 已推送 GitHub/Gitee）；
- 后续版本发布时，按以下步骤先改版本号，再走第 2、3 节流程：

```bash
# 1) 更新 pom.xml 版本（示例：1.2.0）
#    <version>1.2.0</version>

# 2) 同步文档中的 JAR 示例名（docs/ 与 README 中的 target/...jar）
#    脚本已使用通配符 target/minicat-server-*.jar，无需修改

# 3) 更新 CHANGELOG.md：把 [Unreleased] 改为 [1.2.0] - YYYY-MM-DD

# 4) 重新构建并冒烟
./mvnw clean package -DskipTests
java -jar target/minicat-server-1.2.0.jar --server.port=8888
```

---

## 2. 发布前检查清单

- [ ] 当前分支为待发布分支，`git status` 干净；
- [ ] 后端测试全绿：`./mvnw -Dskip.installnodenpm -Dskip.npm test`（当前 51 个）；
- [ ] 前端构建通过：`cd MiniCat-Frontend && npm run build`；
- [ ] 完整打包 + 启动冒烟：首页 200、`/api/database/connections` 200、`/api/database/settings` 200；
- [ ] `CHANGELOG.md` 已更新（新增/修复/变更/安全）；
- [ ] README 与 docs/ 的版本号、接口数量、测试数量与实际一致；
- [ ] 敏感信息自查：

```bash
git ls-files | Select-String -Pattern '\.env|\.key|\.pem|\.db|secret|password' -CaseSensitive:$false
git ls-files data/          # 应为空
```

- [ ] `target/`、`data/`、`node_modules/` 未被提交；
- [ ] CI（GitHub Actions）在待发布提交上为绿色。

---

## 3. 合并与打 Tag

```bash
# 1) 切到 main 并确认最新
git checkout main
git pull origin main

# 2) 合并功能分支（保留里程碑历史）
git merge --no-ff feat/inline-edit -m "merge: P0 前端重构 + P1-1 内联编辑"

# 3) 冒烟（见第 2 节）后提交
git push origin main

# 4) 打标签（与 CHANGELOG 版本一致）
git tag -a v1.2.0 -m "MiniCat v1.2.0: 前端信息架构重构 + 表格内联编辑"
git push origin v1.2.0
```

> 建议给当前 M0/M1/P0/P1 里程碑也补历史 Tag（如 `v1.0.1`、`v1.1.0`），便于对比。

---

## 4. GitHub 主仓库

### 4.1 首次配置（若尚未配置）

```bash
git remote -v
# origin  git@github.com:zhuyuebin/mini-cat.git (fetch/push)
```

推荐使用 SSH（参考 GitHub → Settings → SSH and GPG keys 配置公钥）：

```bash
ssh -T git@github.com
```

### 4.2 创建 Release

1. 打开 <https://github.com/zhuyuebin/mini-cat/releases/new>；
2. Tag 选择刚推送的 `v1.2.0`；
3. 标题：`MiniCat v1.2.0`；正文可从 `CHANGELOG.md` 对应版本复制（重点：新增/修复/变更/安全）；
4. 可附加构建产物：`target/minicat-server-1.2.0.jar`（发布 jar 由 CI `package` job 产出，可下载后上传）。

---

## 5. Gitee 备份同步

### 5.1 首次添加远程

```bash
# HTTPS（需要 Gitee 账号密码/私人令牌）
git remote add gitee https://gitee.com/zhuyuebin/mini-cat.git

# 或 SSH（推荐）
git remote add gitee git@gitee.com:zhuyuebin/mini-cat.git
```

### 5.2 同步 main 与 Tag

```bash
git push gitee main
git push gitee --tags
```

- 若 Gitee 仓库已存在旧历史且出现 non-fast-forward：
  `git push gitee main --force-with-lease`（**确认 Gitee 无独有提交后再强推**）；
- Gitee Release：仓库 → 「发行版」→「新建发行版」，Tag 选 `v1.2.0`，正文同 GitHub；
- Gitee 仓库设置建议：默认分支 `main`、启用 Issues、补充话题标签
  （`mysql` / `vue` / `spring-boot` / `database-tools` 等）。

### 5.3 日常双远程同步

```bash
# 日常开发只在本地/功能分支进行；发布时：
git push origin main --tags   # GitHub 主
git push gitee  main --tags   # Gitee 备份
```

> Gitee 仅作代码镜像，**不配置 CI/CD**（用户无 Gitee Go 流水线额度；CI 全部由 GitHub Actions
> 的公开仓库免费额度承担，长期只用 GitHub Actions）。因此仓库中只保留 `.github/workflows/`。

---

## 6. 发布后验证

- [ ] GitHub Actions 三个 job 全绿；
- [ ] GitHub Release 页面附件/说明正常；
- [ ] Gitee 镜像仓库代码、Tag、Release 与 GitHub 一致；
- [ ] 全新目录 `git clone` → `java -jar target/...`（或下载 Release JAR）→ 首页可访问；
- [ ] 用旧 `data/` 启动，确认历史连接仍可用（密钥未变）。

---

## 7. 热修复（Hotfix）

```bash
git checkout main
git checkout -b fix/<issue>
# 修复 + 补测试 + 更新 CHANGELOG
git commit -m "fix: ..."
git checkout main
git merge --no-ff fix/<issue>
git tag -a v1.2.1 -m "MiniCat v1.2.1"
git push origin main v1.2.1
git push gitee  main v1.2.1
```

---

## 8. 凭据与安全

- 不要把 Gitee/GitHub Token、密码写进 remote URL、脚本或 `.git/config` 后提交；
- 本机 SSH 私钥权限 600，不放进仓库；
- `data/`（H2 元数据 + `secret.key`）永远不提交；发布构建也不要打包 `data/`；
- 若误提交了密钥/密码：立即轮换凭据，再用 `git filter-repo`/`filter-branch` 清理历史并强推双远程。

---

## 9. 相关文档

- [CHANGELOG.md](../CHANGELOG.md) — 版本记录
- [DEPLOYMENT.md](DEPLOYMENT.md) — 部署与回滚
- [DEVELOPMENT.md](DEVELOPMENT.md) — 开发流程
- [CONTRIBUTING.md](../CONTRIBUTING.md) — 贡献规范
