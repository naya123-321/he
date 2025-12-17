<template>
  <div class="home-container">
    <!-- 导航栏 -->
    <el-header class="main-header">
      <div class="container">
        <div class="logo">宠物纪念馆</div>
        <div class="nav-links">
          <span @click="scrollToSection('home')">首页</span>

          <span @click="scrollToSection('process')">服务流程</span>

          <span @click="scrollToSection('packages')">套餐介绍</span>
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
          <el-button
            type="primary"
            size="large"
            class="learn-more-btn"
            @click="scrollToSection('packages')"
          >
            查看套餐
          </el-button>
        </div>
        <div class="hero-media">
          <el-carousel
            :interval="4500"
            height="360px"
            indicator-position="outside"
            arrow="hover"
            class="hero-carousel"
          >
            <el-carousel-item v-for="(s, idx) in heroSlides" :key="idx">
              <div class="hero-slide" :class="`slide-${idx + 1}`">
                <div class="slide-meta">
                  <div class="slide-badge">{{ s.badge }}</div>
                  <div class="slide-name" :title="s.title">{{ s.title }}</div>
                </div>
                <div class="slide-media">
                  <img class="slide-img" :src="s.image" :alt="s.title" />
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
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
        <div class="process-grid">
          <div class="process-card">
            <div class="pc-top">
              <div class="pc-index">1</div>
              <div class="pc-icon">
                <el-icon><ChatLineRound /></el-icon>
              </div>
            </div>
            <div class="pc-title">咨询预约</div>
            <div class="pc-desc">在线沟通需求，确认服务内容与时间安排</div>
          </div>
          <div class="process-card">
            <div class="pc-top">
              <div class="pc-index">2</div>
              <div class="pc-icon">
                <el-icon><Van /></el-icon>
              </div>
            </div>
            <div class="pc-title">上门接收</div>
            <div class="pc-desc">专车接送，全程温柔呵护与尊重</div>
          </div>
          <div class="process-card">
            <div class="pc-top">
              <div class="pc-index">3</div>
              <div class="pc-icon">
                <el-icon><MagicStick /></el-icon>
              </div>
            </div>
            <div class="pc-title">准备仪式</div>
            <div class="pc-desc">根据意愿布置告别场景，细节更安心</div>
          </div>
          <div class="process-card">
            <div class="pc-top">
              <div class="pc-index">4</div>
              <div class="pc-icon">
                <el-icon><Sunny /></el-icon>
              </div>
            </div>
            <div class="pc-title">告别仪式</div>
            <div class="pc-desc">在温馨庄重的氛围里，好好说再见</div>
          </div>
          <div class="process-card">
            <div class="pc-top">
              <div class="pc-index">5</div>
              <div class="pc-icon">
                <el-icon><Present /></el-icon>
              </div>
            </div>
            <div class="pc-title">后续关怀</div>
            <div class="pc-desc">骨灰寄存、纪念册制作，让思念有处安放</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 服务套餐 -->
    <section class="packages" id="packages">
      <div class="container">
        <h2 class="section-title">服务套餐</h2>
        <div class="packages-grid">
          <el-card
            v-for="(pkg, idx) in homePackages"
            :key="pkg.id || idx"
            class="package-card"
            :class="idx === 0 ? 'basic-package' : idx === 1 ? 'standard-package' : 'premium-package'"
          >
            <div class="package-header">
              <h3>{{ pkg.name }}</h3>
              <p class="package-price">¥{{ formatPrice(pkg.price) }}</p>
            </div>
            <ul class="package-features">
              <li v-for="(f, i) in parseProcessToFeatures(pkg.process).slice(0, 8)" :key="i">
                • {{ f }}
              </li>
            </ul>
            <el-button type="primary" class="package-btn" @click="goToPackage('')">选择套餐</el-button>
          </el-card>
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
  ChatLineRound,
  Van,
  MagicStick,
  Sunny,
  Present,
  User,
  SwitchButton,
  ArrowDown,
} from "@element-plus/icons-vue";
import { useUserStore } from "@/store/user";
import { memorialApi, type TemplateVO, templateApi } from "@/api/memorial";
import { serviceTypeApi, type ServiceTypeVO } from "@/api/order";

const router = useRouter();
const userStore = useUserStore();

const heroSlides = ref<Array<{ badge: string; title: string; image: string }>>([
  { badge: "纪念册模板", title: "精选模板 · 让思念更温柔", image: new URL("@/assets/images/pet-hero.svg", import.meta.url).toString() },
  { badge: "纪念册模板", title: "多图展示 · 细节更动人", image: new URL("@/assets/images/pet-hero.svg", import.meta.url).toString() },
  { badge: "纪念册模板", title: "风格多样 · 总有一款适合TA", image: new URL("@/assets/images/pet-hero.svg", import.meta.url).toString() },
]);

const homePackages = ref<ServiceTypeVO[]>([]);

// 初始化用户信息
onMounted(() => {
  // 如果已登录，初始化用户信息
  if (userStore.isLogin && !userStore.userInfo) {
    userStore.initUserInfo();
  }

  loadHomeTemplates();
  loadHomePackages();
});

function getTemplateImages(template: any): string[] {
  if (!template) return [];
  const out: string[] = [];
  const tryParse = (val: any) => {
    if (!val) return;
    if (Array.isArray(val)) {
      val.forEach((x) => x && out.push(String(x)));
      return;
    }
    if (typeof val === "string") {
      const s = val.trim();
      if (!s) return;
      try {
        const parsed = JSON.parse(s);
        if (Array.isArray(parsed)) {
          parsed.forEach((x) => x && out.push(String(x)));
          return;
        }
        if (parsed?.templateImages && Array.isArray(parsed.templateImages)) {
          parsed.templateImages.forEach((x: any) => x && out.push(String(x)));
          return;
        }
      } catch {
        // ignore
      }
      // 逗号分隔兜底
      if (s.includes(",")) {
        s.split(",").map((x) => x.trim()).filter(Boolean).forEach((x) => out.push(x));
      }
    }
  };
  tryParse(template.templateImages);
  tryParse(template.styleConfig);
  if (template.previewImage) out.push(String(template.previewImage));
  return Array.from(new Set(out)).filter(Boolean);
}

function getTemplateCover(template: any): string | null {
  const imgs = getTemplateImages(template);
  return imgs.length > 0 ? imgs[0] : null;
}

async function loadHomeTemplates() {
  try {
    // 公开接口：启用模板
    const res = await memorialApi.getTemplates();
    const list = (res.data || []) as TemplateVO[];

    const isBad = (t: any, cover: string) => {
      const name = String(t?.name || "").trim();
      const cat = String(t?.categoryText || t?.category || "").trim();
      const url = String(cover || "").toLowerCase();
      // 跳过：测试模板 / “其他”分类 / 明显水印图
      if (cat === "其他") return true;
      if (name.includes("测试") || name.toLowerCase().includes("test")) return true;
      if (url.includes("redocn") || url.includes("watermark")) return true;
      return false;
    };

    const candidates = list
      .map((t) => ({ t, cover: getTemplateCover(t) }))
      .filter((x) => !!x.cover)
      .filter((x) => !isBad(x.t, x.cover!));

    const picked = candidates.slice(0, 3);
    if (picked.length > 0) {
      heroSlides.value = picked.map((x) => ({
        badge: x.t.categoryText || "纪念册模板",
        title: x.t.name || "纪念册模板",
        image: x.cover!,
      }));
    }
  } catch {
    // 忽略：无模板/接口失败时保持默认占位图
  }
}

const parseProcessToFeatures = (process?: string): string[] => {
  if (!process) return [];
  return process
    .split(/[\n;]/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
};

const formatPrice = (price: any) => {
  const n = Number(price);
  if (Number.isFinite(n)) return n.toFixed(0);
  return "0";
};

async function loadHomePackages() {
  try {
    const res = await serviceTypeApi.getEnabled();
    const list = (res.data || []) as ServiceTypeVO[];
    // 取前三个（按价格从低到高）
    homePackages.value = [...list]
      .filter((x) => x && x.status === 1)
      .sort((a, b) => Number(a.price || 0) - Number(b.price || 0))
      .slice(0, 3);
  } catch {
    homePackages.value = [];
  }
}

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
    // 未登录：访客可先填写宠物信息，进入公开套餐页并获取推荐
    router.push("/visitor/pet-info");
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
// 温暖柔和色系
$primary-color: #e07a5f; // 柔和珊瑚橙
$success-color: #81b29a; // 温柔绿
$warning-color: #f2cc8f; // 奶油杏
$danger-color: #d66b6b;
$light-bg: #fff7f2; // 暖白
$white: #ffffff;
$text-primary: #3d2c23; // 暖深棕
$text-secondary: #6e5a4f;
$text-tertiary: #9a857a;
$border-color: #f1e3d8;

.section-title {
  text-align: center;
  font-size: 32px;
  color: $text-primary;
  margin-bottom: 34px;
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

.home-container {
  font-family: "Helvetica Neue", Arial, sans-serif;
  background: $light-bg;
}

/* 导航栏样式 */
.main-header {
  background-color: $white;
  box-shadow: 0 2px 14px rgba(61, 44, 35, 0.06);
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
  background: radial-gradient(900px 500px at 75% 35%, rgba(224, 122, 95, 0.16) 0%, rgba(255, 247, 242, 0) 60%),
    linear-gradient(135deg, #fff7f2 0%, #fff 100%);
  padding: 72px 0;

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

      .learn-more-btn {
        background: $primary-color;
        border-color: $primary-color;
        border-radius: 10px;
        height: 44px;
        padding: 0 22px;

        &:hover {
          opacity: 0.92;
        }
      }
    }

    .hero-media {
      width: 580px;
      max-width: 580px;
    }

    .hero-carousel {
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 14px 34px rgba(61, 44, 35, 0.12);
      border: 1px solid rgba(241, 227, 216, 0.9);
      background: #fff;
    }

    .hero-slide {
      height: 360px;
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: stretch;
      justify-content: flex-start;
      padding: 18px;
      overflow: hidden;

      &.slide-1 {
        background: radial-gradient(480px 260px at 30% 30%, rgba(242, 204, 143, 0.35) 0%, rgba(255, 255, 255, 0) 60%),
          linear-gradient(135deg, rgba(224, 122, 95, 0.12) 0%, rgba(255, 247, 242, 0.2) 60%, rgba(255, 255, 255, 1) 100%);
      }
      &.slide-2 {
        background: radial-gradient(480px 260px at 30% 30%, rgba(129, 178, 154, 0.28) 0%, rgba(255, 255, 255, 0) 60%),
          linear-gradient(135deg, rgba(242, 204, 143, 0.16) 0%, rgba(255, 247, 242, 0.2) 60%, rgba(255, 255, 255, 1) 100%);
      }
      &.slide-3 {
        background: radial-gradient(480px 260px at 30% 30%, rgba(224, 122, 95, 0.22) 0%, rgba(255, 255, 255, 0) 60%),
          linear-gradient(135deg, rgba(129, 178, 154, 0.12) 0%, rgba(255, 247, 242, 0.2) 60%, rgba(255, 255, 255, 1) 100%);
      }

      .slide-meta {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 12px;
      }

      .slide-badge {
        padding: 6px 10px;
        border-radius: 999px;
        font-size: 12px;
        font-weight: 700;
        color: $text-primary;
        background: rgba(255, 255, 255, 0.78);
        border: 1px solid rgba(241, 227, 216, 0.95);
        backdrop-filter: blur(8px);
        flex-shrink: 0;
      }

      .slide-name {
        font-size: 15px;
        font-weight: 700;
        color: $text-primary;
        line-height: 1.2;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .slide-media {
        flex: 1;
        border-radius: 14px;
        background: rgba(255, 255, 255, 0.82);
        border: 1px solid rgba(241, 227, 216, 0.95);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 10px;
      }

      .slide-img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
        opacity: 0.98;
        filter: drop-shadow(0 10px 18px rgba(61, 44, 35, 0.12));
        user-select: none;
        pointer-events: none;
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
    box-shadow: 0 8px 22px rgba(61, 44, 35, 0.08);
    transition: transform 0.3s, box-shadow 0.3s;
    padding: 30px;
    text-align: center;
    border-radius: 14px;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 14px 30px rgba(61, 44, 35, 0.12);
    }

    .service-icon {
      width: 60px;
      height: 60px;
      background-color: rgba(224, 122, 95, 0.12);
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
  background-color: rgba(242, 204, 143, 0.12);
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

  .process-grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 18px;
  }

  .process-card {
    background: rgba(255, 255, 255, 0.85);
    border: 1px solid rgba(241, 227, 216, 0.95);
    border-radius: 16px;
    padding: 18px 16px;
    box-shadow: 0 10px 22px rgba(61, 44, 35, 0.08);
    transition: transform 0.25s ease, box-shadow 0.25s ease;
    position: relative;
    overflow: hidden;

    &::before {
      content: "";
      position: absolute;
      inset: 0;
      background: radial-gradient(300px 140px at 20% 10%, rgba(224, 122, 95, 0.12) 0%, rgba(255, 255, 255, 0) 60%);
      pointer-events: none;
    }

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 16px 30px rgba(61, 44, 35, 0.12);
    }

    .pc-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      position: relative;
      z-index: 1;
      margin-bottom: 10px;
    }

    .pc-index {
      width: 34px;
      height: 34px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      color: $text-primary;
      background: rgba(242, 204, 143, 0.35);
      border: 1px solid rgba(241, 227, 216, 0.95);
    }

    .pc-icon {
      width: 34px;
      height: 34px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(224, 122, 95, 0.12);
      border: 1px solid rgba(241, 227, 216, 0.95);

      :deep(.el-icon) {
        color: $primary-color;
        font-size: 18px;
      }
    }

    .pc-title {
      position: relative;
      z-index: 1;
      font-size: 16px;
      font-weight: 700;
      color: $text-primary;
      margin-bottom: 6px;
    }

    .pc-desc {
      position: relative;
      z-index: 1;
      color: $text-secondary;
      font-size: 13px;
      line-height: 1.6;
      min-height: 42px;
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
    box-shadow: 0 8px 22px rgba(61, 44, 35, 0.08);
    transition: transform 0.3s, box-shadow 0.3s;
    overflow: hidden;
    border-radius: 14px;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 14px 30px rgba(61, 44, 35, 0.12);
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
      border-radius: 10px;
      height: 40px;
    }
  }

  .basic-package {
    .package-header {
      background-color: rgba(242, 204, 143, 0.25);
    }
  }

  .standard-package {
    .package-header {
      background-color: rgba(129, 178, 154, 0.18);
    }

    .package-price {
      color: $success-color;
    }
  }

  .premium-package {
    .package-header {
      background-color: rgba(224, 122, 95, 0.12);
    }

    .package-price {
      color: $warning-color;
    }
  }
}

/* 页脚样式 */
.footer {
  background-color: #3d2c23;
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

    .hero-media {
      margin-top: 30px;
      width: 100%;
    }
  }

  .services-grid,
  .packages-grid {
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

  .process .process-grid {
    grid-template-columns: 1fr;
  }
}
</style>
