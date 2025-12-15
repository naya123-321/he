<template>
  <div class="template-list-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>纪念册模板</h1>
      <p>选择精美的模板，快速创建宠物纪念册</p>
    </div>

    <!-- 分类筛选和排序 -->
    <div class="category-filter">
      <el-radio-group v-model="activeCategory" @change="handleCategoryChange">
        <el-radio-button label="all">全部模板</el-radio-button>
        <el-radio-button
          v-for="category in categories"
          :key="category"
          :label="category"
        >
          {{ category }}
        </el-radio-button>
      </el-radio-group>

      <div class="sort-options">
        <el-select
          v-model="sortBy"
          @change="sortTemplates"
          placeholder="排序方式"
        >
          <el-option label="最新模板" value="newest" />
          <el-option label="最多下载" value="download" />
          <el-option label="价格从低到高" value="price_asc" />
          <el-option label="价格从高到低" value="price_desc" />
        </el-select>
      </div>
    </div>

    <!-- 模板网格 -->
    <div v-loading="loading" class="template-grid">
      <div v-if="templates.length === 0" class="empty-state">
        <el-empty description="暂无模板数据" />
      </div>

      <div
        v-for="template in templates"
        :key="template.id"
        class="template-card"
      >
        <div class="template-preview" @click="showPreview(template)">
          <img
            :src="template.coverImage || '/default-template.jpg'"
            :alt="template.name"
          />

          <!-- 价格标签 -->
          <div v-if="!template.isFree" class="price-tag">
            ¥{{ template.price }}
          </div>

          <!-- 免费标签 -->
          <div v-else class="free-tag">免费</div>
        </div>

        <div class="template-info">
          <h3 class="template-name" @click="showTemplateDetail(template)">
            {{ template.name }}
          </h3>
          <p class="template-desc">{{ template.description }}</p>

          <div class="template-meta">
            <div class="download-count">
              <el-icon><Download /></el-icon>
              {{ template.downloadCount || 0 }} 次下载
            </div>
            <div class="template-category">
              <el-tag size="small" type="info">{{ template.category }}</el-tag>
            </div>
          </div>

          <div class="template-actions">
            <el-button
              type="primary"
              size="small"
              @click="useTemplate(template)"
            >
              使用模板
            </el-button>
            <el-button size="small" @click="showPreview(template)">
              预览
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="templates.length > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 36, 48]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 模板预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewTemplateData?.name"
      width="80%"
      class="template-preview-dialog"
    >
      <div v-if="previewTemplateData" class="template-preview">
        <div class="main-preview">
          <el-carousel
            :autoplay="false"
            arrow="always"
            indicator-position="outside"
            @change="activePreviewIndex = $event"
          >
            <el-carousel-item
              v-for="(image, index) in previewTemplateData.previewImages"
              :key="index"
            >
              <img
                :src="image"
                :alt="`${previewTemplateData.name} - 预览${index + 1}`"
              />
            </el-carousel-item>
          </el-carousel>
        </div>

        <div class="thumbnail-list">
          <div
            v-for="(image, index) in previewTemplateData.previewImages"
            :key="index"
            class="thumbnail-item"
            :class="{ active: activePreviewIndex === index }"
            @click="activePreviewIndex = index"
          >
            <img :src="image" :alt="`缩略图${index + 1}`" />
          </div>
        </div>

        <div class="preview-info">
          <h3>{{ previewTemplateData.name }}</h3>
          <p class="preview-description">
            {{ previewTemplateData.description }}
          </p>

          <div class="preview-features">
            <h4>模板特点</h4>
            <ul>
              <li
                v-for="feature in getTemplateFeatures(previewTemplateData)"
                :key="feature"
              >
                <el-icon><Check /></el-icon>
                {{ feature }}
              </li>
            </ul>
          </div>

          <div class="preview-actions">
            <el-button type="primary" @click="useTemplate(previewTemplateData)">
              使用模板
            </el-button>
            <el-button @click="showTemplateDetail(previewTemplateData)">
              查看详情
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 模板详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="detailTemplateData?.name"
      width="70%"
      class="template-detail-dialog"
    >
      <div v-if="detailTemplateData" class="template-detail">
        <div class="detail-images">
          <el-carousel :autoplay="false" arrow="always" height="400px">
            <el-carousel-item
              v-for="(image, index) in detailTemplateData.previewImages"
              :key="index"
            >
              <img
                :src="image"
                :alt="`${detailTemplateData.name} - 预览${index + 1}`"
              />
            </el-carousel-item>
          </el-carousel>
        </div>

        <div class="detail-info">
          <h2>{{ detailTemplateData.name }}</h2>
          <p class="detail-description">{{ detailTemplateData.description }}</p>

          <div class="detail-meta">
            <div class="meta-item">
              <span class="meta-label">分类：</span>
              <el-tag>{{ detailTemplateData.category }}</el-tag>
            </div>
            <div class="meta-item">
              <span class="meta-label">下载次数：</span>
              <span>{{ detailTemplateData.downloadCount || 0 }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">创建时间：</span>
              <span>{{ formatDate(detailTemplateData.createTime) }}</span>
            </div>
          </div>

          <div class="specifications">
            <h4>模板规格</h4>
            <div class="spec-item">
              <span class="spec-name">页面数量</span>
              <span class="spec-value"
                >{{ detailTemplateData.templateData?.pages || 8 }} 页</span
              >
            </div>
            <div class="spec-item">
              <span class="spec-name">模板尺寸</span>
              <span class="spec-value">A4 (210×297mm)</span>
            </div>
            <div class="spec-item">
              <span class="spec-name">分辨率</span>
              <span class="spec-value">300 DPI</span>
            </div>
            <div class="spec-item">
              <span class="spec-name">导出格式</span>
              <span class="spec-value">PDF, JPG</span>
            </div>
          </div>

          <div class="detail-actions">
            <el-button
              type="primary"
              size="large"
              @click="useTemplate(detailTemplateData)"
            >
              使用模板
            </el-button>
            <el-button
              size="large"
              @click="
                previewVisible = true;
                detailDialogVisible = false;
              "
            >
              预览模板
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 使用模板创建纪念册对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建纪念册"
      width="500px"
      class="create-memorial-dialog"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="80px"
      >
        <el-form-item label="模板" v-if="selectedTemplate">
          <div class="selected-template">
            <img
              :src="selectedTemplate.coverImage"
              :alt="selectedTemplate.name"
            />
            <div class="template-info">
              <h4>{{ selectedTemplate.name }}</h4>
              <p>{{ selectedTemplate.description }}</p>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="纪念册标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入纪念册标题" />
        </el-form-item>

        <el-form-item label="宠物名称" prop="petName">
          <el-input v-model="createForm.petName" placeholder="请输入宠物名称" />
        </el-form-item>

        <el-form-item label="宠物类型" prop="petType">
          <el-select v-model="createForm.petType" placeholder="请选择宠物类型">
            <el-option label="狗" value="dog" />
            <el-option label="猫" value="cat" />
            <el-option label="鸟" value="bird" />
            <el-option label="兔子" value="rabbit" />
            <el-option label="仓鼠" value="hamster" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="关联订单" prop="orderId">
          <el-select
            v-model="createForm.orderId"
            placeholder="请选择关联订单（可选）"
          >
            <el-option
              v-for="order in userOrders"
              :key="order.id"
              :label="`订单 #${order.id} - ${order.createTime}`"
              :value="order.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="creating"
            @click="submitCreateForm"
          >
            创建纪念册
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { Download, Check } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import dayjs from "dayjs";
import { memorialApi } from "@/api/memorial";
import { orderApi } from "@/api/order";
import type { TemplateVO } from "@/api/memorial";

// 路由实例
const router = useRouter();

// 响应式数据
const loading = ref(false);
const templates = ref<TemplateVO[]>([]);
const categories = ref<string[]>(["简约", "温馨", "复古", "现代", "创意"]);
const activeCategory = ref("all");
const sortBy = ref("newest");
const currentPage = ref(1);
const pageSize = ref(12);
const total = ref(0);

// 预览相关
const previewVisible = ref(false);
const previewTemplateData = ref<TemplateVO | null>(null);
const activePreviewIndex = ref(0);

// 详情相关
const detailDialogVisible = ref(false);
const detailTemplateData = ref<TemplateVO | null>(null);

// 创建纪念册相关
const createDialogVisible = ref(false);
const selectedTemplate = ref<TemplateVO | null>(null);
const createFormRef = ref();
const creating = ref(false);
const userOrders = ref<any[]>([]);

// 创建表单
const createForm = reactive({
  title: "",
  petName: "",
  petType: "",
  templateId: "",
  orderId: "",
});

// 表单验证规则
const createRules = {
  title: [
    { required: true, message: "请输入纪念册标题", trigger: "blur" },
    { min: 2, max: 50, message: "标题长度在 2 到 50 个字符", trigger: "blur" },
  ],
  petName: [
    { required: true, message: "请输入宠物名称", trigger: "blur" },
    {
      min: 1,
      max: 20,
      message: "宠物名称长度在 1 到 20 个字符",
      trigger: "blur",
    },
  ],
  petType: [{ required: true, message: "请选择宠物类型", trigger: "change" }],
};

// 加载模板
const loadTemplates = async () => {
  loading.value = true;
  try {
    const category =
      activeCategory.value === "all" ? undefined : activeCategory.value;
    const res = await memorialApi.getTemplates(category);

    if (res.data.code === 200) {
      templates.value = res.data.data;

      // 排序
      sortTemplates();
    }
  } catch (error) {
    console.error("加载模板失败:", error);
    ElMessage.error("加载模板失败");
  } finally {
    loading.value = false;
  }
};

// 排序模板
const sortTemplates = () => {
  switch (sortBy.value) {
    case "download":
      templates.value.sort(
        (a, b) => (b.downloadCount || 0) - (a.downloadCount || 0)
      );
      break;
    case "price_asc":
      templates.value.sort((a, b) => (a.price || 0) - (b.price || 0));
      break;
    case "price_desc":
      templates.value.sort((a, b) => (b.price || 0) - (a.price || 0));
      break;
    default:
      // 默认按ID排序
      templates.value.sort((a, b) => b.id - a.id);
  }
};

// 加载用户订单
const loadUserOrders = async () => {
  try {
    const res = await orderApi.getOrderList({
      pageSize: 100,
      status: 3, // 已完成订单
    });

    if (res.data.code === 200) {
      userOrders.value = res.data.data.records || [];
    }
  } catch (error) {
    console.error("加载订单失败:", error);
  }
};

// 分类变更
const handleCategoryChange = () => {
  currentPage.value = 1;
  loadTemplates();
};

// 分页大小变更
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadTemplates();
};

// 页码变更
const handlePageChange = (page: number) => {
  currentPage.value = page;
  loadTemplates();
};

// 预览模板
const showPreview = (template: TemplateVO) => {
  previewTemplateData.value = template;
  activePreviewIndex.value = 0;
  previewVisible.value = true;
};

// 显示模板详情
const showTemplateDetail = (template: TemplateVO) => {
  detailTemplateData.value = template;
  detailDialogVisible.value = true;
};

// 使用模板
const useTemplate = (template: TemplateVO) => {
  selectedTemplate.value = template;
  createForm.templateId = template.id;

  // 设置默认标题
  if (!createForm.title) {
    createForm.title = `${template.name} - 宠物纪念册`;
  }

  createDialogVisible.value = true;
};

// 提交创建表单
const submitCreateForm = async () => {
  if (!createFormRef.value) return;

  const valid = await createFormRef.value.validate();
  if (!valid) return;

  creating.value = true;

  try {
    const res = await memorialApi.createMemorial(createForm);

    if (res.data.code === 200) {
      ElMessage.success("纪念册创建成功");

      // 跳转到编辑页面
      router.push({
        path: "/memorial/edit",
        query: { id: res.data.data.id },
      });
    }
  } catch (error: any) {
    ElMessage.error(error.message || "创建失败");
  } finally {
    creating.value = false;
    createDialogVisible.value = false;
  }
};

// 获取模板特点
const getTemplateFeatures = (template: TemplateVO | null) => {
  if (!template) return [];

  const features = [];

  if (template.isFree) {
    features.push("完全免费使用");
  }

  if (template.templateData?.pages) {
    features.push(`${template.templateData.pages}页完整设计`);
  }

  if (template.category === "简约") {
    features.push("简洁大方，适合所有宠物");
  } else if (template.category === "温馨") {
    features.push("温馨风格，充满爱意");
  }

  features.push("支持自定义文字和图片");
  features.push("可导出高清PDF");

  return features;
};

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return "";
  return dayjs(dateStr).format("YYYY年MM月DD日");
};

// 初始化
onMounted(() => {
  loadTemplates();
  loadUserOrders();
});
</script>

<style scoped lang="scss">
.template-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;

  h1 {
    font-size: 28px;
    margin-bottom: 10px;
    color: #333;
  }

  p {
    color: #666;
    font-size: 16px;
  }
}

.category-filter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;

  .el-radio-button {
    margin-right: 8px;
  }

  .sort-options {
    min-width: 200px;
  }
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 30px;

  .empty-state {
    grid-column: 1 / -1;
    text-align: center;
    padding: 50px 0;
  }
}

.template-card {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  background-color: #fff;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  .template-preview {
    position: relative;
    height: 200px;
    overflow: hidden;
    cursor: pointer;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.5s ease;
    }

    &:hover img {
      transform: scale(1.05);
    }

    .price-tag {
      position: absolute;
      top: 10px;
      right: 10px;
      background-color: #f56c6c;
      color: white;
      padding: 4px 8px;
      border-radius: 4px;
      font-weight: bold;
    }

    .free-tag {
      position: absolute;
      top: 10px;
      right: 10px;
      background-color: #67c23a;
      color: white;
      padding: 4px 8px;
      border-radius: 4px;
      font-weight: bold;
    }
  }

  .template-info {
    padding: 16px;

    .template-name {
      margin: 0 0 8px;
      font-size: 16px;
      font-weight: 500;
      color: #303133;
      cursor: pointer;

      &:hover {
        color: #409eff;
      }
    }

    .template-desc {
      margin: 0 0 12px;
      font-size: 14px;
      color: #606266;
      line-height: 1.5;
      height: 42px;
      overflow: hidden;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .template-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      font-size: 13px;
      color: #909399;

      .download-count {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    .template-actions {
      display: flex;
      gap: 8px;
    }
  }
}

.pagination-wrapper {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.template-preview-dialog {
  .template-preview {
    .main-preview {
      text-align: center;
      padding: 20px;
      background: #f9f9f9;
      border-radius: 8px;

      img {
        max-height: 400px;
        max-width: 100%;
        object-fit: contain;
      }
    }

    .thumbnail-list {
      display: flex;
      gap: 10px;
      overflow-x: auto;
      padding: 10px 0;

      .thumbnail-item {
        width: 80px;
        height: 60px;
        border: 2px solid transparent;
        cursor: pointer;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        &.active {
          border-color: #409eff;
        }
      }
    }

    .preview-info {
      padding: 10px;

      .preview-description {
        color: #666;
        margin: 16px 0;
        line-height: 1.6;
      }

      .preview-features {
        margin: 20px 0;

        h4 {
          margin-bottom: 10px;
          font-size: 16px;
        }

        ul {
          padding-left: 20px;

          li {
            margin-bottom: 8px;
            display: flex;
            align-items: center;

            .el-icon {
              margin-right: 8px;
              color: #409eff;
            }
          }
        }
      }

      .preview-actions {
        margin-top: 30px;
        display: flex;
        gap: 16px;
      }
    }
  }
}

.template-detail-dialog {
  .template-detail {
    display: flex;
    gap: 24px;

    .detail-images {
      flex: 1;
      height: 400px;
      border-radius: 8px;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }

    .detail-info {
      flex: 1;
      padding: 0 10px;

      h2 {
        margin: 0 0 12px;
        font-size: 24px;
        color: #303133;
      }

      .detail-description {
        margin: 0 0 20px;
        color: #606266;
        line-height: 1.6;
      }

      .detail-meta {
        margin-bottom: 20px;

        .meta-item {
          display: flex;
          align-items: center;
          margin-bottom: 8px;

          .meta-label {
            min-width: 80px;
            font-weight: 500;
            color: #606266;
          }
        }
      }

      .specifications {
        margin: 20px 0 30px;
        background-color: #f9f9f9;
        padding: 15px;
        border-radius: 4px;

        h4 {
          margin: 0 0 15px;
          color: #333;
          font-size: 16px;
          padding-bottom: 8px;
          border-bottom: 1px solid #e0e0e0;
        }

        .spec-item {
          display: flex;
          justify-content: space-between;
          padding: 10px 0;
          border-bottom: 1px dashed #e5e5e5;

          &:last-child {
            border-bottom: none;
          }

          .spec-name {
            color: #666;
            font-weight: 500;
          }

          .spec-value {
            color: #333;
            font-weight: 500;
          }
        }
      }

      .detail-actions {
        display: flex;
        justify-content: flex-end;
        gap: 16px;
        margin-top: 20px;
      }
    }
  }
}

.create-memorial-dialog {
  .selected-template {
    display: flex;
    gap: 12px;
    padding: 12px;
    background-color: #f9f9f9;
    border-radius: 4px;

    img {
      width: 60px;
      height: 60px;
      object-fit: cover;
      border-radius: 4px;
    }

    .template-info {
      h4 {
        margin: 0 0 5px;
        font-size: 16px;
      }

      p {
        margin: 0;
        font-size: 13px;
        color: #666;
      }
    }
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .template-list-container {
    padding: 8px;

    .template-grid {
      grid-template-columns: 1fr;
    }

    .template-preview {
      flex-direction: column;
      gap: 16px;
    }

    .template-detail .detail-content {
      flex-direction: column;
      gap: 16px;
    }
  }

  .category-filter {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 16px;
  }

  .template-grid {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }

  .template-card {
    margin: 8px;
    .template-info {
      padding: 12px;
    }
  }

  .template-preview {
    flex-direction: column;
    .main-preview {
      max-height: 300px;
    }
    .preview-info {
      padding: 0;
    }
  }

  .template-detail .detail-images {
    height: 240px;
  }
  .template-detail .detail-info {
    padding: 8px 0;
  }
  .detail-actions {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
