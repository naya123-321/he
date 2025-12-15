<template>
  <div class="grief-resources-container">
    <el-page-header @back="goBack" content="哀伤支持资源" />

    <div class="content-wrapper">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="文章" name="article">
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
        </el-tab-pane>

        <el-tab-pane label="视频" name="video">
          <div v-loading="loading" class="resources-list">
            <div
              v-for="item in videos"
              :key="item.id"
              class="resource-item"
              @click="openDetail(item.id)"
            >
              <div v-if="item.coverImage" class="resource-cover">
                <img :src="item.coverImage" alt="封面" />
                <div class="play-icon">
                  <el-icon><VideoPlay /></el-icon>
                </div>
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
            <el-empty v-if="!loading && videos.length === 0" description="暂无视频资源" :image-size="150" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      :title="detail?.title || '资源详情'"
      width="820px"
      @close="detail = null"
    >
      <div v-if="detail" class="resource-detail">
        <div v-if="detail.coverImage" class="detail-cover">
          <img :src="detail.coverImage" alt="封面" />
        </div>

        <div v-if="detail.summary" class="detail-summary">
          {{ detail.summary }}
        </div>

        <div v-if="detail.type === 'video' && detail.fileUrl" class="video-player">
          <video :src="detail.fileUrl" controls style="width: 100%; max-height: 520px" />
        </div>

        <div v-else class="detail-content">
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
import { VideoPlay } from "@element-plus/icons-vue";
import dayjs from "dayjs";
import { griefResourceApi, type GriefResourceVO } from "@/api/griefResource";

const router = useRouter();

const activeTab = ref<"article" | "video">("article");
const loading = ref(false);
const list = ref<GriefResourceVO[]>([]);

const detailDialogVisible = ref(false);
const detail = ref<GriefResourceVO | null>(null);

const articles = computed(() => list.value.filter((r) => r.type === "article"));
const videos = computed(() => list.value.filter((r) => r.type === "video"));

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
    list.value = res.data || [];
  } catch (e: any) {
    // request.ts 已提示，这里只兜底
    list.value = [];
    ElMessage.error(e?.message || "加载资源失败");
  } finally {
    loading.value = false;
  }
}

function handleTabChange(name: string | number) {
  const t = String(name);
  activeTab.value = t === "video" ? "video" : "article";
  load(activeTab.value);
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

onMounted(() => {
  load(activeTab.value);
});
</script>

<style scoped lang="scss">
.grief-resources-container {
  padding: 20px;

  .content-wrapper {
    margin-top: 20px;
  }

  .resources-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-top: 20px;
  }

  .resource-item {
    background: #fff;
    padding: 20px;
    border-radius: 10px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
    cursor: pointer;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
    display: flex;
    gap: 16px;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
    }

    .resource-cover {
      width: 200px;
      height: 150px;
      flex-shrink: 0;
      border-radius: 10px;
      overflow: hidden;
      position: relative;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .play-icon {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 56px;
        height: 56px;
        background: rgba(0, 0, 0, 0.55);
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

      h3 {
        margin: 0 0 8px;
        font-size: 18px;
        color: #303133;
      }

      p {
        margin: 0 0 12px;
        font-size: 14px;
        color: #606266;
        line-height: 1.6;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .resource-meta {
        display: flex;
        align-items: center;
        gap: 16px;
        font-size: 12px;
        color: #909399;

        .author {
          color: #606266;
        }
      }
    }
  }

  .resource-detail {
    .detail-cover {
      width: 100%;
      max-height: 420px;
      margin-bottom: 16px;
      border-radius: 10px;
      overflow: hidden;

      img {
        width: 100%;
        height: auto;
        object-fit: cover;
      }
    }

    .detail-summary {
      margin-bottom: 12px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 8px;
      color: #303133;
      line-height: 1.7;
    }

    .detail-content {
      margin-bottom: 12px;
      line-height: 1.8;
      color: #303133;
      font-size: 15px;
    }

    .video-player {
      margin-bottom: 12px;
    }

    .detail-meta {
      display: flex;
      gap: 20px;
      padding-top: 14px;
      border-top: 1px solid #ebeef5;
      font-size: 13px;
      color: #909399;
    }
  }
}
</style>



