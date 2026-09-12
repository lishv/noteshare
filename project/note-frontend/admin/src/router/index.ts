import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/views/layout/index.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '首页仪表盘' } },
        { path: 'users', name: 'Users', component: () => import('@/views/users/index.vue'), meta: { title: '用户管理' } },
        { path: 'notes', name: 'Notes', component: () => import('@/views/notes/index.vue'), meta: { title: '笔记管理' } },
        { path: 'comments', name: 'Comments', component: () => import('@/views/comments/index.vue'), meta: { title: '评论管理' } },
        { path: 'notifications', name: 'Notifications', component: () => import('@/views/notifications/index.vue'), meta: { title: '用户消息' } },
        { path: 'links', name: 'Links', component: () => import('@/views/links/index.vue'), meta: { title: '总链接管理' } },
        { path: 'words', name: 'Words', component: () => import('@/views/words/index.vue'), meta: { title: '敏感词管理' } },
        { path: 'stats', name: 'Stats', component: () => import('@/views/stats/index.vue'), meta: { title: '数据统计' } },
        { path: 'logs', name: 'Logs', component: () => import('@/views/logs/index.vue'), meta: { title: '操作日志' } },
        { path: 'admins', name: 'Admins', component: () => import('@/views/admins/index.vue'), meta: { title: '管理员管理', superOnly: true } },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || ''} - 管理后台`
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
