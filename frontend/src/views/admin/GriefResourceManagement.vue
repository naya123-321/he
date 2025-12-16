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
          <el-form-item label="类型">
            <el-select v-model="filterForm.type" placeholder="全部" clearable style="width: 140px">
              <el-option label="文章" value="article" />
              <el-option label="视频" value="video" />
            </el-select>
          </el-form-item>
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
        <el-form-item label="类型" prop="type">
          <el-select v-model="formData.type" style="width: 240px">
            <el-option label="文章" value="article" />
            <el-option label="视频" value="video" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="formData.summary" type="textarea" :rows="3" maxlength="300" show-word-limit />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="formData.author" maxlength="50" />
        </el-form-item>
        <el-form-item label="封面" prop="coverImage">
          <div class="upload-row">
            <el-upload
              :action="uploadAction"
              :headers="uploadHeaders"
              :show-file-list="false"
              accept="image/*"
              :before-upload="beforeUploadCover"
              :on-success="handleCoverUploadSuccess"
            >
              <el-button>上传封面</el-button>
            </el-upload>
            <el-input v-model="formData.coverImage" placeholder="可填写图片 URL" />
          </div>
        </el-form-item>
        <el-form-item v-if="formData.type === 'video'" label="视频文件" prop="fileUrl">
          <div class="upload-row">
            <el-upload
              :action="uploadAction"
              :headers="uploadHeaders"
              :show-file-list="false"
              accept="video/*"
              :before-upload="beforeUploadVideo"
              :on-success="handleVideoUploadSuccess"
            >
              <el-button type="primary">上传视频</el-button>
            </el-upload>
            <el-input v-model="formData.fileUrl" placeholder="上传后自动填充，也可手动填写URL" />
          </div>
          <div class="form-tip">支持 mp4 等视频格式，单个视频建议不超过 200MB。</div>
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
        <div v-if="detail.coverImage" class="detail-cover">
          <img :src="detail.coverImage" alt="封面" />
        </div>
        <div v-if="detail.summary" class="detail-block">
          <div class="label">摘要</div>
          <div class="value">{{ detail.summary }}</div>
        </div>
        <div v-if="detail.fileUrl" class="detail-block">
          <div class="label">文件URL</div>
          <div class="value">
            <a :href="detail.fileUrl" target="_blank" rel="noreferrer">{{ detail.fileUrl }}</a>
          </div>
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
  type: undefined as string | undefined,
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
  fileUrl: "",
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

const uploadAction = "/api/grief-resource/upload";
const uploadHeaders = computed(() => {
  const token = sessionStorage.getItem("token");
  return {
    Authorization: token ? `Bearer ${token}` : "",
  };
});

function beforeUploadCover(file: File) {
  if (!file.type.startsWith("image/")) {
    ElMessage.error("只能上传图片文件");
    return false;
  }
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    ElMessage.error("封面图片不能超过 10MB");
    return false;
  }
  return true;
}

function beforeUploadVideo(file: File) {
  if (!file.type.startsWith("video/")) {
    ElMessage.error("只能上传视频文件");
    return false;
  }
  const isLt200M = file.size / 1024 / 1024 < 200;
  if (!isLt200M) {
    ElMessage.error("视频文件不能超过 200MB");
    return false;
  }
  return true;
}

function handleCoverUploadSuccess(response: any) {
  if (response?.code === 200) {
    formData.coverImage = response.data;
    ElMessage.success("封面上传成功");
  } else {
    ElMessage.error(response?.message || "封面上传失败");
  }
}

function handleVideoUploadSuccess(response: any) {
  if (response?.code === 200) {
    formData.fileUrl = response.data;
    ElMessage.success("视频上传成功");
  } else {
    ElMessage.error(response?.message || "视频上传失败");
  }
}

const formRules = {
  type: [{ required: true, message: "请选择类型", trigger: "change" }],
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  fileUrl: [
    {
      validator: (_: any, value: any, cb: any) => {
        if (formData.type === "video" && (!value || String(value).trim() === "")) {
          cb(new Error("请上传视频文件或填写文件URL"));
          return;
        }
        cb();
      },
      trigger: "blur",
    },
  ],
};

function goBack() {
  router.back();
}

async function loadResources() {
  loading.value = true;
  try {
    const res = await griefResourceApi.getGriefResourceList({
      type: filterForm.type,
      status: filterForm.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    });
    resourceList.value = res.data?.records || [];
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
  filterForm.type = undefined;
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
    fileUrl: "",
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
    type: row.type || "article",
    content: row.content || "",
    fileUrl: row.fileUrl || "",
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
.grief-resource-management-container {
  padding: 20px;

  .content-wrapper {
    margin-top: 20px;
  }

  .action-bar {
    margin-bottom: 16px;
    display: flex;
    gap: 10px;
  }

  .filter-card {
    margin-bottom: 16px;
  }

  .pager {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .detail {
    .detail-title {
      font-size: 18px;
      font-weight: 700;
      color: #303133;
      margin-bottom: 10px;
    }

    .detail-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      color: #909399;
      font-size: 12px;
      margin-bottom: 12px;
    }

    .detail-cover {
      width: 100%;
      max-height: 420px;
      border-radius: 10px;
      overflow: hidden;
      margin-bottom: 12px;

      img {
        width: 100%;
        height: auto;
        object-fit: cover;
      }
    }

    .detail-block {
      margin-bottom: 12px;
      .label {
        font-size: 12px;
        color: #909399;
        margin-bottom: 6px;
      }
      .value {
        padding: 12px;
        background: #f5f7fa;
        border-radius: 8px;
        color: #303133;
        line-height: 1.7;
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
    color: #909399;
    font-size: 12px;
  }
}
</style>



