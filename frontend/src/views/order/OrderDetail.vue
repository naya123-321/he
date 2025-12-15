<template>
  <div class="order-detail-container">
    <el-page-header @back="goBack">
      <template #content>
        <span class="header-title">
          订单详情：{{ orderDetail?.orderNo }}
          <el-tag
            :type="getStatusType(orderDetail?.status)"
            size="small"
            style="margin-left: 10px"
          >
            {{ orderDetail?.statusText }}
          </el-tag>
        </span>
      </template>
    </el-page-header>

    <div class="detail-content">
      <!-- 订单信息卡片 -->
      <el-row :gutter="20">
        <el-col :span="16">
          <!-- 基本信息 -->
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <span>订单信息</span>
                <div class="card-actions">
                  <el-button
                    v-if="userStore.userRole === 2 && (orderDetail?.status === 0 || orderDetail?.status === 1) && !orderDetail?.serviceProviderId"
                    type="primary"
                    size="small"
                    @click="handleAssignProvider"
                  >
                    分配服务人员
                  </el-button>
                  <el-button
                    v-if="userStore.userRole === 2 && orderDetail?.serviceProviderId && (orderDetail?.status === 0 || orderDetail?.status === 1)"
                    type="warning"
                    size="small"
                    @click="handleAssignProvider"
                  >
                    重新分配
                  </el-button>
                  <el-button
                    v-if="(orderDetail?.status === 0 || orderDetail?.status === 1) && userStore.userRole === 0"
                    type="danger"
                    size="small"
                    @click="handleCancel"
                  >
                    取消订单
                  </el-button>
                  <el-button
                    v-if="orderDetail?.status === 3 && !orderDetail?.rating && userStore.userRole === 0"
                    type="success"
                    size="small"
                    @click="handleReview"
                  >
                    评价服务
                  </el-button>
                </div>
              </div>
            </template>

            <el-descriptions :column="2" border>
              <el-descriptions-item label="订单号">{{
                orderDetail?.orderNo
              }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{
                formatTime(orderDetail?.createTime)
              }}</el-descriptions-item>
              <el-descriptions-item label="服务类型">{{
                orderDetail?.serviceTypeName
              }}</el-descriptions-item>
              <el-descriptions-item label="服务价格"
                >¥{{ orderDetail?.totalAmount }}</el-descriptions-item
              >
              <el-descriptions-item label="预约时间">{{
                formatTime(orderDetail?.appointmentTime)
              }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 宠物信息 -->
          <el-card class="info-card" style="margin-top: 20px">
            <template #header>
              <span>宠物信息</span>
            </template>

            <el-descriptions :column="2" border>
              <el-descriptions-item label="宠物姓名">{{
                orderDetail?.petName
              }}</el-descriptions-item>
              <el-descriptions-item label="宠物类型">{{
                orderDetail?.petType
              }}</el-descriptions-item>
              <el-descriptions-item label="宠物品种">{{
                orderDetail?.petBreed || "未填写"
              }}</el-descriptions-item>
              <el-descriptions-item label="年龄">{{
                orderDetail?.petAge ? orderDetail.petAge + "岁" : "未填写"
              }}</el-descriptions-item>
              <el-descriptions-item label="体重">{{
                orderDetail?.petWeight ? orderDetail.petWeight + "kg" : "未填写"
              }}</el-descriptions-item>
              <el-descriptions-item label="离世日期">{{
                orderDetail?.deathDate
              }}</el-descriptions-item>
              <el-descriptions-item label="离世原因" :span="2">{{
                orderDetail?.deathCause || "未填写"
              }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 联系信息 -->
          <el-card class="info-card" style="margin-top: 20px">
            <template #header>
              <span>联系信息</span>
            </template>

            <el-descriptions :column="2" border>
              <el-descriptions-item label="联系人">{{
                orderDetail?.contactName
              }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{
                orderDetail?.contactPhone
              }}</el-descriptions-item>
              <el-descriptions-item label="服务地址" :span="2">{{
                orderDetail?.address
              }}</el-descriptions-item>
              <el-descriptions-item label="特殊要求" :span="2">{{
                orderDetail?.specialRequirements || "无"
              }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 已通过审核的特殊需求 -->
          <el-card
            v-if="approvedSpecialRequest"
            class="info-card special-request-card"
            style="margin-top: 20px"
          >
            <template #header>
              <div class="card-header">
                <span>已通过审核的特殊需求</span>
                <el-tag type="success" size="small">已通过</el-tag>
              </div>
            </template>

            <el-descriptions :column="2" border>
              <el-descriptions-item label="需求类型">
                <el-tag type="info" size="small">{{ approvedSpecialRequest.requestType }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="审核状态">
                <el-tag type="success" size="small">{{ approvedSpecialRequest.statusText }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="需求描述" :span="2">
                <div class="special-request-description">
                  {{ approvedSpecialRequest.description }}
                </div>
              </el-descriptions-item>
              <el-descriptions-item
                v-if="approvedSpecialRequest.adminComment"
                label="审核意见"
                :span="2"
              >
                <div class="admin-comment">
                  {{ approvedSpecialRequest.adminComment }}
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="审核时间" :span="2">
                {{ formatTime(approvedSpecialRequest.reviewTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 服务记录 -->
          <el-card
            v-if="orderDetail?.serviceNotes"
            class="info-card"
            style="margin-top: 20px"
          >
            <template #header>
              <span>服务记录</span>
            </template>
            <div class="service-notes">
              {{ orderDetail?.serviceNotes }}
            </div>
          </el-card>

          <!-- 评价信息 -->
          <el-card
            v-if="orderDetail?.rating"
            class="info-card"
            style="margin-top: 20px"
          >
            <template #header>
              <span>服务评价</span>
            </template>

            <div class="review-content">
              <div class="rating-section">
                <el-rate :value="orderDetail?.rating" disabled show-text />
                <span class="rating-text">{{ orderDetail?.rating }}分</span>
              </div>
              <div class="review-text" v-if="orderDetail?.review">
                <p>{{ orderDetail?.review }}</p>
              </div>
            </div>
          </el-card>

          <!-- 服务人员信息 -->
          <el-card
            class="provider-card"
            style="margin-top: 20px"
          >
            <template #header>
              <span>服务人员</span>
            </template>

            <div v-if="orderDetail?.serviceProviderName" class="provider-info">
              <div class="provider-avatar">
                <el-avatar :size="60" :src="orderDetail?.avatar" />
              </div>
              <div class="provider-details">
                <h4>{{ orderDetail?.serviceProviderName }}</h4>
                <p>
                  <el-icon><Phone /></el-icon>
                  {{ orderDetail?.serviceProviderPhone }}
                </p>
                <p>
                  <el-icon><Clock /></el-icon> 平均评分：{{
                    orderDetail?.rating || "暂无"
                  }}
                </p>
              </div>
            </div>
            <div v-else class="provider-info">
              <el-empty description="未分配服务人员" :image-size="80" />
            </div>

            <div class="provider-actions" v-if="canOperateOrderDetail">
              <!-- 管理员操作 -->
              <template v-if="userStore.userRole === 2">
                <el-button
                  v-if="!orderDetail?.serviceProviderId && (orderDetail?.status === 0 || orderDetail?.status === 1)"
                  type="primary"
                  size="small"
                  @click="handleAssignProvider"
                >
                  分配服务人员
                </el-button>
                <el-button
                  v-if="orderDetail?.serviceProviderId && (orderDetail?.status === 0 || orderDetail?.status === 1)"
                  type="warning"
                  size="small"
                  @click="handleAssignProvider"
                >
                  重新分配
                </el-button>
                <el-button
                  v-if="orderDetail?.serviceProviderId"
                  type="primary"
                  size="small"
                  @click="showNoteDialog = true"
                >
                  添加服务记录
                </el-button>
                <el-button
                  v-if="orderDetail?.status === 1"
                  type="success"
                  size="small"
                  @click="updateStatus(2)"
                >
                  开始服务
                </el-button>
                <el-button
                  v-if="orderDetail?.status === 2"
                  type="warning"
                  size="small"
                  @click="updateStatus(3)"
                >
                  完成服务
                </el-button>
              </template>
              <!-- 服务人员操作（只有分配给当前用户的订单） -->
              <template v-else-if="userStore.userRole === 1 && isAssignedToCurrentUser">
                <el-button
                  v-if="orderDetail?.status === 1"
                  type="success"
                  size="small"
                  @click="updateStatus(2)"
                >
                  开始服务
                </el-button>
                <el-button
                  v-if="orderDetail?.status === 2"
                  type="warning"
                  size="small"
                  @click="updateStatus(3)"
                >
                  完成服务
                </el-button>
                <el-button
                  v-if="orderDetail?.serviceProviderId"
                  type="primary"
                  size="small"
                  @click="showNoteDialog = true"
                >
                  添加服务记录
                </el-button>
              </template>
            </div>
          </el-card>

          <!-- 操作记录 -->
          <el-card class="log-card" style="margin-top: 20px">
            <template #header>
              <span>操作记录</span>
            </template>

            <div class="log-list">
              <div v-for="log in statusLogs" :key="log.id" class="log-item">
                <div class="log-time">{{ formatTime(log.createTime) }}</div>
                <div class="log-content">{{ log.remark }}</div>
                <div class="log-operator">操作人：{{ log.operatorName }}</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <!-- 状态时间线 -->
          <el-card class="timeline-card">
            <template #header>
              <span>订单进度</span>
            </template>

            <el-timeline v-if="timelineData && timelineData.length > 0">
              <el-timeline-item
                v-for="(item, index) in timelineData"
                :key="index"
                :timestamp="item.time"
                :type="item.type"
                :color="item.color"
                placement="top"
              >
                <div class="timeline-content">
                  <strong>{{ item.title }}</strong>
                  <p v-if="item.content">{{ item.content }}</p>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无订单进度信息" :image-size="100" />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 取消订单弹窗 -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="400px">
      <el-form ref="cancelForm" :model="cancelForm" :rules="cancelRules">
        <el-form-item prop="reason">
          <el-input
            type="textarea"
            v-model="cancelForm.reason"
            placeholder="请输入取消原因"
            rows="4"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCancel">确认取消</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="评价服务" width="500px">
      <el-form ref="reviewForm" :model="reviewForm" :rules="reviewRules">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" allow-half show-text></el-rate>
        </el-form-item>
        <el-form-item prop="review">
          <el-input
            type="textarea"
            v-model="reviewForm.review"
            placeholder="请输入评价内容"
            rows="4"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReview">提交评价</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 分配服务人员弹窗 -->
    <el-dialog v-model="assignDialogVisible" title="分配服务人员" width="500px">
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

    <!-- 添加服务记录弹窗 -->
    <el-dialog v-model="showNoteDialog" title="添加服务记录">
      <el-form :model="noteForm" label-width="80px">
        <el-form-item label="服务记录">
          <el-input
            v-model="noteForm.notes"
            type="textarea"
            :rows="4"
            placeholder="请填写服务过程记录"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoteDialog = false">取 消</el-button>
        <el-button type="primary" @click="addServiceNote">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import dayjs from "dayjs";
import { orderApi } from "@/api/order";
import { useOrderStore } from "@/store/order";
import { useUserStore } from "@/store/user";
import { getUserList } from "@/api/user";
import type { UserVO } from "@/api/user";
import { Phone, Clock } from "@element-plus/icons-vue";
import { specialRequestApi, type SpecialRequestVO } from "@/api/specialRequest";

// 订单详情数据
const orderDetail = computed(() => orderStore.currentOrder);
// 已通过审核的特殊需求
const approvedSpecialRequest = ref<SpecialRequestVO | null>(null);
// 时间线数据
const timelineData = computed(() => {
  if (!orderDetail.value || !orderDetail.value.id) return [];

  const timeline = [];
  const status = orderDetail.value.status;

  // 创建时间
  if (orderDetail.value.createTime) {
    timeline.push({
      time: formatTime(orderDetail.value.createTime),
      title: "订单创建",
      content: `用户提交订单申请，订单号：${orderDetail.value.orderNo || ''}`,
      type: "primary",
      color: "#409eff",
    });
  }

  // 如果订单已取消（status === 4），只显示到取消前的状态，然后显示取消信息
  if (status === 4) {
    // 如果订单在取消前已经确认（status >= 1），显示确认信息
    // 注意：由于订单状态已经是4，我们需要通过其他方式判断取消前的状态
    // 这里我们假设如果订单有服务人员信息，说明曾经被确认过
    if (orderDetail.value.serviceProviderName || orderDetail.value.serviceProviderId) {
      timeline.push({
        time: formatTime(orderDetail.value.appointmentTime) || formatTime(orderDetail.value.createTime),
        title: "订单已确认",
        content: orderDetail.value.serviceProviderName 
          ? `${orderDetail.value.serviceProviderName}已确认订单，将按时提供服务`
          : "服务人员已确认订单，将按时提供服务",
        type: "success",
        color: "#67c23a",
      });
    }
    
    // 显示取消信息
    timeline.push({
      time: formatTime(orderDetail.value.updateTime) || formatTime(orderDetail.value.createTime),
      title: "订单已取消",
      content: "订单已被取消",
      type: "danger",
      color: "#f56c6c",
    });
    
    return timeline; // 已取消的订单，只显示到取消步骤
  }

  // 状态变更 - 只有未取消的订单才显示后续步骤
  // 使用精确匹配而不是 >=，确保只显示实际达到的状态
  if (status === 1 || status === 2 || status === 3) {
    timeline.push({
      time: formatTime(orderDetail.value.appointmentTime) || formatTime(orderDetail.value.createTime),
      title: "订单已确认",
      content: orderDetail.value.serviceProviderName 
        ? `${orderDetail.value.serviceProviderName}已确认订单，将按时提供服务`
        : "服务人员已确认订单，将按时提供服务",
      type: "success",
      color: "#67c23a",
    });
  }

  if (status === 2 || status === 3) {
    timeline.push({
      time: formatTime(orderDetail.value.appointmentTime) || "服务中",
      title: "服务进行中",
      content: "服务正在进行，请稍候",
      type: "warning",
      color: "#e6a23c",
    });
  }

  if (status === 3) {
    timeline.push({
      time: formatTime(orderDetail.value.completionTime) || formatTime(orderDetail.value.appointmentTime),
      title: "服务已完成",
      content: "服务流程全部完成",
      type: "success",
      color: "#67c23a",
    });
  }

  if (status === 3 && orderDetail.value.rating) {
    timeline.push({
      time: formatTime(orderDetail.value.completionTime) || "评价时间",
      title: "服务评价",
      content: `用户评分：${orderDetail.value.rating}星${orderDetail.value.review ? '，评价：' + orderDetail.value.review : ''}`,
      type: "info",
      color: "#909399",
    });
  }

  return timeline;
});
// 取消订单弹窗
const cancelDialogVisible = ref(false);
// 评价弹窗
const reviewDialogVisible = ref(false);
// 加载状态
const loading = ref(false);
// 添加服务记录弹窗
const showNoteDialog = ref(false);
// 服务记录表单
const noteForm = reactive({
  notes: "",
});
// 状态日志
const statusLogs = ref<any[]>([]);

// 分配服务人员相关
const assignDialogVisible = ref(false);
const assigning = ref(false);
const serviceProviders = ref<UserVO[]>([]);
const assignForm = reactive({
  serviceProviderId: null as number | null,
});

// 取消订单表单
const cancelForm = reactive({
  reason: "",
});

// 取消订单表单规则
const cancelRules = reactive({
  reason: [{ required: true, message: "请输入取消原因", trigger: "blur" }],
});

// 评价表单
const reviewForm = reactive({
  rating: 5,
  review: "",
});

// 评价表单规则
const reviewRules = reactive({
  review: [{ required: true, message: "请输入评价内容", trigger: "blur" }],
});

const route = useRoute();
const router = useRouter();
const orderStore = useOrderStore();
const userStore = useUserStore();

// 检查是否有权限操作订单详情
// 只有管理员或分配给当前服务人员的订单可以操作
const canOperateOrderDetail = computed(() => {
  // 管理员可以操作所有订单
  if (userStore.userRole === 2) {
    return true;
  }
  
  // 服务人员只能操作分配给自己的订单
  if (userStore.userRole === 1) {
    return isAssignedToCurrentUser.value;
  }
  
  // 宠主可以查看自己的订单
  return userStore.userRole === 0;
});

// 检查订单是否分配给当前服务人员
const isAssignedToCurrentUser = computed(() => {
  if (!orderDetail.value || !orderDetail.value.serviceProviderId) {
    return false;
  }
  
  const currentUserId = userStore.userId;
  return orderDetail.value.serviceProviderId === currentUserId;
});

// 返回上一页
const goBack = () => {
  router.back();
};

// 获取订单ID
const orderId = route.params.id as string;

// 获取订单详情
const getOrderDetail = async () => {
  try {
    loading.value = true;
    await orderStore.fetchOrderDetail(orderId);
    await loadStatusLogs(orderId);
    await loadApprovedSpecialRequest(orderId);
  } catch (error) {
    ElMessage.error("获取订单详情失败");
    console.error(error);
  } finally {
    loading.value = false;
  }
};

// 加载已通过审核的特殊需求
const loadApprovedSpecialRequest = async (orderId: string) => {
  try {
    const res = await specialRequestApi.getApprovedSpecialRequestByOrderId(Number(orderId));
    if (res && res.code === 200) {
      approvedSpecialRequest.value = res.data;
    } else {
      approvedSpecialRequest.value = null;
    }
  } catch (error: any) {
    // 404错误表示没有特殊需求，这是正常的，不需要显示错误
    if (error?.response?.status === 404) {
      approvedSpecialRequest.value = null;
      return;
    }
    // 其他错误才记录日志，但不显示错误提示（因为这是可选功能）
    console.warn("获取特殊需求失败（可能没有特殊需求）:", error?.message || error);
    approvedSpecialRequest.value = null;
  }
};

// 加载状态日志
const loadStatusLogs = async (orderId: string) => {
  try {
    const res = await orderApi.getStatusLogs(Number(orderId));
    if (res && res.code === 200 && res.data) {
      statusLogs.value = res.data;
    } else if (res && res.data && res.data.code === 200) {
      // 兼容旧格式
      statusLogs.value = res.data.data || [];
    }
  } catch (error) {
    console.error("获取操作记录失败:", error);
    // 不显示错误提示，避免频繁弹窗
    statusLogs.value = [];
  }
};

// 更新状态
const updateStatus = async (targetStatus: number) => {
  if (!orderDetail.value) return;

  try {
    await orderStore.updateOrderStatus({
      orderId: orderDetail.value.id,
      targetStatus,
      remark: userStore.userRole === 2 ? "服务人员操作" : "用户操作",
    });
    ElMessage.success("状态更新成功");
    await getOrderDetail();
  } catch (error: any) {
    ElMessage.error(error.message || "状态更新失败");
  }
};

// 添加服务记录
const addServiceNote = async () => {
  if (!orderDetail.value || !noteForm.notes.trim()) return;

  try {
    await orderApi.addServiceNote(orderDetail.value.id, noteForm.notes);
    ElMessage.success("服务记录添加成功");
    showNoteDialog.value = false;
    noteForm.notes = "";
    await getOrderDetail();
  } catch (error: any) {
    ElMessage.error(error.message || "添加失败");
  }
};

// 取消订单
const handleCancel = () => {
  if (!orderDetail.value) {
    ElMessage.warning("订单信息不存在");
    return;
  }
  
  // 检查订单状态，只有待确认或已确认的订单可以取消
  if (orderDetail.value.status !== 0 && orderDetail.value.status !== 1) {
    ElMessage.warning("该订单状态不允许取消");
    return;
  }
  
  cancelForm.reason = "";
  cancelDialogVisible.value = true;
};

// 打开评价弹窗
const handleReview = () => {
  reviewDialogVisible.value = true;
  // 重置评价表单
  reviewForm.rating = 5;
  reviewForm.review = "";
};

// 提交取消订单
const submitCancel = async () => {
  if (!cancelForm.reason.trim()) {
    ElMessage.warning("请输入取消原因");
    return;
  }
  
  try {
    const res = await orderApi.cancelOrder(Number(orderId), cancelForm.reason.trim());
    
    if (res && res.code === 200) {
      ElMessage.success("取消订单成功");
      cancelDialogVisible.value = false;
      cancelForm.reason = "";
      
      // 刷新订单详情
      await getOrderDetail();
      
      // 触发订单状态更新事件，通知其他页面刷新
      window.dispatchEvent(new CustomEvent('order-status-updated', { 
        detail: { 
          orderId: Number(orderId),
          status: 4 // 已取消
        } 
      }));
    } else {
      ElMessage.error(res?.message || "取消订单失败");
    }
  } catch (error: any) {
    console.error("取消订单失败:", error);
    const errorMsg = error?.response?.data?.message || error?.message || "取消订单失败，请稍后重试";
    ElMessage.error(errorMsg);
  }
};

// 打开分配服务人员对话框
const handleAssignProvider = async () => {
  if (!orderDetail.value) {
    ElMessage.warning("订单信息不存在");
    return;
  }
  
  assignForm.serviceProviderId = orderDetail.value.serviceProviderId || null;
  
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

// 确认分配服务人员
const confirmAssign = async () => {
  if (!assignForm.serviceProviderId) {
    ElMessage.warning("请选择服务人员");
    return;
  }
  
  if (!orderDetail.value) {
    ElMessage.error("订单信息丢失，请刷新页面重试");
    return;
  }
  
  assigning.value = true;
  try {
    const serviceProviderId = assignForm.serviceProviderId;
    console.log("分配服务人员，订单ID:", orderDetail.value.id, "服务人员ID:", serviceProviderId);
    
    // 调用后端API分配服务人员
    const res = await orderApi.assignServiceProvider({
      orderId: orderDetail.value.id,
      serviceProviderId: serviceProviderId,
    });
    
    if (res && res.code === 200) {
      assignDialogVisible.value = false;
      
      // 等待一下确保后端数据已更新
      await new Promise(resolve => setTimeout(resolve, 300));
      
      // 刷新订单详情
      await getOrderDetail();
      
      // 验证分配是否成功
      const assignedProvider = serviceProviders.value.find(p => p.id === serviceProviderId);
      ElMessage.success(`已成功分配给${assignedProvider?.username || '服务人员'}`);
      
      // 触发事件，通知宠主端服务进度页面刷新
      window.dispatchEvent(new CustomEvent('order-assigned', { 
        detail: { 
          orderId: orderDetail.value.id,
          serviceProviderId: serviceProviderId
        } 
      }));
      // 同时触发订单状态更新事件
      window.dispatchEvent(new CustomEvent('order-status-updated', { 
        detail: { orderId: orderDetail.value.id, status: orderDetail.value.status || 1 } 
      }));
    } else {
      ElMessage.error(res?.message || "分配失败");
    }
  } catch (error: any) {
    console.error("分配服务人员失败:", error);
    const errorMsg = error?.response?.data?.message || error?.message || "分配失败";
    ElMessage.error(`分配服务人员失败: ${errorMsg}`);
  } finally {
    assigning.value = false;
  }
};

// 工具方法
const formatTime = (time?: string) => {
  return time ? dayjs(time).format("YYYY-MM-DD HH:mm") : "";
};

const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return "warning";
    case 1:
      return "primary";
    case 2:
      return "info";
    case 3:
      return "success";
    case 4:
      return "danger";
    default:
      return "default";
  }
};

// 提交评价
const submitReview = async () => {
  try {
    await orderApi.completeOrder(
      Number(orderId),
      reviewForm.rating,
      reviewForm.review
    );
    ElMessage.success("评价提交成功");
    reviewDialogVisible.value = false;
    getOrderDetail();
    
    // 触发评价提交事件，通知管理端刷新满意度统计
    window.dispatchEvent(new CustomEvent('review-submitted', { 
      detail: { orderId: Number(orderId), rating: reviewForm.rating } 
    }));
  } catch (error) {
    ElMessage.error("评价提交失败");
    console.error(error);
  }
};

onMounted(() => {
  getOrderDetail();
});
</script>

<style scoped lang="scss">
.order-detail-container {
  padding: 20px;

  .header-title {
    display: flex;
    align-items: center;
  }

  .detail-content {
    margin-top: 20px;

    .info-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .service-notes {
        padding: 10px;
        background: #f8f9fa;
        border-radius: 4px;
        line-height: 1.6;
      }

      .review-content {
        .rating-section {
          display: flex;
          align-items: center;
          margin-bottom: 10px;

          .rating-text {
            margin-left: 10px;
            font-size: 16px;
            color: #e6a23c;
            font-weight: bold;
          }
        }

        .review-text {
          padding: 10px;
          background: #f8f9fa;
          border-radius: 4px;

          p {
            margin: 0;
            line-height: 1.6;
          }
        }
      }
    }

    .timeline-card {
      .timeline-content {
        p {
          margin: 5px 0 0;
          color: #666;
          font-size: 14px;
        }
      }
    }

    .provider-card {
      .provider-info {
        display: flex;
        align-items: center;
        margin-bottom: 20px;

        .provider-avatar {
          margin-right: 20px;
        }

        .provider-details {
          h4 {
            margin: 0 0 10px;
            color: #333;
          }

          p {
            margin: 5px 0;
            color: #666;
            font-size: 14px;

            .el-icon {
              margin-right: 5px;
            }
          }
        }
      }

      .provider-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
      }
    }

    .log-card {
      .log-list {
        max-height: 300px;
        overflow-y: auto;

        .log-item {
          padding: 10px 0;
          border-bottom: 1px solid #eee;

          &:last-child {
            border-bottom: none;
          }

          .log-time {
            font-size: 12px;
            color: #999;
          }

          .log-content {
            margin: 5px 0;
            color: #333;
          }

          .log-operator {
            font-size: 12px;
            color: #666;
          }
        }
      }
    }

    .special-request-card {
      border: 2px solid #67c23a;
      background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .special-request-description {
        padding: 12px;
        background: #fff;
        border-radius: 4px;
        line-height: 1.6;
        color: #303133;
        white-space: pre-wrap;
        word-break: break-word;
      }

      .admin-comment {
        padding: 12px;
        background: #f0f9ff;
        border-left: 3px solid #409eff;
        border-radius: 4px;
        line-height: 1.6;
        color: #303133;
        white-space: pre-wrap;
        word-break: break-word;
      }
    }
  }
}

@media (max-width: 768px) {
  .order-detail-container {
    .provider-info {
      display: flex;
      align-items: center;
      padding: 10px 0;
    }
  }
}

.provider-avatar {
  margin-right: 15px;
}

.provider-details {
  flex: 1;
}

.provider-details h4 {
  margin: 0 0 10px;
  font-size: 16px;
}

.provider-details p {
  margin: 5px 0;
  color: #606266;
}

.provider-actions {
  margin-top: 15px;
  text-align: right;
}

/* 操作记录样式 */
.log-list {
  max-height: 300px;
  overflow-y: auto;
}

.log-item {
  padding: 10px 0;
  border-bottom: 1px dashed #e4e7ed;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.log-content {
  line-height: 1.6;
}

.log-operator {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .el-col {
    &[span="16"],
    &[span="8"] {
      width: 100%;
    }
  }
}
</style>