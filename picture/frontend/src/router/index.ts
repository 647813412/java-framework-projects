import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/BasicLayout.vue'),
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'picture/upload',
          name: 'PictureUpload',
          component: () => import('@/views/PictureUploadView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'picture/edit/:id',
          name: 'PictureEdit',
          component: () => import('@/views/PictureEditView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'picture/:id',
          name: 'PictureDetail',
          component: () => import('@/views/PictureDetailView.vue'),
        },
        {
          path: 'user/:id',
          name: 'UserProfile',
          component: () => import('@/views/UserProfileView.vue'),
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/ProfileView.vue'),
        },
        {
          path: 'space/:id',
          name: 'SpaceDetail',
          component: () => import('@/views/SpaceDetailView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'search',
          name: 'Search',
          component: () => import('@/views/SearchView.vue'),
        },
        {
          path: 'ai/image',
          name: 'AiImage',
          component: () => import('@/views/AiImageView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'team-spaces',
          name: 'TeamSpaces',
          component: () => import('@/views/TeamSpacesView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'admin/user-manage',
          name: 'AdminUserManage',
          component: () => import('@/views/admin/UserManageView.vue'),
          meta: { requiresAdmin: true },
        },
        {
          path: 'admin/picture-manage',
          name: 'AdminPictureManage',
          component: () => import('@/views/admin/PictureManageView.vue'),
          meta: { requiresAdmin: true },
        },
        {
          path: 'admin/space-manage',
          name: 'AdminSpaceManage',
          component: () => import('@/views/admin/SpaceManageView.vue'),
          meta: { requiresAdmin: true },
        },

      ],
    },
  ],
})

// 全局前置守卫
router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 尝试获取登录状态（仅首次），超时 5 秒防止白屏
  if (!userStore.userFetched) {
    await Promise.race([
      userStore.fetchLoginUser(),
      new Promise<void>((resolve) => setTimeout(resolve, 5000)),
    ])
  }

  // 管理员页面权限校验
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { path: '/' }
  }

  // 需要登录的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login' }
  }

  // 已登录用户不需要访问 login/register
  if (to.meta.guest && userStore.isLoggedIn) {
    return { path: '/' }
  }
})

export default router
