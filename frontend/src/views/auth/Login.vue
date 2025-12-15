<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>宠物殡葬服务系统</h2>
        <p>为爱宠送上最后的温暖</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <span>还没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')">
            立即注册
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
import { login } from "@/api/user";
import { useUserStore } from "@/store/user";

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref<FormInstance>();

const loading = ref(false);

const loginForm = reactive({
  username: "",
  password: "",
});

const loginRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6位", trigger: "blur" },
  ],
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;

  const valid = await loginFormRef.value.validate();
  if (!valid) return;

  loading.value = true;

  try {
    const res = await login(loginForm);

    if (res.data.code === 200) {
      // 保存token
      userStore.setToken(res.data.data);

      // 获取用户信息
      await userStore.getUserInfo();

      ElMessage.success("登录成功");

      // 根据角色跳转到不同页面
      const role = userStore.userRole;
      if (role === 3) {
        router.push("/admin/dashboard");
      } else {
        router.push("/home");
      }
    } else {
      ElMessage.error(res.data.message || "登录失败");
    }
  } catch (error: any) {
    ElMessage.error(error.message || "登录失败");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .login-box {
    width: 400px;
    padding: 40px;
    background: white;
    border-radius: 10px;
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);

    .login-header {
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

    .login-form {
      .el-form-item {
        margin-bottom: 24px;
      }
    }

    .login-footer {
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
