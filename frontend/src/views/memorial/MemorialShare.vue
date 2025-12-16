<template>
  <div class="memorial-share-container">
    <el-page-header @back="goBack" content="纪念册分享预览" />

    <div class="content-wrapper">
      <el-card v-loading="loading" shadow="never">
        <el-empty v-if="!loading && !memorial" description="未找到可分享的纪念册" />

        <template v-else-if="memorial">
          <div class="header">
            <div class="title">{{ memorial.title }}</div>
            <div class="sub">{{ memorial.subtitle || "" }}</div>
            <div class="meta">
              <el-tag type="success">已通过审核</el-tag>
              <el-tag type="info">{{ memorial.designStatusText }}</el-tag>
            </div>
          </div>

          <el-divider />

          <div v-if="(memorial.designFinalImages && memorial.designFinalImages.length) || memorial.designFinalPdfUrl" class="section">
            <div class="section-title">最终版</div>
            <div v-if="memorial.designFinalImages && memorial.designFinalImages.length" class="images">
              <el-image
                v-for="url in memorial.designFinalImages"
                :key="url"
                :src="url"
                :preview-src-list="memorial.designFinalImages"
                fit="cover"
                style="width: 200px; height: 140px; border-radius: 8px"
              />
            </div>
            <div v-if="memorial.designFinalPdfUrl" class="pdf">
              <el-button type="primary" link @click="openUrl(memorial.designFinalPdfUrl)">在线查看PDF</el-button>
              <el-button link @click="downloadUrl(memorial.designFinalPdfUrl)">下载PDF</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无最终版内容" />
        </template>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { memorialApi, type MemorialVO } from "@/api/memorial";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const memorial = ref<MemorialVO | null>(null);

function goBack() {
  router.back();
}

function openUrl(url: string) {
  window.open(url, "_blank");
}

function downloadUrl(url: string) {
  const a = document.createElement("a");
  a.href = url;
  a.download = "";
  a.target = "_blank";
  document.body.appendChild(a);
  a.click();
  a.remove();
}

async function load() {
  const token = String(route.params.token || "");
  if (!token) return;

  loading.value = true;
  try {
    const res = await memorialApi.getSharedMemorial(token);
    memorial.value = res.data;
  } catch (e: any) {
    memorial.value = null;
    ElMessage.error(e?.message || "加载分享内容失败");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  load();
});
</script>

<style scoped lang="scss">
.memorial-share-container {
  padding: 16px;

  .content-wrapper {
    margin-top: 16px;
  }

  .header {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .title {
    font-size: 20px;
    font-weight: 800;
    color: #303133;
  }

  .sub {
    color: #909399;
    font-size: 13px;
  }

  .meta {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .section-title {
    font-weight: 700;
    margin-bottom: 10px;
  }

  .images {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 10px;
  }
}
</style>


