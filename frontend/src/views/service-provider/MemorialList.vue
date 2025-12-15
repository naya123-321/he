<template>
  <div class="memorial-list-container">
    <el-page-header @back="goBack" content="纪念册设计" />
    
    <div class="content-wrapper">
      <div v-if="errorMessage" class="error-message">
        <el-alert :title="errorMessage" type="error" :closable="false" />
      </div>
      
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>纪念册列表</span>
            <el-button type="primary" @click="refreshList">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>
        
        <el-table
          v-loading="loading"
          :data="memorialList"
          border
          style="width: 100%"
        >
          <el-table-column prop="title" label="纪念册标题" width="250" />
          <el-table-column prop="petName" label="宠物姓名" width="120" />
          <el-table-column prop="petType" label="宠物类型" width="120" />
          <el-table-column prop="statusText" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="goToDesign(row.id)"
              >
                设计
              </el-button>
              <el-button
                size="small"
                @click="viewDetail(row.id)"
              >
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <el-empty v-if="!loading && memorialList.length === 0" description="暂无纪念册" />
        
        <el-pagination
          v-if="total > 0"
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          style="margin-top: 20px; justify-content: flex-end;"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElPageHeader, ElCard, ElTable, ElTableColumn, ElTag, ElButton, ElEmpty, ElPagination, ElIcon, ElAlert } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { memorialApi } from '@/api/memorial';
import type { MemorialVO } from '@/api/memorial';

const router = useRouter();
const loading = ref(false);
const memorialList = ref<MemorialVO[]>([]);
const total = ref(0);
const errorMessage = ref<string>('');

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
});

// 返回上一页
const goBack = () => {
  router.back();
};

// 获取状态类型
const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    0: 'info',      // 草稿
    1: 'warning',   // 待审核
    2: 'success',   // 已发布
    3: 'danger',    // 已下架
  };
  return typeMap[status] || 'info';
};

// 跳转到设计页面
const goToDesign = (id: number) => {
  router.push(`/service-provider/memorial-design/${id}`);
};

// 查看详情
const viewDetail = (id: number) => {
  router.push(`/memorial/detail/${id}`);
};

// 刷新列表
const refreshList = () => {
  loadMemorialList();
};

// 加载纪念册列表
const loadMemorialList = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    console.log('开始加载纪念册列表...');
    const res = await memorialApi.getMemorialList({
      status: undefined, // 获取所有状态的纪念册
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    });
    
    console.log('纪念册列表响应:', res);
    
    if (res && res.code === 200 && res.data) {
      memorialList.value = res.data.records || [];
      total.value = res.data.total || 0;
      console.log('加载纪念册列表成功，数量:', memorialList.value.length);
      if (memorialList.value.length === 0) {
        errorMessage.value = '暂无纪念册数据';
      }
    } else {
      const errorMsg = res?.message || '加载纪念册列表失败';
      console.error('加载纪念册列表失败:', errorMsg, res);
      errorMessage.value = errorMsg;
      ElMessage.error(errorMsg);
      memorialList.value = [];
      total.value = 0;
    }
  } catch (error: any) {
    console.error('加载纪念册列表异常:', error);
    const errorMsg = error?.response?.data?.message || error?.message || '加载纪念册列表失败';
    errorMessage.value = errorMsg;
    ElMessage.error(errorMsg);
    memorialList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

// 分页大小改变
const handleSizeChange = (size: number) => {
  pagination.pageSize = size;
  pagination.pageNum = 1;
  loadMemorialList();
};

// 页码改变
const handlePageChange = (page: number) => {
  pagination.pageNum = page;
  loadMemorialList();
};

onMounted(() => {
  loadMemorialList();
});
</script>

<style scoped lang="scss">
.memorial-list-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .error-message {
    margin-bottom: 20px;
  }
}
</style>

