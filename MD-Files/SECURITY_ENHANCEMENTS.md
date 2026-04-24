# MiniCat 安全性增强说明

## 概述

本次更新为 MiniCat 数据库管理工具添加了重要的安全性增强功能，包括密码加密存储和SQL注入防护。

## 功能特性

### 1. 密码加密（BCrypt）

#### 实现位置
- **工具类**: `PasswordEncryptionService.java`
- **集成位置**: `DatabaseServiceImpl.java`

#### 功能说明
- 使用 BCrypt 强哈希算法对数据库连接密码进行加密
- 自动检测密码是否已加密，避免重复加密
- 支持向后兼容未加密的旧密码
- 密码在存储前自动加密，使用时自动解密验证

#### 使用方法
```java
@Autowired
private PasswordEncryptionService passwordEncryptionService;

// 加密密码
String encrypted = passwordEncryptionService.encrypt("myPassword");

// 验证密码
boolean matches = passwordEncryptionService.matches("myPassword", encrypted);

// 检查是否已加密
boolean isEncrypted = passwordEncryptionService.isEncrypted(password);
```

#### 安全措施
- Controller层返回连接信息时自动移除密码字段
- 密码永远不会以明文形式返回给前端
- 新旧密码自动识别和处理

### 2. SQL注入防护

#### 实现位置
- **工具类**: `SqlInjectionValidator.java`
- **集成位置**: `DatabaseServiceImpl.java`, `DatabaseController.java`

#### 防护机制

##### SELECT查询验证
- 只允许执行SELECT语句
- 检测并阻止以下危险操作：
  - SQL注释注入 (`--`, `/* */`)
  - 多语句执行 (`;`)
  - UNION注入
  - 系统表访问 (`information_schema`, `sys.tables`, `pg_catalog`)
  - 文件操作 (`load_file`, `into outfile`, `into dumpfile`)
  - 时间盲注 (`sleep()`, `benchmark()`, `waitfor delay`)
  - DDL操作 (`DROP`, `ALTER`, `CREATE`, `GRANT`, `REVOKE`)
  - 存储过程执行 (`exec()`, `execute()`, `xp_`, `sp_`)

##### UPDATE/DELETE验证
- 只允许执行UPDATE和DELETE语句
- 强制要求WHERE条件（防止全表误操作）
- 检测并阻止所有SELECT查询中的危险操作
- 额外阻止INSERT注入

#### 使用方法
```java
@Autowired
private SqlInjectionValidator sqlInjectionValidator;

// 验证SELECT查询
SqlValidationResult result = sqlInjectionValidator.validate("SELECT * FROM users WHERE id = 1");
if (!result.isValid()) {
    throw new RuntimeException(result.getMessage());
}

// 验证UPDATE/DELETE查询
SqlValidationResult updateResult = sqlInjectionValidator.validateUpdate("UPDATE users SET name = 'test' WHERE id = 1");
if (!updateResult.isValid()) {
    throw new RuntimeException(updateResult.getMessage());
}
```

### 3. Spring Security配置

#### 实现位置
- **配置类**: `SecurityConfig.java`

#### 功能说明
- 禁用默认的HTTP安全认证（因为这是内部数据库管理工具）
- 仅使用Spring Security的BCrypt密码加密功能
- 允许所有请求通过（API级别的控制在Controller层实现）

## 测试

### 单元测试
创建了完整的安全功能测试套件 `SecurityTests.java`，包括：

1. **密码加密测试**
   - 密码加密功能
   - 密码验证功能
   - 加密状态检测

2. **SQL注入防护测试 - SELECT**
   - 合法SELECT查询通过
   - SQL注释注入拦截
   - UNION注入拦截
   - 非SELECT语句拦截

3. **SQL注入防护测试 - UPDATE/DELETE**
   - 带WHERE条件的UPDATE通过
   - 不带WHERE条件的UPDATE拦截
   - 危险操作拦截
   - 带WHERE条件的DELETE通过
   - 不带WHERE条件的DELETE拦截

### 运行测试
```bash
mvn test -Dtest=SecurityTests
```

## 依赖变更

在 `pom.xml` 中添加了以下依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

## API行为变更

### 获取连接列表
**端点**: `GET /api/database/connections`
- **变更前**: 返回包含明文密码的连接信息
- **变更后**: 返回的连接信息中密码字段为null

### 获取单个连接
**端点**: `GET /api/database/connections/{id}`
- **变更前**: 返回包含明文密码的连接信息
- **变更后**: 返回的连接信息中密码字段为null

### 添加连接
**端点**: `POST /api/database/connections`
- **变更前**: 明文存储密码
- **变更后**: 自动加密密码后存储

### 更新连接
**端点**: `PUT /api/database/connections/{id}`
- **变更前**: 明文存储新密码
- **变更后**: 自动检测并加密新密码

### 执行查询
**端点**: `POST /api/database/connections/{id}/query`
- **变更前**: 直接执行任意SQL
- **变更后**: 先进行SQL注入验证，只允许SELECT查询

### 执行更新
**端点**: `POST /api/database/connections/{id}/update`
- **变更前**: 直接执行任意SQL
- **变更后**: 先进行SQL注入验证，只允许带WHERE条件的UPDATE/DELETE

## 安全建议

1. **生产环境部署**
   - 考虑启用HTTPS
   - 添加API认证和授权机制
   - 实施速率限制
   - 记录所有SQL操作日志

2. **密码管理**
   - 定期轮换数据库密码
   - 使用强密码策略
   - 不要在前端缓存密码

3. **SQL查询**
   - 始终使用参数化查询（未来可以进一步增强）
   - 限制查询结果集大小
   - 设置查询超时时间

4. **访问控制**
   - 限制可访问的IP地址
   - 实施角色-based访问控制
   - 审计所有数据库操作

## 向后兼容性

- ✅ 已有的未加密密码会自动被识别并在使用时正确处理
- ✅ 新增连接时密码自动加密
- ✅ 更新连接时，如果密码改变则自动加密新密码
- ⚠️ API返回的连接信息不再包含密码字段（前端需要适配）

## 故障排除

### 问题：SQL验证失败
**原因**: SQL语句包含被禁止的模式
**解决**: 
- 检查SQL是否包含危险关键字
- 确保SELECT查询不包含DDL/DML操作
- 确保UPDATE/DELETE包含WHERE条件

### 问题：密码验证失败
**原因**: 密码格式不正确或已损坏
**解决**:
- 重新输入正确的密码
- 检查密码是否包含特殊字符
- 确认数据库连接配置正确

## 总结

本次安全性增强为MiniCat提供了：
- 🔒 强密码加密存储（BCrypt）
- 🛡️ 全面的SQL注入防护
- ✅ 完整的单元测试覆盖
- 🔄 向后兼容的实现
- 📝 清晰的API行为变更

这些措施大大提升了系统的安全性，保护了数据库连接凭证和数据安全。
