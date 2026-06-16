<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  CloudUploadOutlined,
  LinkOutlined,
  InboxOutlined,
} from '@ant-design/icons-vue'
import { uploadPictureUsingPost, uploadUrlPictureUsingPost } from '@/api/pictureController'

const router = useRouter()
const route = useRoute()

// 从 query 中获取 spaceId（私有空间上传）
const spaceId = computed(() => route.query.spaceId as string | undefined)

// 计算编辑页跳转路径
function getEditPath(pictureId: string) {
  if (spaceId.value) {
    return `/picture/edit/${pictureId}?from=space&spaceId=${spaceId.value}`
  }
  return `/picture/edit/${pictureId}`
}

// ===== 检查是否有未完成编辑的图片 =====
onMounted(() => {
  const pendingId = localStorage.getItem('pending_edit_picture_id')
  if (pendingId) {
    Modal.confirm({
      title: '有未完成的图片编辑',
      content: '上次上传的图片尚未完成编辑，是否继续编辑？',
      okText: '继续编辑',
      cancelText: '放弃，重新上传',
      onOk() {
        const pendingSpaceId = localStorage.getItem('pending_edit_space_id')
        if (pendingSpaceId) {
          router.push(`/picture/edit/${pendingId}?from=space&spaceId=${pendingSpaceId}`)
        } else {
          router.push(`/picture/edit/${pendingId}`)
        }
      },
      onCancel() {
        localStorage.removeItem('pending_edit_picture_id')
        localStorage.removeItem('pending_edit_space_id')
      },
    })
  }
})

// ===== 上传模式 =====
const activeTab = ref<'file' | 'url'>('file')

// ===== 文件上传 =====
const uploading = ref(false)
const fileList = ref<any[]>([])
const previewUrl = ref('')

function handleFileChange(info: any) {
  const file = info.file?.originFileObj || info.file
  if (!file) return
  fileList.value = [info.file]
  // 本地预览
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
  // 自动上传
  autoUpload(file)
}

async function autoUpload(file: File) {
  uploading.value = true
  try {
    const res: any = await uploadPictureUsingPost({ spaceId: spaceId.value as any }, {}, file)
    if (res?.data?.id) {
      message.success('上传成功')
      localStorage.setItem('pending_edit_picture_id', String(res.data.id))
      if (spaceId.value) {
        localStorage.setItem('pending_edit_space_id', spaceId.value)
      }
      router.push(getEditPath(String(res.data.id)))
    }
  } catch {
    // 拦截器已处理，用户可手动重试
  } finally {
    uploading.value = false
  }
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  const file = e.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('image/')) {
    fileList.value = [{ originFileObj: file, name: file.name }]
    const reader = new FileReader()
    reader.onload = (ev) => {
      previewUrl.value = ev.target?.result as string
    }
    reader.readAsDataURL(file)
    // 自动上传
    autoUpload(file)
  }
}

function clearFile() {
  fileList.value = []
  previewUrl.value = ''
}

async function handleFileUpload() {
  const file = fileList.value[0]?.originFileObj || fileList.value[0]
  if (!file) {
    message.warning('请先选择要上传的图片')
    return
  }
  uploading.value = true
  try {
    const res: any = await uploadPictureUsingPost({ spaceId: spaceId.value as any }, {}, file)
    if (res?.data?.id) {
      message.success('上传成功')
      localStorage.setItem('pending_edit_picture_id', String(res.data.id))
      if (spaceId.value) {
        localStorage.setItem('pending_edit_space_id', spaceId.value)
      }
      router.push(getEditPath(String(res.data.id)))
    }
  } catch {
    // 拦截器已处理
  } finally {
    uploading.value = false
  }
}

// ===== URL 上传 =====
const urlInput = ref('')
const urlUploading = ref(false)

async function handleUrlUpload() {
  const url = urlInput.value.trim()
  if (!url) {
    message.warning('请输入图片 URL 地址')
    return
  }
  if (!/^https?:\/\/.+/i.test(url)) {
    message.warning('请输入有效的 http/https 图片链接')
    return
  }
  urlUploading.value = true
  try {
    const res: any = await uploadUrlPictureUsingPost({ fileUrl: url, spaceId: spaceId.value as any })
    if (res?.data?.id) {
      message.success('抓取成功')
      localStorage.setItem('pending_edit_picture_id', String(res.data.id))
      if (spaceId.value) {
        localStorage.setItem('pending_edit_space_id', spaceId.value)
      }
      router.push(getEditPath(String(res.data.id)))
    }
  } catch {
    // 拦截器已处理
  } finally {
    urlUploading.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="upload-page">
    <!-- 顶部栏 -->
    <div class="upload-topbar">
      <a-button type="text" class="back-btn" @click="goBack">
        <ArrowLeftOutlined />
      </a-button>
      <h2 class="topbar-title">发布图片</h2>
      <div class="topbar-spacer"></div>
    </div>

    <!-- Tab 切换 -->
    <div class="upload-tabs">
      <div
        class="tab-item"
        :class="{ active: activeTab === 'file' }"
        @click="activeTab = 'file'"
      >
        <CloudUploadOutlined />
        <span>上传图片</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'url' }"
        @click="activeTab = 'url'"
      >
        <LinkOutlined />
        <span>链接上传</span>
      </div>
    </div>

    <!-- 文件上传区域 -->
    <div v-if="activeTab === 'file'" class="upload-content">
      <div
        v-if="!previewUrl"
        class="upload-dragger"
        @dragover.prevent
        @drop="handleDrop"
      >
        <a-upload
          :file-list="fileList"
          :before-upload="() => false"
          :show-upload-list="false"
          accept="image/jpeg,image/png,image/gif,image/webp"
          @change="handleFileChange"
        >
          <div class="dragger-inner">
            <div class="dragger-illustration">
              <InboxOutlined class="dragger-icon" />
            </div>
            <p class="dragger-text">点击或拖拽上传图片</p>
            <div class="dragger-formats">
              <span class="format-tag">JPG</span>
              <span class="format-tag">PNG</span>
              <span class="format-tag">GIF</span>
              <span class="format-tag">WEBP</span>
            </div>
          </div>
        </a-upload>
      </div>

      <!-- 预览 + 操作 -->
      <div v-else class="upload-preview-area">
        <div class="preview-card">
          <img :src="previewUrl" class="preview-img" alt="预览" />
          <div class="preview-overlay">
            <a-button size="small" danger @click="clearFile">移除</a-button>
          </div>
        </div>
        <div class="upload-actions">
          <a-button
            type="primary"
            size="large"
            :loading="uploading"
            @click="handleFileUpload"
          >
            {{ uploading ? '上传中...' : '确认上传' }}
          </a-button>
        </div>
      </div>
    </div>

    <!-- URL 上传区域 -->
    <div v-else class="upload-content">
      <div class="url-upload-area">
        <div class="url-illustration">
          <LinkOutlined class="url-icon" />
        </div>
        <p class="url-desc">通过图片链接发布</p>
        <div class="url-input-row">
          <a-input
            v-model:value="urlInput"
            size="large"
            placeholder="请粘贴或输入图片 URL 地址..."
            @press-enter="handleUrlUpload"
          >
            <template #prefix><LinkOutlined style="color: var(--color-text-disabled)" /></template>
          </a-input>
          <a-button
            type="primary"
            size="large"
            :loading="urlUploading"
            @click="handleUrlUpload"
          >
            {{ urlUploading ? '抓取中...' : '立即抓取' }}
          </a-button>
        </div>
        <p class="url-hint">支持 http/https 协议的公开图片地址</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-page {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--space-lg) var(--space-md);
}

/* ===== 顶部栏 ===== */
.upload-topbar {
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

.topbar-spacer {
  width: 32px;
}

/* ===== Tab 切换 ===== */
.upload-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-surface-sand);
  margin-bottom: var(--space-lg);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: var(--space-md) 0;
  font-size: 15px;
  font-weight: var(--weight-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  border-bottom: 2px solid transparent;
  user-select: none;
}

.tab-item:hover {
  color: var(--color-primary);
}

.tab-item.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* ===== 上传内容区 ===== */
.upload-content {
  min-height: 400px;
}

/* ===== 文件上传拖拽区 ===== */
.upload-dragger {
  border: 2px dashed var(--color-surface-sand);
  border-radius: var(--radius-card);
  padding: var(--space-xl) var(--space-lg);
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-normal);
  background: var(--color-bg);
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-dragger:hover {
  border-color: var(--color-primary);
  background: rgba(230, 0, 35, 0.01);
}

.dragger-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}

.dragger-illustration {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(67, 94, 229, 0.08), rgba(230, 0, 35, 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
}

.dragger-icon {
  font-size: 42px;
  color: var(--color-primary);
}

.dragger-text {
  font-size: 16px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  margin: 0;
}

.dragger-formats {
  display: flex;
  gap: var(--space-sm);
}

.format-tag {
  padding: 2px 12px;
  border-radius: var(--radius-base);
  border: 1px solid var(--color-surface-sand);
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg-warm);
}

/* ===== 预览区域 ===== */
.upload-preview-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
}

.preview-card {
  position: relative;
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  max-width: 600px;
  width: 100%;
}

.preview-img {
  width: 100%;
  height: auto;
  display: block;
  max-height: 500px;
  object-fit: contain;
  background: var(--color-bg-warm);
}

.preview-overlay {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
}

.upload-actions {
  display: flex;
  gap: var(--space-md);
}

/* ===== URL 上传区域 ===== */
.url-upload-area {
  border: 2px dashed var(--color-surface-sand);
  border-radius: var(--radius-card);
  padding: var(--space-xl) var(--space-lg);
  text-align: center;
  background: var(--color-bg);
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
}

.url-illustration {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(67, 94, 229, 0.08), rgba(230, 0, 35, 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
}

.url-icon {
  font-size: 42px;
  color: var(--color-primary);
}

.url-desc {
  font-size: 16px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  margin: 0;
}

.url-input-row {
  display: flex;
  gap: var(--space-sm);
  max-width: 560px;
  width: 100%;
}

.url-input-row .ant-input-affix-wrapper {
  flex: 1;
}

.url-hint {
  font-size: 13px;
  color: var(--color-text-disabled);
  margin: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .upload-page {
    padding: var(--space-md) var(--space-sm);
  }

  .url-input-row {
    flex-direction: column;
  }
}
</style>
