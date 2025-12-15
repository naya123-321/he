<template>
  <div class="appointment-list-container">
    <el-page-header @back="goBack" content="预约列表" />

    <div class="appointment-intro">
      <h2>接收预约并确认排期</h2>
      <p>查看和处理客户的预约请求</p>
    </div>

    <!-- 筛选条件 -->
    <el-card shadow="hover" class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="预约状态">
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
          <el-button type="primary" @click="searchAppointments">
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

    <!-- 预约列表 -->
    <el-card shadow="hover" class="appointments-card">
      <el-table
        v-loading="loading"
        :data="appointments"
        border
        style="width: 100%"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="petName" label="宠物姓名" width="120" />
        <el-table-column prop="petType" label="宠物类型" width="120" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="150" />
        <el-table-column prop="appointmentTime" label="预约时间" width="180" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="servicePackage" label="服务套餐" width="150" />
        <el-table-column prop="serviceProviderName" label="服务人员" width="120">
          <template #default="scope">
            <span v-if="scope.row.serviceProviderName">{{ scope.row.serviceProviderName }}</span>
            <el-tag v-else type="info" size="small">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <!-- 检查是否有权限操作：未分配或当前用户是服务人员或管理员 -->
            <template v-if="canOperateOrder(scope.row)">
              <el-button
                v-if="scope.row.status === 0 && !scope.row.serviceProviderId"
                type="success"
                size="small"
                @click="assignProvider(scope.row)"
              >
                分配人员
              </el-button>
              <el-button
                v-if="scope.row.status === 0 && !scope.row.serviceProviderId"
                type="primary"
                size="small"
                @click="confirmAppointment(scope.row)"
              >
                确认
              </el-button>
              <el-button
                v-if="scope.row.status === 0"
                type="danger"
                size="small"
                @click="rejectAppointment(scope.row)"
              >
                拒绝
              </el-button>
              <el-button
                v-if="scope.row.status === 1"
                type="success"
                size="small"
                @click="startService(scope.row)"
              >
                开始服务
              </el-button>
              <el-button
                v-if="scope.row.status === 1"
                type="warning"
                size="small"
                @click="viewOrderDetail(scope.row)"
              >
                详情
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
                v-if="scope.row.status === 3"
                type="info"
                size="small"
                @click="viewOrderDetail(scope.row)"
              >
                查看
              </el-button>
              <el-button
                v-if="scope.row.status === 4"
                type="info"
                size="small"
                @click="viewOrderDetail(scope.row)"
              >
                查看
              </el-button>
            </template>
            <template v-else>
              <el-button
                type="info"
                size="small"
                @click="viewOrderDetail(scope.row)"
              >
                查看
              </el-button>
            </template>
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

    <!-- 分配服务人员对话框 -->
    <el-dialog
      v-model="assignDialogVisible"
      title="分配服务人员"
      width="500px"
    >
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="选择服务人员" required>
          <el-select
            v-model="assignForm.serviceProviderId"
            placeholder="请选择服务人员"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="provider in serviceProviders"
              :key="provider.id"
              :label="`${provider.username}${provider.nickname ? ' (' + provider.nickname + ')' : ''}`"
              :value="provider.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAssign" :loading="assigning">
          确定分配
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { useOrderStore } from "@/store/order";
import { orderApi } from "@/api/order";
import { getUserList } from "@/api/user";
import { useUserStore } from "@/store/user";
import type { UserVO } from "@/api/user";

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

// 预约列表数据
const appointments = ref<any[]>([]);

// 分配服务人员相关
const assignDialogVisible = ref(false);
const assigning = ref(false);
const currentAssignOrder = ref<any>(null);
const serviceProviders = ref<UserVO[]>([]);
const assignForm = reactive({
  serviceProviderId: null as number | null,
});

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

// 返回上一页
const goBack = () => {
  router.back();
};

// 检查是否有权限操作订单
// 只有未分配订单、分配给当前用户的订单或管理员可以操作
const canOperateOrder = (order: any) => {
  // 如果是管理员，可以操作所有订单
  if (userStore.userRole === 2) {
    return true;
  }
  
  // 如果订单未分配服务人员，所有服务人员都可以操作
  if (!order.serviceProviderId) {
    return true;
  }
  
  // 如果订单已分配，只有分配给当前用户的订单可以操作
  const currentUserId = userStore.userId;
  return order.serviceProviderId === currentUserId;
};

// 查询预约列表
const searchAppointments = async () => {
  loading.value = true;
  try {
    const queryParams: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      // 服务人员查询时，不传递userId，后端会根据角色自动处理
      // 后端会识别服务人员角色，允许查询所有订单
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

    // 直接调用API，因为后端已经根据角色处理了查询逻辑
    // request.ts 的响应拦截器已经处理了响应，如果 code !== 200 会 reject
    // 所以这里 res 已经是处理后的数据，结构是 { code, message, data }
    const res = await orderApi.getOrderList(queryParams);
    console.log("服务人员查询预约列表响应:", res);
    
    if (res && res.code === 200 && res.data) {
      const result = res.data;
      console.log("查询到的订单列表:", result.records);
      appointments.value = result.records.map((order: any) => {
        // 确保serviceProviderId是数字类型
        const providerId = order.serviceProviderId 
          ? (typeof order.serviceProviderId === 'string' 
              ? parseInt(order.serviceProviderId, 10) 
              : order.serviceProviderId)
          : null;
        
        return {
          id: order.id,
          orderNo: order.orderNo,
          status: typeof order.status === 'string' ? parseInt(order.status, 10) : order.status,
          petName: order.petName,
          petType: order.petType,
          contactName: order.contactName,
          contactPhone: order.contactPhone,
          appointmentTime: order.appointmentTime,
          servicePackage: order.serviceTypeName || order.servicePackage || "",
          serviceProviderId: providerId,
          serviceProviderName: order.serviceProviderName || "",
          createTime: order.createTime,
        };
      });
      pagination.total = result.total;
      console.log("处理后的预约列表:", appointments.value);
    } else {
      console.warn("查询结果为空或格式不正确:", res);
      appointments.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error("查询预约列表失败:", error);
    ElMessage.error("查询预约列表失败");
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
  searchAppointments();
};

// 确认预约
const confirmAppointment = (appointment: any) => {
  ElMessageBox.confirm("确定要确认该预约吗？确认后订单将自动分配给您。", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        const currentUserId = userStore.userId;
        console.log("确认预约，当前用户ID:", currentUserId);
        
        // 先更新状态，后端会自动分配服务人员ID
        const success = await orderStore.updateOrderStatus({
          orderId: appointment.id,
          targetStatus: 1,
        });
        
        if (success) {
          // 等待一下确保后端数据已更新
          await new Promise(resolve => setTimeout(resolve, 300));
          
          // 重新加载列表，确保获取最新的服务人员信息
          await searchAppointments();
          
          // 验证服务人员是否已分配
          const updatedAppointment = appointments.value.find(a => a.id === appointment.id);
          if (updatedAppointment && updatedAppointment.serviceProviderId) {
            ElMessage.success(`预约已确认，订单已分配给${updatedAppointment.serviceProviderName || '您'}`);
          } else {
            ElMessage.success("预约已确认");
            console.warn("服务人员ID可能未正确分配，请刷新页面查看");
          }
          
          // 触发事件，通知"我的服务"页面和宠主端服务进度页面刷新
          window.dispatchEvent(new CustomEvent('order-assigned', { 
            detail: { 
              orderId: appointment.id,
              serviceProviderId: updatedAppointment?.serviceProviderId || currentUserId
            } 
          }));
          // 同时触发订单状态更新事件
          window.dispatchEvent(new CustomEvent('order-status-updated', { 
            detail: { orderId: appointment.id, status: 1 } 
          }));
        } else {
          ElMessage.error("确认失败");
        }
      } catch (error) {
        console.error("确认预约失败:", error);
        ElMessage.error("确认预约失败，请稍后重试");
      }
    })
    .catch(() => {
      // 取消操作
    });
};

// 拒绝预约
const rejectAppointment = (appointment: any) => {
  ElMessageBox.prompt("请输入拒绝原因", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    inputType: "textarea",
    inputPlaceholder: "请输入拒绝原因",
  })
    .then(async ({ value }) => {
      if (!value.trim()) {
        ElMessage.warning("请输入拒绝原因");
        return;
      }

      try {
        const success = await orderStore.updateOrderStatus({
          orderId: appointment.id,
          targetStatus: 4, // 已取消
          remark: value,
        });
        if (success) {
          ElMessage.success("预约已拒绝");
          // 更新本地数据
          appointment.status = 4;
          // 重新加载列表
          await searchAppointments();
        } else {
          ElMessage.error("拒绝失败");
        }
      } catch (error) {
        console.error("拒绝预约失败:", error);
        ElMessage.error("拒绝预约失败，请稍后重试");
      }
    })
    .catch(() => {
      // 取消操作
    });
};

// 开始服务
const startService = (appointment: any) => {
  ElMessageBox.confirm("确定要开始服务吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        const success = await orderStore.updateOrderStatus({
          orderId: appointment.id,
          targetStatus: 2, // 服务中
        });
        if (success) {
          ElMessage.success("服务已开始");
          // 更新本地数据
          appointment.status = 2;
          // 重新加载列表
          await searchAppointments();
          
          // 触发事件，通知宠主端服务进度页面刷新
          window.dispatchEvent(new CustomEvent('order-status-updated', { 
            detail: { orderId: appointment.id, status: 2 } 
          }));
        } else {
          ElMessage.error("开始服务失败");
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

// 完成服务（在预约列表中）
const completeService = (appointment: any) => {
  ElMessageBox.confirm("确定要完成服务吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        const success = await orderStore.updateOrderStatus({
          orderId: appointment.id,
          targetStatus: 3, // 已完成
        });
        if (success) {
          ElMessage.success("服务已完成");
          // 更新本地数据
          appointment.status = 3;
          // 重新加载列表
          await searchAppointments();
          
          // 触发事件，通知宠主端服务进度页面刷新
          window.dispatchEvent(new CustomEvent('order-status-updated', { 
            detail: { orderId: appointment.id, status: 3 } 
          }));
        } else {
          ElMessage.error("完成服务失败");
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
const viewOrderDetail = (appointment: any) => {
  // 跳转到订单详情页面
  router.push(`/order/detail/${appointment.id}`);
};

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size;
  searchAppointments();
};

// 当前页码变化
const handleCurrentChange = (current: number) => {
  pagination.current = current;
  searchAppointments();
};

// 分配服务人员
const assignProvider = async (appointment: any) => {
  currentAssignOrder.value = appointment;
  assignForm.serviceProviderId = null;
  
  // 加载服务人员列表
  try {
    const res = await getUserList({
      role: 1, // 只获取服务人员（role=1）
      pageNum: 1,
      pageSize: 100,
    });
    
    if (res && res.code === 200 && res.data) {
      serviceProviders.value = res.data.records || [];
    } else {
      ElMessage.warning("获取服务人员列表失败");
      return;
    }
  } catch (error) {
    console.error("获取服务人员列表失败:", error);
    ElMessage.error("获取服务人员列表失败");
    return;
  }
  
  assignDialogVisible.value = true;
};

// 确认分配
const confirmAssign = async () => {
  if (!assignForm.serviceProviderId) {
    ElMessage.warning("请选择服务人员");
    return;
  }
  
  if (!currentAssignOrder.value) {
    ElMessage.error("订单信息丢失，请刷新页面重试");
    return;
  }
  
  assigning.value = true;
  try {
    const serviceProviderId = assignForm.serviceProviderId;
    console.log("分配服务人员，订单ID:", currentAssignOrder.value.id, "服务人员ID:", serviceProviderId);
    
    // 调用后端API分配服务人员
    const res = await orderApi.assignServiceProvider({
      orderId: currentAssignOrder.value.id,
      serviceProviderId: serviceProviderId,
    });
    
    if (res && res.code === 200) {
      assignDialogVisible.value = false;
      
      // 等待一下确保后端数据已更新
      await new Promise(resolve => setTimeout(resolve, 300));
      
      // 重新加载列表，确保获取最新的服务人员信息
      await searchAppointments();
      
      // 验证分配是否成功
      const updatedAppointment = appointments.value.find(a => a.id === currentAssignOrder.value.id);
      const assignedProvider = serviceProviders.value.find(p => p.id === serviceProviderId);
      if (updatedAppointment && updatedAppointment.serviceProviderId === serviceProviderId) {
        ElMessage.success(`已成功分配给${assignedProvider?.username || '服务人员'}`);
      } else {
        ElMessage.success("分配成功");
      }
      
      // 触发事件，通知宠主端服务进度页面刷新
      window.dispatchEvent(new CustomEvent('order-assigned', { 
        detail: { 
          orderId: currentAssignOrder.value.id,
          serviceProviderId: serviceProviderId
        } 
      }));
      // 同时触发订单状态更新事件
      window.dispatchEvent(new CustomEvent('order-status-updated', { 
        detail: { orderId: currentAssignOrder.value.id, status: 1 } 
      }));
    } else {
      ElMessage.error("分配失败");
    }
  } catch (error: any) {
    console.error("分配服务人员失败:", error);
    const errorMsg = error?.response?.data?.message || error?.message || "分配失败";
    ElMessage.error(`分配服务人员失败: ${errorMsg}`);
  } finally {
    assigning.value = false;
  }
};

// 页面加载时获取预约列表
onMounted(async () => {
  await searchAppointments();
});
</script>

<style scoped lang="scss">
.appointment-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.appointment-intro {
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

.appointments-card {
  margin-bottom: 30px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
