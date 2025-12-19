<template>
  <div class="grief-resources-container">
    <el-page-header @back="goBack" content="哀伤支持资源" />

    <div class="content-wrapper">
      <div v-loading="loading" class="resources-list">
        <div
          v-for="item in articles"
          :key="item.id"
          class="resource-item"
          @click="openDetail(item.id)"
        >
          <div v-if="item.coverImage" class="resource-cover">
            <img :src="item.coverImage" alt="封面" />
          </div>
          <div class="resource-content">
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary || item.content || "暂无描述" }}</p>
            <div class="resource-meta">
              <span v-if="item.author" class="author">{{ item.author }}</span>
              <span class="date">{{ formatDate(item.createTime) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && articles.length === 0" description="暂无文章资源" :image-size="150" />
      </div>
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      :title="detail?.title || '资源详情'"
      width="820px"
      @close="handleCloseDetail"
    >
      <div v-if="detail" class="resource-detail">
        <div v-if="detail.coverImage" class="detail-cover">
          <img :src="detail.coverImage" alt="封面" />
        </div>

        <div v-if="detail.summary" class="detail-summary">
          {{ detail.summary }}
        </div>

        <div class="detail-content">
          <div v-if="detail.content" style="white-space: pre-wrap">
            {{ detail.content }}
          </div>
          <el-empty v-else description="暂无内容" :image-size="120" />
        </div>

        <div class="detail-meta">
          <span v-if="detail.author">作者：{{ detail.author }}</span>
          <span>发布时间：{{ formatDate(detail.createTime) }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import dayjs from "dayjs";
import { griefResourceApi, type GriefResourceVO } from "@/api/griefResource";

const router = useRouter();

const loading = ref(false);
const list = ref<GriefResourceVO[]>([]);

const detailDialogVisible = ref(false);
const detail = ref<GriefResourceVO | null>(null);

const articles = computed(() => list.value.filter((r) => r.type === "article"));

function goBack() {
  router.back();
}

function formatDate(date: string) {
  return dayjs(date).format("YYYY-MM-DD");
}

async function load(type?: string) {
  loading.value = true;
  try {
    const res = await griefResourceApi.getEnabledGriefResources(type);
    // 宠主端只展示文章资源
    list.value = (res.data || []).filter((r: any) => r?.type === "article");
  } catch (e: any) {
    // request.ts 已提示，这里只兜底
    list.value = [];
    ElMessage.error(e?.message || "加载资源失败");
  } finally {
    loading.value = false;
  }
}

async function openDetail(id: number) {
  try {
    const res = await griefResourceApi.getGriefResourceDetail(id);
    detail.value = res.data;
    detailDialogVisible.value = true;
  } catch (e: any) {
    ElMessage.error(e?.message || "获取详情失败");
  }
}

function handleCloseDetail() {
  detail.value = null;
}

onMounted(() => {
  load("article");
});
</script>

<style scoped lang="scss">
// 统一配色方案（与其他页面保持一致）
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$text-primary: #303133;
$text-secondary: #606266;
$text-light: #909399;
$border-color: #ebeef5;
$bg-light: #f5f7fa;
$bg-white: #ffffff;

.grief-resources-container {
  padding: 30px 20px;
  max-width: 1200px;
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
    margin-top: 20px;
  }

  .resources-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
    margin-top: 20px;
  }

  .resource-item {
    background: $bg-white;
    padding: 24px;
    border-radius: 12px;
    border: 1px solid $border-color;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    cursor: pointer;
    transition: all 0.3s ease;
    display: flex;
    gap: 20px;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
      border-color: $primary-color;
    }

    .resource-cover {
      width: 220px;
      height: 160px;
      flex-shrink: 0;
      border-radius: 10px;
      overflow: hidden;
      position: relative;
      background: $bg-light;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.3s ease;
      }

      &:hover img {
        transform: scale(1.05);
      }

      .play-icon {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 56px;
        height: 56px;
        background: rgba(0, 0, 0, 0.6);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 22px;
      }
    }

    .resource-content {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      h3 {
        margin: 0 0 12px;
        font-size: 20px;
        font-weight: 600;
        color: $text-primary;
        line-height: 1.4;
        transition: color 0.3s;
      }

      p {
        margin: 0 0 16px;
        font-size: 14px;
        color: $text-secondary;
        line-height: 1.8;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .resource-meta {
        display: flex;
        align-items: center;
        gap: 20px;
        font-size: 13px;
        color: $text-light;
        padding-top: 12px;
        border-top: 1px solid $border-color;

        .author {
          color: $text-secondary;
          font-weight: 500;
        }
      }
    }

    &:hover .resource-content h3 {
      color: $primary-color;
    }
  }

  // 优化空状态
  :deep(.el-empty) {
    padding: 80px 0;
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
        font-size: 20px;
        font-weight: 600;
      }

      .el-dialog__headerbtn .el-dialog__close {
        color: $bg-white;
        font-size: 20px;
      }
    }

    .el-dialog__body {
      padding: 24px;
      max-height: 70vh;
      overflow-y: auto;
    }
  }

  .resource-detail {
    .detail-cover {
      width: 100%;
      max-height: 420px;
      margin-bottom: 20px;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

      img {
        width: 100%;
        height: auto;
        object-fit: cover;
      }
    }

    .detail-summary {
      margin-bottom: 20px;
      padding: 16px;
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.08) 0%, rgba(103, 194, 58, 0.05) 100%);
      border-radius: 10px;
      border-left: 4px solid $primary-color;
      color: $text-primary;
      line-height: 1.8;
      font-size: 15px;
    }

    .detail-content {
      margin-bottom: 20px;
      line-height: 2;
      color: $text-primary;
      font-size: 15px;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .detail-meta {
      display: flex;
      gap: 24px;
      padding-top: 20px;
      border-top: 2px solid $border-color;
      font-size: 14px;
      color: $text-light;

      span {
        display: flex;
        align-items: center;
        gap: 6px;

        &::before {
          content: "•";
          color: $primary-color;
          font-weight: bold;
        }
      }
    }
  }
}
</style>



