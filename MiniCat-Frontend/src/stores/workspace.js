import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { getAllConnections, getDatabases, getTables } from '@/api/database'
import { getConnectionCapabilities } from '@/utils/capabilities'

const STORAGE_KEY = 'minicat.workspace.v1'
const STORAGE_VERSION = 1
const MAX_TABS = 12

function readStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    console.warn('[workspace] 读取本地工作区状态失败', error)
    return null
  }
}

function emptyTableData() {
  return {
    columns: [],
    columnDefs: [],
    rows: [],
    total: 0,
    loading: false,
    loaded: false,
    error: ''
  }
}

export const useWorkspaceStore = defineStore('workspace', () => {
  const connections = ref([])
  const connectionsLoading = ref(false)
  const connectionsError = ref('')

  const dbCache = ref({})
  const tableCache = ref({})

  const expandedKeys = ref([])
  const tabs = ref([])
  const activeTabId = ref(null)
  const treeSelection = ref(null)
  const pageSizeDefault = ref(20)

  let querySeq = 1
  let persistTimer = null

  const hasConnections = computed(() => connections.value.length > 0)
  const activeTab = computed(() => tabs.value.find(tab => tab.id === activeTabId.value) || null)
  const activeConnection = computed(() => {
    const connectionId = activeTab.value?.connectionId || treeSelection.value?.connectionId
    return connections.value.find(connection => connection.id === connectionId) || null
  })

  function connectionById(id) {
    return connections.value.find(connection => connection.id === id) || null
  }

  function capabilitiesOf(connection) {
    return getConnectionCapabilities(connection?.databaseType)
  }

  function connectionKey(connectionId) {
    return `conn:${connectionId}`
  }

  function databaseKey(connectionId, database) {
    return `db:${connectionId}::${database}`
  }

  function tablesCacheKey(connectionId, database) {
    return `${connectionId}::${database}`
  }

  async function loadConnections() {
    if (connectionsLoading.value) return
    connectionsLoading.value = true
    connectionsError.value = ''
    try {
      const res = await getAllConnections()
      connections.value = res.data || []
    } catch (error) {
      connectionsError.value = '连接列表加载失败'
      console.error('[workspace] 连接列表加载失败', error)
    } finally {
      connectionsLoading.value = false
    }
  }

  async function refreshConnections() {
    dbCache.value = {}
    tableCache.value = {}
    await loadConnections()
  }

  function ensureDbEntry(connectionId) {
    if (!dbCache.value[connectionId]) {
      dbCache.value[connectionId] = { databases: [], loading: false, error: '', loaded: false }
    }
    return dbCache.value[connectionId]
  }

  async function loadDatabases(connectionId, force = false) {
    const connection = connectionById(connectionId)
    if (!connection || !capabilitiesOf(connection).browse) return
    const entry = ensureDbEntry(connectionId)
    if (entry.loading) return
    if (entry.loaded && !force) return
    entry.loading = true
    entry.error = ''
    try {
      const res = await getDatabases(connectionId)
      entry.databases = res.data || []
      entry.loaded = true
    } catch (error) {
      entry.error = '数据库列表加载失败'
      console.error('[workspace] 数据库列表加载失败', error)
    } finally {
      entry.loading = false
    }
  }

  function ensureTableEntry(connectionId, database) {
    const key = tablesCacheKey(connectionId, database)
    if (!tableCache.value[key]) {
      tableCache.value[key] = { tables: [], loading: false, error: '', loaded: false }
    }
    return tableCache.value[key]
  }

  async function loadTables(connectionId, database, force = false) {
    const connection = connectionById(connectionId)
    if (!connection || !capabilitiesOf(connection).browse) return
    const entry = ensureTableEntry(connectionId, database)
    if (entry.loading) return
    if (entry.loaded && !force) return
    entry.loading = true
    entry.error = ''
    try {
      const res = await getTables(connectionId, database)
      entry.tables = res.data || []
      entry.loaded = true
    } catch (error) {
      entry.error = '数据表列表加载失败'
      console.error('[workspace] 表列表加载失败', error)
    } finally {
      entry.loading = false
    }
  }

  function isExpanded(key) {
    return expandedKeys.value.includes(key)
  }

  function setExpanded(key, expanded) {
    const set = new Set(expandedKeys.value)
    if (expanded) set.add(key)
    else set.delete(key)
    expandedKeys.value = Array.from(set)
  }

  function toggleConnection(connection) {
    const key = connectionKey(connection.id)
    const next = !isExpanded(key)
    setExpanded(key, next)
    if (next) {
      treeSelection.value = { connectionId: connection.id, database: null }
      if (capabilitiesOf(connection).browse) loadDatabases(connection.id)
    }
  }

  function toggleDatabase(connection, database) {
    const key = databaseKey(connection.id, database)
    const next = !isExpanded(key)
    setExpanded(key, next)
    treeSelection.value = { connectionId: connection.id, database }
    if (next) loadTables(connection.id, database)
  }

  function setTreeSelection(selection) {
    treeSelection.value = selection
  }

  function tableTabId(connectionId, database, table) {
    return `table:${connectionId}:${database}:${table}`
  }

  function resolveQueryContext(options = {}) {
    if (options.connectionId) {
      return { connectionId: options.connectionId, database: options.database || null }
    }
    if (activeTab.value?.connectionId) {
      return { connectionId: activeTab.value.connectionId, database: activeTab.value.database || null }
    }
    if (treeSelection.value?.connectionId) {
      return { connectionId: treeSelection.value.connectionId, database: treeSelection.value.database || null }
    }
    const first = connections.value[0]
    return first ? { connectionId: first.id, database: null } : null
  }

  function openTableTab({ connectionId, database, table, subTab = null }) {
    const id = tableTabId(connectionId, database, table)
    const existing = tabs.value.find(tab => tab.id === id)
    if (existing) {
      if (subTab) existing.subTab = subTab
      activeTabId.value = id
      treeSelection.value = { connectionId, database }
      return { ok: true, tab: existing, created: false }
    }
    if (tabs.value.length >= MAX_TABS) return { ok: false, reason: 'limit' }
    const tab = {
      id,
      type: 'table',
      title: table,
      connectionId,
      database,
      table,
      subTab: subTab || 'data',
      page: 1,
      pageSize: pageSizeDefault.value,
      ...emptyTableData()
    }
    tabs.value.push(tab)
    activeTabId.value = id
    treeSelection.value = { connectionId, database }
    return { ok: true, tab, created: true }
  }

  function openQueryTab(options = {}) {
    if (tabs.value.length >= MAX_TABS) return { ok: false, reason: 'limit' }
    const context = resolveQueryContext(options)
    if (!context) return { ok: false, reason: 'no-connection' }
    const id = `query:${Date.now()}:${querySeq}`
    const tab = {
      id,
      type: 'query',
      title: `查询 ${querySeq++}`,
      connectionId: context.connectionId,
      database: context.database,
      sqlText: '',
      executing: false,
      result: null,
      error: '',
      affectedRows: null
    }
    tabs.value.push(tab)
    activeTabId.value = id
    if (context.database) {
      treeSelection.value = { connectionId: context.connectionId, database: context.database }
    }
    return { ok: true, tab, created: true }
  }

  function activateTab(id) {
    const tab = tabs.value.find(item => item.id === id)
    if (!tab) return
    activeTabId.value = id
    treeSelection.value = { connectionId: tab.connectionId, database: tab.database || null }
  }

  function closeTab(id) {
    const index = tabs.value.findIndex(tab => tab.id === id)
    if (index === -1) return
    const wasActive = activeTabId.value === id
    tabs.value.splice(index, 1)
    if (wasActive) {
      const next = tabs.value[index] || tabs.value[index - 1] || null
      activeTabId.value = next ? next.id : null
      if (next) {
        treeSelection.value = { connectionId: next.connectionId, database: next.database || null }
      }
    }
  }

  function closeTabsForConnection(connectionId) {
    tabs.value = tabs.value.filter(tab => tab.connectionId !== connectionId)
    if (!tabs.value.some(tab => tab.id === activeTabId.value)) {
      activeTabId.value = tabs.value[0]?.id || null
    }
  }

  function updateTab(id, patch) {
    const tab = tabs.value.find(item => item.id === id)
    if (tab) Object.assign(tab, patch)
  }

  function resetTabs() {
    tabs.value = []
    activeTabId.value = null
  }

  function clearPersistedState() {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch (error) {
      /* ignore */
    }
    resetTabs()
    expandedKeys.value = []
    treeSelection.value = null
    dbCache.value = {}
    tableCache.value = {}
  }

  function serialize() {
    const persistedTabs = tabs.value.map(tab => tab.type === 'table'
      ? {
          id: tab.id,
          type: tab.type,
          title: tab.title,
          connectionId: tab.connectionId,
          database: tab.database,
          table: tab.table,
          subTab: tab.subTab,
          page: tab.page,
          pageSize: tab.pageSize
        }
      : {
          id: tab.id,
          type: tab.type,
          title: tab.title,
          connectionId: tab.connectionId,
          database: tab.database,
          sqlText: tab.sqlText
        })
    return {
      version: STORAGE_VERSION,
      tabs: persistedTabs,
      activeTabId: activeTabId.value,
      expandedKeys: expandedKeys.value,
      treeSelection: treeSelection.value,
      pageSizeDefault: pageSizeDefault.value,
      querySeq
    }
  }

  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(serialize()))
    } catch (error) {
      /* ignore */
    }
  }

  function schedulePersist() {
    if (persistTimer) clearTimeout(persistTimer)
    persistTimer = setTimeout(() => {
      persistTimer = null
      persist()
    }, 300)
  }

  watch([tabs, activeTabId, expandedKeys, treeSelection, pageSizeDefault], schedulePersist, { deep: true })

  /**
   * 启动入口：加载连接 → 恢复持久化的工作区状态（只恢复外壳，不自动拉表数据）。
   */
  async function init() {
    await loadConnections()
    const saved = readStorage()
    if (!saved || saved.version !== STORAGE_VERSION) return { droppedTabs: 0 }

    const existingIds = new Set(connections.value.map(connection => connection.id))
    let droppedTabs = 0
    const restoredTabs = []
    for (const tab of Array.isArray(saved.tabs) ? saved.tabs : []) {
      if (!tab || !existingIds.has(tab.connectionId)) {
        droppedTabs += 1
        continue
      }
      if (tab.type === 'table') {
        restoredTabs.push({ ...tab, ...emptyTableData() })
      } else if (tab.type === 'query') {
        restoredTabs.push({ ...tab, result: null, executing: false, error: '' })
      }
    }

    tabs.value = restoredTabs
    activeTabId.value = restoredTabs.some(tab => tab.id === saved.activeTabId)
      ? saved.activeTabId
      : (restoredTabs[0]?.id || null)
    expandedKeys.value = (Array.isArray(saved.expandedKeys) ? saved.expandedKeys : []).filter(key => {
      const connectionId = key.startsWith('conn:') ? key.slice(5) : key.slice(3).split('::')[0]
      return existingIds.has(connectionId)
    })
    treeSelection.value = saved.treeSelection && existingIds.has(saved.treeSelection.connectionId)
      ? saved.treeSelection
      : null
    pageSizeDefault.value = Number(saved.pageSizeDefault) || 20
    const maxQueryNumber = restoredTabs
      .filter(tab => tab.type === 'query')
      .reduce((max, tab) => Math.max(max, Number(String(tab.title).replace(/\D/g, '')) || 0), 0)
    querySeq = Math.max(Number(saved.querySeq) || 1, maxQueryNumber + 1, 1)

    return { droppedTabs }
  }

  return {
    connections,
    connectionsLoading,
    connectionsError,
    dbCache,
    tableCache,
    expandedKeys,
    tabs,
    activeTabId,
    treeSelection,
    pageSizeDefault,
    hasConnections,
    activeTab,
    activeConnection,
    connectionById,
    capabilitiesOf,
    connectionKey,
    databaseKey,
    loadConnections,
    refreshConnections,
    loadDatabases,
    loadTables,
    isExpanded,
    toggleConnection,
    toggleDatabase,
    setTreeSelection,
    openTableTab,
    openQueryTab,
    activateTab,
    closeTab,
    closeTabsForConnection,
    updateTab,
    resetTabs,
    clearPersistedState,
    init
  }
})
