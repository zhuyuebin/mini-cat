# MiniCat 数据增删改功能说明

## 功能概述

MiniCat 现已完整支持数据库的增删改查（CRUD）操作，除了原有的查询功能外，新增了以下数据修改功能：

### 1. SQL编辑器中的增删改功能（Query.vue）

#### 功能特性
- **执行查询按钮**：执行 SELECT 语句，查看数据
- **执行更新按钮**：执行 INSERT、UPDATE、DELETE 语句，修改数据
- **危险操作警告**：对于 DELETE、DROP、TRUNCATE 等危险操作，会弹出确认对话框
- **结果显示**：显示受影响的行数

#### 使用方法

1. **插入数据（INSERT）**
```sql
INSERT INTO users (name, email, age) VALUES ('张三', 'zhangsan@example.com', 25)
```

2. **更新数据（UPDATE）**
```sql
UPDATE users SET age = 26 WHERE name = '张三'
```

3. **删除数据（DELETE）**
```sql
DELETE FROM users WHERE id = 1
```

4. **批量操作**
```sql
-- 批量插入
INSERT INTO users (name, email) VALUES 
  ('李四', 'lisi@example.com'),
  ('王五', 'wangwu@example.com');

-- 批量更新
UPDATE users SET status = 'active' WHERE created_at > '2024-01-01';

-- 批量删除
DELETE FROM logs WHERE created_at < '2023-01-01';
```

### 2. 表格视图中的数据编辑功能（Tables.vue）

#### 功能特性
- **新增行**：通过表单界面添加新数据行
- **删除行**：直接删除表格中的某一行数据
- **自定义SQL**：在表格视图中执行自定义SQL语句
- **主键保护**：删除操作自动使用主键作为WHERE条件，确保精确删除

#### 使用方法

##### 新增数据行
1. 选择数据库连接和数据库
2. 从左侧表列表中选择要操作的表
3. 切换到"表数据"标签页
4. 点击工具栏的"新增行"按钮
5. 在弹出的对话框中填写字段值
   - 必填字段会标记为"必填"
   - 主键字段会标记为"主键"
   - 显示字段的数据类型
6. 点击"确定"提交

##### 删除数据行
1. 在"表数据"标签页中查看数据
2. 找到要删除的行
3. 点击该行右侧的"删除"按钮
4. 在确认对话框中点击"确定"
5. 系统会自动使用主键构建安全的DELETE语句

##### 执行自定义SQL
1. 点击工具栏的"自定义SQL"按钮
2. 在弹出的对话框中输入SQL语句
3. 支持 SELECT、INSERT、UPDATE、DELETE 等操作
4. 点击"执行"按钮
5. 如果是SELECT查询，结果会直接显示在表格中

### 3. 安全特性

#### SQL注入防护
- 所有SQL语句都经过严格的注入检测
- 禁止使用危险的SQL操作（如 xp_、sp_、exec等）
- 禁止访问系统表（information_schema、sys.tables等）
- 禁止时间盲注攻击（sleep、benchmark等）

#### 操作保护
- UPDATE/DELETE 语句必须包含 WHERE 条件
- 危险操作（DELETE、DROP、TRUNCATE）需要二次确认
- 删除操作自动使用主键，防止误删

#### 错误处理
- 详细的错误提示信息
- SQL执行失败时显示具体原因
- 前端友好的错误提示

### 4. 使用场景示例

#### 测试数据生成
```sql
-- 批量生成测试用户
INSERT INTO users (name, email, age, status) VALUES
  ('测试用户1', 'test1@example.com', 20, 'active'),
  ('测试用户2', 'test2@example.com', 25, 'active'),
  ('测试用户3', 'test3@example.com', 30, 'inactive'),
  ('测试用户4', 'test4@example.com', 35, 'active'),
  ('测试用户5', 'test5@example.com', 40, 'active');
```

#### 数据清理
```sql
-- 删除过期的日志记录
DELETE FROM logs WHERE created_at < '2023-01-01';

-- 清空测试数据
DELETE FROM test_data WHERE is_test = 1;
```

#### 数据修正
```sql
-- 批量更新状态
UPDATE orders SET status = 'cancelled' 
WHERE status = 'pending' AND created_at < '2024-01-01';

-- 修正错误数据
UPDATE products SET price = 99.99 WHERE id = 12345;
```

### 5. 注意事项

1. **备份重要数据**：在执行大量删除或更新操作前，建议先备份数据
2. **使用WHERE条件**：UPDATE和DELETE务必添加WHERE条件，避免影响全部数据
3. **事务支持**：当前版本每条SQL独立执行，复杂操作建议在数据库中手动管理事务
4. **性能考虑**：批量操作时注意数据量，避免一次性操作过多数据
5. **权限控制**：确保数据库用户有相应的INSERT、UPDATE、DELETE权限

### 6. 技术实现

#### 后端
- `DatabaseController.java`：提供 `/api/database/connections/{id}/update` 接口
- `DatabaseServiceImpl.java`：实现 executeUpdate 方法，支持INSERT/UPDATE/DELETE
- `SqlInjectionValidator.java`：提供三种验证方法
  - `validate()`：验证SELECT查询
  - `validateUpdate()`：验证UPDATE/DELETE语句
  - `validateInsert()`：验证INSERT语句

#### 前端
- `Query.vue`：SQL编辑器页面，支持手动编写和执行SQL
- `Tables.vue`：表格浏览页面，提供可视化的增删改操作
- `database.js`：API调用封装，包含 executeUpdate 方法

### 7. 后续优化建议

1. **事务支持**：添加事务管理，支持多条SQL作为一个事务执行
2. **数据导入导出**：支持CSV、Excel格式的数据导入导出
3. **SQL历史记录**：保存执行过的SQL语句，方便重复使用
4. **批量编辑**：在表格中直接编辑多个单元格后统一提交
5. **数据验证**：在前端添加数据类型验证和约束检查
6. **操作日志**：记录所有的数据修改操作，便于审计

---

**开发完成时间**：2026-04-25  
**版本**：v1.1.0
