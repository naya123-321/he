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
              <el-button link @click="downloadFinalPdf">下载终稿PDF</el-button>
            </div>
            <div v-else class="pdf">
              <el-button type="primary" @click="downloadFinalPdf">生成并下载终稿PDF</el-button>
              <div class="tip">未提供终稿PDF时，将自动由终稿图片生成PDF下载。</div>
            </div>
          </div>
          <el-empty v-else description="暂无最终版内容" />
        </template>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { memorialApi, type MemorialVO } from "@/api/memorial";
import { jsPDF } from "jspdf";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const memorial = ref<MemorialVO | null>(null);

const finalPdf = computed(() => memorial.value?.designFinalPdfUrl || null);
const finalImages = computed<string[]>(() => {
  const imgs: any = (memorial.value as any)?.designFinalImages;
  if (Array.isArray(imgs)) return imgs.filter(Boolean);
  // 兼容后端若返回 JSON 字符串
  if (typeof imgs === "string") {
    try {
      const parsed = JSON.parse(imgs);
      if (Array.isArray(parsed)) return parsed.filter(Boolean);
    } catch {
      // ignore
    }
  }
  return [];
});

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

function safeFilename(name: string) {
  return name.replace(/[\\/:*?"<>|]+/g, "_");
}

async function fetchImageAsDataUrl(url: string): Promise<{ dataUrl: string; width: number; height: number }> {
  const resp = await fetch(url, { mode: "cors" });
  if (!resp.ok) throw new Error(`图片下载失败：${resp.status}`);
  const blob = await resp.blob();
  const dataUrl: string = await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error("读取图片失败"));
    reader.readAsDataURL(blob);
  });

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
    const margin = 24;
    const maxW = pageW - margin * 2;
    const maxH = pageH - margin * 2;
    const ratio = Math.min(maxW / width, maxH / height);
    const drawW = width * ratio;
    const drawH = height * ratio;
    const x = (pageW - drawW) / 2;
    const y = (pageH - drawH) / 2;

    const type = dataUrl.startsWith("data:image/png") ? "PNG" : "JPEG";
    doc.addImage(dataUrl, type as any, x, y, drawW, drawH);
    if (i !== imageUrls.length - 1) doc.addPage();
  }

  doc.save(filename);
}

async function downloadFinalPdf() {
  if (finalPdf.value) {
    downloadUrl(finalPdf.value);
    return;
  }
  if (finalImages.value.length === 0) {
    ElMessage.warning("暂无终稿图片，无法生成PDF");
    return;
  }
  try {
    const title = safeFilename(memorial.value?.title || "纪念册");
    await generatePdfFromImagesAndDownload(finalImages.value, `${title}-终稿.pdf`);
  } catch (e: any) {
    ElMessage.error(e?.message || "自动生成PDF失败");
  }
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

  .pdf {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
  }

  .tip {
    color: #909399;
    font-size: 12px;
  }
}
</style>


