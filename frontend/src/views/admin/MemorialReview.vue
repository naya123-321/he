<template>
  <div class="memorial-review-container">
    <el-page-header @back="goBack" content="纪念册审核" />
    
    <div class="content-wrapper">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="待审核" name="pending">
          <el-table :data="pendingMemorials" style="width: 100%">
            <el-table-column prop="title" label="纪念册标题" width="200" />
            <el-table-column prop="petName" label="宠物名称" width="150" />
            <el-table-column prop="ownerName" label="创建人" width="150" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="reviewMemorial(row.id, 'approve')">
                  通过
                </el-button>
                <el-button type="danger" size="small" @click="reviewMemorial(row.id, 'reject')">
                  拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="已审核" name="reviewed">
          <el-table :data="reviewedMemorials" style="width: 100%">
            <el-table-column prop="title" label="纪念册标题" width="200" />
            <el-table-column prop="petName" label="宠物名称" width="150" />
            <el-table-column prop="ownerName" label="创建人" width="150" />
            <el-table-column prop="reviewTime" label="审核时间" width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'approved' ? 'success' : 'danger'">
                  {{ row.status === 'approved' ? '已通过' : '已拒绝' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElPageHeader, ElTabs, ElTabPane, ElTable, ElTableColumn, ElTag, ElButton } from 'element-plus';

const router = useRouter();
const activeTab = ref('pending');
const pendingMemorials = ref<any[]>([]);
const reviewedMemorials = ref<any[]>([]);

// 返回上一页
const goBack = () => {
  router.back();
};

const reviewMemorial = (id: number, action: 'approve' | 'reject') => {
  // TODO: 审核纪念册
};

onMounted(() => {
  // TODO: 加载审核数据
});
</script>

<style scoped lang="scss">
.memorial-review-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }
}
</style>


