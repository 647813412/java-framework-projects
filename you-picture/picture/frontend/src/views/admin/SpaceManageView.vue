<script setup lang="ts">
import { ref, reactive, onMounted, h, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons-vue'
import {
  listSpaceByPageUsingPost,
  updateSpaceUsingPost,
  deleteSpaceUsingPost,
  listSpaceLevelUsingGet,
} from '@/api/spaceController'

// ===== 列表数据 =====
const tableData = ref<API.Space[]>([])
const total = ref(0)
const loading = ref(false)
const spaceLevels = ref<API.SpaceLevel[]>([])
const searchParams = reactive<API.SpaceQueryRequest>({
  current: 1,
  pageSize: 10,
  spaceName: '',
  spaceLevel: undefined,
  spaceType: undefined,
})

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
    ellipsis: true,
  },
  {
    title: '空间名称',
    dataIndex: 'spaceName',
    width: 160,
  },
  {
    title: '空间类型',
    dataIndex: 'spaceType',
    width: 100,
  },
  {
    title: '空间级别',
    dataIndex: 'spaceLevel',
    width: 100,
  },
  {
    title: '图片数量',
    key: 'picCount',
    width: 120,
  },
  {
    title: '存储用量',
    key: 'storage',
    width: 140,
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: 100,
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 160,
    fixed: 'right' as const,
  },
]

async function fetchSpaces() {
  loading.value = true
  try {
    const res: any = await listSpaceByPageUsingPost(searchParams)
    if (res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

async function fetchSpaceLevels() {
  try {
    const res: any = await listSpaceLevelUsingGet()
    if (res?.data) {
      spaceLevels.value = res.data
    }
  } catch {
    // ignore
  }
}

function handleSearch() {
  searchParams.current = 1
  fetchSpaces()
}

function handleReset() {
  searchParams.spaceName = ''
  searchParams.spaceLevel = undefined
  searchParams.spaceType = undefined
  searchParams.current = 1
  fetchSpaces()
}

function handlePageChange(page: number, pageSize: number) {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchSpaces()
}

// ===== 空间级别 / 类型 标签 =====
function getSpaceLevelLabel(level?: number) {
  if (level === undefined || level === null) return '-'
  const found = spaceLevels.value.find((l) => l.value === level)
  if (found) return found.text
  const map: Record<number, string> = { 0: '普通版', 1: '专业版', 2: '旗舰版' }
  return map[level] ?? `级别${level}`
}

function getSpaceLevelColor(level?: number) {
  const map: Record<number, string> = { 0: 'default', 1: 'blue', 2: 'gold' }
  return map[level ?? 0] ?? 'default'
}

function getSpaceTypeLabel(type?: number) {
  const map: Record<number, string> = { 0: '私有', 1: '团队' }
  return map[type ?? 0] ?? `类型${type}`
}

function formatSize(bytes?: number) {
  if (!bytes) return '0 MB'
  if (bytes >= 1024 * 1024 * 1024) return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// ===== 编辑 Modal =====
const modalVisible = ref(false)
const modalLoading = ref(false)
const formState = reactive<API.SpaceUpdateRequest>({
  id: undefined,
  spaceName: '',
  spaceLevel: undefined,
  maxCount: undefined,
  maxSize: undefined,
})

// 空间级别默认值映射
const spaceLevelDefaults: Record<number, { maxCount: number; maxSize: number }> = {
  0: { maxCount: 100, maxSize: 100 * 1024 * 1024 },
  1: { maxCount: 1000, maxSize: 1000 * 1024 * 1024 },
  2: { maxCount: 10000, maxSize: 10000 * 1024 * 1024 },
}

// 修改空间级别时自动更新默认值
watch(() => formState.spaceLevel, (newLevel) => {
  if (newLevel !== undefined && spaceLevelDefaults[newLevel]) {
    formState.maxCount = spaceLevelDefaults[newLevel].maxCount
    formState.maxSize = spaceLevelDefaults[newLevel].maxSize
  }
})

function openEditModal(record: API.Space) {
  Object.assign(formState, {
    id: record.id,
    spaceName: record.spaceName || '',
    spaceLevel: record.spaceLevel,
    maxCount: record.maxCount,
    maxSize: record.maxSize,
  })
  modalVisible.value = true
}

async function handleModalOk() {
  modalLoading.value = true
  try {
    await updateSpaceUsingPost({
      id: formState.id,
      spaceName: formState.spaceName,
      spaceLevel: formState.spaceLevel,
      maxCount: formState.maxCount,
      maxSize: formState.maxSize,
    })
    message.success('更新成功')
    modalVisible.value = false
    fetchSpaces()
  } catch {
    // 拦截器已处理
  } finally {
    modalLoading.value = false
  }
}

// ===== 删除 =====
function handleDelete(record: API.Space) {
  Modal.confirm({
    title: '确认删除',
    icon: h(ExclamationCircleOutlined),
    content: `确定要删除空间「${record.spaceName}」吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteSpaceUsingPost({ id: record.id })
        message.success('删除成功')
        fetchSpaces()
      } catch {
        // 拦截器已处理
      }
    },
  })
}

function formatTime(dateStr?: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(() => {
  fetchSpaceLevels()
  fetchSpaces()
})
</script>

<template>
  <div class="space-manage-page">
    <div class="page-header">
      <h2 class="page-title">空间管理</h2>
      <p class="page-desc">管理系统中的所有用户空间</p>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <a-space wrap>
        <a-input
          v-model:value="searchParams.spaceName"
          placeholder="搜索空间名称"
          allow-clear
          style="width: 180px"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="searchParams.spaceLevel"
          placeholder="空间级别"
          allow-clear
          style="width: 140px"
        >
          <a-select-option :value="0">普通版</a-select-option>
          <a-select-option :value="1">专业版</a-select-option>
          <a-select-option :value="2">旗舰版</a-select-option>
        </a-select>
        <a-select
          v-model:value="searchParams.spaceType"
          placeholder="空间类型"
          allow-clear
          style="width: 140px"
        >
          <a-select-option :value="0">私有空间</a-select-option>
          <a-select-option :value="1">团队空间</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">
          <SearchOutlined /> 搜索
        </a-button>
        <a-button @click="handleReset">重置</a-button>
      </a-space>
    </div>

    <!-- 空间表格 -->
    <div class="table-section">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="{
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`,
          onChange: handlePageChange,
        }"
        :scroll="{ x: 1100 }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'spaceType'">
            <a-tag :color="record.spaceType === 1 ? 'blue' : 'default'">
              {{ getSpaceTypeLabel(record.spaceType) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'spaceLevel'">
            <a-tag :color="getSpaceLevelColor(record.spaceLevel)">
              {{ getSpaceLevelLabel(record.spaceLevel) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'picCount'">
            {{ record.totalCount ?? 0 }} / {{ record.maxCount ?? '∞' }}
          </template>
          <template v-else-if="column.key === 'storage'">
            {{ formatSize(record.totalSize) }} / {{ formatSize(record.maxSize) }}
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 编辑 Modal -->
    <a-modal
      v-model:open="modalVisible"
      title="编辑空间"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      :width="520"
    >
      <a-form :model="formState" layout="vertical" style="margin-top: 16px">
        <a-form-item label="空间名称">
          <a-input v-model:value="formState.spaceName" placeholder="请输入空间名称" />
        </a-form-item>
        <a-form-item label="空间级别">
          <a-select v-model:value="formState.spaceLevel">
            <a-select-option :value="0">普通版</a-select-option>
            <a-select-option :value="1">专业版</a-select-option>
            <a-select-option :value="2">旗舰版</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="最大图片数">
          <a-input-number v-model:value="formState.maxCount" :min="0" style="width: 100%" placeholder="请输入最大图片数" />
        </a-form-item>
        <a-form-item label="最大存储空间 (字节)">
          <a-input-number v-model:value="formState.maxSize" :min="0" style="width: 100%" placeholder="请输入最大存储空间" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.space-manage-page {
  padding: var(--space-md) 0;
}

.page-header {
  margin-bottom: var(--space-lg);
}

.page-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.search-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-bg);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
}

.table-section {
  background: var(--color-bg);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
  padding: var(--space-md);
  overflow: hidden;
}
</style>
