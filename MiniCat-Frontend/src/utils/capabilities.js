/**
 * 连接类型能力矩阵。
 *
 * 前端所有「能不能浏览 / 能不能查询」的判断都集中在这里，
 * 后续后端提供能力接口时只改这一处。
 */
const DEFAULT_CAPABILITIES = {
  browse: false,
  query: false,
  ddl: false,
  label: '未知类型',
  badge: '?',
  color: '#909399',
  hint: '暂不支持该数据库类型的浏览能力'
}

const CAPABILITIES = {
  mysql: {
    browse: true,
    query: true,
    ddl: true,
    label: 'MySQL',
    badge: 'MySQL',
    color: '#409eff',
    hint: ''
  },
  postgresql: {
    browse: false,
    query: false,
    ddl: false,
    label: 'PostgreSQL',
    badge: 'PG',
    color: '#67c23a',
    hint: 'PostgreSQL 浏览能力开发中（当前仅支持连接测试）'
  }
}

export function getConnectionCapabilities(databaseType) {
  const key = String(databaseType || '').toLowerCase()
  return CAPABILITIES[key] || DEFAULT_CAPABILITIES
}

export default CAPABILITIES
