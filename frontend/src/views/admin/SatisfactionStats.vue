<template>
  <div class="satisfaction-stats-container">
    <el-page-header @back="goBack" content="满意度统计" />
    
    <div class="content-wrapper">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">平均满意度</div>
            <div class="stat-value">
              {{ averageSatisfaction.toFixed(1) }}<span class="stat-unit">分</span>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">总评价数</div>
            <div class="stat-value">{{ totalReviews }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">5星评价</div>
            <div class="stat-value">{{ fiveStarReviews }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">4星评价</div>
            <div class="stat-value">{{ fourStarReviews }}</div>
          </div>
        </el-col>
      </el-row>
      
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">3星评价</div>
            <div class="stat-value">{{ threeStarReviews }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">2星评价</div>
            <div class="stat-value">{{ twoStarReviews }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">1星评价</div>
            <div class="stat-value">{{ oneStarReviews }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">评价率</div>
            <div class="stat-value">
              {{ totalOrders > 0 ? ((totalReviews / totalOrders) * 100).toFixed(1) : 0 }}<span class="stat-unit">%</span>
            </div>
          </div>
        </el-col>
      </el-row>
      
      <div class="chart-container" style="margin-top: 30px;">
        <div class="chart-header">
          <h3>满意度趋势</h3>
          <el-select
            v-model="trendDays"
            style="width: 120px"
            @change="loadTrendData"
          >
            <el-option label="最近7天" :value="7" />
            <el-option label="最近30天" :value="30" />
            <el-option label="最近90天" :value="90" />
            <el-option label="最近180天" :value="180" />
          </el-select>
        </div>
        <div
          ref="chartContainer"
          v-loading="trendLoading"
          style="width: 100%; height: 400px;"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElPageHeader, ElRow, ElCol, ElEmpty, ElMessage, ElSelect, ElOption } from 'element-plus';
import { orderApi } from '@/api/order';
import * as echarts from 'echarts';

const router = useRouter();

// 统计数据
const averageSatisfaction = ref(0);
const totalReviews = ref(0);
const fiveStarReviews = ref(0);
const fourStarReviews = ref(0);
const threeStarReviews = ref(0);
const twoStarReviews = ref(0);
const oneStarReviews = ref(0);
const totalOrders = ref(0);

// 趋势图表相关
const chartContainer = ref<HTMLElement | null>(null);
const trendDays = ref(30);
const trendLoading = ref(false);
let chartInstance: echarts.ECharts | null = null;
let resizeHandler: (() => void) | null = null;

// 刷新定时器
let refreshInterval: number | null = null;

// 加载满意度统计数据
const loadSatisfactionStats = async () => {
  try {
    const res = await orderApi.getSatisfactionStats();
    if (res && res.code === 200 && res.data) {
      const stats = res.data;
      averageSatisfaction.value = stats.averageSatisfaction || 0;
      totalReviews.value = stats.totalReviews || 0;
      fiveStarReviews.value = stats.fiveStarReviews || 0;
      fourStarReviews.value = stats.fourStarReviews || 0;
      threeStarReviews.value = stats.threeStarReviews || 0;
      twoStarReviews.value = stats.twoStarReviews || 0;
      oneStarReviews.value = stats.oneStarReviews || 0;
    }
    
    // 获取总订单数（用于计算评价率）
    const orderRes = await orderApi.getOrderList({ pageNum: 1, pageSize: 1 });
    if (orderRes && orderRes.code === 200 && orderRes.data) {
      totalOrders.value = orderRes.data.total || 0;
    }
  } catch (error: any) {
    console.error("加载满意度统计失败:", error);
    if (error?.response?.status !== 401) {
      ElMessage.error("加载满意度统计失败，请稍后重试");
    }
  }
};

// 监听评价提交事件
const handleReviewSubmitted = () => {
  console.log("收到评价提交事件，刷新满意度统计");
  loadSatisfactionStats();
  loadTrendData();
};

// 加载趋势数据
const loadTrendData = async () => {
  if (!chartContainer.value) return;
  
  trendLoading.value = true;
  try {
    const res = await orderApi.getSatisfactionTrend(trendDays.value);
    if (res && res.code === 200 && res.data) {
      const { dateLabels, averageRatings, reviewCounts } = res.data;
      renderChart(dateLabels, averageRatings, reviewCounts);
    } else {
      ElMessage.error('加载趋势数据失败');
    }
  } catch (error: any) {
    console.error('加载趋势数据失败:', error);
    ElMessage.error('加载趋势数据失败，请稍后重试');
  } finally {
    trendLoading.value = false;
  }
};

// 渲染图表
const renderChart = (dateLabels: string[], averageRatings: number[], reviewCounts: number[]) => {
  if (!chartContainer.value) return;
  
  // 初始化图表实例
  if (!chartInstance) {
    chartInstance = echarts.init(chartContainer.value);
  }
  
  // 格式化日期标签（只显示月-日）
  const formattedLabels = dateLabels.map(date => {
    const d = new Date(date);
    return `${d.getMonth() + 1}-${d.getDate()}`;
  });
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
      },
      formatter: (params: any) => {
        let result = `<div style="margin-bottom: 8px;"><strong>${params[0].axisValue}</strong></div>`;
        params.forEach((item: any) => {
          result += `<div style="margin: 4px 0;">
            <span style="display: inline-block; width: 10px; height: 10px; background: ${item.color}; border-radius: 50%; margin-right: 6px;"></span>
            ${item.seriesName}: <strong>${item.value}</strong>
          </div>`;
        });
        return result;
      },
    },
    legend: {
      data: ['平均满意度', '评价数量'],
      top: 10,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: formattedLabels,
      axisLabel: {
        rotate: 45,
        interval: Math.floor(formattedLabels.length / 10), // 根据数据量自动调整显示间隔
      },
    },
    yAxis: [
      {
        type: 'value',
        name: '平均满意度',
        min: 0,
        max: 5,
        position: 'left',
        axisLabel: {
          formatter: '{value}分',
        },
      },
      {
        type: 'value',
        name: '评价数量',
        min: 0,
        position: 'right',
        axisLabel: {
          formatter: '{value}条',
        },
      },
    ],
    series: [
      {
        name: '平均满意度',
        type: 'line',
        smooth: true,
        data: averageRatings,
        yAxisIndex: 0,
        itemStyle: {
          color: '#409eff',
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.05)' },
            ],
          },
        },
        markLine: {
          data: [
            { type: 'average', name: '平均值' },
          ],
        },
      },
      {
        name: '评价数量',
        type: 'bar',
        data: reviewCounts,
        yAxisIndex: 1,
        itemStyle: {
          color: '#67c23a',
        },
      },
    ],
  };
  
  chartInstance.setOption(option);
  
  // 响应式调整
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler);
  }
  resizeHandler = () => {
    chartInstance?.resize();
  };
  window.addEventListener('resize', resizeHandler);
};

// 返回上一页
const goBack = () => {
  router.back();
};

onMounted(async () => {
  await loadSatisfactionStats();
  
  // 等待DOM渲染完成后再加载图表
  await nextTick();
  await loadTrendData();
  
  // 监听评价提交事件
  window.addEventListener('review-submitted', handleReviewSubmitted);
  
  // 每30秒自动刷新一次统计数据
  refreshInterval = setInterval(() => {
    loadSatisfactionStats();
    loadTrendData();
  }, 30000);
});

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('review-submitted', handleReviewSubmitted);
  
  // 清除定时器
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
  
  // 移除resize监听
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler);
    resizeHandler = null;
  }
  
  // 销毁图表实例
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});
</script>

<style scoped lang="scss">
// 统一配色变量
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$info-color: #909399;
$bg-light: #f5f7fa;
$bg-white: #ffffff;
$text-primary: #303133;
$text-secondary: #606266;
$text-placeholder: #909399;
$border-color: #dcdfe6;
$border-color-light: #ebeef5;

.satisfaction-stats-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 20px;
  min-height: calc(100vh - 100px);
  background: $bg-light;
  
  .content-wrapper {
    margin-top: 20px;
  }
  
  .stat-card {
    background: $bg-white;
    padding: 24px;
    border-radius: 12px;
    border: 1px solid $border-color-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    text-align: center;
    transition: all 0.3s ease;
    height: 100%;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      border-color: $primary-color;
    }
    
    .stat-title {
      font-size: 14px;
      color: $text-secondary;
      margin-bottom: 16px;
      font-weight: 500;
    }
    
    .stat-value {
      font-size: 36px;
      font-weight: 700;
      color: $text-primary;
      
      .stat-unit {
        font-size: 18px;
        margin-left: 4px;
        color: $text-placeholder;
        font-weight: 500;
      }
    }
  }
  
  .chart-container {
    background: $bg-white;
    padding: 24px;
    border-radius: 12px;
    border: 1px solid $border-color-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    margin-top: 30px;
    
    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid $border-color-light;
      
      h3 {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: $text-primary;
      }

      .el-select {
        border-radius: 8px;
      }
    }
  }
}
</style>

