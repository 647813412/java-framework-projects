<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  UserOutlined,
  CalendarOutlined,
  LeftOutlined,
  HeartFilled,
  GlobalOutlined,
} from '@ant-design/icons-vue'
import { getUserVoByIdUsingGet } from '@/api/userController'
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { getLikeCountUsingGet } from '@/api/pictureLikeController'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userInfo = ref<API.UserVO | null>(null)
const loading = ref(true)

// ===== 公共图库 =====
const pictures = ref<API.PictureVO[]>([])
const picLoading = ref(false)
const picCurrent = ref(1)
const picPageSize = ref(12)
const picTotal = ref(0)
const likeCountMap = ref<Record<string, number>>({})

async function fetchUser() {
  const id = route.params.id as string
  if (!id) {
    router.push('/')
    return
  }
  loading.value = true
  try {
    const res: any = await getUserVoByIdUsingGet({ id })
    if (res?.data) {
      userInfo.value = res.data
    }
  } catch {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

async function fetchPictures() {
  const idStr = route.params.id as string
  if (!idStr) return
  picLoading.value = true
  try {
    const res: any = await listPictureVoByPageUsingPost({
      current: picCurrent.value,
      pageSize: picPageSize.value,
      nullSpaceId: true,
      userId: idStr as any,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res?.data) {
      pictures.value = res.data.records || []
      picTotal.value = res.data.total || 0
      if (pictures.value.length) {
        fetchLikeCounts(pictures.value)
      }
    }
  } catch {
    // 静默
  } finally {
    picLoading.value = false
  }
}

function handlePageChange(page: number, size: number) {
  picCurrent.value = page
  picPageSize.value = size
  fetchPictures()
}

function handlePictureClick(pic: API.PictureVO) {
  router.push(`/picture/${String(pic.id)}`)
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function getPicLikeCount(pic: API.PictureVO) {
  return likeCountMap.value[String(pic.id)] ?? 0
}

async function fetchLikeCounts(pics: API.PictureVO[]) {
  const results = await Promise.allSettled(
    pics.map((pic) =>
      getLikeCountUsingGet({ pictureId: pic.id as number }).then((res: any) => ({
        id: String(pic.id),
        count: res?.data ?? 0,
      }))
    )
  )
  for (const r of results) {
    if (r.status === 'fulfilled') {
      likeCountMap.value[r.value.id] = r.value.count
    }
  }
}

function showTotal(t: number) {
  return `共 ${t} 张`
}

onMounted(async () => {
  // 如果是当前登录用户，跳转个人中心
  const id = route.params.id as string
  if (userStore.isLoggedIn && String(userStore.loginUser?.id) === id) {
    router.replace('/profile')
    return
  }
  await fetchUser()
  fetchPictures()
})
</script>

<template>
  <div class="user-profile-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <a-spin size="large" tip="加载中..." />
    </div>

    <template v-else-if="userInfo">
      <!-- 封面横幅 -->
      <div class="profile-banner">
        <div class="banner-ink-bg"></div>
        <div class="banner-overlay"></div>
        <div class="banner-content">
          <button class="back-btn" @click="router.back()" title="返回">
            <LeftOutlined />
          </button>
          <div class="avatar-wrap">
            <a-avatar :size="96" :src="userInfo.userAvatar" class="profile-avatar">
              <template v-if="!userInfo.userAvatar" #icon><UserOutlined style="font-size: 44px" /></template>
            </a-avatar>
          </div>
          <div class="banner-info">
            <h1 class="profile-name">{{ userInfo.userName || '未命名用户' }}</h1>
            <div class="profile-meta">
              <span class="meta-chip"><UserOutlined /> {{ userInfo.userAccount || '—' }}</span>
              <span class="meta-chip"><CalendarOutlined /> {{ formatDate(userInfo.createTime) }}</span>
            </div>
            <p v-if="userInfo.userProfile" class="profile-bio">{{ userInfo.userProfile }}</p>
            <p v-else class="profile-bio muted">这个人很懒，什么都没有写~</p>
          </div>
        </div>
      </div>

      <!-- 作品标题 -->
      <div class="section-header">
        <GlobalOutlined /> 作品
        <span v-if="picTotal" class="section-count">{{ picTotal }}</span>
      </div>

      <!-- 公共图库 -->
      <div class="gallery-container">
        <a-spin :spinning="picLoading">
          <div v-if="pictures.length > 0" class="gallery-grid">
            <div
              v-for="pic in pictures"
              :key="pic.id"
              class="pic-card"
              @click="handlePictureClick(pic)"
            >
              <div class="pic-img-wrap">
                <img
                  :src="pic.url"
                  :alt="pic.name"
                  class="pic-img"
                  loading="lazy"
                />
              </div>
              <div class="pic-info">
                <div class="pic-name">{{ pic.name || '无标题' }}</div>
                <div class="pic-bottom">
                  <span v-if="pic.category" class="pic-category">{{ pic.category }}</span>
                  <span class="pic-like"><HeartFilled class="like-icon" /> {{ getPicLikeCount(pic) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!picLoading" class="empty-state">
            <a-empty description="该用户暂未上传公共图片" />
          </div>
        </a-spin>
        <div v-if="picTotal > 0" class="gallery-pagination">
          <a-pagination
            v-model:current="picCurrent"
            v-model:page-size="picPageSize"
            :total="picTotal"
            :page-size-options="['12', '20', '40']"
            show-size-changer
            show-quick-jumper
            :show-total="showTotal"
            @change="handlePageChange"
            @show-size-change="handlePageChange"
          />
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <a-empty description="用户不存在" />
    </div>
  </div>
</template>

<style scoped>
.user-profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: var(--space-3xl);
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-3xl);
  min-height: 300px;
}

/* ===== 封面横幅 ===== */
.profile-banner {
  position: relative;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  overflow: hidden;
  margin-bottom: var(--space-lg);
}

.banner-ink-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 75% 20%, rgba(230, 0, 35, 0.08) 0%, transparent 55%),
    radial-gradient(ellipse at 25% 80%, rgba(16, 60, 37, 0.06) 0%, transparent 55%),
    radial-gradient(ellipse at 50% 50%, rgba(33, 25, 34, 0.04) 0%, transparent 70%),
    linear-gradient(135deg, #f6f6f3 0%, #e5e5e0 40%, #e0e0d9 70%, #f6f6f3 100%);
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 3px,
      rgba(33, 25, 34, 0.006) 3px,
      rgba(33, 25, 34, 0.006) 6px
    );
}

.banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--space-xl);
  padding: var(--space-2xl) var(--space-xl);
}

.back-btn {
  position: absolute;
  top: var(--space-md);
  left: var(--space-md);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--color-surface-sand);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.back-btn:hover {
  background: var(--color-bg);
  color: var(--color-text-primary);
}

.avatar-wrap {
  flex-shrink: 0;
}

.profile-avatar {
  border: 4px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 20px rgba(33, 25, 34, 0.12);
  background: var(--color-surface-sand);
}

.banner-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-family: var(--font-display);
  font-size: 1.75rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-sm);
  letter-spacing: 0.06em;
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(4px);
  padding: 3px 10px;
  border-radius: 20px;
  border: 1px solid rgba(229, 229, 224, 0.5);
}

.profile-bio {
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-primary);
  margin: 0;
}

.profile-bio.muted {
  color: var(--color-text-disabled);
  font-style: italic;
}

/* ===== 区块标题 ===== */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  padding: 0 var(--space-sm);
  margin-bottom: var(--space-md);
}

.section-count {
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-surface-sand);
  padding: 1px 8px;
  border-radius: 10px;
}

/* ===== 图库 ===== */
.gallery-container {
  padding: 0 var(--space-sm);
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-md);
}

.pic-card {
  background: var(--color-bg);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.pic-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(33, 25, 34, 0.08);
}

.pic-img-wrap {
  position: relative;
  padding-top: 75%;
  overflow: hidden;
}

.pic-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pic-info {
  padding: var(--space-sm) var(--space-md);
}

.pic-name {
  font-size: 13px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.pic-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pic-category {
  font-size: 11px;
  color: var(--color-text-secondary);
  background: var(--color-bg-warm);
  padding: 1px 8px;
  border-radius: 8px;
}

.pic-like {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--color-text-disabled);
}

.like-icon {
  color: var(--color-primary);
  font-size: 12px;
}

.gallery-pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--space-xl);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-3xl);
}

@media (max-width: 640px) {
  .banner-content {
    flex-direction: column;
    text-align: center;
  }
  .profile-meta {
    justify-content: center;
  }
  .gallery-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}
</style>
