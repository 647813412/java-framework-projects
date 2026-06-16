<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { TeamOutlined, UserOutlined, ClockCircleOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { applyJoinSpaceUsingPost, listMyTeamSpaceUsingPost } from '@/api/spaceUserController'

const router = useRouter()
const userStore = useUserStore()

// ===== 团队空间列表 =====
const teamSpaces = ref<API.SpaceVO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const applyingSpaceId = ref<string | null>(null)

// 已加入（审核通过）的空间 ID 集合
const mySpaceIds = ref<Set<string>>(new Set())
// 申请中（待审核）的空间 ID 集合
const pendingSpaceIds = ref<Set<string>>(new Set())

async function fetchMySpaces() {
  if (!userStore.isLoggedIn) return
  try {
    const res: any = await listMyTeamSpaceUsingPost()
    if (res?.data) {
      const joined = new Set<string>()
      const pending = new Set<string>()
      for (const item of res.data) {
        const sid = String(item.spaceId || item.space?.id)
        if (item.status === 0) {
          pending.add(sid)
        } else {
          joined.add(sid)
        }
      }
      mySpaceIds.value = joined
      pendingSpaceIds.value = pending
    }
  } catch {
    // 静默
  }
}

async function fetchTeamSpaces() {
  loading.value = true
  try {
    const params: API.SpaceQueryRequest = {
      spaceType: 1,
      current: currentPage.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    }
    const res: any = await listSpaceVoByPageUsingPost(params)
    if (res?.data) {
      teamSpaces.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
}

function isMemberOrOwner(space: API.SpaceVO): boolean {
  const sid = String(space.id)
  if (String(space.userId) === String(userStore.loginUser?.id)) return true
  return mySpaceIds.value.has(sid)
}

function isPending(space: API.SpaceVO): boolean {
  return pendingSpaceIds.value.has(String(space.id))
}

// 已加入的空间（当前页）
const joinedSpaces = computed(() =>
  teamSpaces.value.filter((s) => isMemberOrOwner(s))
)
// 未加入（含待审核）的空间（当前页）
const unjoinedSpaces = computed(() =>
  teamSpaces.value.filter((s) => !isMemberOrOwner(s))
)

function handleSpaceClick(space: API.SpaceVO) {
  router.push(`/space/${String(space.id)}`)
}

async function handleApplyJoin(space: API.SpaceVO, e: Event) {
  e.stopPropagation()
  applyingSpaceId.value = String(space.id)
  try {
    await applyJoinSpaceUsingPost({ spaceId: space.id as any })
    message.success('申请已提交，等待管理员审批')
    pendingSpaceIds.value.add(String(space.id))
  } catch {
    // 拦截器已处理
  } finally {
    applyingSpaceId.value = null
  }
}

function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  fetchTeamSpaces()
}

function showTotal(t: number) {
  return `共 ${t} 个团队空间`
}

onMounted(async () => {
  await fetchMySpaces()
  fetchTeamSpaces()
})
</script>

<template>
  <div class="team-spaces-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header-content">
        <h1 class="page-title"><TeamOutlined /> 团队空间</h1>
        <p class="page-desc">浏览所有团队空间，加入感兴趣的团队</p>
      </div>
    </div>

    <a-spin :spinning="loading">
      <!-- ===== 已加入的团队 ===== -->
      <template v-if="joinedSpaces.length > 0">
        <div class="section-label">
          <span class="section-label-dot joined"></span> 我已加入
        </div>
        <div class="team-grid section-gap">
          <div
            v-for="space in joinedSpaces"
            :key="space.id"
            class="team-card"
            @click="handleSpaceClick(space)"
          >
            <div class="team-card-avatar">
              <a-avatar :size="56" :src="space.avatar">
                <template #icon><TeamOutlined style="font-size: 26px" /></template>
              </a-avatar>
            </div>
            <div class="team-card-body">
              <div class="team-card-name">{{ space.spaceName || '未命名空间' }}</div>
              <div class="team-card-meta">
                <span class="meta-item"><UserOutlined /> {{ space.user?.userName || '未知' }}</span>
              </div>
            </div>
            <button class="team-enter-btn" @click.stop="handleSpaceClick(space)">进入</button>
          </div>
        </div>
      </template>

      <!-- ===== 未加入的团队 ===== -->
      <template v-if="unjoinedSpaces.length > 0">
        <div class="section-label" :style="{ marginTop: joinedSpaces.length > 0 ? 'var(--space-2xl)' : '0' }">
          <span class="section-label-dot unjoined"></span> 发现更多
        </div>
        <div class="team-grid section-gap">
          <div
            v-for="space in unjoinedSpaces"
            :key="space.id"
            class="team-card"
            @click="isPending(space) ? undefined : handleSpaceClick(space)"
          >
            <div class="team-card-avatar">
              <a-avatar :size="56" :src="space.avatar">
                <template #icon><TeamOutlined style="font-size: 26px" /></template>
              </a-avatar>
            </div>
            <div class="team-card-body">
              <div class="team-card-name">{{ space.spaceName || '未命名空间' }}</div>
              <div class="team-card-meta">
                <span class="meta-item"><UserOutlined /> {{ space.user?.userName || '未知' }}</span>
              </div>
            </div>
            <button
              v-if="isPending(space)"
              class="team-pending-btn"
              disabled
              @click.stop
            >
              <ClockCircleOutlined /> 待审核
            </button>
            <button
              v-else
              class="team-apply-btn"
              :disabled="applyingSpaceId === String(space.id)"
              @click.stop="handleApplyJoin(space, $event)"
            >
              {{ applyingSpaceId === String(space.id) ? '申请中...' : '申请加入' }}
            </button>
          </div>
        </div>
      </template>

      <!-- 全空状态 -->
      <div v-if="!loading && teamSpaces.length === 0" class="empty-state">
        <a-empty description="暂无团队空间" />
      </div>
    </a-spin>

    <!-- 分页（始终显示在底部） -->
    <div class="pagination-bar">
      <a-pagination
        v-if="total > 0"
        v-model:current="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-size-options="['12', '20', '40']"
        show-size-changer
        show-quick-jumper
        :show-total="showTotal"
        @change="handlePageChange"
        @showSizeChange="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.team-spaces-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-xl) var(--space-lg) 0;
  min-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--color-surface-sand);
}

.page-title {
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.page-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--color-text-disabled);
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-lg);
}

.team-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-bg-warm);
  border: 1px solid var(--color-surface-sand);
  border-radius: var(--radius-card);
  cursor: pointer;
  transition:
    transform var(--transition-normal),
    box-shadow var(--transition-normal);
}

.team-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(33, 25, 34, 0.1);
}

.team-card-avatar .ant-avatar {
  border: 3px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 2px 8px rgba(33, 25, 34, 0.1);
  background: linear-gradient(135deg, rgba(230, 0, 35, 0.06), rgba(67, 94, 229, 0.06));
}

.team-card-body {
  flex: 1;
  min-width: 0;
}

.team-card-name {
  font-size: 15px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-card-meta {
  display: flex;
  gap: var(--space-sm);
}

.meta-item {
  font-size: 12px;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.team-apply-btn,
.team-enter-btn,
.team-pending-btn {
  flex-shrink: 0;
  padding: 6px 18px;
  border-radius: var(--radius-button);
  font-size: 13px;
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.team-pending-btn {
  border: 1px solid #d9d9d9;
  background: #fafafa;
  color: var(--color-text-secondary);
  cursor: not-allowed;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.team-apply-btn {
  border: 1px solid var(--color-primary);
  background: transparent;
  color: var(--color-primary);
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
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.team-enter-btn:hover {
  opacity: 0.85;
}

.empty-state {
  padding: 80px 0;
  text-align: center;
  flex: 1;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-md);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.section-label-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.section-label-dot.joined {
  background: #52c41a;
}

.section-label-dot.unjoined {
  background: var(--color-primary);
}

.section-gap {
  margin-bottom: var(--space-lg);
}

.pagination-bar {
  margin-top: auto;
  display: flex;
  justify-content: center;
  padding: var(--space-xl) 0;
}
</style>
