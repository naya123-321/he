<template>
  <div class="memorial-preview-container">
    <el-page-header @back="goBack" content="纪念册预览" />
    
    <div class="content-wrapper">
      <MemorialPreviewComponent
        v-model="previewVisible"
        :memorial-info="memorialInfo"
        :pages="pages"
        :elements="elements"
        @export="handleExport"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElPageHeader } from 'element-plus';
import MemorialPreviewComponent from '@/components/memorial/MemorialPreview.vue';

const router = useRouter();
const route = useRoute();
const previewVisible = ref(true);
const memorialInfo = ref<any>(null);
const pages = ref<any[]>([]);
const elements = ref<any[]>([]);

const goBack = () => {
  router.back();
};

const handleExport = (type: string) => {
  // TODO: 处理导出
};

onMounted(() => {
  const id = route.params.id;
  if (id) {
    // TODO: 加载纪念册数据
    memorialInfo.value = {
      id: Number(id),
      title: '纪念册预览',
      subtitle: '美好的回忆永远珍藏'
    };
    pages.value = [];
    elements.value = [];
  }
});
</script>

<style scoped lang="scss">
.memorial-preview-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }
}
</style>















