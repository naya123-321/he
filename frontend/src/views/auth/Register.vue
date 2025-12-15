<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h2>用户注册</h2>
        <p>加入我们，为爱宠送上最后的仪式感</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            @blur="checkUsername"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱（可选）"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号（可选）"
            prefix-icon="Phone"
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleRegister"
            style="width: 100%"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <el-link type="primary" @click="$router.push('/login')">
            立即登录
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, type FormInstance } from "element-plus";
import { register, checkUsername } from "@/api/user";

const router = useRouter();
const registerFormRef = ref<FormInstance>();

const loading = ref(false);

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  email: "",
  phone: "",
});

// 自定义验证规则
const validateUsername = async (rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error("请输入用户名"));
  }
  if (value.length < 3 || value.length > 20) {
    return callback(new Error("用户名长度3-20个字符"));
  }
};

const validatePassword = (rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error("请输入密码"));
  }
  if (value.length < 6) {
    return callback(new Error("密码长度不能小于6位"));
  }
};

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error("请确认密码"));
  }
  if (value !== registerForm.password) {
    return callback(new Error("两次密码不一致"));
  }
  callback();
};

const registerRules = {
  username: [{ validator: validateUsername, trigger: "blur" }],
  password: [{ validator: validatePassword, trigger: "blur" }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: "blur" }],
  email: [{ type: "email", message: "邮箱格式不正确", trigger: "blur" }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
};

// 检查用户名是否已存在
const checkUsername = async () => {
  if (!registerForm.username) return;

  try {
    const res = await checkUsername(registerForm.username);
    if (res.data.data) {
      ElMessage.warning("用户名已存在");
    }
  } catch (error) {
    // 忽略错误
  }
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  const valid = await registerFormRef.value.validate();
  if (!valid) return;

  loading.value = true;

  try {
    const { confirmPassword, ...registerData } = registerForm;
    const res = await register(registerData);

    if (res.data.code === 200 && res.data.data) {
      ElMessage.success("注册成功");
      router.push("/login");
    } else {
      ElMessage.error(res.data.message || "注册失败");
    }
  } catch (error: any) {
    ElMessage.error(error.message || "注册失败");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .register-box {
    width: 450px;
    padding: 40px;
    background: white;
    border-radius: 10px;
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);

    .register-header {
      text-align: center;
      margin-bottom: 30px;

      h2 {
        margin: 0 0 10px;
        color: #333;
        font-size: 24px;
      }

      p {
        margin: 0;
        color: #666;
        font-size: 14px;
      }
    }

    .register-form {
      .el-form-item {
        margin-bottom: 20px;
      }
    }

    .register-footer {
      display: flex;
      justify-content: center;
      align-items: center;
      margin-top: 20px;
      font-size: 14px;
      color: #666;

      .el-link {
        margin-left: 5px;
      }
    }
  }
}
</style>
