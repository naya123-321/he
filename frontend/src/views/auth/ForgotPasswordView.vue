<template>
  <div class="forgot-password-container">
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
            @blur="loadSecurityQuestion"
          />
        </el-form-item>

        <el-form-item prop="securityQuestion">
          <el-select
            v-model="forgotForm.securityQuestion"
            placeholder="请先输入账号以获取密保问题"
            size="large"
            class="custom-select"
            style="width: 100%"
            :disabled="!forgotForm.securityQuestion"
          >
            <el-option :label="forgotForm.securityQuestion" :value="forgotForm.securityQuestion" />
          </el-select>
        </el-form-item>

        <el-form-item prop="securityAnswer">
          <el-input
            v-model="forgotForm.securityAnswer"
            placeholder="请输入密保答案"
            prefix-icon="Key"
            size="large"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="newPassword">
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

        <el-form-item prop="confirmPassword">
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
            重置密码
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
  getSecurityQuestion,
  resetPasswordBySecurity,
} from "@/api/user";

const router = useRouter();
const forgotFormRef = ref<FormInstance>();

const loading = ref(false);

const forgotForm = reactive({
  username: "",
  securityQuestion: "",
  securityAnswer: "",
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
  securityQuestion: [{ required: true, message: "请先获取密保问题", trigger: "change" }],
  securityAnswer: [{ required: true, message: "请输入密保答案", trigger: "blur" }],
  newPassword: [{ validator: validatePassword, trigger: "blur" }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: "blur" }],
};

const loadSecurityQuestion = async () => {
  const username = (forgotForm.username || "").trim();
  if (!username || username.length < 3) return;
  try {
    // request.ts 成功时直接返回 { code, message, data }
    const res = await getSecurityQuestion(username);
    forgotForm.securityQuestion = (res.data || "").trim();
  } catch (error: any) {
    forgotForm.securityQuestion = "";
    // 失败提示交给 axios 拦截器，避免重复弹窗
  }
};

// 处理表单提交
const handleSubmit = async () => {
  if (!forgotFormRef.value) return;

  const valid = await forgotFormRef.value.validate();
  if (!valid) return;

  try {
    loading.value = true;
    const res = await resetPasswordBySecurity({
      username: forgotForm.username.trim(),
      securityQuestion: forgotForm.securityQuestion,
      securityAnswer: forgotForm.securityAnswer,
      newPassword: forgotForm.newPassword,
    });

    // request.ts 成功时直接返回 { code, message, data }，其中 data 为 boolean
    if (res.data) {
      ElMessage.success("密码重置成功，请使用新密码登录");
      router.push("/login");
    } else {
      ElMessage.error("重置密码失败");
    }
  } catch (error: any) {
    // 失败提示交给 axios 拦截器，避免重复弹窗
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
  width: 100%;
  padding: 24px;
  background: #f5f7fa;

  .forgot-password-box {
    width: 100%;
    max-width: 520px;
    padding: 24px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

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
    .forgot-password-box {
      padding: 20px;
      box-shadow: none;
    }
  }
}
</style>
