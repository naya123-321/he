<template>
  <div class="special-requests-container">
    <el-page-header @back="goBack" content="特殊需求管理" />
    
    <div class="content-wrapper">
      <!-- 筛选栏 -->
      <el-card shadow="hover" style="margin-bottom: 20px">
        <el-form :inline="true" :model="filterForm">
          <el-form-item label="状态筛选">
            <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 150px">
              <el-option label="待审核" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已拒绝" :value="2" />
              <el-option label="处理中" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadRequests">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 特殊需求列表 -->
      <el-card shadow="hover">
        <el-table 
          :data="requestsList" 
          v-loading="loading"
          style="width: 100%"
          border
        >
          <el-table-column prop="orderNo" label="订单号" width="150" />
          <el-table-column prop="userName" label="用户姓名" width="120" />
          <el-table-column prop="userPhone" label="联系电话" width="130" />
          <el-table-column prop="requestType" label="需求类型" width="120">
            <template #default="{ row }">
              <el-tag type="info" size="small">{{ row.requestType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="需求描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="statusText" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button 
                v-if="row.status === 0"
                type="primary" 
                size="small" 
                @click="openReviewDialog(row)"
              >
                审核
              </el-button>
              <el-button 
                size="small" 
                @click="viewDetail(row)"
              >
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div style="margin-top: 20px; text-align: right">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadRequests"
            @current-change="loadRequests"
          />
        </div>
      </el-card>
    </div>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="审核特殊需求"
      width="600px"
      @close="resetReviewForm"
    >
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="订单号">
          <el-input :value="currentRequest?.orderNo" disabled />
        </el-form-item>
        <el-form-item label="用户姓名">
          <el-input :value="currentRequest?.userName" disabled />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input :value="currentRequest?.userPhone" disabled />
        </el-form-item>
        <el-form-item label="需求类型">
          <el-input :value="currentRequest?.requestType" disabled />
        </el-form-item>
        <el-form-item label="需求描述">
          <el-input 
            :value="currentRequest?.description" 
            type="textarea" 
            :rows="4"
            disabled 
          />
        </el-form-item>
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewForm.adminComment"
            type="textarea"
            :rows="3"
            placeholder="请输入审核意见（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview" :loading="reviewing">
          提交审核
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="特殊需求详情"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="currentRequest">
        <el-descriptions-item label="订单号">{{ currentRequest.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户姓名">{{ currentRequest.userName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentRequest.userPhone }}</el-descriptions-item>
        <el-descriptions-item label="需求类型">
          <el-tag type="info">{{ currentRequest.requestType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="getStatusType(currentRequest.status)">
            {{ currentRequest.statusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="需求描述" :span="2">
          <div style="white-space: pre-wrap">{{ currentRequest.description }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(currentRequest.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" v-if="currentRequest.updateTime">
          {{ formatTime(currentRequest.updateTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2" v-if="currentRequest.adminComment">
          <div style="white-space: pre-wrap">{{ currentRequest.adminComment }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="审核人" v-if="currentRequest.adminName">
          {{ currentRequest.adminName }}
        </el-descriptions-item>
        <el-descriptions-item label="审核时间" v-if="currentRequest.reviewTime">
          {{ formatTime(currentRequest.reviewTime) }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { specialRequestApi, type SpecialRequestVO } from '@/api/specialRequest';
import dayjs from 'dayjs';

const router = useRouter();
const loading = ref(false);
const requestsList = ref<SpecialRequestVO[]>([]);

// 筛选表单
const filterForm = reactive({
  status: undefined as number | undefined,
});

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

// 审核对话框
const reviewDialogVisible = ref(false);
const reviewing = ref(false);
const currentRequest = ref<SpecialRequestVO | null>(null);
const reviewForm = reactive({
  status: 1, // 1:通过 2:拒绝
  adminComment: '',
});

// 详情对话框
const detailDialogVisible = ref(false);

// 返回上一页
const goBack = () => {
  router.back();
};

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
};

// 获取状态类型
const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    0: 'warning',  // 待审核
    1: 'success',  // 已通过
    2: 'danger',   // 已拒绝
    3: 'info',     // 处理中
  };
  return typeMap[status] || 'info';
};

// 加载特殊需求列表
const loadRequests = async () => {
  loading.value = true;
  try {
    const res = await specialRequestApi.getSpecialRequestList({
      status: filterForm.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    });
    
    if (res.code === 200) {
      requestsList.value = res.data.records;
      pagination.total = res.data.total;
    } else {
      ElMessage.error(res.message || '加载失败');
    }
  } catch (error: any) {
    console.error('加载特殊需求列表失败:', error);
    ElMessage.error(error?.message || '加载失败');
  } finally {
    loading.value = false;
  }
};

// 重置筛选
const resetFilter = () => {
  filterForm.status = undefined;
  pagination.current = 1;
  loadRequests();
};

// 打开审核对话框
const openReviewDialog = (request: SpecialRequestVO) => {
  currentRequest.value = request;
  reviewForm.status = 1;
  reviewForm.adminComment = '';
  reviewDialogVisible.value = true;
};

// 重置审核表单
const resetReviewForm = () => {
  reviewForm.status = 1;
  reviewForm.adminComment = '';
  currentRequest.value = null;
};

// 提交审核
const submitReview = async () => {
  if (!currentRequest.value) return;
  
  reviewing.value = true;
  try {
    const res = await specialRequestApi.reviewSpecialRequest({
      requestId: currentRequest.value.id,
      status: reviewForm.status,
      adminComment: reviewForm.adminComment,
    });
    
    if (res.code === 200) {
      ElMessage.success('审核成功');
      reviewDialogVisible.value = false;
      loadRequests();
    } else {
      ElMessage.error(res.message || '审核失败');
    }
  } catch (error: any) {
    console.error('审核失败:', error);
    ElMessage.error(error?.message || '审核失败');
  } finally {
    reviewing.value = false;
  }
};

// 查看详情
const viewDetail = async (request: SpecialRequestVO) => {
  try {
    const res = await specialRequestApi.getSpecialRequestDetail(request.id);
    if (res.code === 200) {
      currentRequest.value = res.data;
      detailDialogVisible.value = true;
    } else {
      ElMessage.error(res.message || '获取详情失败');
    }
  } catch (error: any) {
    console.error('获取详情失败:', error);
    ElMessage.error(error?.message || '获取详情失败');
  }
};

// 初始化
onMounted(() => {
  loadRequests();
});
</script>

<style scoped lang="scss">
.special-requests-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }
}
</style>
