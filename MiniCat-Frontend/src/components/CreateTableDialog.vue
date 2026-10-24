<template>
  <el-dialog v-model="visible" title="新建数据表" width="900px" :close-on-click-modal="false" append-to-body>
    <el-form label-width="90px">
      <el-form-item label="表名">
        <el-input v-model="form.tableName" placeholder="例如：users" clearable />
      </el-form-item>
      <el-form-item label="表注释">
        <el-input v-model="form.tableComment" placeholder="可选" clearable />
      </el-form-item>

      <el-divider content-position="left">列定义</el-divider>

      <el-table :data="form.columns" border size="small" max-height="380">
        <el-table-column label="列名" width="130">
          <template #default="{ row }">
            <el-input v-model="row.columnName" size="small" placeholder="列名" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-select v-model="row.dataType" size="small" style="width: 100%">
              <el-option v-for="type in dataTypes" :key="type" :label="type" :value="type" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="长度" width="80">
          <template #default="{ row }">
            <el-input v-model="row.length" size="small" placeholder="长度" :disabled="!['VARCHAR', 'CHAR', 'DECIMAL'].includes(row.dataType)" />
          </template>
        </el-table-column>
        <el-table-column label="可空" width="60" align="center">
          <template #default="{ row }"><el-checkbox v-model="row.nullable" /></template>
        </el-table-column>
        <el-table-column label="主键" width="60" align="center">
          <template #default="{ row }"><el-checkbox v-model="row.primaryKey" /></template>
        </el-table-column>
        <el-table-column label="自增" width="60" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.autoIncrement" :disabled="row.dataType !== 'INT' && row.dataType !== 'BIGINT'" />
          </template>
        </el-table-column>
        <el-table-column label="默认值" width="110">
          <template #default="{ row }"><el-input v-model="row.defaultValue" size="small" placeholder="默认值" /></template>
        </el-table-column>
        <el-table-column label="注释" min-width="120">
          <template #default="{ row }"><el-input v-model="row.comment" size="small" placeholder="注释" /></template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeColumn($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="add-column">
        <el-button size="small" type="primary" @click="addColumn">
          <el-icon><Plus /></el-icon>
          添加列
        </el-button>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">创建表</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createTable } from '@/api/database'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  connectionId: { type: String, required: true },
  database: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const submitting = ref(false)
const dataTypes = ['INT', 'BIGINT', 'VARCHAR', 'TEXT', 'DECIMAL', 'DOUBLE', 'FLOAT', 'BOOLEAN', 'DATE', 'DATETIME', 'TIMESTAMP', 'CHAR', 'LONGTEXT']

function defaultColumn() {
  return {
    columnName: '',
    dataType: 'VARCHAR',
    nullable: true,
    primaryKey: false,
    autoIncrement: false,
    length: '255',
    defaultValue: '',
    comment: ''
  }
}

const form = reactive({
  tableName: '',
  tableComment: '',
  columns: [
    {
      columnName: 'id',
      dataType: 'INT',
      nullable: false,
      primaryKey: true,
      autoIncrement: true,
      length: null,
      defaultValue: '',
      comment: '主键 ID'
    }
  ]
})

watch(() => props.modelValue, value => {
  if (!value) return
  form.tableName = ''
  form.tableComment = ''
  form.columns = [
    {
      columnName: 'id',
      dataType: 'INT',
      nullable: false,
      primaryKey: true,
      autoIncrement: true,
      length: null,
      defaultValue: '',
      comment: '主键 ID'
    }
  ]
})

function addColumn() {
  form.columns.push(defaultColumn())
}

function removeColumn(index) {
  if (form.columns.length <= 1) {
    ElMessage.warning('至少需要保留一列')
    return
  }
  form.columns.splice(index, 1)
}

async function submit() {
  if (!form.tableName.trim()) {
    ElMessage.warning('请输入表名')
    return
  }
  for (let index = 0; index < form.columns.length; index++) {
    const column = form.columns[index]
    if (!column.columnName || !column.columnName.trim()) {
      ElMessage.warning(`第 ${index + 1} 行的列名不能为空`)
      return
    }
    if (!column.dataType) {
      ElMessage.warning(`第 ${index + 1} 行的数据类型不能为空`)
      return
    }
  }

  submitting.value = true
  try {
    await createTable(props.connectionId, props.database, form)
    ElMessage.success('数据表创建成功')
    emit('saved')
    visible.value = false
  } catch (error) {
    console.error('[create-table] 创建表失败', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.add-column {
  margin-top: 10px;
  text-align: center;
}
</style>
