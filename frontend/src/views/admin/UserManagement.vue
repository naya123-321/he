<template>
  <div class="user-management-container">
    <el-page-header @back="goBack" content="用户管理" />
    
    <div class="content-wrapper">
      <!-- 搜索和筛选栏 -->
      <div class="search-bar">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索用户名、昵称、邮箱"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :span="4">
            <el-select
              v-model="searchForm.role"
              placeholder="选择角色"
              clearable
              @change="handleSearch"
            >
              <el-option label="全部" :value="undefined" />
              <el-option label="宠主" :value="0" />
              <el-option label="服务人员" :value="1" />
              <el-option label="管理员" :value="2" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </el-col>
          <el-col :span="4">
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 用户列表表格 -->
      <el-table
        v-loading="loading"
        :data="userList"
        style="width: 100%"
        stripe
        border
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="info" size="small" @click="handleView(row)">
              详情
            </el-button>
            <el-button type="warning" size="small" @click="handleResetPassword(row)">
              重置密码
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
              :disabled="row.role === 2"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 编辑用户对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editDialogTitle"
      width="600px"
      @close="handleEditDialogClose"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" :disabled="!!editForm.id" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" type="email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="宠主" :value="0" />
            <el-option label="服务人员" :value="1" />
            <el-option label="管理员" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="editForm.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
            <el-radio label="">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="editForm.birthday"
            type="date"
            placeholder="选择生日"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitEdit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="用户详情"
      width="700px"
    >
      <div v-if="currentUser" class="user-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="getRoleType(currentUser.role)">
              {{ getRoleText(currentUser.role) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="性别">{{ currentUser.gender || '-' }}</el-descriptions-item>
          <el-descriptions-item label="生日">{{ currentUser.birthday || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间" :span="2">
            {{ formatDate(currentUser.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">
            {{ formatDate(currentUser.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="个人简介" :span="2">
            {{ currentUser.bio || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button type="primary" @click="handleEditFromDetail">编辑</el-button>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog
      v-model="resetPasswordDialogVisible"
      title="重置密码"
      width="500px"
    >
      <el-form
        ref="resetPasswordFormRef"
        :model="resetPasswordForm"
        :rules="resetPasswordRules"
        label-width="100px"
      >
        <el-form-item label="用户名">
          <el-input v-model="resetPasswordForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetPasswordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="resetPasswordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitResetPassword" :loading="submitting">
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
import { Search, Refresh } from '@element-plus/icons-vue';
import { getUserList, updateUser, deleteUser, adminResetPassword } from '@/api/user';
import type { UserVO } from '@/api/user';

const router = useRouter();

// 数据
const loading = ref(false);
const submitting = ref(false);
const userList = ref<UserVO[]>([]);
const currentUser = ref<UserVO | null>(null);

// 搜索表单
const searchForm = reactive({
  keyword: '',
  role: undefined as number | undefined,
});

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
});

// 对话框状态
const editDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const resetPasswordDialogVisible = ref(false);
const editDialogTitle = ref('编辑用户');
const editFormRef = ref<FormInstance>();
const resetPasswordFormRef = ref<FormInstance>();

// 编辑表单
const editForm = reactive({
  id: undefined as number | undefined,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  role: 0,
  gender: '',
  birthday: '',
  bio: '',
});

// 重置密码表单
const resetPasswordForm = reactive({
  userId: 0,
  username: '',
  newPassword: '',
  confirmPassword: '',
});

// 表单验证规则
const editRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度2-20个字符', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' },
  ],
};

const resetPasswordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== resetPasswordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 获取角色类型
const getRoleType = (role: number) => {
  const typeMap: Record<number, string> = {
    0: 'success',
    1: 'info',
    2: 'warning',
  };
  return typeMap[role] || 'info';
};

// 获取角色文本
const getRoleText = (role: number) => {
  const textMap: Record<number, string> = {
    0: '宠主',
    1: '服务人员',
    2: '管理员',
  };
  return textMap[role] || '未知';
};

// 格式化日期
const formatDate = (dateString?: string) => {
  if (!dateString) return '-';
  try {
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch (error) {
    return dateString;
  }
};

// 加载用户列表
const loadUserList = async () => {
  loading.value = true;
  try {
    const res = await getUserList({
      role: searchForm.role,
      keyword: searchForm.keyword || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    });

    if (res && res.code === 200 && res.data) {
      userList.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    } else {
      ElMessage.error('获取用户列表失败');
      userList.value = [];
    }
  } catch (error: any) {
    console.error('获取用户列表失败:', error);
    ElMessage.error(error?.message || '获取用户列表失败，请稍后重试');
    userList.value = [];
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1;
  loadUserList();
};

// 重置搜索
const handleReset = () => {
  searchForm.keyword = '';
  searchForm.role = undefined;
  pagination.pageNum = 1;
  loadUserList();
};

// 分页大小改变
const handleSizeChange = (size: number) => {
  pagination.pageSize = size;
  pagination.pageNum = 1;
  loadUserList();
};

// 页码改变
const handlePageChange = (page: number) => {
  pagination.pageNum = page;
  loadUserList();
};

// 编辑用户
const handleEdit = (row: UserVO) => {
  editDialogTitle.value = '编辑用户';
  Object.assign(editForm, {
    id: row.id,
    username: row.username,
    nickname: row.nickname || '',
    email: row.email || '',
    phone: row.phone || '',
    role: row.role ?? 0,
    gender: row.gender || '',
    birthday: row.birthday || '',
    bio: row.bio || '',
  });
  editDialogVisible.value = true;
};

// 查看用户详情
const handleView = (row: UserVO) => {
  currentUser.value = row;
  detailDialogVisible.value = true;
};

// 从详情页编辑
const handleEditFromDetail = () => {
  if (currentUser.value) {
    detailDialogVisible.value = false;
    handleEdit(currentUser.value);
  }
};

// 删除用户
const handleDelete = (row: UserVO) => {
  if (row.role === 2) {
    ElMessage.warning('不能删除管理员账号');
    return;
  }

  ElMessageBox.confirm(
    `确定要删除用户"${row.username}"吗？此操作不可恢复。`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        const res = await deleteUser(row.id!);
        if (res && res.code === 200) {
          ElMessage.success('删除成功');
          loadUserList();
        } else {
          ElMessage.error(res?.message || '删除失败');
        }
      } catch (error: any) {
        console.error('删除用户失败:', error);
        ElMessage.error(error?.message || '删除失败，请稍后重试');
      }
    })
    .catch(() => {
      // 取消操作
    });
};

// 重置密码
const handleResetPassword = (row: UserVO) => {
  resetPasswordForm.userId = row.id!;
  resetPasswordForm.username = row.username;
  resetPasswordForm.newPassword = '';
  resetPasswordForm.confirmPassword = '';
  resetPasswordDialogVisible.value = true;
};

// 提交编辑
const handleSubmitEdit = async () => {
  if (!editFormRef.value) return;

  await editFormRef.value.validate(async (valid) => {
    if (!valid) return;

    submitting.value = true;
    try {
      const res = await updateUser(editForm.id!, editForm);
      if (res && res.code === 200) {
        ElMessage.success('更新成功');
        editDialogVisible.value = false;
        loadUserList();
      } else {
        ElMessage.error(res?.message || '更新失败');
      }
    } catch (error: any) {
      console.error('更新用户失败:', error);
      ElMessage.error(error?.message || '更新失败，请稍后重试');
    } finally {
      submitting.value = false;
    }
  });
};

// 提交重置密码
const handleSubmitResetPassword = async () => {
  if (!resetPasswordFormRef.value) return;

  await resetPasswordFormRef.value.validate(async (valid) => {
    if (!valid) return;

    submitting.value = true;
    try {
      const res = await adminResetPassword(resetPasswordForm.userId, resetPasswordForm.newPassword);
      if (res && res.code === 200) {
        ElMessage.success('密码重置成功');
        resetPasswordDialogVisible.value = false;
        resetPasswordFormRef.value.resetFields();
      } else {
        ElMessage.error(res?.message || '密码重置失败');
      }
    } catch (error: any) {
      console.error('重置密码失败:', error);
      ElMessage.error(error?.message || '重置密码失败，请稍后重试');
    } finally {
      submitting.value = false;
    }
  });
};

// 编辑对话框关闭
const handleEditDialogClose = () => {
  editFormRef.value?.resetFields();
  Object.assign(editForm, {
    id: undefined,
    username: '',
    nickname: '',
    email: '',
    phone: '',
    role: 0,
    gender: '',
    birthday: '',
    bio: '',
  });
};

onMounted(() => {
  loadUserList();
});
</script>

<style scoped lang="scss">
.user-management-container {
  padding: 20px;

  .content-wrapper {
    margin-top: 20px;
  }

  .search-bar {
    margin-bottom: 20px;
    padding: 20px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .user-detail {
    padding: 10px 0;
  }
}
</style>
