<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="90%"
    top="5vh"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <div class="image-picker-container">
      <!-- 上传区域 -->
      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          class="upload-area"
          :action="uploadAction"
          :headers="uploadHeaders"
          :data="uploadData"
          :multiple="true"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :on-progress="handleUploadProgress"
          drag
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <div class="upload-title">点击或拖拽图片到这里</div>
            <div class="upload-tip">
              支持 JPG、PNG、GIF 格式，单张图片不超过 10MB
            </div>
          </div>
        </el-upload>

        <!-- 上传进度 -->
        <div v-if="uploading" class="upload-progress">
          <div class="progress-info">
            <span
              >正在上传 {{ uploadFileName } // 选择预览图片 const
              selectPreviewImage = () => { const image = images.value.find(img
              => img.name === previewImageName); if (image) {
              selectedImages.value = [image.id]; confirmSelection(); } } //
              格式化日期 const formatDate = (dateString: string) => { return
              dayjs(dateString).format('YYYY-MM-DD HH:mm') } // 格式化文件大小
              const formatFileSize = (bytes: number) => { if (bytes === 0)
              return '0 B' const k = 1024 const sizes = ['B', 'KB', 'MB', 'GB']
              const i = Math.floor(Math.log(bytes) / Math.log(k)) return
              parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i] }
              // 处理分页变化 const handlePageChange = (page: number) => {
              pagination.current = page loadImages() } // 处理页大小变化 const
              handlePageSizeChange = (size: number) => { pagination.pageSize =
              size pagination.current = 1 loadImages() } // 搜索图片 const
              handleSearch = () => { pagination.current = 1 loadImages() } //
              刷新图片列表 const refreshImages = () => { loadImages() } //
              复制图片地址 const copyImageUrl = () => {
              copyText(previewImageUrl.value) .then(() =>
              ElMessage.success('图片地址已复制')) .catch(() =>
              ElMessage.error('复制失败')) } // 删除图片 const deleteImage =
              async (image: any) => { try { await
              ElMessageBox.confirm('确定要删除这张图片吗？', '确认删除', {
              confirmButtonText: '确定', cancelButtonText: '取消', type:
              'warning' }) // 这里应该调用删除图片的API // 暂时使用模拟删除
              const index = images.value.findIndex(img => img.id === image.id)
              if (index > -1) { images.value.splice(index, 1)
              ElMessage.success('删除成功') } } catch (error) { // 用户取消删除
              } } // 处理图片加载 const handleImageLoad = (image: any) => { //
              可以在这里处理图片加载完成的逻辑 } // 处理图片加载失败 const
              handleImageError = (image: any) => { //
              可以在这里处理图片加载失败的逻辑 image.error = true } //
              处理标签页切换 const handleTabChange = () => {
              activeCategory.value = null loadImages() } // 选择分类 const
              selectCategory = (categoryId: number) => { activeCategory.value =
              categoryId pagination.current = 1 loadImages() } // 上传前检查
              const beforeUpload = (file: File) => { // 检查文件类型 const
              isImage = /^image\//.test(file.type); if (!isImage) {
              ElMessage.error('请上传图片文件'); return false; } // 检查文件大小
              const isLt10M = file.size / 1024 / 1024 < 10; if (!isLt10M) {
              ElMessage.error('图片大小不能超过 10MB'); return false; }
              uploadFileName.value = file.name; uploading.value = true;
              uploadProgress.value = 0; uploadStatus.value = undefined; return
              true; } // 处理上传进度 const handleUploadProgress = (event: any)
              => { uploadProgress.value = Math.floor(event.percent); } //
              处理上传成功 const handleUploadSuccess = () => {
              uploadStatus.value = 'success'; ElMessage.success('上传成功');
              setTimeout(() => { uploading.value = false; loadImages(); },
              1000); } // 处理上传失败 const handleUploadError = () => {
              uploadStatus.value = 'exception'; ElMessage.error('上传失败');
              setTimeout(() => { uploading.value = false; }, 1000); } //
              处理弹窗关闭 const handleClosed = () => { resetSelection()
              uploadStatus.value = undefined uploading.value = false
              };}...</span
            >
            <span>{{ uploadProgress }}%</span>
          </div>
          <el-progress
            :percentage="uploadProgress"
            :status="uploadStatus"
            :stroke-width="6"
          />
        </div>
      </div>

      <!-- 图片管理 -->
      <div class="image-management">
        <div class="management-header">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="我的图片" name="my" />
            <el-tab-pane label="系统素材" name="system" v-if="showSystemTab" />
            <el-tab-pane label="上传历史" name="history" />
          </el-tabs>

          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索图片"
              clearable
              size="small"
              style="width: 200px; margin-right: 12px"
              @input="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-button size="small" @click="refresh" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>

        <!-- 图片分类 -->
        <div class="image-categories" v-if="categories.length > 0">
          <div class="categories-scroll">
            <div
              v-for="category in categories"
              :key="category.id"
              class="category-item"
              :class="{ active: activeCategory === category.id }"
              @click="selectCategory(category.id)"
            >
              {{ category.name }}
              <span class="count">({{ category.count }})</span>
            </div>
          </div>
        </div>

        <!-- 图片列表 -->
        <div class="image-list-container">
          <div v-if="filteredImages.length === 0" class="empty-state">
            <el-empty :description="emptyDescription" />
          </div>

          <div v-else class="image-grid">
            <div
              v-for="image in filteredImages"
              :key="image.id"
              class="image-item"
              :class="{ selected: selectedImages.includes(image.id) }"
              @click="toggleSelect(image)"
              @dblclick="handleDoubleClick(image)"
            >
              <!-- 图片容器 -->
              <div class="image-wrapper">
                <img
                  :src="getImageUrl(image)"
                  :alt="image.name"
                  loading="lazy"
                  @load="handleImageLoad(image)"
                  @error="handleImageError(image)"
                />

                <!-- 选中标记 -->
                <div
                  v-if="selectedImages.includes(image.id)"
                  class="selected-mark"
                >
                  <el-icon><Check /></el-icon>
                </div>

                <!-- 图片信息遮罩 -->
                <div class="image-overlay">
                  <div class="overlay-content">
                    <div class="image-name" :title="image.name">
                      {{ image.name }}
                    </div>
                    <div class="image-size">
                      {{ formatFileSize(image.size) }}
                    </div>
                    <div class="image-actions">
                      <el-button
                        size="small"
                        circle
                        @click.stop="previewImage(image)"
                        title="预览"
                      >
                        <el-icon><ZoomIn /></el-icon>
                      </el-button>
                      <el-button
                        size="small"
                        circle
                        type="danger"
                        @click.stop="deleteImage(image)"
                        title="删除"
                        v-if="isMyImage(image)"
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 图片名称和元数据 -->
              <div class="image-info">
                <div class="image-name" :title="image.name">
                  {{ image.name }}
                </div>
                <div class="image-meta">
                  <span>{{ image.width }}×{{ image.height }}</span>
                  <span>{{ formatDate(image.createTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div
          class="pagination-wrapper"
          v-if="pagination.total > pagination.pageSize"
        >
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[20, 40, 60, 80]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 图片预览弹窗 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="80%" top="5vh">
      <div class="image-preview">
        <div class="preview-main">
          <img
            :src="previewImageUrl"
            :alt="previewImageName"
            class="preview-image"
          />
        </div>

        <div class="preview-info">
          <h3>{{ previewImageName }}</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="图片名称">
              {{ previewImageName }}
            </el-descriptions-item>
            <el-descriptions-item label="图片尺寸">
              {{ previewImageWidth }} × {{ previewImageHeight }} 像素
            </el-descriptions-item>
            <el-descriptions-item label="文件大小">
              {{ formatFileSize(previewImageSize) }}
            </el-descriptions-item>
            <el-descriptions-item label="上传时间">
              {{ formatDate(previewImageTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="图片地址" v-if="previewImageUrl">
              <el-input :value="previewImageUrl" readonly size="small">
                <template #append>
                  <el-button size="small" @click="copyImageUrl">
                    复制
                  </el-button>
                </template>
              </el-input>
            </el-descriptions-item>
          </el-descriptions>

          <div class="preview-actions">
            <el-button type="primary" @click="selectPreviewImage">
              使用此图片
            </el-button>
            <el-button @click="previewVisible = false"> 关闭 </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button
          type="primary"
          @click="handleConfirm"
          :disabled="selectedImages.length === 0"
        >
          确认选择 ({{ selectedImages.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { memorialApi } from "@/api/memorial";
import { useUserStore } from "@/store/user";
import dayjs from "dayjs";
import { copyText } from "@/utils/clipboard";
import {
  ElUpload,
  ElProgress,
  ElInput,
  ElButton,
  ElTabs,
  ElTabPane,
  ElEmpty,
  ElDialog,
  ElIcon,
  ElPagination,
  ElDescriptions,
  ElDescriptionsItem,
} from "element-plus";
import {
  UploadFilled,
  Search,
  Refresh,
  ZoomIn,
  Delete,
  Check,
} from "@element-plus/icons-vue";

const props = defineProps<{
  modelValue: boolean;
  memorialId?: number;
  title?: string;
  multiple?: boolean;
  maxCount?: number;
  showSystemTab?: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  select: [images: any[]];
}>();

const userStore = useUserStore();
const uploadRef = ref();

// 弹窗控制
const dialogVisible = computed({
  get() {
    return props.modelValue;
  },
  set(value) {
    emit("update:modelValue", value);
  },
});

// 数据状态
const activeTab = ref("my");
const activeCategory = ref<number | null>(null);
const searchKeyword = ref("");
const loading = ref(false);
const uploading = ref(false);
const uploadProgress = ref(0);
const uploadStatus = ref<"success" | "exception" | undefined>();
const uploadFileName = ref("");

// 图片数据
const images = ref<any[]>([]);
const categories = ref<any[]>([]);
const selectedImages = ref<number[]>([]);

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

// 预览相关
const previewVisible = ref(false);
const previewImageUrl = ref("");
const previewImageName = ref("");
const previewImageWidth = ref(0);
const previewImageHeight = ref(0);
const previewImageSize = ref(0);
const previewImageTime = ref("");

// 上传配置
const uploadAction = computed(() => {
  if (props.memorialId) {
    return `/memorial/upload-image/${props.memorialId}`;
  }
  return "/memorial/upload-image/temp";
});

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`,
}));

const uploadData = computed(() => ({
  categoryId: activeCategory.value,
}));

// 计算属性
const title = computed(() => props.title || "选择图片");
const multiple = computed(() => props.multiple ?? true);
const maxCount = computed(() => props.maxCount || 10);

const filteredImages = computed(() => {
  let result = images.value;

  // 按分类筛选
  if (activeCategory.value !== null) {
    result = result.filter((img) => img.categoryId === activeCategory.value);
  }

  // 按关键词搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(
      (img) =>
        img.name.toLowerCase().includes(keyword) ||
        (img.description && img.description.toLowerCase().includes(keyword))
    );
  }

  return result;
});

const emptyDescription = computed(() => {
  if (searchKeyword.value.trim()) {
    return `没有找到包含"${searchKeyword.value}"的图片`;
  }
  if (activeCategory.value !== null) {
    const category = categories.value.find(
      (c) => c.id === activeCategory.value
    );
    return category ? `${category.name}分类下暂无图片` : "该分类下暂无图片";
  }
  return activeTab.value === "my" ? "您还没有上传过图片" : "暂无图片";
});

// 监听弹窗打开
watch(dialogVisible, (newVal) => {
  if (newVal) {
    resetSelection();
    loadCategories();
    loadImages();
  }
});

// 加载分类
const loadCategories = async () => {
  try {
    // 这里应该调用获取分类的API
    // 暂时使用模拟数据
    categories.value = [
      { id: 1, name: "宠物照片", count: 12 },
      { id: 2, name: "风景背景", count: 8 },
      { id: 3, name: "装饰元素", count: 15 },
      { id: 4, name: "文字背景", count: 6 },
    ];
  } catch (error) {
    console.error("加载分类失败:", error);
  }
};

// 加载图片
const loadImages = async () => {
  loading.value = true;

  try {
    // 根据activeTab加载不同来源的图片
    if (activeTab.value === "my") {
      // 加载用户图片
      await loadUserImages();
    } else if (activeTab.value === "system") {
      // 加载系统素材
      await loadSystemImages();
    } else if (activeTab.value === "history") {
      // 加载上传历史
      await loadHistoryImages();
    }
  } catch (error) {
    console.error("加载图片失败:", error);
    ElMessage.error("加载图片失败");
  } finally {
    loading.value = false;
  }
};

// 加载用户图片
const loadUserImages = async () => {
  // 这里应该调用获取用户图片的API
  // 暂时使用模拟数据
  images.value = generateMockImages(25, "user");
  pagination.total = images.value.length;
};

// 加载系统素材
const loadSystemImages = async () => {
  // 这里应该调用获取系统素材的API
  // 暂时使用模拟数据
  images.value = generateMockImages(50, "system");
  pagination.total = images.value.length;
};

// 加载历史图片
const loadHistoryImages = async () => {
  // 这里应该调用获取历史图片的API
  // 暂时使用模拟数据
  images.value = generateMockImages(15, "history");
  pagination.total = images.value.length;
};

// 生成模拟图片数据
const generateMockImages = (count: number, type: string) => {
  const images = [];
  const categories = ["宠物照片", "风景背景", "装饰元素", "文字背景"];
  const sizes = ["small", "medium", "large"];

  for (let i = 1; i <= count; i++) {
    const categoryId = Math.floor(Math.random() * categories.length) + 1;
    const size = sizes[Math.floor(Math.random() * sizes.length)];

    images.push({
      id: type === "user" ? i : 1000 + i,
      name: `${type === "user" ? "我的" : "系统"}图片${i}.jpg`,
      url: `https://picsum.photos/400/300?random=${i}&${size}`,
      thumbnailUrl: `https://picsum.photos/200/150?random=${i}`,
      width: 400,
      height: 300,
      size: Math.floor(Math.random() * 5000000) + 100000, // 100KB - 5MB
      categoryId: categoryId,
      categoryName: categories[categoryId - 1],
      description: `这是一张${categories[categoryId - 1]}图片`,
      createTime: dayjs()
        .subtract(Math.floor(Math.random() * 30), "day")
        .toISOString(),
      isSystem: type === "system",
      uploaderId: type === "user" ? userStore.userInfo?.id : 0,
      uploaderName: type === "user" ? userStore.userInfo?.username : "系统",
    });
  }

  return images;
};

// 获取图片URL
const getImageUrl = (image: any) => {
  return image.thumbnailUrl || image.url;
};

// 检查是否为我的图片
const isMyImage = (image: any) => {
  return !image.isSystem && image.uploaderId === userStore.userInfo?.id;
};

// 重置选择状态
const resetSelection = () => {
  selectedImages.value = [];
};

// 切换选择
const toggleSelect = (image: any) => {
  if (!props.multiple) {
    selectedImages.value = [image.id];
    return;
  }

  const index = selectedImages.value.indexOf(image.id);
  if (index > -1) {
    selectedImages.value.splice(index, 1);
  } else {
    if (selectedImages.value.length >= (props.maxCount || 10)) {
      ElMessage.warning(`最多只能选择${props.maxCount || 10}张图片`);
      return;
    }
    selectedImages.value.push(image.id);
  }
};

// 选择分类
const selectCategory = (categoryId: number | null) => {
  activeCategory.value = categoryId;
  pagination.current = 1;
  // 不需要重新加载，通过computed过滤
};

// 切换标签
const handleTabChange = () => {
  activeCategory.value = null;
  searchKeyword.value = "";
  pagination.current = 1;
  loadImages();
};

// 搜索图片
const handleSearch = () => {
  // 通过computed自动过滤
};

// 刷新
const refresh = () => {
  loadCategories();
  loadImages();
};

// 分页处理
const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size;
  pagination.current = 1;
  loadImages();
};

const handlePageChange = (page: number) => {
  pagination.current = page;
  loadImages();
};

// 上传处理
const beforeUpload = (file: File) => {
  // 检查文件类型
  const validTypes = [
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp",
    "image/bmp",
  ];
  if (!validTypes.includes(file.type)) {
    ElMessage.error("只支持 JPG、PNG、GIF、WEBP、BMP 格式的图片");
    return false;
  }

  // 检查文件大小 (10MB)
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.error("图片大小不能超过 10MB");
    return false;
  }

  // 重置上传状态
  uploading.value = true;
  uploadProgress.value = 0;
  uploadStatus.value = undefined;
  uploadFileName.value = file.name;

  return true;
};

const handleUploadProgress = (event: any) => {
  if (event.total > 0) {
    uploadProgress.value = Math.round((event.loaded / event.total) * 100);
  }
};

const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    uploadProgress.value = 100;
    uploadStatus.value = "success";

    // 添加新图片到列表
    const newImage = {
      id: Date.now(),
      name: uploadFileName.value,
      url: response.data,
      thumbnailUrl: response.data,
      width: 0,
      height: 0,
      size: 0,
      categoryId: activeCategory.value,
      createTime: new Date().toISOString(),
      isSystem: false,
      uploaderId: userStore.userInfo?.id,
      uploaderName: userStore.userInfo?.username,
    };

    images.value.unshift(newImage);

    // 2秒后重置上传状态
    setTimeout(() => {
      uploading.value = false;
      uploadProgress.value = 0;
      uploadStatus.value = undefined;
      uploadFileName.value = "";

      ElMessage.success("图片上传成功");
    }, 2000);
  } else {
    handleUploadError(new Error(response.message || "上传失败"));
  }
};

const handleUploadError = (error: Error) => {
  uploading.value = false;
  uploadProgress.value = 0;
  uploadStatus.value = "exception";

  ElMessage.error(`上传失败: ${error.message}`);
};

// 图片加载处理
const handleImageLoad = (image: any) => {
  // 图片加载成功，可以获取实际尺寸
  const img = new Image();
  img.onload = () => {
    image.width = img.width;
    image.height = img.height;
  };
  img.src = image.url;
};

const handleImageError = (image: any) => {
  console.warn(`图片加载失败: ${image.url}`);
  // 可以设置一个默认的占位图
};

// 图片选择处理
const toggleSelect = (image: any) => {
  const index = selectedImages.value.indexOf(image.id);

  if (index === -1) {
    // 检查是否超过最大选择数量
    if (selectedImages.value.length >= maxCount.value) {
      ElMessage.warning(`最多只能选择 ${maxCount.value} 张图片`);
      return;
    }

    if (multiple.value) {
      selectedImages.value.push(image.id);
    } else {
      selectedImages.value = [image.id];
    }
  } else {
    selectedImages.value.splice(index, 1);
  }
};

const handleDoubleClick = (image: any) => {
  if (!multiple.value) {
    selectedImages.value = [image.id];
    handleConfirm();
  }
};

// 重置选择
const resetSelection = () => {
  selectedImages.value = [];
};

// 删除图片
const deleteImage = async (image: any) => {
  try {
    await ElMessageBox.confirm(
      "确定要删除这张图片吗？删除后无法恢复。",
      "确认删除",
      { type: "warning" }
    );

    // 调用删除API
    // await memorialApi.deleteImage(image.id)

    // 从列表中移除
    const index = images.value.findIndex((img) => img.id === image.id);
    if (index !== -1) {
      images.value.splice(index, 1);
    }

    // 从选中列表中移除
    const selectedIndex = selectedImages.value.indexOf(image.id);
    if (selectedIndex !== -1) {
      selectedImages.value.splice(selectedIndex, 1);
    }

    ElMessage.success("图片删除成功");
  } catch (error) {
    // 用户取消了删除
  }
};

// 预览图片
const previewImage = (image: any) => {
  previewImageUrl.value = image.url;
  previewImageName.value = image.name;
  previewImageWidth.value = image.width;
  previewImageHeight.value = image.height;
  previewImageSize.value = image.size;
  previewImageTime.value = image.createTime;
  previewVisible.value = true;
};

const selectPreviewImage = () => {
  const image = images.value.find((img) => img.url === previewImageUrl.value);
  if (image) {
    if (multiple.value) {
      if (!selectedImages.value.includes(image.id)) {
        if (selectedImages.value.length < maxCount.value) {
          selectedImages.value.push(image.id);
        } else {
          ElMessage.warning(`最多只能选择 ${maxCount.value} 张图片`);
        }
      }
    } else {
      selectedImages.value = [image.id];
    }
  }
  previewVisible.value = false;
};

const copyImageUrl = async () => {
  try {
    await copyText(previewImageUrl.value);
    ElMessage.success("图片地址已复制到剪贴板");
  } catch (error) {
    ElMessage.error("复制失败");
  }
};

// 弹窗关闭处理
const handleClosed = () => {
  resetSelection();
  activeCategory.value = null;
  searchKeyword.value = "";
  uploading.value = false;
  uploadProgress.value = 0;
};

// 确认选择
const handleConfirm = () => {
  const selected = images.value.filter((img) =>
    selectedImages.value.includes(img.id)
  );

  emit("select", selected);
  dialogVisible.value = false;
};

// 取消选择
const handleCancel = () => {
  dialogVisible.value = false;
};

// 预览图片
const previewImage = (image: any) => {
  previewImageUrl.value = getImageUrl(image);
  previewImageName.value = image.name;
  previewImageWidth.value = image.width;
  previewImageHeight.value = image.height;
  previewImageSize.value = image.size;
  previewImageTime.value = image.createTime;
  previewVisible.value = true;
};

const selectPreviewImage = () => {
  const image = allImages.value.find(
    (img) => img.name === previewImageName.value
  );
  if (image) {
    selectedImages.value = [image.id];
    confirmSelection();
  }
};

// 工具函数
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return "0 Bytes";

  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
};

const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format("YYYY-MM-DD HH:mm");
};
</script>

<style scoped lang="scss">
.image-picker-container {
  height: 70vh;
  display: flex;
  flex-direction: column;

  .upload-section {
    margin-bottom: 20px;

    .upload-area {
      :deep(.el-upload) {
        width: 100%;

        .el-upload-dragger {
          width: 100%;
          padding: 40px 20px;

          .upload-icon {
            font-size: 48px;
            color: #409eff;
            margin-bottom: 16px;
          }

          .upload-text {
            .upload-title {
              font-size: 16px;
              color: #303133;
              margin-bottom: 8px;
            }

            .upload-tip {
              font-size: 12px;
              color: #909399;
            }
          }
        }
      }
    }

    .upload-progress {
      margin-top: 16px;

      .progress-info {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        font-size: 14px;
        color: #606266;
      }
    }
  }

  .image-management {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .management-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .header-actions {
        display: flex;
        align-items: center;
      }
    }

    .image-categories {
      margin-bottom: 16px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e4e7ed;

      .categories-scroll {
        display: flex;
        gap: 8px;
        overflow-x: auto;
        padding: 4px 0;

        .category-item {
          flex-shrink: 0;
          padding: 6px 16px;
          border: 1px solid #dcdfe6;
          border-radius: 16px;
          font-size: 13px;
          color: #606266;
          cursor: pointer;
          transition: all 0.2s;
          white-space: nowrap;

          &:hover {
            border-color: #409eff;
            color: #409eff;
          }

          &.active {
            background: #409eff;
            border-color: #409eff;
            color: white;
          }

          .count {
            font-size: 12px;
            color: inherit;
            opacity: 0.8;
          }
        }
      }
    }

    .image-list-container {
      flex: 1;
      overflow-y: auto;
      margin-bottom: 16px;

      .empty-state {
        height: 300px;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .image-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
        gap: 16px;

        .image-item {
          border: 2px solid transparent;
          border-radius: 8px;
          overflow: hidden;
          transition: all 0.3s;
          cursor: pointer;

          &:hover {
            border-color: #409eff;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

            .image-overlay {
              opacity: 1;
            }
          }

          &.selected {
            border-color: #409eff;

            .selected-mark {
              display: flex;
            }
          }

          .image-wrapper {
            position: relative;
            aspect-ratio: 4/3;
            overflow: hidden;
            background: #f5f7fa;

            img {
              width: 100%;
              height: 100%;
              object-fit: cover;
              transition: transform 0.3s;
            }

            &:hover img {
              transform: scale(1.05);
            }

            .selected-mark {
              display: none;
              position: absolute;
              top: 8px;
              right: 8px;
              width: 24px;
              height: 24px;
              background: #409eff;
              border-radius: 50%;
              align-items: center;
              justify-content: center;
              color: white;
              z-index: 2;

              .el-icon {
                font-size: 14px;
              }
            }

            .image-overlay {
              position: absolute;
              top: 0;
              left: 0;
              right: 0;
              bottom: 0;
              background: rgba(0, 0, 0, 0.6);
              opacity: 0;
              transition: opacity 0.3s;
              display: flex;
              align-items: center;
              justify-content: center;

              .overlay-content {
                text-align: center;
                color: white;
                padding: 12px;

                .image-name {
                  font-size: 12px;
                  margin-bottom: 4px;
                  white-space: nowrap;
                  overflow: hidden;
                  text-overflow: ellipsis;
                }

                .image-size {
                  font-size: 11px;
                  opacity: 0.8;
                  margin-bottom: 8px;
                }
              }
            }
          }
        }
      }
    }
  }
.image-actions {
                   display: flex;
                   justify-content: center;
                   gap: 8px;
                 }
               }
             }
           }

           .image-info {
             padding: 8px;
             background: white;

             .image-name {
               font-size: 13px;
               color: #303133;
               margin-bottom: 4px;
               white-space: nowrap;
               overflow: hidden;
               text-overflow: ellipsis;
             }

             .image-meta {
               display: flex;
               justify-content: space-between;
               font-size: 11px;
               color: #909399;
             }
           }
         }
       }
     }

     .pagination-wrapper {
       display: flex;
       justify-content: center;
       padding: 12px 0;
       border-top: 1px solid #e4e7ed;
     }
   }
 }

 .footer-content {
   display: flex;
   justify-content: space-between;
   align-items: center;
   width: 100%;

   .selected-info {
     font-size: 14px;
     color: #606266;
   }
 }

 .image-preview {
   display: flex;
   gap: 24px;

   .preview-main {
     flex: 2;
     display: flex;
     align-items: center;
     justify-content: center;
     min-height: 400px;
     background: #f5f7fa;
     border-radius: 8px;
     overflow: hidden;

     .preview-image {
       max-width: 100%;
       max-height: 70vh;
       object-fit: contain;
     }
   }

   .preview-info {
     flex: 1;
     min-width: 300px;

     h3 {
       margin: 0 0 16px;
       color: #303133;
     }

     .preview-actions {
       margin-top: 24px;
       display: flex;
       gap: 12px;
     }
   }
 }

 @media (max-width: 768px) {
   .image-picker-container {
     height: 60vh;

     .image-management {
       .image-grid {
         grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
       }
     }
   }

   .image-preview {
     flex-direction: column;

     .preview-info {
       min-width: auto;
     }
   }
 }
}
</style>
