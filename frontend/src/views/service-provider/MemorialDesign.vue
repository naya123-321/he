<template>
  <div class="memorial-design-container">
    <el-page-header @back="goBack" content="纪念册设计" />
    
    <div class="content-wrapper">
      <el-empty v-if="!memorialInfo" description="未找到纪念册信息" />
      
      <div v-else>
        <div class="memorial-header">
          <h2>{{ memorialInfo.title }}</h2>
          <p>{{ memorialInfo.subtitle }}</p>
        </div>
        
        <el-alert
          message="此功能正在开发中，敬请期待"
          type="info"
          show-icon
          :closable="false"
          style="margin-bottom: 20px;"
        />
        
        <div class="design-tools">
          <el-button type="primary" @click="openEditor">
            打开编辑器
          </el-button>
          <el-button @click="previewMemorial">
            预览纪念册
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElPageHeader, ElEmpty, ElAlert, ElButton } from 'element-plus';

const router = useRouter();
const route = useRoute();
const memorialInfo = ref<any>(null);

const goBack = () => {
  router.back();
};

const openEditor = () => {
  if (memorialInfo.value?.id) {
    router.push(`/memorial/edit/${memorialInfo.value.id}`);
  }
};

const previewMemorial = () => {
  if (memorialInfo.value?.id) {
    router.push(`/memorial/preview/${memorialInfo.value.id}`);
  }
};

onMounted(() => {
  const id = route.params.id;
  if (id) {
    // TODO: 加载纪念册信息
    memorialInfo.value = {
      id: Number(id),
      title: '纪念册设计',
      subtitle: '为宠物创建美好的回忆'
    };
  }
});
</script>

<style scoped lang="scss">
.memorial-design-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }
  
  .memorial-header {
    margin-bottom: 20px;
    
    h2 {
      margin: 0 0 8px;
      font-size: 24px;
      color: #303133;
    }
    
    p {
      margin: 0;
      font-size: 14px;
      color: #606266;
    }
  }
  
  .design-tools {
    display: flex;
    gap: 12px;
  }
}
</style>















