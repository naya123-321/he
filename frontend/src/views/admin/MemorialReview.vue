<template>
  <div class="memorial-review-container">
    <el-page-header @back="goBack" content="纪念册审核" />
    
    <div class="content-wrapper">
      <el-tabs v-model="activeTab" @tab-change="load">
        <el-tab-pane label="待审核" name="pending">
          <el-table v-loading="loading" :data="pendingMemorials" style="width: 100%" border>
            <el-table-column prop="title" label="纪念册标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="petName" label="宠物名称" width="140" />
            <el-table-column prop="username" label="创建人" width="140" />
            <el-table-column prop="designProviderName" label="服务人员" width="140" />
            <el-table-column prop="designStatusText" label="协作状态" width="160" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openDetail(row.id)">查看终稿</el-button>
                <el-button type="primary" size="small" @click="openReview(row, true)">通过</el-button>
                <el-button type="danger" size="small" @click="openReview(row, false)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="已审核" name="reviewed">
          <el-table v-loading="loading" :data="reviewedMemorials" style="width: 100%" border>
            <el-table-column prop="title" label="纪念册标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="petName" label="宠物名称" width="140" />
            <el-table-column prop="username" label="创建人" width="140" />
            <el-table-column prop="designStatusText" label="结果" width="140" />
            <el-table-column prop="adminReviewTime" label="审核时间" width="180" />
            <el-table-column label="分享" width="220">
              <template #default="{ row }">
                <el-tag v-if="row.designStatus === 60 && row.isPublic && row.shareToken" type="success">可分享</el-tag>
                <el-tag v-else type="info">不可分享</el-tag>
                <el-button
                  v-if="row.designStatus === 60 && row.isPublic && row.shareToken"
                  link
                  type="primary"
                  @click="copyShare(row)"
                >
                  复制链接
                </el-button>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openDetail(row.id)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 终稿预览 -->
    <el-dialog v-model="detailVisible" title="终稿预览" width="920px">
      <el-empty v-if="!detail" description="暂无数据" />
      <template v-else>
        <div class="detail-header">
          <div class="d-title">{{ detail.title }}</div>
          <div class="d-sub">{{ detail.subtitle || "" }}</div>
          <div class="d-meta">
            <el-tag type="info">{{ detail.designStatusText }}</el-tag>
            <el-tag :type="detail.status === 2 ? 'success' : 'warning'">{{ detail.statusText }}</el-tag>
          </div>
        </div>
        <el-divider />
        <el-empty v-if="finalImages.length === 0 && !finalPdf" description="暂无终稿内容" />
        <div v-if="finalImages.length > 0" class="images">
          <el-image
            v-for="url in finalImages"
            :key="url"
            :src="url"
            :preview-src-list="finalImages"
            fit="cover"
            style="width: 220px; height: 150px; border-radius: 8px"
          />
        </div>
        <div v-if="finalPdf" class="pdf">
          <el-button type="primary" link @click="openUrl(finalPdf)">在线查看PDF</el-button>
          <el-button link @click="downloadUrl(finalPdf)">下载PDF</el-button>
        </div>
        <div v-if="detail.designMessage" class="note">
          <div class="note-title">备注</div>
          <div class="note-body">{{ detail.designMessage }}</div>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="reviewVisible" :title="reviewApproved ? '审核通过' : '审核拒绝'" width="620px">
      <el-form label-width="110px">
        <el-form-item label="审核意见">
          <el-input v-model="reviewComment" type="textarea" :rows="4" placeholder="填写审核意见（选填）" />
        </el-form-item>
        <el-form-item v-if="reviewApproved" label="是否公开">
          <el-switch v-model="reviewIsPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElPageHeader } from 'element-plus';
import { memorialApi, type MemorialVO } from '@/api/memorial';

const router = useRouter();
const activeTab = ref('pending');
const pendingMemorials = ref<MemorialVO[]>([]);
const reviewedMemorials = ref<MemorialVO[]>([]);
const loading = ref(false);

const detailVisible = ref(false);
const detail = ref<MemorialVO | null>(null);
const finalImages = computed<string[]>(() => detail.value?.designFinalImages || []);
const finalPdf = computed<string | null>(() => detail.value?.designFinalPdfUrl || null);

const reviewVisible = ref(false);
const reviewSubmitting = ref(false);
const reviewApproved = ref(true);
const reviewTargetId = ref<number | null>(null);
const reviewComment = ref('');
const reviewIsPublic = ref(true);

// 返回上一页
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

async function load() {
  loading.value = true;
  try {
    if (activeTab.value === 'pending') {
      const res = await memorialApi.getMemorialList({ designStatus: 50, pageNum: 1, pageSize: 100 });
      pendingMemorials.value = res.data?.records || [];
    } else {
      // 已审核：60/61
      const resApproved = await memorialApi.getMemorialList({ designStatus: 60, pageNum: 1, pageSize: 100 });
      const resRejected = await memorialApi.getMemorialList({ designStatus: 61, pageNum: 1, pageSize: 100 });
      reviewedMemorials.value = [...(resApproved.data?.records || []), ...(resRejected.data?.records || [])];
    }
  } catch (e: any) {
    ElMessage.error(e?.message || "加载失败");
    pendingMemorials.value = [];
    reviewedMemorials.value = [];
  } finally {
    loading.value = false;
  }
}

async function openDetail(id: number) {
  try {
    const res = await memorialApi.getMemorialDetail(id);
    detail.value = res.data;
    detailVisible.value = true;
  } catch (e: any) {
    ElMessage.error(e?.message || "加载详情失败");
  }
}

function openReview(row: MemorialVO, approved: boolean) {
  reviewApproved.value = approved;
  reviewTargetId.value = row.id;
  reviewComment.value = '';
  reviewIsPublic.value = true;
  reviewVisible.value = true;
}

async function submitReview() {
  if (!reviewTargetId.value) return;
  reviewSubmitting.value = true;
  try {
    await memorialApi.adminReview(reviewTargetId.value, reviewApproved.value, reviewComment.value || undefined, reviewApproved.value ? reviewIsPublic.value : undefined);
    ElMessage.success("操作成功");
    reviewVisible.value = false;
    await load();
  } catch (e: any) {
    ElMessage.error(e?.message || "操作失败");
  } finally {
    reviewSubmitting.value = false;
  }
}

async function copyShare(row: MemorialVO) {
  if (!row.shareToken) return;
  const url = `${window.location.origin}/memorial/share/${row.shareToken}`;
  try {
    await navigator.clipboard.writeText(url);
    ElMessage.success("已复制");
  } catch {
    ElMessage.warning("复制失败，请手动复制");
  }
}

onMounted(() => {
  load();
});
</script>

<style scoped lang="scss">
// 统一配色变量
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$info-color: #909399;
$bg-light: #f5f7fa;
$bg-white: #ffffff;
$text-primary: #303133;
$text-secondary: #606266;
$text-placeholder: #909399;
$border-color: #dcdfe6;
$border-color-light: #ebeef5;

.memorial-review-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 20px;
  min-height: calc(100vh - 100px);
  background: $bg-light;
  
  .content-wrapper {
    margin-top: 20px;
    background: $bg-white;
    border-radius: 12px;
    border: 1px solid $border-color-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    padding: 20px;
    overflow: hidden;
  }

  :deep(.el-tabs) {
    .el-tabs__header {
      margin-bottom: 20px;
    }

    .el-tabs__item {
      font-size: 16px;
      font-weight: 500;
      padding: 0 24px;
      height: 48px;
      line-height: 48px;

      &.is-active {
        color: $primary-color;
        font-weight: 600;
      }
    }

    .el-tabs__active-bar {
      height: 3px;
      background: $primary-color;
    }
  }

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;

    .el-table__header {
      th {
        background-color: $bg-light;
        font-weight: 600;
        color: $text-primary;
      }
    }

    .el-table__body {
      tr {
        &:hover {
          background-color: rgba(64, 158, 255, 0.05);
        }
      }
    }
  }

  :deep(.el-button) {
    border-radius: 8px;
    font-weight: 500;
  }

  :deep(.el-tag) {
    border-radius: 20px;
    padding: 0 12px;
    font-weight: 500;
  }

  .detail-header {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding-bottom: 16px;
    border-bottom: 1px solid $border-color-light;
    margin-bottom: 20px;
  }

  .d-title {
    font-weight: 700;
    font-size: 20px;
    color: $text-primary;
  }

  .d-sub {
    color: $text-secondary;
    font-size: 14px;
  }

  .d-meta {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .images {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 16px;

    .el-image {
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      transition: transform 0.3s;

      &:hover {
        transform: scale(1.02);
      }
    }
  }

  .pdf {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    padding: 16px;
    background: $bg-light;
    border-radius: 8px;
    border-left: 4px solid $primary-color;
  }

  .note {
    margin-top: 16px;
    padding: 16px;
    border-radius: 8px;
    background: linear-gradient(135deg, #e0f7fa, #b2ebf2);
    border-left: 4px solid $primary-color;

    .note-title {
      font-weight: 700;
      margin-bottom: 8px;
      color: $text-primary;
    }

    .note-body {
      color: $text-secondary;
      white-space: pre-wrap;
      line-height: 1.6;
    }
  }

  :deep(.el-dialog) {
    border-radius: 12px;

    .el-dialog__header {
      background: linear-gradient(135deg, #409eff, #66b1ff);
      color: #fff;
      padding: 20px 24px;
      border-radius: 12px 12px 0 0;

      .el-dialog__title {
        color: #fff;
        font-weight: 700;
        font-size: 18px;
      }

      .el-dialog__headerbtn {
        .el-dialog__close {
          color: #fff;
          font-size: 20px;

          &:hover {
            color: rgba(255, 255, 255, 0.8);
          }
        }
      }
    }

    .el-dialog__body {
      padding: 24px;
    }

    .el-form-item__label {
      color: $text-primary;
      font-weight: 500;
    }

    .el-input,
    .el-textarea {
      border-radius: 8px;
    }
  }
}
</style>


