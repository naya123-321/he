<template>
  <div class="my-services-container">
    <el-page-header @back="goBack" content="我的服务" />

    <div class="services-intro">
      <h2>我的服务</h2>
      <p>查看和管理分配给您的服务订单</p>
    </div>

    <!-- 筛选条件 -->
    <el-card shadow="hover" class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="订单状态">
          <el-select v-model="filterForm.status" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="待确认" value="0" />
            <el-option label="已确认" value="1" />
            <el-option label="服务中" value="2" />
            <el-option label="已完成" value="3" />
            <el-option label="已取消" value="4" />
          </el-select>
        </el-form-item>

        <el-form-item label="宠物姓名">
          <el-input
            v-model="filterForm.petName"
            placeholder="请输入宠物姓名"
            clearable
          />
        </el-form-item>

        <el-form-item label="预约日期">
          <el-date-picker
            v-model="filterForm.appointmentDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="searchServices">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 服务列表 -->
    <el-card shadow="hover" class="services-card">
      <el-table
        v-loading="loading"
        :data="services"
        border
        style="width: 100%"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="petName" label="宠物姓名" width="120" />
        <el-table-column prop="petType" label="宠物类型" width="120" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="150" />
        <el-table-column prop="appointmentTime" label="预约时间" width="180" />
        <el-table-column prop="address" label="服务地址" width="200" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 1"
              type="success"
              size="small"
              @click="startService(scope.row)"
            >
              开始服务
            </el-button>
            <el-button
              v-if="scope.row.status === 2"
              type="warning"
              size="small"
              @click="completeService(scope.row)"
            >
              完成服务
            </el-button>
            <el-button
              type="info"
              size="small"
              @click="viewOrderDetail(scope.row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, RefreshLeft } from "@element-plus/icons-vue";
import { useOrderStore } from "@/store/order";
import { orderApi } from "@/api/order";
import { useUserStore } from "@/store/user";

const router = useRouter();
const loading = ref(false);
const orderStore = useOrderStore();
const userStore = useUserStore();

// 筛选条件
const filterForm = reactive({
  status: "",
  petName: "",
  appointmentDate: "",
});

// 分页信息
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

// 服务列表数据
const services = ref<any[]>([]);

// 状态类型映射
const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    0: "warning",
    1: "primary",
    2: "success",
    3: "info",
    4: "danger",
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

// 返回上一页
const goBack = () => {
  router.back();
};

// 查询我的服务列表
const searchServices = async () => {
  loading.value = true;
  try {
    const queryParams: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      // 查询分配给当前服务人员的订单
      serviceProviderId: userStore.userId,
    };

    if (filterForm.status !== "") {
      queryParams.status = Number(filterForm.status);
    }
    if (filterForm.petName) {
      queryParams.petName = filterForm.petName;
    }
    if (filterForm.appointmentDate) {
      queryParams.startDate = filterForm.appointmentDate + " 00:00:00";
      queryParams.endDate = filterForm.appointmentDate + " 23:59:59";
    }

    const res = await orderApi.getOrderList(queryParams);
    console.log("查询我的服务列表响应:", res);

    if (res && res.code === 200 && res.data) {
      const result = res.data;
      services.value = result.records.map((order: any) => ({
        id: order.id,
        orderNo: order.orderNo,
        status: order.status,
        petName: order.petName,
        petType: order.petType,
        contactName: order.contactName,
        contactPhone: order.contactPhone,
        appointmentTime: order.appointmentTime,
        address: order.address,
        createTime: order.createTime,
      }));
      pagination.total = result.total;
    } else {
      console.warn("查询结果为空或格式不正确:", res);
      services.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error("查询我的服务列表失败:", error);
    ElMessage.error("查询我的服务列表失败");
  } finally {
    loading.value = false;
  }
};

// 重置筛选条件
const resetFilter = () => {
  Object.assign(filterForm, {
    status: "",
    petName: "",
    appointmentDate: "",
  });
  searchServices();
};

// 开始服务
const startService = (service: any) => {
  ElMessageBox.confirm("确定要开始服务吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        const success = await orderStore.updateOrderStatus({
          orderId: service.id,
          targetStatus: 2, // 服务中
        });
        if (success) {
          ElMessage.success("服务已开始");
          await searchServices();
          
          // 触发事件，通知宠主端服务进度页面刷新
          window.dispatchEvent(new CustomEvent('order-status-updated', { 
            detail: { orderId: service.id, status: 2 } 
          }));
        } else {
          ElMessage.error("操作失败");
        }
      } catch (error) {
        console.error("开始服务失败:", error);
        ElMessage.error("开始服务失败，请稍后重试");
      }
    })
    .catch(() => {
      // 取消操作
    });
};

// 完成服务
const completeService = (service: any) => {
  ElMessageBox.confirm("确定要完成服务吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        const success = await orderStore.updateOrderStatus({
          orderId: service.id,
          targetStatus: 3, // 已完成
        });
        if (success) {
          ElMessage.success("服务已完成");
          await searchServices();
          
          // 触发事件，通知宠主端服务进度页面刷新
          window.dispatchEvent(new CustomEvent('order-status-updated', { 
            detail: { orderId: service.id, status: 3 } 
          }));
        } else {
          ElMessage.error("操作失败");
        }
      } catch (error) {
        console.error("完成服务失败:", error);
        ElMessage.error("完成服务失败，请稍后重试");
      }
    })
    .catch(() => {
      // 取消操作
    });
};

// 查看订单详情
const viewOrderDetail = (service: any) => {
  router.push(`/order/detail/${service.id}`);
};

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size;
  searchServices();
};

// 当前页码变化
const handleCurrentChange = (current: number) => {
  pagination.current = current;
  searchServices();
};

// 监听订单分配事件，实时刷新列表
const handleOrderAssigned = () => {
  console.log("收到订单分配事件，刷新我的服务列表");
  searchServices();
};

// 页面加载时获取服务列表
onMounted(async () => {
  await searchServices();
  // 监听订单分配事件
  window.addEventListener('order-assigned', handleOrderAssigned);
});

// 页面卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('order-assigned', handleOrderAssigned);
});
</script>

<style scoped lang="scss">
.my-services-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.services-intro {
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

.filter-card {
  margin-bottom: 30px;
}

.services-card {
  margin-bottom: 30px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>

