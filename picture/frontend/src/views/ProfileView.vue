<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  UserOutlined,
  EditOutlined,
  CameraOutlined,
  GlobalOutlined,
  LockOutlined,
  CalendarOutlined,
  HeartFilled,
  DeleteOutlined,
  FormOutlined,
  PlusOutlined,
  CloudOutlined,
  UploadOutlined,
  CloseCircleFilled,
  CloudUploadOutlined,
  LinkOutlined,
  InboxOutlined,
  TeamOutlined,
  EyeOutlined,
  CrownOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { updateUserVoUsingPost } from '@/api/userController'
import {
  listPictureVoByPageUsingPost,
  deletePictureUsingPost,
  editPictureUsingPost,
  uploadPictureUsingPost,
  uploadUrlPictureUsingPost,
} from '@/api/pictureController'
import {
  listSpaceVoByPageUsingPost,
  addSpaceUsingPost,
  editSpaceUsingPost,
  listSpaceLevelUsingGet,
  getSpaceVoByIdUsingGet,
} from '@/api/spaceController'
import {
  getLikeCountUsingGet,
  getLikedPictureListUsingPost,
} from '@/api/pictureLikeController'
import {
  fileUploadUsingPost,
  uploadByUrlUsingPost,
} from '@/api/fileController'
import {
  listMyTeamSpaceUsingPost,
  listSpaceUserUsingPost,
  deleteSpaceUserUsingPost,
} from '@/api/spaceUserController'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const user = computed(() => userStore.loginUser)

// ===== 编辑信息 =====
const editModalOpen = ref(false)
const editForm = ref({
  userName: '',
  userAvatar: '',
  userProfile: '',
})
const editLoading = ref(false)
const avatarMode = ref<'url' | 'upload'>('url')
const avatarUploading = ref(false)
const avatarFileList = ref<any[]>([])

function openEditModal() {
  editForm.value = {
    userName: user.value?.userName || '',
    userAvatar: user.value?.userAvatar || '',
    userProfile: user.value?.userProfile || '',
  }
  avatarMode.value = 'upload'
  avatarFileList.value = []
  editModalOpen.value = true
}

async function handleAvatarUpload(info: any) {
  const file = info.file?.originFileObj || info.file
  if (!file) return
  avatarUploading.value = true
  try {
    const res: any = await uploadPictureUsingPost({ picName: 'avatar' }, {}, file)
    if (res?.data) {
      editForm.value.userAvatar = res.data.url
      avatarFileList.value = []
      message.success('头像上传成功')
    }
  } catch {
    message.error('上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

function clearAvatar() {
  editForm.value.userAvatar = ''
  avatarFileList.value = []
}

async function handleSaveProfile() {
  editLoading.value = true
  try {
    await updateUserVoUsingPost({
      id: user.value?.id,
      userName: editForm.value.userName,
      userAvatar: editForm.value.userAvatar,
      userProfile: editForm.value.userProfile,
    })
    message.success('资料更新成功')
    editModalOpen.value = false
    await userStore.fetchLoginUser()
  } catch {
    // request 拦截器已处理错误提示
  } finally {
    editLoading.value = false
  }
}

// ===== 图库切换 =====
type GalleryTab = 'public' | 'liked' | 'private' | 'team'
const activeTab = ref<GalleryTab>('public')

// ===== 公共图库 =====
const publicPictures = ref<API.PictureVO[]>([])
const publicLoading = ref(false)
const publicCurrent = ref(1)
const publicPageSize = ref(12)
const publicTotal = ref(0)

async function fetchPublicPictures() {
  if (!user.value?.id) return
  publicLoading.value = true
  try {
    const res: any = await listPictureVoByPageUsingPost({
      current: publicCurrent.value,
      pageSize: publicPageSize.value,
      nullSpaceId: true,
      userId: user.value.id,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res?.data) {
      publicPictures.value = res.data.records || []
      publicTotal.value = res.data.total || 0
      if (publicPictures.value.length) {
        fetchLikeCounts(publicPictures.value)
      }
    }
  } catch {
    // 静默
  } finally {
    publicLoading.value = false
  }
}

function handlePublicPageChange(page: number, size: number) {
  publicCurrent.value = page
  publicPageSize.value = size
  fetchPublicPictures()
}

// ===== 喜欢的图片 =====
const likedPictures = ref<API.PictureVO[]>([])
const likedLoading = ref(false)
const likedCurrent = ref(1)
const likedPageSize = ref(12)
const likedTotal = ref(0)

async function fetchLikedPictures() {
  likedLoading.value = true
  try {
    const res: any = await getLikedPictureListUsingPost({
      current: likedCurrent.value,
      pageSize: likedPageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res?.data) {
      likedPictures.value = res.data.records || []
      likedTotal.value = res.data.total || 0
      if (likedPictures.value.length) {
        fetchLikeCounts(likedPictures.value)
      }
    }
  } catch {
    // 静默
  } finally {
    likedLoading.value = false
  }
}

function handleLikedPageChange(page: number, size: number) {
  likedCurrent.value = page
  likedPageSize.value = size
  fetchLikedPictures()
}

// ===== 私有图库 =====
const privateSpace = ref<API.SpaceVO | null>(null)
const privatePictures = ref<API.PictureVO[]>([])
const privateLoading = ref(false)
const privateCurrent = ref(1)
const privatePageSize = ref(12)
const privateTotal = ref(0)

async function fetchPrivateSpace() {
  if (!user.value?.id) return
  try {
    const res: any = await listSpaceVoByPageUsingPost({
      userId: user.value.id,
      spaceType: 0,
      current: 1,
      pageSize: 1,
    })
    if (res?.data?.records?.length) {
      privateSpace.value = res.data.records[0]
    }
  } catch {
    // 静默
  }
}

async function fetchPrivatePictures() {
  if (!privateSpace.value?.id) return
  privateLoading.value = true
  try {
    const res: any = await listPictureVoByPageUsingPost({
      current: privateCurrent.value,
      pageSize: privatePageSize.value,
      spaceId: privateSpace.value.id,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res?.data) {
      privatePictures.value = res.data.records || []
      privateTotal.value = res.data.total || 0
    }
  } catch {
    // 静默
  } finally {
    privateLoading.value = false
  }
}

function handlePrivatePageChange(page: number, size: number) {
  privateCurrent.value = page
  privatePageSize.value = size
  fetchPrivatePictures()
}

// ===== 创建空间 =====
const spaceLevels = ref<API.SpaceLevel[]>([])
const createSpaceForm = ref({
  spaceName: '',
  spaceLevel: 0,
  spaceAvatar: '',
})
const createSpaceLoading = ref(false)
const spaceAvatarMode = ref<'upload' | 'url'>('upload')
const spaceAvatarUploading = ref(false)
const spaceAvatarFileList = ref<any[]>([])

async function handleSpaceAvatarUpload(info: any) {
  const file = info.file?.originFileObj || info.file
  if (!file) return
  spaceAvatarUploading.value = true
  try {
    const res: any = await fileUploadUsingPost({}, file)
    if (res?.data) {
      createSpaceForm.value.spaceAvatar = res.data
      spaceAvatarFileList.value = []
      message.success('空间头像上传成功')
    }
  } catch {
    message.error('上传失败，请重试')
  } finally {
    spaceAvatarUploading.value = false
  }
}

async function handleSpaceAvatarUrlUpload() {
  const url = createSpaceForm.value.spaceAvatar.trim()
  if (!url) {
    message.warning('请输入头像 URL')
    return
  }
  if (!/^https?:\/\/.+/i.test(url)) {
    message.warning('请输入有效的 http/https 链接')
    return
  }
  spaceAvatarUploading.value = true
  try {
    const res: any = await uploadByUrlUsingPost({ url })
    if (res?.data) {
      createSpaceForm.value.spaceAvatar = res.data
      message.success('头像上传成功')
    }
  } catch {
    message.error('上传失败，请重试')
  } finally {
    spaceAvatarUploading.value = false
  }
}

function clearSpaceAvatar() {
  createSpaceForm.value.spaceAvatar = ''
  spaceAvatarFileList.value = []
}

async function fetchSpaceLevels() {
  try {
    const res: any = await listSpaceLevelUsingGet()
    if (res?.data) {
      spaceLevels.value = res.data
    }
  } catch {
    // 静默
  }
}

async function handleCreateSpace() {
  if (!createSpaceForm.value.spaceName.trim()) {
    message.warning('请输入空间名称')
    return
  }
  createSpaceLoading.value = true
  try {
    const res: any = await addSpaceUsingPost({
      spaceName: createSpaceForm.value.spaceName,
      spaceLevel: createSpaceForm.value.spaceLevel,
      spaceType: 0, // 私有空间
      avatar: createSpaceForm.value.spaceAvatar || undefined,
    })
    if (res?.data) {
      message.success('空间创建成功')
      await fetchPrivateSpace()
      if (privateSpace.value) {
        fetchPrivatePictures()
      }
    }
  } catch {
    // 拦截器已处理
  } finally {
    createSpaceLoading.value = false
  }
}

// ===== 删除图片 =====
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
        if (activeTab.value === 'public') {
          fetchPublicPictures()
        } else {
          fetchPrivatePictures()
          fetchPrivateSpace()
        }
      } catch {
        // 拦截器已处理
      }
    },
  })
}

// ===== 编辑图片 =====
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
    if (activeTab.value === 'public') {
      fetchPublicPictures()
    } else {
      fetchPrivatePictures()
      fetchPrivateSpace()
    }
  } catch {
    // 拦截器已处理
  } finally {
    editPicLoading.value = false
  }
}

// ===== Tab 切换 =====
watch(activeTab, (tab) => {
  if (tab === 'public') {
    publicCurrent.value = 1
    fetchPublicPictures()
  } else if (tab === 'liked') {
    likedCurrent.value = 1
    fetchLikedPictures()
  } else if (tab === 'team') {
    fetchTeamSpaces()
  } else {
    privateCurrent.value = 1
    if (privateSpace.value) {
      fetchPrivatePictures()
    }
  }
})

// ===== 点击图片 =====
function handlePictureClick(pic: API.PictureVO) {
  router.push(`/picture/${String(pic.id)}?from=${activeTab.value}`)
}

// ===== 判断是否自己的图片 =====
function isOwnPicture(pic: API.PictureVO) {
  return pic.userId && user.value?.id && String(pic.userId) === String(user.value.id)
}

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
  for (const r of results) {
    if (r.status === 'fulfilled') {
      likeCountMap.value[r.value.id] = r.value.count
    }
  }
}

function getPicLikeCount(pic: API.PictureVO) {
  return likeCountMap.value[String(pic.id)] ?? 0
}

// ===== 格式化日期 =====
function formatDate(dateStr?: string) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

// ===== 空间用量 =====
function formatSize(bytes?: number) {
  if (!bytes) return '0'
  const mb = bytes / 1024 / 1024
  if (mb < 1024) return mb.toFixed(1) + ' MB'
  return (mb / 1024).toFixed(2) + ' GB'
}

function usagePercent(used?: number, max?: number) {
  if (!used || !max) return 0
  return Math.min(100, Math.round((used / max) * 100))
}

function showTotal(t: number) {
  return `共 ${t} 张`
}

function getSpaceLevelLabel(level: number) {
  const labels: Record<number, string> = { 0: '普通版', 1: '专业版', 2: '旗舰版' }
  return labels[level] ?? '普通版'
}

// ===== 升级空间弹窗 =====
const upgradeModalOpen = ref(false)

// ===== 编辑空间名称 =====
const editSpaceModalOpen = ref(false)
const editSpaceName = ref('')
const editSpaceLoading = ref(false)

function openEditSpaceModal() {
  editSpaceName.value = privateSpace.value?.spaceName || ''
  editSpaceModalOpen.value = true
}

async function handleSaveSpaceName() {
  if (!editSpaceName.value.trim()) {
    message.warning('请输入空间名称')
    return
  }
  editSpaceLoading.value = true
  try {
    await editSpaceUsingPost({
      id: privateSpace.value?.id,
      spaceName: editSpaceName.value.trim(),
    })
    message.success('空间名称已更新')
    editSpaceModalOpen.value = false
    await fetchPrivateSpace()
  } catch {
    // 拦截器已处理
  } finally {
    editSpaceLoading.value = false
  }
}

// ===== 私有空间上传 =====
const privateUploadOpen = ref(false)
const privateUploadTab = ref<'file' | 'url'>('file')
const privateUploading = ref(false)
const privateFileList = ref<any[]>([])
const privatePreviewUrl = ref('')
const privateUrlInput = ref('')
const privateUrlUploading = ref(false)

function openPrivateUpload() {
  privateUploadTab.value = 'file'
  privateUploading.value = false
  privateFileList.value = []
  privatePreviewUrl.value = ''
  privateUrlInput.value = ''
  privateUrlUploading.value = false
  privateUploadOpen.value = true
}

function handlePrivateFileChange(info: any) {
  const file = info.file?.originFileObj || info.file
  if (!file) return
  privateFileList.value = [info.file]
  const reader = new FileReader()
  reader.onload = (e) => {
    privatePreviewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
  // 自动上传
  privateAutoUpload(file)
}

async function privateAutoUpload(file: File) {
  if (!privateSpace.value?.id) return
  privateUploading.value = true
  try {
    const res: any = await uploadPictureUsingPost({ spaceId: privateSpace.value.id as any }, {}, file)
    if (res?.data?.id) {
      message.success('上传成功')
      localStorage.setItem('pending_edit_picture_id', String(res.data.id))
      localStorage.setItem('pending_edit_space_id', String(privateSpace.value.id))
      privateUploadOpen.value = false
      router.push(`/picture/edit/${res.data.id}?from=space&spaceId=${privateSpace.value.id}`)
    }
  } catch {
    // 拦截器已处理
  } finally {
    privateUploading.value = false
  }
}

function handlePrivateDrop(e: DragEvent) {
  e.preventDefault()
  const file = e.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('image/')) {
    privateFileList.value = [{ originFileObj: file, name: file.name }]
    const reader = new FileReader()
    reader.onload = (ev) => {
      privatePreviewUrl.value = ev.target?.result as string
    }
    reader.readAsDataURL(file)
    privateAutoUpload(file)
  }
}

function clearPrivateFile() {
  privateFileList.value = []
  privatePreviewUrl.value = ''
}

async function handlePrivateFileUpload() {
  const file = privateFileList.value[0]?.originFileObj || privateFileList.value[0]
  if (!file || !privateSpace.value?.id) return
  privateUploading.value = true
  try {
    const res: any = await uploadPictureUsingPost({ spaceId: privateSpace.value.id as any }, {}, file)
    if (res?.data?.id) {
      message.success('上传成功')
      localStorage.setItem('pending_edit_picture_id', String(res.data.id))
      localStorage.setItem('pending_edit_space_id', String(privateSpace.value.id))
      privateUploadOpen.value = false
      router.push(`/picture/edit/${res.data.id}?from=space&spaceId=${privateSpace.value.id}`)
    }
  } catch {
    // 拦截器已处理
  } finally {
    privateUploading.value = false
  }
}

async function handlePrivateUrlUpload() {
  const url = privateUrlInput.value.trim()
  if (!url) {
    message.warning('请输入图片 URL 地址')
    return
  }
  if (!/^https?:\/\/.+/i.test(url)) {
    message.warning('请输入有效的 http/https 图片链接')
    return
  }
  if (!privateSpace.value?.id) return
  privateUrlUploading.value = true
  try {
    const res: any = await uploadUrlPictureUsingPost({ fileUrl: url, spaceId: privateSpace.value.id as any })
    if (res?.data?.id) {
      message.success('抓取成功')
      localStorage.setItem('pending_edit_picture_id', String(res.data.id))
      localStorage.setItem('pending_edit_space_id', String(privateSpace.value.id))
      privateUploadOpen.value = false
      router.push(`/picture/edit/${res.data.id}?from=space&spaceId=${privateSpace.value.id}`)
    }
  } catch {
    // 拦截器已处理
  } finally {
    privateUrlUploading.value = false
  }
}

// ===== 团队空间 =====
const teamSpaces = ref<API.SpaceUserVO[]>([])
const teamLoading = ref(false)
const createTeamModalOpen = ref(false)
const createTeamForm = ref({
  spaceName: '',
  spaceLevel: 0,
  spaceAvatar: '',
})
const createTeamLoading = ref(false)
const teamAvatarMode = ref<'upload' | 'url'>('upload')
const teamAvatarUploading = ref(false)
const teamAvatarFileList = ref<any[]>([])

async function fetchTeamSpaces() {
  teamLoading.value = true
  try {
    const res: any = await listMyTeamSpaceUsingPost()
    if (res?.data) {
      teamSpaces.value = res.data
    }
  } catch {
    // 静默
  } finally {
    teamLoading.value = false
  }
}

function openCreateTeamModal() {
  createTeamForm.value = { spaceName: '', spaceLevel: 0, spaceAvatar: '' }
  teamAvatarMode.value = 'upload'
  teamAvatarFileList.value = []
  createTeamModalOpen.value = true
}

async function handleTeamAvatarUpload(info: any) {
  const file = info.file?.originFileObj || info.file
  if (!file) return
  teamAvatarUploading.value = true
  try {
    const res: any = await fileUploadUsingPost({}, file)
    if (res?.data) {
      createTeamForm.value.spaceAvatar = res.data
      teamAvatarFileList.value = []
      message.success('头像上传成功')
    }
  } catch {
    message.error('上传失败，请重试')
  } finally {
    teamAvatarUploading.value = false
  }
}

async function handleTeamAvatarUrlUpload() {
  const url = createTeamForm.value.spaceAvatar.trim()
  if (!url || !/^https?:\/\/.+/i.test(url)) {
    message.warning('请输入有效的 http/https 链接')
    return
  }
  teamAvatarUploading.value = true
  try {
    const res: any = await uploadByUrlUsingPost({ url })
    if (res?.data) {
      createTeamForm.value.spaceAvatar = res.data
      message.success('头像上传成功')
    }
  } catch {
    message.error('上传失败，请重试')
  } finally {
    teamAvatarUploading.value = false
  }
}

function clearTeamAvatar() {
  createTeamForm.value.spaceAvatar = ''
  teamAvatarFileList.value = []
}

async function handleCreateTeamSpace() {
  if (!createTeamForm.value.spaceName.trim()) {
    message.warning('请输入空间名称')
    return
  }
  createTeamLoading.value = true
  try {
    const res: any = await addSpaceUsingPost({
      spaceName: createTeamForm.value.spaceName,
      spaceLevel: createTeamForm.value.spaceLevel,
      spaceType: 1, // 团队空间
      avatar: createTeamForm.value.spaceAvatar || undefined,
    })
    if (res?.data) {
      message.success('团队空间创建成功')
      createTeamModalOpen.value = false
      fetchTeamSpaces()
    }
  } catch {
    // 拦截器已处理
  } finally {
    createTeamLoading.value = false
  }
}

function handleTeamSpaceClick(spaceUserVO: API.SpaceUserVO) {
  const sid = spaceUserVO.spaceId || spaceUserVO.space?.id
  if (sid) {
    router.push(`/space/${String(sid)}`)
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  // 根据 URL query 恢复 tab
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery === 'public' || tabFromQuery === 'liked' || tabFromQuery === 'private' || tabFromQuery === 'team') {
    activeTab.value = tabFromQuery
  }
  await Promise.all([fetchPrivateSpace(), fetchSpaceLevels()])
  // 根据当前 tab 加载对应数据
  if (activeTab.value === 'liked') {
    fetchLikedPictures()
  } else if (activeTab.value === 'private') {
    if (privateSpace.value) fetchPrivatePictures()
  } else if (activeTab.value === 'team') {
    fetchTeamSpaces()
  } else {
    fetchPublicPictures()
  }
})
</script>

<template>
  <div class="profile-page">
    <!-- ===== 封面横幅 ===== -->
    <div class="profile-banner">
      <div class="banner-ink-bg"></div>
      <div class="banner-overlay"></div>
      <div class="banner-content">
        <div class="avatar-wrap">
          <a-avatar :size="96" :src="user?.userAvatar" class="profile-avatar">
            <template v-if="!user?.userAvatar" #icon><UserOutlined style="font-size: 44px" /></template>
          </a-avatar>
          <button class="avatar-edit-dot" title="编辑资料" @click="openEditModal">
            <EditOutlined />
          </button>
        </div>
        <div class="banner-info">
          <h1 class="profile-name">{{ user?.userName || '未命名用户' }}</h1>
          <div class="profile-meta">
            <span class="meta-chip"><UserOutlined /> {{ user?.userAccount || '—' }}</span>
            <span class="meta-chip"><CalendarOutlined /> {{ formatDate(user?.createTime) }}</span>
          </div>
          <p v-if="user?.userProfile" class="profile-bio">{{ user.userProfile }}</p>
          <p v-else class="profile-bio muted">这个人很懒，什么都没有写~</p>
        </div>
      </div>
    </div>

    <!-- ===== Tab 导航 ===== -->
    <div class="tab-bar">
      <button
        class="tab-item"
        :class="{ active: activeTab === 'public' }"
        @click="activeTab = 'public'"
      >
        <GlobalOutlined /> 作品
        <span v-if="publicTotal" class="tab-count">{{ publicTotal }}</span>
      </button>
      <button
        class="tab-item"
        :class="{ active: activeTab === 'liked' }"
        @click="activeTab = 'liked'"
      >
        <HeartFilled /> 喜欢
        <span v-if="likedTotal" class="tab-count">{{ likedTotal }}</span>
      </button>
      <button
        class="tab-item"
        :class="{ active: activeTab === 'private' }"
        @click="activeTab = 'private'"
      >
        <LockOutlined /> 私有图库
        <span v-if="privateTotal" class="tab-count">{{ privateTotal }}</span>
      </button>
      <button
        class="tab-item"
        :class="{ active: activeTab === 'team' }"
        @click="activeTab = 'team'"
      >
        <TeamOutlined /> 团队空间
        <span v-if="teamSpaces.length" class="tab-count">{{ teamSpaces.length }}</span>
      </button>
    </div>

    <!-- ===== 公共图库 ===== -->
    <div v-show="activeTab === 'public'" class="gallery-container">
      <div class="gallery-toolbar">
        <a-button type="primary" @click="router.push('/picture/upload')">
          <UploadOutlined /> 上传图片
        </a-button>
      </div>
      <a-spin :spinning="publicLoading">
        <div v-if="publicPictures.length > 0" class="gallery-grid">
          <div
            v-for="pic in publicPictures"
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
              <!-- 审核状态角标 -->
              <div v-if="pic.reviewStatus !== undefined && pic.reviewStatus !== 1" class="review-badge" :class="pic.reviewStatus === 0 ? 'pending' : 'rejected'">
                {{ pic.reviewStatus === 0 ? '待审核' : '未通过' }}
              </div>
              <!-- 悬浮操作层 -->
              <div v-if="isOwnPicture(pic)" class="pic-overlay">
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
                <span class="pic-like"><HeartFilled class="like-icon" /> {{ getPicLikeCount(pic) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else-if="!publicLoading" class="empty-state">
          <a-empty description="暂未上传公共图片" />
        </div>
      </a-spin>
      <div v-if="publicTotal > 0" class="gallery-pagination">
        <a-pagination
          v-model:current="publicCurrent"
          v-model:page-size="publicPageSize"
          :total="publicTotal"
          :page-size-options="['12', '20', '40']"
          show-size-changer
          show-quick-jumper
          :show-total="showTotal"
          @change="handlePublicPageChange"
          @show-size-change="handlePublicPageChange"
        />
      </div>
    </div>

    <!-- ===== 喜欢的图片 ===== -->
    <div v-show="activeTab === 'liked'" class="gallery-container">
      <a-spin :spinning="likedLoading">
        <div v-if="likedPictures.length > 0" class="gallery-grid">
          <div
            v-for="pic in likedPictures"
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
        <div v-else-if="!likedLoading" class="empty-state">
          <a-empty description="还没有点赞任何图片，去发现喜欢的图片吧" />
        </div>
      </a-spin>
      <div v-if="likedTotal > 0" class="gallery-pagination">
        <a-pagination
          v-model:current="likedCurrent"
          v-model:page-size="likedPageSize"
          :total="likedTotal"
          :page-size-options="['12', '20', '40']"
          show-size-changer
          show-quick-jumper
          :show-total="showTotal"
          @change="handleLikedPageChange"
          @show-size-change="handleLikedPageChange"
        />
      </div>
    </div>

    <!-- ===== 私有图库 ===== -->
    <div v-show="activeTab === 'private'" class="gallery-container">
      <!-- 没有空间 → 创建引导 -->
      <div v-if="!privateSpace" class="create-space-section">
        <div class="create-space-card">
          <div class="create-space-icon">
            <CloudOutlined />
          </div>
          <h3 class="create-space-title">创建你的私有图库</h3>
          <p class="create-space-desc">给你的图片一个安全的家，专属于你的私密空间</p>
          <div class="create-space-form">
            <a-input
              v-model:value="createSpaceForm.spaceName"
              placeholder="空间名称"
              size="large"
              :maxlength="30"
              class="space-name-input"
            />
            <!-- 空间头像上传 -->
            <div class="space-avatar-section">
              <label class="space-avatar-label">空间头像</label>
              <div class="space-avatar-field">
                <div v-if="spaceAvatarMode === 'upload'" class="space-avatar-upload-wrap">
                  <a-upload
                    :show-upload-list="false"
                    :before-upload="() => false"
                    accept="image/*"
                    :file-list="spaceAvatarFileList"
                    @change="handleSpaceAvatarUpload"
                  >
                    <a-button :loading="spaceAvatarUploading">
                      <CameraOutlined /> {{ spaceAvatarUploading ? '上传中...' : '选择图片' }}
                    </a-button>
                  </a-upload>
                </div>
                <div v-else class="space-avatar-url-wrap">
                  <a-input
                    v-model:value="createSpaceForm.spaceAvatar"
                    placeholder="输入头像 URL"
                    @press-enter="handleSpaceAvatarUrlUpload"
                  >
                    <template #prefix><LinkOutlined /></template>
                  </a-input>
                  <a-button :loading="spaceAvatarUploading" @click="handleSpaceAvatarUrlUpload">确认</a-button>
                </div>
                <a-button
                  size="small"
                  type="link"
                  @click="spaceAvatarMode = spaceAvatarMode === 'upload' ? 'url' : 'upload'"
                >
                  {{ spaceAvatarMode === 'upload' ? '切换为URL' : '切换为上传' }}
                </a-button>
              </div>
              <div v-if="createSpaceForm.spaceAvatar" class="space-avatar-preview">
                <div class="space-avatar-preview-img-wrap">
                  <a-avatar :size="56" :src="createSpaceForm.spaceAvatar">
                    <template #icon><CloudOutlined /></template>
                  </a-avatar>
                  <button class="avatar-remove-btn" title="移除头像" @click="clearSpaceAvatar">
                    <CloseCircleFilled />
                  </button>
                </div>
                <span class="preview-label">头像预览</span>
              </div>
            </div>
            <div class="space-level-options">
              <div
                v-for="level in spaceLevels"
                :key="level.value"
                class="level-card"
                :class="{ selected: createSpaceForm.spaceLevel === level.value }"
                @click="createSpaceForm.spaceLevel = level.value ?? 0"
              >
                <div class="level-name">{{ level.text || getSpaceLevelLabel(level.value ?? 0) }}</div>
                <div class="level-detail">
                  {{ level.maxCount ?? '—' }} 张 · {{ formatSize(level.maxSize) }}
                </div>
              </div>
              <!-- 如果后端没返回级别列表，显示默认选项 -->
              <template v-if="spaceLevels.length === 0">
                <div
                  v-for="lv in [{ value: 0, text: '普通版', count: 100, size: '100 MB' }, { value: 1, text: '专业版', count: 500, size: '500 MB' }, { value: 2, text: '旗舰版', count: 1000, size: '1 GB' }]"
                  :key="lv.value"
                  class="level-card"
                  :class="{ selected: createSpaceForm.spaceLevel === lv.value }"
                  @click="createSpaceForm.spaceLevel = lv.value"
                >
                  <div class="level-name">{{ lv.text }}</div>
                  <div class="level-detail">{{ lv.count }} 张 · {{ lv.size }}</div>
                </div>
              </template>
            </div>
            <!-- 选了非普通版，显示联系二维码 -->
            <div v-if="createSpaceForm.spaceLevel > 0" class="upgrade-qr-section">
              <p class="upgrade-hint">非普通版空间需联系管理员开通</p>
              <img :src="'/wechat-qrcode.png'" alt="微信二维码" class="upgrade-qrcode" />
              <p class="upgrade-tip">扫码添加微信，备注「空间升级」</p>
            </div>
            <a-button
              v-else
              type="primary"
              size="large"
              block
              :loading="createSpaceLoading"
              class="create-space-btn"
              @click="handleCreateSpace"
            >
              <PlusOutlined /> 创建空间
            </a-button>
          </div>
        </div>
      </div>

      <!-- 有空间 -->
      <template v-else>
        <!-- 空间状态栏 -->
        <div class="space-status-bar">
          <div class="space-header">
            <span class="space-name-label">{{ privateSpace.spaceName || '我的空间' }}</span>
            <a-tag color="red">{{ getSpaceLevelLabel(privateSpace.spaceLevel ?? 0) }}</a-tag>
            <a-button type="link" size="small" @click="openEditSpaceModal"><EditOutlined /> 编辑</a-button>
          </div>
          <div class="space-usage-bars">
            <div class="usage-item">
              <span class="usage-label">图片数量</span>
              <a-progress
                :percent="usagePercent(privateSpace.totalCount, privateSpace.maxCount)"
                :stroke-color="{ from: '#e60023', to: '#ff6b6b' }"
                size="small"
              />
              <span class="usage-text">{{ privateSpace.totalCount || 0 }} / {{ privateSpace.maxCount || '∞' }}</span>
            </div>
            <div class="usage-item">
              <span class="usage-label">存储空间</span>
              <a-progress
                :percent="usagePercent(privateSpace.totalSize, privateSpace.maxSize)"
                :stroke-color="{ from: '#e60023', to: '#ff6b6b' }"
                size="small"
              />
              <span class="usage-text">{{ formatSize(privateSpace.totalSize) }} / {{ formatSize(privateSpace.maxSize) }}</span>
            </div>
          </div>
        </div>

        <div class="gallery-toolbar">
          <a-button type="primary" @click="openPrivateUpload">
            <UploadOutlined /> 上传图片
          </a-button>
          <a-button @click="upgradeModalOpen = true">升级空间</a-button>
        </div>

        <a-spin :spinning="privateLoading">
          <div v-if="privatePictures.length > 0" class="gallery-grid">
            <div
              v-for="pic in privatePictures"
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
                <div class="pic-overlay">
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
          <div v-else-if="!privateLoading" class="empty-state">
            <a-empty description="暂无私有图片，上传你的第一张图片吧" />
          </div>
        </a-spin>

        <div v-if="privateTotal > 0" class="gallery-pagination">
          <a-pagination
            v-model:current="privateCurrent"
            v-model:page-size="privatePageSize"
            :total="privateTotal"
            :page-size-options="['12', '20', '40']"
            show-size-changer
            show-quick-jumper
            :show-total="showTotal"
            @change="handlePrivatePageChange"
            @show-size-change="handlePrivatePageChange"
          />
        </div>
      </template>
    </div>

    <!-- ===== 团队空间 ===== -->
    <div v-show="activeTab === 'team'" class="gallery-container">
      <div class="gallery-toolbar">
        <a-button type="primary" @click="openCreateTeamModal">
          <PlusOutlined /> 新建团队空间
        </a-button>
      </div>
      <a-spin :spinning="teamLoading">
        <div v-if="teamSpaces.length > 0" class="team-space-grid">
          <div
            v-for="item in teamSpaces"
            :key="item.id"
            class="team-space-card"
            @click="handleTeamSpaceClick(item)"
          >
            <div class="team-space-avatar">
              <a-avatar :size="64" :src="item.space?.avatar">
                <template #icon><TeamOutlined style="font-size: 28px" /></template>
              </a-avatar>
            </div>
            <div class="team-space-info">
              <h4 class="team-space-name">{{ item.space?.spaceName || '未命名团队' }}</h4>
              <div class="team-space-meta">
                <a-tooltip :title="item.spaceRole === 'admin' ? '管理员' : item.spaceRole === 'editor' ? '编辑者' : '查看者'">
                  <span class="team-space-role-icon">
                    <CrownOutlined v-if="item.spaceRole === 'admin'" style="color: #faad14" />
                    <EditOutlined v-else-if="item.spaceRole === 'editor'" style="color: #1890ff" />
                    <EyeOutlined v-else style="color: #8c8c8c" />
                  </span>
                </a-tooltip>
                <span class="team-space-time">{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else-if="!teamLoading" class="empty-state">
          <a-empty description="暂未加入任何团队空间">
            <a-button type="primary" @click="openCreateTeamModal">
              <PlusOutlined /> 创建一个团队空间
            </a-button>
          </a-empty>
        </div>
      </a-spin>
    </div>

    <!-- ===== 创建团队空间弹窗 ===== -->
    <a-modal
      v-model:open="createTeamModalOpen"
      title="创建团队空间"
      :confirm-loading="createTeamLoading"
      ok-text="创建"
      cancel-text="取消"
      @ok="handleCreateTeamSpace"
      :width="520"
    >
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="团队名称">
          <a-input
            v-model:value="createTeamForm.spaceName"
            placeholder="输入团队空间名称"
            :maxlength="30"
            show-count
          />
        </a-form-item>
        <a-form-item label="团队头像">
          <div class="space-avatar-field">
            <div v-if="teamAvatarMode === 'upload'" class="space-avatar-upload-wrap">
              <a-upload
                :show-upload-list="false"
                :before-upload="() => false"
                accept="image/*"
                :file-list="teamAvatarFileList"
                @change="handleTeamAvatarUpload"
              >
                <a-button :loading="teamAvatarUploading">
                  <CameraOutlined /> {{ teamAvatarUploading ? '上传中...' : '选择图片' }}
                </a-button>
              </a-upload>
            </div>
            <div v-else class="space-avatar-url-wrap">
              <a-input
                v-model:value="createTeamForm.spaceAvatar"
                placeholder="输入头像 URL"
                @press-enter="handleTeamAvatarUrlUpload"
              >
                <template #prefix><LinkOutlined /></template>
              </a-input>
              <a-button :loading="teamAvatarUploading" @click="handleTeamAvatarUrlUpload">确认</a-button>
            </div>
            <a-button
              size="small"
              type="link"
              @click="teamAvatarMode = teamAvatarMode === 'upload' ? 'url' : 'upload'"
            >
              {{ teamAvatarMode === 'upload' ? '切换为URL' : '切换为上传' }}
            </a-button>
          </div>
          <div v-if="createTeamForm.spaceAvatar" class="space-avatar-preview">
            <div class="space-avatar-preview-img-wrap">
              <a-avatar :size="56" :src="createTeamForm.spaceAvatar">
                <template #icon><TeamOutlined /></template>
              </a-avatar>
              <button class="avatar-remove-btn" title="移除头像" @click="clearTeamAvatar">
                <CloseCircleFilled />
              </button>
            </div>
            <span class="preview-label">头像预览</span>
          </div>
        </a-form-item>
        <a-form-item label="空间等级">
          <div class="space-level-options">
            <div
              v-for="level in spaceLevels"
              :key="level.value"
              class="level-card"
              :class="{ selected: createTeamForm.spaceLevel === level.value }"
              @click="createTeamForm.spaceLevel = level.value ?? 0"
            >
              <div class="level-name">{{ level.text || getSpaceLevelLabel(level.value ?? 0) }}</div>
              <div class="level-detail">
                {{ level.maxCount ?? '—' }} 张 · {{ formatSize(level.maxSize) }}
              </div>
            </div>
            <template v-if="spaceLevels.length === 0">
              <div
                v-for="lv in [{ value: 0, text: '普通版', count: 100, size: '100 MB' }, { value: 1, text: '专业版', count: 500, size: '500 MB' }, { value: 2, text: '旗舰版', count: 1000, size: '1 GB' }]"
                :key="lv.value"
                class="level-card"
                :class="{ selected: createTeamForm.spaceLevel === lv.value }"
                @click="createTeamForm.spaceLevel = lv.value"
              >
                <div class="level-name">{{ lv.text }}</div>
                <div class="level-detail">{{ lv.count }} 张 · {{ lv.size }}</div>
              </div>
            </template>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ===== 编辑资料弹窗 ===== -->
    <a-modal
      v-model:open="editModalOpen"
      title="编辑个人资料"
      :confirm-loading="editLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSaveProfile"
      :width="480"
    >
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="昵称">
          <a-input
            v-model:value="editForm.userName"
            placeholder="输入昵称"
            :maxlength="20"
            show-count
          />
        </a-form-item>
        <a-form-item label="头像">
          <div class="avatar-field">
            <div v-if="avatarMode === 'url'" class="avatar-input-wrap">
              <a-input v-model:value="editForm.userAvatar" placeholder="输入头像 URL">
                <template #prefix><CameraOutlined /></template>
              </a-input>
            </div>
            <div v-else class="avatar-upload-wrap">
              <a-upload
                :show-upload-list="false"
                :before-upload="() => false"
                accept="image/*"
                :file-list="avatarFileList"
                @change="handleAvatarUpload"
              >
                <a-button :loading="avatarUploading">
                  <CameraOutlined /> {{ avatarUploading ? '上传中...' : '选择图片' }}
                </a-button>
              </a-upload>
            </div>
            <a-button
              size="small"
              type="link"
              @click="avatarMode = avatarMode === 'url' ? 'upload' : 'url'"
            >
              {{ avatarMode === 'url' ? '切换为上传' : '切换为URL' }}
            </a-button>
          </div>
          <div v-if="editForm.userAvatar" class="avatar-preview-card">
            <div class="avatar-preview-img-wrap">
              <a-avatar :size="64" :src="editForm.userAvatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <button class="avatar-remove-btn" title="移除头像" @click="clearAvatar">
                <CloseCircleFilled />
              </button>
            </div>
            <span class="preview-label">头像预览</span>
          </div>
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="editForm.userProfile"
            placeholder="介绍一下自己吧"
            :maxlength="200"
            show-count
            :auto-size="{ minRows: 3, maxRows: 6 }"
          />
        </a-form-item>
      </a-form>
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

    <!-- ===== 升级空间弹窗 ===== -->
    <a-modal
      v-model:open="upgradeModalOpen"
      title="升级空间"
      :footer="null"
      :width="400"
    >
      <div class="upgrade-modal-body">
        <p class="upgrade-hint">如需升级空间额度，请联系管理员</p>
        <img :src="'/wechat-qrcode.png'" alt="微信二维码" class="upgrade-qrcode" />
        <p class="upgrade-tip">扫码添加微信，备注「空间升级」</p>
      </div>
    </a-modal>

    <!-- ===== 编辑空间名称弹窗 ===== -->
    <a-modal
      v-model:open="editSpaceModalOpen"
      title="编辑空间"
      :confirm-loading="editSpaceLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSaveSpaceName"
      :width="400"
    >
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="空间名称">
          <a-input v-model:value="editSpaceName" placeholder="请输入空间名称" :maxlength="30" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ===== 私有空间上传弹窗 ===== -->
    <a-modal
      v-model:open="privateUploadOpen"
      title="上传到私有空间"
      :footer="null"
      :width="680"
      :destroy-on-close="true"
    >
      <!-- Tab 切换 -->
      <div class="priv-upload-tabs">
        <div
          class="priv-tab-item"
          :class="{ active: privateUploadTab === 'file' }"
          @click="privateUploadTab = 'file'"
        >
          <CloudUploadOutlined />
          <span>上传图片</span>
        </div>
        <div
          class="priv-tab-item"
          :class="{ active: privateUploadTab === 'url' }"
          @click="privateUploadTab = 'url'"
        >
          <LinkOutlined />
          <span>链接上传</span>
        </div>
      </div>

      <!-- 文件上传区域 -->
      <div v-if="privateUploadTab === 'file'" class="priv-upload-content">
        <div
          v-if="!privatePreviewUrl"
          class="priv-upload-dragger"
          @dragover.prevent
          @drop="handlePrivateDrop"
        >
          <a-upload
            :file-list="privateFileList"
            :before-upload="() => false"
            :show-upload-list="false"
            accept="image/jpeg,image/png,image/gif,image/webp"
            @change="handlePrivateFileChange"
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

        <!-- 预览 + 操作 -->
        <div v-else class="priv-upload-preview">
          <div class="priv-preview-card">
            <img :src="privatePreviewUrl" class="priv-preview-img" alt="预览" />
            <div class="priv-preview-overlay">
              <a-button size="small" danger @click="clearPrivateFile">移除</a-button>
            </div>
          </div>
          <div class="priv-upload-actions">
            <a-button
              type="primary"
              size="large"
              :loading="privateUploading"
              @click="handlePrivateFileUpload"
            >
              {{ privateUploading ? '上传中...' : '确认上传' }}
            </a-button>
          </div>
        </div>
      </div>

      <!-- URL 上传区域 -->
      <div v-else class="priv-upload-content">
        <div class="priv-url-area">
          <div class="priv-url-illustration">
            <LinkOutlined class="priv-url-icon" />
          </div>
          <p class="priv-url-desc">通过图片链接上传到私有空间</p>
          <div class="priv-url-input-row">
            <a-input
              v-model:value="privateUrlInput"
              size="large"
              placeholder="请粘贴或输入图片 URL 地址..."
              @press-enter="handlePrivateUrlUpload"
            >
              <template #prefix><LinkOutlined style="color: var(--color-text-disabled)" /></template>
            </a-input>
            <a-button
              type="primary"
              size="large"
              :loading="privateUrlUploading"
              @click="handlePrivateUrlUpload"
            >
              {{ privateUrlUploading ? '抓取中...' : '立即抓取' }}
            </a-button>
          </div>
          <p class="priv-url-hint">支持 http/https 协议的公开图片地址</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: var(--space-3xl);
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

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.profile-avatar {
  border: 4px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 20px rgba(33, 25, 34, 0.12);
  background: var(--color-surface-sand);
}

.avatar-edit-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  border: 2px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  transition: transform var(--transition-fast), background var(--transition-fast);
  box-shadow: 0 2px 8px rgba(230, 0, 35, 0.3);
}

.avatar-edit-dot:hover {
  transform: scale(1.1);
  background: var(--color-primary-hover);
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

/* ===== Tab 导航 ===== */
.tab-bar {
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

/* ===== 图库容器 ===== */
.gallery-container {
  min-height: 300px;
}

.gallery-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 0 var(--space-sm) var(--space-md);
}

/* ===== 图片卡片网格 ===== */
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

/* 审核状态角标 */
.review-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  z-index: 2;
  pointer-events: none;
}
.review-badge.pending {
  background: #fa8c16;
}
.review-badge.rejected {
  background: #f5222d;
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

/* 悬浮操作层 */
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

.pic-like {
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

/* ===== 升级弹窗 ===== */
.upgrade-modal-body {
  text-align: center;
  padding: var(--space-md) 0;
}

.upgrade-qr-section {
  text-align: center;
  padding: var(--space-md) 0;
}

.upgrade-hint {
  font-size: 15px;
  color: var(--color-text-primary);
  margin-bottom: var(--space-lg);
}

.upgrade-qrcode {
  width: 200px;
  height: 200px;
  object-fit: contain;
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
}

.upgrade-tip {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: var(--space-md);
}

/* ===== 空状态 ===== */
.empty-state {
  padding: var(--space-3xl) 0;
  text-align: center;
}

/* ===== 分页 ===== */
.gallery-pagination {
  display: flex;
  justify-content: center;
  padding: var(--space-xl) 0;
}

/* ===== 创建空间 ===== */
.create-space-section {
  display: flex;
  justify-content: center;
  padding: var(--space-xl) var(--space-sm);
}

.create-space-card {
  width: 100%;
  max-width: 520px;
  text-align: center;
  padding: var(--space-2xl) var(--space-xl);
  background: var(--color-bg);
  border: 2px dashed var(--color-surface-sand);
  border-radius: var(--radius-card);
  transition: border-color var(--transition-normal);
}

.create-space-card:hover {
  border-color: var(--color-border-hover);
}

.create-space-icon {
  font-size: 48px;
  color: var(--color-text-disabled);
  margin-bottom: var(--space-md);
}

.create-space-title {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-sm);
  letter-spacing: 0.04em;
}

.create-space-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-xl);
}

.create-space-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.space-name-input {
  border-radius: var(--radius-button);
}

.space-level-options {
  display: flex;
  gap: var(--space-sm);
}

.level-card {
  flex: 1;
  padding: var(--space-md) var(--space-sm);
  border: 2px solid var(--color-surface-sand);
  border-radius: var(--radius-button);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: center;
}

.level-card:hover {
  border-color: var(--color-border-hover);
}

.level-card.selected {
  border-color: var(--color-primary);
  background: rgba(230, 0, 35, 0.03);
}

.level-name {
  font-size: 14px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.level-detail {
  font-size: 11px;
  color: var(--color-text-disabled);
}

.create-space-btn {
  border-radius: var(--radius-button);
  height: 44px;
  font-size: 15px;
  font-weight: var(--weight-semibold);
  letter-spacing: 0.04em;
}

/* ===== 空间状态栏 ===== */
.space-status-bar {
  background: var(--color-bg);
  border: 1px solid var(--color-surface-sand);
  border-radius: var(--radius-card);
  padding: var(--space-md) var(--space-lg);
  margin: 0 var(--space-sm) var(--space-lg);
}

.space-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}

.space-name-label {
  font-size: 15px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
}

.space-usage-bars {
  display: flex;
  gap: var(--space-xl);
}

.usage-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.usage-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  min-width: 52px;
}

.usage-item :deep(.ant-progress) {
  flex: 1;
}

.usage-text {
  font-size: 11px;
  color: var(--color-text-disabled);
  white-space: nowrap;
}

/* ===== 编辑弹窗 ===== */
.avatar-field {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.avatar-input-wrap {
  flex: 1;
}

.avatar-upload-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.avatar-preview-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-bg-warm);
  border-radius: var(--radius-base);
  border: 1px solid var(--color-surface-sand);
}

.avatar-preview-img-wrap {
  position: relative;
  flex-shrink: 0;
}

.avatar-remove-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  color: var(--color-text-disabled);
  font-size: 18px;
  line-height: 1;
  transition: color var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-remove-btn:hover {
  color: var(--color-primary);
}

.preview-label {
  font-size: 12px;
  color: var(--color-text-disabled);
}

/* ===== 团队空间卡片 ===== */
.team-space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-md);
  padding: 0 var(--space-sm);
}

.team-space-card {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-bg);
  border: 1px solid var(--color-surface-sand);
  border-radius: var(--radius-card);
  cursor: pointer;
  transition:
    transform var(--transition-normal),
    box-shadow var(--transition-normal);
}

.team-space-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(33, 25, 34, 0.1);
}

.team-space-avatar .ant-avatar {
  background: linear-gradient(135deg, rgba(230, 0, 35, 0.08), rgba(67, 94, 229, 0.08));
  border: 2px solid var(--color-surface-sand);
}

.team-space-info {
  flex: 1;
  min-width: 0;
}

.team-space-name {
  font-size: 15px;
  font-weight: var(--weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-space-meta {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  font-size: 12px;
  color: var(--color-text-secondary);
}

.team-space-role-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: default;
}

.team-space-time {
  color: var(--color-text-disabled);
}

/* ===== 空间头像上传 ===== */
.space-avatar-section {
  text-align: left;
}

.space-avatar-label {
  font-size: 14px;
  font-weight: var(--weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
  display: block;
}

.space-avatar-field {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.space-avatar-url-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex: 1;
}

.space-avatar-preview {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-bg-warm);
  border-radius: var(--radius-base);
  border: 1px solid var(--color-surface-sand);
}

.space-avatar-preview-img-wrap {
  position: relative;
  flex-shrink: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .banner-content {
    flex-direction: column;
    text-align: center;
    padding: var(--space-xl) var(--space-lg);
  }
  .profile-meta {
    justify-content: center;
  }
  .gallery-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: var(--space-sm);
    padding: 0 var(--space-sm);
  }
  .space-level-options {
    flex-direction: column;
  }
  .space-usage-bars {
    flex-direction: column;
    gap: var(--space-sm);
  }
}

/* ===== 私有空间上传弹窗样式 ===== */
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
</style>
