# MiniCat 文档索引

> 根目录 [README.md](../README.md) 是项目门面；本页是文档地图。
> 文档原则：与当前代码一致、命令可跑、不写真实密码/密钥。

---

## 1. 按角色阅读

| 我是… | 建议顺序 |
|---|---|
| 第一次接触 MiniCat | [README](../README.md) → [GETTING_STARTED](GETTING_STARTED.md) → [USER_GUIDE](USER_GUIDE.md) |
| 想部署到 Docker/服务器 | [DEPLOYMENT](DEPLOYMENT.md) → [CONFIGURATION](CONFIGURATION.md) → [SECURITY](../SECURITY.md) |
| 要对接/调试 API | [API](API.md) → [ARCHITECTURE](ARCHITECTURE.md) |
| 想参与开发/提交 PR | [CONTRIBUTING](../CONTRIBUTING.md) → [DEVELOPMENT](DEVELOPMENT.md) → [ARCHITECTURE](ARCHITECTURE.md) |
| 维护者/发布者 | [RELEASING](RELEASING.md) → [CHANGELOG](../CHANGELOG.md) → [ROADMAP](ROADMAP.md) |
| 关注安全边界 | [SECURITY](../SECURITY.md) |

---

## 2. 文档清单

| 文档 | 内容 | 适合谁 |
|---|---|---|
| [README.md](../README.md) | 项目简介、特性、快速开始、文档导航 | 所有人 |
| [GETTING_STARTED.md](GETTING_STARTED.md) | 前置要求、四种启动方式、首次使用、常见问题 | 新用户 |
| [USER_GUIDE.md](USER_GUIDE.md) | 资源树、多 Tab、表工作区、内联编辑、SQL、导入、快捷键 | 使用者 |
| [API.md](API.md) | 19 个 REST 接口、参数、响应、错误码、curl 示例 | 开发者/集成方 |
| [CONFIGURATION.md](CONFIGURATION.md) | 配置项、环境变量、Profile、`data/` 持久化、Docker 配置 | 使用者/运维 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 架构、代码结构、连接池、加密、行更新、前端状态、设计决策 | 开发者 |
| [DEPLOYMENT.md](DEPLOYMENT.md) | JAR、systemd、Docker、反代、K8s、备份升级 | 运维 |
| [DEVELOPMENT.md](DEVELOPMENT.md) | 环境、分支、测试分层、CI、代码约定、开发坑 | 贡献者 |
| [RELEASING.md](RELEASING.md) | 版本约定、发布检查清单、GitHub/Gitee 双远程同步 | 维护者 |
| [ROADMAP.md](ROADMAP.md) | 已完里程碑、下一步（P0-4/P1-2/M2）、已知限制 | 所有人 |
| [design/UI_REFACTOR.md](design/UI_REFACTOR.md) | P0/P1 前端信息架构设计契约与实现记录 | 前端开发者 |
| [../SECURITY.md](../SECURITY.md) | 安全模型、威胁边界、加固清单、漏洞报告 | 部署/安全 |
| [../CHANGELOG.md](../CHANGELOG.md) | 版本更新日志（Keep a Changelog） | 所有人 |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | 贡献流程、PR 要求、行为准则 | 贡献者 |

---

## 3. 文档维护规则

- 功能/接口/配置变更时，同步修改对应文档与 `CHANGELOG.md`；
- 已过时的历史文档直接删除或合并，不保留“历史版本副本”（Git 历史可查）；
- 链接使用相对路径，保证 GitHub/Gitee 与本地都可打开；
- 发布前按 [RELEASING.md](RELEASING.md) 的清单检查文档中的版本号、接口数量、测试数量。
