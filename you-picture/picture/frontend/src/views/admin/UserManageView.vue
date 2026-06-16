<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  CameraOutlined,
} from '@ant-design/icons-vue'
import {
  listUserVoByPageUsingPost,
  addUserUsingPost,
  updateUserUsingPost,
  deleteUserUsingPost,
} from '@/api/userController'
import { fileUploadUsingPost } from '@/api/fileController'
import { h } from 'vue'

// ===== 列表数据 =====
const tableData = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
  userAccount: '',
  userName: '',
  userRole: undefined,
})

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 80,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    width: 150,
  },
  {
    title: '昵称',
    dataIndex: 'userName',
    width: 150,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    ellipsis: true,
  },
  {
    title: '角色',
    dataIndex: 'userRole',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 160,
    fixed: 'right' as const,
  },
]

async function fetchUsers() {
  loading.value = true
  try {
    const res: any = await listUserVoByPageUsingPost(searchParams)
    if (res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchParams.current = 1
  fetchUsers()
}

function handleReset() {
  searchParams.userAccount = ''
  searchParams.userName = ''
  searchParams.userRole = undefined
  searchParams.current = 1
  fetchUsers()
}

function handlePageChange(page: number, pageSize: number) {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchUsers()
}

// ===== 新增 / 编辑 Modal =====
const modalVisible = ref(false)
const modalTitle = ref('新增用户')
const modalLoading = ref(false)
const adminAvatarMode = ref<'url' | 'upload'>('url')
const adminAvatarUploading = ref(false)
const formState = reactive<API.UserAddRequest & { id?: number }>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

function openAddModal() {
  modalTitle.value = '新增用户'
  Object.assign(formState, {
    id: undefined,
    userAccount: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
  })
  adminAvatarMode.value = 'url'
  modalVisible.value = true
}

function openEditModal(record: API.UserVO) {
  modalTitle.value = '编辑用户'
  Object.assign(formState, {
    id: record.id,
    userAccount: record.userAccount || '',
    userName: record.userName || '',
    userAvatar: record.userAvatar || '',
    userProfile: record.userProfile || '',
    userRole: record.userRole || 'user',
  })
  adminAvatarMode.value = 'url'
  modalVisible.value = true
}

async function handleAdminAvatarUpload(info: any) {
  const file = info.file?.originFileObj || info.file
  if (!file) return
  adminAvatarUploading.value = true
  try {
    const res: any = await fileUploadUsingPost({}, file)
    if (res?.data) {
      formState.userAvatar = res.data
      message.success('头像上传成功')
    }
  } catch {
    message.error('上传失败，请重试')
  } finally {
    adminAvatarUploading.value = false
  }
}

async function handleModalOk() {
  modalLoading.value = true
  try {
    if (formState.id) {
      // 编辑
      await updateUserUsingPost({
        id: formState.id,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
        userRole: formState.userRole,
      })
      message.success('更新成功')
    } else {
      // 新增
      await addUserUsingPost({
        userAccount: formState.userAccount,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
        userRole: formState.userRole,
      })
      message.success('添加成功')
    }
    modalVisible.value = false
    fetchUsers()
  } catch {
    // 拦截器已处理
  } finally {
    modalLoading.value = false
  }
}

// ===== 删除 =====
function handleDelete(record: API.UserVO) {
  Modal.confirm({
    title: '确认删除',
    icon: h(ExclamationCircleOutlined),
    content: `确定要删除用户「${record.userName || record.userAccount}」吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteUserUsingPost({ id: record.id })
        message.success('删除成功')
        fetchUsers()
      } catch {
        // 拦截器已处理
      }
    },
  })
}

onMounted(() => {
  fetchUsers()
})

function formatTime(dateStr?: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
</script>

<template>
  <div class="user-manage-page">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <p class="page-desc">管理系统中的所有用户账号</p>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <a-space wrap>
        <a-input
          v-model:value="searchParams.userAccount"
          placeholder="搜索账号"
          allow-clear
          style="width: 180px"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-input
          v-model:value="searchParams.userName"
          placeholder="搜索昵称"
          allow-clear
          style="width: 180px"
        />
        <a-select
          v-model:value="searchParams.userRole"
          placeholder="选择角色"
          allow-clear
          style="width: 140px"
        >
          <a-select-option value="user">普通用户</a-select-option>
          <a-select-option value="admin">管理员</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">
          <SearchOutlined /> 搜索
        </a-button>
        <a-button @click="handleReset">重置</a-button>
      </a-space>
      <a-button type="primary" @click="openAddModal">
        <PlusOutlined /> 新增用户
      </a-button>
    </div>

    <!-- 用户表格 -->
    <div class="table-section">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="{
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`,
          onChange: handlePageChange,
        }"
        :scroll="{ x: 950 }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-avatar
              :src="record.userAvatar"
              :size="36"
              style="background: var(--color-surface-sand)"
            >
              {{ (record.userName || '?')[0] }}
            </a-avatar>
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <a-tag :color="record.userRole === 'admin' ? 'red' : 'default'">
              {{ record.userRole === 'admin' ? '管理员' : '用户' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑 Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      :width="520"
    >
      <a-form :model="formState" layout="vertical" style="margin-top: 16px">
        <a-form-item label="账号" v-if="!formState.id">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
        </a-form-item>
        <a-form-item label="昵称">
          <a-input v-model:value="formState.userName" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item label="头像">
          <div class="avatar-field">
            <div v-if="adminAvatarMode === 'url'" class="avatar-input-wrap">
              <a-input v-model:value="formState.userAvatar" placeholder="请输入头像URL" />
            </div>
            <div v-else class="avatar-upload-wrap">
              <a-upload
                :show-upload-list="false"
                :before-upload="() => false"
                accept="image/*"
                @change="handleAdminAvatarUpload"
              >
                <a-button :loading="adminAvatarUploading">
                  <CameraOutlined /> {{ adminAvatarUploading ? '上传中...' : '选择图片' }}
                </a-button>
              </a-upload>
              <span v-if="formState.userAvatar" class="upload-done-text">已上传</span>
            </div>
            <a-button
              size="small"
              type="link"
              @click="adminAvatarMode = adminAvatarMode === 'url' ? 'upload' : 'url'"
            >
              {{ adminAvatarMode === 'url' ? '切换为上传' : '切换为URL' }}
            </a-button>
          </div>
          <div v-if="formState.userAvatar" class="avatar-preview">
            <a-avatar :size="40" :src="formState.userAvatar">
              {{ (formState.userName || '?')[0] }}
            </a-avatar>
            <span class="preview-label">预览</span>
          </div>
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea v-model:value="formState.userProfile" placeholder="请输入个人简介" :rows="3" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="formState.userRole">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.user-manage-page {
  padding: var(--space-md) 0;
}

.page-header {
  margin-bottom: var(--space-lg);
}

.page-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.search-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-bg);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
}

.table-section {
  background: var(--color-bg);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-surface-sand);
  padding: var(--space-md);
  overflow: hidden;
}

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

.upload-done-text {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.avatar-preview {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
}

.preview-label {
  font-size: 12px;
  color: var(--color-text-disabled);
}
</style>
