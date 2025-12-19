<template>
  <div class="service-feedback-container">
    <el-page-header @back="goBack" content="服务反馈" />
    
    <div class="content-wrapper">
      <div class="action-bar" style="margin-bottom: 20px">
        <el-button type="primary" @click="handleCreateFeedback">
          <el-icon><Plus /></el-icon>
          主动反馈
        </el-button>
      </div>
      
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待处理反馈" name="pending">
          <div class="feedback-list" v-loading="loading">
            <el-empty v-if="!loading && pendingFeedback.length === 0" description="暂无待处理反馈" />
            <div
              v-for="feedback in pendingFeedback"
              :key="feedback.id"
              class="feedback-item"
            >
              <div class="feedback-header">
                <div>
                  <h3>订单号：{{ feedback.orderNo }}</h3>
                  <p class="pet-name">宠物：{{ feedback.petName }}</p>
                  <p class="user-name">用户：{{ feedback.username }}</p>
                </div>
                <span class="date">{{ formatDate(feedback.createTime) }}</span>
              </div>
              <div class="feedback-content">
                <strong>反馈内容：</strong>
                <p>{{ feedback.content }}</p>
              </div>
              <div class="feedback-actions">
                <el-button type="primary" size="small" @click="handleFeedback(feedback)">
                  处理反馈
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="已处理反馈" name="processed">
          <div class="feedback-list" v-loading="loading">
            <el-empty v-if="!loading && processedFeedback.length === 0" description="暂无已处理反馈" />
            <div
              v-for="feedback in processedFeedback"
              :key="feedback.id"
              class="feedback-item"
            >
              <div class="feedback-header">
                <div>
                  <h3>订单号：{{ feedback.orderNo }}</h3>
                  <p class="pet-name">宠物：{{ feedback.petName }}</p>
                  <p class="user-name">用户：{{ feedback.username }}</p>
                </div>
                <span class="date">{{ formatDate(feedback.createTime) }}</span>
              </div>
              <div class="feedback-content">
                <strong>反馈内容：</strong>
                <p>{{ feedback.content }}</p>
              </div>
              <div class="feedback-response">
                <strong>处理回复：</strong>
                <p>{{ feedback.response }}</p>
                <span class="response-time">回复时间：{{ formatDate(feedback.responseTime) }}</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="我发出的反馈" name="sent">
          <div class="feedback-list" v-loading="sentLoading">
            <el-empty v-if="!sentLoading && sentFeedback.length === 0" description="暂无发出的反馈" />
            <div
              v-for="feedback in sentFeedback"
              :key="feedback.id"
              class="feedback-item"
            >
              <div class="feedback-header">
                <div>
                  <h3>订单号：{{ feedback.orderNo }}</h3>
                  <p class="pet-name">宠物：{{ feedback.petName }}</p>
                  <p class="user-name">发送给：{{ feedback.username }}</p>
                </div>
                <span class="date">{{ formatDate(feedback.createTime) }}</span>
              </div>
              <div class="feedback-content">
                <strong>反馈内容：</strong>
                <p>{{ feedback.content }}</p>
              </div>
              <div v-if="feedback.response" class="feedback-response">
                <strong>宠主回复：</strong>
                <p>{{ feedback.response }}</p>
                <span class="response-time">回复时间：{{ formatDate(feedback.responseTime) }}</span>
              </div>
              <div v-else class="feedback-status">
                <el-tag type="warning" size="small">待回复</el-tag>
              </div>
            </div>
          </div>
          
          <!-- 分页 -->
          <el-pagination
            v-if="sentTotal > 0"
            v-model:current-page="sentPagination.current"
            v-model:page-size="sentPagination.pageSize"
            :total="sentTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadSentFeedback"
            @current-change="loadSentFeedback"
            style="margin-top: 20px; justify-content: center;"
          />
        </el-tab-pane>
      </el-tabs>
      
      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadFeedback"
        @current-change="loadFeedback"
        style="margin-top: 20px; justify-content: center;"
      />
    </div>

    <!-- 主动反馈对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="主动反馈"
      width="600px"
    >
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="选择订单" prop="orderId">
          <el-select
            v-model="createForm.orderId"
            placeholder="请选择要反馈的订单"
            filterable
            style="width: 100%"
            @change="handleOrderChange"
          >
            <el-option
              v-for="order in availableOrders"
              :key="order.id"
              :label="`${order.orderNo} - ${order.petName}`"
              :value="order.id"
            >
              <div style="display: flex; justify-content: space-between;">
                <span>{{ order.orderNo }}</span>
                <span style="color: #909399; font-size: 12px;">{{ order.petName }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedOrder" label="宠主信息">
          <p style="margin: 0; color: #606266;">
            宠主：{{ selectedOrder.username || '未知' }}<br/>
            宠物：{{ selectedOrder.petName }}
          </p>
        </el-form-item>
        <el-form-item label="反馈内容" prop="content">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入您要向宠主反馈的内容..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateFeedback" :loading="creating">
          提交反馈
        </el-button>
      </template>
    </el-dialog>

    <!-- 处理反馈对话框 -->
    <el-dialog
      v-model="responseDialogVisible"
      title="处理反馈"
      width="600px"
    >
      <el-form :model="responseForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ currentFeedback?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="反馈内容">
          <p style="color: #606266; margin: 0;">{{ currentFeedback?.content }}</p>
        </el-form-item>
        <el-form-item label="回复内容" required>
          <el-input
            v-model="responseForm.response"
            type="textarea"
            :rows="6"
            placeholder="请输入您的回复内容..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="responseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResponse" :loading="responding">
          提交回复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { getProviderFeedbackList, respondToFeedback, createFeedbackByProvider, getProviderSentFeedbackList, type FeedbackVO, type FeedbackCreateDTO } from '@/api/feedback';
import { orderApi, type OrderVO } from '@/api/order';
import { useUserStore } from '@/store/user';
import dayjs from 'dayjs';

const router = useRouter();
const userStore = useUserStore();
const activeTab = ref('pending');
const loading = ref(false);
const responding = ref(false);
const creating = ref(false);
const pendingFeedback = ref<FeedbackVO[]>([]);
const processedFeedback = ref<FeedbackVO[]>([]);
const total = ref(0);
const pagination = reactive({
  current: 1,
  pageSize: 10,
});

// 我发出的反馈
const sentFeedback = ref<FeedbackVO[]>([]);
const sentTotal = ref(0);
const sentLoading = ref(false);
const sentPagination = reactive({
  current: 1,
  pageSize: 10,
});

const responseDialogVisible = ref(false);
const currentFeedback = ref<FeedbackVO | null>(null);
const responseForm = reactive({
  response: '',
});

const createDialogVisible = ref(false);
const createFormRef = ref();
const availableOrders = ref<OrderVO[]>([]);
const selectedOrder = ref<OrderVO | null>(null);
const createForm = reactive<FeedbackCreateDTO>({
  orderId: 0,
  content: '',
});
const createRules = {
  orderId: [
    { required: true, message: '请选择订单', trigger: 'change' },
  ],
  content: [
    { required: true, message: '请输入反馈内容', trigger: 'blur' },
    { min: 5, max: 1000, message: '反馈内容长度在 5 到 1000 个字符', trigger: 'blur' },
  ],
};

const goBack = () => {
  router.back();
};

const formatDate = (date?: string) => {
  if (!date) return '';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
};

const handleTabChange = () => {
  if (activeTab.value === 'pending' || activeTab.value === 'processed') {
    pagination.current = 1;
    loadFeedback();
  } else if (activeTab.value === 'sent') {
    sentPagination.current = 1;
    loadSentFeedback();
  }
};

const loadFeedback = async () => {
  loading.value = true;
  try {
    const status = activeTab.value === 'pending' ? 0 : 1;
    const res = await getProviderFeedbackList({
      status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    });
    
    if (res && res.code === 200 && res.data) {
      if (activeTab.value === 'pending') {
        pendingFeedback.value = res.data.records || [];
      } else {
        processedFeedback.value = res.data.records || [];
      }
      total.value = res.data.total || 0;
    } else {
      ElMessage.error('加载反馈列表失败');
    }
  } catch (error: any) {
    console.error('加载反馈列表失败:', error);
    ElMessage.error(error?.message || '加载反馈列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handleFeedback = (feedback: FeedbackVO) => {
  currentFeedback.value = feedback;
  responseForm.response = '';
  responseDialogVisible.value = true;
};

const submitResponse = async () => {
  if (!responseForm.response.trim()) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  
  if (!currentFeedback.value?.id) {
    ElMessage.error('反馈信息不存在');
    return;
  }
  
  responding.value = true;
  try {
    const res = await respondToFeedback({
      feedbackId: currentFeedback.value.id,
      response: responseForm.response.trim(),
    });
    
    if (res && res.code === 200) {
      ElMessage.success('回复成功');
      responseDialogVisible.value = false;
      await loadFeedback();
      // 触发反馈处理事件
      window.dispatchEvent(new CustomEvent('feedback-processed', {
        detail: { feedbackId: currentFeedback.value?.id }
      }));
    } else {
      ElMessage.error(res?.message || '回复失败');
    }
  } catch (error: any) {
    console.error('回复失败:', error);
    ElMessage.error(error?.message || '回复失败，请稍后重试');
  } finally {
    responding.value = false;
  }
};

// 加载我发出的反馈
const loadSentFeedback = async () => {
  sentLoading.value = true;
  try {
    const res = await getProviderSentFeedbackList({
      pageNum: sentPagination.current,
      pageSize: sentPagination.pageSize,
    });
    
    if (res && res.code === 200 && res.data) {
      sentFeedback.value = res.data.records || [];
      sentTotal.value = res.data.total || 0;
    } else {
      ElMessage.error('加载发出的反馈列表失败');
    }
  } catch (error: any) {
    console.error('加载发出的反馈列表失败:', error);
    ElMessage.error(error?.message || '加载发出的反馈列表失败，请稍后重试');
  } finally {
    sentLoading.value = false;
  }
};

// 监听反馈提交事件
const handleFeedbackSubmitted = () => {
  if (activeTab.value === 'pending') {
    loadFeedback();
  }
};

// 加载可用的订单列表（分配给当前服务人员的订单）
const loadAvailableOrders = async () => {
  try {
    const res = await orderApi.getOrderList({
      serviceProviderId: userStore.userId,
      pageNum: 1,
      pageSize: 100, // 获取足够多的订单
    });
    
    if (res && res.code === 200 && res.data) {
      // 只显示状态为已确认(1)、服务中(2)或已完成(3)的订单
      availableOrders.value = (res.data.records || []).filter(
        (order: OrderVO) => order.status !== undefined && order.status >= 1 && order.status <= 3
      );
    }
  } catch (error: any) {
    console.error('加载订单列表失败:', error);
    ElMessage.error('加载订单列表失败');
  }
};

// 处理创建反馈
const handleCreateFeedback = async () => {
  createDialogVisible.value = true;
  await loadAvailableOrders();
  createForm.orderId = 0;
  createForm.content = '';
  selectedOrder.value = null;
};

// 处理订单选择变化
const handleOrderChange = (orderId: number) => {
  selectedOrder.value = availableOrders.value.find(order => order.id === orderId) || null;
};

// 提交创建反馈
const submitCreateFeedback = async () => {
  if (!createFormRef.value) return;
  
  await createFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    
    creating.value = true;
    try {
      const res = await createFeedbackByProvider({
        orderId: createForm.orderId,
        content: createForm.content.trim(),
      });
      
      if (res && res.code === 200) {
        ElMessage.success('反馈提交成功');
        createDialogVisible.value = false;
        await loadFeedback();
        // 触发反馈创建事件
        window.dispatchEvent(new CustomEvent('feedback-created', {
          detail: { orderId: createForm.orderId }
        }));
      } else {
        ElMessage.error(res?.message || '提交失败');
      }
    } catch (error: any) {
      console.error('提交反馈失败:', error);
      ElMessage.error(error?.message || '提交失败，请稍后重试');
    } finally {
      creating.value = false;
    }
  });
};

onMounted(() => {
  loadFeedback();
  window.addEventListener('feedback-submitted', handleFeedbackSubmitted as EventListener);
  // 监听反馈创建事件，刷新发出的反馈列表
  window.addEventListener('feedback-created', () => {
    if (activeTab.value === 'sent') {
      loadSentFeedback();
    }
  });
});

onUnmounted(() => {
  window.removeEventListener('feedback-submitted', handleFeedbackSubmitted as EventListener);
});
</script>

<style scoped lang="scss">
// 统一配色方案（与其他页面保持一致）
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$text-primary: #303133;
$text-secondary: #606266;
$text-light: #909399;
$border-color: #ebeef5;
$bg-light: #f5f7fa;
$bg-white: #ffffff;

.service-feedback-container {
  padding: 30px 20px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 100px);
  background: #ffffff;

  :deep(.el-page-header) {
    margin-bottom: 24px;

    .el-page-header__content {
      color: $text-primary;
      font-weight: 700;
      font-size: 18px;
    }
  }
  
  .content-wrapper {
    margin-top: 24px;
  }

  .action-bar {
    margin-bottom: 24px;

    :deep(.el-button) {
      border-radius: 8px;
      font-weight: 500;
      padding: 12px 24px;

      &.el-button--primary {
        background-color: $primary-color;
        border-color: $primary-color;
      }
    }
  }

  :deep(.el-tabs) {
    .el-tabs__header {
      margin-bottom: 24px;
    }

    .el-tabs__nav-wrap::after {
      background-color: $border-color;
    }

    .el-tabs__item {
      font-size: 16px;
      font-weight: 500;
      color: $text-secondary;

      &.is-active {
        color: $primary-color;
        font-weight: 600;
      }
    }

    .el-tabs__active-bar {
      background-color: $primary-color;
      height: 3px;
    }
  }
  
  .feedback-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
    margin-top: 20px;
  }
  
  .feedback-item {
    background: $bg-white;
    padding: 24px;
    border-radius: 12px;
    border: 1px solid $border-color;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      transform: translateY(-2px);
      border-color: $primary-color;
    }
    
    .feedback-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
      padding-bottom: 16px;
      border-bottom: 2px solid $border-color;
      
      h3 {
        margin: 0 0 8px;
        font-size: 18px;
        color: $text-primary;
        font-weight: 600;
      }

      .pet-name,
      .user-name {
        margin: 4px 0;
        font-size: 14px;
        color: $text-secondary;
      }
      
      .date {
        font-size: 13px;
        color: $text-light;
        font-weight: 500;
        white-space: nowrap;
      }
    }
    
    .feedback-content {
      margin: 0 0 16px;
      padding: 16px;
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.08) 0%, rgba(103, 194, 58, 0.05) 100%);
      border-radius: 10px;
      border-left: 3px solid $primary-color;

      strong {
        display: block;
        margin-bottom: 10px;
        font-size: 14px;
        color: $text-primary;
        font-weight: 600;
      }
      
      p {
        margin: 0;
        font-size: 14px;
        color: $text-primary;
        line-height: 2;
        white-space: pre-wrap;
        word-break: break-word;
      }
    }
    
    .feedback-actions {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid $border-color;

      :deep(.el-button) {
        border-radius: 8px;
        font-weight: 500;
      }
    }
    
    .feedback-response {
      margin-top: 16px;
      padding: 16px;
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.05) 100%);
      border-radius: 10px;
      border-left: 3px solid #667eea;
      
      strong {
        display: block;
        margin-bottom: 10px;
        font-size: 14px;
        color: $text-primary;
        font-weight: 600;
      }
      
      p {
        margin: 0 0 10px;
        font-size: 14px;
        color: $text-primary;
        line-height: 2;
        white-space: pre-wrap;
        word-break: break-word;
      }

      .response-time {
        color: $text-light;
        font-size: 12px;
        display: block;
        margin-top: 10px;
        padding-top: 10px;
        border-top: 1px dashed $border-color;
      }
    }

    .feedback-status {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid $border-color;

      :deep(.el-tag) {
        border-radius: 999px;
        font-weight: 500;
      }
    }
  }

  :deep(.el-pagination) {
    margin-top: 24px;
    justify-content: center;

    .el-pager li {
      border-radius: 6px;
      margin: 0 4px;

      &.is-active {
        background-color: $primary-color;
        color: $bg-white;
      }
    }

    .btn-prev,
    .btn-next {
      border-radius: 6px;
    }
  }
}

// 优化对话框样式
:deep(.el-dialog) {
  border-radius: 12px;

  .el-dialog__header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 20px 24px;
    border-radius: 12px 12px 0 0;

    .el-dialog__title {
      color: $bg-white;
      font-size: 18px;
      font-weight: 600;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: $bg-white;
      font-size: 20px;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-form-item__label {
    color: $text-secondary;
    font-weight: 500;
  }

  .el-input__wrapper,
  .el-textarea__inner,
  .el-select .el-input__wrapper {
    border-radius: 8px;
  }

  .el-button {
    border-radius: 8px;
    font-weight: 500;

    &.el-button--primary {
      background-color: $primary-color;
      border-color: $primary-color;
    }
  }
}
</style>


