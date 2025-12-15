<template>
  <div class="login-container">
    <div class="login-image">
      <div class="image-content">
        <h2>用爱守护</h2>
        <h3>最后的陪伴</h3>
        <p>专业温馨的宠物殡葬服务<br />让每一次告别都充满尊严与爱</p>
        <div class="client-count">
          <span>已服务家庭</span>
          <span class="count">10,000+</span>
        </div>
      </div>
    </div>
    <div class="login-box">
      <div class="login-header">
        <h2>用户登录</h2>
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
            placeholder="请输入账号"
            prefix-icon="User"
            size="large"
            class="custom-input"
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
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="role" class="role-selector">
          <el-select
            v-model="loginForm.role"
            placeholder="请选择角色"
            size="large"
            class="custom-select"
          >
            <el-option label="宠主" :value="0"></el-option>
            <el-option label="服务人员" :value="1"></el-option>
            <el-option label="管理员" :value="2"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item class="agreement">
          <el-checkbox v-model="loginForm.agree" class="agreement-checkbox">
            我已阅读并同意<a href="#" class="agreement-link">用户服务协议</a>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
            class="login-button"
          >
            登录
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <el-link
            type="default"
            @click="$router.push('/forgot-password')"
            class="forgot-password"
            >忘记密码?</el-link
          >
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
import type { FormItemRule } from "element-plus";
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
  role: null as number | null,
  agree: false,
});

const loginRules: Record<string, FormItemRule[]> = {
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6位", trigger: "blur" },
  ],
  role: [{ required: true, message: "请选择角色", trigger: "change" }],
  agree: [{ required: true, message: "请同意用户服务协议", trigger: "change" }],
};

const handleLogin = async () => {
  console.log("登录按钮被点击");
  if (!loginFormRef.value) {
    console.error("表单引用不存在");
    return;
  }

  try {
    const valid = await loginFormRef.value.validate();
    console.log("表单验证结果:", valid);
    if (!valid) {
      ElMessage.warning("请检查表单填写是否正确");
      return;
    }

    console.log("开始登录，表单数据:", loginForm);
    loading.value = true;

    const res = await login(loginForm);
    console.log("登录响应:", res);

    // 根据request.ts的响应拦截器，res已经是处理后的数据（code === 200时才会返回）
    if (res.data) {
      // 保存token
      userStore.setToken(res.data);

      // 获取用户信息
      await userStore.getUserInfo();

      ElMessage.success({
        message: "🎉 登录成功！",
        duration: 2000,
        showClose: true,
      });

      // 根据角色跳转到不同页面
      const role = userStore.userRole;
      console.log("用户角色:", role);
      
      // 延迟跳转，让用户看到成功提示
      setTimeout(() => {
        if (role === 0) {
          // 宠主端 - 跳转到服务套餐页面
          router.push("/pet-owner/service-packages").catch(() => {
            window.location.href = "/pet-owner/service-packages";
          });
        } else if (role === 1) {
          // 服务人员端
          router.push("/service-provider/appointment-list").catch(() => {
            window.location.href = "/service-provider/appointment-list";
          });
        } else if (role === 2) {
          // 管理员端
          router.push("/admin/dashboard").catch(() => {
            window.location.href = "/admin/dashboard";
          });
        } else {
          router.push("/").catch(() => {
            window.location.href = "/";
          });
        }
      }, 500);
    } else {
      ElMessage.error(res.message || "登录失败");
    }
  } catch (error: any) {
    console.error("登录错误:", error);
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
  overflow: hidden;
  max-width: 1200px;
  margin: 0 auto;

  .login-image {
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
        color: #666;
      }

      .client-count {
        background-color: rgba(255, 255, 255, 0.8);
        padding: 10px 20px;
        border-radius: 20px;
        display: inline-block;

        span {
          display: block;
          font-size: 14px;
          color: #666;
        }

        .count {
          font-size: 24px;
          font-weight: bold;
          color: #333;
        }
      }
    }
  }

  .login-box {
    flex: 0 0 40%;
    max-width: 450px;
    padding: 12px;
    margin: 10px auto;
    min-width: 0;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
    display: flex;
    flex-direction: column;

    .login-header {
      text-align: center;
      margin-bottom: 40px;

      h2 {
        margin: 0;
        color: #333;
        font-size: 26px;
        font-weight: 600;
      }
    }

    .login-form {
      flex: 1;
      display: flex;
      flex-direction: column;

      .el-form-item {
        margin-bottom: 20px;
      }

      .custom-input {
        border-radius: 8px;
        border-color: #e0e0e0;
        height: 50px;
        width: 100% !important;

        &:focus-within {
          border-color: #409eff;
          box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
        }
      }

      .role-tabs {
        margin-bottom: 30px;
      }

      .role-tab-group {
        width: 100%;

        .el-tabs__nav {
          width: 100%;
          display: flex;

          .el-tabs__item {
            flex: 1;
            text-align: center;
            height: 50px;
            line-height: 50px;
            font-size: 16px;
            border-radius: 8px;
            margin-right: 10px;
          }

          .el-tabs__item:last-child {
            margin-right: 0;
          }

          .el-tabs__item.is-active {
            background-color: #409eff;
            color: white;
          }
        }
      }

      .custom-select {
        border-radius: 8px;
        border-color: #e0e0e0;
        height: 50px;
        width: 100% !important;
      }

      .agreement {
        margin-bottom: 25px;
        font-size: 14px;

        .agreement-link {
          color: #409eff;
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }
      }

      .el-button--primary {
        background-color: #409eff;
        border-color: #409eff;
        height: 50px;
        border-radius: 8px;
        font-size: 16px;
        font-weight: 500;
      }
    }

    .login-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 25px;
      font-size: 14px;

      .forgot-password {
        color: #666;
      }

      .register-link {
        color: #666;

        .el-link {
          color: #409eff;
          margin-left: 5px;
        }
      }
    }
  }
}

@media (max-width: 992px) {
  .login-container {
    flex-direction: column;

    .login-image {
      width: 100%;
      height: 300px;
    }

    .login-box {
      width: 100%;
      box-shadow: none;
      padding: 30px;
    }
  }
}
</style>
