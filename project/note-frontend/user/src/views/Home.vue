<template>
  <div class="app-container">
    <!-- 顶部操作区 -->
    <header class="top-bar">
      <div class="top-bar-left">
        <h1 class="logo" @click="handleLogoClick" title="返回首页">笔记共享平台</h1>
        <div class="search-group">
          <el-input
            v-model="authorKeyword"
            placeholder="搜索作者姓名..."
            class="search-input search-author"
            clearable
            :prefix-icon="UserIcon"
            @keyup.enter="handleAuthorSearch"
            @clear="handleClearAuthorSearch"
          />
          <el-input
            v-model="noteKeyword"
            placeholder="搜索笔记名称或内容..."
            class="search-input search-note"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleNoteSearch"
            @clear="handleClearNoteSearch"
          />
        </div>
      </div>
      <div class="top-bar-right">
        <template v-if="isLoggedIn">
          <el-button type="primary" @click="showPublishDialog = true">
            <el-icon><Plus /></el-icon>
            发布笔记
          </el-button>
          <el-dropdown @command="handleUserCommand">
            <span class="user-avatar">
              <el-avatar :size="32" :src="userInfo?.avatar || defaultAvatar">
                {{ userInfo?.username?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username-text">{{ userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="myNotes">我的笔记</el-dropdown-item>
                <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                <el-dropdown-item command="settings">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </header>

    <!-- 移动端菜单按钮 -->
    <el-button
      class="mobile-menu-btn"
      :icon="Menu"
      circle
      @click="drawerVisible = true"
    />

    <div class="main-content">
      <!-- 左侧导航栏（支持工作台、热门标签、作者列表） -->
      <aside
        class="sidebar"
        :class="{ 'drawer-open': drawerVisible, 'collapsed': isSidebarCollapsed }"
        v-show="!isMobile || drawerVisible"
      >
        <div class="sidebar-header">
          <div class="sidebar-title-group" v-if="!isSidebarCollapsed">
            <h3>{{ isSearching ? '搜索结果' : '导航与工作台' }}</h3>
          </div>
          <div class="sidebar-actions">
            <!-- 桌面端折叠切换按钮 -->
            <el-tooltip :content="isSidebarCollapsed ? '展开侧边栏' : '收起侧边栏'" placement="right">
              <el-button
                v-if="!isMobile"
                text
                circle
                size="small"
                class="collapse-btn"
                @click="toggleSidebarCollapse"
              >
                <el-icon :size="16"><Expand v-if="isSidebarCollapsed" /><Fold v-else /></el-icon>
              </el-button>
            </el-tooltip>
            <!-- 移动端关闭按钮 -->
            <el-button v-if="isMobile" text circle size="small" @click="drawerVisible = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="sidebar-scrollable">
          <!-- 1. 我的工作台 -->
          <div class="sidebar-section" v-if="!isSidebarCollapsed">
            <div class="sidebar-sec-title">我的工作台</div>
            <div class="workbench-nav">
              <div class="workbench-item" :class="{ active: viewMode === 'recommend' }" @click="switchToRecommend">
                <el-icon><Compass /></el-icon>
                <span>发现与推荐</span>
              </div>
              <div class="workbench-item" :class="{ active: viewMode === 'trending' }" @click="handleModeChange('trending')">
                <el-icon><TrendCharts /></el-icon>
                <span>全站热门榜</span>
              </div>
              <div class="workbench-item" :class="{ active: viewMode === 'following' }" @click="handleModeChange('following')">
                <el-icon><Connection /></el-icon>
                <span>关注动态流</span>
              </div>
              <div class="workbench-item" @click="$router.push('/favorites')">
                <el-icon><Star /></el-icon>
                <span>我的收藏夹</span>
              </div>
              <div class="workbench-item" @click="$router.push('/note/editor')">
                <el-icon><EditPen /></el-icon>
                <span>创作新笔记</span>
              </div>
            </div>
          </div>

          <!-- 折叠模式下的极简图标导航 -->
          <div class="sidebar-collapsed-nav" v-if="isSidebarCollapsed">
            <el-tooltip content="发现与推荐" placement="right">
              <div class="collapsed-icon-btn" :class="{ active: viewMode === 'recommend' }" @click="switchToRecommend">
                <el-icon :size="18"><Compass /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="全站热门榜" placement="right">
              <div class="collapsed-icon-btn" :class="{ active: viewMode === 'trending' }" @click="handleModeChange('trending')">
                <el-icon :size="18"><TrendCharts /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="关注动态流" placement="right">
              <div class="collapsed-icon-btn" :class="{ active: viewMode === 'following' }" @click="handleModeChange('following')">
                <el-icon :size="18"><Connection /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="我的收藏夹" placement="right">
              <div class="collapsed-icon-btn" @click="$router.push('/favorites')">
                <el-icon :size="18"><Star /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="创作新笔记" placement="right">
              <div class="collapsed-icon-btn" @click="$router.push('/note/editor')">
                <el-icon :size="18"><EditPen /></el-icon>
              </div>
            </el-tooltip>
          </div>

          <el-divider v-if="!isSidebarCollapsed" style="margin: 10px 0;" />

          <!-- 2. 热门偏好标签筛选 (基于真实热门标签，若无则不显示) -->
          <div class="sidebar-section" v-if="!isSidebarCollapsed && hotTopics.length > 0">
            <div class="sidebar-sec-title">
              <span>热门偏好标签</span>
              <el-button v-if="selectedTag" type="primary" link size="small" @click="clearTagFilter">清除</el-button>
            </div>
            <div class="tag-chips-wrap">
              <span
                v-for="t in hotTopics"
                :key="t.name"
                class="sidebar-tag-chip"
                :class="{ active: selectedTag === t.name }"
                @click="handleFilterByTag(t.name)"
              >
                #{{ t.name }}
              </span>
            </div>
          </div>

          <el-divider v-if="!isSidebarCollapsed && hotTopics.length > 0" style="margin: 10px 0;" />
          <div class="sidebar-section">
            <div class="sidebar-sec-title" v-if="!isSidebarCollapsed">
              <span>{{ isSearching ? '匹配创作者' : (isLoggedIn ? '我的关注创作者' : '活跃创作者') }}</span>
              <span class="user-count">({{ totalUsers }})</span>
            </div>

            <div class="user-list" v-loading="userLoading">
              <template v-for="user in userList" :key="user.id">
                <el-tooltip
                  v-if="isSidebarCollapsed"
                  :content="`${user.username} (${user.id === userInfo?.id ? '我 · ' : ''}${user.noteCount || 0}篇 · ${user.followerCount || 0}粉丝)`"
                  placement="right"
                >
                  <div
                    class="user-item collapsed-item"
                    :class="{ active: viewMode === 'authors' && selectedUserId === user.id }"
                    @click="selectUser(user)"
                  >
                    <el-avatar :size="32" :src="user.avatar || defaultAvatar">
                      {{ user.username.charAt(0) }}
                    </el-avatar>
                  </div>
                </el-tooltip>

                <div
                  v-else
                  class="user-item"
                  :class="{ active: viewMode === 'authors' && selectedUserId === user.id }"
                  @click="selectUser(user)"
                >
                  <el-avatar :size="34" :src="user.avatar || defaultAvatar">
                    {{ user.username.charAt(0) }}
                  </el-avatar>
                  <div class="user-info">
                    <div class="user-name" :title="user.username">
                      <span>{{ user.username }}</span>
                      <el-tag
                        v-if="user.id === userInfo?.id"
                        size="small"
                        type="primary"
                        effect="plain"
                        class="self-tag"
                      >我</el-tag>
                    </div>
                    <div class="user-meta">
                      <span>{{ user.noteCount || 0 }} 篇笔记</span>
                      <span class="meta-dot">·</span>
                      <span>{{ user.followerCount || 0 }} 粉丝</span>
                    </div>
                  </div>
                </div>
              </template>
              <el-empty
                v-if="!userLoading && userList.length === 0"
                :description="isSidebarCollapsed ? '' : (isSearching ? '未找到匹配的作者' : (isLoggedIn ? '暂无关注作者' : '暂无活跃作者'))"
              />
            </div>
          </div>
        </div>

        <!-- 创作者分页（仅展开时显示） -->
        <div class="pagination-wrapper" v-if="totalUsers > pageSize && !isSidebarCollapsed">
          <el-pagination
            small
            layout="prev, pager, next"
            :total="totalUsers"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handlePageChange"
          />
        </div>
      </aside>

      <!-- 移动端遮罩 -->
      <div v-if="drawerVisible && isMobile" class="drawer-mask" @click="drawerVisible = false" />

      <!-- 主内容区与右侧看板 -->
      <div class="content-wrapper">
        <main class="content-area" v-loading="contentLoading || socialLoading || recommendLoading">
          <!-- 顶部模式切换与换一批操作栏 -->
          <div class="content-mode-bar">
            <div class="mode-bar-left">
              <div class="content-mode-eyebrow">公共知识主页</div>
              <div class="content-mode-title">
                {{ viewModeTitle }}
              </div>
            </div>

            <div class="mode-bar-right">
              <el-radio-group v-model="viewMode" size="default" @change="handleModeChange">
                <el-radio-button label="recommend">
                  <el-icon><Compass /></el-icon>
                  智能推荐
                </el-radio-button>
                <el-radio-button label="trending">
                  <el-icon><TrendCharts /></el-icon>
                  热门榜
                </el-radio-button>
                <el-radio-button label="following">
                  <el-icon><Connection /></el-icon>
                  关注动态
                </el-radio-button>
                <el-radio-button label="authors">
                  <el-icon><UserIcon /></el-icon>
                  作者专栏
                </el-radio-button>
              </el-radio-group>

              <!-- 换一批 (一次刷新6条) 按钮 -->
              <el-button
                v-if="viewMode === 'recommend'"
                type="primary"
                plain
                :icon="Refresh"
                :loading="recommendLoading"
                class="refresh-batch-btn"
                @click="handleBatchRefresh"
              >
                🔄 换一批 (一次刷新6条)
              </el-button>
            </div>
          </div>

          <!-- ==================== 1. 公共智能推荐视图 (Default) ==================== -->
          <template v-if="viewMode === 'recommend'">
            <section class="recommend-section">
              <!-- 推荐状态栏与当前标签提示 -->
              <div class="recommend-banner" v-if="selectedTag">
                <div class="tag-filter-notice">
                  <span class="filter-label">当前筛选标签：</span>
                  <el-tag
                    closable
                    size="default"
                    type="primary"
                    effect="light"
                    @close="clearTagFilter"
                  >
                    # {{ selectedTag }}
                  </el-tag>
                  <span class="filter-hint">（已为您匹配该标签下的高热度推荐笔记）</span>
                </div>
                <el-button link type="primary" size="small" @click="clearTagFilter">查看全部推荐</el-button>
              </div>

              <!-- 6条卡片网格 -->
              <div v-if="recommendList.length" class="recommend-cards-grid">
                <article
                  v-for="row in recommendList"
                  :key="row.id"
                  class="recommend-note-card"
                  @click="openNoteDetail(row)"
                >
                  <!-- 顶部徽标栏 -->
                  <div class="card-badge-row">
                    <div class="heat-badge" title="热度值 = 浏览×1 + 点赞×3 + 收藏×5 + 评论×2">
                      <span class="heat-icon">🔥</span>
                      <span class="heat-score">热度 {{ Math.round(row.heatScore || calculateLocalHeat(row)) }}</span>
                    </div>

                    <el-tag
                      size="small"
                      :type="row.noteType === 2 ? 'success' : 'info'"
                      effect="plain"
                      class="type-pill"
                    >
                      {{ row.noteType === 2 ? 'Markdown 原创' : '外链精选' }}
                    </el-tag>
                  </div>

                  <!-- 笔记标题 -->
                  <h3 class="recommend-note-title" :title="row.title">
                    <el-tag v-if="row.isPinned === 1" type="warning" size="small" class="pinned-tag">置顶</el-tag>
                    {{ row.title }}
                  </h3>

                  <!-- 笔记摘要与外链地址 -->
                  <div class="recommend-note-desc-wrap">
                    <p class="recommend-note-desc">
                      {{ row.summary || (row.noteType === 2 ? '在线创作笔记，点击阅读全文深度解析...' : '收录外部知识链接，点击查看详情与链接') }}
                    </p>
                    <div
                      v-if="row.noteType === 1 && row.locationOrUrl"
                      class="card-external-link-pill"
                      @click.stop="openExternalUrl(row.locationOrUrl)"
                      title="点击在新窗口打开外链"
                    >
                      <el-icon><Link /></el-icon>
                      <span class="url-text">{{ row.locationOrUrl }}</span>
                      <el-icon class="url-open-icon"><TopRight /></el-icon>
                    </div>
                  </div>

                  <!-- 标签芯片 -->
                  <div class="recommend-tags-row" v-if="row.tags && row.tags.length" @click.stop>
                    <span
                      v-for="t in row.tags"
                      :key="t"
                      class="card-tag-item"
                      :class="{ active: selectedTag === t }"
                      @click="handleFilterByTag(t)"
                    >
                      #{{ t }}
                    </span>
                  </div>

                  <!-- 作者与互动数据行 -->
                  <div class="recommend-card-meta">
                    <div class="card-author" @click.stop="$router.push(`/user/${row.userId}`)">
                      <el-avatar :size="26" :src="row.avatar || defaultAvatar">
                        {{ row.username?.charAt(0) || 'U' }}
                      </el-avatar>
                      <span class="author-name">@{{ row.username || '匿名作者' }}</span>
                      <span class="meta-time">{{ formatDate(row.createdAt) }}</span>
                    </div>

                    <div class="card-metrics">
                      <span title="浏览量"><el-icon><View /></el-icon> {{ row.viewCount || 0 }}</span>
                      <span title="评论数"><el-icon><ChatDotRound /></el-icon> {{ row.commentCount || 0 }}</span>
                    </div>
                  </div>

                  <!-- 卡片底部操作栏 (适配红心点赞、黄星收藏、分享) -->
                  <div class="recommend-card-actions" @click.stop>
                    <!-- 点赞按钮 (爱心，点赞变红) -->
                    <button
                      type="button"
                      class="action-btn heart-btn"
                      :class="{ 'is-liked': row.isLiked }"
                      :disabled="recommendLikeLoading[row.id]"
                      @click="toggleRecommendLike(row)"
                    >
                      <svg
                        class="action-svg heart-svg"
                        viewBox="0 0 24 24"
                        width="15"
                        height="15"
                        :fill="row.isLiked ? '#ef4444' : 'none'"
                        :stroke="row.isLiked ? '#ef4444' : 'currentColor'"
                        stroke-width="2"
                      >
                        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                      </svg>
                      <span class="btn-text">{{ row.isLiked ? '已点赞' : '点赞' }}</span>
                      <span class="btn-count">({{ row.likeCount || 0 }})</span>
                    </button>

                    <!-- 收藏按钮 (五角星，收藏变黄) -->
                    <button
                      type="button"
                      class="action-btn star-btn"
                      :class="{ 'is-favorited': row.isFavorited }"
                      :disabled="recommendFavLoading[row.id]"
                      @click="toggleRecommendFavorite(row)"
                    >
                      <svg
                        class="action-svg star-svg"
                        viewBox="0 0 24 24"
                        width="15"
                        height="15"
                        :fill="row.isFavorited ? '#f59e0b' : 'none'"
                        :stroke="row.isFavorited ? '#f59e0b' : 'currentColor'"
                        stroke-width="2"
                      >
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                      </svg>
                      <span class="btn-text">{{ row.isFavorited ? '已收藏' : '收藏' }}</span>
                      <span class="btn-count">({{ row.favoriteCount || 0 }})</span>
                    </button>

                    <!-- 分享按钮 -->
                    <button
                      type="button"
                      class="action-btn share-btn"
                      @click="handleShareNote(row)"
                      title="复制分享链接"
                    >
                      <el-icon :size="14"><Share /></el-icon>
                      <span class="btn-text">分享</span>
                    </button>

                    <!-- 查看全文 -->
                    <el-button
                      type="primary"
                      link
                      size="small"
                      class="read-more-btn"
                      @click="openNoteDetail(row)"
                    >
                      阅读全文 <el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                </article>
              </div>

              <!-- 空状态 -->
              <el-empty
                v-else-if="!recommendLoading"
                description="暂无符合条件的推荐笔记，点击上方“换一批”或更换标签探索"
              >
                <el-button type="primary" plain @click="clearTagFilter">返回全部推荐</el-button>
              </el-empty>

              <!-- 底部换一批常驻条 -->
              <div class="recommend-bottom-bar" v-if="recommendList.length">
                <span class="batch-hint">当前展示第 {{ recommendPage }} 批优质推荐笔记（每次刷新 6 条）</span>
                <el-button
                  type="primary"
                  :icon="Refresh"
                  :loading="recommendLoading"
                  @click="handleBatchRefresh"
                >
                  换一批看看
                </el-button>
              </div>
            </section>
          </template>

          <!-- ==================== 2. 全站热门榜 ==================== -->
          <template v-else-if="viewMode === 'trending'">
            <section class="social-section">
              <div class="social-section-header">
                <div>
                  <h2>🔥 全站综合热门榜</h2>
                  <p>根据浏览量(×1)、点赞(×3)、收藏(×5)、评论(×2)综合加权计算的全站热度排行榜</p>
                </div>
                <el-button text type="primary" :icon="Refresh" @click="fetchTrendingNotes">刷新</el-button>
              </div>
              <div v-if="trendingList.length" class="trending-list">
                <article
                  v-for="(row, index) in trendingList"
                  :key="row.id"
                  class="trending-card"
                  @click="openNoteDetail(row)"
                >
                  <div class="trend-rank" :class="{ top: index < 3 }">
                    {{ String(index + 1).padStart(2, '0') }}
                  </div>
                  <div class="trending-main">
                    <div class="social-note-author">
                      <el-avatar :size="32" :src="row.avatar || defaultAvatar">
                        {{ row.username?.charAt(0) || 'U' }}
                      </el-avatar>
                      <div>
                        <strong>{{ row.username || '匿名用户' }}</strong>
                        <span>{{ formatDate(row.createdAt) }}</span>
                      </div>
                    </div>
                    <h3>{{ row.title }}</h3>
                    <p class="social-note-summary">{{ row.summary || (row.noteType === 2 ? '在线创作笔记，点击阅读全文' : row.locationOrUrl || '暂无摘要') }}</p>

                    <!-- 标签行 -->
                    <div class="recommend-tags-row" v-if="row.tags && row.tags.length" @click.stop>
                      <span v-for="t in row.tags" :key="t" class="card-tag-item" @click="switchToTag(t)">#{{ t }}</span>
                    </div>

                    <div class="social-note-footer">
                      <div class="social-note-stats">
                        <span><el-icon><View /></el-icon>{{ row.viewCount || 0 }}</span>
                        <span><el-icon><ChatDotRound /></el-icon>{{ row.commentCount || 0 }}</span>
                      </div>
                      <div class="recommend-card-actions" @click.stop>
                        <button
                          type="button"
                          class="action-btn heart-btn"
                          :class="{ 'is-liked': row.isLiked }"
                          :disabled="socialLikeLoading[row.id]"
                          @click="toggleSocialLike(row)"
                        >
                          <svg
                            class="action-svg heart-svg"
                            viewBox="0 0 24 24"
                            width="14"
                            height="14"
                            :fill="row.isLiked ? '#ef4444' : 'none'"
                            :stroke="row.isLiked ? '#ef4444' : 'currentColor'"
                            stroke-width="2"
                          >
                            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                          </svg>
                          <span>{{ row.isLiked ? '已点赞' : '点赞' }}</span>
                          <span class="btn-count">({{ row.likeCount || 0 }})</span>
                        </button>
                        <button
                          type="button"
                          class="action-btn star-btn"
                          :class="{ 'is-favorited': row.isFavorited }"
                          :disabled="recommendFavLoading[row.id]"
                          @click="toggleRecommendFavorite(row)"
                        >
                          <svg
                            class="action-svg star-svg"
                            viewBox="0 0 24 24"
                            width="14"
                            height="14"
                            :fill="row.isFavorited ? '#f59e0b' : 'none'"
                            :stroke="row.isFavorited ? '#f59e0b' : 'currentColor'"
                            stroke-width="2"
                          >
                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                          </svg>
                          <span>{{ row.isFavorited ? '已收藏' : '收藏' }}</span>
                          <span class="btn-count">({{ row.favoriteCount || 0 }})</span>
                        </button>
                        <button type="button" class="action-btn share-btn" @click="handleShareNote(row)">
                          <el-icon :size="14"><Share /></el-icon>
                          <span>分享</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
              <el-empty v-else-if="!socialLoading" description="暂时没有热门笔记" />
            </section>
          </template>

          <!-- ==================== 3. 关注动态流 ==================== -->
          <template v-else-if="viewMode === 'following'">
            <section class="social-section">
              <div class="social-section-header">
                <div>
                  <h2>💬 创作者关注动态</h2>
                  <p>查看你所关注的优秀作者近期公开发布的笔记</p>
                </div>
                <el-button text type="primary" :icon="Refresh" @click="fetchFollowingFeed">刷新</el-button>
              </div>
              <div v-if="feedList.length" class="social-note-list">
                <article
                  v-for="row in feedList"
                  :key="row.id"
                  class="social-note-card"
                  @click="openNoteDetail(row)"
                >
                  <div class="social-note-author">
                    <el-avatar :size="36" :src="row.avatar || defaultAvatar">
                      {{ row.username?.charAt(0) || 'U' }}
                    </el-avatar>
                    <div>
                      <strong>{{ row.username || '匿名用户' }}</strong>
                      <span>{{ formatDate(row.createdAt) }}</span>
                    </div>
                  </div>
                  <el-tag size="small" type="success" effect="plain" class="new-tag">最新发布</el-tag>
                  <h3>{{ row.title }}</h3>
                  <p class="social-note-summary">{{ row.summary || (row.noteType === 2 ? '在线创作笔记，点击阅读全文' : row.locationOrUrl || '暂无摘要') }}</p>

                  <div class="social-note-footer">
                    <div class="social-note-stats">
                      <span><el-icon><View /></el-icon>{{ row.viewCount || 0 }}</span>
                      <span><el-icon><ChatDotRound /></el-icon>{{ row.commentCount || 0 }}</span>
                    </div>
                    <div class="recommend-card-actions" @click.stop>
                      <button
                        type="button"
                        class="action-btn heart-btn"
                        :class="{ 'is-liked': row.isLiked }"
                        :disabled="socialLikeLoading[row.id]"
                        @click="toggleSocialLike(row)"
                      >
                        <svg
                          class="action-svg heart-svg"
                          viewBox="0 0 24 24"
                          width="14"
                          height="14"
                          :fill="row.isLiked ? '#ef4444' : 'none'"
                          :stroke="row.isLiked ? '#ef4444' : 'currentColor'"
                          stroke-width="2"
                        >
                          <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                        </svg>
                        <span>{{ row.isLiked ? '已点赞' : '点赞' }}</span>
                        <span class="btn-count">({{ row.likeCount || 0 }})</span>
                      </button>
                      <button
                        type="button"
                        class="action-btn star-btn"
                        :class="{ 'is-favorited': row.isFavorited }"
                        :disabled="recommendFavLoading[row.id]"
                        @click="toggleRecommendFavorite(row)"
                      >
                        <svg
                          class="action-svg star-svg"
                          viewBox="0 0 24 24"
                          width="14"
                          height="14"
                          :fill="row.isFavorited ? '#f59e0b' : 'none'"
                          :stroke="row.isFavorited ? '#f59e0b' : 'currentColor'"
                          stroke-width="2"
                        >
                          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                        </svg>
                        <span>{{ row.isFavorited ? '已收藏' : '收藏' }}</span>
                        <span class="btn-count">({{ row.favoriteCount || 0 }})</span>
                      </button>
                      <button type="button" class="action-btn share-btn" @click="handleShareNote(row)">
                        <el-icon :size="14"><Share /></el-icon>
                        <span>分享</span>
                      </button>
                    </div>
                  </div>
                </article>
              </div>
              <el-empty v-else-if="!socialLoading" description="暂无关注动态，可在左侧关注几位活跃作者" />
              <div v-if="feedTotal > 0" class="social-pagination">
                <el-pagination
                  layout="total, prev, pager, next"
                  :total="feedTotal"
                  :page-size="feedPageSize"
                  :current-page="feedPage"
                  @current-change="handleFeedPageChange"
                />
              </div>
            </section>
          </template>

          <!-- ==================== 4. 作者个人笔记专栏 ==================== -->
          <template v-else-if="viewMode === 'authors' && selectedUser">
            <!-- 用户基本信息与关注状态 -->
            <div class="user-header">
              <div class="user-header-left">
                <el-avatar
                  :size="60"
                  :src="selectedUser.avatar || defaultAvatar"
                  class="clickable-avatar"
                  @click="$router.push(`/user/${selectedUser.id}`)"
                >
                  {{ selectedUser.username.charAt(0) }}
                </el-avatar>
                <div class="user-details">
                  <div class="user-details-top">
                    <h2 class="clickable-username" @click="$router.push(`/user/${selectedUser.id}`)">
                      {{ selectedUser.username }}
                    </h2>
                    <el-tag v-if="isOwner" size="small" type="success" effect="plain" round>
                      我的主页
                    </el-tag>

                    <!-- 个性签名编辑 -->
                    <div class="user-bio-wrapper">
                      <div v-if="isEditingBio" class="bio-edit-inline" @click.stop>
                        <el-input
                          ref="bioInputRef"
                          v-model="editBioText"
                          size="small"
                          placeholder="填写个性签名（最多30字）"
                          maxlength="30"
                          show-word-limit
                          class="bio-input"
                          @keyup.enter="handleSaveBio"
                        />
                        <el-button size="small" type="primary" :loading="bioSaving" :icon="Check" @click="handleSaveBio" />
                        <el-button size="small" :icon="Close" @click="cancelEditBio" />
                      </div>
                      <div
                        v-else
                        class="bio-badge"
                        :class="{ 'editable-bio': isOwner }"
                        :title="isOwner ? '点击修改个性签名' : ''"
                        @click="startEditBio"
                      >
                        <span v-if="selectedUser.bio" class="bio-text">{{ selectedUser.bio }}</span>
                        <span v-else-if="isOwner" class="bio-placeholder">暂未设置个性签名，点击添加</span>
                        <span v-else class="bio-placeholder">这个人很神秘，什么都没写</span>
                        <el-icon v-if="isOwner" class="bio-icon"><EditPen /></el-icon>
                      </div>
                    </div>
                  </div>

                  <div class="user-stats">
                    <span class="stat-item"><strong>{{ totalNotes }}</strong> 篇笔记</span>
                    <span class="stat-divider">·</span>
                    <span class="stat-item"><strong>{{ currentFollowStats?.followerCount ?? selectedUser.followerCount ?? 0 }}</strong> 粉丝</span>
                    <span class="stat-divider">·</span>
                    <span class="stat-item"><strong>{{ currentFollowStats?.followingCount ?? 0 }}</strong> 关注</span>
                  </div>
                </div>
              </div>

              <div class="user-header-actions">
                <el-button
                  v-if="!isOwner"
                  :type="currentFollowStats?.isFollowing ? 'default' : 'primary'"
                  :loading="followActionLoading"
                  @click="toggleFollowAuthor"
                >
                  {{ currentFollowStats?.isFollowing ? '已关注' : '+ 关注' }}
                </el-button>
                <el-button
                  v-if="isOwner"
                  @click="router.push('/settings')"
                >
                  个人设置
                </el-button>
              </div>
            </div>

            <!-- 总链接卡片 -->
            <div v-if="mainLink" class="main-link-card" @click="openMainLink">
              <div class="main-link-icon">
                <el-icon :size="24"><Link /></el-icon>
              </div>
              <div class="main-link-info">
                <div class="main-link-title">个人主页/知识库总链接</div>
                <div class="main-link-url">{{ mainLink.mainUrl }}</div>
              </div>
              <el-icon class="main-link-arrow"><ArrowRight /></el-icon>
            </div>

            <!-- 笔记列表区 -->
            <div class="notes-section">
              <div class="notes-toolbar">
                <h3>TA 的全部笔记</h3>
                <div class="note-filter-bar">
                  <el-input
                    v-model="localNoteKeyword"
                    placeholder="搜索笔记名称..."
                    size="default"
                    style="width: 180px"
                    clearable
                    @keyup.enter="handleLocalNoteSearch"
                    @clear="handleClearLocalNoteSearch"
                  />
                  <el-date-picker
                    v-model="noteDateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD"
                    size="default"
                    style="width: 240px"
                    @change="handleLocalNoteSearch"
                  />
                  <el-button type="primary" :icon="Search" @click="handleLocalNoteSearch">搜索</el-button>
                </div>
              </div>

              <el-table :data="noteList" style="width: 100%" stripe>
                <el-table-column prop="title" label="笔记名称" min-width="200">
                  <template #default="{ row }">
                    <span class="note-title" :class="{ 'clickable-title': row.noteType === 2 }" @click="row.noteType === 2 && router.push(`/note/${row.id}`)">
                      <el-tag v-if="row.isPinned === 1" type="warning" size="small" style="margin-right: 6px;">置顶</el-tag>
                      <el-tag :type="row.noteType === 2 ? 'success' : 'info'" size="small" style="margin-right: 6px;">
                        {{ row.noteType === 2 ? '原创' : '外链' }}
                      </el-tag>
                      <el-tag v-if="row.isPublic === 0" type="danger" effect="plain" size="small" style="margin-right: 6px;">
                        <el-icon style="margin-right: 2px; vertical-align: -1px;"><Lock /></el-icon>私密
                      </el-tag>
                      {{ row.title }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="内容/位置" min-width="240">
                  <template #default="{ row }">
                    <div
                      v-if="row.noteType === 2"
                      class="note-article-summary"
                      @click="router.push(`/note/${row.id}`)"
                    >
                      <el-icon><Document /></el-icon>
                      <span class="summary-text">{{ row.summary || '在线创作笔记 (点击阅读全文)' }}</span>
                    </div>
                    <template v-else>
                      <a
                        v-if="isUrl(row.locationOrUrl)"
                        :href="row.locationOrUrl"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="note-link"
                        @click="incrementView(row)"
                      >
                        <el-icon><Link /></el-icon>
                        {{ row.locationOrUrl }}
                      </a>
                      <div v-else class="note-location">
                        <span>{{ row.locationOrUrl }}</span>
                        <el-button
                          type="info"
                          size="small"
                          text
                          @click="copyToClipboard(row.locationOrUrl)"
                        >
                          <el-icon><CopyDocument /></el-icon>
                        </el-button>
                      </div>
                    </template>
                  </template>
                </el-table-column>
                <el-table-column prop="viewCount" label="浏览" width="70" align="center" />
                <el-table-column prop="updatedAt" label="更新时间" width="150">
                  <template #default="{ row }">
                    {{ formatDate(row.updatedAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" :width="isOwner ? '180' : '100'" align="center" fixed="right">
                  <template #default="{ row }">
                    <div class="table-action-group">
                      <button
                        type="button"
                        class="action-btn star-btn table-fav-btn"
                        :class="{ 'is-favorited': favoriteMap[row.id] || row.isFavorited }"
                        @click="toggleFavorite(row)"
                      >
                        <svg
                          class="action-svg star-svg"
                          viewBox="0 0 24 24"
                          width="13"
                          height="13"
                          :fill="(favoriteMap[row.id] || row.isFavorited) ? '#f59e0b' : 'none'"
                          :stroke="(favoriteMap[row.id] || row.isFavorited) ? '#f59e0b' : 'currentColor'"
                          stroke-width="2"
                        >
                          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                        </svg>
                        <span>{{ (favoriteMap[row.id] || row.isFavorited) ? '已收藏' : '收藏' }}</span>
                      </button>

                      <template v-if="isOwner">
                        <el-button type="primary" size="small" text class="table-op-btn" @click="editNote(row)">
                          编辑
                        </el-button>
                        <el-dropdown trigger="click" @command="(cmd: string) => handleTableCommand(cmd, row)">
                          <el-button type="info" size="small" text class="table-more-btn" title="更多操作">
                            <el-icon><MoreFilled /></el-icon>
                          </el-button>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-dropdown-item command="togglePin">
                                <el-icon><Top /></el-icon>
                                <span>{{ row.isPinned === 1 ? '取消置顶' : '置顶笔记' }}</span>
                              </el-dropdown-item>
                              <el-dropdown-item command="delete" divided style="color: #f56c6c;">
                                <el-icon><Delete /></el-icon>
                                <span>删除笔记</span>
                              </el-dropdown-item>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                      </template>
                    </div>
                  </template>
                </el-table-column>
              </el-table>

              <div class="note-pagination" v-if="totalNotes > 0">
                <el-pagination
                  layout="total, prev, pager, next"
                  :total="totalNotes"
                  :page-size="notePageSize"
                  :current-page="noteCurrentPage"
                  @current-change="handleNotePageChange"
                />
              </div>
              <el-empty v-if="!contentLoading && noteList.length === 0" description="暂无笔记" />
            </div>
          </template>

          <el-empty
            v-else-if="viewMode === 'authors'"
            description="请在左侧选择一位创作者查看笔记专栏"
            :image-size="180"
          />

          <!-- ==================== 5. 全局笔记搜索结果视图 (图七区域展示所能搜到的相关公共笔记) ==================== -->
          <template v-else-if="viewMode === 'search'">
            <section class="search-result-section">
              <div class="search-result-banner">
                <div class="search-notice-left">
                  <div class="search-main-tag">
                    <el-icon><Search /></el-icon>
                    <span>公共笔记搜索结果</span>
                  </div>
                  <div class="search-desc-text">
                    共找到 <strong>{{ searchedNotes.length }}</strong> 篇与关键词 <el-tag size="small" type="primary" effect="light">“{{ noteKeyword }}”</el-tag> 相关的公共笔记，左侧已同步匹配出相关创作者
                  </div>
                </div>
                <el-button type="primary" plain size="small" :icon="Close" @click="handleClearNoteSearch">
                  退出搜索并返回推荐
                </el-button>
              </div>

              <!-- 搜索公共笔记卡片网格 -->
              <div v-if="searchedNotes.length" class="recommend-cards-grid">
                <article
                  v-for="row in searchedNotes"
                  :key="row.id"
                  class="recommend-note-card"
                  @click="openNoteDetail(row)"
                >
                  <!-- 顶部徽标栏 -->
                  <div class="card-badge-row">
                    <div class="heat-badge" title="热度值 = 浏览×1 + 点赞×3 + 收藏×5 + 评论×2">
                      <span class="heat-icon">🔥</span>
                      <span class="heat-score">热度 {{ Math.round(row.heatScore || calculateLocalHeat(row)) }}</span>
                    </div>

                    <el-tag
                      size="small"
                      :type="row.noteType === 2 ? 'success' : 'info'"
                      effect="plain"
                      class="type-pill"
                    >
                      {{ row.noteType === 2 ? 'Markdown 原创' : '外链精选' }}
                    </el-tag>
                  </div>

                  <!-- 笔记标题 -->
                  <h3 class="recommend-note-title" :title="row.title">
                    <el-tag v-if="row.isPinned === 1" type="warning" size="small" class="pinned-tag">置顶</el-tag>
                    {{ row.title }}
                  </h3>

                  <!-- 笔记摘要与外链卡片 -->
                  <div class="recommend-note-desc-wrap">
                    <p class="recommend-note-desc">
                      {{ row.summary || (row.noteType === 2 ? '在线创作笔记，点击阅读全文深度解析...' : '收录外部知识链接，点击查看详情与链接') }}
                    </p>
                    <div
                      v-if="row.noteType === 1 && row.locationOrUrl"
                      class="card-external-link-pill"
                      @click.stop="openExternalUrl(row.locationOrUrl)"
                      title="点击在新窗口打开外链"
                    >
                      <el-icon><Link /></el-icon>
                      <span class="url-text">{{ row.locationOrUrl }}</span>
                      <el-icon class="url-open-icon"><TopRight /></el-icon>
                    </div>
                  </div>

                  <!-- 标签芯片 -->
                  <div class="recommend-tags-row" v-if="row.tags && row.tags.length" @click.stop>
                    <span
                      v-for="t in row.tags"
                      :key="t"
                      class="card-tag-item"
                      :class="{ active: selectedTag === t }"
                      @click="handleFilterByTag(t)"
                    >
                      #{{ t }}
                    </span>
                  </div>

                  <!-- 作者与互动数据行 -->
                  <div class="recommend-card-meta">
                    <div class="card-author" @click.stop="$router.push(`/user/${row.userId}`)">
                      <el-avatar :size="26" :src="row.avatar || defaultAvatar">
                        {{ row.username?.charAt(0) || 'U' }}
                      </el-avatar>
                      <span class="author-name">@{{ row.username || '匿名作者' }}</span>
                      <span class="meta-time">{{ formatDate(row.createdAt) }}</span>
                    </div>

                    <div class="card-metrics">
                      <span title="浏览量"><el-icon><View /></el-icon> {{ row.viewCount || 0 }}</span>
                      <span title="评论数"><el-icon><ChatDotRound /></el-icon> {{ row.commentCount || 0 }}</span>
                    </div>
                  </div>

                  <!-- 卡片底部操作栏 (适配红心点赞、黄星收藏、分享) -->
                  <div class="recommend-card-actions" @click.stop>
                    <!-- 点赞按钮 (爱心，点赞变红) -->
                    <button
                      type="button"
                      class="action-btn heart-btn"
                      :class="{ 'is-liked': row.isLiked }"
                      :disabled="recommendLikeLoading[row.id]"
                      @click="toggleRecommendLike(row)"
                    >
                      <svg
                        class="action-svg heart-svg"
                        viewBox="0 0 24 24"
                        width="15"
                        height="15"
                        :fill="row.isLiked ? '#ef4444' : 'none'"
                        :stroke="row.isLiked ? '#ef4444' : 'currentColor'"
                        stroke-width="2"
                      >
                        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                      </svg>
                      <span class="btn-text">{{ row.isLiked ? '已点赞' : '点赞' }}</span>
                      <span class="btn-count">({{ row.likeCount || 0 }})</span>
                    </button>

                    <!-- 收藏按钮 (五角星，收藏变黄) -->
                    <button
                      type="button"
                      class="action-btn star-btn"
                      :class="{ 'is-favorited': row.isFavorited }"
                      :disabled="recommendFavLoading[row.id]"
                      @click="toggleRecommendFavorite(row)"
                    >
                      <svg
                        class="action-svg star-svg"
                        viewBox="0 0 24 24"
                        width="15"
                        height="15"
                        :fill="row.isFavorited ? '#f59e0b' : 'none'"
                        :stroke="row.isFavorited ? '#f59e0b' : 'currentColor'"
                        stroke-width="2"
                      >
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                      </svg>
                      <span class="btn-text">{{ row.isFavorited ? '已收藏' : '收藏' }}</span>
                      <span class="btn-count">({{ row.favoriteCount || 0 }})</span>
                    </button>

                    <!-- 分享按钮 -->
                    <button
                      type="button"
                      class="action-btn share-btn"
                      @click="handleShareNote(row)"
                      title="复制分享链接"
                    >
                      <el-icon :size="14"><Share /></el-icon>
                      <span class="btn-text">分享</span>
                    </button>

                    <!-- 查看全文 -->
                    <el-button
                      type="primary"
                      link
                      size="small"
                      class="read-more-btn"
                      @click="openNoteDetail(row)"
                    >
                      阅读全文 <el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                </article>
              </div>
              <el-empty
                v-else-if="!contentLoading"
                description="未搜索到包含该关键词的相关公共笔记，您可以尝试其他关键词搜索"
                :image-size="160"
              />
            </section>
          </template>
        </main>

        <!-- ==================== 右侧辅助看板 (匹配原型图右侧) ==================== -->
        <aside class="right-widget-sidebar" v-if="!isMobile && (viewMode === 'recommend' || viewMode === 'trending')">
          <!-- Widget 1: 今日学习与阅读看板 -->
          <div class="widget-card">
            <div class="widget-header">
              <span class="widget-title">📊 学习与阅读看板</span>
            </div>
            <div class="dashboard-stats-grid">
              <div class="stat-tile tile-blue">
                <span class="stat-num">{{ recommendList.length * 2 + 5 }}</span>
                <span class="stat-label">今日精选</span>
              </div>
              <div class="stat-tile tile-amber">
                <span class="stat-num">{{ hotTopics.length }}</span>
                <span class="stat-label">热门专题</span>
              </div>
              <div class="stat-tile tile-purple">
                <span class="stat-num">{{ totalUsers || 8 }}</span>
                <span class="stat-label">活跃作者</span>
              </div>
            </div>
            <div class="target-progress-box">
              <div class="target-label">
                <span>每日学习目标推荐</span>
                <span class="target-percent">75%</span>
              </div>
              <el-progress :percentage="75" :stroke-width="8" color="#4f46e5" :show-text="false" />
            </div>
          </div>

          <!-- Widget 2: 猜你感兴趣的知识专题 (从数据库真实热门标签获取，若无标签则不显示) -->
          <div class="widget-card" v-if="hotTopics.length > 0">
            <div class="widget-header">
              <span class="widget-title">💡 猜你感兴趣的专题</span>
            </div>
            <div class="topic-list">
              <div
                v-for="item in hotTopics"
                :key="item.name"
                class="topic-item"
                @click="switchToTag(item.name)"
              >
                <div class="topic-title"># {{ item.name }}</div>
                <div class="topic-meta">
                  <span>{{ item.noteCount }} 篇公开笔记</span>
                  <span class="match-score">热门专题</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Widget 3: 推荐关注创作者 (过滤掉已关注创作者和当前登录用户) -->
          <div class="widget-card" v-if="recommendedCreators.length > 0">
            <div class="widget-header">
              <span class="widget-title">👥 推荐创作者</span>
            </div>
            <div class="rec-creator-list">
              <div
                v-for="u in recommendedCreators"
                :key="u.id"
                class="creator-item"
                @click="selectUser(u)"
              >
                <el-avatar :size="34" :src="u.avatar || defaultAvatar">
                  {{ u.username.charAt(0) }}
                </el-avatar>
                <div class="creator-info">
                  <div class="creator-name">@{{ u.username }}</div>
                  <div class="creator-desc">{{ u.bio || `${u.noteCount || 0} 篇笔记 · 推荐关注` }}</div>
                </div>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  class="quick-follow-btn"
                  @click.stop="quickFollowUser(u)"
                >
                  关注
                </el-button>
              </div>
            </div>
            <div class="creator-card-footer">关注后，最新公开笔记将优先推送</div>
          </div>
        </aside>
      </div>
    </div>

    <!-- 发布/编辑笔记弹窗 -->
    <PublishDialog
      v-model:visible="showPublishDialog"
      :edit-note="editingNote"
      :has-main-link="!!mainLink"
      :is-owner="isOwner"
      @success="onPublishSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  User as UserIcon,
  Search,
  Plus,
  Menu,
  Link,
  ArrowRight,
  CopyDocument,
  Close,
  Document,
  Lock,
  Fold,
  Expand,
  Check,
  EditPen,
  Connection,
  TrendCharts,
  Refresh,
  View,
  Star,
  ChatDotRound,
  Compass,
  Share,
  MoreFilled,
  Top,
  Delete,
  TopRight,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateUserBio, getRecommendedCreators } from '@/api/user'
import {
  getActiveUsers,
  getMainLink,
  getUserNotes,
  deleteNote,
  globalSearch,
  incrementNoteView,
  addFavorite,
  removeFavorite,
  checkFavorite,
  pinNote,
  unpinNote,
  followUser,
  unfollowUser,
  getFollowStats,
  getFollowingFeed,
  getTrendingNotes,
  getRecommendedNotes,
  likeNote,
  unlikeNote,
  checkNoteLiked,
  getPopularTags,
} from '@/api/note'
import { formatDate, isUrl, isSafeUrl } from '@/utils/helpers'
import PublishDialog from '@/components/PublishDialog.vue'
import type { User, MainLink, NoteItem, FollowStatsVO, PopularTagItem } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

const defaultAvatar = ''
const authorKeyword = ref('')
const noteKeyword = ref('')
const isSearching = computed(() => !!authorKeyword.value.trim() || !!noteKeyword.value.trim())
const userList = ref<User[]>([])
const userLoading = ref(false)
const contentLoading = ref(false)
const selectedUserId = ref<number | null>(null)
const selectedUser = ref<User | null>(null)
const mainLink = ref<MainLink | null>(null)
const noteList = ref<NoteItem[]>([])
const currentPage = ref(1)
const pageSize = 20
const totalUsers = ref(0)
const totalNotes = ref(0)
const noteCurrentPage = ref(1)
const notePageSize = 10
const localNoteKeyword = ref('')
const noteDateRange = ref<[string, string] | null>(null)
const showPublishDialog = ref(false)
const editingNote = ref<NoteItem | null>(null)
const drawerVisible = ref(false)
const isMobile = ref(false)

// 当前主页模式：默认进入公共“智能推荐” (recommend)，搜索时切换至 'search'
const viewMode = ref<'recommend' | 'trending' | 'following' | 'authors' | 'search'>('recommend')
const socialLoading = ref(false)
const feedList = ref<NoteItem[]>([])
const feedPage = ref(1)
const feedPageSize = 10
const feedTotal = ref(0)
const trendingList = ref<NoteItem[]>([])
const socialLikeLoading = ref<Record<number, boolean>>({})

// ==================== 智能推荐状态 (每次刷新6条) ====================
const recommendList = ref<NoteItem[]>([])
const recommendPage = ref(1)
const recommendPageSize = 6
const recommendTotal = ref(0)
const recommendLoading = ref(false)
const selectedTag = ref('')
const recommendLikeLoading = ref<Record<number, boolean>>({})
const recommendFavLoading = ref<Record<number, boolean>>({})

// 数据库动态热门标签与推荐创作者
const hotTopics = ref<PopularTagItem[]>([])
const recommendedCreators = ref<User[]>([])
// 全局搜索搜出的公共笔记列表
const searchedNotes = ref<NoteItem[]>([])

// 视图标题计算
const viewModeTitle = computed(() => {
  switch (viewMode.value) {
    case 'recommend':
      return '✨ 智能综合推荐 (基于热度加权推荐)'
    case 'trending':
      return '🔥 全站综合热门榜'
    case 'following':
      return '💬 创作者关注动态'
    case 'authors':
      return selectedUser.value ? `@${selectedUser.value.username} 的笔记专栏` : '创作者笔记专栏'
    case 'search':
      return `🔍 公共笔记搜索结果 — “${noteKeyword.value}”`
    default:
      return '发现优质知识'
  }
})

// 侧边栏折叠状态与持久化
const isSidebarCollapsed = ref(localStorage.getItem('sidebar_collapsed') === 'true')

function toggleSidebarCollapse() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
  localStorage.setItem('sidebar_collapsed', String(isSidebarCollapsed.value))
}

// 关注状态与加载
const currentFollowStats = ref<FollowStatsVO | null>(null)
const followActionLoading = ref(false)

// 个性签名即时编辑
const isEditingBio = ref(false)
const editBioText = ref('')
const bioSaving = ref(false)
const bioInputRef = ref<any>(null)

function startEditBio() {
  if (!isOwner.value) return
  editBioText.value = selectedUser.value?.bio || ''
  isEditingBio.value = true
  setTimeout(() => {
    bioInputRef.value?.focus()
  }, 50)
}

function cancelEditBio() {
  isEditingBio.value = false
  editBioText.value = ''
}

async function handleSaveBio() {
  if (!selectedUser.value || !isOwner.value) return
  const newBio = editBioText.value.trim()
  if (newBio.length > 30) {
    ElMessage.warning('个性签名不能超过30个字符')
    return
  }
  bioSaving.value = true
  try {
    const res = await updateUserBio(newBio)
    const savedBio = res.data.bio !== undefined ? res.data.bio : newBio
    selectedUser.value.bio = savedBio
    if (userStore.userInfo) {
      userStore.userInfo.bio = savedBio
    }
    const found = userList.value.find(u => u.id === selectedUser.value?.id)
    if (found) {
      found.bio = savedBio
    }
    ElMessage.success('个性签名修改成功')
    isEditingBio.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '修改个性签名失败')
  } finally {
    bioSaving.value = false
  }
}

const isOwner = computed(() => {
  return isLoggedIn.value && selectedUserId.value === userStore.userInfo?.id
})

// 响应式检测
function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  fetchUsers()
  fetchRecommendedNotes()
  fetchPopularTags()
  fetchRecommendedCreators()
  if (isLoggedIn.value) {
    userStore.fetchUserInfo()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

// ==================== 动态热门标签与推荐创作者 ====================
async function fetchPopularTags() {
  try {
    const res = await getPopularTags(6)
    hotTopics.value = res.data || []
  } catch {
    hotTopics.value = []
  }
}

async function fetchRecommendedCreators() {
  try {
    const res = await getRecommendedCreators(5)
    recommendedCreators.value = res.data || []
  } catch {
    recommendedCreators.value = []
  }
}

function getExternalUrl(url?: string): string | null {
  const value = url?.trim()
  if (!value) return null
  const targetUrl = /^https?:\/\//i.test(value) ? value : `https://${value}`
  return isSafeUrl(targetUrl) && /^https?:\/\//i.test(targetUrl) ? targetUrl : null
}

function openExternalUrl(url?: string) {
  const targetUrl = getExternalUrl(url)
  if (!targetUrl) {
    ElMessage.warning('链接地址无效，仅支持 HTTP/HTTPS 链接')
    return
  }
  window.open(targetUrl, '_blank', 'noopener,noreferrer')
}

// ==================== 推荐列表拉取与换一批 (每次6条) ====================
async function fetchRecommendedNotes() {
  recommendLoading.value = true
  try {
    const res = await getRecommendedNotes({
      page: recommendPage.value,
      size: recommendPageSize,
      tag: selectedTag.value || undefined,
    })
    recommendList.value = res.data?.list || []
    recommendTotal.value = res.data?.total || 0
  } catch (error) {
    console.warn('获取推荐笔记失败:', error)
    recommendList.value = []
    recommendTotal.value = 0
  } finally {
    recommendLoading.value = false
  }
}

// 换一批 (一次刷新6条)
async function handleBatchRefresh() {
  if (recommendLoading.value) return
  const maxPage = Math.max(1, Math.ceil(recommendTotal.value / recommendPageSize))
  if (recommendPage.value >= maxPage) {
    recommendPage.value = 1
  } else {
    recommendPage.value += 1
  }
  await fetchRecommendedNotes()
  ElMessage.success({
    message: `已为您刷新第 ${recommendPage.value} 批推荐笔记`,
    duration: 1200,
  })
}

// 切换到推荐并清除标签
function switchToRecommend() {
  viewMode.value = 'recommend'
  selectedTag.value = ''
  recommendPage.value = 1
  fetchRecommendedNotes()
}

// 标签筛选与切换
function handleFilterByTag(tag: string) {
  if (selectedTag.value === tag) {
    selectedTag.value = ''
  } else {
    selectedTag.value = tag
  }
  viewMode.value = 'recommend'
  recommendPage.value = 1
  fetchRecommendedNotes()
}

function switchToTag(tag: string) {
  selectedTag.value = tag
  viewMode.value = 'recommend'
  recommendPage.value = 1
  fetchRecommendedNotes()
}

function clearTagFilter() {
  selectedTag.value = ''
  recommendPage.value = 1
  fetchRecommendedNotes()
}

function calculateLocalHeat(row: NoteItem): number {
  const views = row.viewCount || 0
  const likes = row.likeCount || 0
  const favs = row.favoriteCount || 0
  const comments = row.commentCount || 0
  return views * 1 + likes * 3 + favs * 5 + comments * 2
}

// ==================== 推荐卡片红心点赞与黄星收藏 ====================
async function toggleRecommendLike(row: NoteItem) {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('点赞需要先登录，是否前往登录？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
    }).then(() => router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }))
    return
  }
  if (recommendLikeLoading.value[row.id]) return
  recommendLikeLoading.value[row.id] = true
  const prevLiked = row.isLiked === true
  const prevCount = row.likeCount || 0
  row.isLiked = !prevLiked
  row.likeCount = Math.max(0, prevCount + (row.isLiked ? 1 : -1))
  try {
    if (row.isLiked) {
      await likeNote(row.id)
    } else {
      await unlikeNote(row.id)
    }
  } catch {
    row.isLiked = prevLiked
    row.likeCount = prevCount
  } finally {
    recommendLikeLoading.value[row.id] = false
  }
}

async function toggleRecommendFavorite(row: NoteItem) {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('收藏需要先登录，是否前往登录？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
    }).then(() => router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }))
    return
  }
  if (recommendFavLoading.value[row.id]) return
  recommendFavLoading.value[row.id] = true
  const prevFav = row.isFavorited === true
  const prevCount = row.favoriteCount || 0
  row.isFavorited = !prevFav
  row.favoriteCount = Math.max(0, prevCount + (row.isFavorited ? 1 : -1))
  try {
    if (row.isFavorited) {
      await addFavorite(row.id)
      ElMessage.success('收藏成功')
    } else {
      await removeFavorite(row.id)
      ElMessage.success('已取消收藏')
    }
  } catch {
    row.isFavorited = prevFav
    row.favoriteCount = prevCount
  } finally {
    recommendFavLoading.value[row.id] = false
  }
}

// 分享笔记
function handleShareNote(row: NoteItem) {
  const url = `${window.location.origin}/note/${row.id}`
  if (navigator.clipboard) {
    navigator.clipboard.writeText(`${row.title} - 笔记共享平台：${url}`).then(() => {
      ElMessage.success('笔记链接已复制到剪贴板，可直接分享给好友！')
    }).catch(() => {
      ElMessage.info(`笔记链接：${url}`)
    })
  } else {
    ElMessage.info(`笔记链接：${url}`)
  }
}

function openNoteDetail(row: NoteItem) {
  router.push(`/note/${row.id}`)
}

// ==================== 创作者列表与模式切换 ====================
async function fetchUsers() {
  userLoading.value = true
  try {
    const res = await getActiveUsers({
      page: currentPage.value,
      size: pageSize,
      keyword: authorKeyword.value || undefined,
    })
    userList.value = res.data.list
    totalUsers.value = res.data.total
  } catch (error) {
    console.warn('获取创作者列表失败:', error)
  } finally {
    userLoading.value = false
  }
}

async function handleModeChange(mode: string | number | boolean) {
  const nextMode = String(mode) as 'recommend' | 'trending' | 'following' | 'authors'
  viewMode.value = nextMode

  if (nextMode === 'recommend') {
    if (recommendList.value.length === 0) {
      await fetchRecommendedNotes()
    }
  } else if (nextMode === 'following') {
    if (!isLoggedIn.value) {
      ElMessageBox.confirm('关注动态需要登录，是否前往登录？', '提示', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'info',
      }).then(() => {
        router.push({ name: 'Login', query: { redirect: '/' } })
      }).catch(() => {
        viewMode.value = 'recommend'
      })
      return
    }
    await fetchFollowingFeed()
  } else if (nextMode === 'trending') {
    if (trendingList.value.length === 0) {
      await fetchTrendingNotes()
    }
  } else if (nextMode === 'authors') {
    if (!selectedUser.value && userList.value.length > 0) {
      selectUser(userList.value[0])
    }
  }
}

async function fetchFollowingFeed() {
  if (!isLoggedIn.value) return
  socialLoading.value = true
  try {
    const res = await getFollowingFeed({ page: feedPage.value, size: feedPageSize })
    feedList.value = res.data?.list || []
    feedTotal.value = res.data?.total || 0
    await hydrateSocialLikeStates(feedList.value)
  } catch {
    feedList.value = []
    feedTotal.value = 0
  } finally {
    socialLoading.value = false
  }
}

async function fetchTrendingNotes() {
  socialLoading.value = true
  try {
    const res = await getTrendingNotes(10)
    trendingList.value = res.data || []
    await hydrateSocialLikeStates(trendingList.value)
  } catch {
    trendingList.value = []
  } finally {
    socialLoading.value = false
  }
}

async function hydrateSocialLikeStates(list: NoteItem[]) {
  if (!isLoggedIn.value) return
  await Promise.all(list.map(async row => {
    if (typeof row.isLiked === 'boolean') return
    try {
      const res = await checkNoteLiked(row.id)
      row.isLiked = res.data
    } catch {
      row.isLiked = false
    }
  }))
}

async function toggleSocialLike(row: NoteItem) {
  if (!isLoggedIn.value) {
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (socialLikeLoading.value[row.id]) return
  const previousLiked = row.isLiked === true
  const previousCount = row.likeCount || 0
  row.isLiked = !previousLiked
  row.likeCount = Math.max(0, previousCount + (row.isLiked ? 1 : -1))
  socialLikeLoading.value[row.id] = true
  try {
    if (row.isLiked) {
      await likeNote(row.id)
    } else {
      await unlikeNote(row.id)
    }
  } catch {
    row.isLiked = previousLiked
    row.likeCount = previousCount
  } finally {
    socialLikeLoading.value[row.id] = false
  }
}

function handleFeedPageChange(page: number) {
  feedPage.value = page
  fetchFollowingFeed()
}

// 选择用户
async function selectUser(user: User) {
  viewMode.value = 'authors'
  isEditingBio.value = false
  selectedUserId.value = user.id
  selectedUser.value = user
  localNoteKeyword.value = ''
  noteDateRange.value = null
  noteCurrentPage.value = 1
  drawerVisible.value = false
  await fetchUserDetail(user.id)
}

// 快速关注推荐创作者
async function quickFollowUser(user: User) {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('关注创作者需要先登录，是否前往登录？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
    }).then(() => router.push('/login')).catch(() => {})
    return
  }
  try {
    await followUser(user.id)
    ElMessage.success(`已关注 @${user.username}`)
    fetchUsers()
    fetchRecommendedCreators()
  } catch (e: any) {
    ElMessage.error(e.message || '关注失败')
  }
}

// 获取用户详情
async function fetchUserDetail(userId: number) {
  if (!userId) return
  contentLoading.value = true
  mainLink.value = null
  noteList.value = []
  try {
    try {
      const linkRes = await getMainLink(userId)
      mainLink.value = linkRes.data
    } catch { mainLink.value = null }

    fetchFollowStats(userId)

    const notesRes = await getUserNotes(userId, {
      page: noteCurrentPage.value,
      size: notePageSize,
      keyword: localNoteKeyword.value || undefined,
      startDate: noteDateRange.value?.[0] || undefined,
      endDate: noteDateRange.value?.[1] || undefined,
    })
    noteList.value = notesRes.data.list
    totalNotes.value = notesRes.data.total
  } catch (error) {
    console.warn('获取笔记列表失败:', error)
  } finally {
    contentLoading.value = false
    loadFavorites()
  }
}

async function fetchFollowStats(userId: number) {
  try {
    const res = await getFollowStats(userId)
    currentFollowStats.value = res.data
  } catch {
    currentFollowStats.value = null
  }
}

async function toggleFollowAuthor() {
  if (!selectedUserId.value) return
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('关注作者需要先登录，是否前往登录？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
    }).then(() => router.push('/login')).catch(() => {})
    return
  }

  followActionLoading.value = true
  try {
    if (currentFollowStats.value?.isFollowing) {
      await unfollowUser(selectedUserId.value)
      ElMessage.success('已取消关注')
      if (currentFollowStats.value) {
        currentFollowStats.value.isFollowing = false
        currentFollowStats.value.followerCount = Math.max(0, currentFollowStats.value.followerCount - 1)
      }
    } else {
      await followUser(selectedUserId.value)
      ElMessage.success('关注成功')
      if (currentFollowStats.value) {
        currentFollowStats.value.isFollowing = true
        currentFollowStats.value.followerCount = (currentFollowStats.value.followerCount || 0) + 1
      }
    }
    fetchUsers()
    fetchRecommendedCreators()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    followActionLoading.value = false
  }
}

async function handleAuthorSearch() {
  if (!authorKeyword.value.trim()) {
    fetchUsers()
    return
  }
  userLoading.value = true
  try {
    const res = await getActiveUsers({
      page: 1,
      size: 20,
      keyword: authorKeyword.value,
    })
    userList.value = res.data.list
    totalUsers.value = res.data.total
    if (res.data.list.length === 1) {
      selectUser(res.data.list[0])
    }
  } catch {
    fetchUsers()
  } finally {
    userLoading.value = false
  }
}

// ==================== 顶部 Logo 点击回到初始登录公共主页 ====================
function handleLogoClick() {
  authorKeyword.value = ''
  noteKeyword.value = ''
  localNoteKeyword.value = ''
  noteDateRange.value = null
  searchedNotes.value = []

  selectedUserId.value = null
  selectedUser.value = null
  currentFollowStats.value = null
  mainLink.value = null
  noteList.value = []
  totalNotes.value = 0
  noteCurrentPage.value = 1
  isEditingBio.value = false
  editBioText.value = ''

  selectedTag.value = ''
  viewMode.value = 'recommend'
  recommendPage.value = 1
  currentPage.value = 1
  feedPage.value = 1
  feedList.value = []
  feedTotal.value = 0
  drawerVisible.value = false

  fetchUsers()
  fetchRecommendedNotes()
  fetchPopularTags()
  fetchRecommendedCreators()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleClearAuthorSearch() {
  authorKeyword.value = ''
  currentPage.value = 1
  if (noteKeyword.value.trim()) {
    handleNoteSearch()
  } else {
    fetchUsers()
  }
}

// ==================== 全局公共笔记与创作者双栏联动搜索 ====================
async function handleNoteSearch() {
  const kw = noteKeyword.value.trim()
  if (!kw) {
    handleClearNoteSearch()
    return
  }
  userLoading.value = true
  contentLoading.value = true
  try {
    const res = await globalSearch({
      keyword: kw,
      page: 1,
      size: 50,
    })

    // 1. 设置主区域展示搜到的相关公共笔记列表 (图七搜索结果区域)
    searchedNotes.value = res.data?.notes || []
    viewMode.value = 'search'

    // 2. 设置左侧侧边栏展示包含匹配公共笔记的创作者 (使用后端返回的真实创作者数据，包含完整笔记数与粉丝数，修复0的问题)
    userList.value = res.data?.users || []
    totalUsers.value = userList.value.length

    if (searchedNotes.value.length === 0 && userList.value.length === 0) {
      ElMessage.info('未找到包含该关键词的相关公共笔记或创作者')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '搜索公共笔记失败')
  } finally {
    userLoading.value = false
    contentLoading.value = false
  }
}

function handleClearNoteSearch() {
  noteKeyword.value = ''
  searchedNotes.value = []
  viewMode.value = 'recommend'
  recommendPage.value = 1
  fetchUsers()
  fetchRecommendedNotes()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchUsers()
}

function handleNotePageChange(page: number) {
  noteCurrentPage.value = page
  if (selectedUserId.value) {
    fetchUserDetail(selectedUserId.value)
  }
}

function handleLocalNoteSearch() {
  noteCurrentPage.value = 1
  if (selectedUserId.value) {
    fetchUserDetail(selectedUserId.value)
  }
}

function handleClearLocalNoteSearch() {
  localNoteKeyword.value = ''
  noteCurrentPage.value = 1
  if (selectedUserId.value) {
    fetchUserDetail(selectedUserId.value)
  }
}

function openMainLink() {
  if (mainLink.value && isSafeUrl(mainLink.value.mainUrl)) {
    window.open(mainLink.value.mainUrl, '_blank', 'noopener,noreferrer')
  }
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

function editNote(note: NoteItem) {
  if (note.noteType === 2) {
    router.push(`/note/editor/${note.id}`)
  } else {
    editingNote.value = note
    showPublishDialog.value = true
  }
}

async function handleDeleteNote(note: NoteItem) {
  try {
    await ElMessageBox.confirm('确定要删除该笔记吗？此操作无法恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteNote(note.id)
    ElMessage.success('删除成功')
    if (selectedUserId.value) {
      fetchUserDetail(selectedUserId.value)
    }
  } catch {
    // 用户取消
  }
}

function incrementView(note: NoteItem) {
  incrementNoteView(note.id).catch(() => {})
}

function onPublishSuccess() {
  showPublishDialog.value = false
  editingNote.value = null
  if (selectedUserId.value) {
    fetchUserDetail(selectedUserId.value)
  } else {
    fetchRecommendedNotes()
    fetchUsers()
  }
}

async function handleUserCommand(command: string) {
  if (command === 'myNotes') {
    if (userInfo.value?.id) {
      selectUser(userInfo.value)
    }
  } else if (command === 'favorites') {
    router.push('/favorites')
  } else if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}

// ========== 表格项收藏 ==========
const favoriteMap = ref<Record<number, boolean>>({})

async function loadFavorites() {
  if (!isLoggedIn.value) return
  favoriteMap.value = {}
  for (const note of noteList.value) {
    if (!note.id) continue
    try {
      const res = await checkFavorite(note.id)
      favoriteMap.value[note.id] = res.data
    } catch { favoriteMap.value[note.id] = false }
  }
}

async function toggleFavorite(row: NoteItem) {
  if (!isLoggedIn.value) return
  try {
    if (favoriteMap.value[row.id] || row.isFavorited) {
      await removeFavorite(row.id)
      favoriteMap.value[row.id] = false
      row.isFavorited = false
      row.favoriteCount = Math.max(0, (row.favoriteCount || 1) - 1)
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(row.id)
      favoriteMap.value[row.id] = true
      row.isFavorited = true
      row.favoriteCount = (row.favoriteCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch { /* handled */ }
}

async function togglePin(row: NoteItem) {
  try {
    if (row.isPinned === 1) {
      await unpinNote(row.id)
      ElMessage.success('已取消置顶')
    } else {
      await pinNote(row.id)
      ElMessage.success('已置顶')
    }
    if (selectedUserId.value) {
      fetchUserDetail(selectedUserId.value)
    }
  } catch { /* handled */ }
}

function handleTableCommand(command: string, row: NoteItem) {
  if (command === 'togglePin') {
    togglePin(row)
  } else if (command === 'delete') {
    handleDeleteNote(row)
  }
}
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: #f8fafc;
}

/* 顶部操作区 */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 50;
  flex-shrink: 0;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.logo {
  font-size: 19px;
  font-weight: 700;
  color: #16856f;
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s, transform 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.logo:hover {
  opacity: 0.9;
}

.search-group {
  display: flex;
  gap: 10px;
  flex: 1;
  max-width: 520px;
}

.search-author {
  max-width: 180px;
}

.search-note {
  flex: 1;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username-text {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.mobile-menu-btn {
  display: none;
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 100;
  width: 48px;
  height: 48px;
}

/* 主布局 */
.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  height: calc(100vh - 60px);
}

/* 左侧导航栏 */
.sidebar {
  width: 250px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  height: 48px;
  box-sizing: border-box;
}

.sidebar.collapsed .sidebar-header {
  justify-content: center;
  padding: 12px 6px;
}

.sidebar-header h3 {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
  margin: 0;
}

.sidebar-scrollable {
  flex: 1;
  overflow-y: auto;
  padding: 10px 12px;
}

.sidebar.collapsed .sidebar-scrollable {
  padding: 8px 4px;
}

.sidebar-sec-title {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.workbench-nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.workbench-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s ease;
}

.workbench-item:hover {
  background: #f1f5f9;
  color: #16856f;
}

.workbench-item.active {
  background: #eaf7f2;
  color: #16856f;
  font-weight: 600;
}

.sidebar-collapsed-nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding-top: 6px;
}

.collapsed-icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.collapsed-icon-btn:hover,
.collapsed-icon-btn.active {
  background: #eaf7f2;
  color: #16856f;
}

/* 热门标签 */
.tag-chips-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sidebar-tag-chip {
  display: inline-block;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 12px;
  background: #f1f5f9;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.sidebar-tag-chip:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.sidebar-tag-chip.active {
  background: #4f46e5;
  color: #ffffff;
  font-weight: 500;
}

/* 创作者列表 */
.user-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.user-item:hover {
  background: #f1f5f9;
}

.user-item.active {
  background: #eaf7f2;
}

.user-item.collapsed-item {
  justify-content: center;
  padding: 6px 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.self-tag {
  font-size: 10px;
  height: 16px;
  padding: 0 4px;
  line-height: 14px;
}

.user-meta {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
  display: flex;
  gap: 4px;
}

.pagination-wrapper {
  padding: 6px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: center;
}

/* 主内容与右侧看板外层包裹 */
.content-wrapper {
  display: flex;
  flex: 1;
  overflow: hidden;
  background: #f8fafc;
}

/* 右侧内容滚动区 */
.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* 顶部模式切换栏 */
.content-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 14px 18px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.content-mode-eyebrow {
  font-size: 11px;
  font-weight: 600;
  color: #16856f;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.content-mode-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  margin-top: 3px;
}

.mode-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.refresh-batch-btn {
  font-weight: 600;
  border-radius: 8px;
}

/* ==================== 智能推荐流样式 ==================== */
.recommend-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommend-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 10px 16px;
}

.tag-filter-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.filter-label {
  font-weight: 600;
  color: #1e3a8a;
}

.filter-hint {
  font-size: 12px;
  color: #3b82f6;
}

/* 6条卡片网格系统 (响应式 2 列) */
.recommend-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 18px;
}

.recommend-note-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 18px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.recommend-note-card:hover {
  transform: translateY(-2px);
  border-color: #93c5fd;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.card-badge-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.heat-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #fff7ed;
  border: 1px solid #ffedd5;
  color: #ea580c;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.recommend-note-title {
  font-size: 16px;
  font-weight: 650;
  color: #0f172a;
  margin: 0 0 8px;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-note-card:hover .recommend-note-title {
  color: #2563eb;
}

.pinned-tag {
  margin-right: 6px;
}

.recommend-note-desc {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.recommend-tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.card-tag-item {
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.card-tag-item:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.card-tag-item.active {
  background: #4f46e5;
  color: #ffffff;
}

.recommend-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #64748b;
  margin-top: auto;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.card-author {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.card-author:hover .author-name {
  color: #2563eb;
}

.author-name {
  font-weight: 500;
  color: #334155;
}

.meta-time {
  font-size: 11px;
  color: #94a3b8;
  margin-left: 4px;
}

.card-metrics {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-metrics span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

/* 卡片底部操作栏 (适配红心点赞、黄星收藏、分享) */
.recommend-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.action-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

/* 红心点赞按钮适配 */
.heart-btn:hover {
  color: #ef4444;
  border-color: #fca5a5;
  background: #fff5f5;
}

.heart-btn.is-liked {
  color: #ef4444;
  background: #fef2f2;
  border-color: #fecaca;
  font-weight: 600;
}

.heart-btn.is-liked .heart-svg {
  fill: #ef4444;
  stroke: #ef4444;
}

/* 黄星收藏按钮适配 */
.star-btn:hover {
  color: #d97706;
  border-color: #fde68a;
  background: #fffbeb;
}

.star-btn.is-favorited {
  color: #d97706;
  background: #fffbeb;
  border-color: #fde68a;
  font-weight: 600;
}

.star-btn.is-favorited .star-svg {
  fill: #f59e0b;
  stroke: #f59e0b;
}

/* 分享按钮适配 */
.share-btn:hover {
  color: #10b981;
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.read-more-btn {
  margin-left: auto;
  font-weight: 500;
}

.table-action-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  white-space: nowrap;
}

.table-fav-btn {
  padding: 3px 8px;
  font-size: 12px;
}

.table-op-btn {
  padding: 3px 6px;
  font-size: 12px;
}

.table-more-btn {
  padding: 3px 6px;
  color: #64748b;
  font-size: 14px;
}

.table-more-btn:hover {
  color: #4f46e5;
  background-color: #f1f5f9;
}

/* 底部换一批常驻条 */
.recommend-bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  margin-top: 10px;
}

.batch-hint {
  font-size: 13px;
  color: #64748b;
}

/* ==================== 右侧看板部件 ==================== */
.right-widget-sidebar {
  width: 290px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 20px 20px 0;
  overflow-y: auto;
  flex-shrink: 0;
}

.widget-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.widget-header {
  margin-bottom: 12px;
}

.widget-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.dashboard-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.stat-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 4px;
  border-radius: 8px;
  text-align: center;
}

.tile-blue {
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.tile-amber {
  background: #fffbeb;
  border: 1px solid #fef3c7;
}

.tile-purple {
  background: #f5f3ff;
  border: 1px solid #ede9fe;
}

.stat-num {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.target-progress-box {
  background: #f8fafc;
  padding: 8px 10px;
  border-radius: 6px;
}

.target-label {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #475569;
  margin-bottom: 4px;
}

.target-percent {
  font-weight: 600;
  color: #4f46e5;
}

/* 专题列表 */
.topic-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.topic-item {
  padding: 8px 10px;
  background: #f8fafc;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.topic-item:hover {
  background: #eff6ff;
}

.topic-title {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
}

.topic-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #64748b;
  margin-top: 3px;
}

.match-score {
  color: #16a34a;
  font-weight: 500;
}

/* 推荐创作者列表 */
.rec-creator-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.creator-item {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.creator-info {
  flex: 1;
  min-width: 0;
}

.creator-name {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-desc {
  font-size: 11px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-follow-btn {
  padding: 3px 8px;
  font-size: 11px;
}

.creator-card-footer {
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 12px;
}

/* ==================== 关注流与热门榜通用样式 ==================== */
.social-section {
  min-height: 420px;
  padding: 20px;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.social-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.social-section-header h2 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.social-section-header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}

.social-note-list,
.trending-list {
  display: grid;
  gap: 14px;
}

.social-note-card,
.trending-card {
  cursor: pointer;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  padding: 16px;
  transition: all 0.2s ease;
}

.social-note-card:hover,
.trending-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.social-note-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.social-note-author > div {
  display: flex;
  flex-direction: column;
}

.social-note-author strong {
  font-size: 13px;
  color: #1e293b;
}

.social-note-author span {
  font-size: 11px;
  color: #94a3b8;
}

.social-note-card > .new-tag {
  float: right;
  margin-top: -24px;
}

.social-note-card h3,
.trending-card h3 {
  margin: 12px 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.social-note-summary {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.social-note-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.social-note-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.social-note-stats span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.trending-card {
  display: flex;
  gap: 16px;
}

.trend-rank {
  width: 32px;
  font-size: 20px;
  font-weight: 700;
  color: #94a3b8;
  flex-shrink: 0;
}

.trend-rank.top {
  color: #e6a23c;
}

.trending-main {
  flex: 1;
  min-width: 0;
}

.social-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ==================== 用户主页与笔记列表 ==================== */
.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 16px;
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  flex-wrap: wrap;
  gap: 14px;
}

.user-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.clickable-avatar:hover {
  transform: scale(1.05);
}

.user-details-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.user-details h2 {
  font-size: 18px;
  color: #0f172a;
  font-weight: 700;
  margin: 0;
}

.clickable-username:hover {
  color: #2563eb;
}

.user-bio-wrapper {
  display: inline-flex;
  align-items: center;
}

.bio-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 14px;
  font-size: 12px;
  color: #475569;
  background: #f1f5f9;
}

.bio-badge.editable-bio {
  cursor: pointer;
  border: 1px dashed #cbd5e1;
}

.bio-badge.editable-bio:hover {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #2563eb;
}

.bio-edit-inline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.bio-input {
  width: 220px;
}

.user-stats {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  display: flex;
  align-items: center;
}

.stat-divider {
  margin: 0 6px;
  color: #cbd5e1;
}

.main-link-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #16856f;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 16px;
  transition: all 0.2s;
}

.main-link-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
}

.main-link-icon {
  width: 40px;
  height: 40px;
  background: #eaf7f2;
  color: #16856f;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.main-link-info {
  flex: 1;
  min-width: 0;
}

.main-link-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.main-link-url {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notes-section {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.notes-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}

.notes-toolbar h3 {
  font-size: 15px;
  color: #0f172a;
  font-weight: 600;
  margin: 0;
}

.note-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.note-title {
  font-weight: 500;
  color: #1e293b;
}

.clickable-title:hover {
  color: #2563eb;
  cursor: pointer;
}

.note-article-summary {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
}

.note-article-summary:hover {
  color: #2563eb;
}

.summary-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #2563eb;
  text-decoration: none;
  font-size: 12px;
}

.note-link:hover {
  text-decoration: underline;
}

.note-location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  font-size: 12px;
}

.note-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

/* ==================== 外链卡片预览胶囊 ==================== */
.card-external-link-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 6px 12px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  color: #2563eb;
  font-size: 12px;
  max-width: 100%;
  cursor: pointer;
  transition: all 0.2s ease;
}

.card-external-link-pill:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.card-external-link-pill .url-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.card-external-link-pill .url-open-icon {
  flex-shrink: 0;
  font-size: 13px;
}

/* ==================== 搜索结果视图 ==================== */
.search-result-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-result-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  padding: 12px 18px;
  gap: 16px;
  flex-wrap: wrap;
}

.search-notice-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.search-main-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #166534;
}

.search-desc-text {
  font-size: 13px;
  color: #374151;
}

.search-desc-text strong {
  color: #16a34a;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .top-bar {
    padding: 0 14px;
  }
  .search-group {
    display: none;
  }
  .mobile-menu-btn {
    display: flex;
  }
  .sidebar {
    position: fixed;
    top: 60px;
    left: -260px;
    bottom: 0;
    z-index: 99;
    transition: left 0.3s ease;
  }
  .sidebar.drawer-open {
    left: 0;
  }
  .drawer-mask {
    position: fixed;
    top: 60px;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 98;
  }
  .content-area {
    padding: 14px;
  }
  .content-mode-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .mode-bar-right {
    flex-direction: column;
    align-items: stretch;
  }
  .mode-bar-right .el-radio-group {
    width: 100%;
    display: flex;
  }
  .mode-bar-right .el-radio-button {
    flex: 1;
  }
  .recommend-cards-grid {
    grid-template-columns: 1fr;
  }
  .right-widget-sidebar {
    display: none;
  }
}
</style>
