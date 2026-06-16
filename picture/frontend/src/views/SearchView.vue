<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  BgColorsOutlined,
  UserOutlined,
  CloudOutlined,
  DeleteOutlined,
  CloseOutlined,
  PictureOutlined,
} from '@ant-design/icons-vue'
import {
  listPictureVoByPageUsingPost,
  listPictureTagCategoryUsingGet,
  searchPictureByColorUsingPost,
} from '@/api/pictureController'
import { searchUserUsingPost } from '@/api/userController'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import {
  getHistoryUsingGet,
  clearHistoryUsingPost,
  deleteHistoryUsingPost,
  getHotKeywordsUsingGet,
} from '@/api/searchController'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ===== Tab =====
type SearchTab = 'picture' | 'user' | 'space'
const activeTab = ref<SearchTab>((route.query.tab as SearchTab) || 'picture')

// ===== 共用搜索关键词 =====
const picKeyword = ref((route.query.keyword as string) || '')

const searchPlaceholder = computed(() => {
  if (activeTab.value === 'user') return '搜索用户名称或账号...'
  if (activeTab.value === 'space') return '搜索空间名称...'
  return '搜索图片...'
})

// ===== 图片搜索 =====
type PicSearchMode = 'keyword' | 'color'
const picSearchMode = ref<PicSearchMode>('keyword')
const picCategory = ref('')
const categoryList = ref<API.PictureVO[]>([])
const picResults = ref<API.PictureVO[]>([])
const picLoading = ref(false)
const picCurrent = ref(1)
const picPageSize = ref(20)
const picTotal = ref(0)

async function fetchCategories() {
  try {
    const res: any = await listPictureTagCategoryUsingGet()
    if (res?.data?.categoryList) categoryList.value = res.data.categoryList
  } catch {}
}

async function searchPicturesBasic() {
  router.replace({
    query: {
      tab: activeTab.value,
      keyword: picKeyword.value.trim() || undefined,
      category: picCategory.value || undefined,
      page: picCurrent.value !== 1 ? String(picCurrent.value) : undefined,
    },
  })
  picLoading.value = true
  try {
    const params: API.PictureQueryRequest = {
      current: picCurrent.value,
      pageSize: picPageSize.value,
      nullSpaceId: true,
      sortField: 'createTime',
      sortOrder: 'descend',
    }
    if (picKeyword.value.trim()) params.searchText = picKeyword.value.trim()
    if (picCategory.value) params.category = picCategory.value
    const res: any = await listPictureVoByPageUsingPost(params)
    if (res?.data) {
      picResults.value = res.data.records || []
      picTotal.value = res.data.total || 0
    }
    if (picKeyword.value.trim()) fetchHistory('picture')
  } catch {}
  finally { picLoading.value = false }
}

function handlePicPageChange(page: number, size: number) {
  picCurrent.value = page
  picPageSize.value = size
  searchPicturesBasic()
}

// ===== 颜色搜索 =====
const presetColors = [
  '#ff4d4f', '#ff7a45', '#ffc53d', '#52c41a', '#1677ff',
  '#722ed1', '#eb2f96', '#ffffff', '#8c8c8c', '#000000',
]
const colorValue = ref('#1677ff')
const colorResults = ref<API.PictureVO[]>([])
const colorLoading = ref(false)

async function searchByColor() {
  colorLoading.value = true
  try {
    const hexColor = colorValue.value.replace('#', '')
    const res: any = await searchPictureByColorUsingPost({ picColor: hexColor })
    colorResults.value = res?.data || []
    if (colorResults.value.length === 0) message.info('未找到相似颜色的图片')
  } catch (e: any) {
    message.error(e?.message || '搜索失败')
  } finally {
    colorLoading.value = false
  }
}

// ===== 用户搜索 =====
const userResults = ref<API.UserVO[]>([])
const userLoading = ref(false)
const userCurrent = ref(1)
const userPageSize = ref(10)
const userTotal = ref(0)

async function searchUsers() {
  userLoading.value = true
  try {
    const params: API.UserQueryRequest = {
      current: userCurrent.value,
      pageSize: userPageSize.value,
      searchText: picKeyword.value.trim() || undefined,
    }
    const res: any = await searchUserUsingPost(params)
    if (res?.data) {
      userResults.value = res.data.records || []
      userTotal.value = res.data.total || 0
    }
    if (picKeyword.value.trim()) fetchHistory('user')
  } catch {}
  finally { userLoading.value = false }
}

function handleUserPageChange(page: number, size: number) {
  userCurrent.value = page
  userPageSize.value = size
  searchUsers()
}

// ===== 空间搜索 =====
const spaceResults = ref<API.SpaceVO[]>([])
const spaceLoading = ref(false)
const spaceCurrent = ref(1)
const spacePageSize = ref(10)
const spaceTotal = ref(0)

async function searchSpaces() {
  spaceLoading.value = true
  try {
    const res: any = await listSpaceVoByPageUsingPost({
      current: spaceCurrent.value,
      pageSize: spacePageSize.value,
      spaceName: picKeyword.value.trim() || undefined,
    })
    if (res?.data) {
      spaceResults.value = res.data.records || []
      spaceTotal.value = res.data.total || 0
    }
    if (picKeyword.value.trim()) fetchHistory('space')
  } catch {}
  finally { spaceLoading.value = false }
}

function handleSpacePageChange(page: number, size: number) {
  spaceCurrent.value = page
  spacePageSize.value = size
  searchSpaces()
}

// ===== 历史 & 热门搜索 =====
const historyList = ref<API.SearchHistory[]>([])
const hotKeywords = ref<string[]>([])
const userHistoryList = ref<API.SearchHistory[]>([])
const spaceHistoryList = ref<API.SearchHistory[]>([])

async function fetchHistory(searchType: string = 'picture') {
  if (!userStore.isLoggedIn) return
  try {
    const res: any = await getHistoryUsingGet({ searchType })
    if (searchType === 'picture') historyList.value = res?.data || []
    else if (searchType === 'user') userHistoryList.value = res?.data || []
    else if (searchType === 'space') spaceHistoryList.value = res?.data || []
  } catch {}
}

async function fetchHotKeywords() {
  try {
    const res: any = await getHotKeywordsUsingGet({ type: 'picture' })
    hotKeywords.value = res?.data || []
  } catch {}
}

async function handleDeleteHistory(id?: number) {
  if (!id) return
  try {
    await deleteHistoryUsingPost({ id })
    if (activeTab.value === 'picture') historyList.value = historyList.value.filter(h => h.id !== id)
    else if (activeTab.value === 'user') userHistoryList.value = userHistoryList.value.filter(h => h.id !== id)
    else if (activeTab.value === 'space') spaceHistoryList.value = spaceHistoryList.value.filter(h => h.id !== id)
  } catch {}
}

async function handleClearHistory() {
  try {
    await clearHistoryUsingPost()
    if (activeTab.value === 'picture') historyList.value = []
    else if (activeTab.value === 'user') userHistoryList.value = []
    else if (activeTab.value === 'space') spaceHistoryList.value = []
    message.success('已清空搜索历史')
  } catch {}
}

function applyKeyword(kw: string) {
  picKeyword.value = kw
  doSearch()
}

// ===== 统一搜索分发 =====
function doSearch() {
  if (activeTab.value === 'picture') {
    picCurrent.value = 1
    searchPicturesBasic()
  } else if (activeTab.value === 'user') {
    userCurrent.value = 1
    searchUsers()
  } else if (activeTab.value === 'space') {
    spaceCurrent.value = 1
    searchSpaces()
  }
}

function clearKeyword() {
  picKeyword.value = ''
  picResults.value = []
  picTotal.value = 0
  userResults.value = []
  spaceResults.value = []
  router.replace({ query: { tab: activeTab.value } })
}

// ===== Tab 切换 =====
function switchTab(tab: SearchTab) {
  activeTab.value = tab
  const newQuery: Record<string, string> = { tab }
  if (tab === 'picture' && picKeyword.value.trim()) {
    newQuery.keyword = picKeyword.value.trim()
  }
  router.replace({ query: newQuery })
  fetchHistory(tab)
}

// ===== 工具 =====
function formatSize(bytes: number) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0; let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(1)} ${units[i]}`
}

const spaceLevelMap: Record<number, string> = { 0: '普通版', 1: '专业版', 2: '旗舰版' }
const spaceTypeMap: Record<number, string> = { 0: '私有', 1: '团队' }

function goToPicture(id: number | string) {
  sessionStorage.setItem('search_scrollY', String(Math.round(window.scrollY)))
  router.push(`/picture/${String(id)}`)
}

function goToUser(id: number | string) {
  if (userStore.isLoggedIn && String(id) === String(userStore.loginUser?.id)) {
    router.push('/profile')
  } else {
    router.push(`/user/${String(id)}`)
  }
}

// ===== 初始化 =====
onMounted(async () => {
  const q = route.query
  if (q.tab) activeTab.value = q.tab as SearchTab
  if (q.keyword) picKeyword.value = q.keyword as string
  if (q.category) picCategory.value = q.category as string
  if (q.page) picCurrent.value = Number(q.page) || 1

  fetchCategories()
  fetchHotKeywords()
  fetchHistory('picture')
  fetchHistory('user')
  fetchHistory('space')

  if (picKeyword.value.trim() || picCategory.value) {
    await searchPicturesBasic()
  }

  const savedY = sessionStorage.getItem('search_scrollY')
  if (savedY) {
    sessionStorage.removeItem('search_scrollY')
    nextTick(() => window.scrollTo({ top: Number(savedY) }))
  }
})

watch(() => route.query, (q) => {
  if (q.tab) activeTab.value = q.tab as SearchTab
  if (q.keyword && activeTab.value === 'picture') {
    picKeyword.value = q.keyword as string
    picCurrent.value = 1
    searchPicturesBasic()
  }
})
</script>

<template>
  <div class="search-page">

    <!-- ===== 顶部搜索区 ===== -->
    <div class="search-top">
      <div class="search-bar-wrap">
        <div class="search-bar">
          <SearchOutlined class="search-bar-icon" />
          <input
            v-model="picKeyword"
            class="search-bar-input"
            :placeholder="searchPlaceholder"
            @keydown.enter="doSearch()"
          />
          <CloseOutlined
            v-if="picKeyword"
            class="search-bar-clear"
            @click="clearKeyword()"
          />
        </div>
        <button class="search-top-btn" @click="doSearch()">搜索</button>
      </div>
    </div>

    <!-- ===== Tab 栏 ===== -->
    <div class="tab-bar">
      <button class="tab-item" :class="{ active: activeTab === 'picture' }" @click="switchTab('picture')">图片</button>
      <button class="tab-item" :class="{ active: activeTab === 'user' }" @click="switchTab('user')">用户</button>
      <button class="tab-item" :class="{ active: activeTab === 'space' }" @click="switchTab('space')">空间</button>
    </div>

    <!-- ===== 图片 Tab ===== -->
    <div v-if="activeTab === 'picture'" class="tab-content">

      <!-- 模式切换 -->
      <div class="mode-switch">
        <button class="mode-btn" :class="{ active: picSearchMode === 'keyword' }" @click="picSearchMode = 'keyword'">
          <SearchOutlined /><span>关键词搜索</span>
        </button>
        <button class="mode-btn" :class="{ active: picSearchMode === 'color' }" @click="picSearchMode = 'color'">
          <BgColorsOutlined /><span>颜色搜索</span>
        </button>
      </div>

      <!-- 关键词搜索 -->
      <div v-if="picSearchMode === 'keyword'">

        <!-- 历史 & 热门（无结果时展示） -->
        <div v-if="!picLoading && picResults.length === 0 && !picCategory" class="discovery">

          <!-- 历史搜索 -->
          <div v-if="userStore.isLoggedIn && historyList.length > 0" class="discovery-block">
            <div class="discovery-header">
              <span class="discovery-title">历史搜索</span>
              <button class="discovery-clear" title="清空历史" @click="handleClearHistory">
                <DeleteOutlined />
              </button>
            </div>
            <div class="history-tags">
              <span
                v-for="h in historyList.slice(0, 12)"
                :key="h.id"
                class="history-tag"
                @click="applyKeyword(h.keyword || '')"
              >
                {{ h.keyword }}
                <CloseOutlined class="history-tag-x" @click.stop="handleDeleteHistory(h.id)" />
              </span>
            </div>
          </div>

          <!-- 热门搜索 -->
          <div v-if="hotKeywords.length > 0" class="discovery-block">
            <div class="discovery-header">
              <span class="discovery-title">热门搜索</span>
            </div>
            <div class="hot-grid">
              <div
                v-for="(kw, idx) in hotKeywords"
                :key="kw"
                class="hot-row"
                @click="applyKeyword(kw)"
              >
                <span class="hot-rank" :class="{ top3: idx < 3 }">{{ idx + 1 }}</span>
                <span class="hot-kw">{{ kw }}</span>
                <span v-if="idx < 3" class="hot-badge">热</span>
              </div>
            </div>
          </div>

        </div>

        <!-- 分类筛选 -->
        <div v-if="categoryList.length > 0" class="filter-bar">
          <span
            class="filter-chip"
            :class="{ active: picCategory === '' }"
            @click="picCategory = ''; picCurrent = 1; searchPicturesBasic()"
          >全部</span>
          <span
            v-for="cat in categoryList"
            :key="cat"
            class="filter-chip"
            :class="{ active: picCategory === cat }"
            @click="picCategory = cat; picCurrent = 1; searchPicturesBasic()"
          >{{ cat }}</span>
        </div>

        <!-- 搜索结果 -->
        <a-spin :spinning="picLoading">
          <div v-if="picResults.length" class="pic-grid">
            <div
              v-for="pic in picResults"
              :key="pic.id"
              class="pic-card"
              @click="goToPicture(pic.id)"
            >
              <div class="pic-img-wrap">
                <img :src="pic.url" :alt="pic.name" class="pic-img" loading="lazy" />
                <div class="pic-overlay"><SearchOutlined /></div>
              </div>
              <div class="pic-info">
                <span class="pic-name">{{ pic.name || '无标题' }}</span>
                <span v-if="pic.category" class="pic-cat">{{ pic.category }}</span>
              </div>
            </div>
          </div>
          <div v-else-if="!picLoading && (picKeyword.trim() || picCategory)" class="empty-state">
            <div class="empty-icon"><PictureOutlined /></div>
            <p>没有找到相关图片</p>
          </div>
        </a-spin>

        <div v-if="picTotal > picPageSize" class="pagination-wrap">
          <a-pagination
            :current="picCurrent"
            :page-size="picPageSize"
            :total="picTotal"
            show-size-changer
            @change="handlePicPageChange"
          />
        </div>
      </div>

      <!-- 颜色搜索 -->
      <div v-else-if="picSearchMode === 'color'">
        <div class="color-search-wrap">
          <div class="color-input-row">
            <input type="color" v-model="colorValue" class="color-picker-input" />
            <span class="color-hex">{{ colorValue }}</span>
            <button class="search-btn color-search-btn" :disabled="colorLoading" @click="searchByColor()">
              <BgColorsOutlined /> 按颜色搜索
            </button>
          </div>
          <div class="preset-colors">
            <button
              v-for="c in presetColors"
              :key="c"
              class="preset-chip"
              :class="{ active: colorValue === c }"
              :style="{ background: c }"
              @click="colorValue = c"
            />
          </div>
        </div>
        <a-spin :spinning="colorLoading">
          <div v-if="colorResults.length" class="pic-grid">
            <div v-for="pic in colorResults" :key="pic.id" class="pic-card" @click="goToPicture(pic.id)">
              <div class="pic-img-wrap">
                <img :src="pic.url" :alt="pic.name" class="pic-img" loading="lazy" />
                <div class="pic-overlay"><SearchOutlined /></div>
              </div>
              <div class="pic-info">
                <span class="pic-name">{{ pic.name || '无标题' }}</span>
                <span v-if="pic.category" class="pic-cat">{{ pic.category }}</span>
              </div>
            </div>
          </div>
          <div v-else-if="!colorLoading" class="empty-state">
            <div class="empty-icon"><BgColorsOutlined /></div>
            <p>选择颜色，搜索相似色调的图片</p>
          </div>
        </a-spin>
      </div>
    </div>

    <!-- ===== 用户 Tab ===== -->
    <div v-if="activeTab === 'user'" class="tab-content">

      <!-- 历史搜索（无结果时展示） -->
      <div v-if="!userLoading && userResults.length === 0" class="discovery">
        <div v-if="userStore.isLoggedIn && userHistoryList.length > 0" class="discovery-block">
          <div class="discovery-header">
            <span class="discovery-title">历史搜索</span>
            <button class="discovery-clear" title="清空历史" @click="handleClearHistory">
              <DeleteOutlined />
            </button>
          </div>
          <div class="history-tags">
            <span
              v-for="h in userHistoryList.slice(0, 12)"
              :key="h.id"
              class="history-tag"
              @click="applyKeyword(h.keyword || '')"
            >
              {{ h.keyword }}
              <CloseOutlined class="history-tag-x" @click.stop="handleDeleteHistory(h.id)" />
            </span>
          </div>
        </div>
      </div>

      <a-spin :spinning="userLoading">
        <div v-if="userResults.length" class="user-grid">
          <div v-for="u in userResults" :key="u.id" class="user-card" @click="goToUser(u.id)">
            <a-avatar :size="56" :src="u.userAvatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <div class="user-meta">
              <div class="user-name-text">{{ u.userName || '未命名用户' }}</div>
              <div class="user-account-text">@{{ u.userAccount }}</div>
              <div v-if="u.userProfile" class="user-bio">{{ u.userProfile }}</div>
            </div>
          </div>
        </div>
        <div v-else-if="!userLoading && picKeyword.trim()" class="empty-state">
          <div class="empty-icon"><UserOutlined /></div>
          <p>没有找到相关用户</p>
        </div>
      </a-spin>

      <div v-if="userTotal > userPageSize" class="pagination-wrap">
        <a-pagination :current="userCurrent" :page-size="userPageSize" :total="userTotal" show-size-changer @change="handleUserPageChange" />
      </div>
    </div>

    <!-- ===== 空间 Tab ===== -->
    <div v-if="activeTab === 'space'" class="tab-content">

      <!-- 历史搜索（无结果时展示） -->
      <div v-if="!spaceLoading && spaceResults.length === 0" class="discovery">
        <div v-if="userStore.isLoggedIn && spaceHistoryList.length > 0" class="discovery-block">
          <div class="discovery-header">
            <span class="discovery-title">历史搜索</span>
            <button class="discovery-clear" title="清空历史" @click="handleClearHistory">
              <DeleteOutlined />
            </button>
          </div>
          <div class="history-tags">
            <span
              v-for="h in spaceHistoryList.slice(0, 12)"
              :key="h.id"
              class="history-tag"
              @click="applyKeyword(h.keyword || '')"
            >
              {{ h.keyword }}
              <CloseOutlined class="history-tag-x" @click.stop="handleDeleteHistory(h.id)" />
            </span>
          </div>
        </div>
      </div>

      <a-spin :spinning="spaceLoading">
        <div v-if="spaceResults.length" class="space-list">
          <div v-for="s in spaceResults" :key="s.id" class="space-card">
            <div class="space-card-header">
              <div class="space-icon-wrap"><CloudOutlined /></div>
              <div class="space-card-info">
                <div class="space-name">{{ s.spaceName }}</div>
                <div class="space-tags">
                  <span class="space-tag type">{{ spaceTypeMap[s.spaceType ?? 0] || '私有' }}</span>
                  <span class="space-tag level">{{ spaceLevelMap[s.spaceLevel ?? 0] || '普通版' }}</span>
                </div>
              </div>
            </div>
            <div class="space-stats">
              <div class="stat-item">
                <span class="stat-value">{{ s.totalCount ?? 0 }}</span>
                <span class="stat-label">图片</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-value">{{ formatSize(s.totalSize || 0) }}</span>
                <span class="stat-label">已用</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-value">{{ formatSize(s.maxSize || 0) }}</span>
                <span class="stat-label">容量</span>
              </div>
            </div>
            <div class="space-card-footer">
              <span v-if="s.user" class="space-owner">
                <a-avatar :size="20" :src="s.user.userAvatar"><template #icon><UserOutlined /></template></a-avatar>
                {{ s.user.userName || '未知' }}
              </span>
              <span class="space-time">{{ s.createTime }}</span>
            </div>
          </div>
        </div>
        <div v-else-if="!spaceLoading && picKeyword.trim()" class="empty-state">
          <div class="empty-icon"><CloudOutlined /></div>
          <p>没有找到相关空间</p>
        </div>
      </a-spin>

      <div v-if="spaceTotal > spacePageSize" class="pagination-wrap">
        <a-pagination :current="spaceCurrent" :page-size="spacePageSize" :total="spaceTotal" show-size-changer @change="handleSpacePageChange" />
      </div>
    </div>

  </div>
</template>

<style scoped>
/* ===== 页面 ===== */
.search-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 64px;
}

/* ===== 顶部搜索栏 ===== */
.search-top {
  padding: 36px 0 0;
}

.search-bar-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 760px;
}

.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--color-bg-warm, #f5f5f0);
  border: 1.5px solid transparent;
  border-radius: 999px;
  padding: 0 16px;
  height: 44px;
  gap: 10px;
  transition: border-color 0.2s, background 0.2s;
}

.search-bar:focus-within {
  border-color: var(--color-primary, #e60023);
  background: var(--color-bg, #fff);
  box-shadow: 0 0 0 3px rgba(230, 0, 35, 0.08);
}

.search-bar-icon {
  color: var(--color-text-disabled, #bbb);
  font-size: 15px;
  flex-shrink: 0;
}

.search-bar-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--color-text-primary, #211922);
  min-width: 0;
}

.search-bar-input::placeholder {
  color: var(--color-text-disabled, #bbb);
}

.search-bar-clear {
  font-size: 12px;
  color: var(--color-text-disabled, #bbb);
  cursor: pointer;
  padding: 2px;
  border-radius: 50%;
  transition: color 0.15s;
  flex-shrink: 0;
}

.search-bar-clear:hover {
  color: var(--color-text-secondary, #888);
}

.search-top-btn {
  padding: 0 24px;
  height: 44px;
  border-radius: 999px;
  border: none;
  background: var(--color-primary, #e60023);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.search-top-btn:hover {
  background: var(--color-primary-hover, #cc001f);
  box-shadow: 0 4px 12px rgba(230, 0, 35, 0.25);
}

/* ===== Tab 栏 ===== */
.tab-bar {
  display: flex;
  border-bottom: 1px solid var(--color-surface-sand, #e5e5e0);
  margin: 20px 0 0;
}

.tab-item {
  padding: 12px 20px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary, #888);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.tab-item::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-primary, #e60023);
  transform: scaleX(0);
  transition: transform 0.2s ease;
}

.tab-item:hover { color: var(--color-text-primary, #211922); }
.tab-item.active { color: var(--color-primary, #e60023); font-weight: 600; }
.tab-item.active::after { transform: scaleX(1); }

/* ===== Tab 内容区 ===== */
.tab-content { padding: 24px 0 0; }

/* ===== 模式切换 ===== */
.mode-switch {
  display: flex;
  gap: 6px;
  margin-bottom: 24px;
  padding: 4px;
  background: var(--color-bg-warm, #f5f5f0);
  border-radius: 10px;
  width: fit-content;
}

.mode-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary, #888);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mode-btn.active {
  background: var(--color-bg, #fff);
  color: var(--color-text-primary, #211922);
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

/* ===== 发现区 ===== */
.discovery { margin-bottom: 28px; }
.discovery-block { margin-bottom: 28px; }

.discovery-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.discovery-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary, #211922);
}

.discovery-clear {
  background: none;
  border: none;
  font-size: 16px;
  color: var(--color-text-disabled, #bbb);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: color 0.15s;
  line-height: 1;
}
.discovery-clear:hover { color: var(--color-primary, #e60023); }

/* 历史标签 */
.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.history-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--color-surface-sand, #e5e5e0);
  background: var(--color-bg, #fff);
  font-size: 13px;
  color: var(--color-text-primary, #211922);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.history-tag:hover {
  border-color: var(--color-primary, #e60023);
  color: var(--color-primary, #e60023);
  background: rgba(230, 0, 35, 0.04);
}

.history-tag-x {
  font-size: 10px;
  color: var(--color-text-disabled, #bbb);
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
}

.history-tag:hover .history-tag-x {
  opacity: 1;
  color: var(--color-primary, #e60023);
}

/* 热门搜索两列网格 */
.hot-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 24px;
}

.hot-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.hot-row:hover { background: var(--color-bg-warm, #f5f5f0); }

.hot-rank {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-disabled, #bbb);
  flex-shrink: 0;
}

.hot-rank.top3 { color: var(--color-primary, #e60023); }
.hot-kw {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-primary, #211922);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-badge {
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary, #e60023);
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

/* ===== 分类筛选 ===== */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.filter-chip {
  padding: 5px 14px;
  border-radius: 20px;
  border: 1px solid var(--color-surface-sand, #e5e5e0);
  background: var(--color-bg, #fff);
  color: var(--color-text-secondary, #888);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.filter-chip:hover {
  border-color: var(--color-border-hover, #ccc);
  color: var(--color-text-primary, #211922);
}

.filter-chip.active {
  border-color: var(--color-primary, #e60023);
  background: rgba(230, 0, 35, 0.04);
  color: var(--color-primary, #e60023);
  font-weight: 500;
}

/* ===== 图片网格 ===== */
.pic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.pic-card {
  background: var(--color-bg, #fff);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(229,229,224,0.5);
  cursor: pointer;
  transition: all 0.25s ease;
}

.pic-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(33,25,34,0.1);
  border-color: transparent;
}

.pic-img-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--color-bg-warm, #f5f5f0);
}

.pic-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.35s ease;
}

.pic-card:hover .pic-img { transform: scale(1.05); }

.pic-overlay {
  position: absolute;
  inset: 0;
  background: rgba(33,25,34,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.pic-card:hover .pic-overlay { opacity: 1; }

.pic-info {
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.pic-name {
  font-size: 13px;
  color: var(--color-text-primary, #211922);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.pic-cat {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--color-bg-warm, #f5f5f0);
  color: var(--color-text-secondary, #888);
  flex-shrink: 0;
}

/* ===== 颜色搜索 ===== */
.color-search-wrap { margin-bottom: 24px; }

.color-input-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.color-picker-input {
  width: 52px;
  height: 40px;
  border: 1px solid var(--color-surface-sand, #e5e5e0);
  border-radius: 8px;
  cursor: pointer;
  padding: 2px;
  background: none;
  flex-shrink: 0;
}

.color-hex {
  font-size: 14px;
  color: var(--color-text-secondary, #888);
  font-family: monospace;
}

.search-btn {
  padding: 0 20px;
  height: 40px;
  border-radius: 8px;
  border: none;
  background: var(--color-primary, #e60023);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.search-btn:hover:not(:disabled) { background: var(--color-primary-hover, #cc001f); }
.search-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.color-search-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.preset-colors { display: flex; flex-wrap: wrap; gap: 8px; }

.preset-chip {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 2px solid rgba(0,0,0,0.08);
  cursor: pointer;
  transition: all 0.2s;
  outline: 2px solid transparent;
  outline-offset: 2px;
}

.preset-chip:hover { transform: scale(1.18); }
.preset-chip.active {
  outline-color: var(--color-primary, #e60023);
  transform: scale(1.12);
}

/* ===== 用户网格 ===== */
.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--color-bg, #fff);
  border-radius: 14px;
  border: 1px solid rgba(229,229,224,0.5);
  cursor: pointer;
  transition: all 0.25s ease;
}

.user-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(33,25,34,0.08);
  border-color: transparent;
}

.user-meta { flex: 1; min-width: 0; }
.user-name-text { font-size: 15px; font-weight: 600; color: var(--color-text-primary, #211922); }
.user-account-text { font-size: 13px; color: var(--color-text-disabled, #bbb); margin-top: 2px; }
.user-bio { font-size: 13px; color: var(--color-text-secondary, #888); margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ===== 空间列表 ===== */
.space-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.space-card {
  padding: 20px;
  background: var(--color-bg, #fff);
  border-radius: 14px;
  border: 1px solid rgba(229,229,224,0.5);
  transition: all 0.25s ease;
}

.space-card:hover { box-shadow: 0 4px 16px rgba(33,25,34,0.06); }

.space-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.space-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(230,0,35,0.08), rgba(230,0,35,0.02));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary, #e60023);
  font-size: 20px;
  flex-shrink: 0;
}

.space-card-info { flex: 1; min-width: 0; }
.space-name { font-size: 15px; font-weight: 600; color: var(--color-text-primary, #211922); }
.space-tags { display: flex; gap: 6px; margin-top: 4px; }

.space-tag { font-size: 11px; padding: 1px 8px; border-radius: 10px; }
.space-tag.type { background: rgba(67,94,229,0.08); color: #435ee5; }
.space-tag.level { background: rgba(16,60,37,0.08); color: #103c25; }

.space-stats {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid rgba(229,229,224,0.5);
  border-bottom: 1px solid rgba(229,229,224,0.5);
  margin-bottom: 12px;
}

.stat-item { flex: 1; text-align: center; }
.stat-value { display: block; font-size: 15px; font-weight: 600; color: var(--color-text-primary, #211922); }
.stat-label { display: block; font-size: 11px; color: var(--color-text-disabled, #bbb); margin-top: 2px; }
.stat-divider { width: 1px; height: 28px; background: var(--color-surface-sand, #e5e5e0); }

.space-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-disabled, #bbb);
}

.space-owner { display: flex; align-items: center; gap: 6px; }

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 72px 0;
  color: var(--color-text-disabled, #bbb);
}

.empty-icon { font-size: 48px; margin-bottom: 12px; opacity: 0.5; }
.empty-state p { font-size: 14px; margin: 0; }

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin: 24px 0 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .search-page { padding: 0 16px 40px; }
  .search-top { padding: 24px 0 0; }
  .search-top-btn { flex: 1; }
  .tab-item { padding: 10px 14px; font-size: 14px; }
  .hot-grid { grid-template-columns: 1fr; }
  .pic-grid { grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 10px; }
  .user-grid { grid-template-columns: 1fr; }
  .space-list { grid-template-columns: 1fr; }
}
</style>
