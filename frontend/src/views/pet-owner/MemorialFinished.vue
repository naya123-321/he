<template>
  <div class="memorial-finished-container">
    <el-page-header @back="goBack" content="已完成的纪念册" />
    
    <div class="content-wrapper">
      <el-empty v-if="memorialList.length === 0" description="暂无已完成的纪念册" />
      
      <div v-else class="memorial-grid">
        <div
          v-for="memorial in memorialList"
          :key="memorial.id"
          class="memorial-card"
          @click="viewMemorial(memorial.id)"
        >
          <div class="card-image">
            <img :src="memorial.coverImage || '/default-memorial.jpg'" :alt="memorial.title" />
          </div>
          <div class="card-content">
            <h3>{{ memorial.title }}</h3>
            <p class="subtitle">{{ memorial.subtitle }}</p>
            <div class="card-footer">
              <span class="date">{{ formatDate(memorial.createTime) }}</span>
              <el-button type="primary" size="small" @click.stop="previewMemorial(memorial.id)">
                预览
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElPageHeader, ElEmpty, ElButton } from 'element-plus';
import dayjs from 'dayjs';

const router = useRouter();
const memorialList = ref<any[]>([]);

const goBack = () => {
  router.back();
};

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD');
};

const viewMemorial = (id: number) => {
  router.push(`/memorial/preview/${id}`);
};

const previewMemorial = (id: number) => {
  router.push(`/memorial/preview/${id}`);
};

onMounted(() => {
  // TODO: 加载已完成的纪念册列表
});
</script>

<style scoped lang="scss">
.memorial-finished-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }
  
  .memorial-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
    margin-top: 20px;
  }
  
  .memorial-card {
    background: white;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    cursor: pointer;
    transition: transform 0.2s;
    
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
    
    .card-image {
      width: 100%;
      height: 200px;
      overflow: hidden;
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
    
    .card-content {
      padding: 16px;
      
      h3 {
        margin: 0 0 8px;
        font-size: 18px;
        color: #303133;
      }
      
      .subtitle {
        margin: 0 0 12px;
        font-size: 14px;
        color: #606266;
      }
      
      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .date {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}
</style>















