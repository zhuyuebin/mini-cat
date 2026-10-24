## 变更说明

<!-- 这个 PR 做了什么，为什么需要 -->

## 关联 Issue

<!-- 例如：Closes #123 -->

## 类型

- [ ] 新功能（feat）
- [ ] 缺陷修复（fix）
- [ ] 重构（refactor）
- [ ] 测试（test）
- [ ] 文档（docs）
- [ ] 构建/CI

## 验证方式

<!-- 本地如何验证；贴关键命令输出或截图 -->

```bash
./mvnw -Dskip.installnodenpm -Dskip.npm test
cd MiniCat-Frontend && npm run build
```

## 自检清单

- [ ] 后端测试通过（当前 51 个用例）
- [ ] 涉及前端时 `npm run build` 通过
- [ ] 新行为有对应测试覆盖
- [ ] 涉及接口/配置/行为变化时已更新 `docs/` 与 `CHANGELOG.md`
- [ ] 未提交 `data/`、`target/`、密钥/密码/`.env`
- [ ] 提交信息符合 Conventional Commits

## 影响范围 / 风险

<!-- 是否影响 API、配置、数据安全；是否需要回滚方案 -->
