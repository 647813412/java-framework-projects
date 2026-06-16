<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { SearchOutlined, HeartFilled, TeamOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import {
  listPictureVoByPageUsingPost,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController'
import { getLikeCountUsingGet } from '@/api/pictureLikeController'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { applyJoinSpaceUsingPost, listMyTeamSpaceUsingPost } from '@/api/spaceUserController'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ===== 搜索 & 筛选 =====
const searchKeyword = ref('')
const activeCategory = ref('')
const categoryList = ref<string[]>([])

// ===== 图片列表 & 分页 =====
const pictures = ref<API.PictureVO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const likeCountMap = ref<Record<string, number>>({})

async function fetchLikeCounts(pics: API.PictureVO[]) {
  const results = await Promise.allSettled(
    pics.map((pic) =>
      getLikeCountUsingGet({ pictureId: pic.id as number }).then((res: any) => ({
        id: String(pic.id),
        count: res?.data ?? 0,
      }))
    )
  )
  const map: Record<string, number> = {}
  for (const r of results) {
    if (r.status === 'fulfilled') {
      map[r.value.id] = r.value.count
    }
  }
  likeCountMap.value = map
}

async function fetchCategories() {
  try {
    const res: any = await listPictureTagCategoryUsingGet()
    if (res?.data?.categoryList) {
      categoryList.value = res.data.categoryList
    }
  } catch {
    // 静默失败
  }
}

async function fetchPictures() {
  loading.value = true
  try {
    const params: API.PictureQueryRequest = {
      current: currentPage.value,
      pageSize: pageSize.value,
      nullSpaceId: true,
      sortField: 'createTime',
      sortOrder: 'descend',
    }
    if (searchKeyword.value.trim()) {
      params.searchText = searchKeyword.value.trim()
    }
    if (activeCategory.value) {
      params.category = activeCategory.value
    }
    const res: any = await listPictureVoByPageUsingPost(params)
    if (res?.data) {
      pictures.value = res.data.records || []
      total.value = res.data.total || 0
      if (pictures.value.length) {
        fetchLikeCounts(pictures.value)
      }
    }
  } catch {
    // 静默失败
  } finally {
    loading.value = false
  }
  // 翻页后回到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleSearch() {
  currentPage.value = 1
  fetchPictures()
}

function selectCategory(cat: string) {
  activeCategory.value = activeCategory.value === cat ? '' : cat
  currentPage.value = 1
  fetchPictures()
}

function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  fetchPictures()
}

// ===== 点击图片 =====
function handlePictureClick(pic: API.PictureVO) {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/picture/${pic.id}` } })
    return
  }
  // 使用字符串拼接，避免超大ID（雪花ID）在JS中精度丢失
  router.push(`/picture/${String(pic.id)}`)
}

// ===== 团队空间 =====
const teamSpaces = ref<API.SpaceVO[]>([])
const teamSpacesLoading = ref(false)
const applyingSpaceId = ref<string | null>(null)
const mySpaceIds = ref<Set<string>>(new Set())

async function fetchMySpaces() {
  if (!userStore.isLoggedIn) return
  try {
    const res: any = await listMyTeamSpaceUsingPost()
    if (res?.data) {
      const ids = new Set<string>()
      for (const item of res.data) {
        ids.add(String(item.spaceId || item.space?.id))
      }
      mySpaceIds.value = ids
    }
  } catch {
    // 静默
  }
}

function isMemberOrOwner(space: API.SpaceVO): boolean {
  if (String(space.userId) === String(userStore.loginUser?.id)) return true
  return mySpaceIds.value.has(String(space.id))
}

async function fetchTeamSpaces() {
  teamSpacesLoading.value = true
  try {
    const res: any = await listSpaceVoByPageUsingPost({
      spaceType: 1,
      current: 1,
      pageSize: 12,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res?.data?.records) {
      teamSpaces.value = res.data.records
    }
  } catch {
    // 静默
  } finally {
    teamSpacesLoading.value = false
  }
}

function handleTeamSpaceClick(space: API.SpaceVO) {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/space/${String(space.id)}` } })
    return
  }
  router.push(`/space/${String(space.id)}`)
}

async function handleApplyJoin(space: API.SpaceVO, e: Event) {
  e.stopPropagation()
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: '/' } })
    return
  }
  applyingSpaceId.value = String(space.id)
  try {
    await applyJoinSpaceUsingPost({ spaceId: space.id as any })
    message.success('申请已提交，等待管理员审批')
  } catch {
    // 拦截器已处理
  } finally {
    applyingSpaceId.value = null
  }
}

onMounted(() => {
  // 从路由 query 读取搜索关键词（由顶部导航栏传入）
  if (route.query.search) {
    searchKeyword.value = route.query.search as string
  }
  fetchCategories()
  fetchPictures()
  fetchMySpaces()
  fetchTeamSpaces()
})

// 监听路由 query 变化（导航栏搜索触发）
watch(
  () => route.query.search,
  (newSearch) => {
    const keyword = (newSearch as string) || ''
    if (keyword !== searchKeyword.value) {
      searchKeyword.value = keyword
      currentPage.value = 1
      fetchPictures()
    }
  },
)
</script>

<template>
  <div class="home-page">
    <!-- 顶部横幅 -->
    <div class="hero-section">
      <h1 class="hero-title">拾光图库</h1>
      <p class="hero-desc">以光影之名，拾时光之美</p>
    </div>

    <!-- 团队空间区域 -->
    <div v-if="teamSpaces.length > 0" class="team-spaces-section">
      <div class="section-header">
        <h2 class="section-title"><TeamOutlined /> 团队空间</h2>
        <span class="section-desc">加入团队，发现更多精彩内容</span>
      </div>
      <div class="team-spaces-scroll">
        <div
          v-for="space in teamSpaces"
          :key="space.id"
          class="team-card"
          @click="handleTeamSpaceClick(space)"
        >
          <div class="team-card-avatar">
            <a-avatar :size="52" :src="space.avatar">
              <template #icon><TeamOutlined style="font-size: 24px" /></template>
            </a-avatar>
          </div>
          <div class="team-card-info">
            <div class="team-card-name">{{ space.spaceName || '未命名空间' }}</div>
            <div class="team-card-meta">
              <span class="team-meta-item">
                <UserOutlined /> {{ space.user?.userName || '未知' }}
              </span>
            </div>
          </div>
          <button
            v-if="userStore.isLoggedIn && isMemberOrOwner(space)"
            class="team-enter-btn"
            @click.stop="handleTeamSpaceClick(space)"
          >
            进入
          </button>
          <button
            v-else-if="userStore.isLoggedIn"
            class="team-apply-btn"
            :disabled="applyingSpaceId === String(space.id)"
            @click="handleApplyJoin(space, $event)"
          >
            {{ applyingSpaceId === String(space.id) ? '申请中...' : '申请加入' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 标签筛选区 -->
    <div class="tag-bar">
      <span
        class="tag-item"
        :class="{ active: activeCategory === '' }"
        @click="selectCategory('')"
      >全部</span>
      <span
        v-for="cat in categoryList"
        :key="cat"
        class="tag-item"
        :class="{ active: activeCategory === cat }"
        @click="selectCategory(cat)"
      >{{ cat }}</span>
    </div>

    <!-- 加载中骨架 -->
    <div v-if="loading" class="waterfall-grid">
      <div v-for="i in 12" :key="i" class="waterfall-item skeleton-item">
        <div class="waterfall-img skeleton-img" :style="{ paddingBottom: (55 + (i % 3) * 20) + '%' }"></div>
        <div class="waterfall-info">
          <div class="info-title-bar skeleton-bar"></div>
          <div class="info-meta-bar skeleton-bar"></div>
        </div>
      </div>
    </div>

    <!-- 瀑布流图片区 -->
    <div v-else class="waterfall-grid">
      <div
        v-for="pic in pictures"
        :key="pic.id"
        class="waterfall-item"
        @click="handlePictureClick(pic)"
      >
        <div class="waterfall-img-wrap">
          <img
            :src="pic.url"
            :alt="pic.name"
            class="waterfall-img-real"
            loading="lazy"
          />
          <div class="waterfall-overlay">
            <span v-if="!userStore.isLoggedIn" class="overlay-hint">登录后查看详情</span>
            <span v-else class="overlay-hint">查看详情</span>
          </div>
        </div>
        <div class="waterfall-info">
          <div class="info-name">{{ pic.name || '无标题' }}</div>
          <div class="info-meta">
            <span v-if="pic.category" class="info-category">{{ pic.category }}</span>
            <span class="info-like"><HeartFilled class="like-icon" /> {{ likeCountMap[String(pic.id)] ?? 0 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && pictures.length === 0" class="empty-state">
      <a-empty description="暂无图片" />
    </div>

    <!-- 分页器 -->
    <div v-if="total > 0" class="pagination-bar">
      <a-pagination
        v-model:current="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-size-options="['12', '20', '40']"
        show-size-changer
        show-quick-jumper
        :show-total="(t: number) => `共 ${t} 张图片`"
        @change="handlePageChange"
        @show-size-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.home-page {
  padding: 0;
}

/* ===== 横幅 ===== */
.hero-section {
  text-align: center;
  padding: var(--space-3xl) var(--space-lg) var(--space-xl);
  background:
    radial-gradient(ellipse at 30% 70%, rgba(230, 0, 35, 0.03) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 30%, rgba(16, 60, 37, 0.03) 0%, transparent 50%),
    var(--color-bg-warm);
}

.hero-title {
  font-family: var(--font-display);
  font-size: 2.5rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
  letter-spacing: 0.12em;
}

.hero-desc {
  font-family: var(--font-display);
  font-size: 1rem;
  color: var(--color-text-secondary);
  letter-spacing: 0.15em;
  margin-bottom: 0;
}

/* ===== 标签筛选 ===== */
.tag-bar {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  overflow-x: auto;
  border-bottom: 1px solid var(--color-surface-sand);
  background: var(--color-bg);
  flex-wrap: wrap;
}

/* ===== 团队空间区域 ===== */
.team-spaces-section {
  padding: var(--space-lg) var(--space-lg) 0;
  background: var(--color-bg);
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.section-title {
  font-family: var(--font-display);
  font-size: 1.15rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  letter-spacing: 0.04em;
}

.section-desc {
  font-size: 13px;
  color: var(--color-text-disabled);
}

.team-spaces-scroll {
  display: flex;
  gap: var(--space-md);
  overflow-x: auto;
  padding-bottom: var(--space-md);
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.team-spaces-scroll::-webkit-scrollbar {
  height: 4px;
}

.team-spaces-scroll::-webkit-scrollbar-thumb {
  background: var(--color-surface-sand);
  border-radius: 2px;
}

.team-card {
  flex: 0 0 260px;
  scroll-snap-align: start;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-lg) var(--space-md);
  background: var(--color-bg-warm);
  border: 1px solid var(--color-surface-sand);
  border-radius: var(--radius-card);
  cursor: pointer;
  transition:
    transform var(--transition-normal),
    box-shadow var(--transition-normal);
  text-align: center;
}

.team-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(33, 25, 34, 0.1);
}

.team-card-avatar .ant-avatar {
  border: 3px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 2px 8px rgba(33, 25, 34, 0.1);
  background: linear-gradient(135deg, rgba(230, 0, 35, 0.06), rgba(67, 94, 229, 0.06));
}

.team-card-info {
  min-width: 0;
  width: 100%;
}

.team-card-name {
  font-size: 14px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-card-meta {
  display: flex;
  justify-content: center;
  gap: var(--space-sm);
}

.team-meta-item {
  font-size: 12px;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.team-apply-btn {
  padding: 4px 16px;
  border-radius: var(--radius-button);
  border: 1px solid var(--color-primary);
  background: transparent;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.team-apply-btn:hover:not(:disabled) {
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.team-apply-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.team-enter-btn {
  padding: 4px 16px;
  border-radius: var(--radius-button);
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  font-size: 12px;
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.team-enter-btn:hover {
  opacity: 0.85;
}

.tag-item {
  cursor: pointer;
  padding: 4px 16px;
  border-radius: var(--radius-button);
  font-size: 14px;
  border: 1px solid var(--color-surface-sand);
  color: var(--color-text-secondary);
  background: transparent;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  user-select: none;
}

.tag-item:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.tag-item.active {
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-color: var(--color-primary);
}

/* ===== 瀑布流 ===== */
.waterfall-grid {
  column-count: 4;
  column-gap: var(--space-md);
  padding: var(--space-lg);
}

.waterfall-item {
  break-inside: avoid;
  margin-bottom: var(--space-md);
  background: var(--color-bg);
  border-radius: var(--radius-card);
  overflow: hidden;
  border: 1px solid var(--color-surface-sand);
  cursor: pointer;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}

.waterfall-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(33, 25, 34, 0.12);
}

/* 真实图片 */
.waterfall-img-wrap {
  position: relative;
  overflow: hidden;
}

.waterfall-img-real {
  width: 100%;
  height: auto;
  display: block;
  transition: transform var(--transition-normal);
}

.waterfall-item:hover .waterfall-img-real {
  transform: scale(1.03);
}

.waterfall-overlay {
  position: absolute;
  inset: 0;
  background: rgba(33, 25, 34, 0);
  display: flex;
  align-items: flex-end;
  padding: var(--space-sm) var(--space-md);
  transition: background var(--transition-normal);
}

.waterfall-item:hover .waterfall-overlay {
  background: rgba(33, 25, 34, 0.28);
}

.overlay-hint {
  color: #ffffff;
  font-size: 13px;
  font-weight: var(--weight-medium);
  opacity: 0;
  transition: opacity var(--transition-normal);
  background: rgba(0,0,0,0.4);
  padding: 2px 8px;
  border-radius: 4px;
}

.waterfall-item:hover .overlay-hint {
  opacity: 1;
}

/* 骨架屏 */
.skeleton-item .skeleton-img {
  background: linear-gradient(90deg, var(--color-surface-sand) 25%, var(--color-bg-warm) 50%, var(--color-surface-sand) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-bar {
  animation: shimmer 1.5s infinite;
  background: linear-gradient(90deg, var(--color-surface-sand) 25%, var(--color-bg-warm) 50%, var(--color-surface-sand) 75%);
  background-size: 200% 100%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.waterfall-img {
  position: relative;
  width: 100%;
}

.waterfall-info {
  padding: var(--space-sm) var(--space-md) var(--space-md);
}

.info-name {
  font-size: 13px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-meta {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.info-category {
  font-size: 11px;
  color: var(--color-primary);
  background: rgba(230, 0, 35, 0.06);
  padding: 1px 6px;
  border-radius: 4px;
}

.info-like {
  font-size: 12px;
  color: var(--color-text-disabled);
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.like-icon {
  color: var(--color-primary);
  font-size: 11px;
}

.info-title-bar {
  height: 14px;
  width: 70%;
  border-radius: 4px;
  margin-bottom: var(--space-xs);
}

.info-meta-bar {
  height: 10px;
  width: 45%;
  border-radius: 4px;
  opacity: 0.6;
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: var(--space-3xl) 0;
}

/* ===== 分页器 ===== */
.pagination-bar {
  display: flex;
  justify-content: center;
  padding: var(--space-xl) var(--space-lg) var(--space-3xl);
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .waterfall-grid { column-count: 3; }
}
@media (max-width: 768px) {
  .waterfall-grid { column-count: 2; }
  .hero-title { font-size: 1.8rem; }
}
@media (max-width: 480px) {
  .waterfall-grid { column-count: 1; padding: var(--space-sm); }
}
</style>
