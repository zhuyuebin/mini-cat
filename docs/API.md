# MiniCat REST API 参考

> 适用版本：v1.2.0（P0 前端重构 + P1-1 内联编辑）
> 基础路径：`/api/database`（下文省略该前缀）
> 在线调试：启动应用后可直接用浏览器/curl 访问 `http://localhost:8888/api/database/...`

---

## 1. 通用约定

### 1.1 响应封装

所有接口返回统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "success": true
}
```

- `code`：业务码，成功恒为 `200`；
- `message`：成功为 `success`，失败为可读的中文错误信息；
- `data`：业务数据，可能为 `null`、对象、数组或标量；
- `success`：仅用于兼容旧前端逻辑，**判断成功请以 HTTP 状态码 + `code` 为准**。

### 1.2 HTTP 状态码与业务码

失败时 HTTP 状态码与业务 `code` 一起下发（详见 [错误码](#2-错误码)）。前端 Axios 拦截器同时兼容
「语义化 HTTP 状态码」与旧版「HTTP 200 + success=false」两种形态。

### 1.3 Content-Type

| 接口类型 | 请求体 |
|---|---|
| 大多数 JSON 接口 | `application/json` |
| `POST .../query`、`POST .../update` | `text/plain`（请求体为原始 SQL 文本） |
| `POST .../import-data` | `multipart/form-data`（字段名 `file`） |

### 1.4 连接模型字段

`DatabaseConnection`（GET 接口会屏蔽 `password`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | string | 连接 ID（UUID） |
| `name` | string | 连接显示名称 |
| `host` | string | 主机 |
| `port` | int | 端口 |
| `username` | string | 用户名 |
| `password` | string | 密码（请求时明文；响应中为 `null`） |
| `databaseType` | string | `mysql` / `postgresql` |
| `databaseName` | string | 默认数据库（可空，实际以界面选择为准） |
| `charset` | string | 字符集（MySQL 常用 `utf8mb4`） |
| `active` | boolean | 是否启用 |
| `connectionStatus` | string | `success` / `failed` / `unknown` |
| `lastTestTime` | string | 最近一次测试时间 |
| `lastTestMessage` | string | 最近一次测试结果信息 |

---

## 2. 错误码

| code | HTTP | 含义 |
|---|---|---|
| 200 | 200 | 成功 |
| 40001 | 400 | 参数校验失败（含表名/列名非法、请求体不完整） |
| 40301 | 403 | SQL 被安全策略拦截 |
| 40401 | 404 | 连接不存在 |
| 40402 | 404 | 数据行不存在或已被删除 |
| 40901 | 409 | 元数据实体乐观锁冲突 |
| 40902 | 409 | 数据行已被修改（单元格编辑乐观冲突，请刷新） |
| 50001 | 500 | SQL 执行失败（含完整性约束冲突、语法错误） |
| 50002 | 500 | 数据库连接失败 |
| 50000 | 500 | 内部错误 |

---

## 3. 连接管理

### 3.1 获取所有连接

```http
GET /connections
```

返回 `data`: `DatabaseConnection[]`（不含密码）。

### 3.2 获取单个连接

```http
GET /connections/{id}
```

- `404/40401`：连接不存在。

### 3.3 新增连接

```http
POST /connections
Content-Type: application/json

{
  "name": "本地 MySQL",
  "databaseType": "mysql",
  "host": "127.0.0.1",
  "port": 3306,
  "username": "root",
  "password": "your-password",
  "charset": "utf8mb4",
  "databaseName": "demo",
  "active": true
}
```

返回 `data`: 已保存的连接。密码在服务端使用 AES-256-GCM 加密后落库；
**响应中的 `password` 可能是密文字符串，请勿依赖该字段，也不要记录日志。**

### 3.4 更新连接

```http
PUT /connections/{id}
Content-Type: application/json
```

请求体同新增。**密码字段留空或为 `null` 表示不修改密码**。
更新成功会关闭该连接对应的全部连接池，下次访问使用新配置重建。

- `404/40401`：连接不存在。

### 3.5 删除连接

```http
DELETE /connections/{id}
```

删除连接并回收该连接的所有连接池；前端同时关闭对应工作区 Tab。

- `404/40401`：连接不存在。

### 3.6 测试连接

```http
POST /connections/{id}/test
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "message": "Connection successful",
    "host": "127.0.0.1",
    "port": 3306,
    "database": "demo"
  },
  "success": true
}
```

- `404/40401`：连接不存在；
- `500/50002`：连接测试失败。

> PostgreSQL 目前仅支持本接口；库表浏览/查询仍按 MySQL 方言，见 [ROADMAP](ROADMAP.md)。

---

## 4. 元数据浏览

### 4.1 数据库列表

```http
GET /connections/{id}/databases
```

返回 `data`: `string[]`（包含系统库，由前端按需过滤）。

### 4.2 数据表列表

```http
GET /connections/{id}/tables?database={db}
```

返回 `data`: `TableInfoDTO[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `tableName` | string | 表名 |
| `tableComment` | string | 表注释（可能为空） |
| `rowCount` | int | 行数（`SELECT COUNT(*)`，大表可能较慢） |
| `createTime` | string | 创建时间（部分数据库驱动为 `null`） |
| `updateTime` | string | 更新时间（部分数据库驱动为 `null`） |

### 4.3 列信息

```http
GET /connections/{id}/columns?database={db}&table={tbl}
```

返回 `data`: `ColumnInfoDTO[]`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `columnName` | string | 列名 |
| `dataType` | string | 类型名（JDBC `TYPE_NAME`，如 `VARCHAR`、`INT`、`DATETIME`） |
| `nullable` | boolean | 是否可空 |
| `columnKey` | string | `PRI`（主键）/ `UNI`（行标识唯一索引）/ `null` |
| `defaultValue` | string | 默认值 |
| `extra` | string | 驱动附加信息（`IS_AUTOINCREMENT` 原始值） |
| `autoIncrement` | boolean | 是否自增 |
| `rowIdentityPart` | boolean | 是否为行标识列（主键，或无主键时第一个 `UNIQUE NOT NULL` 索引的列） |
| `columnComment` | string | 列注释 |

> `rowIdentityPart` 是单元格编辑/按行删除定位一行的依据。整表没有行标识列时前端为只读。

---

## 5. 表数据

### 5.1 服务端分页读取

```http
GET /connections/{id}/table-data?database={db}&table={tbl}&page=1&size=50
```

参数与约束：

| 参数 | 默认 | 约束 |
|---|---|---|
| `page` | `1` | 小于等于 0 按 1 处理 |
| `size` | `50` | 小于等于 0 按 50；最终 clamp 到 `10 ~ 200` |

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "columns": ["id", "username", "beizhu"],
    "rows": [
      { "id": 1, "username": "zhangsan1", "beizhu": "无" }
    ],
    "page": 1,
    "size": 50,
    "total": 100
  },
  "success": true
}
```

- `columns` 是字符串列名，`rows` 是 `列名 -> 值` 的行对象；
- `total` 由服务端 `COUNT(*)` 得到，前端分页器直接使用。

### 5.2 单行/单元格更新（P1-1 新增）

```http
PUT /connections/{id}/table-row?database={db}
Content-Type: application/json

{
  "table": "user",
  "keys":     { "id": 1 },
  "changes":  { "username": "zhangsan2", "beizhu": null },
  "expected": { "username": "zhangsan1" }
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `table` | 是 | 表名（服务端白名单校验：仅字母/数字/`_`/`$`） |
| `keys` | 是 | 行标识列 → 值，必须覆盖服务端判定的**全部**行标识列 |
| `changes` | 是 | 要修改的列 → 新值，不能为空 |
| `expected` | 否 | 列 → 旧值，乐观冲突检测（CAS）；只能引用 `changes` 中的列 |

服务端行为：

1. 读取列元数据，确定行标识列（主键优先，无主键回退 `UNIQUE NOT NULL` 唯一索引）；
2. 校验表/列存在、标识符合法、类型可编辑；
3. 生成参数化 SQL：
   `UPDATE \`t\` SET \`c\` = ? WHERE \`k\` = ? [AND \`c\` = ? | AND \`c\` IS NULL]`；
4. 更新 `0` 行时：行仍存在 → `409/40902` 冲突；行不存在 → `404/40402`；
5. 成功后回查整行返回。

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "affectedRows": 1,
    "row": { "id": 1, "username": "zhangsan2", "beizhu": null }
  },
  "success": true
}
```

约束：

- 行标识列、自增列、`BLOB`/`BINARY`/`BIT`/`JSON`/空间类型不允许修改（`40001`）；
- 普通 NULL 与空字符串区分：`changes` 传 `null` 表示写入 SQL NULL；
- `expected` 旧值为 `null` 时服务端生成 `IS NULL`（避免 `col = NULL` 恒假导致误判冲突）；
- 值全部走 `PreparedStatement` 参数绑定，不拼接用户输入；
- 目前 SQL 方言为 MySQL（反引号），PostgreSQL 行编辑待 P0-4 适配。

---

## 6. SQL 执行

### 6.1 执行查询（仅 SELECT）

```http
POST /connections/{id}/query?database={db}
Content-Type: text/plain

SELECT * FROM `user` LIMIT 100
```

返回 `data`: `QueryResultDTO`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `columns` | string[] | 结果列名 |
| `rows` | object[] | 结果行 |
| `totalRows` | int | 返回行数 |
| `executionTime` | long | 执行耗时（毫秒） |

- `403/40301`：被 SQL 安全校验拦截；
- `500/50001`：执行失败。

> 查询结果一次性加载到内存，暂无行数上限与执行超时，大表查询请自行加 `LIMIT`。

### 6.2 执行更新（INSERT / UPDATE / DELETE / DROP TABLE）

```http
POST /connections/{id}/update?database={db}
Content-Type: text/plain

UPDATE `user` SET `beizhu` = '手动修改' WHERE `id` = 1
```

返回 `data`: 受影响行数（`int`）。

- 仅允许 `INSERT` / `UPDATE` / `DELETE` / `DROP TABLE`；
- `UPDATE` / `DELETE` 必须包含 `WHERE`；
- `DROP TABLE` 禁止多语句；
- `403/40301`：被安全校验拦截；`500/50001`：执行失败。

---

## 7. 表操作

### 7.1 创建数据表

```http
POST /connections/{id}/create-table?database={db}
Content-Type: application/json

{
  "tableName": "demo_user",
  "tableComment": "示例表",
  "columns": [
    { "columnName": "id", "dataType": "INT", "nullable": false, "primaryKey": true, "autoIncrement": true, "comment": "主键" },
    { "columnName": "name", "dataType": "VARCHAR", "length": 64, "nullable": true, "comment": "姓名" }
  ]
}
```

返回 `data`: `true`。表名/列名非法或列为空时返回 `400/40001`。

### 7.2 批量导入 CSV / Excel

```http
POST /connections/{id}/import-data?database={db}&table={tbl}
Content-Type: multipart/form-data

file=@data.csv
```

返回 `data`: `ImportResult`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `success` | boolean | 是否整体成功 |
| `totalRows` | int | 数据总行数（不含表头） |
| `successRows` | int | 成功行数 |
| `failedRows` | int | 失败行数 |
| `message` | string | 结果描述 |

要求：Excel（`.xlsx`/`.xls`）或 CSV，单文件 ≤ 50MB，第一行表头必须与表的所有列名完全一致；
批量插入失败整体回滚。

### 7.3 删除数据表

```http
DELETE /connections/{id}/tables?database={db}&table={tbl}
```

返回 `data`: `true`。

---

## 8. 监控与信息

### 8.1 数据库信息

```http
GET /connections/{id}/info?database={db}
```

返回 `data` 包含 `databaseProduct`、`databaseVersion`、`driverName` 等驱动信息。

### 8.2 连接池状态

```http
GET /connections/{id}/pool-status?database={db}
```

返回该「连接配置 + 目标数据库」HikariCP 池的活跃/空闲/总连接数等状态。

---

## 9. 前端功能开关

### 9.1 获取 UI 设置

```http
GET /settings
```

```json
{
  "code": 200,
  "message": "success",
  "data": { "inlineEditEnabled": true },
  "success": true
}
```

对应配置项 `minicat.ui.inline-edit-enabled`（默认 `true`）。关闭后前端表格为只读模式。

---

## 10. cURL 速查

```bash
BASE=http://localhost:8888/api/database

# 连接
curl $BASE/connections
curl -X POST $BASE/connections -H 'Content-Type: application/json' -d @conn.json
curl -X POST $BASE/connections/$ID/test
curl -X DELETE $BASE/connections/$ID

# 浏览
curl "$BASE/connections/$ID/databases"
curl "$BASE/connections/$ID/tables?database=usertest"
curl "$BASE/connections/$ID/columns?database=usertest&table=user"
curl "$BASE/connections/$ID/table-data?database=usertest&table=user&page=1&size=20"

# 单元格更新（带旧值 CAS）
curl -X PUT "$BASE/connections/$ID/table-row?database=usertest" \
  -H 'Content-Type: application/json' \
  -d '{"table":"user","keys":{"id":1},"changes":{"username":"zhangsan2"},"expected":{"username":"zhangsan1"}}'

# SQL
curl -X POST "$BASE/connections/$ID/query?database=usertest" \
  -H 'Content-Type: text/plain' --data-binary 'SELECT * FROM `user` LIMIT 10'
curl -X POST "$BASE/connections/$ID/update?database=usertest" \
  -H 'Content-Type: text/plain' --data-binary "UPDATE \`user\` SET \`beizhu\`='x' WHERE \`id\`=1"

# 前端设置
curl $BASE/settings
```
