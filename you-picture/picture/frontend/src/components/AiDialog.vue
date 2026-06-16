<script setup lang="ts">
import { ref, nextTick, watch, onUnmounted, computed } from 'vue'
import {
  CloseOutlined,
  SendOutlined,
  PictureOutlined,
  DeleteOutlined,
  DownloadOutlined,
  LoadingOutlined,
  RobotOutlined,
  ClearOutlined,
  ReloadOutlined,
  EditOutlined,
  SaveOutlined,
  CheckOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import {
  createText2ImageTaskUsingPost,
  getText2ImageTaskUsingGet,
  createImageEditTaskUsingPost,
  getImageEditTaskUsingGet,
  uploadPictureUsingPost,
  uploadUrlPictureUsingPost,
} from '@/api/pictureController'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
}>()

// ===== 消息模型 =====
interface ChatMessage {
  id: string
  role: 'user' | 'ai'
  content: string
  imageUrl?: string
  attachedImage?: string
  attachedPictureId?: number
  status: 'sending' | 'generating' | 'success' | 'error'
  taskId?: string
  taskType?: 'text2image' | 'image_edit'
  errorMsg?: string
  createdAt: number
}

// ===== 状态 =====
const messages = ref<ChatMessage[]>([
  {
    id: generateId(),
    role: 'ai',
    content:
      '你好！我是 AI 图片助手 🎨\n\n我可以帮你：\n• **文字生图** — 描述你想要的画面，我来生成\n• **图片编辑** — 上传一张图片 + 描述修改要求\n\n支持的尺寸：1024×1024、720×1280、1280×720',
    status: 'success',
    createdAt: Date.now(),
  },
])

const inputText = ref('')
const attachedImage = ref<File | null>(null)
const attachedImagePreview = ref('')
const attachedPictureId = ref<number | null>(null)
const attachedImageUploading = ref(false)
const isPolling = ref(false)
const selectedSize = ref('1024*1024')
const chatBodyRef = ref<HTMLElement>()
const previewVisible = ref(false)
const previewImageUrl = ref('')
const showSizeSelector = ref(false)

const MAX_INPUT_LENGTH = 800
const POLL_INTERVAL = 2000
const MAX_POLL_ATTEMPTS = 60

const sizeOptions = [
  { label: '1:1', value: '1024*1024', desc: '1024×1024' },
  { label: '9:16', value: '720*1280', desc: '720×1280' },
  { label: '16:9', value: '1280*720', desc: '1280×720' },
]

const inputLength = computed(() => inputText.value.length)
const canSend = computed(() => inputText.value.trim().length > 0 && !isPolling.value)
const hasAttachment = computed(() => !!attachedImagePreview.value)

// ===== 工具 =====
function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 9)
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// ===== 关闭弹窗 =====
function handleClose() {
  emit('update:open', false)
}

// ===== 清空对话 =====
function handleClear() {
  messages.value = [
    {
      id: generateId(),
      role: 'ai',
      content: '对话已清空，可以开始新的创作 🎨',
      status: 'success',
      createdAt: Date.now(),
    },
  ]
}

// ===== 上传附件图片 =====
function handleAttachImage(info: any) {
  const file = info.file?.originFileObj || info.file
  if (!file) return
  attachedImage.value = file
  attachedImagePreview.value = URL.createObjectURL(file)
  // 上传到后端获取 pictureId
  attachedImageUploading.value = true
  uploadPictureUsingPost({ picName: 'ai_edit_source' }, {}, file)
    .then((res: any) => {
      if (res?.data) {
        attachedPictureId.value = res.data.id
        // 使用后端返回的 URL 替换本地预览
        const serverUrl = res.data.url || res.data.thumbnailUrl || ''
        if (serverUrl) {
          attachedImagePreview.value = serverUrl
        }
      } else {
        removeAttachment()
        message.error('图片上传失败')
      }
    })
    .catch(() => {
      removeAttachment()
      message.error('图片上传失败')
    })
    .finally(() => {
      attachedImageUploading.value = false
    })
}

function removeAttachment() {
  if (attachedImagePreview.value && attachedImagePreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(attachedImagePreview.value)
  }
  attachedImage.value = null
  attachedImagePreview.value = ''
  attachedPictureId.value = null
}

// ===== 从 AI 结果继续编辑 =====
async function handleContinueEdit(aiMsg: ChatMessage) {
  if (!aiMsg.imageUrl) return

  // 先保存到图库获取 pictureId
  message.loading({ content: '正在准备图片...', key: 'continue-edit' })
  try {
    const res: any = await uploadUrlPictureUsingPost({
      fileUrl: aiMsg.imageUrl,
      picName: `ai-edit-${Date.now()}`,
    })
    if (res?.data) {
      attachedPictureId.value = res.data.id
      attachedImagePreview.value = res.data.url || res.data.thumbnailUrl || aiMsg.imageUrl
      message.success({ content: '图片已就绪，请输入编辑指令', key: 'continue-edit' })
    } else {
      message.error({ content: '图片准备失败', key: 'continue-edit' })
    }
  } catch {
    message.error({ content: '图片准备失败', key: 'continue-edit' })
  }
}

// ===== 保存到空间 =====
async function handleSaveToSpace(aiMsg: ChatMessage) {
  if (!aiMsg.imageUrl) return

  message.loading({ content: '正在保存到图库...', key: 'save-space' })
  try {
    const res: any = await uploadUrlPictureUsingPost({
      fileUrl: aiMsg.imageUrl,
      picName: `ai-generated-${Date.now()}`,
    })
    if (res?.data) {
      message.success({ content: '已保存到图库', key: 'save-space' })
    } else {
      message.error({ content: '保存失败', key: 'save-space' })
    }
  } catch {
    message.error({ content: '保存失败', key: 'save-space' })
  }
}

// ===== 发送消息 =====
async function handleSend() {
  const text = inputText.value.trim()
  if (!text) {
    message.warning('请输入提示词')
    return
  }
  if (text.length > MAX_INPUT_LENGTH) {
    message.warning(`提示词最多 ${MAX_INPUT_LENGTH} 字符`)
    return
  }
  if (isPolling.value) {
    message.warning('当前有任务进行中，请等待完成')
    return
  }

  const hasImage = !!attachedPictureId.value
  const pictureId = attachedPictureId.value
  const imagePreview = attachedImagePreview.value

  // 添加用户消息
  const userMsg: ChatMessage = {
    id: generateId(),
    role: 'user',
    content: text,
    attachedImage: hasImage ? imagePreview : undefined,
    status: 'sending',
    createdAt: Date.now(),
  }
  messages.value.push(userMsg)

  // 清空输入
  inputText.value = ''
  removeAttachment()

  // 添加 AI 占位消息
  const aiMsg: ChatMessage = {
    id: generateId(),
    role: 'ai',
    content: hasImage ? '图片编辑中...' : '图片生成中...',
    status: 'generating',
    taskType: hasImage ? 'image_edit' : 'text2image',
    createdAt: Date.now(),
  }
  messages.value.push(aiMsg)
  scrollToBottom()

  // 调用 API
  isPolling.value = true
  try {
    let taskId: string | undefined

    if (hasImage && pictureId) {
      // 图片编辑
      const res: any = await createImageEditTaskUsingPost({
        pictureId,
        prompt: text,
      })
      taskId = res?.data?.taskId
    } else {
      // 文字生图
      const res: any = await createText2ImageTaskUsingPost({
        prompt: text,
        size: selectedSize.value,
      })
      taskId = res?.data?.taskId
    }

    if (!taskId) {
      throw new Error('任务创建失败')
    }

    aiMsg.taskId = taskId
    userMsg.status = 'success'

    // 开始轮询
    await pollTask(aiMsg)
  } catch (err: any) {
    aiMsg.status = 'error'
    aiMsg.errorMsg = err?.message || '任务创建失败，请重试'
    userMsg.status = 'success'
  } finally {
    isPolling.value = false
    scrollToBottom()
  }
}

// ===== 轮询任务状态 =====
async function pollTask(aiMsg: ChatMessage) {
  const queryFn =
    aiMsg.taskType === 'text2image' ? getText2ImageTaskUsingGet : getImageEditTaskUsingGet

  for (let attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
    await sleep(POLL_INTERVAL)

    // 页面不可见时暂停轮询
    if (document.hidden) {
      await waitForVisible()
    }

    try {
      const res: any = await queryFn({ taskId: aiMsg.taskId })
      const output = res?.data?.output
      if (!output) continue

      switch (output.taskStatus) {
        case 'SUCCEEDED': {
          const resultUrl = output.results?.[0]?.url
          if (resultUrl) {
            aiMsg.status = 'success'
            aiMsg.imageUrl = resultUrl
            aiMsg.content = aiMsg.taskType === 'text2image' ? '图片生成完成' : '图片编辑完成'
            scrollToBottom()
            return
          }
          throw new Error('未获取到结果图片')
        }
        case 'FAILED':
          throw new Error(output.message || '生成失败')
        case 'PENDING':
        case 'RUNNING':
          aiMsg.content =
            output.taskStatus === 'RUNNING'
              ? aiMsg.taskType === 'text2image'
                ? '✨ 正在生成图片...'
                : '✏️ 正在编辑图片...'
              : aiMsg.content
          scrollToBottom()
          break
        default:
          throw new Error('未知任务状态')
      }
    } catch (err: any) {
      aiMsg.status = 'error'
      aiMsg.errorMsg = err?.message || '查询任务状态失败'
      scrollToBottom()
      return
    }
  }

  // 超时
  aiMsg.status = 'error'
  aiMsg.errorMsg = '任务超时（超过 2 分钟），请重试'
}

function waitForVisible(): Promise<void> {
  return new Promise((resolve) => {
    const handler = () => {
      if (!document.hidden) {
        document.removeEventListener('visibilitychange', handler)
        resolve()
      }
    }
    document.addEventListener('visibilitychange', handler)
  })
}

// ===== 重试 =====
async function handleRetry(aiMsg: ChatMessage) {
  if (isPolling.value) {
    message.warning('当前有任务进行中')
    return
  }

  // 找到对应的用户消息（前一条）
  const aiIdx = messages.value.findIndex((m) => m.id === aiMsg.id)
  if (aiIdx <= 0) return
  const userMsg = messages.value[aiIdx - 1]
  if (userMsg.role !== 'user') return

  // 重置 AI 消息状态
  aiMsg.status = 'generating'
  aiMsg.errorMsg = undefined
  aiMsg.imageUrl = undefined
  aiMsg.content = aiMsg.taskType === 'text2image' ? '图片生成中...' : '图片编辑中...'
  scrollToBottom()

  isPolling.value = true
  try {
    let taskId: string | undefined

    if (aiMsg.taskType === 'image_edit' && userMsg.attachedImage) {
      // 需要重新上传图片获取 pictureId
      const uploadRes: any = await uploadUrlPictureUsingPost({
        fileUrl: userMsg.attachedImage,
        picName: 'ai_edit_retry',
      })
      const pictureId = uploadRes?.data?.id
      if (!pictureId) throw new Error('图片准备失败')

      const res: any = await createImageEditTaskUsingPost({
        pictureId,
        prompt: userMsg.content,
      })
      taskId = res?.data?.taskId
    } else {
      const res: any = await createText2ImageTaskUsingPost({
        prompt: userMsg.content,
        size: selectedSize.value,
      })
      taskId = res?.data?.taskId
    }

    if (!taskId) throw new Error('任务创建失败')

    aiMsg.taskId = taskId
    await pollTask(aiMsg)
  } catch (err: any) {
    aiMsg.status = 'error'
    aiMsg.errorMsg = err?.message || '重试失败'
  } finally {
    isPolling.value = false
    scrollToBottom()
  }
}

// ===== 下载图片 =====
function handleDownload(url: string) {
  const a = document.createElement('a')
  a.href = url
  a.target = '_blank'
  a.download = `ai-image-${Date.now()}.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

// ===== 图片预览 =====
function openPreview(url: string) {
  previewImageUrl.value = url
  previewVisible.value = true
}

function closePreview() {
  previewVisible.value = false
  previewImageUrl.value = ''
}

// ===== 尺寸选择器 =====
function toggleSizeSelector() {
  showSizeSelector.value = !showSizeSelector.value
}

function selectSize(value: string) {
  selectedSize.value = value
  showSizeSelector.value = false
}

// ===== 监听 open =====
watch(
  () => props.open,
  (val) => {
    if (val) scrollToBottom()
  },
)

// ===== 清理 =====
onUnmounted(() => {
  if (attachedImagePreview.value && attachedImagePreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(attachedImagePreview.value)
  }
})
</script>

<template>
  <Transition name="ai-dialog-fade">
    <div v-if="open" class="ai-dialog-mask" @click.self="handleClose">
      <div class="ai-dialog">
        <!-- ===== 头部 ===== -->
        <div class="ai-dialog-header">
          <div class="ai-header-left">
            <div class="ai-header-avatar">
              <RobotOutlined />
            </div>
            <span class="ai-header-title">AI 图片助手</span>
          </div>
          <div class="ai-header-actions">
            <a-tooltip title="清空对话">
              <button class="header-action-btn" @click="handleClear">
                <ClearOutlined />
              </button>
            </a-tooltip>
            <button class="header-action-btn close-btn" @click="handleClose">
              <CloseOutlined />
            </button>
          </div>
        </div>

        <!-- ===== 消息区 ===== -->
        <div ref="chatBodyRef" class="ai-dialog-body">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="chat-message"
            :class="[msg.role]"
          >
            <!-- AI 头像 -->
            <div v-if="msg.role === 'ai'" class="msg-avatar ai-avatar">
              <RobotOutlined />
            </div>

            <div class="msg-bubble" :class="[msg.role, msg.status]">
              <!-- 用户附图 -->
              <div
                v-if="msg.role === 'user' && msg.attachedImage"
                class="msg-attached-img"
                @click="openPreview(msg.attachedImage ?? '')"
              >
                <img :src="msg.attachedImage" alt="附图" />
              </div>

              <!-- 文字内容 -->
              <div v-if="msg.content" class="msg-text">{{ msg.content }}</div>

              <!-- 生成中 loading -->
              <div v-if="msg.status === 'generating'" class="msg-loading">
                <div class="loading-dots">
                  <span></span><span></span><span></span>
                </div>
              </div>

              <!-- 生成结果图片 -->
              <div v-if="msg.status === 'success' && msg.imageUrl" class="msg-result-img">
                <img
                  :src="msg.imageUrl"
                  alt="AI 生成"
                  @click="openPreview(msg.imageUrl ?? '')"
                />
                <div class="result-actions">
                  <button class="result-action-btn" @click="handleDownload(msg.imageUrl ?? '')">
                    <DownloadOutlined /> 下载
                  </button>
                  <button class="result-action-btn" @click="handleSaveToSpace(msg)">
                    <SaveOutlined /> 保存到图库
                  </button>
                  <button class="result-action-btn" @click="handleContinueEdit(msg)">
                    <EditOutlined /> 继续编辑
                  </button>
                </div>
              </div>

              <!-- 错误状态 -->
              <div v-if="msg.status === 'error'" class="msg-error">
                <span class="error-text">{{ msg.errorMsg || '生成失败' }}</span>
                <button class="retry-btn" @click="handleRetry(msg)">
                  <ReloadOutlined /> 重试
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- ===== 输入区 ===== -->
        <div class="ai-dialog-footer">
          <!-- 附件预览 -->
          <div v-if="hasAttachment" class="attached-preview">
            <div class="attached-thumb-wrap">
              <img
                :src="attachedImagePreview"
                alt="附件"
                class="attached-thumb"
                @click="openPreview(attachedImagePreview)"
              />
              <div v-if="attachedImageUploading" class="attached-uploading">
                <LoadingOutlined spin />
              </div>
              <div v-else class="attached-ready">
                <CheckOutlined />
              </div>
            </div>
            <button class="attached-remove" @click="removeAttachment">
              <DeleteOutlined />
            </button>
          </div>

          <!-- 尺寸选择（仅无附件时可选） -->
          <div v-if="showSizeSelector && !hasAttachment" class="size-selector">
            <button
              v-for="opt in sizeOptions"
              :key="opt.value"
              class="size-option"
              :class="{ active: selectedSize === opt.value }"
              @click="selectSize(opt.value)"
            >
              <span class="size-label">{{ opt.label }}</span>
              <span class="size-desc">{{ opt.desc }}</span>
            </button>
          </div>

          <div class="input-row">
            <!-- 上传图片 -->
            <a-upload
              :show-upload-list="false"
              :before-upload="() => false"
              accept="image/*"
              @change="handleAttachImage"
            >
              <a-tooltip title="上传图片（编辑模式）">
                <button class="input-action-btn" :disabled="attachedImageUploading || isPolling">
                  <LoadingOutlined v-if="attachedImageUploading" spin />
                  <PictureOutlined v-else />
                </button>
              </a-tooltip>
            </a-upload>

            <!-- 尺寸选择按钮 -->
            <a-tooltip v-if="!hasAttachment" title="图片尺寸">
              <button
                class="input-action-btn size-toggle-btn"
                :class="{ active: showSizeSelector }"
                @click="toggleSizeSelector"
              >
                {{ sizeOptions.find((o) => o.value === selectedSize)?.label }}
              </button>
            </a-tooltip>

            <!-- 输入框 -->
            <div class="input-wrapper">
              <input
                v-model="inputText"
                class="chat-input"
                :placeholder="
                  hasAttachment
                    ? '描述你想要的编辑效果...'
                    : '描述你想要的画面...'
                "
                :maxlength="MAX_INPUT_LENGTH"
                @keydown.enter.prevent="handleSend"
              />
              <span v-if="inputLength > 0" class="input-counter" :class="{ warn: inputLength > MAX_INPUT_LENGTH - 50 }">
                {{ inputLength }}/{{ MAX_INPUT_LENGTH }}
              </span>
            </div>

            <!-- 发送 -->
            <button
              class="send-btn"
              :class="{ active: canSend }"
              :disabled="!canSend"
              @click="handleSend"
            >
              <SendOutlined />
            </button>
          </div>
        </div>
      </div>

      <!-- ===== 图片预览 ===== -->
      <Transition name="preview-fade">
        <div v-if="previewVisible" class="preview-mask" @click="closePreview">
          <img :src="previewImageUrl" alt="预览" class="preview-img" @click.stop />
          <button class="preview-close" @click="closePreview">
            <CloseOutlined />
          </button>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<style scoped>
/* ===== 遮罩 ===== */
.ai-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1100;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 对话容器 ===== */
.ai-dialog {
  width: 560px;
  max-width: 95vw;
  height: 720px;
  max-height: 88vh;
  background: #1a1b2e;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 32px 64px -12px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(255, 255, 255, 0.06);
}

/* ===== 头部 ===== */
.ai-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.02);
}

.ai-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
}

.ai-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 0.3px;
}

.ai-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-action-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.header-action-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #94a3b8;
}

.close-btn:hover {
  color: #f87171;
}

/* ===== 消息区 ===== */
.ai-dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ai-dialog-body::-webkit-scrollbar {
  width: 5px;
}

.ai-dialog-body::-webkit-scrollbar-track {
  background: transparent;
}

.ai-dialog-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.ai-dialog-body::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.18);
}

/* ===== 消息气泡 ===== */
.chat-message {
  display: flex;
  gap: 10px;
  animation: msgFadeIn 0.3s ease;
}

@keyframes msgFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-message.user {
  justify-content: flex-end;
}

.chat-message.ai {
  justify-content: flex-start;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.ai-avatar {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 16px;
}

.msg-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
}

.msg-bubble.user {
  background: #6366f1;
  color: #fff;
  border-bottom-right-radius: 6px;
}

.msg-bubble.ai {
  background: #252640;
  color: #d1d5db;
  border-bottom-left-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

/* ===== 用户附图 ===== */
.msg-attached-img {
  margin-bottom: 8px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
}

.msg-attached-img img {
  max-width: 180px;
  max-height: 140px;
  display: block;
  object-fit: cover;
  border-radius: 10px;
  transition: opacity 0.2s;
}

.msg-attached-img:hover img {
  opacity: 0.85;
}

/* ===== 消息文字 ===== */
.msg-text {
  white-space: pre-wrap;
  margin: 0;
}

/* ===== Loading 动画 ===== */
.msg-loading {
  padding: 4px 0;
}

.loading-dots {
  display: flex;
  gap: 4px;
  align-items: center;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a78bfa;
  animation: dotBounce 1.4s ease-in-out infinite both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes dotBounce {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ===== 结果图片 ===== */
.msg-result-img {
  margin-top: 10px;
}

.msg-result-img > img {
  width: 100%;
  max-width: 320px;
  display: block;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.msg-result-img > img:hover {
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.15);
}

/* ===== 结果操作按钮 ===== */
.result-actions {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.result-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.result-action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e2e8f0;
  border-color: rgba(255, 255, 255, 0.2);
}

/* ===== 错误状态 ===== */
.msg-error {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.error-text {
  color: #f87171;
  font-size: 13px;
}

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px solid rgba(248, 113, 113, 0.3);
  background: rgba(248, 113, 113, 0.1);
  color: #fca5a5;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.retry-btn:hover {
  background: rgba(248, 113, 113, 0.2);
  border-color: rgba(248, 113, 113, 0.5);
}

/* ===== 输入区 ===== */
.ai-dialog-footer {
  padding: 12px 16px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.02);
}

/* ===== 附件预览 ===== */
.attached-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.attached-thumb-wrap {
  position: relative;
  width: 52px;
  height: 52px;
}

.attached-thumb {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
}

.attached-uploading,
.attached-ready {
  position: absolute;
  bottom: -3px;
  right: -3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

.attached-uploading {
  background: #6366f1;
  color: #fff;
}

.attached-ready {
  background: #22c55e;
  color: #fff;
}

.attached-remove {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  border: none;
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
  margin-left: auto;
}

.attached-remove:hover {
  background: rgba(248, 113, 113, 0.3);
}

/* ===== 尺寸选择器 ===== */
.size-selector {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.size-option {
  flex: 1;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s;
}

.size-option:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #e2e8f0;
}

.size-option.active {
  background: rgba(99, 102, 241, 0.15);
  border-color: #6366f1;
  color: #a5b4fc;
}

.size-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
}

.size-desc {
  display: block;
  font-size: 11px;
  opacity: 0.65;
  margin-top: 2px;
}

/* ===== 输入行 ===== */
.input-row {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #252640;
  border-radius: 14px;
  padding: 6px 8px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: border-color 0.2s;
}

.input-row:focus-within {
  border-color: rgba(99, 102, 241, 0.4);
}

.input-action-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.input-action-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #a78bfa;
}

.input-action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.size-toggle-btn {
  font-size: 11px;
  font-weight: 600;
  width: auto;
  padding: 0 10px;
  color: #64748b;
}

.size-toggle-btn.active {
  color: #a78bfa;
  background: rgba(99, 102, 241, 0.1);
}

.input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  min-width: 0;
}

.chat-input {
  width: 100%;
  border: none;
  background: transparent;
  color: #e2e8f0;
  font-size: 14px;
  outline: none;
  min-width: 0;
  padding-right: 48px;
}

.chat-input::placeholder {
  color: #4b5563;
}

.input-counter {
  position: absolute;
  right: 0;
  font-size: 11px;
  color: #4b5563;
  pointer-events: none;
}

.input-counter.warn {
  color: #f59e0b;
}

.send-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: none;
  background: rgba(255, 255, 255, 0.04);
  color: #4b5563;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn.active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.3);
}

.send-btn.active:hover {
  background: linear-gradient(135deg, #5558e6, #7c4ce0);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* ===== 图片预览 ===== */
.preview-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.preview-img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5);
  cursor: default;
}

.preview-close {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.preview-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* ===== 动画 ===== */
.ai-dialog-fade-enter-active,
.ai-dialog-fade-leave-active {
  transition: opacity 0.25s ease;
}

.ai-dialog-fade-enter-active .ai-dialog,
.ai-dialog-fade-leave-active .ai-dialog {
  transition: transform 0.25s ease;
}

.ai-dialog-fade-enter-from,
.ai-dialog-fade-leave-to {
  opacity: 0;
}

.ai-dialog-fade-enter-from .ai-dialog {
  transform: translateY(20px) scale(0.96);
}

.ai-dialog-fade-leave-to .ai-dialog {
  transform: translateY(20px) scale(0.96);
}

.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity 0.2s ease;
}

.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 600px) {
  .ai-dialog {
    width: 100%;
    height: 100%;
    max-height: 100vh;
    border-radius: 0;
  }

  .msg-bubble {
    max-width: 88%;
  }

  .result-actions {
    gap: 4px;
  }

  .result-action-btn {
    padding: 5px 8px;
    font-size: 11px;
  }
}
</style>
