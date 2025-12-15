<template>
  <div class="pending-tasks-container">
    <el-page-header @back="goBack" content="待处理任务" />

    <div class="tasks-intro">
      <h2>待处理任务</h2>
      <p>查看和管理所有待处理的任务，包括待确认订单和待审核纪念册</p>
    </div>

    <!-- 筛选区域 -->
    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true">
        <el-form-item label="任务类型：">
          <el-select v-model="filter.type" placeholder="全部类型" clearable style="width: 200px">
            <el-option label="全部类型" value="" />
            <el-option label="订单处理" value="order" />
            <el-option label="纪念册审核" value="memorial" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTasks">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 任务列表 -->
    <el-card shadow="hover" class="tasks-card">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="filteredTasks.length === 0" class="empty-data">
        <el-empty description="暂无待处理任务" />
      </div>

      <div v-else class="tasks-list">
        <el-timeline>
          <el-timeline-item
            v-for="task in filteredTasks"
            :key="task.id"
            :timestamp="formatDateTime(task.createTime)"
            :type="task.type === 'memorial' ? 'primary' : 'warning'"
            :icon="task.type === 'memorial' ? 'Document' : 'ShoppingBag'"
          >
            <el-card shadow="hover" class="task-card">
              <div class="task-header">
                <div class="task-title-wrapper">
                  <el-tag :type="task.type === 'memorial' ? 'primary' : 'warning'" size="large">
                    {{ task.type === 'memorial' ? '纪念册审核' : '订单处理' }}
                  </el-tag>
                  <h3 class="task-title">{{ task.title }}</h3>
                </div>
                <div class="task-actions">
                  <el-button
                    v-if="task.type === 'memorial'"
                    type="primary"
                    size="small"
                    @click="reviewMemorial(task)"
                  >
                    审核纪念册
                  </el-button>
                  <el-button
                    v-if="task.type === 'order'"
                    type="success"
                    size="small"
                    @click="processOrder(task)"
                  >
                    处理订单
                  </el-button>
                </div>
              </div>
              <div class="task-description">{{ task.description }}</div>
              <div class="task-meta">
                <span class="task-time">创建时间：{{ formatDateTime(task.createTime) }}</span>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { orderApi } from "@/api/order";
import { memorialApi } from "@/api/memorial";

const router = useRouter();

// 筛选条件
const filter = ref({
  type: "", // "" 全部, "order" 订单, "memorial" 纪念册
});

// 任务列表
const tasks = ref<any[]>([]);
const loading = ref(false);

// 过滤后的任务列表
const filteredTasks = computed(() => {
  if (!filter.value.type) {
    return tasks.value;
  }
  return tasks.value.filter((task) => task.type === filter.value.type);
});

// 加载任务列表
const loadTasks = async () => {
  loading.value = true;
  try {
    // 加载待确认订单
    const ordersRes = await orderApi.getOrderList({
      status: 0, // 待确认
      pageNum: 1,
      pageSize: 1000, // 获取所有待确认订单
    });

    const orderTasks: any[] = [];
    if (ordersRes && ordersRes.code === 200 && ordersRes.data) {
      const orders = ordersRes.data.records || [];
      orderTasks.push(
        ...orders.map((order: any) => ({
          id: order.id,
          type: "order",
          title: "新订单待处理",
          description: `订单号：${order.orderNo}，宠物：${order.petName}（${order.petType}）`,
          createTime: order.createTime,
          orderId: order.id,
        }))
      );
    }

    // 加载待审核纪念册
    const memorialsRes = await memorialApi.getMemorialList({
      status: 1, // 待审核
      pageNum: 1,
      pageSize: 1000, // 获取所有待审核纪念册
    });

    const memorialTasks: any[] = [];
    if (memorialsRes && memorialsRes.code === 200 && memorialsRes.data) {
      const memorials = memorialsRes.data.records || [];
      memorialTasks.push(
        ...memorials.map((memorial: any) => ({
          id: memorial.id,
          type: "memorial",
          title: "纪念册审核",
          description: `纪念册标题：${memorial.title}，宠物：${memorial.petName}（${memorial.petType}）`,
          createTime: memorial.createTime,
          memorialId: memorial.id,
        }))
      );
    }

    // 合并任务并按时间倒序排序
    tasks.value = [...orderTasks, ...memorialTasks].sort((a, b) => {
      const timeA = new Date(a.createTime).getTime();
      const timeB = new Date(b.createTime).getTime();
      return timeB - timeA; // 按创建时间倒序
    });
  } catch (error: any) {
    console.error("加载待处理任务失败:", error);
    ElMessage.error(error?.message || "加载待处理任务失败");
  } finally {
    loading.value = false;
  }
};

// 重置筛选
const resetFilter = () => {
  filter.value.type = "";
  loadTasks();
};

// 格式化日期时间
const formatDateTime = (dateString: string) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
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

// 审核纪念册
const reviewMemorial = (task: any) => {
  router.push("/admin/memorial-review");
};

// 处理订单
const processOrder = (task: any) => {
  if (task.orderId) {
    router.push(`/order/detail/${task.orderId}`);
  } else {
    router.push("/order/list?status=0");
  }
};

// 页面加载时获取数据
onMounted(() => {
  loadTasks();
});
</script>

<style scoped lang="scss">
.pending-tasks-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.tasks-intro {
  margin-bottom: 20px;

  h2 {
    color: #303133;
    margin-bottom: 10px;
  }

  p {
    color: #606266;
    font-size: 14px;
  }
}

.filter-card {
  margin-bottom: 20px;
}

.tasks-card {
  min-height: 400px;
}

.loading-container {
  padding: 20px;
}

.empty-data {
  padding: 60px 20px;
  text-align: center;
}

.tasks-list {
  padding: 20px 0;
}

.task-card {
  margin-bottom: 10px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateX(5px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.task-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.task-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.task-actions {
  display: flex;
  gap: 10px;
}

.task-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.task-time {
  color: #909399;
}
</style>


