<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
} from '@ant-design/icons-vue'
import {
  listPictureByPageUsingPost,
  updatePictureUsingPost,
  deletePictureUsingPost,
  doPictureReviewUsingPost,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController'

const router = useRouter()

// ===== 审核状态 =====
const reviewStatusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '已通过', color: 'green' },
  2: { text: '已拒绝', color: 'red' },
}

// ===== 列表数据 =====
const tableData = ref<API.Picture[]>([])
const total = ref(0)
const loading = ref(false)
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  name: '',
  category: undefined,
  reviewStatus: undefined,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// ===== 标签分类 =====
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])

async function fetchTagCategory() {
  try {
    const res: any = await listPictureTagCategoryUsingGet()
    if (res?.data) {
      categoryList.value = res.data.categoryList || []
      tagList.value = res.data.tagList || []
    }
  } catch {
    // 静默
  }
}

const columns = [
  {
    title: '缩略图',
    dataIndex: 'thumbnailUrl',
    width: 80,
  },
  {
    title: '图片名称',
    dataIndex: 'name',
    width: 180,
    ellipsis: true,
  },
  {
    title: '分类',
    dataIndex: 'category',
    width: 100,
  },
  {
    title: '格式',
    dataIndex: 'picFormat',
    width: 80,
  },
  {
    title: '尺寸',
    key: 'dimension',
    width: 120,
  },
  {
    title: '大小',
    dataIndex: 'picSize',
    width: 100,
  },
  {
    title: '审核状态',
    dataIndex: 'reviewStatus',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 170,
  },
  {
    title: '操作',
    key: 'action',
    width: 240,
    fixed: 'right' as const,
  },
]

async function fetchPictures() {
  loading.value = true
  try {
    const params = { ...searchParams }
    // 清除空字符串参数
    if (!params.name) delete params.name
    if (!params.category) delete params.category
    if (params.reviewStatus === undefined) delete params.reviewStatus
    const res: any = await listPictureByPageUsingPost(params)
    if (res?.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchParams.current = 1
  fetchPictures()
}

function handleReset() {
  searchParams.name = ''
  searchParams.category = undefined
  searchParams.reviewStatus = undefined
  searchParams.current = 1
  fetchPictures()
}

function handlePageChange(page: number, pageSize: number) {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchPictures()
}

// ===== 编辑图片 =====
const editModalVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive<API.PictureUpdateRequest>({
  id: undefined,
  name: '',
  introduction: '',
  category: '',
  tags: [],
})

function openEditModal(record: API.Picture) {
  editForm.id = record.id
  editForm.name = record.name || ''
  editForm.introduction = record.introduction || ''
  editForm.category = record.category || ''
  // Picture.tags 是 JSON 字符串，需要解析
  try {
    editForm.tags = record.tags ? JSON.parse(record.tags) : []
  } catch {
    editForm.tags = []
  }
  editModalVisible.value = true
}

async function handleEditOk() {
  if (!editForm.id) return
  editLoading.value = true
  try {
    await updatePictureUsingPost({ ...editForm })
    message.success('更新成功')
    editModalVisible.value = false
    fetchPictures()
  } catch {
    // 拦截器已处理
  } finally {
    editLoading.value = false
  }
}

// ===== 删除图片 =====
function handleDelete(record: API.Picture) {
  Modal.confirm({
    title: '确认删除',
    icon: h(ExclamationCircleOutlined),
    content: `确定要删除图片「${record.name || '无标题'}」吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deletePictureUsingPost({ id: record.id })
        message.success('删除成功')
        fetchPictures()
      } catch {
        // 拦截器已处理
      }
    },
  })
}

// ===== 审核图片 =====
const reviewModalVisible = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive<API.PictureReviewRequest>({
  id: undefined,
  reviewStatus: undefined,
  reviewMessage: '',
})

function openReviewModal(record: API.Picture) {
  reviewForm.id = record.id
  reviewForm.reviewStatus = undefined
  reviewForm.reviewMessage = ''
  reviewModalVisible.value = true
}

async function handleReviewOk() {
  if (!reviewForm.id || reviewForm.reviewStatus === undefined) {
    message.warning('请选择审核结果')
    return
  }
  reviewLoading.value = true
  try {
    await doPictureReviewUsingPost({ ...reviewForm })
    message.success('审核完成')
    reviewModalVisible.value = false
    fetchPictures()
  } catch {
    // 拦截器已处理
  } finally {
    reviewLoading.value = false
  }
}

// ===== 预览 =====
const previewVisible = ref(false)
const previewUrl = ref('')
const previewTitle = ref('')

function handlePreview(record: API.Picture) {
  previewUrl.value = record.url || record.thumbnailUrl || ''
  previewTitle.value = record.name || '图片预览'
  previewVisible.value = true
}

// ===== 下载 =====
function handleDownload(record: API.Picture) {
  if (!record.url) {
    message.warning('该图片没有可下载的链接')
    return
  }
  const link = document.createElement('a')
  link.href = record.url
  link.download = record.name || 'picture'
  link.target = '_blank'
  link.rel = 'noopener noreferrer'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// ===== 格式化文件大小 =====
function formatSize(bytes?: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  const kb = bytes / 1024
  if (kb < 1024) return kb.toFixed(1) + ' KB'
  const mb = kb / 1024
  return mb.toFixed(2) + ' MB'
}

onMounted(() => {
  fetchPictures()
  fetchTagCategory()
})

function formatTime(dateStr?: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
</script>

<template>
  <div class="picture-manage-page">
    <div class="page-header">
      <h2 class="page-title">图片管理</h2>
      <p class="page-desc">管理系统中的所有图片（增删改查 & 审核）</p>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <a-space wrap>
        <a-input
          v-model:value="searchParams.name"
          placeholder="搜索图片名称"
          allow-clear
          style="width: 200px"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="searchParams.category"
          placeholder="选择分类"
          allow-clear
          style="width: 140px"
        >
          <a-select-option v-for="cat in categoryList" :key="cat" :value="cat">
            {{ cat }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="searchParams.reviewStatus"
          placeholder="审核状态"
          allow-clear
          style="width: 140px"
        >
          <a-select-option :value="0">待审核</a-select-option>
          <a-select-option :value="1">已通过</a-select-option>
          <a-select-option :value="2">已拒绝</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">
          <SearchOutlined /> 搜索
        </a-button>
        <a-button @click="handleReset">重置</a-button>
      </a-space>
      <a-button type="primary" @click="router.push('/picture/upload')">
        <PlusOutlined /> 上传图片
      </a-button>
    </div>

    <!-- 图片表格 -->
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
        :scroll="{ x: 1200 }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <!-- 缩略图 -->
          <template v-if="column.dataIndex === 'thumbnailUrl'">
            <div class="thumb-cell" @click="handlePreview(record)">
              <img
                v-if="record.thumbnailUrl || record.url"
                :src="record.thumbnailUrl || record.url"
                class="thumb-img"
                alt=""
              />
              <div v-else class="thumb-placeholder">无图</div>
            </div>
          </template>
          <!-- 尺寸 -->
          <template v-else-if="column.key === 'dimension'">
            <span v-if="record.picWidth && record.picHeight">
              {{ record.picWidth }} × {{ record.picHeight }}
            </span>
            <span v-else>-</span>
          </template>
          <!-- 大小 -->
          <template v-else-if="column.dataIndex === 'picSize'">
            {{ formatSize(record.picSize) }}
          </template>
          <!-- 审核状态 -->
          <template v-else-if="column.dataIndex === 'reviewStatus'">
            <a-tag
              v-if="reviewStatusMap[record.reviewStatus ?? -1]"
              :color="reviewStatusMap[record.reviewStatus ?? -1]?.color"
            >
              {{ reviewStatusMap[record.reviewStatus ?? -1]?.text }}
            </a-tag>
            <span v-else>-</span>
          </template>
          <!-- 创建时间 -->
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-tooltip title="预览">
                <a-button type="link" size="small" @click="handlePreview(record)">
                  <EyeOutlined />
                </a-button>
              </a-tooltip>
              <a-tooltip title="编辑">
                <a-button type="link" size="small" @click="openEditModal(record)">
                  <EditOutlined />
                </a-button>
              </a-tooltip>
              <a-tooltip title="审核">
                <a-button type="link" size="small" @click="openReviewModal(record)">
                  <CheckCircleOutlined />
                </a-button>
              </a-tooltip>
              <a-tooltip title="下载">
                <a-button type="link" size="small" @click="handleDownload(record)">
                  <DownloadOutlined />
                </a-button>
              </a-tooltip>
              <a-tooltip title="删除">
                <a-button type="link" danger size="small" @click="handleDelete(record)">
                  <DeleteOutlined />
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 编辑 Modal -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑图片"
      :confirm-loading="editLoading"
      @ok="handleEditOk"
      :width="560"
    >
      <a-form :model="editForm" layout="vertical" style="margin-top: 16px">
        <a-form-item label="图片名称">
          <a-input v-model:value="editForm.name" placeholder="请输入图片名称" />
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea v-model:value="editForm.introduction" placeholder="请输入图片简介" :rows="3" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="editForm.category" placeholder="选择分类" allow-clear>
            <a-select-option v-for="cat in categoryList" :key="cat" :value="cat">
              {{ cat }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="标签">
          <a-select
            v-model:value="editForm.tags"
            mode="tags"
            placeholder="添加标签（回车确认）"
            :options="tagList.map((t) => ({ label: t, value: t }))"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 审核 Modal -->
    <a-modal
      v-model:open="reviewModalVisible"
      title="审核图片"
      :confirm-loading="reviewLoading"
      @ok="handleReviewOk"
      :width="480"
    >
      <a-form :model="reviewForm" layout="vertical" style="margin-top: 16px">
        <a-form-item label="审核结果" required>
          <a-radio-group v-model:value="reviewForm.reviewStatus">
            <a-radio-button :value="1">
              <CheckCircleOutlined /> 通过
            </a-radio-button>
            <a-radio-button :value="2">
              <CloseCircleOutlined /> 拒绝
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核意见">
          <a-textarea
            v-model:value="reviewForm.reviewMessage"
            placeholder="请输入审核意见（可选）"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 预览 Modal -->
    <a-modal
      v-model:open="previewVisible"
      :title="previewTitle"
      :footer="null"
      :width="720"
    >
      <div class="preview-wrap">
        <img :src="previewUrl" class="preview-img" alt="" />
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.picture-manage-page {
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

/* 缩略图 */
.thumb-cell {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-button);
  overflow: hidden;
  cursor: pointer;
  background: var(--color-surface-sand);
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--color-text-disabled);
}

/* 上传预览 */
.upload-preview {
  margin-top: var(--space-sm);
  display: flex;
  align-items: flex-end;
  gap: var(--space-sm);
}

.upload-preview-img {
  max-width: 200px;
  max-height: 140px;
  border-radius: var(--radius-button);
  border: 1px solid var(--color-surface-sand);
  object-fit: contain;
}

/* 图片预览 */
.preview-wrap {
  display: flex;
  justify-content: center;
  padding: var(--space-sm) 0;
}

.preview-img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: var(--radius-button);
}
</style>
