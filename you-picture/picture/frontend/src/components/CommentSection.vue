<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  UserOutlined,
  DeleteOutlined,
  MessageOutlined,
  DownOutlined,
  LoadingOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  addCommentUsingPost,
  deleteCommentUsingPost,
  listCommentsUsingGet,
  listRepliesUsingGet,
} from '@/api/pictureCommentController'

// ===== 类型定义 =====
interface CommentUser {
  id: number
  userName?: string
  userAvatar?: string
  userRole?: string
}

interface CommentItem {
  id: number
  pictureId?: number
  content?: string
  createTime?: string
  userInfo?: CommentUser
  parentId?: number
  replyCount?: number
  replies?: CommentItem[]
  replyUserId?: number
  replyUserInfo?: CommentUser
}

// ===== Props =====
const props = defineProps<{ pictureId: number | undefined }>()

const userStore = useUserStore()

// ===== 评论列表状态 =====
const comments = ref<CommentItem[]>([])
const loading = ref(false)
const total = ref(0)
const current = ref(1)
const pageSize = 10

// ===== 发表评论 =====
const newContent = ref('')
const submitting = ref(false)

// ===== 回复状态 =====
// key = 父评论 id，value = 回复输入内容
const replyContent = ref<Record<number, string>>({})
// 当前展开回复输入框的评论 id
const activeReplyId = ref<number | null>(null)
const replySubmitting = ref<Record<number, boolean>>({})

// ===== 展开全部回复 =====
// key = 顶级评论 id，value = 全部回复列表
const expandedReplies = ref<Record<number, CommentItem[]>>({})
const replyLoading = ref<Record<number, boolean>>({})
// 是否已展开全部（即已加载）
const replyExpanded = ref<Record<number, boolean>>({})

// ===== 获取评论列表 =====
async function fetchComments(page = 1) {
  if (!props.pictureId) return
  loading.value = true
  try {
    const res: any = await listCommentsUsingGet({
      pictureId: props.pictureId,
      current: page,
      pageSize,
    })
    if (res?.data) {
      const data = res.data
      if (page === 1) {
        comments.value = data.records || data.list || data || []
      } else {
        comments.value = [...comments.value, ...(data.records || data.list || data || [])]
      }
      total.value = data.total ?? comments.value.length
      current.value = page
    }
  } catch {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

// ===== 发表顶级评论 =====
async function handleSubmit() {
  if (!userStore.isLoggedIn) {
    message.warning('请先登录后再发表评论')
    return
  }
  const content = newContent.value.trim()
  if (!content) {
    message.warning('评论内容不能为空')
    return
  }
  if (!props.pictureId) return
  submitting.value = true
  try {
    await addCommentUsingPost({ pictureId: props.pictureId, content })
    newContent.value = ''
    message.success('评论成功')
    // 刷新回第一页
    await fetchComments(1)
  } catch {
    // 拦截器已处理
  } finally {
    submitting.value = false
  }
}

// ===== 发表回复 =====
async function handleReply(parentComment: CommentItem) {
  if (!userStore.isLoggedIn) {
    message.warning('请先登录后再回复')
    return
  }
  const content = (replyContent.value[parentComment.id] || '').trim()
  if (!content) {
    message.warning('回复内容不能为空')
    return
  }
  if (!props.pictureId) return
  replySubmitting.value[parentComment.id] = true
  try {
    await addCommentUsingPost({
      pictureId: props.pictureId,
      content,
      parentId: parentComment.id,
      replyUserId: parentComment.userInfo?.id,
    })
    replyContent.value[parentComment.id] = ''
    activeReplyId.value = null
    message.success('回复成功')
    // 如果该评论已展开全部回复则重新加载全部回复，否则刷新列表
    if (replyExpanded.value[parentComment.id]) {
      await loadAllReplies(parentComment)
    } else {
      await fetchComments(1)
    }
  } catch {
    // 拦截器已处理
  } finally {
    replySubmitting.value[parentComment.id] = false
  }
}

// ===== 展开全部回复 =====
async function loadAllReplies(comment: CommentItem) {
  replyLoading.value[comment.id] = true
  try {
    const res: any = await listRepliesUsingGet({
      parentId: comment.id,
      current: 1,
      pageSize: 100,
    })
    if (res?.data) {
      expandedReplies.value[comment.id] = res.data.records || res.data.list || res.data || []
      replyExpanded.value[comment.id] = true
    }
  } catch {
    // 拦截器已处理
  } finally {
    replyLoading.value[comment.id] = false
  }
}

// 获取当前展示的回复列表（展开后用全部，否则用前3条）
function getDisplayedReplies(comment: CommentItem): CommentItem[] {
  if (replyExpanded.value[comment.id]) {
    return expandedReplies.value[comment.id] || []
  }
  return comment.replies || []
}

// ===== 删除评论 =====
function handleDelete(comment: CommentItem) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这条评论吗？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteCommentUsingPost({ id: comment.id })
        message.success('已删除')
        await fetchComments(1)
      } catch {
        // 拦截器已处理
      }
    },
  })
}

// ===== 权限判断 =====
function canDelete(comment: CommentItem) {
  if (!userStore.isLoggedIn) return false
  return (
    userStore.isAdmin ||
    String(comment.userInfo?.id) === String(userStore.loginUser?.id)
  )
}

// ===== 时间格式化 =====
function formatTime(timeStr?: string) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffH = Math.floor(diffMin / 60)
  if (diffH < 24) return `${diffH}小时前`
  const diffD = Math.floor(diffH / 24)
  if (diffD < 30) return `${diffD}天前`
  return d.toLocaleDateString('zh-CN')
}

// ===== 加载更多 =====
const hasMore = ref(true)
async function loadMore() {
  const nextPage = current.value + 1
  const prevLen = comments.value.length
  await fetchComments(nextPage)
  if (comments.value.length >= total.value || comments.value.length === prevLen) {
    hasMore.value = false
  }
}

onMounted(() => {
  if (props.pictureId) fetchComments(1)
})
</script>

<template>
  <div class="comment-section">
    <div class="section-title">
      <MessageOutlined class="title-icon" />
      评论
      <span v-if="total > 0" class="comment-count">{{ total }}</span>
    </div>

    <!-- 发表评论输入框 -->
    <div class="comment-input-wrap">
      <a-avatar :src="userStore.loginUser?.userAvatar" :size="36" class="input-avatar">
        <template #icon><UserOutlined /></template>
      </a-avatar>
      <div class="input-area">
        <a-textarea
          v-model:value="newContent"
          placeholder="说点什么…"
          :rows="2"
          :maxlength="500"
          :disabled="!userStore.isLoggedIn"
          class="comment-textarea"
          @keydown.ctrl.enter="handleSubmit"
        />
        <div class="input-footer">
          <span class="input-hint">{{ userStore.isLoggedIn ? 'Ctrl+Enter 快速发送' : '登录后发表评论' }}</span>
          <a-button
            type="primary"
            size="small"
            :loading="submitting"
            :disabled="!userStore.isLoggedIn || !newContent.trim()"
            class="submit-btn"
            @click="handleSubmit"
          >
            发表
          </a-button>
        </div>
      </div>
    </div>

    <!-- 评论列表 -->
    <div v-if="loading && comments.length === 0" class="loading-wrap">
      <a-spin size="small" />
      <span class="loading-text">加载评论中…</span>
    </div>

    <div v-else-if="comments.length === 0" class="empty-comments">
      <MessageOutlined class="empty-icon" />
      <p>还没有评论，来说第一句话吧</p>
    </div>

    <div v-else class="comment-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <!-- 顶级评论 -->
        <div class="comment-main">
          <a-avatar :src="comment.userInfo?.userAvatar" :size="36" class="comment-avatar">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-username">{{ comment.userInfo?.userName || '匿名用户' }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-actions">
              <button
                class="action-btn reply-action"
                @click="activeReplyId = activeReplyId === comment.id ? null : comment.id"
              >
                回复
              </button>
              <button
                v-if="canDelete(comment)"
                class="action-btn delete-action"
                @click="handleDelete(comment)"
              >
                <DeleteOutlined /> 删除
              </button>
            </div>
          </div>
        </div>

        <!-- 回复区域 -->
        <div class="reply-section">
          <!-- 已有回复列表 -->
          <div
            v-if="getDisplayedReplies(comment).length > 0"
            class="reply-list"
          >
            <div
              v-for="reply in getDisplayedReplies(comment)"
              :key="reply.id"
              class="reply-item"
            >
              <a-avatar :src="reply.userInfo?.userAvatar" :size="28" class="reply-avatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <div class="reply-body">
                <div class="reply-header">
                  <span class="reply-username">{{ reply.userInfo?.userName || '匿名用户' }}</span>
                  <span v-if="reply.replyUserInfo?.userName" class="reply-to">
                    回复 <em>{{ reply.replyUserInfo.userName }}</em>
                  </span>
                  <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                </div>
                <div class="reply-content">{{ reply.content }}</div>
                <div class="reply-actions">
                  <button
                    class="action-btn reply-action"
                    @click="activeReplyId = activeReplyId === reply.id ? null : reply.id"
                  >
                    回复
                  </button>
                  <button
                    v-if="canDelete(reply)"
                    class="action-btn delete-action"
                    @click="handleDelete(reply)"
                  >
                    <DeleteOutlined /> 删除
                  </button>
                </div>

                <!-- 回复某条 reply 的输入框 -->
                <div v-if="activeReplyId === reply.id" class="reply-input-wrap">
                  <a-textarea
                    v-model:value="replyContent[comment.id]"
                    :placeholder="`回复 ${reply.userInfo?.userName || '用户'}…`"
                    :rows="2"
                    :maxlength="500"
                    class="reply-textarea"
                    @keydown.ctrl.enter="handleReply(comment)"
                  />
                  <div class="reply-input-actions">
                    <a-button size="small" @click="activeReplyId = null">取消</a-button>
                    <a-button
                      type="primary"
                      size="small"
                      :loading="replySubmitting[comment.id]"
                      :disabled="!(replyContent[comment.id] || '').trim()"
                      @click="handleReply(comment)"
                    >
                      回复
                    </a-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 展开全部回复按钮 -->
            <button
              v-if="!replyExpanded[comment.id] && (comment.replyCount || 0) > (comment.replies?.length || 0)"
              class="expand-replies-btn"
              :disabled="replyLoading[comment.id]"
              @click="loadAllReplies(comment)"
            >
              <LoadingOutlined v-if="replyLoading[comment.id]" />
              <DownOutlined v-else />
              查看全部 {{ comment.replyCount }} 条回复
            </button>
          </div>

          <!-- 回复顶级评论的输入框 -->
          <div v-if="activeReplyId === comment.id" class="reply-input-wrap top-reply">
            <a-textarea
              v-model:value="replyContent[comment.id]"
              :placeholder="`回复 ${comment.userInfo?.userName || '用户'}…`"
              :rows="2"
              :maxlength="500"
              class="reply-textarea"
              @keydown.ctrl.enter="handleReply(comment)"
            />
            <div class="reply-input-actions">
              <a-button size="small" @click="activeReplyId = null">取消</a-button>
              <a-button
                type="primary"
                size="small"
                :loading="replySubmitting[comment.id]"
                :disabled="!(replyContent[comment.id] || '').trim()"
                @click="handleReply(comment)"
              >
                回复
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore && comments.length < total" class="load-more-wrap">
        <a-button block :loading="loading" class="load-more-btn" @click="loadMore">
          加载更多评论
        </a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 容器 ===== */
.comment-section {
  padding: var(--space-sm) 0 0;
  width: 100%;
  box-sizing: border-box;
}

.section-title {
  font-size: 16px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-lg);
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  color: var(--color-primary);
}

.comment-count {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: var(--weight-normal);
}

/* ===== 发表评论 ===== */
.comment-input-wrap {
  display: flex;
  gap: 12px;
  margin-bottom: var(--space-xl);
  align-items: flex-start;
}

.input-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.input-area {
  flex: 1;
  min-width: 0;
}

.comment-textarea {
  border-radius: var(--radius-sm);
  resize: none;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: var(--color-text-disabled);
}

.submit-btn {
  border-radius: var(--radius-button);
}

/* ===== 加载/空 ===== */
.loading-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: var(--space-xl) 0;
  color: var(--color-text-secondary);
}

.loading-text {
  font-size: 14px;
}

.empty-comments {
  text-align: center;
  padding: var(--space-xl) 0;
  color: var(--color-text-disabled);
}

.empty-icon {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}

/* ===== 评论列表 ===== */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.comment-item {
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-surface-sand);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-main {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.comment-avatar {
  flex-shrink: 0;
  background: var(--color-surface-sand);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-username {
  font-size: 14px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
}

.comment-time {
  font-size: 12px;
  color: var(--color-text-disabled);
}

.comment-content {
  font-size: 14px;
  color: var(--color-text-primary);
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
  margin-bottom: 8px;
}

/* ===== 操作按钮 ===== */
.comment-actions,
.reply-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-btn {
  font-size: 12px;
  color: var(--color-text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 0;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.15s;
}

.reply-action:hover {
  color: var(--color-primary);
}

.delete-action:hover {
  color: #ff4d4f;
}

/* ===== 回复区域 ===== */
.reply-section {
  margin-left: 48px;
  margin-top: 10px;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--color-bg-warm);
  border-radius: var(--radius-sm);
  padding: 12px;
}

.reply-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.reply-avatar {
  flex-shrink: 0;
  background: var(--color-surface-sand);
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 2px;
}

.reply-username {
  font-size: 13px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
}

.reply-to {
  font-size: 12px;
  color: var(--color-text-disabled);
}

.reply-to em {
  font-style: normal;
  color: var(--color-primary);
}

.reply-time {
  font-size: 12px;
  color: var(--color-text-disabled);
}

.reply-content {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
  margin-bottom: 4px;
}

/* ===== 展开全部回复按钮 ===== */
.expand-replies-btn {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-primary);
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  transition: opacity 0.15s;
}

.expand-replies-btn:hover {
  opacity: 0.75;
}

.expand-replies-btn:disabled {
  cursor: default;
  opacity: 0.6;
}

/* ===== 回复输入框 ===== */
.reply-input-wrap {
  margin-top: 10px;
}

.reply-input-wrap.top-reply {
  margin-left: 0;
  background: var(--color-bg-warm);
  border-radius: var(--radius-sm);
  padding: 10px;
}

.reply-textarea {
  border-radius: var(--radius-sm);
  resize: none;
  font-size: 13px;
}

.reply-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

/* ===== 加载更多 ===== */
.load-more-wrap {
  margin-top: var(--space-md);
}

.load-more-btn {
  border-radius: var(--radius-button);
  color: var(--color-text-secondary);
}
</style>
