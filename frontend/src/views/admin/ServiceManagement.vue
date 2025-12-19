<template>
  <div class="service-management-container">
    <el-page-header @back="goBack" content="服务定价与流程管理" />
    
    <div class="content-wrapper">
      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增服务类型
        </el-button>
        <el-button @click="loadServiceTypes">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 服务类型列表 -->
      <el-card shadow="never" style="border-radius: 12px; border: 1px solid #ebeef5; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);">
        <el-table 
          :data="serviceList" 
          v-loading="loading"
          style="width: 100%"
          stripe
        >
        <el-table-column prop="name" label="服务名称" width="200" />
        <el-table-column prop="description" label="服务描述" min-width="300" show-overflow-tooltip />
        <el-table-column label="价格" width="120" align="right">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.price?.toFixed(2) || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="服务时长" width="120" align="center">
          <template #default="{ row }">
            {{ row.duration ? `${row.duration}分钟` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="标签" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRecommended ? 'success' : 'info'" size="small">
              {{ row.isRecommended ? '推荐' : '标准' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button 
              :type="row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        </el-table>

        <!-- 空状态 -->
        <el-empty v-if="!loading && serviceList.length === 0" description="暂无服务类型数据" />
      </el-card>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="服务名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入服务名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="服务描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入服务描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="formData.price"
            :precision="2"
            :step="100"
            :min="0"
            :max="999999"
            placeholder="请输入价格"
            style="width: 100%"
          />
          <span style="margin-left: 10px; color: #909399;">元</span>
        </el-form-item>
        <el-form-item label="服务时长" prop="duration">
          <el-input-number
            v-model="formData.duration"
            :step="15"
            :min="15"
            :max="480"
            placeholder="请输入服务时长"
            style="width: 100%"
          />
          <span style="margin-left: 10px; color: #909399;">分钟</span>
        </el-form-item>
        <el-form-item label="服务流程" prop="process">
          <el-input
            v-model="formData.process"
            type="textarea"
            :rows="5"
            placeholder="请输入服务流程说明，例如：1. 预约咨询 2. 上门服务 3. 告别仪式 4. 后续服务"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="标签类型" prop="isRecommended">
          <el-radio-group v-model="formData.isRecommended">
            <el-radio :label="true">推荐</el-radio>
            <el-radio :label="false">标准</el-radio>
          </el-radio-group>
          <div style="margin-top: 8px; color: #909399; font-size: 12px;">
            选择"推荐"将在宠主端显示为绿色推荐标签，选择"标准"将显示为灰色标准标签
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { serviceTypeApi, type ServiceTypeVO } from '@/api/order';

const router = useRouter();

// 数据
const loading = ref(false);
const submitting = ref(false);
const serviceList = ref<ServiceTypeVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增服务类型');
const formRef = ref<FormInstance>();
const formData = reactive<ServiceTypeVO>({
  id: 0,
  name: '',
  description: '',
  price: 0,
  duration: 60,
  process: '',
  status: 1,
  isRecommended: false,
});

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入服务名称', trigger: 'blur' },
    { min: 1, max: 100, message: '服务名称长度在1到100个字符之间', trigger: 'blur' },
  ],
  description: [
    { max: 500, message: '服务描述不能超过500个字符', trigger: 'blur' },
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格必须大于等于0', trigger: 'blur' },
  ],
  duration: [
    { required: true, message: '请输入服务时长', trigger: 'blur' },
    { type: 'number', min: 15, message: '服务时长至少15分钟', trigger: 'blur' },
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' },
  ],
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 加载服务类型列表
const loadServiceTypes = async () => {
  loading.value = true;
  try {
    const res = await serviceTypeApi.getAll();
    if (res && res.code === 200 && res.data) {
      serviceList.value = res.data;
    } else {
      ElMessage.error('加载服务类型失败');
    }
  } catch (error: any) {
    console.error('加载服务类型失败:', error);
    ElMessage.error(error?.message || '加载服务类型失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 格式化日期
const formatDate = (dateString?: string) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增服务类型';
  resetForm();
  dialogVisible.value = true;
};

// 编辑
const handleEdit = (row: ServiceTypeVO) => {
  dialogTitle.value = '编辑服务类型';
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    description: row.description || '',
    price: row.price,
    duration: row.duration || 60,
    process: row.process || '',
    status: row.status ?? 1,
    isRecommended: row.isRecommended ?? false,
  });
  dialogVisible.value = true;
};

// 删除
const handleDelete = (row: ServiceTypeVO) => {
  ElMessageBox.confirm(
    `确定要删除服务类型"${row.name}"吗？此操作不可恢复。`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        const res = await serviceTypeApi.delete(row.id);
        if (res && res.code === 200) {
          ElMessage.success('删除成功');
          loadServiceTypes();
        } else {
          ElMessage.error(res?.message || '删除失败');
        }
      } catch (error: any) {
        console.error('删除服务类型失败:', error);
        ElMessage.error(error?.message || '删除失败，请稍后重试');
      }
    })
    .catch(() => {
      // 取消操作
    });
};

// 切换状态
const handleToggleStatus = (row: ServiceTypeVO) => {
  const newStatus = row.status === 1 ? 0 : 1;
  const statusText = newStatus === 1 ? '启用' : '禁用';
  
  ElMessageBox.confirm(
    `确定要${statusText}服务类型"${row.name}"吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        const res = await serviceTypeApi.updateStatus(row.id, newStatus);
        if (res && res.code === 200) {
          ElMessage.success(`${statusText}成功`);
          loadServiceTypes();
        } else {
          ElMessage.error(res?.message || `${statusText}失败`);
        }
      } catch (error: any) {
        console.error(`${statusText}服务类型失败:`, error);
        ElMessage.error(error?.message || `${statusText}失败，请稍后重试`);
      }
    })
    .catch(() => {
      // 取消操作
    });
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    
    submitting.value = true;
    try {
      let res;
      if (formData.id) {
        // 更新
        res = await serviceTypeApi.update(formData.id, formData);
      } else {
        // 创建
        res = await serviceTypeApi.create(formData);
      }
      
      if (res && res.code === 200) {
        ElMessage.success(formData.id ? '更新成功' : '创建成功');
        dialogVisible.value = false;
        loadServiceTypes();
      } else {
        ElMessage.error(res?.message || (formData.id ? '更新失败' : '创建失败'));
      }
    } catch (error: any) {
      console.error('提交失败:', error);
      ElMessage.error(error?.message || '操作失败，请稍后重试');
    } finally {
      submitting.value = false;
    }
  });
};

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    id: 0,
    name: '',
    description: '',
    price: 0,
    duration: 60,
    process: '',
    status: 1,
    isRecommended: false,
  });
  formRef.value?.clearValidate();
};

// 对话框关闭
const handleDialogClose = () => {
  resetForm();
};

onMounted(() => {
  loadServiceTypes();
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

.service-management-container {
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
  
  .price-text {
    font-weight: bold;
    color: $danger-color;
    font-size: 16px;
  }
  
  :deep(.el-table) {
    border-radius: 12px;
    overflow: hidden;
    border: 1px solid $border-color-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

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
    .el-input-number,
    .el-select {
      border-radius: 8px;
    }
  }

  :deep(.el-tag) {
    border-radius: 20px;
    padding: 0 12px;
    font-weight: 500;
  }
}
</style>
