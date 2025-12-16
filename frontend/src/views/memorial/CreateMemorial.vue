<template>
  <div class="create-memorial-container">
    <el-page-header @back="goBack" content="创建纪念册" />

    <!-- 流程步骤指示器 -->
    <div class="steps-wrapper">
      <el-steps
        :active="activeStep"
        finish-status="success"
        align-center
        class="steps-container"
      >
        <el-step title="模板选择" description="选择纪念册模板" />
        <el-step title="照片上传" description="上传宠物照片" />
        <el-step title="内容编辑" description="编辑纪念册内容" />
        <el-step title="设计审核" description="审核纪念册设计" />
        <el-step title="制作完成" description="纪念册制作完成" />
      </el-steps>
    </div>

    <!-- 步骤1: 选择模板 -->
    <div v-if="activeStep === 0" class="step-content">
      <div class="step-header">
        <h2>选择纪念册模板</h2>
        <p class="step-description">请选择一个您喜欢的纪念册模板</p>
      </div>

      <div class="template-categories">
        <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
          <el-tab-pane label="全部" name="all" />
          <el-tab-pane label="简约风格" name="simple" />
          <el-tab-pane label="温馨风格" name="warm" />
          <el-tab-pane label="经典风格" name="classic" />
          <el-tab-pane label="现代风格" name="modern" />
          <el-tab-pane label="复古风格" name="vintage" />
          <el-tab-pane label="文艺风格" name="literary" />
          <el-tab-pane label="其他" name="other" />
        </el-tabs>
      </div>

      <div v-loading="loading" class="template-grid">
        <el-empty v-if="!loading && filteredTemplates.length === 0" description="暂无可用模板" />
        <div
          v-for="template in filteredTemplates"
          :key="template.id"
          class="template-card"
          :class="{ selected: selectedTemplateId === template.id }"
          @click="handleTemplateClick(template)"
        >
          <div class="template-preview">
            <img v-if="getTemplateCover(template)" :src="getTemplateCover(template)" :alt="template.name" />
            <div v-else class="template-placeholder">
              <el-icon :size="60"><Picture /></el-icon>
              <span>暂无预览</span>
            </div>
            <div v-if="selectedTemplateId === template.id" class="selected-badge">
              <el-icon><Check /></el-icon>
            </div>
          </div>
          <div class="template-info">
            <h4>{{ template.name }}</h4>
            <p>{{ template.description || '精美的纪念册模板' }}</p>
            <div class="template-footer">
              <el-tag type="info" size="small">{{ template.categoryText || getCategoryText(template.category) }}</el-tag>
              <el-tag v-if="getTemplateImages(template).length > 0" type="success" size="small">
                {{ getTemplateImages(template).length }} 张
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="step-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button
          type="primary"
          @click="nextStep"
          :disabled="!selectedTemplateId"
          size="large"
        >
          下一步：上传照片
        </el-button>
      </div>

      <!-- 模板图片预览 -->
      <el-dialog v-model="previewDialogVisible" title="模板图片预览" width="920px">
        <div v-if="previewTemplate" class="template-preview-dialog">
          <div class="dialog-header">
            <div class="title-area">
              <div class="name">{{ previewTemplate.name }}</div>
              <div class="desc">{{ previewTemplate.description || "—" }}</div>
            </div>
            <el-tag type="info">
              {{ previewTemplate.categoryText || getCategoryText(previewTemplate.category) }}
            </el-tag>
          </div>

          <el-empty v-if="previewImages.length === 0" description="暂无模板图片" />

          <el-carousel v-else height="520px" indicator-position="outside">
            <el-carousel-item v-for="(url, idx) in previewImages" :key="url">
              <el-image
                :src="url"
                fit="contain"
                style="width: 100%; height: 520px"
                :preview-src-list="previewImages"
                :initial-index="idx"
              />
            </el-carousel-item>
          </el-carousel>
        </div>

        <template #footer>
          <el-button @click="previewDialogVisible = false">关闭</el-button>
          <el-button
            type="primary"
            :disabled="!previewTemplate"
            @click="selectFromPreview"
          >
            选择该模板
          </el-button>
        </template>
      </el-dialog>
    </div>

    <!-- 步骤2: 照片上传 -->
    <div v-if="activeStep === 1" class="step-content">
      <div class="step-header">
        <h2>上传宠物照片</h2>
        <p class="step-description">上传您想要添加到纪念册中的宠物照片</p>
      </div>

      <el-card shadow="hover" class="upload-card">
        <el-upload
          class="photo-uploader"
          :action="uploadAction"
          :headers="uploadHeaders"
          v-model:file-list="photoList"
          :on-success="handleUploadSuccess"
          :on-remove="handleRemovePhoto"
          :before-upload="beforeUpload"
          list-type="picture-card"
          :limit="20"
          multiple
        >
          <el-icon><Plus /></el-icon>
          <template #tip>
            <div class="upload-tip">
              <p>支持 JPG、PNG 格式，单张图片不超过 10MB</p>
              <p>建议上传高清照片，最多可上传 20 张</p>
            </div>
          </template>
        </el-upload>
      </el-card>

      <div class="step-actions">
        <el-button @click="prevStep">上一步</el-button>
        <el-button
          type="primary"
          @click="nextStep"
          :disabled="photoList.length === 0"
          size="large"
        >
          下一步：编辑内容
        </el-button>
      </div>
    </div>

    <!-- 步骤3: 内容编辑 -->
    <div v-if="activeStep === 2" class="step-content">
      <div class="step-header">
        <h2>编辑纪念册内容</h2>
        <p class="step-description">填写纪念册的基本信息和内容</p>
      </div>

      <!-- 纪念册信息 -->
      <el-card shadow="hover" class="memorial-info-card" style="margin-bottom: 20px">
        <template #header>
          <div class="card-header">
            <span>纪念册信息</span>
          </div>
        </template>
        <el-form-item label="纪念册标题" prop="title" style="margin-bottom: 20px">
          <el-input
            v-model="memorialForm.title"
            placeholder="请输入纪念册标题"
            size="large"
          />
        </el-form-item>
        <el-form-item label="已选模板" style="margin-bottom: 0">
          <el-tag v-if="selectedTemplate" type="info" size="large">
            {{ selectedTemplate.name }}
          </el-tag>
          <el-button
            v-else
            link
            type="primary"
            @click="activeStep = 0"
          >
            请先选择模板
          </el-button>
        </el-form-item>
      </el-card>

      <el-card shadow="hover" class="form-card">
        <el-form
          ref="memorialFormRef"
          :model="memorialForm"
          :rules="memorialRules"
          label-width="120px"
          class="memorial-form"
        >
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="关联订单" prop="orderId">
                <el-select
                  v-model="memorialForm.orderId"
                  placeholder="请选择订单（不选则无法提交设计）"
                  clearable
                  filterable
                  style="width: 100%"
                  :loading="loadingOrders"
                >
                  <el-option
                    v-for="o in orders"
                    :key="o.id"
                    :label="`订单#${o.id} ${o.orderNo || ''}（${o.petName || ''}）`"
                    :value="o.id"
                  >
                    <div style="display:flex; justify-content: space-between; gap: 12px;">
                      <span>订单#{{ o.id }} {{ o.orderNo }}</span>
                      <span style="color:#909399;">{{ o.createTime ? new Date(o.createTime).toLocaleString('zh-CN') : '' }}</span>
                    </div>
                  </el-option>
                </el-select>
                <div class="form-tip">关联订单后，才可以在“我的纪念册”里提交到服务端设计。</div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="副标题" prop="subtitle">
                <el-input
                  v-model="memorialForm.subtitle"
                  placeholder="请输入副标题（选填）"
                  size="large"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="宠物姓名" prop="petName">
                <el-input
                  v-model="memorialForm.petName"
                  placeholder="请输入宠物姓名"
                  size="large"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="宠物类型" prop="petType">
                <el-select
                  v-model="memorialForm.petType"
                  placeholder="请选择宠物类型"
                  style="width: 100%"
                  size="large"
                >
                  <el-option label="猫" value="cat" />
                  <el-option label="狗" value="dog" />
                  <el-option label="鸟" value="bird" />
                  <el-option label="兔子" value="rabbit" />
                  <el-option label="仓鼠" value="hamster" />
                  <el-option label="其他" value="other" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="出生日期" prop="petBirthDate">
                <el-date-picker
                  v-model="memorialForm.petBirthDate"
                  type="date"
                  placeholder="选择出生日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  size="large"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="离世日期" prop="petDeathDate">
                <el-date-picker
                  v-model="memorialForm.petDeathDate"
                  type="date"
                  placeholder="选择离世日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  size="large"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="纪念寄语" prop="petMemory">
            <el-input
              v-model="memorialForm.petMemory"
              type="textarea"
              :rows="6"
              placeholder="请写下对宠物的纪念寄语..."
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </el-card>

      <div class="step-actions">
        <el-button @click="prevStep">上一步</el-button>
        <el-button
          type="primary"
          @click="nextStep"
          size="large"
        >
          下一步：设计审核
        </el-button>
      </div>
    </div>

    <!-- 步骤4: 设计审核 -->
    <div v-if="activeStep === 3" class="step-content">
      <div class="step-header">
        <h2>审核纪念册设计</h2>
        <p class="step-description">请确认纪念册信息无误后提交审核</p>
      </div>

      <el-card shadow="hover" class="review-card">
        <div class="review-content">
          <div class="review-section">
            <h3>模板信息</h3>
            <div class="review-item">
              <span class="label">选择的模板：</span>
              <span class="value">{{ selectedTemplate?.name || '未选择' }}</span>
            </div>
          </div>

          <el-divider />

          <div class="review-section">
            <h3>基本信息</h3>
            <div class="review-item">
              <span class="label">纪念册标题：</span>
              <span class="value">{{ memorialForm.title || '未填写' }}</span>
            </div>
            <div class="review-item">
              <span class="label">副标题：</span>
              <span class="value">{{ memorialForm.subtitle || '未填写' }}</span>
            </div>
            <div class="review-item">
              <span class="label">宠物姓名：</span>
              <span class="value">{{ memorialForm.petName || '未填写' }}</span>
            </div>
            <div class="review-item">
              <span class="label">宠物类型：</span>
              <span class="value">{{ getPetTypeName(memorialForm.petType) || '未选择' }}</span>
            </div>
            <div class="review-item">
              <span class="label">出生日期：</span>
              <span class="value">{{ memorialForm.petBirthDate || '未填写' }}</span>
            </div>
            <div class="review-item">
              <span class="label">离世日期：</span>
              <span class="value">{{ memorialForm.petDeathDate || '未填写' }}</span>
            </div>
          </div>

          <el-divider />

          <div class="review-section">
            <h3>照片信息</h3>
            <div class="review-item">
              <span class="label">已上传照片：</span>
              <span class="value">{{ photoList.length }} 张</span>
            </div>
          </div>

          <el-divider />

          <div class="review-section">
            <h3>纪念寄语</h3>
            <div class="review-item full-width">
              <p class="memory-text">{{ memorialForm.petMemory || '未填写' }}</p>
            </div>
          </div>
        </div>
      </el-card>

      <div class="step-actions">
        <el-button @click="prevStep">上一步</el-button>
        <el-button
          type="primary"
          @click="createMemorial"
          :loading="submitting"
          size="large"
        >
          提交审核
        </el-button>
      </div>
    </div>

    <!-- 步骤5: 制作完成 -->
    <div v-if="activeStep === 4" class="step-content">
      <div class="success-container">
        <el-result
          icon="success"
          title="纪念册创建成功"
          :sub-title="`您已成功创建《${memorialForm.title}》纪念册，请等待审核`"
        >
          <template #extra>
            <div class="success-actions">
              <el-button type="primary" size="large" @click="editMemorial">
                开始编辑
              </el-button>
              <el-button size="large" @click="viewMemorialList">
                查看我的纪念册
              </el-button>
              <el-button size="large" @click="goHome">
                返回首页
              </el-button>
            </div>
          </template>
        </el-result>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Picture, Check, Plus } from "@element-plus/icons-vue";
import { useMemorialStore } from "@/store/memorial";
import { memorialApi, type TemplateVO } from "@/api/memorial";
import request from "@/api/request";
import type { UploadFile } from "element-plus";
import { orderApi, type OrderVO } from "@/api/order";

// 定义事件
const emit = defineEmits<{
  created: [];
}>();

const router = useRouter();
const route = useRoute();
const memorialStore = useMemorialStore();

// 当前步骤
const activeStep = ref(0);
// 加载状态
const loading = ref(false);
// 提交状态
const submitting = ref(false);
// 当前选中的分类
const activeCategory = ref("all");
// 选中的模板ID
const selectedTemplateId = ref<number | null>(null);
// 选中的模板
const selectedTemplate = ref<TemplateVO | null>(null);
// 照片列表（ElementPlus UploadFile）
const photoList = ref<UploadFile[]>([]);

// 表单引用
const memorialFormRef = ref();

// 纪念册表单
const memorialForm = reactive({
  title: "",
  subtitle: "",
  petName: "",
  petType: "",
  petBirthDate: "",
  petDeathDate: "",
  petMemory: "",
  orderId: null as number | null,
});

// 订单列表（用于关联订单）
const orders = ref<OrderVO[]>([]);
const loadingOrders = ref(false);

async function loadMyOrders() {
  loadingOrders.value = true;
  try {
    const res = await orderApi.getOrderList({ pageNum: 1, pageSize: 50 });
    orders.value = res.data?.records || [];
  } catch {
    orders.value = [];
  } finally {
    loadingOrders.value = false;
  }
}

// 表单验证规则
const memorialRules = reactive({
  title: [
    { required: true, message: "请输入纪念册标题", trigger: "blur" },
    { min: 2, max: 50, message: "长度在 2 到 50 个字符", trigger: "blur" },
  ],
  petName: [{ required: true, message: "请输入宠物姓名", trigger: "blur" }],
  petType: [{ required: true, message: "请选择宠物类型", trigger: "change" }],
  petDeathDate: [
    { required: true, message: "请选择离世日期", trigger: "change" },
  ],
});

// 上传配置
const uploadAction = computed(() => {
  if (memorialStore.currentMemorial?.id) {
    // 通过后端 /api 前缀访问上传接口
    return `/api/memorial/upload-image/${memorialStore.currentMemorial.id}`;
  }
  // 未创建纪念册前，先上传到 temp 目录
  return "/api/memorial/upload-image/temp";
});

const uploadHeaders = computed(() => {
  const token = sessionStorage.getItem("token");
  return {
    Authorization: token ? `Bearer ${token}` : "",
  };
});

// 过滤后的模板列表
const filteredTemplates = computed(() => {
  if (activeCategory.value === "all") {
    return memorialStore.templates;
  }
  return memorialStore.templates.filter(
    (t) => t.category === activeCategory.value
  );
});

// 获取宠物类型名称
const getPetTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    cat: "猫",
    dog: "狗",
    bird: "鸟",
    rabbit: "兔子",
    hamster: "仓鼠",
    other: "其他",
  };
  return typeMap[type] || type;
};

// 返回上一页
const goBack = () => {
  if (activeStep.value > 0) {
    activeStep.value--;
  } else {
    router.back();
  }
};

// 返回首页
const goHome = () => {
  router.push("/home");
};

// 上一步
const prevStep = () => {
  activeStep.value--;
};

// 下一步
const nextStep = () => {
  if (activeStep.value === 0 && !selectedTemplateId.value) {
    ElMessage.warning("请选择一个模板");
    return;
  }

  if (activeStep.value === 1 && photoList.value.length === 0) {
    ElMessage.warning("请至少上传一张照片");
    return;
  }

  if (activeStep.value === 2) {
    memorialFormRef.value.validate((valid: boolean) => {
      if (valid) {
        activeStep.value++;
      }
    });
  } else {
    activeStep.value++;
  }
};

// 选择模板
const selectTemplate = (template: TemplateVO) => {
  selectedTemplateId.value = template.id;
  selectedTemplate.value = template;
};

// 分类切换
const handleCategoryChange = () => {
  selectedTemplateId.value = null;
  selectedTemplate.value = null;
};

function getCategoryText(category?: string) {
  if (!category) return "其他";
  const map: Record<string, string> = {
    simple: "简约风格",
    warm: "温馨风格",
    classic: "经典风格",
    modern: "现代风格",
    vintage: "复古风格",
    literary: "文艺风格",
    other: "其他",
    all: "全部",
  };
  return map[category] || "其他";
}

function getTemplateImages(template: TemplateVO): string[] {
  if (template.templateImages && template.templateImages.length > 0) return template.templateImages;
  if (template.previewImage) return [template.previewImage];
  // 兜底：从 styleConfig 里解析（兼容极端情况下后端未返回 templateImages）
  if (template.styleConfig) {
    try {
      const parsed = JSON.parse(template.styleConfig);
      const images = parsed?.templateImages;
      if (Array.isArray(images)) {
        return images.filter((x: any) => typeof x === "string" && x.trim().length > 0);
      }
    } catch {
      // ignore
    }
  }
  return [];
}

function getTemplateCover(template: TemplateVO): string {
  return template.previewImage || getTemplateImages(template)[0] || "";
}

// 模板预览弹窗：点击模板即可查看图片
const previewDialogVisible = ref(false);
const previewTemplate = ref<TemplateVO | null>(null);
const previewImages = computed(() => {
  if (!previewTemplate.value) return [];
  return getTemplateImages(previewTemplate.value);
});

function openPreview(template: TemplateVO) {
  previewTemplate.value = template;
  previewDialogVisible.value = true;
}

function handleTemplateClick(template: TemplateVO) {
  // 保持“点击模板即预览”的交互，同时也默认选中，方便直接下一步
  selectTemplate(template);
  openPreview(template);
}

function selectFromPreview() {
  if (!previewTemplate.value) return;
  selectTemplate(previewTemplate.value);
  previewDialogVisible.value = false;
}

// 上传前验证
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith("image/");
  const isLt10M = file.size / 1024 / 1024 < 10;

  if (!isImage) {
    ElMessage.error("上传文件只能是图片格式!");
    return false;
  }
  if (!isLt10M) {
    ElMessage.error("上传图片大小不能超过 10MB!");
    return false;
  }
  return true;
};

// 上传成功：使用 v-model:file-list，由 Upload 组件维护列表，这里只回填 url
const handleUploadSuccess = (response: any, uploadFile: any) => {
  if (response && response.code === 200) {
    uploadFile.url = response.data;
    uploadFile.status = "success";
    ElMessage.success("照片上传成功");
  } else {
    uploadFile.status = "fail";
    ElMessage.error(response?.message || "上传失败");
  }
};

// 删除照片
const handleRemovePhoto = (file: any) => {
  const index = photoList.value.findIndex((item) => item.uid === file.uid);
  if (index > -1) {
    photoList.value.splice(index, 1);
  }
};

// 创建纪念册
const createMemorial = async () => {
  try {
    submitting.value = true;

    // 构建创建数据
    // 将日期字符串转换为包含时间的格式（yyyy-MM-ddTHH:mm:ss）
    const formatDateForBackend = (dateStr: string | null | undefined): string | undefined => {
      if (!dateStr) return undefined;
      // 如果已经是完整格式，直接返回
      if (dateStr.includes('T')) return dateStr;
      // 否则添加时间部分
      return `${dateStr}T00:00:00`;
    };

    const createData = {
      title: memorialForm.title,
      subtitle: memorialForm.subtitle,
      templateId: selectedTemplateId.value,
      orderId: memorialForm.orderId,
      petName: memorialForm.petName,
      petType: memorialForm.petType,
      petBirthDate: formatDateForBackend(memorialForm.petBirthDate),
      petDeathDate: formatDateForBackend(memorialForm.petDeathDate),
      petMemory: memorialForm.petMemory,
      photoUrls: photoList.value
        .filter((f) => (f as any)?.status === "success" && (f as any)?.url)
        .map((f) => (f as any).url as string),
    };

    const result = await memorialStore.createMemorial(createData);
    if (result) {
      ElMessage.success("纪念册创建成功，请等待审核");
      activeStep.value = 4;
      // 确保currentMemorial已设置，以便"开始编辑"按钮可以使用
      if (result.id && !memorialStore.currentMemorial) {
        memorialStore.currentMemorial = result;
      }
      // 发射创建完成事件
      emit("created");
    }
  } catch (error) {
    console.error("创建纪念册失败:", error);
    ElMessage.error("创建纪念册失败，请稍后重试");
  } finally {
    submitting.value = false;
  }
};

// 编辑纪念册
const editMemorial = () => {
  // 优先使用store中的currentMemorial，如果没有则从创建结果中获取
  const memorialId = memorialStore.currentMemorial?.id;
  if (memorialId) {
    router.push(`/memorial/edit/${memorialId}`);
  } else {
    ElMessage.warning("无法获取纪念册ID，请稍后重试");
  }
};

// 查看纪念册列表
const viewMemorialList = () => {
  router.push("/memorial/list");
};

// 初始化
onMounted(async () => {
  // 获取订单ID参数
  const orderId = route.query.orderId;
  if (orderId) {
    memorialForm.orderId = Number(orderId);
  }

  // 加载模板列表
  loading.value = true;
  try {
    await memorialStore.fetchTemplates();
  } catch (error) {
    console.error("加载模板失败:", error);
    ElMessage.error("加载模板失败，请刷新页面重试");
  } finally {
    loading.value = false;
  }

  // 加载订单列表（用于关联订单）
  loadMyOrders();
});
</script>

<style scoped lang="scss">
.create-memorial-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 100px);

  .steps-wrapper {
    background: #fff;
    padding: 30px 20px;
    border-radius: 8px;
    margin: 20px 0;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }

  .step-content {
    margin-top: 30px;
    animation: fadeIn 0.3s ease-in;

    .step-header {
      text-align: center;
      margin-bottom: 30px;

      h2 {
        font-size: 28px;
        color: #303133;
        margin-bottom: 10px;
      }

      .step-description {
        font-size: 16px;
        color: #909399;
        margin: 0;
      }
    }

    .template-categories {
      margin-bottom: 30px;
      
      :deep(.el-tabs__header) {
        margin-bottom: 0;
      }
      
      :deep(.el-tabs__nav-wrap) {
        &::after {
          height: 1px;
        }
      }
      
      :deep(.el-tabs__item) {
        font-size: 16px;
        padding: 0 24px;
        height: 50px;
        line-height: 50px;
        font-weight: 500;
        
        &.is-active {
          color: #409eff;
          font-weight: 600;
        }
      }
      
      :deep(.el-tabs__active-bar) {
        height: 3px;
        background: #409eff;
      }
    }

    .template-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
      margin-bottom: 40px;

      .template-card {
        border: 2px solid #ebeef5;
        border-radius: 12px;
        overflow: hidden;
        cursor: pointer;
        transition: all 0.3s;
        background: #fff;

        &:hover {
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
          transform: translateY(-4px);
          border-color: #409eff;
        }

        &.selected {
          border-color: #409eff;
          box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
        }

        .template-preview {
          position: relative;
          height: 220px;
          overflow: hidden;
          background: #f5f7fa;

          img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.3s;
          }

          .template-placeholder {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100%;
            color: #909399;

            span {
              margin-top: 10px;
              font-size: 14px;
            }
          }

          .selected-badge {
            position: absolute;
            top: 10px;
            right: 10px;
            width: 32px;
            height: 32px;
            background: #409eff;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-size: 18px;
          }
        }

        .template-info {
          padding: 20px;

          h4 {
            margin: 0 0 8px;
            font-size: 18px;
            color: #303133;
            font-weight: 600;
          }

          p {
            margin: 0 0 15px;
            font-size: 14px;
            color: #606266;
            line-height: 1.6;
            height: 44px;
            overflow: hidden;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
          }

          .template-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 8px;

            .price {
              font-weight: bold;
              color: #e6a23c;
              font-size: 16px;
            }
          }
        }
      }
    }

    .template-preview-dialog {
      .dialog-header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;
      }

      .title-area {
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;
      }

      .name {
        font-size: 16px;
        font-weight: 700;
        color: #303133;
      }

      .desc {
        font-size: 13px;
        color: #909399;
        word-break: break-word;
      }
    }

    .upload-card {
      margin-bottom: 30px;

      .photo-uploader {
        :deep(.el-upload) {
          border: 2px dashed #d9d9d9;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.3s;

          &:hover {
            border-color: #409eff;
          }
        }

        .upload-tip {
          margin-top: 15px;
          text-align: center;
          color: #909399;
          font-size: 14px;

          p {
            margin: 5px 0;
          }
        }
      }
    }

    .form-card {
      margin-bottom: 30px;
    }

    .review-card {
      margin-bottom: 30px;

      .review-content {
        .review-section {
          h3 {
            font-size: 18px;
            color: #303133;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 2px solid #f0f0f0;
          }

          .review-item {
            display: flex;
            margin-bottom: 15px;

            &.full-width {
              flex-direction: column;
            }

            .label {
              font-weight: 600;
              color: #606266;
              min-width: 120px;
            }

            .value {
              color: #303133;
            }

            .memory-text {
              color: #303133;
              line-height: 1.8;
              white-space: pre-wrap;
              margin: 0;
            }
          }
        }
      }
    }

    .step-actions {
      display: flex;
      justify-content: center;
      margin-top: 40px;
      gap: 20px;
    }

    .success-container {
      padding: 60px 20px;
      text-align: center;

      .success-actions {
        margin-top: 30px;
        display: flex;
        justify-content: center;
        gap: 15px;
        flex-wrap: wrap;
      }
    }
  }

  .form-tip {
    margin-top: 6px;
    color: #909399;
    font-size: 12px;
    line-height: 1.4;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
