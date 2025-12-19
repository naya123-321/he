<template>
  <div class="template-management">
    <el-page-header @back="goBack" content="模板管理" />

    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加模板
      </el-button>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索模板名称或描述"
          clearable
          style="width: 300px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterStatus"
          placeholder="状态"
          clearable
          style="width: 120px; margin-left: 10px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-select
          v-model="filterCategory"
          placeholder="分类"
          clearable
          style="width: 150px; margin-left: 10px"
        >
          <el-option label="全部" value="" />
          <el-option label="简约风格" value="simple" />
          <el-option label="温馨风格" value="warm" />
          <el-option label="经典风格" value="classic" />
          <el-option label="现代风格" value="modern" />
          <el-option label="清新风格" value="vintage" />
          <el-option label="文艺风格" value="literary" />
          <el-option label="其他" value="other" />
        </el-select>
        <el-button type="primary" @click="handleSearch" style="margin-left: 10px">
          搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="templateList"
      stripe
      style="width: 100%; margin-top: 20px"
    >
      <el-table-column type="index" label="序号" width="80" :index="getIndex" />
      <el-table-column label="预览图" width="120">
        <template #default="{ row }">
          <el-image
            v-if="row.previewImage"
            :src="row.previewImage"
            fit="cover"
            style="width: 80px; height: 80px; border-radius: 4px; cursor: pointer"
            @click="openPreview(row)"
          >
            <template #error>
              <div style="width: 80px; height: 80px; display:flex; align-items:center; justify-content:center; color:#909399;">
                加载失败
              </div>
            </template>
          </el-image>
          <span v-else style="color: #999">无预览图</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="模板名称" min-width="150" />
      <el-table-column prop="categoryText" label="分类" width="120" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.statusText }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdByName" label="创建者" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button
            link
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? "禁用" : "启用" }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="1000px"
      @close="handleDialogClose"
    >
      <TemplateEditor
        :key="editorKey"
        ref="templateEditorRef"
        :model-value="formData"
        @update:model-value="(val) => Object.assign(formData, val)"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          保存模板
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览弹窗（点击缩略图放大） -->
    <el-dialog v-model="previewDialogVisible" :title="previewTitle" width="920px">
      <el-empty v-if="previewImages.length === 0" description="暂无模板图片" />
      <el-carousel v-else height="520px" indicator-position="outside">
        <el-carousel-item v-for="(url, idx) in previewImages" :key="url">
          <el-image
            :src="url"
            fit="contain"
            style="width: 100%; height: 520px"
            :preview-src-list="previewImages"
            :initial-index="idx"
          />
        </el-carousel-item>
      </el-carousel>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Search } from "@element-plus/icons-vue";
import { templateApi, type TemplateVO, type TemplateCreateDTO } from "@/api/memorial";
import TemplateEditor from "@/components/template/TemplateEditor.vue";
import dayjs from "dayjs";

const router = useRouter();

const loading = ref(false);
const submitting = ref(false);
const templateList = ref<TemplateVO[]>([]);
const searchKeyword = ref("");
const filterStatus = ref<number | undefined>(undefined);
const filterCategory = ref<string>("");

const dialogVisible = ref(false);
const dialogTitle = ref("添加模板");
const templateEditorRef = ref();
const editorKey = ref(0);
const formData = reactive<TemplateCreateDTO>({
  name: "",
  description: "",
  category: "",
  previewImage: "",
  templateImages: [],
  styleConfig: "",
  layoutConfig: "",
});
const editingId = ref<number | null>(null);

// 预览弹窗
const previewDialogVisible = ref(false);
const previewTitle = ref("模板图片预览");
const previewImages = ref<string[]>([]);

function openPreview(row: TemplateVO) {
  const imgs =
    (row as any)?.templateImages && Array.isArray((row as any).templateImages) && (row as any).templateImages.length > 0
      ? (row as any).templateImages
      : row.previewImage
        ? [row.previewImage]
        : [];
  previewImages.value = imgs.filter(Boolean);
  previewTitle.value = `模板图片预览：${row.name || ""}`.trim();
  previewDialogVisible.value = true;
}

// 返回
const goBack = () => {
  router.back();
};

// 格式化日期
const formatDate = (date: string | Date) => {
  if (!date) return "";
  return dayjs(date).format("YYYY-MM-DD HH:mm:ss");
};

// 计算序号：从1开始自动排列
const getIndex = (index: number) => {
  return index + 1;
};

// 加载模板列表
const loadTemplates = async () => {
  loading.value = true;
  try {
    const res = await templateApi.getTemplateList({
      keyword: searchKeyword.value || undefined,
      status: filterStatus.value,
      category: filterCategory.value || undefined,
    });
    if (res && res.code === 200) {
      templateList.value = res.data || [];
    } else {
      ElMessage.error(res?.message || "获取模板列表失败");
    }
  } catch (error) {
    console.error("获取模板列表失败:", error);
    ElMessage.error("获取模板列表失败");
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  loadTemplates();
};

// 重置
const handleReset = () => {
  searchKeyword.value = "";
  filterStatus.value = undefined;
  filterCategory.value = "";
  loadTemplates();
};

// 添加模板
const handleAdd = () => {
  dialogTitle.value = "添加模板";
  editingId.value = null;
  editorKey.value++;
  Object.assign(formData, {
    name: "",
    description: "",
    category: "",
    previewImage: "",
    templateImages: [],
    styleConfig: "",
    layoutConfig: "",
  });
  dialogVisible.value = true;
};

// 编辑模板
const handleEdit = (row: TemplateVO) => {
  dialogTitle.value = "编辑模板";
  editingId.value = row.id;
  editorKey.value++;
  
  // 尝试从styleConfig中解析templateImages
  let templateImages: string[] = [];
  if (row.templateImages && row.templateImages.length > 0) {
    templateImages = row.templateImages;
  } else if (row.styleConfig) {
    try {
      const parsed = JSON.parse(row.styleConfig);
      if (parsed.templateImages && Array.isArray(parsed.templateImages)) {
        templateImages = parsed.templateImages;
      }
    } catch (e) {
      // 解析失败，使用previewImage作为兼容
      if (row.previewImage) {
        templateImages = [row.previewImage];
      }
    }
  } else if (row.previewImage) {
    templateImages = [row.previewImage];
  }
  
  Object.assign(formData, {
    name: row.name || "",
    description: row.description || "",
    category: row.category || "",
    previewImage: row.previewImage || "",
    templateImages: templateImages,
    styleConfig: row.styleConfig || "",
    layoutConfig: row.layoutConfig || "",
  });
  dialogVisible.value = true;
};

// 删除模板
const handleDelete = async (row: TemplateVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板"${row.name}"吗？`,
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    
    const res = await templateApi.deleteTemplate(row.id);
    if (res && res.code === 200) {
      ElMessage.success("删除成功");
      loadTemplates();
    } else {
      ElMessage.error(res?.message || "删除失败");
    }
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除模板失败:", error);
      ElMessage.error("删除失败");
    }
  }
};

// 切换状态
const handleToggleStatus = async (row: TemplateVO) => {
  const newStatus = row.status === 1 ? 0 : 1;
  const statusText = newStatus === 1 ? "启用" : "禁用";
  
  try {
    const res = await templateApi.updateTemplateStatus(row.id, newStatus);
    if (res && res.code === 200) {
      ElMessage.success(`${statusText}成功`);
      loadTemplates();
    } else {
      ElMessage.error(res?.message || `${statusText}失败`);
    }
  } catch (error) {
    console.error(`${statusText}模板失败:`, error);
    ElMessage.error(`${statusText}失败`);
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!templateEditorRef.value) return;
  
  try {
    // 验证表单
    await templateEditorRef.value.validate();
    
      submitting.value = true;
      try {
        // 准备提交数据：将templateImages转换为JSON字符串存储到styleConfig
        // 同时将第一张图作为previewImage（兼容性）
        const submitData: TemplateCreateDTO = {
          name: formData.name,
          description: formData.description,
          category: formData.category,
          previewImage: formData.templateImages && formData.templateImages.length > 0
            ? formData.templateImages[0]
            : formData.previewImage || "",
          styleConfig: formData.templateImages && formData.templateImages.length > 0
            ? JSON.stringify({ templateImages: formData.templateImages })
            : formData.styleConfig || "",
          layoutConfig: formData.layoutConfig || "",
        };
        
        let res;
        if (editingId.value) {
          res = await templateApi.updateTemplate(editingId.value, submitData);
        } else {
          res = await templateApi.createTemplate(submitData);
        }
        
        if (res && res.code === 200) {
          ElMessage.success(editingId.value ? "更新成功" : "创建成功");
          dialogVisible.value = false;
          loadTemplates();
        } else {
          ElMessage.error(res?.message || "操作失败");
        }
      } catch (error: any) {
        console.error("操作失败:", error);
        ElMessage.error(error?.message || "操作失败");
      } finally {
        submitting.value = false;
      }
  } catch (error) {
    // 表单验证失败，不执行提交
    console.log("表单验证失败");
  }
};

// 对话框关闭
const handleDialogClose = () => {
  templateEditorRef.value?.resetFields();
  editingId.value = null;
  Object.assign(formData, {
    name: "",
    description: "",
    category: "",
    previewImage: "",
    templateImages: [],
    styleConfig: "",
    layoutConfig: "",
  });
};

onMounted(() => {
  loadTemplates();
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

.template-management {
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

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 24px;
    margin-bottom: 24px;
    padding: 20px;
    background: $bg-white;
    border-radius: 12px;
    border: 1px solid $border-color;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    :deep(.el-button) {
      border-radius: 8px;
      font-weight: 500;

      &.el-button--primary {
        background-color: $primary-color;
        border-color: $primary-color;
      }
    }

    .search-bar {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      :deep(.el-input__wrapper) {
        border-radius: 8px;
      }

      :deep(.el-select .el-input__wrapper) {
        border-radius: 8px;
      }

      :deep(.el-button) {
        border-radius: 8px;
        font-weight: 500;

        &.el-button--primary {
          background-color: $primary-color;
          border-color: $primary-color;
        }
      }
    }
  }

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .el-table__header {
      th {
        background: $bg-light;
        color: $text-primary;
        font-weight: 600;
        border-bottom: 2px solid $border-color;
      }
    }

    .el-table__body {
      tr {
        transition: all 0.2s ease;

        &:hover {
          background: rgba(64, 158, 255, 0.05);
        }
      }

      td {
        border-bottom: 1px solid $border-color;
      }
    }
  }

  :deep(.el-tag) {
    border-radius: 999px;
    font-weight: 500;
  }

  :deep(.el-button) {
    border-radius: 8px;
    font-weight: 500;
    margin-right: 8px;
    margin-bottom: 4px;
  }

  :deep(.el-image) {
    border-radius: 8px;
    overflow: hidden;
  }
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
      font-size: 18px;
      font-weight: 600;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: $bg-white;
      font-size: 20px;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }
}
</style>
