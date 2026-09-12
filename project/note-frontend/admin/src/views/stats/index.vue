<template>
  <div class="stats-container">
    <!-- 顶部核心指标概览卡片 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="6" :md="4" v-for="item in summaryCards" :key="item.key">
        <el-card shadow="hover" class="summary-card" :body-style="{ padding: '16px' }">
          <div class="summary-content">
            <div class="summary-icon-box" :style="{ backgroundColor: item.bgColor, color: item.color }">
              <el-icon :size="22">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div class="summary-info">
              <div class="summary-label">{{ item.label }}</div>
              <div class="summary-value">{{ item.value }}</div>
              <div class="summary-sub" v-if="item.sub">{{ item.sub }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 1：趋势对比 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span class="header-title">用户注册增长趋势</span>
              <el-select v-model="registrationDays" size="small" style="width: 100px;" @change="fetchRegistrationStats">
                <el-option label="近7天" :value="7" />
                <el-option label="近30天" :value="30" />
                <el-option label="近90天" :value="90" />
              </el-select>
            </div>
          </template>
          <div ref="registrationChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span class="header-title">笔记内容发布趋势</span>
              <el-select v-model="publishDays" size="small" style="width: 100px;" @change="fetchPublishStats">
                <el-option label="近7天" :value="7" />
                <el-option label="近30天" :value="30" />
                <el-option label="近90天" :value="90" />
              </el-select>
            </div>
          </template>
          <div ref="publishChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 2：笔记形态与公开性分析 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span class="header-title">笔记形态分布 (Markdown形式 vs 仅链接形式 vs 非链接形式)</span>
            </div>
          </template>
          <div ref="linkTypeChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span class="header-title">笔记公开性与权限分布</span>
            </div>
          </template>
          <div ref="visibilityChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 3：用户笔记量分布与社区互动分布 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span class="header-title">用户笔记拥有量阶梯分布</span>
            </div>
          </template>
          <div ref="distributionChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span class="header-title">平台互动与社交生态分布</span>
            </div>
          </template>
          <div ref="interactionChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 4：热门排行榜与平台综合趋势 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span class="header-title">热门笔记浏览量排行 Top 20</span>
            </div>
          </template>
          <div ref="viewRankChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span class="header-title">平台综合动态走势 (近30天)</span>
            </div>
          </template>
          <div ref="platformChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import {
  User, Document, Edit, View, ChatDotRound, Star
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getPlatformSummary, getRegistrationStats, getPublishStats, getDistributionStats,
  getViewRanking, getLinkTypeStats, getVisibilityStats, getInteractionStats, getPlatformTrends,
} from '@/api/admin'

const registrationDays = ref(7)
const publishDays = ref(7)

// 顶部卡片数据
const summaryCards = ref([
  { key: 'users', label: '总用户数', value: 0, sub: '', icon: User, color: '#409EFF', bgColor: '#ecf5ff' },
  { key: 'notes', label: '总笔记数', value: 0, sub: '', icon: Document, color: '#67C23A', bgColor: '#f0f9eb' },
  { key: 'original', label: 'Markdown笔记', value: 0, sub: '', icon: Edit, color: '#E6A23C', bgColor: '#fdf6ec' },
  { key: 'views', label: '总浏览量', value: 0, sub: '', icon: View, color: '#909399', bgColor: '#f4f4f5' },
  { key: 'comments', label: '互动评论数', value: 0, sub: '', icon: ChatDotRound, color: '#F56C6C', bgColor: '#fef0f0' },
  { key: 'favorites', label: '总收藏数', value: 0, sub: '', icon: Star, color: '#8e44ad', bgColor: '#fbf5fd' },
])

const registrationChartRef = ref<HTMLElement>()
const publishChartRef = ref<HTMLElement>()
const linkTypeChartRef = ref<HTMLElement>()
const visibilityChartRef = ref<HTMLElement>()
const distributionChartRef = ref<HTMLElement>()
const interactionChartRef = ref<HTMLElement>()
const viewRankChartRef = ref<HTMLElement>()
const platformChartRef = ref<HTMLElement>()

let registrationChart: echarts.ECharts | null = null
let publishChart: echarts.ECharts | null = null
let linkTypeChart: echarts.ECharts | null = null
let visibilityChart: echarts.ECharts | null = null
let distributionChart: echarts.ECharts | null = null
let interactionChart: echarts.ECharts | null = null
let viewRankChart: echarts.ECharts | null = null
let platformChart: echarts.ECharts | null = null

interface TrendItem { date: string; count: number }
interface DistributionItem { range: string; count: number }
interface NoteItem { id: number; title: string; viewCount: number; username: string }

// 1. 获取汇总数据
const fetchSummary = async () => {
  try {
    const res = await getPlatformSummary()
    const data = res.data || {}
    summaryCards.value[0].value = data.totalUsers || 0
    summaryCards.value[0].sub = `今日活跃: ${data.activeUsersToday || 0}`
    summaryCards.value[1].value = data.totalNotes || 0
    summaryCards.value[1].sub = `今日新增: ${data.newNotesToday || 0}`
    summaryCards.value[2].value = data.markdownNotes || data.originalNotes || 0
    summaryCards.value[2].sub = `仅链接: ${data.linkNotes || 0} | 非链接: ${data.nonLinkNotes || 0}`
    summaryCards.value[3].value = data.totalViews || 0
    summaryCards.value[3].sub = `知识库: ${data.totalMainLinks || 0}`
    summaryCards.value[4].value = data.totalComments || 0
    summaryCards.value[4].sub = `关注关系: ${data.totalFollows || 0}`
    summaryCards.value[5].value = data.totalFavorites || 0
    summaryCards.value[5].sub = `公开笔记: ${data.publicNotes || 0}`
  } catch (e) {
    console.error('获取概览统计失败', e)
  }
}

// 2. 用户注册趋势
const fetchRegistrationStats = async () => {
  try {
    const res = await getRegistrationStats(registrationDays.value)
    const data: TrendItem[] = res.data || []
    if (registrationChart) {
      registrationChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: data.map(d => d.date), boundaryGap: false },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          name: '新注册用户',
          type: 'line',
          data: data.map(d => d.count),
          smooth: true,
          itemStyle: { color: '#409EFF' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.4)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
            ])
          }
        }],
      })
    }
  } catch (e) { /* handled */ }
}

// 3. 笔记发布趋势
const fetchPublishStats = async () => {
  try {
    const res = await getPublishStats(publishDays.value)
    const data: TrendItem[] = res.data || []
    if (publishChart) {
      publishChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: data.map(d => d.date), boundaryGap: false },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          name: '笔记发布数',
          type: 'line',
          data: data.map(d => d.count),
          smooth: true,
          itemStyle: { color: '#67C23A' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(103, 194, 58, 0.4)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.02)' }
            ])
          }
        }],
      })
    }
  } catch (e) { /* handled */ }
}

// 4. 笔记形态分布 (Markdown形式 vs 仅链接形式 vs 非链接形式)
const fetchLinkTypeStats = async () => {
  try {
    const res = await getLinkTypeStats()
    const data: DistributionItem[] = res.data || []
    if (linkTypeChart) {
      linkTypeChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}篇 ({d}%)' },
        legend: { bottom: '5%', left: 'center' },
        color: ['#409EFF', '#67C23A', '#E6A23C'],
        series: [{
          name: '笔记形态',
          type: 'pie',
          radius: ['45%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: true, formatter: '{b}: {c}篇' },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' }
          },
          data: data.map(d => ({ name: d.range, value: d.count })),
        }],
      })
    }
  } catch (e) { /* handled */ }
}

// 5. 笔记公开性分布
const fetchVisibilityStats = async () => {
  try {
    const res = await getVisibilityStats()
    const data: DistributionItem[] = res.data || []
    if (visibilityChart) {
      visibilityChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}篇 ({d}%)' },
        legend: { bottom: '5%', left: 'center' },
        color: ['#409EFF', '#909399'],
        series: [{
          name: '公开权限',
          type: 'pie',
          radius: ['45%', '70%'],
          itemStyle: {
            borderRadius: 6,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: true, formatter: '{b}: {c}篇' },
          data: data.map(d => ({ name: d.range, value: d.count })),
        }],
      })
    }
  } catch (e) { /* handled */ }
}

// 6. 用户笔记拥有量分布
const fetchDistributionStats = async () => {
  try {
    const res = await getDistributionStats()
    const data: DistributionItem[] = res.data || []
    if (distributionChart) {
      distributionChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: data.map(d => d.range) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          name: '用户数',
          type: 'bar',
          barWidth: '40%',
          data: data.map(d => d.count),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#409EFF' },
              { offset: 1, color: '#79bbff' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        }],
      })
    }
  } catch (e) { /* handled */ }
}

// 7. 平台互动分布
const fetchInteractionStats = async () => {
  try {
    const res = await getInteractionStats()
    const data: DistributionItem[] = res.data || []
    if (interactionChart) {
      interactionChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: data.map(d => d.range) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          name: '总量',
          type: 'bar',
          barWidth: '40%',
          data: data.map(d => d.count),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#8e44ad' },
              { offset: 1, color: '#c39bd3' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        }],
      })
    }
  } catch (e) { /* handled */ }
}

// 8. 浏览量排行 Top 20
const fetchViewRanking = async () => {
  try {
    const res = await getViewRanking(20)
    const data: NoteItem[] = res.data || []
    const reversed = [...data].reverse()
    if (viewRankChart) {
      viewRankChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
        yAxis: {
          type: 'category',
          data: reversed.map(d => (d.title && d.title.length > 12 ? d.title.substring(0, 12) + '...' : (d.title || '未知'))),
          axisLabel: { interval: 0 }
        },
        xAxis: { type: 'value', minInterval: 1 },
        series: [{
          name: '浏览量',
          type: 'bar',
          barWidth: '55%',
          data: reversed.map(d => d.viewCount),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
              { offset: 0, color: '#E6A23C' },
              { offset: 1, color: '#f56c6c' }
            ]),
            borderRadius: [0, 4, 4, 0]
          },
          label: { show: true, position: 'right', formatter: '{c}次' }
        }],
      })
    }
  } catch (e) { /* handled */ }
}

// 9. 平台综合趋势走势
const fetchPlatformTrends = async () => {
  try {
    const [platformRes, regRes] = await Promise.all([
      getPlatformTrends(30),
      getRegistrationStats(30)
    ])
    const noteTrends: TrendItem[] = platformRes.data || []
    const userTrends: TrendItem[] = regRes.data || []

    if (platformChart) {
      platformChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
        legend: { data: ['笔记新增', '用户注册'], bottom: '2%' },
        grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
        xAxis: { type: 'category', data: noteTrends.map(d => d.date), boundaryGap: false },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          {
            name: '笔记新增',
            type: 'line',
            data: noteTrends.map(d => d.count),
            smooth: true,
            itemStyle: { color: '#67C23A' },
            areaStyle: { opacity: 0.15 }
          },
          {
            name: '用户注册',
            type: 'line',
            data: userTrends.map(d => d.count),
            smooth: true,
            itemStyle: { color: '#409EFF' },
            areaStyle: { opacity: 0.15 }
          }
        ],
      })
    }
  } catch (e) { /* handled */ }
}

const handleResize = () => {
  registrationChart?.resize()
  publishChart?.resize()
  linkTypeChart?.resize()
  visibilityChart?.resize()
  distributionChart?.resize()
  interactionChart?.resize()
  viewRankChart?.resize()
  platformChart?.resize()
}

const initCharts = () => {
  if (registrationChartRef.value) registrationChart = echarts.init(registrationChartRef.value)
  if (publishChartRef.value) publishChart = echarts.init(publishChartRef.value)
  if (linkTypeChartRef.value) linkTypeChart = echarts.init(linkTypeChartRef.value)
  if (visibilityChartRef.value) visibilityChart = echarts.init(visibilityChartRef.value)
  if (distributionChartRef.value) distributionChart = echarts.init(distributionChartRef.value)
  if (interactionChartRef.value) interactionChart = echarts.init(interactionChartRef.value)
  if (viewRankChartRef.value) viewRankChart = echarts.init(viewRankChartRef.value)
  if (platformChartRef.value) platformChart = echarts.init(platformChartRef.value)
}

onMounted(async () => {
  await fetchSummary()
  await nextTick()
  initCharts()
  fetchRegistrationStats()
  fetchPublishStats()
  fetchLinkTypeStats()
  fetchVisibilityStats()
  fetchDistributionStats()
  fetchInteractionStats()
  fetchViewRanking()
  fetchPlatformTrends()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  registrationChart?.dispose()
  publishChart?.dispose()
  linkTypeChart?.dispose()
  visibilityChart?.dispose()
  distributionChart?.dispose()
  interactionChart?.dispose()
  viewRankChart?.dispose()
  platformChart?.dispose()
})
</script>

<style scoped>
.stats-container {
  padding: 16px 20px;
}

.summary-row {
  margin-bottom: 20px;
}

.summary-card {
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.summary-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.summary-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.summary-info {
  overflow: hidden;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.3;
}

.summary-sub {
  font-size: 12px;
  color: #a8abb2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chart-row {
  margin-bottom: 20px;
}

.chart {
  height: 340px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}
</style>
