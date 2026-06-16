<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  LeftOutlined,
  LinkOutlined,
  HeartOutlined,
  HeartFilled,
  DownloadOutlined,
  DeleteOutlined,
  TagOutlined,
  UserOutlined,
  SendOutlined,
  MessageOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { getPictureVoByIdUsingGet, deletePictureUsingPost } from '@/api/pictureController'
import { likePictureUsingPost, getLikeCountUsingGet, getLikeStatusUsingGet } from '@/api/pictureLikeController'
import {
  addCommentUsingPost,
  deleteCommentUsingPost,
  listCommentsUsingGet,
  listRepliesUsingGet,
} from '@/api/pictureCommentController'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const picture = ref<API.PictureVO | null>(null)
const loading = ref(true)
const liked = ref(false)
const likeCount = ref(0)
const likeLoading = ref(false)

const isOwner = computed(() => {
  if (!picture.value?.userId || !userStore.loginUser?.id) return false
  return String(picture.value.userId) === String(userStore.loginUser.id)
})

const isPrivateSpace = computed(() => {
  return !!picture.value?.spaceId
})

const fileSize = computed(() => {
  if (!picture.value?.picSize) return ''
  const kb = picture.value.picSize / 1024
  if (kb < 1024) return kb.toFixed(1) + ' KB'
  return (kb / 1024).toFixed(2) + ' MB'
})

async function fetchDetail() {
  loading.value = true
  try {
    const id = route.params.id as string
    if (!id) {
      message.error('参数错误')
      router.back()
      return
    }
    const res: any = await getPictureVoByIdUsingGet({ id })
    if (res?.data) {
      picture.value = res.data
      // 获取点赞状态和点赞数
      fetchLikeInfo(res.data.id)
      // 加载评论
      fetchComments(1)
    } else {
      message.error('图片不存在或已被删除')
      router.back()
    }
  } catch (e: any) {
    if (e?.message === '未登录') {
      message.warning('请先登录后再查看图片详情')
      router.push({ path: '/login', query: { redirect: route.fullPath } })
    } else {
      message.error('加载失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

async function fetchLikeInfo(pictureId: any) {
  try {
    const [countRes, statusRes]: any[] = await Promise.all([
      getLikeCountUsingGet({ pictureId }),
      userStore.isLoggedIn ? getLikeStatusUsingGet({ pictureId }) : Promise.resolve(null),
    ])
    if (countRes?.data !== undefined) {
      likeCount.value = countRes.data
    }
    if (statusRes?.data !== undefined) {
      liked.value = statusRes.data
    }
  } catch {
    // 静默
  }
}

async function handleLike() {
  if (!userStore.isLoggedIn) {
    message.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (likeLoading.value) return
  likeLoading.value = true
  try {
    const res: any = await likePictureUsingPost({ pictureId: picture.value?.id })
    if (res?.data) {
      liked.value = res.data.isLiked ?? !liked.value
      likeCount.value = res.data.likeCount ?? likeCount.value
    }
  } catch {
    // 拦截器已处理
  } finally {
    likeLoading.value = false
  }
}

function handleDelete() {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除图片「${picture.value?.name || '无标题'}」吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deletePictureUsingPost({ id: picture.value?.id })
        message.success('删除成功')
        // 根据来源返回对应页面
        const from = route.query.from as string
        if (from === 'public' || from === 'liked' || from === 'private') {
          router.replace(`/profile?tab=${from}`)
        } else {
          router.replace('/')
        }
      } catch {
        // 拦截器已处理
      }
    },
  })
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    message.success('分享链接已复制到剪贴板')
  } catch {
    message.warning('复制失败，请手动复制地址栏链接')
  }
}

function normalizeColor(color?: string) {
  if (!color) return ''
  if (color.startsWith('0x') || color.startsWith('0X')) {
    return '#' + color.slice(2)
  }
  if (/^[0-9a-fA-F]{6}$/.test(color)) {
    return '#' + color
  }
  return color
}

function goToUserProfile(userId?: number) {
  if (userId) {
    if (userStore.isLoggedIn && String(userId) === String(userStore.loginUser?.id)) {
      router.push('/profile')
    } else {
      router.push(`/user/${userId}`)
    }
  }
}

function handleGoBack() {
  const from = route.query.from as string
  if (from === 'public' || from === 'liked' || from === 'private') {
    router.push(`/profile?tab=${from}`)
  } else {
    router.back()
  }
}

function downloadPicture() {
  if (!picture.value?.url) return
  const url = picture.value.url
  const fileName = picture.value.name || 'picture'
  fetch(url, { mode: 'cors' })
    .then((res) => res.blob())
    .then((blob) => {
      const blobUrl = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = blobUrl
      link.download = fileName
      link.click()
      URL.revokeObjectURL(blobUrl)
      message.success('下载已开始')
    })
    .catch(() => {
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      link.target = '_blank'
      link.click()
    })
}

// ===== 评论 =====
const comments = ref<API.CommentVO[]>([])
const commentTotal = ref(0)
const commentCurrent = ref(1)
const commentPageSize = ref(10)
const commentLoading = ref(false)
const commentText = ref('')
const commentSubmitting = ref(false)

// 回复状态
const replyTargetId = ref<number | null>(null)  // 顶级评论 id
const replyTargetUser = ref<API.UserVO | null>(null)
const replyText = ref('')
const replySubmitting = ref(false)

// 展开全部回复
const expandedReplies = ref<Record<number, { list: API.CommentVO[], loading: boolean, total: number, current: number }>>({})

function canDelete(comment: API.CommentVO) {
  if (!userStore.isLoggedIn) return false
  // 评论作者 / 管理员 / 图片所有者 均可删除
  return (
    String(comment.userInfo?.id) === String(userStore.loginUser?.id) ||
    userStore.loginUser?.userRole === 'admin' ||
    String(picture.value?.userId) === String(userStore.loginUser?.id)
  )
}

async function fetchComments(page = 1) {
  if (!picture.value?.id) return
  commentLoading.value = true
  try {
    const res: any = await listCommentsUsingGet({
      pictureId: picture.value.id,
      current: page,
      pageSize: commentPageSize.value,
    })
    if (res?.data) {
      if (page === 1) {
        comments.value = res.data.records || []
      } else {
        comments.value = [...comments.value, ...(res.data.records || [])]
      }
      commentTotal.value = res.data.total || 0
      commentCurrent.value = page
    }
  } catch {}
  finally { commentLoading.value = false }
}

async function submitComment() {
  if (!userStore.isLoggedIn) {
    message.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  const content = commentText.value.trim()
  if (!content) return
  commentSubmitting.value = true
  try {
    const res: any = await addCommentUsingPost({
      pictureId: picture.value?.id,
      content,
    })
    if (res?.data) {
      // 后端接口已返回 userInfo 字段；若因网络/版本差异缺失，则回退到当前登录用户
      if (!res.data.userInfo && userStore.loginUser) {
        res.data.userInfo = { ...userStore.loginUser }
      }
      comments.value.unshift(res.data)
      commentTotal.value++
      commentText.value = ''
      message.success('评论已发布')
    }
  } catch {}
  finally { commentSubmitting.value = false }
}

function startReply(comment: API.CommentVO) {
  if (!userStore.isLoggedIn) {
    message.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (replyTargetId.value === comment.id) {
    replyTargetId.value = null
    replyText.value = ''
  } else {
    replyTargetId.value = comment.id ?? null
    replyTargetUser.value = comment.userInfo ?? null
    replyText.value = ''
  }
}

function startReplyToReply(topComment: API.CommentVO, reply: API.CommentVO) {
  if (!userStore.isLoggedIn) {
    message.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  replyTargetId.value = topComment.id ?? null
  replyTargetUser.value = reply.userInfo ?? null
  replyText.value = `@${reply.userInfo?.userName || '用户'} `
}

async function submitReply(parentComment: API.CommentVO) {
  const content = replyText.value.trim()
  if (!content) return
  replySubmitting.value = true
  try {
    const res: any = await addCommentUsingPost({
      pictureId: picture.value?.id,
      content,
      parentId: parentComment.id,
      replyUserId: replyTargetUser.value?.id,
    })
    if (res?.data) {
      // 后端接口已返回 userInfo/replyUserInfo 字段；若缺失则回退到本地状态
      if (!res.data.userInfo && userStore.loginUser) {
        res.data.userInfo = { ...userStore.loginUser }
      }
      if (!res.data.replyUserInfo && replyTargetUser.value) {
        res.data.replyUserInfo = { ...replyTargetUser.value }
      }
      // 更新顶级评论的 replies 列表
      const idx = comments.value.findIndex(c => c.id === parentComment.id)
      if (idx !== -1) {
        const c = comments.value[idx]
        if (!c.replies) c.replies = []
        c.replies.push(res.data)
        c.replyCount = (c.replyCount || 0) + 1
        // 同步 expandedReplies
        if (expandedReplies.value[parentComment.id!]) {
          expandedReplies.value[parentComment.id!].list.push(res.data)
          expandedReplies.value[parentComment.id!].total++
        }
      }
      replyTargetId.value = null
      replyText.value = ''
      message.success('回复已发布')
    }
  } catch {}
  finally { replySubmitting.value = false }
}

async function handleDeleteComment(comment: API.CommentVO, parentId?: number) {
  Modal.confirm({
    title: '确认删除',
    content: '确定删除这条评论吗？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteCommentUsingPost({ id: comment.id })
        if (parentId) {
          // 删除回复
          const parent = comments.value.find(c => c.id === parentId)
          if (parent?.replies) {
            parent.replies = parent.replies.filter(r => r.id !== comment.id)
            parent.replyCount = Math.max(0, (parent.replyCount || 1) - 1)
          }
          if (expandedReplies.value[parentId]) {
            expandedReplies.value[parentId].list = expandedReplies.value[parentId].list.filter(r => r.id !== comment.id)
            expandedReplies.value[parentId].total = Math.max(0, expandedReplies.value[parentId].total - 1)
          }
        } else {
          comments.value = comments.value.filter(c => c.id !== comment.id)
          commentTotal.value = Math.max(0, commentTotal.value - 1)
        }
        message.success('已删除')
      } catch {}
    },
  })
}

async function expandReplies(comment: API.CommentVO) {
  const cid = comment.id!
  if (expandedReplies.value[cid]) {
    delete expandedReplies.value[cid]
    return
  }
  expandedReplies.value[cid] = { list: [], loading: true, total: 0, current: 1 }
  try {
    const res: any = await listRepliesUsingGet({ commentId: cid, current: 1, pageSize: 20 })
    if (res?.data) {
      expandedReplies.value[cid].list = res.data.records || []
      expandedReplies.value[cid].total = res.data.total || 0
    }
  } catch {}
  finally { expandedReplies.value[cid].loading = false }
}

async function loadMoreReplies(cid: number) {
  const state = expandedReplies.value[cid]
  if (!state) return
  state.loading = true
  const nextPage = state.current + 1
  try {
    const res: any = await listRepliesUsingGet({ commentId: cid, current: nextPage, pageSize: 20 })
    if (res?.data) {
      state.list = [...state.list, ...(res.data.records || [])]
      state.total = res.data.total || 0
      state.current = nextPage
    }
  } catch {}
  finally { state.loading = false }
}

onMounted(fetchDetail)
</script>

<template>
  <div class="detail-page">
    <!-- 顶部导航条 -->
    <div class="detail-topbar">
      <a-button type="text" class="back-btn" @click="handleGoBack">
        <LeftOutlined /> 返回
      </a-button>
      <span class="topbar-title">{{ picture?.name || '图片详情' }}</span>
      <div class="topbar-actions">
        <a-tooltip title="复制分享链接">
          <a-button type="text" @click="copyShareLink"><LinkOutlined /></a-button>
        </a-tooltip>
        <a-tooltip title="下载原图">
          <a-button type="text" @click="downloadPicture"><DownloadOutlined /></a-button>
        </a-tooltip>
        <a-tooltip v-if="isOwner" title="删除图片">
          <a-button type="text" danger @click="handleDelete"><DeleteOutlined /></a-button>
        </a-tooltip>
      </div>
    </div>

    <!-- 主体 -->
    <div v-if="loading" class="loading-state">
      <a-spin size="large" tip="加载中..." />
    </div>

    <div v-else-if="picture" class="detail-body">
      <!-- 左：图片预览 + 基础信息 -->
      <div class="image-panel">
        <div class="image-stage">
          <img
            :src="picture.url"
            :alt="picture.name"
            class="main-img"
          />
        </div>
        <!-- 图片下方：上传者 + 技术参数 -->
        <div class="image-meta">
          <div v-if="picture.user" class="uploader-section" @click="goToUserProfile(picture.user.id)">
            <a-avatar
              :src="picture.user.userAvatar"
              :size="40"
              style="background: var(--color-surface-sand)"
            >
              <template v-if="!picture.user.userAvatar" #icon><UserOutlined /></template>
            </a-avatar>
            <div class="uploader-info">
              <div class="uploader-name">{{ picture.user.userName || '匿名用户' }}</div>
              <div class="uploader-date">上传于 {{ picture.createTime ? new Date(picture.createTime).toLocaleDateString('zh-CN') : '—' }}</div>
            </div>
          </div>
          <a-divider style="margin: 14px 0" />
          <div class="params-section">
            <div class="params-grid">
              <div class="param-item">
                <div class="param-label">尺寸</div>
                <div class="param-value">{{ picture.picWidth }} × {{ picture.picHeight }}</div>
              </div>
              <div class="param-item">
                <div class="param-label">比例</div>
                <div class="param-value">{{ picture.picScale?.toFixed(2) ?? '—' }}</div>
              </div>
              <div class="param-item">
                <div class="param-label">大小</div>
                <div class="param-value">{{ fileSize || '—' }}</div>
              </div>
              <div class="param-item">
                <div class="param-label">格式</div>
                <div class="param-value">{{ picture.picFormat?.toUpperCase() || '—' }}</div>
              </div>
            </div>
            <div v-if="picture.picColor" class="color-row">
              <span class="param-label">主色调</span>
              <span
                class="color-swatch"
                :style="{ background: normalizeColor(picture.picColor) }"
                :title="picture.picColor"
              ></span>
              <span class="color-code">{{ normalizeColor(picture.picColor) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右：信息面板 -->
      <div class="info-panel">
        <!-- 标题 -->
        <h2 class="pic-name">{{ picture.name || '无标题' }}</h2>
        <p v-if="picture.introduction" class="pic-intro">{{ picture.introduction }}</p>

        <!-- 操作行 -->
        <div class="action-row">
          <a-button
            v-if="!isPrivateSpace"
            class="like-btn"
            :class="{ 'is-liked': liked }"
            :type="liked ? 'primary' : 'default'"
            :loading="likeLoading"
            @click="handleLike"
          >
            <HeartFilled v-if="liked" />
            <HeartOutlined v-else />
            {{ liked ? '已点赞' : '点赞' }}
            <span v-if="likeCount > 0" class="like-count">{{ likeCount }}</span>
          </a-button>
          <a-button @click="copyShareLink"><LinkOutlined /> 分享</a-button>
          <a-button @click="downloadPicture"><DownloadOutlined /> 下载</a-button>
          <a-button v-if="isOwner" danger @click="handleDelete"><DeleteOutlined /> 删除</a-button>
        </div>

        <a-divider />

        <!-- 分类与标签 -->
        <div class="meta-section">
          <div v-if="picture.category" class="meta-row">
            <span class="meta-label"><TagOutlined /> 分类</span>
            <a-tag color="red">{{ picture.category }}</a-tag>
          </div>
          <div v-if="picture.tags && picture.tags.length > 0" class="meta-row">
            <span class="meta-label"><TagOutlined /> 标签</span>
            <div class="tag-wrap">
              <a-tag v-for="tag in picture.tags" :key="tag">{{ tag }}</a-tag>
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 点赞信息 -->
        <div v-if="!isPrivateSpace" class="like-info-section">
          <HeartFilled class="like-info-icon" :class="{ active: liked }" />
          <span class="like-info-text">{{ likeCount }} 人喜欢</span>
        </div>

        <a-divider />

        <!-- 评论区 -->
        <div class="comment-section">
          <div class="comment-section-title">
            <MessageOutlined />
            <span>评论</span>
            <span class="comment-count">{{ commentTotal }}</span>
          </div>

          <!-- 发表评论输入框 -->
          <div class="comment-input-wrap">
            <a-avatar :size="32" :src="userStore.loginUser?.userAvatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <div class="comment-input-inner">
              <textarea
                v-model="commentText"
                class="comment-textarea"
                placeholder="写下你的评论..."
                rows="2"
                maxlength="500"
                @keydown.ctrl.enter="submitComment"
              />
              <div class="comment-input-footer">
                <span class="comment-char-count">{{ commentText.length }}/500</span>
                <button
                  class="comment-submit-btn"
                  :disabled="!commentText.trim() || commentSubmitting"
                  @click="submitComment"
                >
                  <SendOutlined />
                  发布
                </button>
              </div>
            </div>
          </div>

          <!-- 评论列表 -->
          <a-spin :spinning="commentLoading">
            <div v-if="comments.length === 0 && !commentLoading" class="comment-empty">
              暂无评论，来说点什么吧
            </div>

            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <!-- 顶级评论 -->
              <div class="comment-main">
                <a-avatar
                  :size="34"
                  :src="comment.userInfo?.userAvatar"
                  class="comment-avatar"
                  @click="goToUserProfile(comment.userInfo?.id)"
                >
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="comment-username" @click="goToUserProfile(comment.userInfo?.id)">
                      {{ comment.userInfo?.userName || '匿名用户' }}
                    </span>
                    <span class="comment-time">{{ comment.createTime ? new Date(comment.createTime).toLocaleString('zh-CN', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : '' }}</span>
                    <button
                      v-if="canDelete(comment)"
                      class="comment-del-btn"
                      title="删除评论"
                      @click="handleDeleteComment(comment)"
                    >
                      <DeleteOutlined />
                    </button>
                  </div>
                  <p class="comment-content">{{ comment.content }}</p>
                  <div class="comment-actions">
                    <button class="comment-reply-link" @click="startReply(comment)">
                      {{ replyTargetId === comment.id ? '取消回复' : '回复' }}
                    </button>
                    <button
                      v-if="(comment.replyCount || 0) > (comment.replies?.length || 0) || (comment.replies && comment.replies.length > 0)"
                      class="comment-expand-link"
                      @click="expandReplies(comment)"
                    >
                      <DownOutlined :class="{ rotated: expandedReplies[comment.id!] }" />
                      {{ expandedReplies[comment.id!] ? '收起回复' : `查看 ${comment.replyCount || comment.replies?.length || ''} 条回复` }}
                    </button>
                  </div>

                  <!-- 回复输入框 -->
                  <div v-if="replyTargetId === comment.id" class="reply-input-wrap">
                    <textarea
                      v-model="replyText"
                      class="comment-textarea reply-textarea"
                      :placeholder="`回复 @${replyTargetUser?.userName || ''}...`"
                      rows="2"
                      maxlength="300"
                      @keydown.ctrl.enter="submitReply(comment)"
                    />
                    <div class="comment-input-footer">
                      <span class="comment-char-count">{{ replyText.length }}/300</span>
                      <button
                        class="comment-submit-btn sm"
                        :disabled="!replyText.trim() || replySubmitting"
                        @click="submitReply(comment)"
                      >
                        <SendOutlined /> 回复
                      </button>
                    </div>
                  </div>

                  <!-- 前3条回复预览 -->
                  <div v-if="comment.replies && comment.replies.length > 0 && !expandedReplies[comment.id!]" class="reply-list">
                    <div v-for="reply in comment.replies.slice(0, 3)" :key="reply.id" class="reply-item">
                      <a-avatar :size="24" :src="reply.userInfo?.userAvatar" @click="goToUserProfile(reply.userInfo?.id)">
                        <template #icon><UserOutlined /></template>
                      </a-avatar>
                      <div class="reply-body">
                        <span class="comment-username sm" @click="goToUserProfile(reply.userInfo?.id)">{{ reply.userInfo?.userName || '匿名' }}</span>
                        <span v-if="reply.replyUserInfo" class="reply-to"> 回复 <span class="comment-username sm">@{{ reply.replyUserInfo.userName }}</span></span>
                        <span class="reply-content">{{ reply.content }}</span>
                        <div class="reply-actions">
                          <span class="comment-time">{{ reply.createTime ? new Date(reply.createTime).toLocaleString('zh-CN', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : '' }}</span>
                          <button class="comment-reply-link sm" @click="startReplyToReply(comment, reply)">回复</button>
                          <button v-if="canDelete(reply)" class="comment-del-btn sm" @click="handleDeleteComment(reply, comment.id)"><DeleteOutlined /></button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 展开全部回复 -->
                  <div v-if="expandedReplies[comment.id!]" class="reply-list">
                    <a-spin :spinning="expandedReplies[comment.id!].loading">
                      <div v-for="reply in expandedReplies[comment.id!].list" :key="reply.id" class="reply-item">
                        <a-avatar :size="24" :src="reply.userInfo?.userAvatar" @click="goToUserProfile(reply.userInfo?.id)">
                          <template #icon><UserOutlined /></template>
                        </a-avatar>
                        <div class="reply-body">
                          <span class="comment-username sm" @click="goToUserProfile(reply.userInfo?.id)">{{ reply.userInfo?.userName || '匿名' }}</span>
                          <span v-if="reply.replyUserInfo" class="reply-to"> 回复 <span class="comment-username sm">@{{ reply.replyUserInfo.userName }}</span></span>
                          <span class="reply-content">{{ reply.content }}</span>
                          <div class="reply-actions">
                            <span class="comment-time">{{ reply.createTime ? new Date(reply.createTime).toLocaleString('zh-CN', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : '' }}</span>
                            <button class="comment-reply-link sm" @click="startReplyToReply(comment, reply)">回复</button>
                            <button v-if="canDelete(reply)" class="comment-del-btn sm" @click="handleDeleteComment(reply, comment.id)"><DeleteOutlined /></button>
                          </div>
                        </div>
                      </div>
                      <button
                        v-if="expandedReplies[comment.id!].list.length < expandedReplies[comment.id!].total"
                        class="load-more-replies"
                        :disabled="expandedReplies[comment.id!].loading"
                        @click="loadMoreReplies(comment.id!)"
                      >加载更多回复</button>
                    </a-spin>
                  </div>
                </div>
              </div>
            </div>

            <!-- 加载更多评论 -->
            <div v-if="comments.length < commentTotal" class="load-more-wrap">
              <button class="load-more-btn" :disabled="commentLoading" @click="fetchComments(commentCurrent + 1)">
                加载更多评论
              </button>
            </div>
          </a-spin>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: var(--color-bg-warm);
  display: flex;
  flex-direction: column;
}

/* ===== 顶部导航条 ===== */
.detail-topbar {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-lg);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-surface-sand);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.topbar-title {
  flex: 1;
  font-size: 15px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* ===== 加载 ===== */
.loading-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-3xl);
}

/* ===== 主体布局 ===== */
.detail-body {
  flex: 1;
  display: flex;
  gap: var(--space-lg);
  padding: var(--space-lg);
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  align-items: flex-start;
}

/* ===== 图片区 ===== */
.image-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.image-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-md);
  background: rgba(33, 25, 34, 0.04);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
  overflow: hidden;
  max-height: calc(100vh - 160px);
}

.main-img {
  max-width: 100%;
  max-height: calc(100vh - 200px);
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: var(--radius-sm);
  display: block;
}

/* ===== 图片下方信息区 ===== */
.image-meta {
  background: var(--color-bg);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
  padding: var(--space-lg) var(--space-xl);
}

.image-meta .params-grid {
  grid-template-columns: repeat(4, 1fr);
}

/* ===== 信息面板 ===== */
.info-panel {
  width: 380px;
  flex-shrink: 0;
  background: var(--color-bg);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
  padding: var(--space-xl);
  overflow-y: auto;
  max-height: calc(100vh - 120px);
  position: sticky;
  top: 70px;
}

.pic-name {
  font-size: 1.3rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
  font-family: var(--font-display);
  letter-spacing: 0.05em;
}

.pic-intro {
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.8;
  margin-bottom: var(--space-md);
}

/* ===== 操作按钮 ===== */
.action-row {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.like-btn {
  transition: all var(--transition-fast);
}

/* ===== 分类 & 标签 ===== */
.meta-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.meta-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
}

.meta-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  min-width: 60px;
  padding-top: 2px;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* ===== 参数 ===== */
.params-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.params-title {
  font-size: 13px;
  font-weight: var(--weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.params-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}

.param-item {
  background: var(--color-bg-warm);
  border-radius: var(--radius-base);
  padding: var(--space-sm) var(--space-md);
}

.param-label {
  font-size: 11px;
  color: var(--color-text-disabled);
  margin-bottom: 2px;
}

.param-value {
  font-size: 13px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
}

.color-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-xs);
}

.color-swatch {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid var(--color-surface-sand);
  flex-shrink: 0;
}

.color-code {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-family: monospace;
}

/* ===== 上传者 ===== */
.uploader-section {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  cursor: pointer;
  padding: var(--space-sm);
  margin: calc(-1 * var(--space-sm));
  border-radius: var(--radius-base);
  transition: background var(--transition-fast);
}

.uploader-section:hover {
  background: var(--color-bg-warm);
}

.uploader-section:hover .uploader-name {
  color: var(--color-primary);
}

.uploader-name {
  font-size: 14px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
}

.uploader-date {
  font-size: 12px;
  color: var(--color-text-disabled);
}

/* ===== 评论占位 ===== */
.like-info-section {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
}

.like-info-icon {
  font-size: 16px;
  color: var(--color-text-disabled);
}

.like-info-icon.active {
  color: var(--color-primary);
}

.like-info-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.like-count {
  margin-left: 4px;
  font-size: 12px;
}

.like-btn.is-liked {
  border-color: var(--color-primary);
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .detail-body {
    flex-direction: column;
    padding: var(--space-md);
  }
  .info-panel {
    width: 100%;
    max-height: none;
    position: static;
  }
  .image-meta .params-grid {
    grid-template-columns: 1fr 1fr;
  }
}

/* ===== 评论区 ===== */
.comment-section {
  padding-bottom: var(--space-md);
}

.comment-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-md);
}

.comment-count {
  font-size: 13px;
  font-weight: var(--weight-regular);
  color: var(--color-text-disabled);
}

/* 发表评论 */
.comment-input-wrap {
  display: flex;
  gap: 10px;
  margin-bottom: var(--space-md);
  align-items: flex-start;
}

.comment-input-inner {
  flex: 1;
  min-width: 0;
}

.comment-textarea {
  width: 100%;
  border: 1px solid var(--color-surface-sand);
  border-radius: var(--radius-base);
  padding: 8px 10px;
  font-size: 13px;
  color: var(--color-text-primary);
  background: var(--color-bg-warm);
  resize: none;
  outline: none;
  font-family: var(--font-body);
  line-height: 1.6;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.comment-textarea:focus {
  border-color: var(--color-primary);
  background: var(--color-bg);
  box-shadow: 0 0 0 2px rgba(230, 0, 35, 0.08);
}

.reply-textarea { font-size: 12px; }

.comment-input-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 6px;
}

.comment-char-count {
  font-size: 11px;
  color: var(--color-text-disabled);
}

.comment-submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 14px;
  border-radius: var(--radius-button);
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all 0.2s;
}

.comment-submit-btn:hover:not(:disabled) { background: var(--color-primary-hover); }
.comment-submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.comment-submit-btn.sm { padding: 4px 12px; font-size: 12px; }

/* 评论列表 */
.comment-empty {
  text-align: center;
  color: var(--color-text-disabled);
  font-size: 13px;
  padding: var(--space-lg) 0;
}

.comment-item {
  margin-bottom: 20px;
}

.comment-main {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.comment-avatar {
  flex-shrink: 0;
  cursor: pointer;
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
  flex-wrap: wrap;
}

.comment-username {
  font-size: 13px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  cursor: pointer;
  transition: color 0.15s;
}

.comment-username:hover { color: var(--color-primary); }
.comment-username.sm { font-size: 12px; }

.comment-time {
  font-size: 11px;
  color: var(--color-text-disabled);
  margin-left: auto;
}

.comment-del-btn {
  background: none;
  border: none;
  color: var(--color-text-disabled);
  cursor: pointer;
  padding: 2px 4px;
  font-size: 12px;
  border-radius: 4px;
  line-height: 1;
  transition: color 0.15s, background 0.15s;
}

.comment-del-btn:hover { color: var(--color-error); background: rgba(158, 10, 10, 0.06); }
.comment-del-btn.sm { font-size: 11px; padding: 1px 3px; }

.comment-content {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.65;
  margin: 0 0 6px;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 8px;
}

.comment-reply-link {
  background: none;
  border: none;
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 0;
  transition: color 0.15s;
}

.comment-reply-link:hover { color: var(--color-primary); }
.comment-reply-link.sm { font-size: 11px; }

.comment-expand-link {
  background: none;
  border: none;
  color: var(--color-link);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.15s;
}

.comment-expand-link:hover { color: var(--color-primary); }
.comment-expand-link .anticon { transition: transform 0.2s; }
.comment-expand-link .anticon.rotated { transform: rotate(180deg); }

/* 回复输入 */
.reply-input-wrap {
  margin: 8px 0 10px;
  padding: 10px;
  background: var(--color-bg-warm);
  border-radius: var(--radius-base);
  border: 1px solid var(--color-surface-sand);
}

/* 回复列表 */
.reply-list {
  margin-top: 8px;
  padding: 10px 12px;
  background: var(--color-bg-warm);
  border-radius: var(--radius-base);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.reply-body {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-primary);
  word-break: break-word;
}

.reply-to { color: var(--color-text-secondary); }
.reply-content { margin-left: 2px; }

.reply-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.load-more-replies {
  background: none;
  border: none;
  color: var(--color-link);
  font-size: 12px;
  cursor: pointer;
  padding: 4px 0;
  width: 100%;
  text-align: center;
  border-top: 1px solid var(--color-surface-sand);
  margin-top: 6px;
  padding-top: 8px;
  transition: color 0.15s;
}

.load-more-replies:hover { color: var(--color-primary); }
.load-more-replies:disabled { opacity: 0.5; cursor: not-allowed; }

/* 加载更多评论 */
.load-more-wrap {
  text-align: center;
  margin-top: var(--space-md);
}

.load-more-btn {
  padding: 7px 24px;
  border-radius: var(--radius-button);
  border: 1px solid var(--color-surface-sand);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.load-more-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.load-more-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
