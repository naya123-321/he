<template>
  <div class="dashboard-container">
    <el-page-header @back="goBack" content="管理员仪表盘" />

    <div class="dashboard-intro">
      <h2>欢迎回来，管理员</h2>
      <p>实时监控系统运行状态和数据统计</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card clickable-card" @click="goToOrderList">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ totalOrders }}</div>
              <div class="stat-label">总订单数</div>
            </div>
            <el-icon class="stat-icon"><ShoppingBag /></el-icon>
          </div>
          <div class="stat-trend" v-if="orderTrend.value > 0">
            <span class="trend-text">
              <el-icon :color="orderTrend.color"><ArrowUp /></el-icon>
              {{ orderTrend.value }}% 较昨日
            </span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card clickable-card" @click="goToPendingOrders">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ pendingOrders }}</div>
              <div class="stat-label">待处理订单</div>
            </div>
            <el-icon class="stat-icon"><Clock /></el-icon>
          </div>
          <div class="stat-trend" v-if="pendingTrend.value > 0">
            <span class="trend-text">
              <el-icon :color="pendingTrend.color"><ArrowDown /></el-icon>
              {{ pendingTrend.value }}% 较昨日
            </span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card clickable-card" @click="goToMemorialList">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ totalMemorials }}</div>
              <div class="stat-label">已审核纪念册</div>
            </div>
            <el-icon class="stat-icon"><Picture /></el-icon>
          </div>
          <div class="stat-trend" v-if="memorialTrend.value > 0">
            <span class="trend-text">
              <el-icon :color="memorialTrend.color"><ArrowUp /></el-icon>
              {{ memorialTrend.value }}% 较昨日
            </span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card clickable-card" @click="goToUserManagement">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ totalUsers }}</div>
              <div class="stat-label">用户总数</div>
            </div>
            <el-icon class="stat-icon"><User /></el-icon>
          </div>
          <div class="stat-trend" v-if="userTrend.value > 0">
            <span class="trend-text">
              <el-icon :color="userTrend.color"><ArrowUp /></el-icon>
              {{ userTrend.value }}% 较昨日
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 订单趋势图 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <h3>订单趋势</h3>
              <el-select
                v-model="chartTimeRange"
                placeholder="选择时间范围"
                size="small"
                @change="handleTimeRangeChange"
              >
                <el-option label="近7天" value="7d" />
                <el-option label="近30天" value="30d" />
                <el-option label="近90天" value="90d" />
              </el-select>
            </div>
          </template>
          <div class="chart-container">
            <div
              ref="orderTrendChartContainer"
              v-loading="orderTrendChartLoading"
              style="width: 100%; height: 300px;"
            ></div>
            <el-empty
              v-if="!orderTrendChartLoading && orderTrendData.dateLabels.length === 0"
              description="暂无订单数据"
              :image-size="80"
            />
          </div>
        </el-card>
      </el-col>

      <!-- ARIMA 未来三天预测 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card forecast-card">
          <template #header>
            <div class="card-header">
              <h3>未来3天预测</h3>
              <el-tag
                :type="forecastMeta.modelType === 'ARIMA' ? 'warning' : 'info'"
                effect="light"
                size="small"
              >
                {{ forecastMeta.modelType === 'ARIMA' ? 'ARIMA' : '降级预测' }}
              </el-tag>
            </div>
          </template>
          <div class="forecast-container" v-loading="forecastLoading">
            <div class="forecast-subtitle">
              {{ forecastMeta.modelType === 'ARIMA' ? '基于ARIMA时间序列算法' : '当前为降级预测（7日均值）' }}
            </div>

            <div class="forecast-items">
              <div v-for="item in forecastItems" :key="item.date" class="forecast-item">
                <div class="fi-date">{{ item.date }}</div>
                <div class="fi-main">
                  <span class="fi-range">{{ item.range }}</span>
                  <span class="fi-unit">单</span>
                </div>
                <div class="fi-conf">置信度{{ item.confidence }}%</div>
              </div>
            </div>

            <div class="forecast-note-title">算法说明：</div>
            <ul class="forecast-notes">
              <li>使用 ARIMA(p,d,q) 模型</li>
              <li>基于过去{{ forecastMeta.historyDays }}天订单数据训练</li>
              <li v-if="forecastMeta.generatedAt">更新时间：{{ forecastMeta.generatedAt }}</li>
            </ul>

            <el-alert
              v-if="forecastMeta.warning && forecastMeta.modelType !== 'ARIMA'"
              class="forecast-alert"
              type="warning"
              :closable="true"
              show-icon
              :title="forecastMeta.warning"
            >
              <template #default>
                <div class="forecast-alert-actions">
                  <el-button size="small" type="primary" @click="retryArima">
                    重试ARIMA
                  </el-button>
                </div>
              </template>
            </el-alert>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 服务类型分布和宠物类型分布 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <h3>服务类型分布</h3>
            </div>
          </template>
          <div class="chart-container">
            <div
              ref="serviceTypeChartContainer"
              v-loading="serviceTypeChartLoading"
              style="width: 100%; height: 320px;"
            ></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <h3>🐾 宠物类型分布</h3>
            </div>
          </template>
          <div class="chart-container">
            <div
              ref="petTypeChartContainer"
              v-loading="petTypeChartLoading"
              style="width: 100%; height: 320px;"
            ></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近订单和待处理任务 -->
    <el-row :gutter="20" class="tasks-row">
      <!-- 最近订单 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="tasks-card">
          <template #header>
            <div class="card-header">
              <h3>最近订单</h3>
              <el-button type="text" @click="viewAllOrders">查看全部</el-button>
            </div>
          </template>

          <div v-if="recentOrders.length === 0" class="empty-data">
            <el-empty description="暂无订单" />
          </div>

          <div v-else class="recent-orders-list">
            <div
              v-for="order in recentOrders"
              :key="order.id"
              class="recent-order-item"
              @click="viewOrderDetail(order)"
            >
              <div class="order-info">
                <div class="order-no">订单号：{{ order.orderNo }}</div>
                <div class="order-time">{{ formatDate(order.createTime) }}</div>
              </div>
              <div class="order-details">
                <div class="pet-info">
                  <span class="pet-name">{{ order.petName }}</span>
                  <span class="pet-type">({{ getPetTypeLabel(order.petType) || order.petType }})</span>
                </div>
                <el-tag :type="getStatusType(order.status)">
                  {{ getStatusText(order.status) }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 待处理任务 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="tasks-card">
          <template #header>
            <div class="card-header">
              <h3>待处理任务</h3>
              <el-button type="text" @click="viewAllTasks">查看全部</el-button>
            </div>
          </template>

          <div v-if="pendingTasks.length === 0" class="empty-data">
            <el-empty description="暂无待处理任务" />
          </div>

          <div v-else class="pending-tasks-list">
            <el-timeline>
              <el-timeline-item
                v-for="task in pendingTasks"
                :key="task.id"
                :timestamp="task.createTime"
                :type="task.type === 'memorial' ? 'primary' : 'warning'"
              >
                <div class="task-content">
                  <div class="task-title">{{ task.title }}</div>
                  <div class="task-description">{{ task.description }}</div>
                  <div class="task-actions">
                    <el-button
                      v-if="task.type === 'memorial'"
                      type="primary"
                      size="small"
                      @click.stop="reviewMemorial(task)"
                    >
                      审核
                    </el-button>
                    <el-button
                      v-if="task.type === 'order'"
                      type="success"
                      size="small"
                      @click.stop="processOrder(task)"
                    >
                      处理
                    </el-button>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { orderApi } from "@/api/order";
import { getUserList } from "@/api/user";
import { memorialApi } from "@/api/memorial";
import * as echarts from "echarts";
import { getPetTypeLabel } from "@/constants/petTypes";

const router = useRouter();

type ForecastItem = {
  date: string; // MM-DD
  range: string; // "12 (10-14)"
  confidence: number; // 95
};

const pad2 = (n: number) => (n < 10 ? `0${n}` : String(n));
const formatMMDD = (d: Date) => `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
const addDays = (d: Date, days: number) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + days);

// 预测结果展示
const forecastItems = ref<ForecastItem[]>([]);

const forecastLoading = ref(false);
const forecastMeta = ref<{ historyDays: number; generatedAt?: string; warning?: string; modelType?: string }>({ historyDays: 30 });

// 统计数据
const totalOrders = ref(0);
const pendingOrders = ref(0);
const totalMemorials = ref(0);
const totalUsers = ref(0);

// 趋势数据（暂时隐藏，因为没有历史数据计算）
const orderTrend = ref({ value: 0, color: "#909399" });
const pendingTrend = ref({ value: 0, color: "#909399" });
const memorialTrend = ref({ value: 0, color: "#909399" });
const userTrend = ref({ value: 0, color: "#909399" });

// 图表时间范围
const chartTimeRange = ref("7d");

// 订单趋势图表
const orderTrendChartContainer = ref<HTMLElement | null>(null);
const orderTrendChartLoading = ref(false);
const orderTrendData = ref<{
  dateLabels: string[];
  orderCounts: number[];
  totalOrders: number;
}>({
  dateLabels: [],
  orderCounts: [],
  totalOrders: 0,
});
let orderTrendChartInstance: echarts.ECharts | null = null;
let orderTrendChartResizeHandler: (() => void) | null = null;

// 服务类型分布图表
const serviceTypeChartContainer = ref<HTMLElement | null>(null);
const serviceTypeChartLoading = ref(false);
let serviceTypeChartInstance: echarts.ECharts | null = null;
let serviceTypeChartResizeHandler: (() => void) | null = null;

// 宠物类型分布图表
const petTypeChartContainer = ref<HTMLElement | null>(null);
const petTypeChartLoading = ref(false);
let petTypeChartInstance: echarts.ECharts | null = null;
let petTypeChartResizeHandler: (() => void) | null = null;

// 最近订单
const recentOrders = ref<any[]>([]);

// 待处理任务（模拟数据）
const pendingTasks = ref([
  {
    id: 1,
    type: "memorial",
    title: "纪念册审核",
    description: "用户提交了新的纪念册需要审核",
    createTime: "2025-12-08 11:30:00",
  },
  {
    id: 2,
    type: "order",
    title: "新订单待处理",
    description: "有新的服务订单需要处理",
    createTime: "2025-12-08 10:15:00",
  },
  {
    id: 3,
    type: "memorial",
    title: "纪念册审核",
    description: "用户提交了新的纪念册需要审核",
    createTime: "2025-12-08 09:30:00",
  },
]);

// 状态类型映射
const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    0: "warning",  // 待确认
    1: "primary",  // 已确认
    2: "success",  // 服务中
    3: "info",     // 已完成
    4: "danger",   // 已取消
  };
  return typeMap[status] || "info";
};

// 状态文本映射
const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    0: "待确认",
    1: "已确认",
    2: "服务中",
    3: "已完成",
    4: "已取消",
  };
  return textMap[status] || "未知";
};

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 跳转到订单列表（全部订单）
const goToOrderList = () => {
  router.push("/order/list");
};

// 跳转到待处理订单列表
const goToPendingOrders = () => {
  router.push("/admin/pending-tasks");
};

// 跳转到纪念册列表
const goToMemorialList = () => {
  router.push("/admin/memorial-review");
};

// 跳转到用户管理
const goToUserManagement = () => {
  router.push("/admin/user-management");
};

// 查看全部订单
const viewAllOrders = () => {
  router.push("/order/list");
};

// 查看全部任务
const viewAllTasks = () => {
  router.push("/admin/pending-tasks");
};

// 查看订单详情
const viewOrderDetail = (order: any) => {
  router.push(`/order/detail/${order.id}`);
};

// 审核纪念册
const reviewMemorial = (task: any) => {
  router.push("/admin/memorial-review");
};

// 处理订单
const processOrder = (task: any) => {
  router.push("/order/list");
};

// 页面加载时获取数据
onMounted(async () => {
  await loadOrderForecast();
  await loadDashboardData();
  await nextTick();
  await loadOrderTrend();
  await loadServiceTypeDistribution();
  await loadPetTypeDistribution();
});

import { dashboardApi, type OrderForecastResult } from "@/api/dashboard";

const loadOrderForecast = async () => {
  forecastLoading.value = true;
  try {
    const res = await dashboardApi.getOrderForecast(3, 30, false);
    const data = res.data as OrderForecastResult;
    forecastMeta.value = {
      historyDays: data.historyDays || 30,
      generatedAt: data.generatedAt,
      warning: data.warning,
      modelType: data.model?.type || (data.warning ? "FALLBACK" : "ARIMA"),
    };
    const conf = Math.round(((data.model?.confidence ?? 0.95) as number) * 100);
    forecastItems.value = (data.forecast || []).slice(0, 3).map((p) => {
      const d = new Date(p.date);
      const mmdd = `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
      const range = (p.lower != null && p.upper != null)
        ? `${p.predicted} (${p.lower}-${p.upper})`
        : String(p.predicted);
      return { date: mmdd, range, confidence: conf };
    });
  } catch (e) {
    // 失败时给一个温和的占位
    const base = new Date();
    forecastItems.value = [1, 2, 3].map((i) => ({
      date: formatMMDD(addDays(base, i)),
      range: "--",
      confidence: 95,
    }));
  } finally {
    forecastLoading.value = false;
  }
};

const retryArima = async () => {
  forecastLoading.value = true;
  try {
    // 强制刷新缓存，重新尝试跑 ARIMA
    const res = await dashboardApi.getOrderForecast(3, 30, true);
    const data = res.data as OrderForecastResult;
    forecastMeta.value = {
      historyDays: data.historyDays || 30,
      generatedAt: data.generatedAt,
      warning: data.warning,
      modelType: data.model?.type || (data.warning ? "FALLBACK" : "ARIMA"),
    };
    const conf = Math.round(((data.model?.confidence ?? 0.95) as number) * 100);
    forecastItems.value = (data.forecast || []).slice(0, 3).map((p) => {
      const d = new Date(p.date);
      const mmdd = `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
      const range = (p.lower != null && p.upper != null)
        ? `${p.predicted} (${p.lower}-${p.upper})`
        : String(p.predicted);
      return { date: mmdd, range, confidence: conf };
    });
  } catch (e) {
    // ignore
  } finally {
    forecastLoading.value = false;
  }
};

onUnmounted(() => {
  // 移除resize监听
  if (orderTrendChartResizeHandler) {
    window.removeEventListener('resize', orderTrendChartResizeHandler);
    orderTrendChartResizeHandler = null;
  }
  if (serviceTypeChartResizeHandler) {
    window.removeEventListener('resize', serviceTypeChartResizeHandler);
    serviceTypeChartResizeHandler = null;
  }
  if (petTypeChartResizeHandler) {
    window.removeEventListener('resize', petTypeChartResizeHandler);
    petTypeChartResizeHandler = null;
  }
  
  // 销毁图表实例
  if (orderTrendChartInstance) {
    orderTrendChartInstance.dispose();
    orderTrendChartInstance = null;
  }
  if (serviceTypeChartInstance) {
    serviceTypeChartInstance.dispose();
    serviceTypeChartInstance = null;
  }
  if (petTypeChartInstance) {
    petTypeChartInstance.dispose();
    petTypeChartInstance = null;
  }
});

// 加载仪表盘数据
const loadDashboardData = async () => {
  try {
    // 并行获取订单、用户和纪念册数据
    const [orderRes, userRes, memorialRes] = await Promise.all([
      orderApi.getOrderList({
        pageNum: 1,
        pageSize: 100, // 获取足够多的订单用于统计
      }),
      getUserList({
        pageNum: 1,
        pageSize: 100, // 获取足够多的用户用于统计
      }).catch(() => null), // 如果获取用户列表失败，继续执行
      memorialApi.getMemorialList({
        designStatus: 60, // 管理员审核通过的纪念册
        pageNum: 1,
        pageSize: 1, // 只需要总数，不需要具体数据
      }).catch(() => null), // 如果获取纪念册列表失败，继续执行
    ]);
    
    // 处理订单数据
    if (orderRes && orderRes.code === 200 && orderRes.data) {
      const allOrders = orderRes.data.records;
      
      // 统计总订单数
      totalOrders.value = orderRes.data.total || allOrders.length;
      
      // 统计待处理订单（status = 0）
      pendingOrders.value = allOrders.filter((o: any) => o.status === 0).length;
      
      // 获取最近5个订单
      recentOrders.value = allOrders
        .sort((a: any, b: any) => {
          const timeA = new Date(a.createTime).getTime();
          const timeB = new Date(b.createTime).getTime();
          return timeB - timeA; // 按创建时间倒序
        })
        .slice(0, 5)
        .map((order: any) => ({
          id: order.id,
          orderNo: order.orderNo,
          status: order.status,
          petName: order.petName,
          petType: order.petType,
          createTime: order.createTime,
        }));
      
      // 更新待处理任务列表（包含待确认的订单）
      const pendingOrderTasks = allOrders
        .filter((o: any) => o.status === 0)
        .slice(0, 3)
        .map((order: any) => ({
          id: order.id,
          type: "order",
          title: "新订单待处理",
          description: `订单号：${order.orderNo}，宠物：${order.petName}`,
          createTime: order.createTime,
        }));
      
      // 合并待处理任务（这里可以添加纪念册审核任务）
      pendingTasks.value = [...pendingOrderTasks];
    }
    
    // 处理用户数据
    if (userRes && userRes.code === 200 && userRes.data) {
      totalUsers.value = userRes.data.total || userRes.data.records.length;
    }
    
    // 处理纪念册数据：获取审核通过的纪念册数量（designStatus === 60）
    if (memorialRes && memorialRes.code === 200 && memorialRes.data) {
      totalMemorials.value = memorialRes.data.total || 0;
    } else {
      totalMemorials.value = 0;
    }
  } catch (error) {
    console.error("加载仪表盘数据失败:", error);
  }
};

// 时间范围变化处理
const handleTimeRangeChange = () => {
  loadOrderTrend();
};

// 加载订单趋势数据
const loadOrderTrend = async () => {
  if (!orderTrendChartContainer.value) return;
  
  orderTrendChartLoading.value = true;
  try {
    // 将时间范围字符串转换为天数
    const daysMap: Record<string, number> = {
      '7d': 7,
      '30d': 30,
      '90d': 90,
    };
    const days = daysMap[chartTimeRange.value] || 7;
    
    const res = await orderApi.getOrderTrend(days);
    if (res && res.code === 200 && res.data) {
      orderTrendData.value = res.data;
      renderOrderTrendChart(res.data.dateLabels, res.data.orderCounts);
    } else {
      console.error("获取订单趋势失败");
      orderTrendData.value = {
        dateLabels: [],
        orderCounts: [],
        totalOrders: 0,
      };
    }
  } catch (error) {
    console.error("加载订单趋势失败:", error);
    orderTrendData.value = {
      dateLabels: [],
      orderCounts: [],
      totalOrders: 0,
    };
  } finally {
    orderTrendChartLoading.value = false;
  }
};

// 渲染订单趋势图表
const renderOrderTrendChart = (dateLabels: string[], orderCounts: number[]) => {
  if (!orderTrendChartContainer.value) return;
  
  // 初始化图表实例
  if (!orderTrendChartInstance) {
    orderTrendChartInstance = echarts.init(orderTrendChartContainer.value);
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
        label: {
          backgroundColor: '#6a7985'
        }
      },
      formatter: (params: any) => {
        const param = params[0];
        const dateIndex = param.dataIndex;
        const originalDate = dateLabels[dateIndex];
        return `${originalDate}<br/>订单数: ${param.value}`;
      },
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
      axisLine: {
        lineStyle: {
          color: '#e0e0e0',
        },
      },
      axisLabel: {
        color: '#606266',
        fontSize: 12,
      },
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: '#e0e0e0',
        },
      },
      axisLabel: {
        color: '#606266',
        fontSize: 12,
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#409eff',
          width: 2,
        },
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
              {
                offset: 0,
                color: 'rgba(64, 158, 255, 0.3)',
              },
              {
                offset: 1,
                color: 'rgba(64, 158, 255, 0.05)',
              },
            ],
          },
        },
        data: orderCounts,
        emphasis: {
          focus: 'series',
          itemStyle: {
            color: '#409eff',
            borderColor: '#fff',
            borderWidth: 2,
            shadowBlur: 10,
            shadowColor: 'rgba(64, 158, 255, 0.5)',
          },
        },
      },
    ],
  };
  
  orderTrendChartInstance.setOption(option);
  
  // 响应式调整
  if (orderTrendChartResizeHandler) {
    window.removeEventListener('resize', orderTrendChartResizeHandler);
  }
  orderTrendChartResizeHandler = () => {
    orderTrendChartInstance?.resize();
  };
  window.addEventListener('resize', orderTrendChartResizeHandler);
};

// 加载服务类型分布数据
const loadServiceTypeDistribution = async () => {
  if (!serviceTypeChartContainer.value) return;
  
  serviceTypeChartLoading.value = true;
  try {
    const res = await orderApi.getServiceTypeDistribution();
    if (res && res.code === 200 && res.data) {
      const { distribution, totalOrders } = res.data;
      renderServiceTypeChart(distribution, totalOrders);
    } else {
      console.error("获取服务类型分布失败");
    }
  } catch (error) {
    console.error("加载服务类型分布失败:", error);
  } finally {
    serviceTypeChartLoading.value = false;
  }
};

// 渲染服务类型分布图表
const renderServiceTypeChart = (distribution: any[], totalOrders: number) => {
  if (!serviceTypeChartContainer.value) return;
  
  // 初始化图表实例
  if (!serviceTypeChartInstance) {
    serviceTypeChartInstance = echarts.init(serviceTypeChartContainer.value);
  }
  
  // 过滤掉没有订单的服务类型（count为0的）
  const filteredDistribution = distribution.filter(item => item.count > 0);
  
  // 如果没有数据，显示空状态
  if (filteredDistribution.length === 0) {
    serviceTypeChartInstance.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#909399',
          fontSize: 14,
        },
      },
    });
    return;
  }
  
  // 定义颜色方案
  const colors = [
    '#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399',
    '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
    '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'
  ];
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const percent = totalOrders > 0 
          ? ((params.value / totalOrders) * 100).toFixed(1) 
          : '0';
        return `${params.name}<br/>订单数: ${params.value}<br/>占比: ${percent}%`;
      },
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      itemWidth: 12,
      itemHeight: 12,
      textStyle: {
        fontSize: 12,
      },
      // 只显示有数据的服务类型
      data: filteredDistribution.map(item => item.serviceTypeName),
    },
    series: [
      {
        name: '服务类型分布',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: true,
          formatter: (params: any) => {
            const percent = totalOrders > 0 
              ? ((params.value / totalOrders) * 100).toFixed(1) 
              : '0';
            return `${params.name}\n${percent}%`;
          },
          fontSize: 12,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
        labelLine: {
          show: true,
        },
        // 只显示有订单的服务类型
        data: filteredDistribution.map((item, index) => ({
          value: item.count,
          name: item.serviceTypeName,
          itemStyle: {
            color: colors[index % colors.length],
          },
        })),
      },
    ],
  };
  
  serviceTypeChartInstance.setOption(option);
  
  // 响应式调整
  if (serviceTypeChartResizeHandler) {
    window.removeEventListener('resize', serviceTypeChartResizeHandler);
  }
  serviceTypeChartResizeHandler = () => {
    serviceTypeChartInstance?.resize();
  };
  window.addEventListener('resize', serviceTypeChartResizeHandler);
};

// 加载宠物类型分布数据
const loadPetTypeDistribution = async () => {
  if (!petTypeChartContainer.value) return;
  
  petTypeChartLoading.value = true;
  try {
    const res = await orderApi.getPetTypeDistribution();
    if (res && res.code === 200 && res.data) {
      const { distribution, totalOrders } = res.data;
      renderPetTypeChart(distribution, totalOrders);
    } else {
      console.error("获取宠物类型分布失败");
    }
  } catch (error) {
    console.error("加载宠物类型分布失败:", error);
    // 如果API不存在，从订单数据中统计
    await loadPetTypeDistributionFromOrders();
  } finally {
    petTypeChartLoading.value = false;
  }
};

// 从订单数据中统计宠物类型分布（备用方案）
const loadPetTypeDistributionFromOrders = async () => {
  try {
    const res = await orderApi.getOrderList({
      pageNum: 1,
      pageSize: 1000, // 获取足够多的订单用于统计
    });
    
    if (res && res.code === 200 && res.data) {
      const orders = res.data.records || [];
      const petTypeMap: Record<string, number> = {};
      
      orders.forEach((order: any) => {
        if (order.petType) {
          petTypeMap[order.petType] = (petTypeMap[order.petType] || 0) + 1;
        }
      });
      
      const distribution = Object.entries(petTypeMap)
        .map(([petType, count]) => ({ petType, count: count as number }))
        .sort((a, b) => b.count - a.count);
      
      renderPetTypeChart(distribution, orders.length);
    }
  } catch (error) {
    console.error("从订单数据统计宠物类型分布失败:", error);
  }
};

// 渲染宠物类型分布图表（柱状图）
const renderPetTypeChart = (distribution: Array<{ petType: string; count: number }>, totalOrders: number) => {
  if (!petTypeChartContainer.value) return;
  
  // 初始化图表实例
  if (!petTypeChartInstance) {
    petTypeChartInstance = echarts.init(petTypeChartContainer.value);
  }
  
  // 过滤掉没有订单的宠物类型（count为0的）
  const filteredDistribution = distribution.filter(item => item.count > 0);
  
  // 如果没有数据，显示空状态
  if (filteredDistribution.length === 0) {
    petTypeChartInstance.setOption({
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#909399',
          fontSize: 14,
        },
      },
    });
    return;
  }
  
  // 定义颜色方案（渐变色）
  const colors = [
    { start: '#409eff', end: '#66b1ff' },
    { start: '#67c23a', end: '#85ce61' },
    { start: '#e6a23c', end: '#ebb563' },
    { start: '#f56c6c', end: '#f78989' },
    { start: '#909399', end: '#a6a9ad' },
    { start: '#5470c6', end: '#7389db' },
    { start: '#91cc75', end: '#a8d88f' },
    { start: '#fac858', end: '#fbd47a' },
    { start: '#ee6666', end: '#f28585' },
    { start: '#73c0de', end: '#8dd0e8' },
  ];
  
  // 将英文宠物类型转换为中文
  const petTypeNames = filteredDistribution.map(item => getPetTypeLabel(item.petType) || item.petType);
  const counts = filteredDistribution.map(item => item.count);
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        const param = params[0];
        const percent = totalOrders > 0 
          ? ((param.value / totalOrders) * 100).toFixed(1) 
          : '0';
        return `${param.name}<br/>订单数: ${param.value}<br/>占比: ${percent}%`;
      },
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '15%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: petTypeNames,
      axisLine: {
        lineStyle: {
          color: '#e0e0e0',
        },
      },
      axisLabel: {
        color: '#606266',
        fontSize: 12,
        rotate: petTypeNames.length > 6 ? 15 : 0, // 如果类型太多，旋转标签
        interval: 0, // 显示所有标签
      },
    },
    yAxis: {
      type: 'value',
      name: '订单数',
      nameTextStyle: {
        color: '#606266',
        fontSize: 12,
      },
      axisLine: {
        lineStyle: {
          color: '#e0e0e0',
        },
      },
      axisLabel: {
        color: '#606266',
        fontSize: 12,
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: counts.map((count, index) => ({
          value: count,
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                {
                  offset: 0,
                  color: colors[index % colors.length].start,
                },
                {
                  offset: 1,
                  color: colors[index % colors.length].end,
                },
              ],
            },
            borderRadius: [4, 4, 0, 0], // 顶部圆角
          },
        })),
        barWidth: '60%',
        label: {
          show: true,
          position: 'top',
          color: '#303133',
          fontSize: 12,
          fontWeight: 500,
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
      },
    ],
  };
  
  petTypeChartInstance.setOption(option);
  
  // 响应式调整
  if (petTypeChartResizeHandler) {
    window.removeEventListener('resize', petTypeChartResizeHandler);
  }
  petTypeChartResizeHandler = () => {
    petTypeChartInstance?.resize();
  };
  window.addEventListener('resize', petTypeChartResizeHandler);
};
</script>

<style scoped lang="scss">
.dashboard-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.dashboard-intro {
  margin-bottom: 30px;

  h2 {
    color: #303133;
    margin-bottom: 10px;
  }

  p {
    color: #606266;
    font-size: 16px;
  }
}

.stats-row {
  margin-bottom: 30px;
}

.stat-card {
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  &.clickable-card {
    cursor: pointer;
    
    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
    }
    
    &:active {
      transform: translateY(-2px);
    }
  }
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.stat-info {
  .stat-value {
    font-size: 32px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 4px;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
  }
}

.stat-icon {
  font-size: 48px;
  color: #409eff;
}

.stat-trend {
  text-align: right;
}

.trend-text {
  font-size: 12px;
  color: #606266;

  .el-icon {
    margin-right: 4px;
  }
}

.charts-row {
  margin-bottom: 30px;
}

.chart-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 {
    margin: 0;
    color: #303133;
    font-size: 16px;
  }
}

.chart-container {
  flex: 1;
  padding: 20px 0;
}

.forecast-card {
  .forecast-container {
    padding: 6px 0 14px;
  }

  .forecast-subtitle {
    font-size: 13px;
    color: #606266;
    margin-bottom: 12px;
  }

  .forecast-items {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 14px;
  }

  .forecast-item {
    border: 1px solid #ebeef5;
    border-radius: 10px;
    padding: 12px 12px;
    background: #fff;
  }

  .fi-date {
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
  }

  .fi-main {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-bottom: 6px;
  }

  .fi-range {
    font-size: 16px;
    font-weight: 700;
    color: #303133;
  }

  .fi-unit {
    font-size: 12px;
    color: #606266;
  }

  .fi-conf {
    font-size: 12px;
    color: #e6a23c;
    font-weight: 600;
  }

  .forecast-note-title {
    font-size: 13px;
    font-weight: 700;
    color: #303133;
    margin-bottom: 6px;
  }

  .forecast-notes {
    margin: 0;
    padding-left: 18px;
    color: #606266;
    font-size: 12px;

    li {
      margin: 4px 0;
      line-height: 1.5;
    }
  }

  .forecast-alert {
    margin-top: 12px;
  }

  .forecast-alert-actions {
    margin-top: 8px;
  }
}

.placeholder-chart {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  background-color: #f5f7fa;
  color: #909399;
  border-radius: 8px;
}

.tasks-row {
  margin-bottom: 30px;
}

.tasks-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.empty-data {
  padding: 40px 0;
  text-align: center;
}

.recent-orders-list {
  max-height: 300px;
  overflow-y: auto;
}

.recent-order-item {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.3s ease;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background-color: #f5f7fa;
    padding-left: 10px;
  }
}

.order-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;

  .order-no {
    font-weight: bold;
    color: #303133;
  }

  .order-time {
    font-size: 12px;
    color: #909399;
  }
}

.order-details {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .pet-info {
    .pet-name {
      font-size: 14px;
      color: #303133;
    }

    .pet-type {
      font-size: 12px;
      color: #909399;
      margin-left: 4px;
    }
  }
}

.pending-tasks-list {
  max-height: 300px;
  overflow-y: auto;
}

.task-content {
  cursor: pointer;

  .task-title {
    font-weight: bold;
    color: #303133;
    margin-bottom: 4px;
  }

  .task-description {
    font-size: 14px;
    color: #606266;
    margin-bottom: 10px;
  }
}
</style>

