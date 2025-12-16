<template>
  <div class="profile-container">
    <el-page-header @back="goBack" content="个人资料" />

    <div class="profile-content">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="profile-card">
            <div class="avatar-container">
              <el-avatar :size="120" :src="userInfo.avatar" />
              <el-button
                type="text"
                class="change-avatar-btn"
                @click="changeAvatar"
              >
                更换头像
              </el-button>
            </div>
            <div class="user-info">
              <h3>{{ userInfo.username }}</h3>
              <p class="user-role">{{ getRoleText(userInfo.role) }}</p>
              <p class="join-date">
                加入时间：{{ formatDate(userInfo.createTime) }}
              </p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="16">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <span>基本信息</span>
                <el-button type="primary" @click="editMode = !editMode">
                  {{ editMode ? "取消" : "编辑" }}
                </el-button>
              </div>
            </template>

            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="100px"
              :disabled="!editMode"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="profileForm.username" />
              </el-form-item>

              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" />
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" />
              </el-form-item>

              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" />
              </el-form-item>

              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="profileForm.gender">
                  <el-radio label="male">男</el-radio>
                  <el-radio label="female">女</el-radio>
                  <el-radio label="secret">保密</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="生日" prop="birthday">
                <el-date-picker
                  v-model="profileForm.birthday"
                  type="date"
                  placeholder="选择生日"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>

              <el-form-item label="个人简介" prop="bio">
                <el-input
                  v-model="profileForm.bio"
                  type="textarea"
                  :rows="4"
                  placeholder="介绍一下自己..."
                />
              </el-form-item>

              <el-form-item v-if="editMode">
                <el-button
                  type="primary"
                  @click="saveProfile"
                  :loading="saving"
                >
                  保存
                </el-button>
                <el-button @click="resetForm">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="24">
          <el-card class="password-card">
            <template #header>
              <div class="card-header">
                <span>修改密码</span>
              </div>
            </template>

            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
            >
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="当前密码" prop="oldPassword">
                    <el-input
                      v-model="passwordForm.oldPassword"
                      type="password"
                      show-password
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input
                      v-model="passwordForm.newPassword"
                      type="password"
                      show-password
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input
                      v-model="passwordForm.confirmPassword"
                      type="password"
                      show-password
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item>
                <el-button
                  type="primary"
                  @click="changePassword"
                  :loading="changingPassword"
                >
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="24">
          <el-card class="password-card">
            <template #header>
              <div class="card-header">
                <span>密保设置</span>
              </div>
            </template>

            <el-form
              ref="securityFormRef"
              :model="securityForm"
              :rules="securityRules"
              label-width="110px"
            >
              <el-form-item label="密保问题" prop="securityQuestion">
                <el-select v-model="securityForm.securityQuestion" placeholder="请选择密保问题" style="width: 100%">
                  <el-option label="您最喜欢的宠物名字是？" value="您最喜欢的宠物名字是？" />
                  <el-option label="您最喜欢的城市是？" value="您最喜欢的城市是？" />
                  <el-option label="您小学班主任的名字是？" value="您小学班主任的名字是？" />
                  <el-option label="您最喜欢的颜色是？" value="您最喜欢的颜色是？" />
                </el-select>
              </el-form-item>

              <el-form-item label="密保答案" prop="securityAnswer">
                <el-input
                  v-model="securityForm.securityAnswer"
                  placeholder="请输入密保答案"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="savingSecurity" @click="saveSecurity">
                  保存密保
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>

      <!-- 头像上传对话框 -->
      <el-dialog
        v-model="avatarDialogVisible"
        title="更换头像"
        width="500px"
        @close="avatarPreview = userInfo.avatar || ''"
      >
        <div class="avatar-upload-container">
          <div class="avatar-preview">
            <el-avatar :size="150" :src="avatarPreview" />
          </div>
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :before-upload="handleAvatarBeforeUpload"
            accept="image/*"
          >
            <el-button type="primary">选择图片</el-button>
          </el-upload>
          <p class="upload-tip">支持 JPG、PNG 格式，大小不超过 2MB</p>
        </div>
        <template #footer>
          <el-button @click="avatarDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="confirmAvatarUpload"
            :loading="avatarUploading"
          >
            确定
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/store/user";
import { updateUserInfo, changePassword, updateSecurityInfo } from "@/api/user";

const router = useRouter();
const userStore = useUserStore();

// 用户信息
const userInfo = ref({
  id: 0,
  username: "",
  nickname: "",
  email: "",
  phone: "",
  avatar: "",
  role: 0 as number,
  gender: "",
  birthday: "",
  bio: "",
  createTime: "",
});

// 编辑模式
const editMode = ref(false);
// 保存状态
const saving = ref(false);
// 修改密码状态
const changingPassword = ref(false);

// 表单引用
const profileFormRef = ref();
const passwordFormRef = ref();
const securityFormRef = ref();

// 个人资料表单
const profileForm = reactive({
  username: "",
  nickname: "",
  email: "",
  phone: "",
  gender: "",
  birthday: "",
  bio: "",
});

// 密码表单
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

// 密保表单
const securityForm = reactive({
  securityQuestion: "",
  securityAnswer: "",
});

const savingSecurity = ref(false);

// 个人资料验证规则
const profileRules = reactive({
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "长度在 3 到 20 个字符", trigger: "blur" },
  ],
  email: [
    {
      type: "email",
      message: "请输入正确的邮箱地址",
      trigger: ["blur", "change"],
    },
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号码",
      trigger: ["blur", "change"],
    },
  ],
});

// 密码验证规则
const passwordRules = reactive({
  oldPassword: [{ required: true, message: "请输入当前密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, max: 20, message: "长度在 6 到 20 个字符", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认新密码", trigger: "blur" },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error("两次输入密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
});

const securityRules = reactive({
  securityQuestion: [{ required: true, message: "请选择密保问题", trigger: "change" }],
  securityAnswer: [{ required: true, message: "请输入密保答案", trigger: "blur" }],
});

// 返回上一页
const goBack = () => {
  router.back();
};

// 获取角色文本
const getRoleText = (role: number | string | null | undefined) => {
  if (role === null || role === undefined) {
    return "未知角色";
  }
  
  // 处理数字类型（0:宠主 1:服务人员 2:管理员）
  const roleNum = typeof role === 'string' ? parseInt(role) : role;
  
  const roleMap: Record<number, string> = {
    0: "宠主",
    1: "服务人员",
    2: "管理员",
  };
  
  return roleMap[roleNum] || "未知角色";
};

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return "";
  const d = new Date(date);
  return `${d.getFullYear()}-${(d.getMonth() + 1)
    .toString()
    .padStart(2, "0")}-${d.getDate().toString().padStart(2, "0")}`;
};

// 头像上传相关
const avatarDialogVisible = ref(false);
const avatarUploading = ref(false);
const avatarPreview = ref("");

// 更换头像
const changeAvatar = () => {
  avatarDialogVisible.value = true;
  avatarPreview.value = userInfo.value.avatar || "";
};

// 压缩图片
const compressImage = (file: File, maxWidth: number = 800, maxHeight: number = 800, quality: number = 0.8): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        // 计算缩放比例
        if (width > height) {
          if (width > maxWidth) {
            height = (height * maxWidth) / width;
            width = maxWidth;
          }
        } else {
          if (height > maxHeight) {
            width = (width * maxHeight) / height;
            height = maxHeight;
          }
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('无法创建画布上下文'));
          return;
        }

        ctx.drawImage(img, 0, 0, width, height);
        const compressedDataUrl = canvas.toDataURL('image/jpeg', quality);
        resolve(compressedDataUrl);
      };
      img.onerror = () => reject(new Error('图片加载失败'));
      img.src = e.target?.result as string;
    };
    reader.onerror = () => reject(new Error('文件读取失败'));
    reader.readAsDataURL(file);
  });
};

// 头像上传前处理
const handleAvatarBeforeUpload = async (file: File) => {
  const isImage = file.type.startsWith("image/");
  const isLt5M = file.size / 1024 / 1024 < 5;

  if (!isImage) {
    ElMessage.error("上传文件只能是图片格式!");
    return false;
  }
  if (!isLt5M) {
    ElMessage.error("上传图片大小不能超过 5MB!");
    return false;
  }

  // 压缩图片并转换为 base64
  try {
    const compressedDataUrl = await compressImage(file, 400, 400, 0.8);
    avatarPreview.value = compressedDataUrl;
    ElMessage.success("图片已压缩，可以上传");
  } catch (error) {
    console.error("图片压缩失败:", error);
    ElMessage.error("图片处理失败，请重试");
    return false;
  }
  
  return false; // 阻止自动上传，我们手动处理
};

// 确认上传头像
const confirmAvatarUpload = async () => {
  if (!avatarPreview.value) {
    ElMessage.warning("请选择头像图片");
    return;
  }

  avatarUploading.value = true;
  try {
    // 调用API更新用户信息（包含头像）
    const res = await updateUserInfo({
      avatar: avatarPreview.value, // base64 格式的头像
    });
    
    if (res && res.code === 200) {
      // 更新store中的用户信息
      await userStore.getUserInfo();

      // 更新本地显示的用户信息
      userInfo.value = { ...userStore.userInfo };

      avatarDialogVisible.value = false;
      ElMessage.success("头像更新成功");
    } else {
      ElMessage.error(res?.message || "更新头像失败");
    }
  } catch (error: any) {
    console.error("更新头像失败:", error);
    const errorMsg = error?.response?.data?.message || error?.message || "更新头像失败，请稍后重试";
    ElMessage.error(errorMsg);
    
    // 如果是404错误，提示用户重启后端服务
    if (error?.response?.status === 404) {
      console.warn("提示：如果后端服务已启动，请尝试重启后端服务以确保新接口生效");
    }
  } finally {
    avatarUploading.value = false;
  }
};

// 保存个人资料
const saveProfile = async () => {
  try {
    const valid = await profileFormRef.value.validate();
    if (!valid) return;

    saving.value = true;

    // 调用API更新用户信息
    await updateUserInfo({
      nickname: profileForm.nickname,
      email: profileForm.email,
      phone: profileForm.phone,
      gender: profileForm.gender,
      birthday: profileForm.birthday,
      bio: profileForm.bio,
    });

    // 更新store中的用户信息
    await userStore.getUserInfo();

    // 更新本地显示的用户信息
    userInfo.value = { ...userStore.userInfo };

    editMode.value = false;
    ElMessage.success("个人资料更新成功");
  } catch (error) {
    console.error("更新个人资料失败:", error);
    ElMessage.error("更新个人资料失败，请稍后重试");
  } finally {
    saving.value = false;
  }
};

// 保存密保
const saveSecurity = async () => {
  if (!securityFormRef.value) return;
  const valid = await securityFormRef.value.validate();
  if (!valid) return;
  try {
    savingSecurity.value = true;
    await updateSecurityInfo(securityForm.securityQuestion, securityForm.securityAnswer);
    ElMessage.success("密保设置成功");
    securityForm.securityAnswer = "";
  } catch (e: any) {
    // 错误提示交给 axios 拦截器统一处理
  } finally {
    savingSecurity.value = false;
  }
};

// 重置表单
const resetForm = () => {
  profileForm.username = userInfo.value.username;
  profileForm.nickname = userInfo.value.nickname;
  profileForm.email = userInfo.value.email;
  profileForm.phone = userInfo.value.phone;
  profileForm.gender = userInfo.value.gender;
  profileForm.birthday = userInfo.value.birthday;
  profileForm.bio = userInfo.value.bio;
};

// 修改密码
const changePassword = async () => {
  try {
    const valid = await passwordFormRef.value.validate();
    if (!valid) return;

    changingPassword.value = true;

    // 调用API修改密码
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });

    // 清空表单
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";

    ElMessage.success("密码修改成功，请重新登录");

    // 退出登录
    setTimeout(() => {
      userStore.logout();
      router.push("/login");
    }, 1500);
  } catch (error) {
    console.error("修改密码失败:", error);
    ElMessage.error("修改密码失败，请检查当前密码是否正确");
  } finally {
    changingPassword.value = false;
  }
};

// 初始化
onMounted(async () => {
  try {
    // 获取用户信息
    await userStore.getUserInfo();
    userInfo.value = { ...userStore.userInfo };

    // 初始化表单数据
    resetForm();
  } catch (error) {
    console.error("获取用户信息失败:", error);
    ElMessage.error("获取用户信息失败");
  }
});
</script>

<style scoped lang="scss">
.profile-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;

  .profile-content {
    margin-top: 20px;

    .profile-card {
      text-align: center;

      .avatar-container {
        position: relative;
        margin-bottom: 20px;

        .change-avatar-btn {
          display: block;
          margin-top: 10px;
          color: #409eff;
        }
      }

      .user-info {
        h3 {
          margin: 10px 0;
        }

        .user-role {
          color: #909399;
          margin: 5px 0;
        }

        .join-date {
          color: #c0c4cc;
          font-size: 14px;
          margin: 10px 0 0;
        }
      }
    }

    .info-card,
    .password-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
    }
  }

  .avatar-upload-container {
    text-align: center;
    padding: 20px 0;

    .avatar-preview {
      margin-bottom: 20px;
    }

    .avatar-uploader {
      margin-bottom: 10px;
    }

    .upload-tip {
      color: #909399;
      font-size: 12px;
      margin-top: 10px;
    }
  }
}
</style>
