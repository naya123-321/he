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
.memorial-review-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }

  .detail-header {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .d-title {
    font-weight: 800;
    font-size: 18px;
    color: #303133;
  }
  .d-sub {
    color: #909399;
    font-size: 13px;
  }
  .d-meta {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
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
  .note {
    margin-top: 12px;
    padding: 10px;
    border-radius: 10px;
    background: #f5f7fa;
    .note-title {
      font-weight: 700;
      margin-bottom: 6px;
    }
    .note-body {
      color: #606266;
      white-space: pre-wrap;
    }
  }
}
</style>


