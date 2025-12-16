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

        <!-- 所选模板预览 -->
        <el-card shadow="never" class="block" style="margin-bottom: 16px;">
          <template #header>
            <div class="block-title">所选模板</div>
          </template>
          <div class="template-meta" v-if="memorialInfo">
            <el-tag type="info">{{ memorialInfo.templateName || '模板' }}</el-tag>
            <el-tag v-if="memorialInfo.templateCategory" style="margin-left: 8px;">
              {{ memorialInfo.templateCategory }}
            </el-tag>
          </div>

          <el-empty v-if="templateImages.length === 0" description="暂无模板图片" :image-size="120" />
          <div v-else class="images">
            <el-image
              v-for="url in templateImages"
              :key="url"
              :src="url"
              :preview-src-list="templateImages"
              fit="cover"
              style="width: 180px; height: 120px; border-radius: 8px"
            />
          </div>
        </el-card>

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
          <div class="share-title">分享（已审核通过且公开）</div>
          <div class="share-actions">
            <el-button type="primary" @click="shareAndDownload">分享并下载</el-button>
            <el-button @click="copyShare">复制链接</el-button>
            <el-button v-if="finalPdf" @click="downloadUrl(finalPdf)">下载终稿PDF</el-button>
          </div>
          <el-input v-model="shareUrl" readonly />
          <div class="tip">点击“分享并下载”会自动下载终稿PDF，你可以把下载的文件转发给别人。</div>
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
import { memorialApi, templateApi, type TemplateVO } from '@/api/memorial';
import { jsPDF } from "jspdf";

const router = useRouter();
const route = useRoute();
const memorialInfo = ref<any>(null);
const feedbackDialogVisible = ref(false);
const uploadingFeedback = ref(false);
const feedbackImageList = ref<UploadFile[]>([]);
const feedbackPdfList = ref<UploadFile[]>([]);
const feedbackMessage = ref("");
const shareUrl = ref("");
const templateInfo = ref<TemplateVO | null>(null);

const draftImages = computed<string[]>(() => memorialInfo.value?.designDraftImages || []);
const draftPdf = computed<string | null>(() => memorialInfo.value?.designDraftPdfUrl || null);
const feedbackImages = computed<string[]>(() => memorialInfo.value?.ownerFeedbackImages || []);
const feedbackPdf = computed<string | null>(() => memorialInfo.value?.ownerFeedbackPdfUrl || null);
const finalPdf = computed<string | null>(() => memorialInfo.value?.designFinalPdfUrl || null);

function parseTemplateImages(t: any): string[] {
  if (Array.isArray(t?.templateImages) && t.templateImages.length > 0) {
    return t.templateImages.filter(Boolean);
  }
  if (t?.styleConfig) {
    try {
      const parsed = JSON.parse(t.styleConfig);
      const imgs = parsed?.templateImages;
      if (Array.isArray(imgs) && imgs.length > 0) return imgs.filter(Boolean);
    } catch {
      // ignore
    }
  }
  if (t?.previewImage) return [t.previewImage];
  return [];
}

const templateImages = computed<string[]>(() => {
  // 优先使用 templateInfo（完整），其次 fallback 到 memorialInfo（若后端未来加入字段）
  return parseTemplateImages(templateInfo.value || memorialInfo.value || {});
});

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

async function shareAndDownload() {
  // “分享”按钮：按需求自动下载文件，用户可分享下载的文件
  if (finalPdf.value) {
    downloadUrl(finalPdf.value);
    return;
  }
  // 没有 PDF 时：用终稿图片自动生成 PDF 下载
  const imgs: string[] = Array.isArray(memorialInfo.value?.designFinalImages) ? memorialInfo.value.designFinalImages : [];
  if (imgs.length > 0) {
    try {
      await generatePdfFromImagesAndDownload(imgs, `${memorialInfo.value?.title || "纪念册"}-终稿.pdf`);
      return;
    } catch (e: any) {
      ElMessage.error(e?.message || "自动生成PDF失败");
      // fallback：打开分享链接
      if (shareUrl.value) openUrl(shareUrl.value);
      return;
    }
  }
  // fallback：打开分享链接
  if (shareUrl.value) {
    openUrl(shareUrl.value);
    return;
  }
  ElMessage.warning("暂无可下载文件");
}

async function fetchImageAsDataUrl(url: string): Promise<{ dataUrl: string; width: number; height: number }> {
  // 通过 fetch 拿 blob 再转 dataUrl，避免跨域导致 canvas/图片解析失败
  const resp = await fetch(url, { mode: "cors" });
  if (!resp.ok) throw new Error(`图片下载失败：${resp.status}`);
  const blob = await resp.blob();
  const dataUrl: string = await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error("读取图片失败"));
    reader.readAsDataURL(blob);
  });

  // 读取图片尺寸
  const img = new Image();
  const size = await new Promise<{ w: number; h: number }>((resolve, reject) => {
    img.onload = () => resolve({ w: img.naturalWidth || img.width, h: img.naturalHeight || img.height });
    img.onerror = () => reject(new Error("解析图片失败"));
    img.src = dataUrl;
  });

  return { dataUrl, width: size.w, height: size.h };
}

async function generatePdfFromImagesAndDownload(imageUrls: string[], filename: string) {
  const doc = new jsPDF({ unit: "pt", format: "a4", compress: true });
  const pageW = doc.internal.pageSize.getWidth();
  const pageH = doc.internal.pageSize.getHeight();

  for (let i = 0; i < imageUrls.length; i++) {
    const { dataUrl, width, height } = await fetchImageAsDataUrl(imageUrls[i]);

    // 按 A4 等比缩放铺满（留少量边距）
    const margin = 24;
    const maxW = pageW - margin * 2;
    const maxH = pageH - margin * 2;
    const ratio = Math.min(maxW / width, maxH / height);
    const drawW = width * ratio;
    const drawH = height * ratio;
    const x = (pageW - drawW) / 2;
    const y = (pageH - drawH) / 2;

    // jsPDF 需要指定图片类型（根据 dataUrl 前缀判断）
    const type = dataUrl.startsWith("data:image/png") ? "PNG" : "JPEG";
    doc.addImage(dataUrl, type as any, x, y, drawW, drawH);
    if (i !== imageUrls.length - 1) doc.addPage();
  }

  doc.save(filename);
}

async function loadMemorial() {
  const id = route.params.id;
  if (!id) return;
  const res = await memorialApi.getMemorialDetail(Number(id));
  memorialInfo.value = res.data;

  // 加载模板详情（公开接口），用于展示模板图片
  try {
    const tid = res.data?.templateId;
    if (tid) {
      const tRes = await templateApi.getTemplateById(Number(tid));
      templateInfo.value = tRes.data || null;
    } else {
      templateInfo.value = null;
    }
  } catch {
    templateInfo.value = null;
  }

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
    .share-actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      margin-bottom: 10px;
    }
    .share-title {
      font-weight: 700;
      margin-bottom: 8px;
    }
  }
}
</style>















