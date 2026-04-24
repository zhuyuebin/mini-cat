# 贡献指南

感谢你考虑为 MiniCat 做出贡献！

## 🎯 行为准则

请尊重所有参与者,共同维护友好的社区环境。

## 📝 如何贡献

### 报告 Bug

1. 在 [Issues](https://gitee.com/your-username/MiniCat/issues) 中搜索是否已有类似问题
2. 如果没有,创建一个新的 Issue
3. 提供详细信息:
   - 问题描述
   - 复现步骤
   - 预期行为
   - 实际行为
   - 截图(如果适用)
   - 环境信息(OS, Java版本等)

### 提出新功能

1. 先在 Issues 中讨论你的想法
2. 说明功能的必要性和使用场景
3. 等待社区反馈和维护者确认

### 提交代码

#### 1. Fork 仓库

点击 Gitee 页面右上角的 "Fork" 按钮

#### 2. 克隆仓库

```bash
git clone https://gitee.com/your-username/MiniCat.git
cd MiniCat
```

#### 3. 创建分支

```bash
git checkout -b feature/your-feature-name
# 或
git checkout -b fix/your-bug-fix
```

分支命名规范:
- `feature/xxx` - 新功能
- `fix/xxx` - Bug修复
- `docs/xxx` - 文档更新
- `refactor/xxx` - 代码重构
- `test/xxx` - 测试相关

#### 4. 进行修改

遵循以下规范:

**代码风格**:
- 使用 4 个空格缩进
- 类名使用 PascalCase
- 方法和变量使用 camelCase
- 常量使用 UPPER_SNAKE_CASE

**提交信息**:
```
类型: 简短描述

详细描述(可选)

相关Issue: #123
```

类型包括:
- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具变动

示例:
```
feat: 添加数据导出功能

支持将查询结果导出为CSV和Excel格式

相关Issue: #45
```

#### 5. 测试你的修改

```bash
# 运行后端测试
mvn test

# 构建前端
cd MiniCat-Frontend
npm run build
```

#### 6. 提交更改

```bash
git add .
git commit -m "feat: 添加xxx功能"
```

#### 7. 推送到 Gitee

```bash
git push origin feature/your-feature-name
```

#### 8. 创建 Pull Request

1. 访问你的 Fork 仓库
2. 点击 "Pull Requests" -> "新建 Pull Request"
3. 填写 PR 描述
4. 关联相关 Issue
5. 提交 PR

### PR 要求

- ✅ 代码符合项目规范
- ✅ 添加了必要的测试
- ✅ 更新了相关文档
- ✅ 通过了 CI 检查
- ✅ 没有合并冲突

## 💻 开发环境设置

### 后端开发

```bash
# 克隆仓库
git clone https://gitee.com/your-username/MiniCat.git
cd MiniCat

# 导入到IDE (IntelliJ IDEA / Eclipse)
# 使用 Maven 构建
mvn clean install

# 运行应用
mvn spring-boot:run
```

### 前端开发

```bash
cd MiniCat-Frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build
```

## 📋 代码审查流程

1. 维护者会审查你的 PR
2. 可能会提出修改建议
3. 根据反馈更新代码
4. 审查通过后合并

## 🎨 设计规范

### UI/UX

- 遵循 Element Plus 设计规范
- 保持界面简洁一致
- 考虑响应式布局
- 提供良好的用户反馈

### API 设计

- RESTful 风格
- 统一的响应格式
- 合理的错误码
- 完善的文档注释

## 📖 文档规范

- 使用 Markdown 格式
- 中文为主,关键术语可保留英文
- 代码块标明语言类型
- 添加必要的截图和示例

## ❓ 需要帮助?

- 查看 [README.md](README.md)
- 查看 [DEPLOYMENT.md](DEPLOYMENT.md)
- 在 Issues 中提问
- 联系维护者

## 🙏 致谢

感谢所有为 MiniCat 做出贡献的开发者!

---

再次感谢你的贡献! 🎉
