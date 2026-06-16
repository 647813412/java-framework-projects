<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  SaveOutlined,
  CloseOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import { getPictureVoByIdUsingGet, editPictureUsingPost, listPictureTagCategoryUsingGet, aiAutoTagUsingPost } from '@/api/pictureController'

const router = useRouter()
const route = useRoute()

// ===== 图片数据 =====
const pictureId = route.params.id as string
const picture = ref<API.PictureVO | null>(null)
const loading = ref(true)

// ===== 分类/标签选项 =====
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])

// ===== 编辑表单 =====
const editForm = reactive<API.PictureEditRequest>({
  id: undefined,
  name: '',
  introduction: '',
  category: '',
  tags: [],
})
const saving = ref(false)

// ===== 获取图片详情 =====
async function fetchPicture() {
  loading.value = true
  try {
    const res: any = await getPictureVoByIdUsingGet({ id: pictureId as any })
    if (res?.data) {
      picture.value = res.data
      editForm.id = res.data.id
      editForm.name = res.data.name || ''
      editForm.introduction = res.data.introduction || ''
      editForm.category = res.data.category || ''
      editForm.tags = res.data.tags || []
    }
  } catch (e: any) {
    if (e?.message === '未登录') {
      router.push('/login')
    } else {
      message.error('获取图片信息失败')
    }
  } finally {
    loading.value = false
  }
}

// ===== 获取分类和标签列表 =====
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

// ===== 保存编辑 =====
async function handleSave() {
  if (!editForm.category) {
    message.warning('请选择分类')
    return
  }
  saving.value = true
  try {
    await editPictureUsingPost({ ...editForm })
    message.success('保存成功')
    localStorage.removeItem('pending_edit_picture_id')
    localStorage.removeItem('pending_edit_space_id')
    // 根据来源页面跳转
    const from = route.query.from as string | undefined
    const spaceId = route.query.spaceId as string | undefined
    if (from === 'space' && spaceId) {
      router.push({ path: '/profile', query: { tab: 'private' } })
    } else if (from === 'profile') {
      router.push({ path: '/profile', query: { tab: 'public' } })
    } else if (from === 'ai') {
      router.push('/ai/image')
    } else {
      router.push('/')
    }
  } catch {
    // 拦截器已处理
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.back()
}

// ===== AI 智能分类标签 =====
const aiTagLoading = ref(false)

async function handleAiAutoTag() {
  if (!editForm.id) return
  aiTagLoading.value = true
  try {
    const res: any = await aiAutoTagUsingPost({ pictureId: editForm.id })
    if (res?.data) {
      if (res.data.category) {
        editForm.category = res.data.category
      }
      if (res.data.tags && res.data.tags.length > 0) {
        editForm.tags = res.data.tags
      }
      message.success('AI 分类标签已生成')
    }
  } catch {
    // 拦截器已处理
  } finally {
    aiTagLoading.value = false
  }
}

function goBack() {
  router.back()
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
  fetchPicture()
  fetchTagCategory()
})
</script>

<template>
  <div class="edit-page">
    <!-- 顶部栏 -->
    <div class="edit-topbar">
      <a-button type="text" class="back-btn" @click="goBack">
        <ArrowLeftOutlined />
      </a-button>
      <h2 class="topbar-title">发布图片</h2>
      <a-button
        type="primary"
        class="publish-btn"
        :loading="saving"
        @click="handleSave"
      >
        <SaveOutlined />
        发布
      </a-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <a-spin size="large" />
    </div>

    <!-- 内容区 -->
    <div v-else-if="picture" class="edit-content">
      <!-- 左侧：图片预览 -->
      <div class="edit-left">
        <div class="image-preview-card">
          <img
            :src="picture.url || picture.thumbnailUrl"
            class="preview-image"
            alt="图片预览"
          />
        </div>
        <!-- 图片信息摘要 -->
        <div class="image-meta">
          <div v-if="picture.picWidth && picture.picHeight" class="meta-item">
            <span class="meta-label">尺寸</span>
            <span class="meta-value">{{ picture.picWidth }} × {{ picture.picHeight }}</span>
          </div>
          <div v-if="picture.picFormat" class="meta-item">
            <span class="meta-label">格式</span>
            <span class="meta-value">{{ picture.picFormat?.toUpperCase() }}</span>
          </div>
          <div v-if="picture.picSize" class="meta-item">
            <span class="meta-label">大小</span>
            <span class="meta-value">{{ formatSize(picture.picSize) }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧：编辑表单 -->
      <div class="edit-right">
        <a-form layout="vertical" class="edit-form">
          <a-form-item label="图片名称">
            <a-input
              v-model:value="editForm.name"
              placeholder="给图片起个名字..."
              :maxlength="60"
              show-count
            />
          </a-form-item>

          <a-form-item label="图片简介">
            <a-textarea
              v-model:value="editForm.introduction"
              placeholder="分享一下这张图片的故事吧..."
              :rows="4"
              :maxlength="500"
              show-count
            />
          </a-form-item>

          <a-form-item label="分类" required>
            <div class="field-with-ai">
              <a-select
                v-model:value="editForm.category"
                placeholder="选择分类"
                allow-clear
                style="flex: 1"
              >
                <a-select-option v-for="cat in categoryList" :key="cat" :value="cat">
                  {{ cat }}
                </a-select-option>
              </a-select>
              <a-tooltip title="AI 智能分类标签">
                <a-button
                  class="ai-tag-btn"
                  :loading="aiTagLoading"
                  @click="handleAiAutoTag"
                  shape="circle"
                >
                  <ThunderboltOutlined />
                </a-button>
              </a-tooltip>
            </div>
          </a-form-item>

          <a-form-item label="标签">
            <a-select
              v-model:value="editForm.tags"
              mode="tags"
              placeholder="添加标签"
              :options="tagList.map((t) => ({ label: t, value: t }))"
            />
          </a-form-item>

          <!-- 操作按钮 -->
          <div class="form-actions">
            <a-button size="large" block @click="handleCancel">
              <CloseOutlined />
              取消
            </a-button>
          </div>
        </a-form>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-wrap">
      <a-empty description="图片不存在或已被删除" />
      <a-button type="primary" @click="router.push('/')">返回首页</a-button>
    </div>
  </div>
</template>

<style scoped>
.edit-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: var(--space-lg) var(--space-md);
}

/* ===== 顶部栏 ===== */
.edit-topbar {
  display: flex;
  align-items: center;
  margin-bottom: var(--space-lg);
}

.back-btn {
  font-size: 18px;
  color: var(--color-text-secondary);
}

.topbar-title {
  flex: 1;
  text-align: center;
  font-family: var(--font-display);
  font-size: 1.35rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin: 0;
}

.publish-btn {
  border-radius: var(--radius-button);
  font-weight: var(--weight-medium);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ===== 加载中 ===== */
.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

/* ===== 内容区 ===== */
.edit-content {
  display: flex;
  gap: var(--space-xl);
  align-items: flex-start;
}

/* ===== 左侧：图片预览 ===== */
.edit-left {
  flex: 1;
  min-width: 0;
}

.image-preview-card {
  border-radius: var(--radius-card);
  overflow: hidden;
  background: var(--color-bg-warm);
  box-shadow: var(--shadow-card);
}

.preview-image {
  width: 100%;
  height: auto;
  display: block;
  max-height: 600px;
  object-fit: contain;
}

.image-meta {
  display: flex;
  gap: var(--space-lg);
  margin-top: var(--space-md);
  padding: var(--space-sm) 0;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 12px;
  color: var(--color-text-disabled);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.meta-value {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: var(--weight-medium);
}

/* ===== 右侧：编辑表单 ===== */
.edit-right {
  width: 380px;
  flex-shrink: 0;
}

.edit-form {
  position: sticky;
  top: 88px;
}

.form-actions {
  display: flex;
  gap: var(--space-md);
  margin-top: var(--space-md);
}

.field-with-ai {
  display: flex;
  gap: var(--space-sm);
  align-items: flex-start;
}

.ai-tag-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  min-width: 36px;
  border: 1px solid var(--color-surface-sand);
  background: var(--color-bg);
  color: var(--color-primary);
  font-size: 16px;
  border-radius: 50%;
  transition: all var(--transition-fast);
}

.ai-tag-btn:hover {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(230, 0, 35, 0.25);
}

/* ===== 空状态 ===== */
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-xl) 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .edit-content {
    flex-direction: column;
  }

  .edit-right {
    width: 100%;
  }

  .edit-form {
    position: static;
  }
}
</style>
