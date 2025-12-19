<template>
  <div class="memorial-design-container">
    <el-page-header @back="goBack" content="纪念册设计" />
    
    <div class="content-wrapper">
      <el-empty v-if="!loading && !memorialInfo" description="未找到纪念册信息" />
      
      <div v-else-if="memorialInfo">
        <div class="memorial-header">
          <h2>{{ memorialInfo.title }}</h2>
          <p>{{ memorialInfo.subtitle }}</p>
        </div>
        
        <el-card shadow="never" class="section">
          <template #header>
            <div class="section-title">
              <span>协作状态</span>
              <el-tag type="info">{{ memorialInfo.designStatusText }}</el-tag>
            </div>
          </template>
          <div class="meta-row">
            <div>宠主：{{ memorialInfo.username || "-" }}</div>
            <div>宠物：{{ memorialInfo.petName }} {{ memorialInfo.petType ? `(${getPetTypeLabel(memorialInfo.petType) || memorialInfo.petType})` : "" }}</div>
          </div>
          <div class="meta-row">
            <div>订单ID：{{ memorialInfo.orderId || "-" }}</div>
            <div>指派服务人员：{{ memorialInfo.designProviderName || memorialInfo.designProviderId || "-" }}</div>
          </div>
        </el-card>

        <el-card shadow="never" class="section">
          <template #header>
            <div class="section-title">
              <span>宠主原始照片（可下载）</span>
              <el-button size="small" @click="downloadAllPetPhotos" :disabled="petPhotos.length === 0">下载全部</el-button>
            </div>
          </template>
          <el-empty v-if="petPhotos.length === 0" description="暂无宠主照片" :image-size="120" />
          <div v-else class="images">
            <div v-for="url in petPhotos" :key="url" class="img-item">
              <el-image :src="url" :preview-src-list="petPhotos" fit="cover" style="width: 220px; height: 150px; border-radius: 8px" />
              <div class="img-actions">
                <el-button link type="primary" @click="downloadUrl(url)">下载</el-button>
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="section">
          <template #header>
            <div class="section-title">
              <span>上传设计稿（多图 / PDF）</span>
            </div>
          </template>
          <el-form label-width="110px">
            <el-form-item label="设计稿图片">
              <el-upload v-model:file-list="draftImageList" :auto-upload="false" list-type="picture-card" accept="image/*" multiple>
                <el-icon><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="设计稿PDF">
              <el-upload v-model:file-list="draftPdfList" :auto-upload="false" accept="application/pdf" :limit="1">
                <el-button>选择PDF</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item label="说明">
              <el-input v-model="draftMessage" type="textarea" :rows="3" placeholder="给宠主的说明（选填）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="uploadingDraft" @click="uploadDraft">上传设计稿</el-button>
              <el-button @click="refresh">刷新</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="section">
          <template #header>
            <div class="section-title">
              <span>宠主回传修改稿</span>
            </div>
          </template>
          <el-empty v-if="feedbackImages.length === 0 && !feedbackPdf" description="暂无回传稿" :image-size="120" />
          <div v-if="feedbackImages.length > 0" class="images">
            <el-image
              v-for="url in feedbackImages"
              :key="url"
              :src="url"
              :preview-src-list="feedbackImages"
              fit="cover"
              style="width: 220px; height: 150px; border-radius: 8px"
            />
          </div>
          <div v-if="feedbackPdf" class="pdf">
            <el-button type="primary" link @click="openUrl(feedbackPdf)">在线查看PDF</el-button>
            <el-button link @click="downloadUrl(feedbackPdf)">下载PDF</el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="section">
          <template #header>
            <div class="section-title">
              <span>上传最终版并提交管理员审核</span>
              <el-tag type="warning">需要宠主先确认最终版</el-tag>
            </div>
          </template>
          <el-form label-width="110px">
            <el-form-item label="最终版图片">
              <el-upload v-model:file-list="finalImageList" :auto-upload="false" list-type="picture-card" accept="image/*" multiple>
                <el-icon><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="最终版PDF">
              <el-upload v-model:file-list="finalPdfList" :auto-upload="false" accept="application/pdf" :limit="1">
                <el-button>选择PDF</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item label="说明">
              <el-input v-model="finalMessage" type="textarea" :rows="3" placeholder="给管理员/宠主的说明（选填）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="uploadingFinal" @click="uploadFinal">上传并提交审核</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <div class="design-tools">
          <el-button @click="previewMemorial">预览纪念册</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElPageHeader, ElEmpty, ElButton } from 'element-plus';
import type { UploadFile } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { memorialApi, type MemorialVO } from '@/api/memorial';
import { getPetTypeLabel } from '@/constants/petTypes';

const router = useRouter();
const route = useRoute();
const memorialInfo = ref<MemorialVO | null>(null);
const loading = ref(false);

const uploadingDraft = ref(false);
const uploadingFinal = ref(false);
const draftImageList = ref<UploadFile[]>([]);
const draftPdfList = ref<UploadFile[]>([]);
const draftMessage = ref('');

const finalImageList = ref<UploadFile[]>([]);
const finalPdfList = ref<UploadFile[]>([]);
const finalMessage = ref('');

const petPhotos = computed<string[]>(() => memorialInfo.value?.petPhotoUrls || []);
const feedbackImages = computed<string[]>(() => memorialInfo.value?.ownerFeedbackImages || []);
const feedbackPdf = computed<string | null>(() => memorialInfo.value?.ownerFeedbackPdfUrl || null);

const goBack = () => {
  router.back();
};

const previewMemorial = () => {
  if (memorialInfo.value?.id) {
    router.push(`/memorial/preview/${memorialInfo.value.id}`);
  }
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

async function refresh() {
  const id = Number(route.params.id);
  if (!id) return;
  loading.value = true;
  try {
    const res = await memorialApi.getMemorialDetail(id);
    memorialInfo.value = res.data;
  } catch (e: any) {
    memorialInfo.value = null;
    ElMessage.error(e?.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

async function uploadDraft() {
  if (!memorialInfo.value?.id) return;
  const images = draftImageList.value.map((f: any) => f.raw as File).filter(Boolean);
  const pdf = draftPdfList.value?.[0] ? ((draftPdfList.value[0] as any).raw as File) : undefined;
  if (images.length === 0 && !pdf) {
    ElMessage.warning("请至少选择图片或PDF");
    return;
  }
  uploadingDraft.value = true;
  try {
    await memorialApi.providerUploadDraft(memorialInfo.value.id, {
      images,
      pdf,
      message: draftMessage.value || undefined,
    });
    ElMessage.success("已上传设计稿");
    draftImageList.value = [];
    draftPdfList.value = [];
    draftMessage.value = "";
    await refresh();
  } catch (e: any) {
    ElMessage.error(e?.message || "上传失败");
  } finally {
    uploadingDraft.value = false;
  }
}

async function uploadFinal() {
  if (!memorialInfo.value?.id) return;
  const images = finalImageList.value.map((f: any) => f.raw as File).filter(Boolean);
  const pdf = finalPdfList.value?.[0] ? ((finalPdfList.value[0] as any).raw as File) : undefined;
  if (images.length === 0 && !pdf) {
    ElMessage.warning("请至少选择图片或PDF");
    return;
  }
  uploadingFinal.value = true;
  try {
    await memorialApi.providerUploadFinal(memorialInfo.value.id, {
      images,
      pdf,
      message: finalMessage.value || undefined,
    });
    ElMessage.success("已上传最终版并提交审核");
    finalImageList.value = [];
    finalPdfList.value = [];
    finalMessage.value = "";
    await refresh();
  } catch (e: any) {
    ElMessage.error(e?.message || "上传失败");
  } finally {
    uploadingFinal.value = false;
  }
}

async function downloadAllPetPhotos() {
  if (petPhotos.value.length === 0) return;
  for (const url of petPhotos.value) {
    downloadUrl(url);
    // 小延迟，避免浏览器拦截过多下载
    await new Promise((r) => setTimeout(r, 150));
  }
}

onMounted(() => {
  refresh();
});
</script>

<style scoped lang="scss">
// 统一配色方案（与其他页面保持一致）
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$text-primary: #303133;
$text-secondary: #606266;
$text-light: #909399;
$border-color: #ebeef5;
$bg-light: #f5f7fa;
$bg-white: #ffffff;

.memorial-design-container {
  padding: 30px 20px;
  max-width: 1400px;
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
    margin-top: 24px;
  }
  
  .memorial-header {
    margin-bottom: 30px;
    padding-bottom: 20px;
    border-bottom: 2px solid $border-color;
    
    h2 {
      margin: 0 0 10px;
      font-size: 28px;
      color: $text-primary;
      font-weight: 700;
    }
    
    p {
      margin: 0;
      font-size: 16px;
      color: $text-secondary;
      line-height: 1.6;
    }
  }
  
  .design-tools {
    display: flex;
    gap: 12px;
    margin-top: 24px;
    padding-top: 20px;
    border-top: 2px solid $border-color;

    :deep(.el-button) {
      border-radius: 8px;
      font-weight: 500;
      padding: 12px 24px;
    }
  }

  .section {
    margin-bottom: 24px;
    border-radius: 12px;
    border: 1px solid $border-color;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }

    :deep(.el-card__header) {
      background: $bg-light;
      border-bottom: 1px solid $border-color;
      padding: 16px 20px;
    }

    :deep(.el-card__body) {
      padding: 24px;
    }
  }

  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    font-weight: 600;
    color: $text-primary;
    font-size: 16px;

    :deep(.el-tag) {
      border-radius: 999px;
      font-weight: 500;
    }

    :deep(.el-button) {
      border-radius: 8px;
      font-weight: 500;
    }
  }

  .meta-row {
    display: flex;
    gap: 24px;
    flex-wrap: wrap;
    color: $text-secondary;
    margin-bottom: 12px;
    padding: 12px;
    background: $bg-light;
    border-radius: 8px;
    font-size: 14px;
    line-height: 1.8;

    div {
      flex: 1;
      min-width: 200px;
    }
  }

  .images {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
  }

  .img-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid $border-color;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
      transform: translateY(-2px);
      border-color: $primary-color;
    }

    :deep(.el-image) {
      border-radius: 10px 10px 0 0;
    }
  }

  .img-actions {
    display: flex;
    justify-content: center;
    padding: 8px;
    background: $bg-light;

    :deep(.el-button) {
      border-radius: 6px;
      font-weight: 500;
    }
  }

  .pdf {
    margin-top: 16px;
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    padding: 16px;
    background: $bg-light;
    border-radius: 10px;
    border-left: 3px solid $primary-color;

    :deep(.el-button) {
      border-radius: 8px;
      font-weight: 500;
    }
  }

  :deep(.el-form) {
    .el-form-item__label {
      color: $text-secondary;
      font-weight: 500;
    }

    .el-input__wrapper,
    .el-textarea__inner,
    .el-upload {
      border-radius: 8px;
    }

    .el-button {
      border-radius: 8px;
      font-weight: 500;

      &.el-button--primary {
        background-color: $primary-color;
        border-color: $primary-color;
      }
    }
  }

  :deep(.el-upload-list--picture-card) {
    .el-upload-list__item {
      border-radius: 10px;
      border: 1px solid $border-color;
    }
  }
}
</style>















