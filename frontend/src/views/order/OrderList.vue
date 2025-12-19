<template>
  <div class="order-list-container">
    <el-page-header @back="goBack" :content="pageTitle" />

    <el-card class="filter-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="订单状态">
          <el-select
            v-model="queryParams.status"
            placeholder="全部状态"
            clearable
            style="width: 100%"
          >
            <el-option label="待确认" :value="0" />
            <el-option label="服务中" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>

        <el-form-item label="宠物名称">
          <el-input
            v-model="queryParams.petName"
            placeholder="输入宠物名称"
            clearable
          />
        </el-form-item>

        <el-form-item label="订单号">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="输入订单号"
            clearable
          />
        </el-form-item>

        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单状态统计 -->
    <div class="stats-panel">
      <el-row :gutter="20">
        <el-col :span="4" v-for="stat in statsList" :key="stat.status">
          <el-card
            :class="['stat-card', stat.className]"
            shadow="hover"
            @click="filterByStatus(stat.status)"
          >
            <div class="stat-content">
              <div class="stat-number">{{ stat.count }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 订单列表 -->
    <el-card>
      <el-table :data="orderStore.orders" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="petName" label="宠物姓名" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ getPetTypeLabel(row.petType) || row.petType }}</el-tag>
            {{ row.petName }}
          </template>
        </el-table-column>
        <el-table-column prop="serviceTypeName" label="服务类型" />
        <el-table-column prop="appointmentTime" label="预约时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.appointmentTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="120">
          <template #default="{ row }"> ¥{{ row.totalAmount }} </template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              size="small"
              type="danger"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <el-button
              v-if="row.status === 3 && !row.rating"
              size="small"
              type="success"
              @click="handleReview(row)"
            >
              评价
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="orderStore.pagination.current"
          v-model:page-size="orderStore.pagination.pageSize"
          :total="orderStore.pagination.total"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 取消订单弹窗 -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单">
      <el-form :model="cancelForm" label-width="80px">
        <el-form-item label="取消原因" required>
          <el-input
            v-model="cancelForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请填写取消原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取 消</el-button>
        <el-button type="danger" @click="confirmCancel" :loading="canceling"
          >确 定</el-button
        >
      </template>
    </el-dialog>

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="服务评价">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="评分" required>
          <el-rate v-model="reviewForm.rating" show-text />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.review"
            type="textarea"
            :rows="4"
            placeholder="请写下您的评价和建议"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitReview" :loading="reviewing"
          >提 交</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import dayjs from "dayjs";
import { ElMessage, ElLoading } from "element-plus";
import { useOrderStore } from "@/store/order";
import { useUserStore } from "@/store/user";
import { orderApi, OrderQueryParams, OrderStat } from "@/api/order";
import { getPetTypeLabel } from "@/constants/petTypes";

const router = useRouter();
const orderStore = useOrderStore();
const userStore = useUserStore();

// 根据用户角色显示不同的页面标题
const pageTitle = computed(() => {
  if (userStore.userRole === 2) {
    return "订单管理";
  }
  return "我的订单";
});

// 返回上一页
const goBack = () => {
  if (userStore.userRole === 2) {
    router.push("/admin/dashboard");
  } else {
    router.push("/home");
  }
};

// 加载状态
const loading = ref(false);

// 查询参数
const queryParams = reactive<OrderQueryParams>({
  status: undefined,
  petName: "",
  orderNo: "",
  startTime: "",
  endTime: "",
  pageNum: 1,
  pageSize: 10,
});

// 日期范围
const dateRange = ref<string[]>([]);

// 统计数据
const statsList = ref<OrderStat[]>([
  { status: undefined, label: "全部订单", count: 0, className: "stat-all" },
  { status: 0, label: "待确认", count: 0, className: "stat-pending" },
  { status: 2, label: "服务中", count: 0, className: "stat-processing" },
  { status: 3, label: "已完成", count: 0, className: "stat-completed" },
  { status: 4, label: "已取消", count: 0, className: "stat-cancelled" },
]);

// 取消订单相关
const cancelDialogVisible = ref(false);
const canceling = ref(false);
const currentOrderId = ref("");
const cancelForm = reactive({ reason: "" });

// 评价订单相关
const reviewDialogVisible = ref(false);
const reviewing = ref(false);
const reviewingOrderId = ref("");
const reviewForm = reactive({
  rating: 5,
  review: "",
});

// 日期范围变化
const handleDateChange = (val: string[]) => {
  if (val && val.length === 2) {
    queryParams.startTime = val[0];
    queryParams.endTime = val[1];
  } else {
    queryParams.startTime = "";
    queryParams.endTime = "";
  }
};

// 搜索订单
const handleSearch = async () => {
  queryParams.pageNum = 1;
  await fetchOrders();
};

// 重置搜索
const resetSearch = () => {
  Object.assign(queryParams, {
    status: undefined,
    petName: "",
    orderNo: "",
    startTime: "",
    endTime: "",
    pageNum: 1,
    pageSize: 10,
  });
  dateRange.value = [];
  fetchOrders();
};

// 按状态筛选
const filterByStatus = (status: number | undefined) => {
  queryParams.status = status;
  queryParams.pageNum = 1;
  fetchOrders();
};

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true;
  try {
    await orderStore.fetchOrders(queryParams);
  } catch (error) {
    ElMessage.error("获取订单列表失败");
  } finally {
    loading.value = false;
  }
};

// 获取订单统计数据（不受筛选条件影响）
const fetchOrderStats = async () => {
  try {
    // 获取所有订单的统计数据，不传status参数
    const res = await orderApi.getOrderList({
      pageNum: 1,
      pageSize: 10000, // 获取足够多的数据用于统计
      // 不传status，获取所有状态的订单
    });
    
    if (res && res.code === 200 && res.data) {
      const allOrders = res.data.records || [];
      
      // 更新统计数据
      statsList.value[0].count = res.data.total || allOrders.length;
      
      statsList.value.forEach((stat) => {
        if (stat.status !== undefined) {
          stat.count = allOrders.filter(
            (order) => order.status === stat.status
          ).length;
        }
      });
    }
  } catch (error) {
    console.error("获取订单统计数据失败:", error);
  }
};

// 分页大小变化
const handleSizeChange = (size: number) => {
  queryParams.pageSize = size;
  fetchOrders();
};

// 页码变化
const handlePageChange = (page: number) => {
  queryParams.pageNum = page;
  fetchOrders();
};

// 查看订单详情
const viewDetail = (id: string | number) => {
  router.push(`/order/detail/${id}`);
};

// 取消订单
const handleCancel = (row: any) => {
  currentOrderId.value = row.id;
  cancelForm.reason = "";
  cancelDialogVisible.value = true;
};

// 确认取消订单
const confirmCancel = async () => {
  if (!cancelForm.reason.trim()) {
    ElMessage.warning("请填写取消原因");
    return;
  }

  canceling.value = true;
  try {
    const success = await orderStore.cancelOrder(
      Number(currentOrderId.value),
      cancelForm.reason.trim()
    );
    
    if (success) {
      ElMessage.success("订单已取消");
      cancelDialogVisible.value = false;
      cancelForm.reason = "";
      
      // 刷新订单列表
      await fetchOrders();
      
      // 触发订单状态更新事件，通知其他页面刷新
      window.dispatchEvent(new CustomEvent('order-status-updated', { 
        detail: { 
          orderId: Number(currentOrderId.value),
          status: 4 // 已取消
        } 
      }));
    } else {
      ElMessage.error("取消订单失败");
    }
  } catch (error: any) {
    console.error("取消订单失败:", error);
    const errorMsg = error?.response?.data?.message || error?.message || "取消订单失败，请稍后重试";
    ElMessage.error(errorMsg);
  } finally {
    canceling.value = false;
  }
};

// 处理评价
const handleReview = (row: any) => {
  reviewingOrderId.value = row.id;
  reviewForm.rating = 5;
  reviewForm.review = "";
  reviewDialogVisible.value = true;
};

// 提交评价
const submitReview = async () => {
  if (!reviewingOrderId.value) return;

  reviewing.value = true;
  try {
    await orderApi.completeOrder(
      Number(reviewingOrderId.value),
      reviewForm.rating,
      reviewForm.review
    );
    ElMessage.success("评价提交成功");
    reviewDialogVisible.value = false;
    fetchOrders();
  } catch (error) {
    ElMessage.error("评价提交失败");
  } finally {
    reviewing.value = false;
  }
};

// 格式化时间
const formatTime = (time: string) => {
  return time ? dayjs(time).format("YYYY-MM-DD HH:mm") : "";
};

// 获取状态标签类型
const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    0: "warning",
    1: "info",
    2: "primary",
    3: "success",
    4: "danger",
  };
  return typeMap[status] || "default";
};

// 初始化
onMounted(() => {
  // 先获取统计数据（所有订单）
  fetchOrderStats();
  // 再获取订单列表（当前页）
  fetchOrders();
});
</script>

<style scoped lang="scss">
.order-list-container {
  padding: 20px;

  .filter-card {
    margin-bottom: 20px;
  }

  .stats-panel {
    margin: 20px 0;

    .stat-card {
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-5px);
      }

      .stat-content {
        text-align: center;
        padding: 15px 0;

        .stat-number {
          font-size: 24px;
          font-weight: bold;
          margin-bottom: 5px;
        }

        .stat-label {
          color: #666;
        }
      }

      &.stat-all {
        border-left: 4px solid #409eff;
        .stat-number {
          color: #409eff;
        }
      }
      &.stat-pending {
        border-left: 4px solid #e6a23c;
        .stat-number {
          color: #e6a23c;
        }
      }
      &.stat-processing {
        border-left: 4px solid #409eff;
        .stat-number {
          color: #409eff;
        }
      }
      &.stat-completed {
        border-left: 4px solid #67c23a;
        .stat-number {
          color: #67c23a;
        }
      }
      &.stat-cancelled {
        border-left: 4px solid #f56c6c;
        .stat-number {
          color: #f56c6c;
        }
      }
    }
  }

  .pagination-wrapper {
    margin-top: 15px;
    text-align: right;
  }
}
</style>
