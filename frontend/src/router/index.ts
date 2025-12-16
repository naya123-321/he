// @ts-ignore
import { createRouter, createWebHistory } from "vue-router";

import HomeView from "@/views/HomeView.vue";

const routes = [
  {
    path: "/",
    name: "Root",
    component: HomeView,
    meta: { requiresAuth: false },
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/auth/LoginView.vue"),
    meta: { requiresAuth: false },
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/views/auth/RegisterView.vue"),
    meta: { requiresAuth: false },
  },
  {
    path: "/forgot-password",
    name: "ForgotPassword",
    component: () => import("@/views/auth/ForgotPasswordView.vue"),
    meta: { requiresAuth: false },
  },
  {
    path: "/profile",
    name: "Profile",
    component: () => import("@/views/user/ProfileView.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/home",
    name: "Home",
    component: () => import("@/views/HomeView.vue"),
    meta: { requiresAuth: true },
  },

  // 宠主端路由 - 角色：0
  {
    path: "/pet-owner",
    component: () => import("@/layouts/BasicLayout.vue"),
    meta: { requiresAuth: true, requiredRole: [0] },
    children: [
      {
        path: "service-packages",
        name: "ServicePackages",
        component: () => import("@/views/pet-owner/ServicePackages.vue"),
      },
      {
        path: "book-service",
        name: "BookService",
        component: () => import("@/views/pet-owner/BookService.vue"),
      },
      {
        path: "service-progress",
        name: "ServiceProgress",
        component: () => import(/* @vite-ignore */ "@/views/pet-owner/ServiceProgress.vue"),
      },
      {
        path: "service-feedback",
        name: "PetOwnerServiceFeedback",
        component: () => import("@/views/pet-owner/ServiceFeedback.vue"),
      },
      {
        path: "grief-resources",
        name: "GriefResources",
        component: () => import("@/views/pet-owner/GriefResources.vue"),
      },
      {
        path: "urn-storage",
        name: "PetOwnerUrnStorage",
        component: () => import("@/views/pet-owner/UrnStorage.vue"),
      },
    ],
  },

  // 服务人员端路由 - 角色：1
  {
    path: "/service-provider",
    component: () => import("@/layouts/BasicLayout.vue"),
    meta: { requiresAuth: true, requiredRole: [1] },
    children: [
      {
        path: "appointment-list",
        name: "AppointmentList",
        component: () => import("@/views/service-provider/AppointmentList.vue"),
      },
      {
        path: "schedule",
        name: "Schedule",
        component: () => import("@/views/service-provider/Schedule.vue"),
      },
      {
        path: "memorial-list",
        name: "ServiceProviderMemorialList",
        component: () => import("@/views/service-provider/MemorialList.vue"),
      },
      {
        path: "memorial-design/:id",
        name: "MemorialDesign",
        component: () => import("@/views/service-provider/MemorialDesign.vue"),
      },
      {
        path: "service-feedback",
        name: "ServiceProviderServiceFeedback",
        component: () => import("@/views/service-provider/ServiceFeedback.vue"),
      },
      {
        path: "my-services",
        name: "MyServices",
        component: () => import("@/views/service-provider/MyServices.vue"),
      },
      {
        path: "template-management",
        name: "ServiceProviderTemplateManagement",
        component: () => import("@/views/admin/TemplateManagement.vue"),
      },
      {
        path: "urn-storage",
        name: "ServiceProviderUrnStorage",
        component: () => import("@/views/service-provider/UrnStorageManagement.vue"),
      },
    ],
  },

  // 管理员端路由 - 角色：2
  {
    path: "/admin",
    component: () => import("@/layouts/BasicLayout.vue"),
    meta: { requiresAuth: true, requiredRole: [2] },
    children: [
      {
        path: "dashboard",
        name: "AdminDashboard",
        component: () => import("@/views/admin/Dashboard.vue"),
      },
      {
        path: "service-management",
        name: "ServiceManagement",
        component: () => import("@/views/admin/ServiceManagement.vue"),
      },
      {
        path: "memorial-review",
        name: "MemorialReview",
        component: () => import("@/views/admin/MemorialReview.vue"),
      },
      {
        path: "satisfaction-stats",
        name: "SatisfactionStats",
        component: () => import("@/views/admin/SatisfactionStats.vue"),
      },
      {
        path: "special-requests",
        name: "SpecialRequests",
        component: () => import("@/views/admin/SpecialRequests.vue"),
      },
      {
        path: "pending-tasks",
        name: "PendingTasks",
        component: () => import("@/views/admin/PendingTasks.vue"),
      },
      {
        path: "user-management",
        name: "UserManagement",
        component: () => import("@/views/admin/UserManagement.vue"),
      },
      {
        path: "template-management",
        name: "TemplateManagement",
        component: () => import("@/views/admin/TemplateManagement.vue"),
      },
      {
        path: "grief-resource",
        name: "GriefResourceManagement",
        component: () => import("@/views/admin/GriefResourceManagement.vue"),
      },
      {
        path: "urn-storage",
        name: "UrnStorageManagement",
        component: () => import("@/views/admin/UrnStorageManagement.vue"),
      },
    ],
  },

  // 公共订单管理路由
  {
    path: "/order",
    component: () => import("@/layouts/BasicLayout.vue"),
    meta: { requiresAuth: true },
    children: [
      {
        path: "create",
        name: "CreateOrder",
        component: () => import("@/views/order/CreateOrder.vue"),
      },
      {
        path: "list",
        name: "OrderList",
        component: () => import("@/views/order/OrderList.vue"),
      },
      {
        path: "detail/:id",
        name: "OrderDetail",
        component: () => import("@/views/order/OrderDetail.vue"),
      },
    ],
  },

  // 公共纪念册管理路由
  {
    path: "/memorial",
    component: () => import("@/layouts/BasicLayout.vue"),
    meta: { requiresAuth: true },
    children: [
      {
        path: "list",
        name: "MemorialList",
        component: () => import("@/views/memorial/MemorialList.vue"),
      },
      {
        path: "create",
        name: "CreateMemorial",
        component: () => import("@/views/memorial/CreateMemorial.vue"),
      },
      {
        path: "edit/:id",
        name: "EditMemorial",
        component: () => import("@/views/memorial/EditMemorial.vue"),
      },
      {
        path: "templates",
        name: "TemplateList",
        component: () => import("@/views/memorial/TemplateList.vue"),
      },
      {
        path: "preview/:id",
        name: "MemorialPreview",
        component: () => import("@/views/memorial/MemorialPreview.vue"),
      },
      {
        path: "annotate/:id",
        name: "MemorialAnnotate",
        component: () => import("@/views/memorial/MemorialAnnotate.vue"),
      },
    ],
  },

  // 公开分享（不需要登录）
  {
    path: "/memorial/share/:token",
    component: () => import("@/layouts/BasicLayout.vue"),
    meta: { requiresAuth: false },
    children: [
      {
        path: "",
        name: "MemorialShare",
        component: () => import("@/views/memorial/MemorialShare.vue"),
      },
    ],
  },

  // 其他路由...
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 路由守卫
router.beforeEach((to: any, from: any, next: any) => {
  console.log("路由守卫触发:", { from: from.path, to: to.path });
  // 使用sessionStorage，让每个标签页独立存储
  const token = sessionStorage.getItem("token");
  const requiresAuth = to.meta.requiresAuth;
  const requiredRole = to.meta.requiredRole;

  // 从session存储获取用户信息
  const userInfoStr = sessionStorage.getItem("userInfo");
  const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null;

  console.log("路由守卫状态:", { token: !!token, requiresAuth, requiredRole, userInfo });

  if (requiresAuth && !token) {
    // 需要登录但未登录，跳转到登录页
    console.log("需要登录但未登录，跳转到登录页");
    next("/login");
  } else if (
    !requiresAuth &&
    token &&
    (to.path === "/login" || to.path === "/register")
  ) {
    // 已登录但访问登录/注册页，跳转到首页
    console.log("已登录但访问登录/注册页，跳转到首页");
    next("/home");
  } else if (requiresAuth && token && requiredRole && userInfo) {
    // 需要特定角色且已登录，检查角色权限
    if (requiredRole.includes(userInfo.role)) {
      // 角色匹配，允许访问
      console.log("角色匹配，允许访问");
      next();
    } else {
      // 角色不匹配，跳转到无权限页面或首页
      console.log("角色不匹配，跳转到首页");
      next("/home");
    }
  } else {
    console.log("允许访问:", to.path);
    next();
  }
});

export default router;
