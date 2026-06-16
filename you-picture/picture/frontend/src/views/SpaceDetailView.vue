<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  TeamOutlined,
  UserOutlined,
  CalendarOutlined,
  CrownOutlined,
  EditOutlined,
  EyeOutlined,
  DeleteOutlined,
  LockOutlined,
  UploadOutlined,
  CloudUploadOutlined,
  LinkOutlined,
  InboxOutlined,
  FormOutlined,
  CheckOutlined,
  StopOutlined,
  ArrowLeftOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { getSpaceVoByIdUsingGet, editSpaceUsingPost, deleteSpaceUsingPost } from '@/api/spaceController'
import { fileUploadUsingPost, uploadByUrlUsingPost } from '@/api/fileController'
import {
  listPictureVoByPageUsingPost,
  uploadPictureUsingPost,
  uploadUrlPictureUsingPost,
  deletePictureUsingPost,
  editPictureUsingPost,
} from '@/api/pictureController'
import {
  listSpaceUserUsingPost,
  deleteSpaceUserUsingPost,
  approveSpaceUserUsingPost,
  editSpaceUserUsingPost,
  quitSpaceUsingPost,
} from '@/api/spaceUserController'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.loginUser)

const spaceId = computed(() => route.params.id as string)
const spaceInfo = ref<API.SpaceVO | null>(null)
const spaceLoading = ref(false)
const isMember = ref(false)
const isAdmin = ref(false)
const isOwner = ref(false)
const isEditor = ref(false)
const noPermission = ref(false)
const currentMemberRecord = ref<API.SpaceUserVO | null>(null)
// canEdit: admin/owner/editor 可以编辑操作
const canEdit = computed(() => isAdmin.value || isOwner.value || isEditor.value)

// 图片列表
const pictures = ref<API.PictureVO[]>([])
const picLoading = ref(false)
const picCurrent = ref(1)
const picPageSize = ref(12)
const picTotal = ref(0)

// 成员列表
const members = ref<API.SpaceUserVO[]>([])
const membersLoading = ref(false)

// 活动 tab
const activeTab = ref<'pictures' | 'members' | 'approval'>('pictures')

// 待审核成员
const pendingMembers = ref<API.SpaceUserVO[]>([])
const pendingLoading = ref(false)
const approvingId = ref<number | null>(null)

async function fetchSpaceInfo() {
  spaceLoading.value = true
  try {
    const res: any = await getSpaceVoByIdUsingGet({ id: spaceId.value as any })
    if (res?.data) {
      spaceInfo.value = res.data
    }
  } catch {
    // 静默
  } finally {
    spaceLoading.value = false
  }
}

async function fetchPictures() {
  picLoading.value = true
  noPermission.value = false
  try {
    const res: any = await listPictureVoByPageUsingPost({
      spaceId: spaceId.value as any,
      current: picCurrent.value,
      pageSize: picPageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res?.data) {
      pictures.value = res.data.records || []
      picTotal.value = res.data.total || 0
    }
  } catch (e: any) {
    // 没有权限访问
    noPermission.value = true
    pictures.value = []
    picTotal.value = 0
  } finally {
    picLoading.value = false
  }
}

async function fetchMembers() {
  membersLoading.value = true
  try {
    const res: any = await listSpaceUserUsingPost({ spaceId: spaceId.value as any })
    if (res?.data) {
      members.value = res.data
      // 检查当前用户是否为成员
      const currentMember = members.value.find(
        (m) => String(m.userId) === String(user.value?.id)
      )
      if (currentMember) {
        isMember.value = true
        isAdmin.value = currentMember.spaceRole === 'admin'
        isOwner.value = currentMember.spaceRole === 'creator'
        isEditor.value = currentMember.spaceRole === 'editor'
        currentMemberRecord.value = currentMember
      }
    }
  } catch {
    // 如果无权获取成员列表，则非成员
    isMember.value = false
  } finally {
    membersLoading.value = false
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

function handleDeleteMember(member: API.SpaceUserVO) {
  Modal.confirm({
    title: '确认移除',
    content: `确定要将「${member.user?.userName || '该用户'}」从团队中移除吗？`,
    okText: '移除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteSpaceUserUsingPost({ id: member.id as number })
        message.success('已移除成员')
        fetchMembers()
      } catch {
        // 拦截器已处理
      }
    },
  })
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function showTotal(t: number) {
  return `共 ${t} 张`
}

function getRoleLabel(role?: string) {
  if (role === 'admin') return '管理员'
  if (role === 'editor') return '编辑者'
  return '查看者'
}

// ===== 成员审核 =====
async function fetchPendingMembers() {
  pendingLoading.value = true
  try {
    const res: any = await listSpaceUserUsingPost({
      spaceId: spaceId.value as any,
      status: 0,
    })
    if (res?.data) {
      pendingMembers.value = res.data
    }
  } catch {
    pendingMembers.value = []
  } finally {
    pendingLoading.value = false
  }
}

async function handleApprove(member: API.SpaceUserVO) {
  approvingId.value = member.id as number
  try {
    await approveSpaceUserUsingPost({ id: member.id, status: 1 })
    message.success(`已通过「${member.user?.userName || '该用户'}」的申请`)
    // 就地标记为已通过，保留记录可见
    member.status = 1
    fetchMembers()
  } catch {
    // 拦截器已处理
  } finally {
    approvingId.value = null
  }
}

async function handleReject(member: API.SpaceUserVO) {
  approvingId.value = member.id as number
  try {
    await approveSpaceUserUsingPost({ id: member.id, status: 2 })
    message.success(`已拒绝「${member.user?.userName || '该用户'}」的申请`)
    // 就地标记为已拒绝，保留记录可见
    member.status = 2
  } catch {
    // 拦截器已处理
  } finally {
    approvingId.value = null
  }
}

// ===== 修改成员权限 =====
async function handleChangeRole(member: API.SpaceUserVO, newRole: string) {
  try {
    await editSpaceUserUsingPost({ id: member.id, spaceRole: newRole })
    message.success(`已将「${member.user?.userName || '该用户'}」的权限修改为${getRoleLabel(newRole)}`)
    fetchMembers()
  } catch {
    // 拦截器已处理
  }
}

// ===== 上传图片到团队空间 =====
const uploadOpen = ref(false)
const uploadTab = ref<'file' | 'url'>('file')
const uploading = ref(false)
const fileList = ref<any[]>([])
const previewUrl = ref('')
const urlInput = ref('')
const urlUploading = ref(false)

function openUpload() {
  uploadTab.value = 'file'
  uploading.value = false
  fileList.value = []
  previewUrl.value = ''
  urlInput.value = ''
  urlUploading.value = false
  uploadOpen.value = true
}

function handleFileChange(info: any) {
  const file = info.file?.originFileObj || info.file
  if (!file) return
  fileList.value = [info.file]
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
  autoUpload(file)
}

async function autoUpload(file: File) {
  uploading.value = true
  try {
    const res: any = await uploadPictureUsingPost({ spaceId: spaceId.value as any }, {}, file)
    if (res?.data?.id) {
      message.success('上传成功')
      uploadOpen.value = false
      router.push(`/picture/edit/${res.data.id}?from=space&spaceId=${spaceId.value}`)
    }
  } catch {
    // 拦截器已处理
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
    autoUpload(file)
  }
}

function clearFile() {
  fileList.value = []
  previewUrl.value = ''
}

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
      uploadOpen.value = false
      router.push(`/picture/edit/${res.data.id}?from=space&spaceId=${spaceId.value}`)
    }
  } catch {
    // 拦截器已处理
  } finally {
    urlUploading.value = false
  }
}

// ===== 编辑/删除图片 =====
const editPicModalOpen = ref(false)
const editPicForm = ref({
  id: undefined as number | undefined,
  name: '',
  introduction: '',
  category: '',
  tags: [] as string[],
})
const editPicLoading = ref(false)

function handleEditPicture(pic: API.PictureVO, e: Event) {
  e.stopPropagation()
  editPicForm.value = {
    id: pic.id,
    name: pic.name || '',
    introduction: pic.introduction || '',
    category: pic.category || '',
    tags: pic.tags ? [...pic.tags] : [],
  }
  editPicModalOpen.value = true
}

async function handleSavePicture() {
  if (!editPicForm.value.id) return
  editPicLoading.value = true
  try {
    await editPictureUsingPost({
      id: editPicForm.value.id,
      name: editPicForm.value.name,
      introduction: editPicForm.value.introduction,
      category: editPicForm.value.category,
      tags: editPicForm.value.tags,
    })
    message.success('图片信息已更新')
    editPicModalOpen.value = false
    fetchPictures()
  } catch {
    // 拦截器已处理
  } finally {
    editPicLoading.value = false
  }
}

function handleDeletePicture(pic: API.PictureVO, e: Event) {
  e.stopPropagation()
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除「${pic.name || '无标题'}」吗？此操作不可撤销。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deletePictureUsingPost({ id: pic.id })
        message.success('已删除')
        fetchPictures()
      } catch {
        // 拦截器已处理
      }
    },
  })
}

// ===== 退出团队 =====
function handleLeaveTeam() {
  Modal.confirm({
    title: '退出团队',
    content: '确定要退出该团队空间吗？退出后将无法查看团队内容。',
    okText: '退出',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await quitSpaceUsingPost({ id: spaceId.value as any })
        message.success('已退出团队')
        router.push('/profile')
      } catch {
        // 拦截器已处理
      }
    },
  })
}

// ===== 编辑空间信息 =====
const editSpaceOpen = ref(false)
const editSpaceForm = ref({ spaceName: '', avatar: '', avatarUrl: '', avatarMode: 'url' as 'url' | 'file' })
const editSpaceLoading = ref(false)
const avatarUploading = ref(false)

function openEditSpace() {
  editSpaceForm.value = {
    spaceName: spaceInfo.value?.spaceName || '',
    avatar: spaceInfo.value?.avatar || '',
    avatarUrl: spaceInfo.value?.avatar || '',
    avatarMode: 'url',
  }
  editSpaceOpen.value = true
}

async function handleAvatarFileUpload(file: File) {
  avatarUploading.value = true
  try {
    const res: any = await fileUploadUsingPost({}, file)
    if (res?.data) {
      editSpaceForm.value.avatar = res.data
      message.success('头像上传成功')
    }
  } catch {
    // 拦截器已处理
  } finally {
    avatarUploading.value = false
  }
  return false // 阻止 a-upload 默认行为
}

async function handleAvatarUrlUpload() {
  const url = editSpaceForm.value.avatarUrl.trim()
  if (!url) {
    message.warning('请输入图片 URL')
    return
  }
  avatarUploading.value = true
  try {
    const res: any = await uploadByUrlUsingPost({ url })
    if (res?.data) {
      editSpaceForm.value.avatar = res.data
      message.success('头像上传成功')
    }
  } catch {
    // 拦截器已处理
  } finally {
    avatarUploading.value = false
  }
}

async function handleSaveSpace() {
  if (!editSpaceForm.value.spaceName.trim()) {
    message.warning('请输入空间名称')
    return
  }
  editSpaceLoading.value = true
  try {
    await editSpaceUsingPost({
      id: spaceId.value as any,
      spaceName: editSpaceForm.value.spaceName,
      avatar: editSpaceForm.value.avatar || undefined,
    } as API.SpaceEditRequest)
    message.success('空间信息已更新')
    editSpaceOpen.value = false
    fetchSpaceInfo()
  } catch {
    // 拦截器已处理
  } finally {
    editSpaceLoading.value = false
  }
}

// ===== 解散团队 =====
function handleDissolveSpace() {
  Modal.confirm({
    title: '解散团队',
    content: `确定要解散「${spaceInfo.value?.spaceName || '该团队'}${'」吗？解散后所有图片和成员数据将永久删除，不可撤销。'}`,
    okText: '解散',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteSpaceUsingPost({ id: spaceId.value as any })
        message.success('团队已解散')
        router.push('/team-spaces')
      } catch {
        // 拦截器已处理
      }
    },
  })
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  await fetchSpaceInfo()
  // 并发获取图片和成员
  await Promise.all([fetchPictures(), fetchMembers()])
  // 管理员/创建者额外获取待审核列表
  if (isAdmin.value || isOwner.value) {
    fetchPendingMembers()
  }
})
</script>

<template>
  <div class="space-detail-page">
    <!-- 返回按钮 -->
    <div class="space-back-bar">
      <a-button type="text" @click="router.back()">
        <ArrowLeftOutlined /> 返回
      </a-button>
    </div>

    <a-spin :spinning="spaceLoading" v-if="spaceLoading" style="padding: 100px 0; display: block; text-align: center" />

    <template v-if="spaceInfo">
      <!-- 空间信息头部 -->
      <div class="space-header-banner">
        <div class="space-header-bg"></div>
        <div class="space-header-content">
          <div class="space-header-avatar">
            <a-avatar :size="80" :src="spaceInfo.avatar">
              <template #icon><TeamOutlined style="font-size: 36px" /></template>
            </a-avatar>
          </div>
          <div class="space-header-info">
            <h1 class="space-header-name">{{ spaceInfo.spaceName || '未命名空间' }}</h1>
            <div class="space-header-meta">
              <span class="space-meta-item">
                <UserOutlined />
                {{ spaceInfo.user?.userName || '未知用户' }}
              </span>
              <span class="space-meta-item">
                <CalendarOutlined />
                {{ formatDate(spaceInfo.createTime) }}
              </span>
              <span class="space-meta-item" v-if="spaceInfo.spaceType === 1">
                <TeamOutlined /> 团队空间
              </span>
              <span class="space-meta-item" v-else>
                <LockOutlined /> 私有空间
              </span>
            </div>
          </div>
          <div class="space-header-actions">
            <!-- 普通成员（非管理员、非创建者）：退出队伍 -->
            <a-button v-if="isMember && !isOwner && !isAdmin" danger @click="handleLeaveTeam">退出队伍</a-button>
            <!-- 管理员/创建者：编辑空间 + 解散团队 -->
            <a-button v-if="isAdmin || isOwner" @click="openEditSpace"><SettingOutlined /> 编辑信息</a-button>
            <a-button v-if="isAdmin || isOwner" danger @click="handleDissolveSpace">解散团队</a-button>
          </div>
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="space-tab-bar">
        <button
          class="tab-item"
          :class="{ active: activeTab === 'pictures' }"
          @click="activeTab = 'pictures'"
        >
          图片 <span v-if="picTotal" class="tab-count">{{ picTotal }}</span>
        </button>
        <button
          v-if="isMember"
          class="tab-item"
          :class="{ active: activeTab === 'members' }"
          @click="activeTab = 'members'"
        >
          成员 <span v-if="members.length" class="tab-count">{{ members.length }}</span>
        </button>
        <button
          v-if="isAdmin || isOwner"
          class="tab-item"
          :class="{ active: activeTab === 'approval' }"
          @click="activeTab = 'approval'; fetchPendingMembers()"
        >
          审核 <span v-if="pendingMembers.filter(m => m.status === 0).length" class="tab-count tab-count-warn">{{ pendingMembers.filter(m => m.status === 0).length }}</span>
        </button>
      </div>

      <!-- 图片列表 -->
      <div v-show="activeTab === 'pictures'" class="space-content">
        <!-- 无权限提示 -->
        <div v-if="noPermission" class="no-permission-section">
          <LockOutlined class="no-perm-icon" />
          <h3>无权限访问</h3>
          <p>你不是该空间的成员，无法查看图片内容</p>
        </div>

        <template v-else>
          <!-- 上传按钮（编辑者及以上可见） -->
          <div v-if="canEdit" class="gallery-toolbar">
            <a-button type="primary" @click="openUpload">
              <UploadOutlined /> 上传图片
            </a-button>
          </div>

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
                  <div v-if="canEdit" class="pic-overlay">
                    <button class="overlay-btn edit" title="编辑" @click="handleEditPicture(pic, $event)">
                      <FormOutlined />
                    </button>
                    <button class="overlay-btn delete" title="删除" @click="handleDeletePicture(pic, $event)">
                      <DeleteOutlined />
                    </button>
                  </div>
                </div>
                <div class="pic-info">
                  <div class="pic-name">{{ pic.name || '无标题' }}</div>
                  <div class="pic-bottom">
                    <span v-if="pic.category" class="pic-category">{{ pic.category }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else-if="!picLoading" class="empty-state">
              <a-empty description="暂无图片" />
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
        </template>
      </div>

      <!-- 成员列表 -->
      <div v-show="activeTab === 'members'" class="space-content">
        <a-spin :spinning="membersLoading">
          <div v-if="members.length > 0" class="members-grid">
            <div
              v-for="member in members"
              :key="member.id"
              class="member-card"
            >
              <div
                class="member-avatar member-clickable"
                @click="router.push('/user/' + member.userId)"
                title="查看个人主页"
              >
                <a-avatar :size="48" :src="member.user?.userAvatar">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
              </div>
              <div class="member-info">
                <div
                  class="member-name member-clickable"
                  @click="router.push('/user/' + member.userId)"
                >{{ member.user?.userName || '未知用户' }}</div>
                <div class="member-role">
                  <CrownOutlined v-if="member.spaceRole === 'admin'" />
                  <EditOutlined v-else-if="member.spaceRole === 'editor'" />
                  <EyeOutlined v-else />
                  {{ getRoleLabel(member.spaceRole) }}
                </div>
              </div>
              <div class="member-actions">
                <span class="member-join-time">{{ formatDate(member.createTime) }}</span>
                <template v-if="(isAdmin || isOwner) && String(member.userId) !== String(user?.id)">
                  <a-dropdown :trigger="['click']">
                    <a-button type="link" size="small">
                      权限 ▾
                    </a-button>
                    <template #overlay>
                      <a-menu @click="(e) => handleChangeRole(member, e.key)">
                        <a-menu-item key="admin" :disabled="member.spaceRole === 'admin'">
                          <CrownOutlined /> 管理员
                        </a-menu-item>
                        <a-menu-item key="editor" :disabled="member.spaceRole === 'editor'">
                          <EditOutlined /> 编辑者
                        </a-menu-item>
                        <a-menu-item key="viewer" :disabled="member.spaceRole === 'viewer'">
                          <EyeOutlined /> 查看者
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                  <a-button
                    type="link"
                    danger
                    size="small"
                    @click="handleDeleteMember(member)"
                  >
                    <DeleteOutlined /> 移除
                  </a-button>
                </template>
              </div>
            </div>
          </div>
          <div v-else-if="!membersLoading" class="empty-state">
            <a-empty description="暂无成员信息" />
          </div>
        </a-spin>
      </div>

      <!-- 审核列表 -->
      <div v-show="activeTab === 'approval'" class="space-content">
        <a-spin :spinning="pendingLoading">
          <div v-if="pendingMembers.length > 0" class="members-grid">
            <div
              v-for="member in pendingMembers"
              :key="member.id"
              class="member-card"
            >
              <div class="member-avatar">
                <a-avatar :size="48" :src="member.user?.userAvatar">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
              </div>
              <div class="member-info">
                <div class="member-name">{{ member.user?.userName || '未知用户' }}</div>
                <div v-if="member.status === 0" class="member-status pending">待审核</div>
                <div v-else-if="member.status === 1" class="member-status approved">已通过</div>
                <div v-else-if="member.status === 2" class="member-status rejected">已拒绝</div>
              </div>
              <div class="member-actions">
                <span class="member-join-time">{{ formatDate(member.createTime) }}</span>
                <template v-if="member.status === 0">
                  <a-button
                    type="primary"
                    size="small"
                    :loading="approvingId === member.id"
                    @click="handleApprove(member)"
                  >
                    <CheckOutlined /> 通过
                  </a-button>
                  <a-button
                    danger
                    size="small"
                    :loading="approvingId === member.id"
                    @click="handleReject(member)"
                  >
                    <StopOutlined /> 拒绝
                  </a-button>
                </template>
              </div>
            </div>
          </div>
          <div v-else-if="!pendingLoading" class="empty-state">
            <a-empty description="暂无待审核申请" />
          </div>
        </a-spin>
      </div>
    </template>

    <!-- 空间不存在 -->
    <div v-if="!spaceLoading && !spaceInfo" class="empty-state" style="padding: 100px 0">
      <a-empty description="空间不存在或已被删除" />
    </div>

    <!-- ===== 上传弹窗 ===== -->
    <a-modal
      v-model:open="uploadOpen"
      title="上传到团队空间"
      :footer="null"
      :width="680"
      :destroy-on-close="true"
    >
      <div class="priv-upload-tabs">
        <div
          class="priv-tab-item"
          :class="{ active: uploadTab === 'file' }"
          @click="uploadTab = 'file'"
        >
          <CloudUploadOutlined />
          <span>上传图片</span>
        </div>
        <div
          class="priv-tab-item"
          :class="{ active: uploadTab === 'url' }"
          @click="uploadTab = 'url'"
        >
          <LinkOutlined />
          <span>链接上传</span>
        </div>
      </div>

      <div v-if="uploadTab === 'file'" class="priv-upload-content">
        <div
          v-if="!previewUrl"
          class="priv-upload-dragger"
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
            <div class="priv-dragger-inner">
              <div class="priv-dragger-illustration">
                <InboxOutlined class="priv-dragger-icon" />
              </div>
              <p class="priv-dragger-text">点击或拖拽上传图片</p>
              <div class="priv-dragger-formats">
                <span class="priv-format-tag">JPG</span>
                <span class="priv-format-tag">PNG</span>
                <span class="priv-format-tag">GIF</span>
                <span class="priv-format-tag">WEBP</span>
              </div>
            </div>
          </a-upload>
        </div>

        <div v-else class="priv-upload-preview">
          <div class="priv-preview-card">
            <img :src="previewUrl" class="priv-preview-img" alt="预览" />
            <div class="priv-preview-overlay">
              <a-button size="small" danger @click="clearFile">移除</a-button>
            </div>
          </div>
          <div class="priv-upload-actions">
            <a-button type="primary" size="large" :loading="uploading">
              {{ uploading ? '上传中...' : '确认上传' }}
            </a-button>
          </div>
        </div>
      </div>

      <div v-else class="priv-upload-content">
        <div class="priv-url-area">
          <div class="priv-url-illustration">
            <LinkOutlined class="priv-url-icon" />
          </div>
          <p class="priv-url-desc">通过图片链接上传到团队空间</p>
          <div class="priv-url-input-row">
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
          <p class="priv-url-hint">支持 http/https 协议的公开图片地址</p>
        </div>
      </div>
    </a-modal>

    <!-- ===== 编辑图片弹窗 ===== -->
    <a-modal
      v-model:open="editPicModalOpen"
      title="编辑图片信息"
      :confirm-loading="editPicLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSavePicture"
      :width="520"
    >
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="图片名称">
          <a-input v-model:value="editPicForm.name" placeholder="给图片起个名字" :maxlength="50" />
        </a-form-item>
        <a-form-item label="图片简介">
          <a-textarea
            v-model:value="editPicForm.introduction"
            placeholder="描述一下这张图片"
            :maxlength="200"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </a-form-item>
        <a-form-item label="分类">
          <a-input v-model:value="editPicForm.category" placeholder="图片分类" :maxlength="20" />
        </a-form-item>
        <a-form-item label="标签">
          <a-select
            v-model:value="editPicForm.tags"
            mode="tags"
            placeholder="输入后回车添加标签"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑空间信息弹窗 -->
    <a-modal
      v-model:open="editSpaceOpen"
      title="编辑空间信息"
      :confirm-loading="editSpaceLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSaveSpace"
    >
      <a-form layout="vertical" style="margin-top: 8px">
        <a-form-item label="空间名称">
          <a-input v-model:value="editSpaceForm.spaceName" placeholder="请输入空间名称" maxlength="30" show-count />
        </a-form-item>
        <a-form-item label="头像">
          <!-- 上传模式切换 -->
          <a-radio-group v-model:value="editSpaceForm.avatarMode" size="small" style="margin-bottom: 10px">
            <a-radio-button value="url">URL 链接</a-radio-button>
            <a-radio-button value="file">本地上传</a-radio-button>
          </a-radio-group>

          <!-- URL 模式 -->
          <template v-if="editSpaceForm.avatarMode === 'url'">
            <div style="display: flex; gap: 8px">
              <a-input
                v-model:value="editSpaceForm.avatarUrl"
                placeholder="输入图片 URL"
                style="flex: 1"
              />
              <a-button :loading="avatarUploading" @click="handleAvatarUrlUpload">上传</a-button>
            </div>
          </template>

          <!-- 文件模式 -->
          <template v-else>
            <a-upload
              :show-upload-list="false"
              accept="image/*"
              :before-upload="handleAvatarFileUpload"
            >
              <a-button :loading="avatarUploading"><UploadOutlined /> 选择图片</a-button>
            </a-upload>
          </template>

          <!-- 头像预览 -->
          <div v-if="editSpaceForm.avatar" style="margin-top: 10px; display: flex; align-items: center; gap: 10px">
            <a-avatar :size="56" :src="editSpaceForm.avatar">
              <template #icon><TeamOutlined /></template>
            </a-avatar>
            <a-button type="link" danger size="small" @click="editSpaceForm.avatar = ''">移除</a-button>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.space-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 0;
  min-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
}

.space-back-bar {
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--color-surface-sand);
  margin-bottom: var(--space-lg);
}

/* ===== 空间头部 ===== */
.space-header-banner {
  position: relative;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  overflow: hidden;
  margin-bottom: var(--space-lg);
}

.space-header-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 75% 20%, rgba(230, 0, 35, 0.08) 0%, transparent 55%),
    radial-gradient(ellipse at 25% 80%, rgba(16, 60, 37, 0.06) 0%, transparent 55%),
    linear-gradient(135deg, #f6f6f3 0%, #e5e5e0 40%, #e0e0d9 70%, #f6f6f3 100%);
}

.space-header-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--space-xl);
  padding: var(--space-2xl) var(--space-xl);
}

.space-header-avatar .ant-avatar {
  border: 4px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 20px rgba(33, 25, 34, 0.12);
  background: linear-gradient(135deg, rgba(230, 0, 35, 0.08), rgba(67, 94, 229, 0.08));
}

.space-header-info {
  flex: 1;
  min-width: 0;
}

.space-header-actions {
  flex-shrink: 0;
}

.space-header-name {
  font-family: var(--font-display);
  font-size: 1.75rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-sm);
  letter-spacing: 0.06em;
}

.space-header-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.space-meta-item {
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

/* ===== Tab 切换 ===== */
.space-tab-bar {
  display: flex;
  gap: var(--space-xs);
  padding: 0 var(--space-sm);
  margin-bottom: var(--space-lg);
}

.tab-item {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: var(--space-sm) var(--space-lg);
  font-size: 15px;
  font-weight: var(--weight-medium);
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all var(--transition-fast);
  border-radius: var(--radius-button) var(--radius-button) 0 0;
  user-select: none;
}

.tab-item:hover {
  color: var(--color-text-primary);
  background: rgba(230, 0, 35, 0.03);
}

.tab-item.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  background: rgba(230, 0, 35, 0.04);
}

.tab-count {
  font-size: 11px;
  font-weight: var(--weight-semibold);
  background: var(--color-surface-sand);
  color: var(--color-text-secondary);
  padding: 1px 8px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.tab-item.active .tab-count {
  background: rgba(230, 0, 35, 0.1);
  color: var(--color-primary);
}

.tab-count-warn {
  background: #fff7e6;
  color: #fa8c16;
}

.tab-item.active .tab-count-warn {
  background: #fff1b8;
  color: #d48806;
}

/* ===== 审核状态标签 ===== */
.member-status.pending {
  font-size: 12px;
  color: #fa8c16;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.member-status.approved {
  font-size: 12px;
  color: #52c41a;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.member-status.rejected {
  font-size: 12px;
  color: #ff4d4f;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ===== 内容区域 ===== */
.space-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 300px;
}

.gallery-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 0 var(--space-sm) var(--space-md);
}

/* ===== 无权限 ===== */
.no-permission-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-3xl) 0;
  text-align: center;
  color: var(--color-text-secondary);
}

.no-perm-icon {
  font-size: 56px;
  color: var(--color-text-disabled);
  margin-bottom: var(--space-lg);
}

.no-permission-section h3 {
  font-size: 18px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-sm);
}

.no-permission-section p {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* ===== 图片网格 ===== */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: var(--space-md);
  padding: 0 var(--space-sm);
}

.pic-card {
  background: var(--color-bg);
  border-radius: var(--radius-card);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--color-surface-sand);
  transition:
    transform var(--transition-normal),
    box-shadow var(--transition-normal);
}

.pic-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(33, 25, 34, 0.12);
}

.pic-img-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--color-bg-warm);
}

.pic-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform var(--transition-normal);
}

.pic-card:hover .pic-img {
  transform: scale(1.04);
}

.pic-overlay {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  gap: 6px;
  padding: var(--space-sm);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.pic-card:hover .pic-overlay {
  opacity: 1;
}

.overlay-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
  backdrop-filter: blur(8px);
}

.overlay-btn.edit {
  background: rgba(255, 255, 255, 0.85);
  color: var(--color-text-primary);
}

.overlay-btn.edit:hover {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.overlay-btn.delete {
  background: rgba(255, 255, 255, 0.85);
  color: var(--color-error);
}

.overlay-btn.delete:hover {
  background: var(--color-primary);
  color: #fff;
}

.pic-info {
  padding: var(--space-sm) var(--space-md) var(--space-md);
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
  align-items: center;
  justify-content: space-between;
}

.pic-category {
  font-size: 11px;
  color: var(--color-primary);
  background: rgba(230, 0, 35, 0.06);
  padding: 1px 8px;
  border-radius: var(--radius-base);
}

/* ===== 成员列表 ===== */
.members-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: 0 var(--space-sm);
}

.member-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-bg);
  border: 1px solid var(--color-surface-sand);
  border-radius: var(--radius-card);
  transition: background var(--transition-fast);
}

.member-card:hover {
  background: var(--color-bg-warm);
}

.member-avatar .ant-avatar {
  border: 2px solid var(--color-surface-sand);
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 14px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.member-clickable {
  cursor: pointer;
}

.member-clickable:hover .ant-avatar,
.member-avatar.member-clickable:hover {
  opacity: 0.8;
}

.member-name.member-clickable:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.member-role {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(230, 0, 35, 0.05);
  padding: 1px 8px;
  border-radius: 10px;
}

.member-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.member-join-time {
  font-size: 12px;
  color: var(--color-text-disabled);
}

/* ===== 空状态 ===== */
.empty-state {
  padding: var(--space-3xl) 0;
  text-align: center;
}

/* ===== 分页 ===== */
.gallery-pagination {
  margin-top: auto;
  display: flex;
  justify-content: center;
  padding: var(--space-xl) 0 var(--space-3xl);
}

/* ===== 上传弹窗样式 ===== */
.priv-upload-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-surface-sand);
  margin-bottom: var(--space-lg);
}

.priv-tab-item {
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

.priv-tab-item:hover {
  color: var(--color-primary);
}

.priv-tab-item.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.priv-upload-content {
  min-height: 300px;
}

.priv-upload-dragger {
  border: 2px dashed var(--color-surface-sand);
  border-radius: var(--radius-card);
  padding: var(--space-xl) var(--space-lg);
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-normal);
  background: var(--color-bg);
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.priv-upload-dragger:hover {
  border-color: var(--color-primary);
  background: rgba(230, 0, 35, 0.01);
}

.priv-dragger-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}

.priv-dragger-illustration {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(67, 94, 229, 0.08), rgba(230, 0, 35, 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
}

.priv-dragger-icon {
  font-size: 36px;
  color: var(--color-primary);
}

.priv-dragger-text {
  font-size: 15px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  margin: 0;
}

.priv-dragger-formats {
  display: flex;
  gap: var(--space-sm);
}

.priv-format-tag {
  padding: 2px 12px;
  border-radius: var(--radius-base);
  border: 1px solid var(--color-surface-sand);
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg-warm);
}

.priv-upload-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
}

.priv-preview-card {
  position: relative;
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  max-width: 500px;
  width: 100%;
}

.priv-preview-img {
  width: 100%;
  height: auto;
  display: block;
  max-height: 400px;
  object-fit: contain;
  background: var(--color-bg-warm);
}

.priv-preview-overlay {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
}

.priv-upload-actions {
  display: flex;
  gap: var(--space-md);
}

.priv-url-area {
  border: 2px dashed var(--color-surface-sand);
  border-radius: var(--radius-card);
  padding: var(--space-xl) var(--space-lg);
  text-align: center;
  background: var(--color-bg);
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
}

.priv-url-illustration {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(67, 94, 229, 0.08), rgba(230, 0, 35, 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
}

.priv-url-icon {
  font-size: 36px;
  color: var(--color-primary);
}

.priv-url-desc {
  font-size: 15px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  margin: 0;
}

.priv-url-input-row {
  display: flex;
  gap: var(--space-sm);
  max-width: 480px;
  width: 100%;
}

.priv-url-input-row .ant-input-affix-wrapper {
  flex: 1;
}

.priv-url-hint {
  font-size: 13px;
  color: var(--color-text-disabled);
  margin: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .space-header-content {
    flex-direction: column;
    text-align: center;
    padding: var(--space-xl) var(--space-lg);
  }
  .space-header-meta {
    justify-content: center;
  }
  .gallery-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: var(--space-sm);
  }
}
</style>
