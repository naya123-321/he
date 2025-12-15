<template>
  <div class="forgot-password-container">
    <div class="forgot-password-image">
      <div class="image-content">
        <h2>用爱守护</h2>
        <h3>最后的陪伴</h3>
        <p>专业温馨的宠物殡葬服务<br />让每一次告别都充满尊严与爱</p>
      </div>
    </div>
    <div class="forgot-password-box">
      <div class="forgot-password-header">
        <h2>找回密码</h2>
      </div>

      <el-form
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        class="forgot-password-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="forgotForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="forgotForm.email"
            placeholder="请输入注册邮箱"
            prefix-icon="Message"
            size="large"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item v-if="showVerificationCode" prop="verificationCode">
          <div class="verification-code-container">
            <el-input
              v-model="forgotForm.verificationCode"
              placeholder="请输入验证码"
              prefix-icon="Key"
              size="large"
              class="custom-input verification-input"
            />
            <el-button
              :disabled="countdown > 0"
              @click="sendCode"
              class="verification-button"
            >
              {{ countdown > 0 ? `${countdown}秒后重试` : "发送验证码" }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item v-if="showNewPassword" prop="newPassword">
          <el-input
            v-model="forgotForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            prefix-icon="Lock"
            size="large"
            show-password
            class="custom-input"
          />
        </el-form-item>

        <el-form-item v-if="showNewPassword" prop="confirmPassword">
          <el-input
            v-model="forgotForm.confirmPassword"
            type="password"
            placeholder="请确认新密码"
            prefix-icon="Lock"
            size="large"
            show-password
            class="custom-input"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleSubmit"
            style="width: 100%"
            class="forgot-password-button"
          >
            {{ getButtonText() }}
          </el-button>
        </el-form-item>

        <div class="forgot-password-footer">
          <el-link type="default" @click="$router.push('/login')">
            返回登录
          </el-link>
          <div class="register-link">
            <span>还没有账号？</span>
            <el-link type="primary" @click="$router.push('/register')">
              注册账号
            </el-link>
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, type FormInstance } from "element-plus";
import {
  userApi,
  sendVerificationCode,
  verifyUserIdentity,
  verifyEmailCode,
  resetPassword,
} from "@/api/user";

const router = useRouter();
const forgotFormRef = ref<FormInstance>();

const loading = ref(false);
const showVerificationCode = ref(false);
const showNewPassword = ref(false);
const countdown = ref(0);
let countdownTimer: number | null = null;

const forgotForm = reactive({
  username: "",
  email: "",
  verificationCode: "",
  newPassword: "",
  confirmPassword: "",
});

// 自定义验证规则
import type { FormItemRule } from "element-plus";

const validatePassword = (
  rule: FormItemRule,
  value: string,
  callback: (error?: Error) => void
) => {
  if (!value) {
    return callback(new Error("请输入新密码"));
  }
  if (value.length < 6) {
    return callback(new Error("密码长度不能小于6位"));
  }
  callback();
};

const validateConfirmPassword = (
  rule: FormItemRule,
  value: string,
  callback: (error?: Error) => void
) => {
  if (!value) {
    return callback(new Error("请确认新密码"));
  }
  if (value !== forgotForm.newPassword) {
    return callback(new Error("两次密码不一致"));
  }
  callback();
};

const forgotRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  email: [
    { required: true, message: "请输入邮箱", trigger: "blur" },
    { type: "email", message: "邮箱格式不正确", trigger: "blur" },
  ],
  verificationCode: [
    { required: true, message: "请输入验证码", trigger: "blur" },
  ],
  newPassword: [{ validator: validatePassword, trigger: "blur" }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: "blur" }],
};

// 获取按钮文本
const getButtonText = () => {
  if (!showVerificationCode.value) return "验证身份";
  if (!showNewPassword.value) return "验证邮箱";
  return "重置密码";
};

// 发送验证码
const sendCode = async () => {
  if (!forgotForm.email) {
    ElMessage.error("请先输入邮箱");
    return;
  }

  try {
    loading.value = true;
    await sendVerificationCode(forgotForm.email);

    ElMessage.success("验证码已发送到您的邮箱");

    // 开始倒计时
    countdown.value = 60;
    countdownTimer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(countdownTimer!);
        countdownTimer = null;
      }
    }, 1000);
  } catch (error: any) {
    ElMessage.error(error.message || "发送验证码失败");
  } finally {
    loading.value = false;
  }
};

// 处理表单提交
const handleSubmit = async () => {
  if (!forgotFormRef.value) return;

  const valid = await forgotFormRef.value.validate();
  if (!valid) return;

  try {
    loading.value = true;

    // 第一步：验证用户名和邮箱
    if (!showVerificationCode.value) {
      const res = await verifyUserIdentity(
        forgotForm.username,
        forgotForm.email
      );

      if (res.data.code === 200 && res.data.data) {
        showVerificationCode.value = true;
        ElMessage.success("验证成功，请输入邮箱验证码");
      } else {
        ElMessage.error(res.data.message || "用户名和邮箱不匹配");
      }
      return;
    }

    // 第二步：验证验证码
    if (!showNewPassword.value) {
      const res = await verifyEmailCode(
        forgotForm.email,
        forgotForm.verificationCode
      );

      if (res.data.code === 200 && res.data.data) {
        showNewPassword.value = true;
        ElMessage.success("验证码正确，请设置新密码");
      } else {
        ElMessage.error(res.data.message || "验证码错误");
      }
      return;
    }

    // 第三步：重置密码
    const res = await resetPassword(
      forgotForm.username,
      forgotForm.newPassword
    );

    if (res.data.code === 200 && res.data.data) {
      ElMessage.success("密码重置成功，请使用新密码登录");
      router.push("/login");
    } else {
      ElMessage.error(res.data.message || "重置密码失败");
    }
  } catch (error: any) {
    ElMessage.error(error.message || "操作失败");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  overflow: hidden;
  max-width: 1200px;
  margin: 0 auto;

  .forgot-password-image {
    flex: 0 0 50%;
    background-image: url("@/assets/images/pet-hero.svg");
    background-size: cover;
    background-position: center;
    background-color: #f5f7fa;
    position: relative;

    .image-content {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      color: #333;
      text-align: center;
      padding: 20px;
      max-width: 80%;

      h2 {
        font-size: 36px;
        margin-bottom: 10px;
        color: #333;
      }

      h3 {
        font-size: 28px;
        margin-bottom: 20px;
        color: #333;
      }

      p {
        font-size: 16px;
        line-height: 1.6;
        margin-bottom: 30px;
      }
    }
  }

  .forgot-password-box {
    flex: 0 0 50%;
    padding: 40px;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .forgot-password-header {
      text-align: center;
      margin-bottom: 30px;

      h2 {
        font-size: 28px;
        color: #333;
        font-weight: 600;
      }
    }

    .forgot-password-form {
      width: 100%;
      max-width: 400px;
      margin: 0 auto;

      .verification-code-container {
        display: flex;
        gap: 10px;

        .verification-input {
          flex: 1;
        }

        .verification-button {
          width: 120px;
        }
      }

      .forgot-password-button {
        height: 50px;
        font-size: 16px;
        font-weight: 500;
        border-radius: 6px;
      }

      .forgot-password-footer {
        margin-top: 20px;
        text-align: center;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .register-link {
          span {
            color: #666;
            margin-right: 5px;
          }
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .forgot-password-container {
    flex-direction: column;

    .forgot-password-image {
      flex: 0 0 auto;
      height: 200px;
    }

    .forgot-password-box {
      flex: 0 0 auto;
      padding: 20px;
    }
  }
}
</style>
