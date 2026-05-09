<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="item in statCards" :key="item.key">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">{{ item.label }}</div>
              <div class="stat-value">{{ item.value }}</div>
            </div>
            <el-icon class="stat-icon" :size="40">
              <component :is="item.icon" />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <div ref="userTrendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div ref="noteTrendChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { User, Document, View, Timer } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboardOverview, getUserTrend, getNoteTrend } from '@/api/admin'

const statCards = ref([
  { key: 'totalUsers', label: '总用户数', value: 0, icon: User },
  { key: 'totalNotes', label: '总笔记数', value: 0, icon: Document },
  { key: 'totalViews', label: '总浏览量', value: 0, icon: View },
  { key: 'activeUsers', label: '活跃用户(今日)', value: 0, icon: Timer },
])

const userTrendChartRef = ref<HTMLElement>()
const noteTrendChartRef = ref<HTMLElement>()
let userTrendChart: echarts.ECharts | null = null
let noteTrendChart: echarts.ECharts | null = null

const initCharts = () => {
  if (userTrendChartRef.value) {
    userTrendChart = echarts.init(userTrendChartRef.value)
  }
  if (noteTrendChartRef.value) {
    noteTrendChart = echarts.init(noteTrendChartRef.value)
  }
}

const fetchOverview = async () => {
  try {
    const res = await getDashboardOverview()
    const data = res.data
    statCards.value[0].value = data.totalUsers
    statCards.value[1].value = data.totalNotes
    statCards.value[2].value = data.totalViews
    statCards.value[3].value = data.activeUsers
  } catch (e) {
    // handled by interceptor
  }
}

const fetchTrends = async () => {
  try {
    const [userRes, noteRes] = await Promise.all([getUserTrend(7), getNoteTrend(7)])
    const userData = userRes.data
    const noteData = noteRes.data

    if (userTrendChart) {
      userTrendChart.setOption({
        title: { text: '近7天用户注册趋势', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: userData.dates },
        yAxis: { type: 'value' },
        series: [{ name: '注册数', type: 'line', data: userData.counts, smooth: true, areaStyle: { opacity: 0.3 } }],
      })
    }
    if (noteTrendChart) {
      noteTrendChart.setOption({
        title: { text: '近7天笔记发布趋势', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: noteData.dates },
        yAxis: { type: 'value' },
        series: [{ name: '发布数', type: 'line', data: noteData.counts, smooth: true, areaStyle: { opacity: 0.3 } }],
      })
    }
  } catch (e) {
    // handled by interceptor
  }
}

onMounted(async () => {
  await fetchOverview()
  await nextTick()
  initCharts()
  await fetchTrends()
  window.addEventListener('resize', () => {
    userTrendChart?.resize()
    noteTrendChart?.resize()
  })
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}
.stat-row {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
}
.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stat-icon {
  color: #409eff;
}
.chart {
  height: 350px;
}
</style>
