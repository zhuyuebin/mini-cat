<template>
  <el-dialog v-model="visible" title="批量导入数据" width="540px" :close-on-click-modal="false" append-to-body>
    <el-alert type="info" :closable="false" title="导入说明" class="import-alert">
      <p>1. 支持 Excel（.xlsx / .xls）与 CSV（.csv），文件最大 50MB；</p>
      <p>2. 第一行必须是表头，且要与数据表的**所有列名**完全一致（含自增列）；</p>
      <p>3. 批量插入失败会整体回滚，请先备份重要数据。</p>
    </el-alert>

    <el-upload
      drag
      action="#"
      :auto-upload="false"
      :limit="1"
      :on-change="onFileChange"
      :on-remove="onFileRemove"
      accept=".xlsx,.xls,.csv"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或 <em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">当前目标表：{{ table }}（{{ database }}）</div>
      </template>
    </el-upload>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">开始导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { importData } from '@/api/database'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  connectionId: { type: String, required: true },
  database: { type: String, required: true },
  table: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'imported'])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const selectedFile = ref(null)
const submitting = ref(false)

function onFileChange(file) {
  selectedFile.value = file.raw || null
}

function onFileRemove() {
  selectedFile.value = null
}

async function submit() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要导入的文件')
    return
  }
  submitting.value = true
  try {
    const res = await importData(props.connectionId, props.database, props.table, selectedFile.value)
    selectedFile.value = null
    visible.value = false
    emit('imported', res.data)
  } catch (error) {
    console.error('[import] 导入失败', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.import-alert {
  margin-bottom: 14px;
}

.import-alert p {
  margin: 2px 0;
  line-height: 1.6;
}

.import-alert :deep(.el-alert__title) {
  font-weight: 600;
}
</style>
