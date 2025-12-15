<template>
  <div class="profile-container">
    <!-- 用户信息卡片 -->
    <el-card class="user-info-card">
      <div class="user-header">
        <!-- 用户头像 -->
        <div class="avatar-section">
          <el-avatar :size="120" :src="userInfo.avatar" class="user-avatar" />
          <div class="avatar-actions">
            <el-upload
              action="/api/user/upload-avatar"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
            >
              <el-button size="small">更换头像</el-button>
            </el-upload>
          </div>
        </div>

        <!-- 用户基本信息 -->
        <div class="user-details">
          <div class="user-name-section">
            <h1>{{ userInfo.username }}</h1>
            <el-tag :type="getRoleType(userInfo.role)" size="large">
              {{ userInfo.roleName }}
            </el-tag>
            <el-button
              type="primary"
              size="small"
              @click="showEditDialog"
              style="margin-left: 12px"
            >
              编辑资料
            </el-button>
          </div>

          <div class="user-stats">
            <div class="stat-item">
              <div class="stat-number">{{ userStats.memorialCount }}</div>
              <div class="stat-label">纪念册</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ userStats.orderCount }}</div>
              <div class="stat-label">服务订单</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ userStats.favoriteCount }}</div>
              <div class="stat-label">收藏</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">
                {{ formatDate(userInfo.createTime) }}
              </div>
              <div class="stat-label">注册时间</div>
            </div>
          </div>

          <!-- 联系方式 -->
          <div class="contact-info">
            <div class="contact-item" v-if="userInfo.email">
              <el-icon><Message /></el-icon>
              <span>{{ userInfo.email }}</span>
            </div>
            <div class="contact-item" v-if="userInfo.phone">
              <el-icon><Phone /></el-icon>
              <span>{{ userInfo.phone }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 内容区域 -->
    <div class="content-tabs">
      <el-tabs v-model="activeTab">
        <!-- 我的纪念册 -->
        <el-tab-pane label="我的纪念册" name="memorials">
          <MemorialList :user-id="userId" />
        </el-tab-pane>

        <!-- 服务订单 -->
        <el-tab-pane label="服务订单" name="orders">
          <OrderList :user-id="userId" />
        </el-tab-pane>

        <!-- 我的收藏 -->
        <el-tab-pane label="我的收藏" name="favorites">
          <FavoriteList :user-id="userId" />
        </el-tab-pane>

        <!-- 账户设置 -->
        <el-tab-pane label="账户设置" name="settings">
          <AccountSettings :user-info="userInfo" @update="loadUserInfo" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑资料" width="600px">
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" />
        </el-form-item>

        <el-form-item label="个人简介">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="3"
            placeholder="简单介绍一下自己..."
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, type FormInstance } from "element-plus";
import { useUserStore } from "@/store/user";
import { userApi } from "@/api/user";
import MemorialList from "@/views/memorial/MemorialList.vue";
import OrderList from "@/views/order/OrderList.vue";
import FavoriteList from "@/components/user/FavoriteList.vue";
import AccountSettings from "@/components/user/AccountSettings.vue";
import dayjs from "dayjs";

const router = useRouter();
const userStore = useUserStore();
const editFormRef = ref<FormInstance>();

// 用户信息
const userId = computed(() => userStore.userInfo?.id);
const userInfo = ref<any>({});
const userStats = reactive({
  memorialCount: 0,
  orderCount: 0,
  favoriteCount: 0,
});

// 编辑状态
const activeTab = ref("memorials");
const editDialogVisible = ref(false);
const saving = ref(false);

// 编辑表单
const editForm = reactive({
  username: "",
  email: "",
  phone: "",
  bio: "",
});

// 表单验证规则
const editRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 2, max: 20, message: "用户名长度2-20个字符", trigger: "blur" },
  ],
  email: [{ type: "email", message: "邮箱格式不正确", trigger: "blur" }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
};

// 生命周期
onMounted(() => {
  if (!userId.value) {
    router.push("/login");
    return;
  }
  loadUserInfo();
  loadUserStats();
});

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const res = await userApi.getUserInfo(userId.value);
    userInfo.value = res.data;
    // 初始化编辑表单
    editForm.username = userInfo.value.username;
    editForm.email = userInfo.value.email || "";
    editForm.phone = userInfo.value.phone || "";
    editForm.bio = userInfo.value.bio || "";
  } catch (error) {
    console.error("加载用户信息失败:", error);
    ElMessage.error("加载用户信息失败");
  }
};

// 加载用户统计数据
const loadUserStats = async () => {
  try {
    const res = await userApi.getUserStats(userId.value);
    userStats.memorialCount = res.data.memorialCount;
    userStats.orderCount = res.data.orderCount;
    userStats.favoriteCount = res.data.favoriteCount;
  } catch (error) {
    console.error("加载用户统计数据失败:", error);
  }
};

// 显示编辑对话框
const showEditDialog = () => {
  editDialogVisible.value = true;
};

// 保存用户资料
const saveProfile = async () => {
  if (!editFormRef.value) return;
  try {
    await editFormRef.value.validate();
    saving.value = true;
    await userApi.updateUserInfo({
      id: userId.value,
      username: editForm.username,
      email: editForm.email,
      phone: editForm.phone,
      bio: editForm.bio,
    });
    ElMessage.success("资料更新成功");
    editDialogVisible.value = false;
    loadUserInfo();
  } catch (error) {
    console.error("更新资料失败:", error);
    if (!(error instanceof Error && error.message.includes("cancel"))) {
      ElMessage.error("更新资料失败");
    }
  } finally {
    saving.value = false;
  }
};

// 头像上传前校验
const beforeAvatarUpload = (file: File) => {
  const isJPG = file.type === "image/jpeg" || file.type === "image/png";
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG) {
    ElMessage.error("上传头像图片只能是 JPG/PNG 格式!");
  }
  if (!isLt2M) {
    ElMessage.error("上传头像图片大小不能超过 2MB!");
  }
  return isJPG && isLt2M;
};

// 头像上传成功处理
const handleAvatarSuccess = (response: any) => {
  if (response.code === 200) {
    userInfo.value.avatar = response.data;
    userStore.setUserAvatar(response.data);
    ElMessage.success("头像更新成功");
  } else {
    ElMessage.error("头像更新失败");
  }
};

// 获取角色标签类型
const getRoleType = (role: string) => {
  switch (role) {
    case "ADMIN":
      return "danger";
    case "VIP":
      return "primary";
    default:
      return "info";
  }
};

// 格式化日期
const formatDate = (dateStr: string) => {
  return dateStr ? dayjs(dateStr).format("YYYY-MM-DD") : "";
};
</script>

<style scoped lang="scss">
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.user-info-card {
  margin-bottom: 30px;
  overflow: hidden;
}

.user-header {
  display: flex;
  padding: 20px 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 40px;
}

.user-avatar {
  border: 4px solid #f5f5f5;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.avatar-actions {
  margin-top: 16px;
}

.user-details {
  flex: 1;
}

.user-name-section {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.user-stats {
  display: flex;
  gap: 30px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.contact-info {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.contact-item {
  display: flex;
  align-items: center;
  color: #606266;
  font-size: 14px;
}

.contact-item .el-icon {
  margin-right: 6px;
}

.content-tabs {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

@media (max-width: 768px) {
  .user-header {
    flex-direction: column;
    align-items: center;
  }

  .avatar-section {
    margin-right: 0;
    margin-bottom: 30px;
  }

  .user-name-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-stats {
    flex-wrap: wrap;
  }
}
</style>
