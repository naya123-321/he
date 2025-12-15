<template>
  <div class="service-feedback-container">
    <el-page-header @back="goBack" content="服务反馈" />
    
    <div class="content-wrapper">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的反馈" name="my-feedback">
          <div class="feedback-list" v-loading="loading">
            <el-empty v-if="!loading && feedbackList.length === 0" description="暂无反馈记录" />
            <div
              v-for="feedback in feedbackList"
              :key="feedback.id"
              class="feedback-item"
            >
              <div class="feedback-header">
                <div class="feedback-meta">
                  <div>
                    <h3>订单号：{{ feedback.orderNo }}</h3>
                    <p class="pet-name">宠物：{{ feedback.petName }}</p>
                  </div>
                  <div class="meta-right">
                    <span class="feedback-time">{{ formatDate(feedback.createTime) }}</span>
                    <el-tag :type="feedback.status === 1 ? 'success' : 'warning'" size="small">
                      {{ feedback.status === 1 ? '已回复' : '待回复' }}
                    </el-tag>
                  </div>
                </div>
              </div>
              <!-- 服务人员主动发送的反馈 -->
              <div v-if="feedback.isFromProvider" class="feedback-content provider-feedback">
                <strong>服务人员主动反馈：</strong>
                <p>{{ feedback.content }}</p>
                <span class="provider-name">来自：{{ feedback.serviceProviderName || '服务人员' }}</span>
                <div v-if="feedback.response" class="my-response">
                  <strong>我的回复：</strong>
                  <p>{{ feedback.response }}</p>
                  <span class="response-time">回复时间：{{ formatDate(feedback.responseTime) }}</span>
                </div>
                <div v-else class="feedback-actions" style="margin-top: 12px;">
                  <el-button type="primary" size="small" @click="handleReplyToProvider(feedback)">
                    回复
                  </el-button>
                </div>
              </div>
              <!-- 宠主提交的反馈 -->
              <template v-else>
                <div class="feedback-content">
                  <strong>我的反馈：</strong>
                  <p>{{ feedback.content }}</p>
                </div>
                <div v-if="feedback.response" class="feedback-response">
                  <strong>服务人员回复：</strong>
                  <p>{{ feedback.response }}</p>
                  <span class="response-time">回复时间：{{ formatDate(feedback.responseTime) }}</span>
                </div>
              </template>
            </div>
          </div>
          
          <!-- 分页 -->
          <el-pagination
            v-if="total > 0"
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadFeedbackList"
            @current-change="loadFeedbackList"
            style="margin-top: 20px; justify-content: center;"
          />
        </el-tab-pane>
        
        <el-tab-pane label="提交反馈" name="submit-feedback">
          <el-card shadow="hover">
            <template #header>
              <h3 style="margin: 0;">提交新反馈</h3>
            </template>
            
            <el-form :model="feedbackForm" label-width="120px">
              <el-form-item label="选择订单" required>
                <el-select
                  v-model="feedbackForm.orderId"
                  placeholder="请选择要反馈的订单"
                  style="width: 100%"
                  filterable
                  @change="handleOrderChange"
                >
                  <el-option
                    v-for="order in availableOrders"
                    :key="order.id"
                    :label="`${order.orderNo} - ${order.petName}`"
                    :value="order.id"
                  >
                    <div>
                      <div>{{ order.orderNo }}</div>
                      <div style="font-size: 12px; color: #909399;">
                        宠物：{{ order.petName }} | 服务人员：{{ order.serviceProviderName || '未分配' }}
                      </div>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
              
              <el-form-item label="反馈内容" required>
                <el-input
                  v-model="feedbackForm.content"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入您的反馈内容，我们会认真处理您的意见..."
                  maxlength="1000"
                  show-word-limit
                />
              </el-form-item>
              
              <el-alert
                type="info"
                :closable="false"
                style="margin-bottom: 20px;"
              >
                您的反馈将发送给对应的服务人员，我们会及时处理并回复
              </el-alert>
              
              <el-form-item>
                <el-button type="primary" @click="submitFeedback" :loading="submittingFeedback">
                  提交反馈
                </el-button>
                <el-button @click="resetForm">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 回复服务人员主动反馈对话框 -->
    <el-dialog
      v-model="replyDialogVisible"
      title="回复服务人员"
      width="600px"
    >
      <el-form label-width="100px">
        <el-form-item label="订单号">
          <span>{{ currentProviderFeedback?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="服务人员反馈">
          <p style="color: #606266; margin: 0;">{{ currentProviderFeedback?.content }}</p>
        </el-form-item>
        <el-form-item label="回复内容" required>
          <el-input
            v-model="replyForm.response"
            type="textarea"
            :rows="6"
            placeholder="请输入您的回复内容..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply" :loading="replying">
          提交回复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { 
  ElMessage, 
  ElPageHeader, 
  ElTabs, 
  ElTabPane, 
  ElCard, 
  ElForm, 
  ElFormItem, 
  ElSelect, 
  ElOption, 
  ElInput, 
  ElButton, 
  ElEmpty, 
  ElPagination, 
  ElAlert, 
  ElTag 
} from 'element-plus';
import { createFeedback, getUserFeedbackList, respondToProviderFeedback, type FeedbackVO, type FeedbackResponseDTO } from '@/api/feedback';
import { orderApi, type OrderVO } from '@/api/order';
import dayjs from 'dayjs';

const router = useRouter();
const activeTab = ref('my-feedback');
const loading = ref(false);
const submittingFeedback = ref(false);
const feedbackList = ref<FeedbackVO[]>([]);
const availableOrders = ref<OrderVO[]>([]);
const total = ref(0);
const pagination = reactive({
  current: 1,
  pageSize: 10,
});

const feedbackForm = reactive({
  orderId: undefined as number | undefined,
  content: '',
});

// 回复服务人员主动反馈
const replyDialogVisible = ref(false);
const replying = ref(false);
const currentProviderFeedback = ref<FeedbackVO | null>(null);
const replyForm = reactive({
  response: '',
});

const goBack = () => {
  router.back();
};

const formatDate = (date?: string) => {
  if (!date) return '';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
};

const handleTabChange = () => {
  if (activeTab.value === 'my-feedback') {
    pagination.current = 1;
    loadFeedbackList();
  } else if (activeTab.value === 'submit-feedback') {
    loadAvailableOrders();
  }
};

const loadFeedbackList = async () => {
  loading.value = true;
  try {
    const res = await getUserFeedbackList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    });
    
    if (res && res.code === 200 && res.data) {
      feedbackList.value = res.data.records || [];
      total.value = res.data.total || 0;
    } else {
      console.error('加载反馈列表失败:', res);
      ElMessage.error(res?.message || '加载反馈列表失败');
      // 设置默认值，避免页面空白
      feedbackList.value = [];
      total.value = 0;
    }
  } catch (error: any) {
    console.error('加载反馈列表失败:', error);
    const errorMsg = error?.response?.data?.message || error?.message || '加载反馈列表失败，请稍后重试';
    ElMessage.error(errorMsg);
    // 即使出错也要设置空数组，避免页面空白
    feedbackList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const loadAvailableOrders = async () => {
  try {
    const res = await orderApi.getOrderList({
      pageNum: 1,
      pageSize: 100,
    });
    
    if (res && res.code === 200 && res.data) {
      // 只显示已确认且有服务人员的订单
      availableOrders.value = (res.data.records || []).filter((order: OrderVO) => {
        return order.status >= 1 && order.serviceProviderId;
      });
    }
  } catch (error) {
    console.error('加载订单列表失败:', error);
  }
};

const handleOrderChange = (orderId: number) => {
  // 订单改变时可以做一些处理
  console.log('选择的订单ID:', orderId);
};

const resetForm = () => {
  feedbackForm.orderId = undefined;
  feedbackForm.content = '';
};

const submitFeedback = async () => {
  if (!feedbackForm.orderId) {
    ElMessage.warning('请选择要反馈的订单');
    return;
  }
  
  if (!feedbackForm.content.trim()) {
    ElMessage.warning('请输入反馈内容');
    return;
  }
  
  submittingFeedback.value = true;
  try {
    const res = await createFeedback({
      orderId: feedbackForm.orderId,
      content: feedbackForm.content.trim(),
    });
    
    if (res && res.code === 200) {
      ElMessage.success('反馈提交成功，服务人员会及时处理');
      resetForm();
      
      // 切换到"我的反馈"标签页并刷新
      activeTab.value = 'my-feedback';
      await loadFeedbackList();
      
      // 触发反馈提交事件，通知服务端刷新
      window.dispatchEvent(new CustomEvent('feedback-submitted', {
        detail: { orderId: feedbackForm.orderId }
      }));
    } else {
      ElMessage.error(res?.message || '提交反馈失败');
    }
  } catch (error: any) {
    console.error('提交反馈失败:', error);
    const errorMsg = error?.response?.data?.message || error?.message || '提交反馈失败，请稍后重试';
    ElMessage.error(errorMsg);
  } finally {
    submittingFeedback.value = false;
  }
};

// 处理回复服务人员主动反馈
const handleReplyToProvider = (feedback: FeedbackVO) => {
  currentProviderFeedback.value = feedback;
  replyForm.response = '';
  replyDialogVisible.value = true;
};

// 提交回复
const submitReply = async () => {
  if (!replyForm.response.trim()) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  
  if (!currentProviderFeedback.value?.id) {
    ElMessage.error('反馈信息不存在');
    return;
  }
  
  replying.value = true;
  try {
    const res = await respondToProviderFeedback({
      feedbackId: currentProviderFeedback.value.id,
      response: replyForm.response.trim(),
    });
    
    if (res && res.code === 200) {
      ElMessage.success('回复成功');
      replyDialogVisible.value = false;
      await loadFeedbackList();
      // 触发反馈回复事件
      window.dispatchEvent(new CustomEvent('feedback-replied', {
        detail: { feedbackId: currentProviderFeedback.value?.id }
      }));
    } else {
      ElMessage.error(res?.message || '回复失败');
    }
  } catch (error: any) {
    console.error('回复失败:', error);
    ElMessage.error(error?.message || '回复失败，请稍后重试');
  } finally {
    replying.value = false;
  }
};

// 监听反馈提交事件
const handleFeedbackSubmitted = () => {
  if (activeTab.value === 'my-feedback') {
    loadFeedbackList();
  }
};

onMounted(() => {
  console.log('ServiceFeedback 页面已挂载');
  try {
    loadFeedbackList();
    window.addEventListener('feedback-submitted', handleFeedbackSubmitted as EventListener);
  } catch (error) {
    console.error('ServiceFeedback 页面初始化失败:', error);
    ElMessage.error('页面初始化失败，请刷新重试');
  }
});

onUnmounted(() => {
  window.removeEventListener('feedback-submitted', handleFeedbackSubmitted as EventListener);
});
</script>

<style scoped lang="scss">
.service-feedback-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  
  .content-wrapper {
    margin-top: 20px;
  }
  
  .feedback-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-top: 20px;
  }
  
  .feedback-item {
    padding: 20px;
    background: #f9fafb;
    border-radius: 8px;
    border-left: 3px solid #409eff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

    .feedback-header {
      margin-bottom: 12px;

      .feedback-meta {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;

        h3 {
          margin: 0 0 8px 0;
          font-size: 16px;
          color: #303133;
        }

        .pet-name {
          margin: 0;
          font-size: 14px;
          color: #909399;
        }

        .meta-right {
          display: flex;
          flex-direction: column;
          align-items: flex-end;
          gap: 8px;

          .feedback-time {
            color: #909399;
            font-size: 12px;
          }
        }
      }
    }

    .feedback-content {
      margin-bottom: 12px;

      strong {
        color: #303133;
        font-size: 14px;
        display: block;
        margin-bottom: 8px;
      }

      p {
        color: #606266;
        font-size: 14px;
        line-height: 1.6;
        margin: 0;
        white-space: pre-wrap;
        word-break: break-word;
      }
    }

    .provider-feedback {
      background: #f0f9ff;
      padding: 12px;
      border-radius: 6px;
      border-left: 3px solid #409eff;
      
      .provider-name {
        display: block;
        margin-top: 8px;
        font-size: 12px;
        color: #909399;
      }
    }

    .feedback-response {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #e4e7ed;
      background: #f0f9ff;
      padding: 12px;
      border-radius: 6px;

      strong {
        color: #303133;
        font-size: 14px;
        display: block;
        margin-bottom: 8px;
      }

      p {
        color: #606266;
        font-size: 14px;
        line-height: 1.6;
        margin: 0 0 8px 0;
        white-space: pre-wrap;
        word-break: break-word;
      }

      .response-time {
        color: #909399;
        font-size: 12px;
      }
    }

    .my-response {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #e4e7ed;
      background: #f0f9ff;
      padding: 12px;
      border-radius: 6px;

      strong {
        color: #303133;
        font-size: 14px;
        display: block;
        margin-bottom: 8px;
      }

      p {
        color: #606266;
        font-size: 14px;
        line-height: 1.6;
        margin: 0 0 8px 0;
        white-space: pre-wrap;
        word-break: break-word;
      }

      .response-time {
        color: #909399;
        font-size: 12px;
      }
    }

    .feedback-actions {
      margin-top: 12px;
    }
  }
}
</style>

