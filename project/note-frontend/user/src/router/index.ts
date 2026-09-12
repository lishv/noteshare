import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
    },
    {
      path: '/settings',
      name: 'Settings',
      component: () => import('@/views/Settings.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/user/:id',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
    },
    {
      path: '/favorites',
      name: 'Favorites',
      component: () => import('@/views/Favorites.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/likes',
      name: 'Likes',
      component: () => import('@/views/Likes.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/note/editor',
      name: 'NoteEditor',
      component: () => import('@/views/Editor.vue'),
      meta: { requiresAuth: true, title: '创作笔记' },
    },
    {
      path: '/note/editor/:id',
      name: 'NoteEdit',
      component: () => import('@/views/Editor.vue'),
      meta: { requiresAuth: true, title: '编辑笔记' },
    },
    {
      path: '/note/:id',
      name: 'NoteDetail',
      component: () => import('@/views/NoteDetail.vue'),
      meta: { title: '笔记详情' },
    },
  ],
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
