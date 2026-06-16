<script setup lang="ts">
import { ref, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  SendOutlined,
  DownloadOutlined,
  ClearOutlined,
  ReloadOutlined,
  SaveOutlined,
  CheckOutlined,
  CloseOutlined,
  ExpandOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import {
  createText2ImageTaskUsingPost,
  getText2ImageTaskUsingGet,
  uploadUrlPictureUsingPost,
} from '@/api/pictureController'

const router = useRouter()

// ===== 生成记录模型 =====
interface GenerationRecord {
  id: string
  prompt: string
  imageUrls: string[]
  status: 'generating' | 'success' | 'error'
  taskId?: string
  errorMsg?: string
  size?: string
  createdAt: number
}

// ===== 状态 =====
const records = ref<GenerationRecord[]>([])
const inputText = ref('')
const isPolling = ref(false)
const selectedSize = ref('1024*1024')
const galleryRef = ref<HTMLElement>()
const previewVisible = ref(false)
const previewImageUrl = ref('')
const selectedImageUrl = ref('')
const showHistory = ref(false)

const MAX_INPUT_LENGTH = 800
const POLL_INTERVAL = 2000
const MAX_POLL_ATTEMPTS = 60

const sizeOptions = [
  { label: '1:1', value: '1024*1024', desc: '1024×1024', icon: '⬜' },
  { label: '9:16', value: '720*1280', desc: '720×1280', icon: '📱' },
  { label: '16:9', value: '1280*720', desc: '1280×720', icon: '🖥️' },
]

const inputLength = computed(() => inputText.value.length)
const canSend = computed(() => inputText.value.trim().length > 0 && !isPolling.value)
const currentRecord = computed(() => records.value.length > 0 ? records.value[0] : null)
const historyRecords = computed(() => records.value.filter(r => r.status === 'success').slice(1))
const currentSizeOption = computed(() => sizeOptions.find(o => o.value === selectedSize.value))

// ===== 工具 =====
function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 9)
}

function scrollGalleryTop() {
  nextTick(() => {
    if (galleryRef.value) {
      galleryRef.value.scrollTop = 0
    }
  })
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// ===== 清空记录 =====
function handleClear() {
  records.value = []
  selectedImageUrl.value = ''
}

// ===== 保存到图库 =====
async function handleSaveToSpace(url: string) {
  if (!url) return

  message.loading({ content: '正在保存到图库...', key: 'save-space' })
  try {
    const res: any = await uploadUrlPictureUsingPost({
      fileUrl: url,
      picName: `ai-generated-${Date.now()}`,
    })
    if (res?.data?.id) {
      message.success({ content: '已保存，正在跳转编辑页...', key: 'save-space' })
      router.push(`/picture/edit/${res.data.id}?from=ai`)
    } else {
      message.error({ content: '保存失败', key: 'save-space' })
    }
  } catch {
    message.error({ content: '保存失败', key: 'save-space' })
  }
}

// ===== 发送生成请求 =====
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

  const record: GenerationRecord = {
    id: generateId(),
    prompt: text,
    imageUrls: [],
    status: 'generating',
    size: selectedSize.value,
    createdAt: Date.now(),
  }
  records.value.unshift(record)
  // 获取响应式代理对象，确保后续修改能触发 UI 更新
  const reactiveRecord = records.value[0]

  inputText.value = ''
  selectedImageUrl.value = ''
  showHistory.value = false
  scrollGalleryTop()

  isPolling.value = true
  try {
    const res: any = await createText2ImageTaskUsingPost({
      prompt: text,
      size: selectedSize.value,
      n: 4,
    })
    const taskId = res?.data?.output?.taskId

    if (!taskId) {
      throw new Error('任务创建失败')
    }

    reactiveRecord.taskId = taskId
    await pollTask(reactiveRecord)
  } catch (err: any) {
    reactiveRecord.status = 'error'
    reactiveRecord.errorMsg = err?.message || '任务创建失败，请重试'
  } finally {
    isPolling.value = false
  }
}

// ===== 轮询任务状态 =====
async function pollTask(record: GenerationRecord) {
  for (let attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
    await sleep(POLL_INTERVAL)

    if (document.hidden) {
      await waitForVisible()
    }

    try {
      const res: any = await getText2ImageTaskUsingGet({ taskId: record.taskId })
      const output = res?.data?.output
      console.log('[pollTask] response:', JSON.stringify(res?.data), 'output:', JSON.stringify(output))
      if (!output) continue

      switch (output.taskStatus) {
        case 'SUCCEEDED': {
          const results = output.results
          console.log('[pollTask] SUCCEEDED, results count:', results?.length, 'results:', JSON.stringify(results))
          if (results && results.length > 0) {
            record.imageUrls = results.map((r: any) => r.url).filter(Boolean)
            record.status = 'success'
            if (record.imageUrls.length > 0) {
              selectedImageUrl.value = record.imageUrls[0]
            }
            return
          }
          throw new Error('未获取到结果图片')
        }
        case 'FAILED':
          throw new Error(output.message || '生成失败')
        case 'PENDING':
        case 'RUNNING':
          break
        default:
          throw new Error('未知任务状态')
      }
    } catch (err: any) {
      record.status = 'error'
      record.errorMsg = err?.message || '查询任务状态失败'
      return
    }
  }

  record.status = 'error'
  record.errorMsg = '任务超时（超过 2 分钟），请重试'
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
async function handleRetry(record: GenerationRecord) {
  if (isPolling.value) {
    message.warning('当前有任务进行中')
    return
  }

  record.status = 'generating'
  record.errorMsg = undefined
  record.imageUrls = []
  selectedImageUrl.value = ''

  isPolling.value = true
  try {
    const res: any = await createText2ImageTaskUsingPost({
      prompt: record.prompt,
      size: record.size || selectedSize.value,
      n: 4,
    })
    const taskId = res?.data?.output?.taskId

    if (!taskId) throw new Error('任务创建失败')

    record.taskId = taskId
    await pollTask(record)
  } catch (err: any) {
    record.status = 'error'
    record.errorMsg = err?.message || '重试失败'
  } finally {
    isPolling.value = false
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

// ===== 使用示例提示词 =====
function usePrompt(text: string) {
  inputText.value = text
}

// ===== 查看历史记录的某条 =====
function viewHistoryRecord(record: GenerationRecord) {
  selectedImageUrl.value = record.imageUrls[0] || ''
  // 将该记录移到最前面
  const idx = records.value.findIndex(r => r.id === record.id)
  if (idx > 0) {
    const [item] = records.value.splice(idx, 1)
    records.value.unshift(item)
  }
  showHistory.value = false
}
</script>

<template>
  <div class="ai-studio">
    <!-- 浮动清空按钮 -->
    <Transition name="fade">
      <button
        v-if="records.length > 0"
        class="floating-clear-btn"
        @click="handleClear"
        title="清空记录"
      >
        <ClearOutlined />
      </button>
    </Transition>

    <!-- ===== 主体 ===== -->
    <main class="studio-main" ref="galleryRef">
      <!-- 空状态 -->
      <div v-if="!currentRecord" class="welcome-screen">
        <div class="welcome-visual">
          <div class="welcome-orb orb-1"></div>
          <div class="welcome-orb orb-2"></div>
          <div class="welcome-orb orb-3"></div>
          <h2 class="welcome-title">用文字，绘万象</h2>
          <p class="welcome-sub">描述你脑海中的画面，AI 将为你生成 4 张独特的图片</p>
        </div>

        <div class="prompt-examples">
          <h3 class="examples-label">试试这些灵感</h3>
          <div class="examples-grid">
            <button class="example-chip" @click="usePrompt('一间有着精致落地窗的花店，午后阳光洒入，暖色调，ins 风格')">
              <span class="chip-emoji">🌸</span>
              <span class="chip-text">午后阳光花店</span>
            </button>
            <button class="example-chip" @click="usePrompt('赛博朋克风格的未来城市夜景，霓虹灯倒映在雨水路面上')">
              <span class="chip-emoji">🌆</span>
              <span class="chip-text">赛博朋克夜景</span>
            </button>
            <button class="example-chip" @click="usePrompt('水墨画风格的中国山水风景，远处有小桥流水人家，意境悠远')">
              <span class="chip-emoji">🏔️</span>
              <span class="chip-text">水墨山水意境</span>
            </button>
            <button class="example-chip" @click="usePrompt('一只穿着宇航服的橘猫漂浮在太空中，背景是蓝色星球，梦幻风格')">
              <span class="chip-emoji">🐱</span>
              <span class="chip-text">太空宇航猫</span>
            </button>
            <button class="example-chip" @click="usePrompt('吉卜力风格的森林小木屋，门前有一条溪流，周围被绿色植物环绕')">
              <span class="chip-emoji">🏡</span>
              <span class="chip-text">吉卜力小屋</span>
            </button>
            <button class="example-chip" @click="usePrompt('极简主义风格的北欧客厅，大落地窗外是雪景，室内有壁炉，温馨')">
              <span class="chip-emoji">🛋️</span>
              <span class="chip-text">北欧雪景客厅</span>
            </button>
          </div>
        </div>


      </div>

      <!-- 生成中状态 -->
      <div v-else-if="currentRecord.status === 'generating'" class="generating-screen">
        <div class="gen-animation">
          <div class="gen-ring ring-outer"></div>
          <div class="gen-ring ring-middle"></div>
          <div class="gen-ring ring-inner"></div>
          <div class="gen-core">
            <svg viewBox="0 0 24 24" fill="none" class="gen-icon">
              <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" fill="currentColor"/>
            </svg>
          </div>
        </div>
        <div class="gen-info">
          <h3 class="gen-title">AI 正在创作中...</h3>
          <p class="gen-prompt">「{{ currentRecord.prompt }}」</p>
          <div class="gen-progress">
            <div class="gen-progress-bar"></div>
          </div>
          <p class="gen-hint">通常需要 10~30 秒，请耐心等待</p>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="currentRecord.status === 'error'" class="error-screen">
        <div class="error-icon-wrap">
          <CloseOutlined />
        </div>
        <h3 class="error-title">生成失败</h3>
        <p class="error-detail">{{ currentRecord.errorMsg || '未知错误' }}</p>
        <p class="error-prompt">提示词：「{{ currentRecord.prompt }}」</p>
        <button class="retry-button" @click="handleRetry(currentRecord)">
          <ReloadOutlined />
          <span>重新生成</span>
        </button>
      </div>

      <!-- 结果展示 - 多图网格 -->
      <div v-else-if="currentRecord.status === 'success'" class="result-screen">
        <div class="result-header">
          <div class="result-prompt-info">
            <div class="result-prompt-text">
              <span class="result-label">文字生图</span>
              <p class="result-prompt">{{ currentRecord.prompt }}</p>
            </div>
          </div>
          <button class="regenerate-btn" @click="handleRetry(currentRecord)" :disabled="isPolling">
            <ReloadOutlined />
            <span>重新生成</span>
          </button>
        </div>

        <!-- 2x2 图片网格 -->
        <div class="image-grid" :class="{ 'grid-single': currentRecord.imageUrls.length === 1 }">
          <div
            v-for="(url, idx) in currentRecord.imageUrls"
            :key="idx"
            class="grid-item"
            :class="{ selected: selectedImageUrl === url }"
            @click="selectedImageUrl = url"
          >
            <img :src="url" :alt="`生成图片 ${idx + 1}`" loading="lazy" />
            <div class="grid-item-index">{{ idx + 1 }}</div>
            <div class="grid-item-overlay">
              <button class="overlay-btn" @click.stop="openPreview(url)" title="放大查看">
                <ExpandOutlined />
              </button>
            </div>
            <div v-if="selectedImageUrl === url" class="grid-item-check">
              <CheckOutlined />
            </div>
          </div>
        </div>

        <!-- 选中图片的操作栏 -->
        <Transition name="actions-slide">
          <div v-if="selectedImageUrl" class="selected-actions">
            <div class="selected-preview-mini" @click="openPreview(selectedImageUrl)">
              <img :src="selectedImageUrl" alt="已选" />
            </div>
            <div class="selected-actions-list">
              <button class="sa-btn sa-primary" @click="handleDownload(selectedImageUrl)">
                <DownloadOutlined />
                <span>下载图片</span>
              </button>
              <button class="sa-btn" @click="handleSaveToSpace(selectedImageUrl)">
                <SaveOutlined />
                <span>保存到图库</span>
              </button>
            </div>
          </div>
        </Transition>
      </div>

      <!-- 历史面板（Overlay） -->
      <Transition name="history-slide">
        <div v-if="showHistory && historyRecords.length > 0" class="history-panel">
          <div class="history-header">
            <h3>生成历史</h3>
            <button class="history-close" @click="showHistory = false">
              <CloseOutlined />
            </button>
          </div>
          <div class="history-list">
            <div
              v-for="rec in historyRecords"
              :key="rec.id"
              class="history-item"
              @click="viewHistoryRecord(rec)"
            >
              <img v-if="rec.imageUrls[0]" :src="rec.imageUrls[0]" class="history-thumb" :alt="rec.prompt" />
              <div class="history-info">
                <p class="history-prompt">{{ rec.prompt }}</p>
                <span class="history-meta">
                  {{ rec.imageUrls.length }} 张 · 文生图
                </span>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </main>

    <!-- ===== 底部输入区 ===== -->
    <footer class="studio-footer">
      <div class="footer-container">
        <div class="input-bar">
          <!-- 尺寸选择 -->
          <div class="size-pills">
            <button
              v-for="opt in sizeOptions"
              :key="opt.value"
              class="size-pill"
              :class="{ active: selectedSize === opt.value }"
              @click="selectedSize = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>

          <!-- 输入框 -->
          <div class="input-field">
            <input
              v-model="inputText"
              class="prompt-input"
              :placeholder="'描述你想要的画面…'"
              :maxlength="MAX_INPUT_LENGTH"
              @keydown.enter.prevent="handleSend"
            />
            <span v-if="inputLength > 0" class="char-count" :class="{ warn: inputLength > MAX_INPUT_LENGTH - 50 }">
              {{ inputLength }}/{{ MAX_INPUT_LENGTH }}
            </span>
          </div>

          <!-- 发送 -->
          <button class="send-button" :class="{ active: canSend }" :disabled="!canSend" @click="handleSend">
            <SendOutlined />
          </button>
        </div>
      </div>
    </footer>

    <!-- ===== 图片预览弹窗 ===== -->
    <Transition name="preview-fade">
      <div v-if="previewVisible" class="preview-mask" @click="closePreview">
        <img :src="previewImageUrl" alt="预览" class="preview-img" @click.stop />
        <button class="preview-close" @click="closePreview">
          <CloseOutlined />
        </button>
        <div class="preview-actions" @click.stop>
          <button class="pa-btn" @click="handleDownload(previewImageUrl)">
            <DownloadOutlined /> 下载
          </button>
          <button class="pa-btn" @click="handleSaveToSpace(previewImageUrl)">
            <SaveOutlined /> 保存
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* ===== 页面根容器 ===== */
.ai-studio {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background: var(--color-bg-warm);
  color: var(--color-text-primary);
  font-family: var(--font-body);
  position: relative;
  overflow: hidden;
}

/* ===== 浮动清空按钮 ===== */
.floating-clear-btn {
  position: absolute;
  top: 16px;
  right: 24px;
  z-index: 20;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid var(--color-surface-sand);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--color-text-secondary);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.floating-clear-btn:hover {
  color: var(--color-primary);
  border-color: rgba(230, 0, 35, 0.3);
  background: #fff;
  box-shadow: 0 4px 16px rgba(230, 0, 35, 0.1);
  transform: scale(1.08);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== 主体区域 ===== */
.studio-main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
}

.studio-main::-webkit-scrollbar { width: 5px; }
.studio-main::-webkit-scrollbar-track { background: transparent; }
.studio-main::-webkit-scrollbar-thumb { background: var(--color-surface-sand); border-radius: 3px; }

/* ===== 欢迎页 ===== */
.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 60px 28px 40px;
  gap: 48px;
}

.welcome-visual {
  position: relative;
  text-align: center;
  padding: 40px 0;
}

.welcome-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.15;
  animation: orbFloat 8s ease-in-out infinite;
}

.orb-1 {
  width: 200px;
  height: 200px;
  background: var(--color-primary);
  top: -40px;
  left: 50%;
  margin-left: -140px;
  animation-delay: 0s;
}

.orb-2 {
  width: 160px;
  height: 160px;
  background: #f59e42;
  top: 10px;
  left: 50%;
  margin-left: 20px;
  animation-delay: -3s;
}

.orb-3 {
  width: 140px;
  height: 140px;
  background: var(--color-focus);
  top: 50px;
  left: 50%;
  margin-left: -60px;
  animation-delay: -5s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(15px, -10px) scale(1.05); }
  66% { transform: translate(-10px, 8px) scale(0.95); }
}

.welcome-title {
  font-size: 42px;
  font-weight: 900;
  margin: 0 0 12px;
  color: var(--color-text-primary);
  letter-spacing: -0.03em;
  position: relative;
  font-family: var(--font-display);
}

.welcome-sub {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
  max-width: 420px;
  line-height: 1.6;
  position: relative;
}

.prompt-examples {
  width: 100%;
  max-width: 640px;
}

.examples-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-disabled);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin: 0 0 16px;
  text-align: center;
}

.examples-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.example-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 100px;
  border: 1px solid var(--color-surface-sand);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
}

.example-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-text-primary);
  background: rgba(230, 0, 35, 0.04);
  transform: translateY(-1px);
  box-shadow: var(--shadow-card);
}

.chip-emoji {
  font-size: 16px;
}

.chip-text {
  font-weight: 500;
}

/* ===== 生成中 ===== */
.generating-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 60px 28px;
  gap: 40px;
}

.gen-animation {
  position: relative;
  width: 140px;
  height: 140px;
}

.gen-ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
}

.ring-outer {
  inset: 0;
  border-top-color: var(--color-primary);
  border-right-color: rgba(230, 0, 35, 0.2);
  animation: spinSlow 3s linear infinite;
}

.ring-middle {
  inset: 16px;
  border-top-color: #f59e42;
  border-left-color: rgba(245, 158, 66, 0.2);
  animation: spinSlow 2s linear infinite reverse;
}

.ring-inner {
  inset: 32px;
  border-bottom-color: var(--color-focus);
  border-right-color: rgba(67, 94, 229, 0.2);
  animation: spinSlow 1.5s linear infinite;
}

@keyframes spinSlow {
  to { transform: rotate(360deg); }
}

.gen-core {
  position: absolute;
  inset: 46px;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: corePulse 2s ease-in-out infinite;
}

@keyframes corePulse {
  0%, 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(230, 0, 35, 0.3); }
  50% { transform: scale(1.08); box-shadow: 0 0 24px 8px rgba(230, 0, 35, 0.15); }
}

.gen-icon {
  width: 22px;
  height: 22px;
  color: #fff;
}

.gen-info {
  text-align: center;
  max-width: 400px;
}

.gen-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 10px;
}

.gen-prompt {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 24px;
  line-height: 1.6;
}

.gen-progress {
  width: 200px;
  height: 3px;
  border-radius: 2px;
  background: var(--color-surface-sand);
  margin: 0 auto 12px;
  overflow: hidden;
}

.gen-progress-bar {
  width: 40%;
  height: 100%;
  border-radius: 2px;
  background: var(--color-primary);
  animation: progressSlide 2s ease-in-out infinite;
}

@keyframes progressSlide {
  0% { transform: translateX(-100%); width: 40%; }
  50% { width: 60%; }
  100% { transform: translateX(300%); width: 40%; }
}

.gen-hint {
  font-size: 12px;
  color: var(--color-text-disabled);
  margin: 0;
}

/* ===== 错误状态 ===== */
.error-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 60px 28px;
  gap: 12px;
}

.error-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(158, 10, 10, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--color-error);
  margin-bottom: 8px;
}

.error-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.error-detail {
  font-size: 14px;
  color: var(--color-error);
  margin: 0;
}

.error-prompt {
  font-size: 13px;
  color: var(--color-text-disabled);
  margin: 0;
}

.retry-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 10px 24px;
  border-radius: var(--radius-base);
  border: 1px solid var(--color-surface-sand);
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.retry-button:hover {
  background: var(--color-bg-warm);
  border-color: var(--color-border-hover);
  box-shadow: var(--shadow-card);
}

/* ===== 结果展示 ===== */
.result-screen {
  padding: 24px 28px 120px;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
}

.result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
}

.result-prompt-info {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.result-source-img {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-base);
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  border: 1px solid var(--color-surface-sand);
}

.result-source-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.result-label {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-primary);
  margin-bottom: 4px;
}

.result-prompt {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.regenerate-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-base);
  border: 1px solid var(--color-surface-sand);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
  font-family: inherit;
}

.regenerate-btn:hover:not(:disabled) {
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
  background: var(--color-bg);
}

.regenerate-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ===== 2×2 图片网格 ===== */
.image-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.image-grid.grid-single {
  grid-template-columns: 1fr;
  max-width: 520px;
}

.grid-item {
  position: relative;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  background: var(--color-bg);
  border: 2px solid transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  aspect-ratio: 1;
}

.grid-item:hover {
  border-color: var(--color-border-hover);
  transform: scale(1.01);
  box-shadow: var(--shadow-card);
}

.grid-item.selected {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px rgba(230, 0, 35, 0.2), 0 8px 32px rgba(230, 0, 35, 0.08);
}

.grid-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.grid-item:hover img {
  transform: scale(1.03);
}

.grid-item-index {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s;
}

.grid-item:hover .grid-item-index {
  opacity: 1;
}

.grid-item-overlay {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.25s;
}

.grid-item:hover .grid-item-overlay {
  opacity: 1;
}

.overlay-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.overlay-btn:hover {
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  transform: scale(1.1);
}

.grid-item-check {
  position: absolute;
  bottom: 10px;
  right: 10px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(230, 0, 35, 0.3);
  animation: checkPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes checkPop {
  from { transform: scale(0); }
  to { transform: scale(1); }
}

/* ===== 选中操作栏 ===== */
.selected-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
  padding: 14px 18px;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  border: 1px solid var(--color-surface-sand);
  box-shadow: var(--shadow-card);
}

.selected-preview-mini {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-base);
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  border: 1px solid var(--color-surface-sand);
}

.selected-preview-mini img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.selected-actions-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sa-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-base);
  border: 1px solid var(--color-surface-sand);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.sa-btn:hover {
  background: var(--color-bg-warm);
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
}

.sa-btn.sa-primary {
  background: var(--color-primary);
  border-color: transparent;
  color: #fff;
  font-weight: 600;
}

.sa-btn.sa-primary:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(230, 0, 35, 0.2);
}

/* ===== 历史面板 ===== */
.history-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 340px;
  height: 100%;
  background: var(--color-bg);
  border-left: 1px solid var(--color-surface-sand);
  z-index: 20;
  display: flex;
  flex-direction: column;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--color-surface-sand);
}

.history-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.history-close {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.history-close:hover {
  background: var(--color-bg-warm);
  color: var(--color-text-primary);
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.history-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-base);
  cursor: pointer;
  transition: background 0.2s;
}

.history-item:hover {
  background: var(--color-bg-warm);
}

.history-thumb {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid var(--color-surface-sand);
}

.history-info {
  flex: 1;
  min-width: 0;
}

.history-prompt {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0 0 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.history-meta {
  font-size: 11px;
  color: var(--color-text-disabled);
}

/* ===== 底部输入 ===== */
.studio-footer {
  flex-shrink: 0;
  border-top: 1px solid var(--color-surface-sand);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  z-index: 10;
}

.footer-container {
  max-width: 760px;
  margin: 0 auto;
  padding: 14px 28px 18px;
}

.attach-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  background: var(--color-bg-warm);
  border: 1px solid rgba(230, 0, 35, 0.12);
}

.attach-thumb-wrap {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.attach-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
}

.attach-status {
  position: absolute;
  bottom: -3px;
  right: -3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  border: 2px solid var(--color-bg);
}

.attach-status.uploading {
  background: #f59e42;
  color: #fff;
}

.attach-status.ready {
  background: var(--color-green);
  color: #fff;
}

.attach-meta {
  flex: 1;
  min-width: 0;
}

.attach-mode {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}

.attach-hint {
  display: block;
  font-size: 12px;
  color: var(--color-text-disabled);
  margin-top: 2px;
}

.attach-remove {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  border: none;
  background: rgba(158, 10, 10, 0.06);
  color: var(--color-error);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  flex-shrink: 0;
}

.attach-remove:hover {
  background: rgba(158, 10, 10, 0.12);
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  border: 1px solid var(--color-surface-sand);
  transition: border-color 0.25s, box-shadow 0.25s;
}

.input-bar:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(230, 0, 35, 0.06);
}

.bar-icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 9px;
  border: none;
  background: transparent;
  color: var(--color-text-disabled);
  font-size: 17px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.bar-icon-btn:hover:not(:disabled) {
  background: var(--color-bg-warm);
  color: var(--color-primary);
}

.bar-icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.size-pills {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
  padding: 2px;
  border-radius: 7px;
  background: var(--color-bg-warm);
}

.size-pill {
  padding: 5px 10px;
  border-radius: 5px;
  border: none;
  background: transparent;
  color: var(--color-text-disabled);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.size-pill:hover {
  color: var(--color-text-secondary);
}

.size-pill.active {
  background: var(--color-bg);
  color: var(--color-primary);
  box-shadow: var(--shadow-subtle);
}

.input-field {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  min-width: 0;
}

.prompt-input {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--color-text-primary);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  min-width: 0;
  padding: 8px 4px;
  padding-right: 48px;
  line-height: 1.4;
}

.prompt-input::placeholder {
  color: var(--color-text-disabled);
}

.char-count {
  position: absolute;
  right: 4px;
  font-size: 10px;
  color: var(--color-text-disabled);
  pointer-events: none;
}

.char-count.warn {
  color: var(--color-error);
}

.send-button {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-base);
  border: none;
  background: var(--color-surface-sand);
  color: var(--color-text-disabled);
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
  flex-shrink: 0;
}

.send-button.active {
  background: var(--color-primary);
  color: #fff;
  box-shadow: 0 4px 16px rgba(230, 0, 35, 0.2);
}

.send-button.active:hover {
  background: var(--color-primary-hover);
  transform: scale(1.06);
  box-shadow: 0 6px 24px rgba(230, 0, 35, 0.25);
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* ===== 图片预览弹窗 ===== */
.preview-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  backdrop-filter: blur(8px);
}

.preview-img {
  max-width: 88vw;
  max-height: 85vh;
  border-radius: var(--radius-sm);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.4);
  cursor: default;
}

.preview-close {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  backdrop-filter: blur(12px);
}

.preview-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.preview-actions {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  padding: 8px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid var(--color-surface-sand);
  backdrop-filter: blur(16px);
  box-shadow: var(--shadow-card);
}

.pa-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: var(--radius-base);
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.pa-btn:hover {
  background: var(--color-bg-warm);
  color: var(--color-text-primary);
}

/* ===== 动画 ===== */
.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity 0.25s ease;
}

.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}

.actions-slide-enter-active {
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.actions-slide-leave-active {
  transition: all 0.2s ease;
}

.actions-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.actions-slide-leave-to {
  opacity: 0;
  transform: translateY(5px);
}

.history-slide-enter-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.history-slide-leave-active {
  transition: transform 0.25s ease;
}

.history-slide-enter-from {
  transform: translateX(100%);
}

.history-slide-leave-to {
  transform: translateX(100%);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .studio-header {
    padding: 12px 16px;
  }

  .welcome-title {
    font-size: 30px;
  }

  .image-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .result-screen {
    padding: 16px 16px 120px;
  }

  .footer-container {
    padding: 12px 16px 16px;
  }

  .examples-grid {
    justify-content: flex-start;
  }

  .history-panel {
    width: 100%;
  }

  .result-header {
    flex-direction: column;
    gap: 12px;
  }

  .selected-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .welcome-title {
    font-size: 26px;
  }

  .image-grid {
    grid-template-columns: 1fr;
  }

  .example-chip {
    font-size: 12px;
    padding: 8px 14px;
  }

  .tool-label {
    display: none;
  }
}
</style>
