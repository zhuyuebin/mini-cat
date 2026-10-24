<template>
  <el-dialog
    v-model="visible"
    :title="edit ? '编辑连接' : '新建连接'"
    width="560px"
    :close-on-click-modal="false"
    append-to-body
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="连接名称" prop="name">
        <el-input v-model="form.name" placeholder="例如：本地 MySQL" clearable />
      </el-form-item>

      <el-form-item label="数据库类型" prop="databaseType">
        <el-select v-model="form.databaseType" style="width: 100%" @change="onTypeChange">
          <el-option label="MySQL" value="mysql" />
          <el-option label="PostgreSQL" value="postgresql" />
        </el-select>
        <div v-if="form.databaseType === 'postgresql'" class="type-hint">
          PostgreSQL 当前仅支持连接测试，库表浏览与查询能力开发中
        </div>
      </el-form-item>

      <el-form-item label="主机" prop="host">
        <el-input v-model="form.host" placeholder="例如：127.0.0.1" clearable />
      </el-form-item>

      <el-form-item label="端口" prop="port">
        <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" controls-position="right" />
      </el-form-item>

      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="数据库用户名" clearable />
      </el-form-item>

      <el-form-item label="密码" prop="password" :rules="passwordRules">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          :placeholder="edit ? '留空表示不修改密码' : '请输入密码'"
        />
      </el-form-item>

      <el-form-item label="字符集">
        <el-input v-model="form.charset" placeholder="例如：utf8mb4" clearable />
      </el-form-item>

      <el-form-item label="启用">
        <el-switch v-model="form.active" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addConnection, updateConnection } from '@/api/database'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  connection: { type: Object, default: null },
  edit: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref(null)
const submitting = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

function defaultForm() {
  return {
    id: '',
    name: '',
    databaseType: 'mysql',
    host: '127.0.0.1',
    port: 3306,
    username: '',
    password: '',
    charset: 'utf8mb4',
    active: true
  }
}

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
  databaseType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const passwordRules = computed(() => (props.edit
  ? []
  : [{ required: true, message: '请输入密码', trigger: 'blur' }]))

watch(() => props.modelValue, value => {
  if (!value) return
  Object.assign(form, defaultForm())
  if (props.edit && props.connection) {
    Object.assign(form, {
      id: props.connection.id || '',
      name: props.connection.name || '',
      databaseType: props.connection.databaseType || 'mysql',
      host: props.connection.host || '',
      port: props.connection.port || 3306,
      username: props.connection.username || '',
      password: '',
      charset: props.connection.charset || 'utf8mb4',
      active: props.connection.active !== false
    })
  }
  if (formRef.value) formRef.value.clearValidate()
})

function onTypeChange(type) {
  if (props.edit) return
  if (form.port === 3306 || form.port === 5432) {
    form.port = type === 'postgresql' ? 5432 : 3306
  }
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      ...form,
      password: form.password ? form.password : null
    }
    if (props.edit) {
      await updateConnection(form.id, payload)
      ElMessage.success('连接已更新')
    } else {
      delete payload.id
      await addConnection(payload)
      ElMessage.success('连接已创建')
    }
    emit('saved')
    visible.value = false
  } catch (error) {
    console.error('保存连接失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.type-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #e6a23c;
}
</style>
