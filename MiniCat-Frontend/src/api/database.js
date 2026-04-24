import request from '@/utils/request'

// 获取所有连接
export function getAllConnections() {
  return request({
    url: '/database/connections',
    method: 'get'
  })
}

// 获取单个连接
export function getConnection(id) {
  return request({
    url: `/database/connections/${id}`,
    method: 'get'
  })
}

// 添加连接
export function addConnection(data) {
  return request({
    url: '/database/connections',
    method: 'post',
    data
  })
}

// 更新连接
export function updateConnection(id, data) {
  return request({
    url: `/database/connections/${id}`,
    method: 'put',
    data
  })
}

// 删除连接
export function deleteConnection(id) {
  return request({
    url: `/database/connections/${id}`,
    method: 'delete'
  })
}

// 测试连接
export function testConnection(id) {
  return request({
    url: `/database/connections/${id}/test`,
    method: 'post'
  })
}

// 获取数据库列表
export function getDatabases(id) {
  return request({
    url: `/database/connections/${id}/databases`,
    method: 'get'
  })
}

// 获取表列表
export function getTables(id, database) {
  return request({
    url: `/database/connections/${id}/tables`,
    method: 'get',
    params: { database }
  })
}

// 获取列信息
export function getColumns(id, database, table) {
  return request({
    url: `/database/connections/${id}/columns`,
    method: 'get',
    params: { database, table }
  })
}

// 执行查询
export function executeQuery(id, database, sql) {
  return request({
    url: `/database/connections/${id}/query`,
    method: 'post',
    params: { database },
    data: sql,
    headers: {
      'Content-Type': 'text/plain'
    }
  })
}

// 执行更新
export function executeUpdate(id, database, sql) {
  return request({
    url: `/database/connections/${id}/update`,
    method: 'post',
    params: { database },
    data: sql,
    headers: {
      'Content-Type': 'text/plain'
    }
  })
}

// 获取数据库信息
export function getDatabaseInfo(id, database) {
  return request({
    url: `/database/connections/${id}/info`,
    method: 'get',
    params: { database }
  })
}

// 获取连接池状态
export function getConnectionPoolStatus(id, database) {
  return request({
    url: `/database/connections/${id}/pool-status`,
    method: 'get',
    params: { database }
  })
}
