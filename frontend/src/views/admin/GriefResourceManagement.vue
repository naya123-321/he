<template>
  <div class="grief-resource-management-container">
    <el-page-header @back="goBack" content="哀伤资源管理" />

    <div class="content-wrapper">
      <div class="action-bar">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增资源
        </el-button>
        <el-button @click="loadResources">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <el-card shadow="hover" class="filter-card">
        <el-form :inline="true" :model="filterForm">
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 140px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="resetToFirstPageAndLoad">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="hover">
        <el-table :data="resourceList" v-loading="loading" border style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="typeText" label="类型" width="100">
            <template #default="{ row }">
              <el-tag type="info" size="small">{{ row.typeText || row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
          <el-table-column prop="statusText" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.statusText || (row.status === 1 ? "启用" : "禁用") }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openDetail(row.id)">查看</el-button>
              <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="820px" @close="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <!-- 仅保留文章资源：类型固定为“文章”，不再显示下拉 -->
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="formData.summary" type="textarea" :rows="3" maxlength="300" show-word-limit />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="formData.author" maxlength="50" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="formData.content" type="textarea" :rows="8" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="资源详情" width="820px" @close="detail = null">
      <div v-if="detail" class="detail">
        <div class="detail-title">{{ detail.title }}</div>
        <div class="detail-meta">
          <span>类型：{{ detail.typeText || detail.type }}</span>
          <span>状态：{{ detail.statusText || (detail.status === 1 ? "启用" : "禁用") }}</span>
          <span v-if="detail.author">作者：{{ detail.author }}</span>
          <span>创建：{{ detail.createTime }}</span>
        </div>
        <div v-if="detail.summary" class="detail-block">
          <div class="label">摘要</div>
          <div class="value">{{ detail.summary }}</div>
        </div>
        <div v-if="detail.content" class="detail-block">
          <div class="label">内容</div>
          <div class="value" style="white-space: pre-wrap">{{ detail.content }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Refresh } from "@element-plus/icons-vue";
import {
  griefResourceApi,
  type GriefResourceCreateDTO,
  type GriefResourceVO,
} from "@/api/griefResource";

const router = useRouter();

const loading = ref(false);
const submitting = ref(false);
const resourceList = ref<GriefResourceVO[]>([]);

const filterForm = reactive({
  status: undefined as number | undefined,
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

const dialogVisible = ref(false);
const dialogTitle = ref("新增资源");
const editingId = ref<number | null>(null);
const formRef = ref<any>(null);

const formData = reactive<GriefResourceCreateDTO>({
  title: "",
  summary: "",
  type: "article",
  content: "",
  coverImage: "",
  author: "",
  status: 1,
  memberCount: 0,
});

const enabled = computed({
  get: () => (formData.status ?? 1) === 1,
  set: (v: boolean) => {
    formData.status = v ? 1 : 0;
  },
});

const formRules = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
};

function goBack() {
  router.back();
}

async function loadResources() {
  loading.value = true;
  try {
    const res = await griefResourceApi.getGriefResourceList({
      // 仅展示文章资源
      type: "article",
      status: filterForm.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    });
    resourceList.value = (res.data?.records || []).filter((r: any) => r?.type === "article");
    pagination.total = res.data?.total || 0;
  } catch (e: any) {
    resourceList.value = [];
    pagination.total = 0;
    ElMessage.error(e?.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

function resetToFirstPageAndLoad() {
  pagination.current = 1;
  loadResources();
}

function resetFilter() {
  filterForm.status = undefined;
  resetToFirstPageAndLoad();
}

function handleSizeChange(size: number) {
  pagination.pageSize = size;
  pagination.current = 1;
  loadResources();
}

function handlePageChange(page: number) {
  pagination.current = page;
  loadResources();
}

function openCreate() {
  dialogTitle.value = "新增资源";
  editingId.value = null;
  Object.assign(formData, {
    title: "",
    summary: "",
    type: "article",
    content: "",
    coverImage: "",
    author: "",
    status: 1,
    memberCount: 0,
  });
  dialogVisible.value = true;
}

function openEdit(row: GriefResourceVO) {
  dialogTitle.value = "编辑资源";
  editingId.value = row.id;
  Object.assign(formData, {
    title: row.title || "",
    summary: row.summary || "",
    // 固定为文章（若历史数据为 video，这里也统一按文章编辑）
    type: "article",
    content: row.content || "",
    coverImage: row.coverImage || "",
    author: row.author || "",
    status: row.status ?? 1,
    memberCount: 0,
  });
  dialogVisible.value = true;
}

function handleDialogClose() {
  formRef.value?.clearValidate?.();
}

async function submit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (editingId.value) {
      await griefResourceApi.updateGriefResource(editingId.value, formData);
      ElMessage.success("更新成功");
    } else {
      await griefResourceApi.createGriefResource(formData);
      ElMessage.success("创建成功");
    }
    dialogVisible.value = false;
    await loadResources();
  } catch (e: any) {
    ElMessage.error(e?.message || "保存失败");
  } finally {
    submitting.value = false;
  }
}

async function remove(row: GriefResourceVO) {
  try {
    await ElMessageBox.confirm(`确定删除“${row.title}”吗？此操作不可恢复。`, "删除确认", {
      type: "warning",
      confirmButtonText: "确定删除",
      cancelButtonText: "取消",
    });
    await griefResourceApi.deleteGriefResource(row.id);
    ElMessage.success("删除成功");
    await loadResources();
  } catch {
    // ignore
  }
}

const detailVisible = ref(false);
const detail = ref<GriefResourceVO | null>(null);

async function openDetail(id: number) {
  try {
    const res = await griefResourceApi.getGriefResourceDetail(id);
    detail.value = res.data;
    detailVisible.value = true;
  } catch (e: any) {
    ElMessage.error(e?.message || "获取详情失败");
  }
}

loadResources();
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

.grief-resource-management-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 20px;
  min-height: calc(100vh - 100px);
  background: $bg-light;

  .content-wrapper {
    margin-top: 20px;
  }

  .action-bar {
    margin-bottom: 20px;
    display: flex;
    gap: 12px;
    padding: 20px;
    background: $bg-white;
    border-radius: 12px;
    border: 1px solid $border-color-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .el-button {
      border-radius: 8px;
      font-weight: 500;
    }
  }

  .filter-card {
    margin-bottom: 20px;
    border-radius: 12px;
    border: 1px solid $border-color-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    overflow: hidden;

    :deep(.el-card__body) {
      padding: 20px;
    }

    .el-input,
    .el-select {
      border-radius: 8px;
    }

    .el-button {
      border-radius: 8px;
      font-weight: 500;
    }
  }

  :deep(.el-card) {
    border-radius: 12px;
    border: 1px solid $border-color-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    overflow: hidden;
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

  .pager {
    margin-top: 20px;
    padding: 20px;
    background: $bg-white;
    border-radius: 12px;
    border: 1px solid $border-color-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    display: flex;
    justify-content: center;

    :deep(.el-pagination) {
      .el-pager li.is-active {
        background: $primary-color;
        color: #fff;
        border-radius: 4px;
      }
    }
  }

  .detail {
    .detail-title {
      font-size: 20px;
      font-weight: 700;
      color: $text-primary;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 2px solid $border-color-light;
    }

    .detail-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      color: $text-placeholder;
      font-size: 13px;
      margin-bottom: 20px;
      padding: 12px;
      background: $bg-light;
      border-radius: 8px;
    }

    .detail-cover {
      width: 100%;
      max-height: 420px;
      border-radius: 12px;
      overflow: hidden;
      margin-bottom: 16px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

      img {
        width: 100%;
        height: auto;
        object-fit: cover;
      }
    }

    .detail-block {
      margin-bottom: 16px;

      .label {
        font-size: 14px;
        color: $text-secondary;
        margin-bottom: 8px;
        font-weight: 600;
      }

      .value {
        padding: 16px;
        background: linear-gradient(135deg, #e0f7fa, #b2ebf2);
        border-left: 4px solid $primary-color;
        border-radius: 8px;
        color: $text-primary;
        line-height: 1.8;
        word-break: break-word;
      }
    }
  }

  .upload-row {
    display: flex;
    gap: 10px;
    align-items: center;

    :deep(.el-upload) {
      flex-shrink: 0;
    }

    :deep(.el-input) {
      flex: 1;
    }
  }

  .form-tip {
    margin-top: 6px;
    color: $text-placeholder;
    font-size: 12px;
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
    .el-textarea,
    .el-select {
      border-radius: 8px;
    }
  }
}
</style>



