<template>
  <div class="home-container">
    <!-- 导航栏 -->
    <el-header class="main-header">
      <div class="container">
        <div class="logo">与AI对话</div>
        <div class="nav-links">
          <span @click="scrollToSection('home')">首页</span>

          <span @click="scrollToSection('process')">服务流程</span>

          <span @click="scrollToSection('packages')">套餐介绍</span>

          <span @click="scrollToSection('contact')">联系我们</span>
        </div>
        <div class="auth-buttons">
          <!-- 未登录状态 -->
          <template v-if="!userStore.isLogin">
            <el-button type="text" @click="goToLogin">登录</el-button>
            <el-button type="primary" @click="goToRegister">注册</el-button>
          </template>
          <!-- 已登录状态 -->
          <template v-else>
            <el-dropdown @command="handleUserCommand" trigger="click">
              <div class="user-info-dropdown">
                <el-avatar 
                  :size="32" 
                  :src="userStore.userInfo?.avatar"
                  style="margin-right: 8px;"
                >
                  {{ userStore.userName?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <span class="username">{{ userStore.userName || '用户' }}</span>
                <el-icon style="margin-left: 4px;"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 主横幅 -->
    <section class="hero" id="home">
      <div class="container hero-content">
        <div class="hero-text">
          <h1>陪伴一生,温暖告别</h1>
          <p>专业的宠物殡葬服务,让爱延续</p>
          <el-button type="primary" size="large" class="learn-more-btn"
            >了解服务</el-button
          >
        </div>
        <div class="hero-image">
          <img src="@/assets/images/pet-hero.svg" alt="宠物陪伴" />
        </div>
      </div>
    </section>

    <!-- 服务特点 -->
    <section class="services">
      <div class="container">
        <h2 class="section-title">用心呵护,温暖送别</h2>
        <div class="services-grid">
          <el-card class="service-card">
            <div class="service-icon">
              <el-icon class="icon"><StarFilled /></el-icon>
            </div>
            <h3>专业服务</h3>
            <p>拥有专业团队和完善的设施,为您的爱宠提供最高规格的服务</p>
          </el-card>

          <el-card class="service-card">
            <div class="service-icon">
              <el-icon class="icon"><HomeFilled /></el-icon>
            </div>
            <h3>温馨环境</h3>
            <p>打造温馨舒适的告别空间,让您和爱宠度过最后平静的时刻</p>
          </el-card>

          <el-card class="service-card">
            <div class="service-icon">
              <el-icon class="icon"><Tools /></el-icon>
            </div>
            <h3>贴心陪伴</h3>
            <p>24小时贴心服务,在您最需要的时候给予温暖的陪伴和支持</p>
          </el-card>
        </div>
      </div>
    </section>

    <!-- 服务流程 -->
    <section class="process" id="process">
      <div class="container">
        <h2 class="section-title">服务流程</h2>
        <el-steps direction="vertical" class="process-steps">
          <template #icon="{ index }">
            <div class="step-icon">{{ index + 1 }}</div>
          </template>
          <el-step title="咨询预约">
            <div class="step-content">
              <p>电话沟通或在线预约,我们的专业顾问可为您详细介绍服务内容</p>
            </div>
          </el-step>
          <el-step title="上门接收">
            <div class="step-content">
              <p>到外省市可上门接收您的爱宠,全程温柔呵护</p>
            </div>
          </el-step>
          <el-step title="准备仪式">
            <div class="step-content">
              <p>根据您的意愿准备心怡的告别仪式,布置温馨的告别场景</p>
            </div>
          </el-step>
          <el-step title="告别仪式">
            <div class="step-content">
              <p>在庄重温馨的氛围中告别爱宠,让爱宠安详地前往汪星天堂</p>
            </div>
          </el-step>
          <el-step title="后续关怀">
            <div class="step-content">
              <p>提供骨灰寄存、纪念品定制等后续服务,让思念有处安放</p>
            </div>
          </el-step>
        </el-steps>
      </div>
    </section>

    <!-- 服务套餐 -->
    <section class="packages" id="packages">
      <div class="container">
        <h2 class="section-title">服务套餐</h2>
        <div class="packages-grid">
          <!-- 基础套餐 -->
          <el-card class="package-card basic-package">
            <div class="package-header">
              <h3>基础套餐</h3>
              <p class="package-price">¥1,888</p>
            </div>
            <ul class="package-features">
              <li>• 专业接送服务</li>
              <li>• 基础告别仪式</li>
              <li>• 火化处理</li>
              <li>• 骨灰盒</li>
              <li>• 纪念证书</li>
            </ul>
            <el-button
              type="primary"
              class="package-btn"
              @click="goToPackage('basic')"
              >选择套餐</el-button
            >
          </el-card>

          <!-- 标准套餐 -->
          <el-card class="package-card standard-package">
            <div class="package-header">
              <h3>标准套餐</h3>
              <p class="package-price">¥3,888</p>
            </div>
            <ul class="package-features">
              <li>• 专业接送服务</li>
              <li>• 标准告别仪式</li>
              <li>• 火化处理</li>
              <li>• 精美骨灰盒</li>
              <li>• 纪念证书</li>
              <li>• 宠物生前照片整理</li>
              <li>• 在线纪念堂</li>
            </ul>
            <el-button
              type="primary"
              class="package-btn"
              @click="goToPackage('standard')"
              >选择套餐</el-button
            >
          </el-card>

          <!-- 尊享套餐 -->
          <el-card class="package-card premium-package">
            <div class="package-header">
              <h3>尊享套餐</h3>
              <p class="package-price">¥6,888</p>
            </div>
            <ul class="package-features">
              <li>• VIP专车接送</li>
              <li>• 豪华告别仪式</li>
              <li>• 火化处理</li>
              <li>• 定制骨灰盒</li>
              <li>• 纪念证书</li>
              <li>• 专业摄影服务</li>
              <li>• 在线纪念堂</li>
              <li>• 定制纪念品</li>
              <li>• 一年免费骨灰寄存</li>
            </ul>
            <el-button
              type="primary"
              class="package-btn"
              @click="goToPackage('premium')"
              >选择套餐</el-button
            >
          </el-card>
        </div>
      </div>
    </section>

    <!-- 联系和咨询 -->
    <section class="contact" id="contact">
      <div class="container contact-content">
        <div class="contact-info">
          <h2 class="section-title">联系我们</h2>
          <p>24小时服务热线: 400-888-8888</p>
          <p>地址: 北京市朝阳区阳光大街1212号</p>
          <p>邮箱: contact@petfuneral.com</p>
        </div>

        <div class="contact-form">
          <h2 class="section-title">在线咨询</h2>
          <el-form
            ref="consultFormRef"
            :model="contactForm"
            :rules="contactRules"
          >
            <el-form-item>
              <el-input v-model="contactForm.name" placeholder="您的姓名" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="contactForm.phone" placeholder="联系电话" />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="contactForm.message"
                type="textarea"
                placeholder="请描述您的需求"
                :rows="4"
              />
            </el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              style="width: 100%"
              @click="submitConsultation(consultFormRef)"
              >提交咨询</el-button
            >
          </el-form>
        </div>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <p>© 2022 宠物殡葬服务系统 版权所有 | 京ICP备12345678号</p>
        <p>宠物殡葬服务 | 纪念册制作 | 骨灰寄存</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  StarFilled,
  HomeFilled,
  Tools,
  Phone,
  Message,
  MapLocation,
  User,
  SwitchButton,
  ArrowDown,
} from "@element-plus/icons-vue";
import { useUserStore } from "@/store/user";
import { contactApi } from "@/api/contact";

const router = useRouter();
const userStore = useUserStore();

// 初始化用户信息
onMounted(() => {
  // 如果已登录，初始化用户信息
  if (userStore.isLogin && !userStore.userInfo) {
    userStore.initUserInfo();
  }
});

// 处理用户下拉菜单命令
const handleUserCommand = (command: string) => {
  if (command === "profile") {
    router.push("/profile");
  } else if (command === "logout") {
    ElMessageBox.confirm("确定要退出登录吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    })
      .then(() => {
        userStore.logout();
        ElMessage.success("已退出登录");
      })
      .catch(() => {
        // 取消退出
      });
  }
};

const goToLogin = () => {
  console.log("点击登录按钮，准备跳转到 /login");
  console.log("当前路由:", router.currentRoute.value.path);
  try {
    router.push("/login").then(() => {
      console.log("router.push 成功，当前路由:", router.currentRoute.value.path);
      // 强制刷新以确保页面更新
      if (router.currentRoute.value.path !== "/login") {
        console.warn("路由路径不匹配，强制跳转");
        window.location.href = "/login";
      }
    }).catch((err) => {
      console.error("路由跳转失败:", err);
      ElMessage.error("跳转失败，请刷新页面重试");
      // 如果路由跳转失败，使用 window.location 作为备选方案
      window.location.href = "/login";
    });
  } catch (error) {
    console.error("跳转异常:", error);
    ElMessage.error("跳转异常，请刷新页面重试");
    // 如果路由跳转失败，使用 window.location 作为备选方案
    window.location.href = "/login";
  }
};

const goToRegister = () => {
  console.log("点击注册按钮，准备跳转到 /register");
  console.log("当前路由:", router.currentRoute.value.path);
  try {
    router.push("/register").then(() => {
      console.log("router.push 成功，当前路由:", router.currentRoute.value.path);
      // 强制刷新以确保页面更新
      if (router.currentRoute.value.path !== "/register") {
        console.warn("路由路径不匹配，强制跳转");
        window.location.href = "/register";
      }
    }).catch((err) => {
      console.error("路由跳转失败:", err);
      ElMessage.error("跳转失败，请刷新页面重试");
      // 如果路由跳转失败，使用 window.location 作为备选方案
      window.location.href = "/register";
    });
  } catch (error) {
    console.error("跳转异常:", error);
    ElMessage.error("跳转异常，请刷新页面重试");
    // 如果路由跳转失败，使用 window.location 作为备选方案
    window.location.href = "/register";
  }
};

// 表单状态
const contactForm = ref({
  name: "",
  phone: "",
  message: "",
});

// 表单验证规则
const contactRules = ref({
  name: [{ required: true, message: "请输入您的姓名", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入联系电话", trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号码",
      trigger: "blur",
    },
  ],
  message: [
    { required: true, message: "请输入您的需求", trigger: "blur" },
    { min: 10, message: "需求描述至少10个字符", trigger: "blur" },
  ],
});

// 提交咨询表单
const submitConsultation = async (formEl: any) => {
  if (!formEl) return;
  try {
    await formEl.validate();
    // 调用API提交表单
    await contactApi.submitConsultation(contactForm.value);
    ElMessage.success("咨询提交成功，我们将尽快与您联系");
    contactForm.value = {
      name: "",
      phone: "",
      message: "",
    };
    formEl.resetFields();
  } catch (error) {
    console.error("表单提交失败", error);
  }
};

// 导航到服务页面
const goToService = () => {
  router.push("/services");
};

// 导航到套餐页面
const scrollToSection = (sectionId: string) => {
  const element = document.getElementById(sectionId);
  if (element) {
    element.scrollIntoView({ behavior: "smooth" });
  }
};

// 导航到套餐页面（宠主端：跳转到“服务套餐”页，避免进入旧的预约/创建订单界面）
const goToPackage = (_packageType: string) => {
  // 检查用户是否已登录
  const token = sessionStorage.getItem("token");
  if (!token) {
    // 未登录，跳转到登录页
    ElMessage.warning("请先登录后再选择套餐");
    router.push({ name: "Login" });
    return;
  }

  // 已登录：宠主端直接跳到服务套餐页
  if (userStore.userRole === 0) {
    router.push("/pet-owner/service-packages");
    return;
  }

  // 其他角色不需要“选择套餐”，跳回各自首页/工作台
  if (userStore.userRole === 1) {
    ElMessage.info("当前角色无需选择套餐");
    router.push("/service-provider/appointment-list");
    return;
  }
  if (userStore.userRole === 2) {
    ElMessage.info("当前角色无需选择套餐");
    router.push("/admin/dashboard");
    return;
  }

  ElMessage.info("当前角色无需选择套餐");
};
</script>

<style scoped lang="scss">
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$light-bg: #f5f7fa;
$white: #ffffff;
$text-primary: #303133;
$text-secondary: #606266;
$text-tertiary: #909399;
$border-color: #e4e7ed;

.home-container {
  font-family: "Helvetica Neue", Arial, sans-serif;
}

/* 导航栏样式 */
.main-header {
  background-color: $white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 0;

  .container {
    max-width: 1200px;
    margin: 0 auto;
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    height: 60px;
  }

  .logo {
    font-size: 20px;
    font-weight: bold;
    color: $primary-color;
  }

  .nav-links {
    display: flex;
    gap: 30px;
    margin-left: 50px;

    span {
      color: $text-secondary;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: $primary-color;
      }
    }
  }

  .auth-buttons {
    display: flex;
    gap: 10px;
    z-index: 10;
    position: relative;
    align-items: center;
    
    .el-button {
      cursor: pointer;
      pointer-events: auto;
    }

    .user-info-dropdown {
      display: flex;
      align-items: center;
      cursor: pointer;
      padding: 4px 12px;
      border-radius: 4px;
      transition: background-color 0.3s;

      &:hover {
        background-color: rgba(64, 158, 255, 0.1);
      }

      .username {
        font-size: 14px;
        color: #303133;
        margin-right: 4px;
      }
    }
  }
}

/* 主横幅样式 */
.hero {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4eaf5 100%);
  padding: 60px 0;

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .hero-content {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .hero-text {
      max-width: 500px;

      h1 {
        font-size: 42px;
        color: $text-primary;
        margin-bottom: 20px;
        line-height: 1.2;
      }

      p {
        font-size: 18px;
        color: $text-secondary;
        margin-bottom: 30px;
      }
    }

    .hero-image {
      width: 500px;
      height: 300px;
      border-radius: 8px;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
  }
}

/* 服务特点样式 */
.services {
  padding: 80px 0;

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .section-title {
    text-align: center;
    font-size: 32px;
    color: $text-primary;
    margin-bottom: 50px;
    position: relative;

    &::after {
      content: "";
      display: block;
      width: 60px;
      height: 4px;
      background-color: $primary-color;
      margin: 10px auto 0;
      border-radius: 2px;
    }
  }

  .services-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 30px;
  }

  .service-card {
    border: none;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
    transition: transform 0.3s, box-shadow 0.3s;
    padding: 30px;
    text-align: center;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
    }

    .service-icon {
      width: 60px;
      height: 60px;
      background-color: rgba(64, 158, 255, 0.1);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 20px;

      .icon {
        font-size: 24px;
        color: $primary-color;
      }
    }

    h3 {
      font-size: 20px;
      margin-bottom: 15px;
      color: $text-primary;
    }

    p {
      color: $text-secondary;
      line-height: 1.6;
    }
  }
}

/* 服务流程样式 */
.process {
  background-color: $light-bg;
  padding: 80px 0;

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .section-title {
    text-align: center;
    margin-bottom: 50px;
  }

  .process-steps {
    max-width: 800px;
    margin: 0 auto;

    .el-step {
      margin-bottom: 30px;
      display: flex;
      align-items: flex-start;

      .step-icon {
        width: 40px;
        height: 40px;
        background-color: #409eff;
        color: white;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: bold;
        margin-right: 20px;
        flex-shrink: 0;
      }

      .el-step__content {
        margin-left: 20px;
      }

      .el-step__title {
        font-size: 18px;
        color: $text-primary;
        margin-bottom: 5px;
      }

      .el-step__description {
        color: $text-secondary;
        font-size: 14px;
        line-height: 1.6;
      }

      .step-content {
        display: flex;
        align-items: center;
        gap: 20px;
      }

      .step-image {
        width: 80px;
        height: 80px;
        object-fit: contain;
        flex-shrink: 0;
      }
    }
  }
}

/* 服务套餐样式 */
.packages {
  padding: 80px 0;

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .packages-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 30px;
  }

  .package-card {
    border: none;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
    transition: transform 0.3s, box-shadow 0.3s;
    overflow: hidden;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
    }

    .package-header {
      padding: 25px 20px;
      text-align: center;

      h3 {
        font-size: 22px;
        margin-bottom: 10px;
      }

      .package-price {
        font-size: 28px;
        font-weight: bold;
        color: $primary-color;
      }
    }

    .package-features {
      padding: 0 20px 20px;
      list-style: none;

      li {
        padding: 10px 0;
        border-bottom: 1px solid $border-color;
        color: $text-secondary;
        display: flex;
        align-items: center;

        &::before {
          content: "✓";
          color: $success-color;
          margin-right: 10px;
        }
      }
    }

    .package-btn {
      width: 80%;
      margin: 0 auto 20px;
      display: block;
    }
  }

  .basic-package {
    .package-header {
      background-color: #f0f7ff;
    }
  }

  .standard-package {
    .package-header {
      background-color: #f6ffed;
    }

    .package-price {
      color: $success-color;
    }
  }

  .premium-package {
    .package-header {
      background-color: #fff7e8;
    }

    .package-price {
      color: $warning-color;
    }
  }
}

/* 联系部分样式 */
.contact {
  background-color: $light-bg;
  padding: 80px 0;

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .contact-content {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 40px;
    background-color: $white;
    padding: 40px;
    border-radius: 8px;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  }

  .contact-info {
    .contact-item {
      display: flex;
      align-items: center;
      margin-bottom: 20px;

      .el-icon {
        margin-right: 10px;
        color: $primary-color;
      }
    }
  }

  .contact-form {
    .el-form-item {
      margin-bottom: 20px;
    }

    .submit-btn {
      width: 100%;
      padding: 12px;
    }
  }
}

/* 页脚样式 */
.footer {
  background-color: #303133;
  color: $white;
  padding: 40px 0;
  text-align: center;

  p {
    margin-bottom: 10px;
    opacity: 0.8;
  }
}

/* 响应式样式 */
@media (max-width: 768px) {
  .hero-content {
    flex-direction: column;
    text-align: center;

    .hero-image {
      margin-top: 30px;
      width: 100%;
    }
  }

  .services-grid,
  .packages-grid,
  .contact-content {
    grid-template-columns: 1fr;
  }

  .main-header .nav-links {
    display: none;
  }

  .section-title {
    font-size: 28px;
  }

  .hero-text h1 {
    font-size: 32px;
  }
}
</style>
