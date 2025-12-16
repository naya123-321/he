<template>
  <div class="memorial-preview-container">
    <el-page-header @back="goBack" content="纪念册预览" />
    
    <div class="content-wrapper">
      <el-card v-if="memorialInfo" shadow="never" class="collab-card">
        <div class="collab-header">
          <div class="left">
            <div class="title">{{ memorialInfo.title }}</div>
            <div class="sub">{{ memorialInfo.subtitle || "" }}</div>
            <div class="meta">
              <el-tag type="info">设计协作：{{ memorialInfo.designStatusText || "-" }}</el-tag>
              <el-tag :type="memorialInfo.status === 2 ? 'success' : memorialInfo.status === 1 ? 'warning' : 'info'">
                {{ memorialInfo.statusText || `状态${memorialInfo.status}` }}
              </el-tag>
            </div>
          </div>
          <div class="right">
            <el-button
              v-if="canAnnotate"
              type="primary"
              @click="goAnnotate"
            >
              在线标注编辑
            </el-button>
            <el-button
              v-if="canUploadFeedback"
              @click="feedbackDialogVisible = true"
            >
              上传修改稿
            </el-button>
            <el-button
              v-if="canConfirmFinal"
              type="success"
              @click="confirmFinal"
            >
              确认最终版
            </el-button>
          </div>
        </div>

        <el-divider />

        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="block">
              <template #header>
                <div class="block-title">服务端设计稿</div>
              </template>

              <el-empty
                v-if="draftImages.length === 0 && !draftPdf"
                description="暂无设计稿（等待服务端上传）"
                :image-size="120"
              />

              <div v-if="draftImages.length > 0" class="images">
                <el-image
                  v-for="url in draftImages"
                  :key="url"
                  :src="url"
                  :preview-src-list="draftImages"
                  fit="cover"
                  style="width: 180px; height: 120px; border-radius: 8px"
                />
              </div>
              <div v-if="draftPdf" class="pdf">
                <el-button type="primary" link @click="openUrl(draftPdf)">在线查看PDF</el-button>
                <el-button link @click="downloadUrl(draftPdf)">下载PDF</el-button>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="block">
              <template #header>
                <div class="block-title">我的回传修改稿</div>
              </template>

              <el-empty
                v-if="feedbackImages.length === 0 && !feedbackPdf"
                description="暂无回传稿（可上传修改稿或使用在线标注）"
                :image-size="120"
              />

              <div v-if="feedbackImages.length > 0" class="images">
                <el-image
                  v-for="url in feedbackImages"
                  :key="url"
                  :src="url"
                  :preview-src-list="feedbackImages"
                  fit="cover"
                  style="width: 180px; height: 120px; border-radius: 8px"
                />
              </div>
              <div v-if="feedbackPdf" class="pdf">
                <el-button type="primary" link @click="openUrl(feedbackPdf)">在线查看PDF</el-button>
                <el-button link @click="downloadUrl(feedbackPdf)">下载PDF</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-divider v-if="shareUrl" />
        <div v-if="shareUrl" class="share">
          <div class="share-title">分享链接（已审核通过且公开）</div>
          <el-input v-model="shareUrl" readonly>
            <template #append>
              <el-button @click="copyShare">复制</el-button>
            </template>
          </el-input>
        </div>
      </el-card>

      <!-- 宠主回传修改稿 -->
      <el-dialog v-model="feedbackDialogVisible" title="上传修改稿（多图 / PDF）" width="760px" @close="resetFeedbackForm">
        <el-form label-width="110px">
          <el-form-item label="修改稿图片">
            <el-upload
              v-model:file-list="feedbackImageList"
              :auto-upload="false"
              list-type="picture-card"
              accept="image/*"
              multiple
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>

          <el-form-item label="修改稿PDF">
            <el-upload
              v-model:file-list="feedbackPdfList"
              :auto-upload="false"
              accept="application/pdf"
              :limit="1"
            >
              <el-button>选择PDF</el-button>
              <template #tip>
                <div class="tip">可选：只上传 PDF 或只上传图片，也可同时上传</div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item label="说明">
            <el-input v-model="feedbackMessage" type="textarea" :rows="3" placeholder="补充说明（选填）" />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="feedbackDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="uploadingFeedback" @click="submitFeedback">
            上传
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox, ElPageHeader } from 'element-plus';
import type { UploadFile } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { memorialApi } from '@/api/memorial';

const router = useRouter();
const route = useRoute();
const memorialInfo = ref<any>(null);
const feedbackDialogVisible = ref(false);
const uploadingFeedback = ref(false);
const feedbackImageList = ref<UploadFile[]>([]);
const feedbackPdfList = ref<UploadFile[]>([]);
const feedbackMessage = ref("");
const shareUrl = ref("");

const draftImages = computed<string[]>(() => memorialInfo.value?.designDraftImages || []);
const draftPdf = computed<string | null>(() => memorialInfo.value?.designDraftPdfUrl || null);
const feedbackImages = computed<string[]>(() => memorialInfo.value?.ownerFeedbackImages || []);
const feedbackPdf = computed<string | null>(() => memorialInfo.value?.ownerFeedbackPdfUrl || null);

const canUploadFeedback = computed(() => {
  const s = memorialInfo.value?.designStatus;
  return s === 20 || s === 30; // 服务端已回传 or 我已回传过
});
const canAnnotate = computed(() => canUploadFeedback.value && draftImages.value.length > 0);
const canConfirmFinal = computed(() => {
  const s = memorialInfo.value?.designStatus;
  return s === 20 || s === 30; // 收到设计稿后可以确认最终版（也允许先回传再确认）
});

const goBack = () => {
  router.back();
};

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

function goAnnotate() {
  const id = memorialInfo.value?.id;
  if (!id) return;
  router.push(`/memorial/annotate/${id}`);
}

function resetFeedbackForm() {
  feedbackImageList.value = [];
  feedbackPdfList.value = [];
  feedbackMessage.value = "";
}

async function submitFeedback() {
  if (!memorialInfo.value?.id) return;
  const id = memorialInfo.value.id as number;

  const images = feedbackImageList.value
    .map((f: any) => f.raw as File)
    .filter(Boolean);
  const pdf = feedbackPdfList.value?.[0] ? ((feedbackPdfList.value[0] as any).raw as File) : undefined;

  if (images.length === 0 && !pdf) {
    ElMessage.warning("请至少选择图片或PDF");
    return;
  }

  uploadingFeedback.value = true;
  try {
    await memorialApi.ownerUploadFeedback(id, { images, pdf, message: feedbackMessage.value || undefined });
    ElMessage.success("已上传修改稿");
    feedbackDialogVisible.value = false;
    await loadMemorial();
  } catch (e: any) {
    ElMessage.error(e?.message || "上传失败");
  } finally {
    uploadingFeedback.value = false;
  }
}

async function confirmFinal() {
  if (!memorialInfo.value?.id) return;
  const id = memorialInfo.value.id as number;
  try {
    await ElMessageBox.confirm(
      "确认最终版后，服务端将上传终稿并提交管理端审核。确定继续吗？",
      "确认最终版",
      { type: "warning", confirmButtonText: "确定确认", cancelButtonText: "取消" }
    );
    await memorialApi.ownerConfirmFinal(id);
    ElMessage.success("已确认最终版");
    await loadMemorial();
  } catch {
    // ignore cancel
  }
}

async function copyShare() {
  try {
    await navigator.clipboard.writeText(shareUrl.value);
    ElMessage.success("已复制");
  } catch {
    ElMessage.warning("复制失败，请手动复制");
  }
}

async function loadMemorial() {
  const id = route.params.id;
  if (!id) return;
  const res = await memorialApi.getMemorialDetail(Number(id));
  memorialInfo.value = res.data;

  // 分享链接：管理员已通过且公开
  if (res.data?.shareToken && res.data?.isPublic && res.data?.designStatus === 60) {
    shareUrl.value = `${window.location.origin}/memorial/share/${res.data.shareToken}`;
  } else {
    shareUrl.value = "";
  }
}

onMounted(() => {
  loadMemorial().catch((e: any) => ElMessage.error(e?.message || "加载纪念册失败"));
});
</script>

<style scoped lang="scss">
.memorial-preview-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }
}

.collab-card {
  margin-bottom: 16px;

  .collab-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    flex-wrap: wrap;

    .title {
      font-size: 18px;
      font-weight: 800;
      color: #303133;
    }

    .sub {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }

    .meta {
      margin-top: 8px;
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .right {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }
  }

  .block-title {
    font-weight: 700;
  }

  .images {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 10px;
  }

  .pdf {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }

  .tip {
    color: #909399;
    font-size: 12px;
  }

  .share {
    .share-title {
      font-weight: 700;
      margin-bottom: 8px;
    }
  }
}
</style>















