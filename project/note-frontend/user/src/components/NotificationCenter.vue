<template>
  <div v-if="isLoggedIn" class="notification-center">
    <el-popover
      v-model:visible="panelVisible"
      placement="bottom-end"
      :width="popoverWidth"
      trigger="click"
      popper-class="notification-popper"
      @show="handlePanelOpen"
    >
      <template #reference>
        <el-badge :value="unreadBadge" :hidden="unreadCount === 0" :max="99" class="notification-badge">
          <el-button
            class="notification-trigger"
            text
            circle
            :aria-label="unreadCount ? `通知，${unreadCount} 条未读` : '通知'"
            title="通知"
          >
            <el-icon :size="20"><Bell /></el-icon>
          </el-button>
        </el-badge>
      </template>

      <section class="notification-panel" aria-label="通知中心">
        <header class="notification-panel__header">
          <div>
            <h2>通知</h2>
            <span v-if="unreadCount" class="notification-panel__unread">{{ unreadCount > 99 ? '99+' : unreadCount }} 条未读</span>
          </div>
          <div class="notification-panel__actions">
            <el-tooltip content="刷新" placement="top">
              <el-button
                text
                circle
                size="small"
                :loading="loading"
                aria-label="刷新通知"
                @click="refreshNotifications"
              >
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
            <el-button
              v-if="unreadCount > 0"
              link
              type="primary"
              size="small"
              :loading="markingAll"
              @click="handleMarkAllRead"
            >
              全部已读
            </el-button>
          </div>
        </header>

        <el-tabs v-model="activeType" class="notification-tabs" @tab-change="handleTypeChange">
          <el-tab-pane label="全部" name="ALL" />
          <el-tab-pane label="评论" name="COMMENT" />
          <el-tab-pane label="关注" name="FOLLOW" />
          <el-tab-pane label="点赞" name="LIKE" />
          <el-tab-pane label="系统" name="SYSTEM" />
        </el-tabs>

        <div class="notification-panel__body" v-loading="loading">
          <div v-if="errorMessage && !loading" class="notification-state notification-state--error">
            <el-icon><WarningFilled /></el-icon>
            <p>{{ errorMessage }}</p>
            <el-button type="primary" plain size="small" @click="refreshNotifications">重新加载</el-button>
          </div>

          <el-empty
            v-else-if="!loading && notifications.length === 0"
            :image-size="72"
            description="暂无通知"
          />

          <template v-else>
            <div
              v-for="item in notifications"
              :key="item.id"
              class="notification-item"
              :class="{ 'notification-item--unread': item.isRead === 0 }"
            >
              <button
                class="notification-item__main"
                type="button"
                :disabled="markingId === item.id || removingId === item.id"
                @click="handleNotificationClick(item)"
              >
                <el-avatar
                  :size="36"
                  :src="item.senderAvatar || undefined"
                  :class="['notification-item__avatar', `notification-item__avatar--${item.type.toLowerCase()}`]"
                >
                  <el-icon><component :is="notificationIcon(item.type)" /></el-icon>
                </el-avatar>

                <span class="notification-item__content">
                  <span class="notification-item__title-row">
                    <strong>{{ item.title }}</strong>
                    <span v-if="item.isRead === 0" class="notification-item__dot" aria-label="未读" />
                  </span>
                  <span v-if="item.content" class="notification-item__message">{{ item.content }}</span>
                  <span class="notification-item__time">{{ formatDate(item.createdAt) }}</span>
                </span>

                <el-icon v-if="canNavigate(item)" class="notification-item__arrow"><ArrowRight /></el-icon>
              </button>

              <el-tooltip content="移除这条通知" placement="top">
                <el-button
                  text
                  circle
                  size="small"
                  class="notification-item__remove"
                  :loading="removingId === item.id"
                  :disabled="markingId === item.id"
                  :aria-label="`移除通知：${item.title}`"
                  @click.stop="handleRemove(item)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>

            <div v-if="total > notifications.length" class="notification-panel__footer">
              <el-button link type="primary" size="small" :loading="loadingMore" @click="loadMore">
                加载更多
              </el-button>
            </div>
          </template>
        </div>
      </section>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Bell,
  ChatDotRound,
  CircleCheck,
  Delete,
  Refresh,
  Star,
  UserFilled,
  WarningFilled,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  removeNotification,
} from '@/api/note'
import { useUserStore } from '@/store/user'
import type { NotificationItem } from '@/types'
import { formatDate } from '@/utils/helpers'

const PAGE_SIZE = 15
const POLL_INTERVAL = 60_000

const router = useRouter()
const userStore = useUserStore()
const panelVisible = ref(false)
const activeType = ref('ALL')
const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)
const total = ref(0)
const currentPage = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const markingId = ref<number | null>(null)
const removingId = ref<number | null>(null)
const markingAll = ref(false)
const errorMessage = ref('')
let pollTimer: ReturnType<typeof setInterval> | undefined
let latestListRequest = 0

const isLoggedIn = computed(() => userStore.isLoggedIn)
const unreadBadge = computed(() => unreadCount.value > 99 ? '99+' : unreadCount.value)
const popoverWidth = computed(() => window.innerWidth <= 480 ? Math.min(window.innerWidth - 24, 360) : 380)

function notificationIcon(type: NotificationItem['type']) {
  switch (type) {
    case 'COMMENT':
      return ChatDotRound
    case 'FOLLOW':
      return UserFilled
    case 'LIKE':
      return Star
    case 'SYSTEM':
      return CircleCheck
    default:
      return Bell
  }
}

function canNavigate(item: NotificationItem): boolean {
  return Boolean(item.targetId && (item.targetType === 'NOTE' || item.targetType === 'USER'))
}

async function fetchUnreadCount(showError = false) {
  if (!isLoggedIn.value) {
    unreadCount.value = 0
    return
  }

  try {
    const res = await getUnreadNotificationCount()
    unreadCount.value = Number(res.data) || 0
  } catch (error) {
    if (showError) {
      ElMessage.error(error instanceof Error ? error.message : '获取未读通知失败')
    }
  }
}

async function fetchNotifications(options: { append?: boolean; showError?: boolean } = {}) {
  if (!isLoggedIn.value) return

  const { append = false, showError = false } = options
  const requestId = ++latestListRequest
  const page = append ? currentPage.value + 1 : 1

  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
    errorMessage.value = ''
  }

  try {
    const res = await getNotifications({
      type: activeType.value === 'ALL' ? undefined : activeType.value,
      page,
      size: PAGE_SIZE,
    })

    if (requestId !== latestListRequest) return

    const data = res.data
    const list = data?.list || []
    notifications.value = append ? [...notifications.value, ...list] : list
    total.value = data?.total || 0
    currentPage.value = data?.page || page
  } catch (error) {
    if (requestId !== latestListRequest) return

    if (!append) {
      notifications.value = []
      total.value = 0
      errorMessage.value = '通知加载失败，请稍后重试'
    }
    if (showError) {
      ElMessage.error(error instanceof Error ? error.message : '通知加载失败')
    }
  } finally {
    if (requestId === latestListRequest) {
      loading.value = false
      loadingMore.value = false
    }
  }
}

async function refreshNotifications() {
  await Promise.all([
    fetchNotifications({ showError: true }),
    fetchUnreadCount(),
  ])
}

async function handlePanelOpen() {
  await refreshNotifications()
}

async function handleTypeChange() {
  await fetchNotifications({ showError: true })
}

async function loadMore() {
  if (loading.value || loadingMore.value || notifications.value.length >= total.value) return
  await fetchNotifications({ append: true, showError: true })
}

async function handleMarkAllRead() {
  if (markingAll.value || unreadCount.value === 0) return

  markingAll.value = true
  try {
    await markAllNotificationsAsRead()
    notifications.value = notifications.value.map(item => ({ ...item, isRead: 1 }))
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '标记失败，请稍后重试')
  } finally {
    markingAll.value = false
  }
}

async function markAsRead(item: NotificationItem): Promise<boolean> {
  if (item.isRead !== 0) return true

  markingId.value = item.id
  try {
    await markNotificationAsRead(item.id)
    item.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    return true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '标记已读失败')
    return false
  } finally {
    markingId.value = null
  }
}

async function handleRemove(item: NotificationItem) {
  if (removingId.value !== null || markingId.value === item.id) return

  removingId.value = item.id
  const wasUnread = item.isRead === 0
  try {
    await removeNotification(item.id)
    notifications.value = notifications.value.filter(notification => notification.id !== item.id)
    total.value = Math.max(0, total.value - 1)
    if (wasUnread) {
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    ElMessage.success('通知已移除')

    // 当前页删空后尝试重新拉取，避免分页停留在空白页。
    if (notifications.value.length === 0 && total.value > 0) {
      await fetchNotifications()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除通知失败，请稍后重试')
  } finally {
    removingId.value = null
  }
}

async function handleNotificationClick(item: NotificationItem) {
  const readSuccessfully = await markAsRead(item)
  if (!readSuccessfully || !canNavigate(item)) return

  panelVisible.value = false
  const path = item.targetType === 'NOTE' ? `/note/${item.targetId}` : `/user/${item.targetId}`
  if (router.currentRoute.value.path !== path) {
    router.push(path)
  }
}

function startPolling() {
  stopPolling()
  if (!isLoggedIn.value) return

  fetchUnreadCount()
  pollTimer = setInterval(() => {
    if (document.visibilityState === 'visible') {
      fetchUnreadCount()
      if (panelVisible.value) {
        fetchNotifications()
      }
    }
  }, POLL_INTERVAL)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
}

watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    startPolling()
  } else {
    stopPolling()
    panelVisible.value = false
    notifications.value = []
    unreadCount.value = 0
    total.value = 0
    errorMessage.value = ''
  }
}, { immediate: true })

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.notification-center {
  display: inline-flex;
  align-items: center;
}

.notification-badge :deep(.el-badge__content) {
  border: 2px solid var(--el-bg-color, #fff);
  font-size: 10px;
  font-weight: 600;
  min-width: 17px;
  height: 17px;
  line-height: 13px;
  padding: 0 4px;
  top: 6px;
  right: 5px;
}

.notification-trigger {
  color: var(--el-text-color-regular);
}

.notification-trigger:hover,
.notification-trigger:focus-visible {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.notification-panel {
  max-height: min(70vh, 600px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.notification-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 2px 8px;
}

.notification-panel__header > div:first-child {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.notification-panel__header h2 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 16px;
  line-height: 24px;
}

.notification-panel__unread {
  color: var(--el-color-primary);
  font-size: 12px;
  white-space: nowrap;
}

.notification-panel__actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.notification-tabs {
  flex: 0 0 auto;
}

.notification-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.notification-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--el-border-color-lighter);
}

.notification-tabs :deep(.el-tabs__item) {
  height: 34px;
  padding: 0 8px;
  font-size: 13px;
}

.notification-panel__body {
  min-height: 164px;
  max-height: min(50vh, 420px);
  overflow-y: auto;
  overscroll-behavior: contain;
}

.notification-state {
  min-height: 164px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.notification-state--error .el-icon {
  color: var(--el-color-warning);
  font-size: 28px;
}

.notification-state p {
  margin: 0;
  font-size: 13px;
}

.notification-item {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  padding: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: transparent;
  color: inherit;
  text-align: left;
  transition: background-color 0.16s ease;
}

.notification-item:hover,
.notification-item:focus-within {
  background: var(--el-fill-color-light);
}

.notification-item--unread {
  background: var(--el-color-primary-light-9);
}

.notification-item--unread:hover,
.notification-item--unread:focus-within {
  background: var(--el-color-primary-light-8);
}

.notification-item__main {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 0 12px 4px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.notification-item__main:hover,
.notification-item__main:focus-visible {
  outline: none;
}

.notification-item__main:disabled {
  cursor: wait;
}

.notification-item__remove {
  flex: 0 0 auto;
  align-self: center;
  margin-right: 1px;
  color: var(--el-text-color-placeholder);
}

.notification-item__remove:hover,
.notification-item__remove:focus-visible {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.notification-item__avatar {
  flex: 0 0 auto;
  color: #fff;
}

.notification-item__avatar--comment {
  background: var(--el-color-primary);
}

.notification-item__avatar--follow {
  background: var(--el-color-success);
}

.notification-item__avatar--like {
  background: var(--el-color-danger);
}

.notification-item__avatar--system {
  background: var(--el-color-warning);
}

.notification-item__content {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  gap: 3px;
}

.notification-item__title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.notification-item__title-row strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-item__dot {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--el-color-danger);
}

.notification-item__message {
  display: -webkit-box;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notification-item__time {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  line-height: 16px;
}

.notification-item__arrow {
  align-self: center;
  flex: 0 0 auto;
  color: var(--el-text-color-placeholder);
  font-size: 14px;
}

.notification-panel__footer {
  display: flex;
  justify-content: center;
  padding: 8px 0 2px;
}

@media (max-width: 480px) {
  .notification-tabs :deep(.el-tabs__item) {
    padding: 0 6px;
    font-size: 12px;
  }

  .notification-panel__body {
    max-height: min(56vh, 420px);
  }
}
</style>

<style>
.notification-popper.el-popover {
  max-width: calc(100vw - 24px);
  padding: 12px;
}
</style>
