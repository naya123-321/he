<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <div class="back-home">
          <el-button link type="primary" @click="goHome">返回首页</el-button>
        </div>
        <h2>用户注册</h2>
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
            class="custom-input"
            @blur="validateUsernameExists"
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
            class="custom-input"
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
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号（可选）"
            prefix-icon="Phone"
            size="large"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="role" class="role-selector">
          <el-select
            v-model="registerForm.role"
            placeholder="请选择角色"
            size="large"
            class="custom-select"
          >
            <el-option label="宠主" :value="0"></el-option>
            <el-option label="服务人员" :value="1"></el-option>
            <el-option label="管理员" :value="2"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item prop="securityQuestion">
          <el-select
            v-model="registerForm.securityQuestion"
            placeholder="请选择密保问题"
            size="large"
            class="custom-select"
          >
            <el-option label="您最喜欢的宠物名字是？" value="您最喜欢的宠物名字是？" />
            <el-option label="您最喜欢的城市是？" value="您最喜欢的城市是？" />
            <el-option label="您小学班主任的名字是？" value="您小学班主任的名字是？" />
            <el-option label="您最喜欢的颜色是？" value="您最喜欢的颜色是？" />
          </el-select>
        </el-form-item>

        <el-form-item prop="securityAnswer">
          <el-input
            v-model="registerForm.securityAnswer"
            placeholder="请输入密保答案"
            prefix-icon="Key"
            size="large"
            class="custom-input"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item class="agreement">
          <el-checkbox v-model="registerForm.agree" class="agreement-checkbox">
            我已阅读并同意<a href="#" class="agreement-link">用户服务协议</a
            >和<a href="#" class="agreement-link">隐私政策</a>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleRegister"
            style="width: 100%"
            class="register-button"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <el-link type="default" class="forgot-password">忘记密码?</el-link>
          <div class="login-link">
            <span>已有账号？</span>
            <el-link type="primary" @click="$router.push('/login')">
              立即登录
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
  register,
  checkUsername as checkUsernameApi,
} from "@/api/user";

const router = useRouter();
const registerFormRef = ref<FormInstance>();

const loading = ref(false);

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  phone: "",
  role: null as number | null,
  securityQuestion: "",
  securityAnswer: "",
  agree: false,
});

// 自定义验证规则
import type { FormItemRule } from "element-plus";

const validateUsername = async (
  rule: FormItemRule,
  value: string,
  callback: (error?: Error) => void
) => {
  if (!value) {
    return callback(new Error("请输入用户名"));
  }
  if (value.length < 3 || value.length > 20) {
    return callback(new Error("用户名长度3-20个字符"));
  }
  callback();
};

const validatePassword = (
  rule: FormItemRule,
  value: string,
  callback: (error?: Error) => void
) => {
  if (!value) {
    return callback(new Error("请输入密码"));
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
  phone: [
    {
      validator: (rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
        if (!value) {
          // 手机号可选，为空时通过验证
          callback();
          return;
        }
        if (!/^1[3-9]\d{9}$/.test(value)) {
          return callback(new Error("手机号格式不正确"));
        }
        callback();
      },
      trigger: "blur"
    },
  ],
  role: [{ required: true, message: "请选择角色", trigger: "change" }],
  securityQuestion: [{ required: true, message: "请选择密保问题", trigger: "change" }],
  securityAnswer: [{ required: true, message: "请输入密保答案", trigger: "blur" }],
  agree: [
    {
      required: true,
      message: "请同意用户服务协议和隐私政策",
      trigger: "change",
    },
  ],
};

// 检查用户名是否已存在
const validateUsernameExists = async () => {
  if (!registerForm.username) return;

  try {
    const res = await checkUsernameApi(registerForm.username);
    if (res.data.data) {
      ElMessage.warning("用户名已存在");
    }
  } catch (error) {
    // 忽略错误
  }
};


const handleRegister = async () => {
  console.log("注册按钮被点击");
  if (!registerFormRef.value) {
    console.error("表单引用不存在");
    return;
  }

  try {
    const valid = await registerFormRef.value.validate();
    console.log("表单验证结果:", valid);
    if (!valid) {
      ElMessage.warning("请检查表单填写是否正确");
      return;
    }

    console.log("开始注册，表单数据:", registerForm);
    loading.value = true;

    const { confirmPassword, ...registerData } = registerForm;
    // 确认密码已在前端验证，无需传递给后端
    console.log("发送注册请求，数据:", registerData);
    const res = await register(registerData);
    console.log("注册响应:", res);

    // 根据request.ts的响应拦截器，res已经是处理后的数据（code === 200时才会返回）
    if (res.data) {
      ElMessage.success({
        message: "🎉 注册成功！正在跳转到登录页面...",
        duration: 2000,
        showClose: true,
      });
      // 延迟跳转，让用户看到成功提示
      setTimeout(() => {
        router.push("/login");
      }, 1500);
    } else {
      ElMessage.error(res.message || "注册失败");
    }
  } catch (error: any) {
    console.error("注册错误:", error);
    ElMessage.error(error.message || "注册失败");
  } finally {
    loading.value = false;
  }
};

const goHome = () => {
  router.push("/");
};
</script>

<style scoped lang="scss">
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100%;
  padding: 24px;
  background: #f5f7fa;

  .register-box {
    width: 100%;
    max-width: 520px;
    padding: 12px;
    margin: 10px auto;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
    display: flex;
    flex-direction: column;

    .register-header {
      position: relative;
      text-align: center;
      margin-bottom: 40px;

      .back-home {
        position: absolute;
        left: 0;
        top: -6px;
      }

      h2 {
        margin: 0;
        color: #333;
        font-size: 26px;
        font-weight: 600;
      }
    }

    .register-form {
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

    .register-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 25px;
      font-size: 14px;

      .forgot-password {
        color: #666;
      }

      .login-link {
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
  .register-container {
    .register-box {
      width: 100%;
      box-shadow: none;
      padding: 30px;
    }
  }
}
</style>
