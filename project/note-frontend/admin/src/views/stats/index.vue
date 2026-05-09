<template>
  <div class="stats-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span>用户注册趋势</span>
              <el-select v-model="registrationDays" size="small" style="width: 100px;" @change="fetchRegistrationStats">
                <el-option label="7天" :value="7" />
                <el-option label="30天" :value="30" />
                <el-option label="90天" :value="90" />
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
              <span>笔记发布趋势</span>
              <el-select v-model="publishDays" size="small" style="width: 100px;" @change="fetchPublishStats">
                <el-option label="7天" :value="7" />
                <el-option label="30天" :value="30" />
                <el-option label="90天" :value="90" />
              </el-select>
            </div>
          </template>
          <div ref="publishChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>用户笔记数量分布</span></template>
          <div ref="distributionChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>浏览量排行Top20</span></template>
          <div ref="viewRankChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>链接类型占比</span></template>
          <div ref="linkTypeChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>平台趋势</span></template>
          <div ref="platformChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getRegistrationStats, getPublishStats, getDistributionStats,
  getViewRanking, getLinkTypeStats, getPlatformTrends,
} from '@/api/admin'

const registrationDays = ref(7)
const publishDays = ref(7)
const registrationChartRef = ref<HTMLElement>()
const publishChartRef = ref<HTMLElement>()
const distributionChartRef = ref<HTMLElement>()
const viewRankChartRef = ref<HTMLElement>()
const linkTypeChartRef = ref<HTMLElement>()
const platformChartRef = ref<HTMLElement>()

let registrationChart: echarts.ECharts | null = null
let publishChart: echarts.ECharts | null = null
let distributionChart: echarts.ECharts | null = null
let viewRankChart: echarts.ECharts | null = null
let linkTypeChart: echarts.ECharts | null = null
let platformChart: echarts.ECharts | null = null

interface TrendItem { date: string; count: number }
interface DistributionItem { range: string; count: number }
interface NoteItem { id: number; title: string; viewCount: number; username: string }

const fetchRegistrationStats = async () => {
  try {
    const res = await getRegistrationStats(registrationDays.value)
    const data: TrendItem[] = res.data
    if (registrationChart) {
      registrationChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.map(d => d.date) },
        yAxis: { type: 'value' },
        series: [{ name: '注册数', type: 'line', data: data.map(d => d.count), smooth: true, areaStyle: { opacity: 0.15 } }],
      })
    }
  } catch (e) { /* handled */ }
}

const fetchPublishStats = async () => {
  try {
    const res = await getPublishStats(publishDays.value)
    const data: TrendItem[] = res.data
    if (publishChart) {
      publishChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.map(d => d.date) },
        yAxis: { type: 'value' },
        series: [{ name: '发布数', type: 'line', data: data.map(d => d.count), smooth: true, areaStyle: { opacity: 0.15 } }],
      })
    }
  } catch (e) { /* handled */ }
}

const fetchDistributionStats = async () => {
  try {
    const res = await getDistributionStats()
    const data: DistributionItem[] = res.data
    if (distributionChart) {
      distributionChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.map(d => d.range) },
        yAxis: { type: 'value' },
        series: [{ name: '用户数', type: 'bar', data: data.map(d => d.count), itemStyle: { color: '#409EFF' } }],
      })
    }
  } catch (e) { /* handled */ }
}

const fetchViewRanking = async () => {
  try {
    const res = await getViewRanking(20)
    const data: NoteItem[] = res.data
    const reversed = [...data].reverse()
    if (viewRankChart) {
      viewRankChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        yAxis: { type: 'category', data: reversed.map(d => d.title?.substring(0, 15) || '未知') },
        xAxis: { type: 'value' },
        series: [{ name: '浏览量', type: 'bar', data: reversed.map(d => d.viewCount), itemStyle: { color: '#E6A23C' } }],
      })
    }
  } catch (e) { /* handled */ }
}

const fetchLinkTypeStats = async () => {
  try {
    const res = await getLinkTypeStats()
    const data: DistributionItem[] = res.data
    if (linkTypeChart) {
      linkTypeChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: '0' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          data: data.map(d => ({ name: d.range, value: d.count })),
          label: { formatter: '{b}: {d}%' },
        }],
      })
    }
  } catch (e) { /* handled */ }
}

const fetchPlatformTrends = async () => {
  try {
    const res = await getPlatformTrends(30)
    const data: TrendItem[] = res.data
    if (platformChart) {
      platformChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.map(d => d.date) },
        yAxis: { type: 'value' },
        series: [{ name: '笔记发布数', type: 'line', data: data.map(d => d.count), smooth: true, areaStyle: { opacity: 0.3 } }],
      })
    }
  } catch (e) { /* handled */ }
}

const initCharts = () => {
  registrationChart = echarts.init(registrationChartRef.value!)
  publishChart = echarts.init(publishChartRef.value!)
  distributionChart = echarts.init(distributionChartRef.value!)
  viewRankChart = echarts.init(viewRankChartRef.value!)
  linkTypeChart = echarts.init(linkTypeChartRef.value!)
  platformChart = echarts.init(platformChartRef.value!)
}

onMounted(async () => {
  await nextTick()
  initCharts()
  fetchRegistrationStats()
  fetchPublishStats()
  fetchDistributionStats()
  fetchViewRanking()
  fetchLinkTypeStats()
  fetchPlatformTrends()
  window.addEventListener('resize', () => {
    registrationChart?.resize()
    publishChart?.resize()
    distributionChart?.resize()
    viewRankChart?.resize()
    linkTypeChart?.resize()
    platformChart?.resize()
  })
})
</script>

<style scoped>
.stats-container {
  padding: 20px;
}
.chart {
  height: 350px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
