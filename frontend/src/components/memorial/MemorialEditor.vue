<template>
  <div class="memorial-editor">
    <!-- 工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
        <el-button type="primary" @click="preview">预览</el-button>
        <el-button type="success" @click="publish" :loading="publishing"
          >发布</el-button
        >
      </div>
      <div class="toolbar-right">
        <el-button @click="addPage">添加页面</el-button>
        <el-button @click="uploadImage">上传图片</el-button>
        <el-select v-model="currentPage" size="small" style="width: 100px">
          <el-option
            v-for="page in pages"
            :key="page.id"
            :label="`第${page.pageNum}页`"
            :value="page.id"
          />
        </el-select>
      </div>
    </div>

    <!-- 编辑区域 -->
    <div class="editor-container">
      <!-- 左侧组件库 -->
      <div class="component-panel">
        <h4>组件库</h4>
        <div class="component-list">
          <div
            v-for="component in components"
            :key="component.type"
            class="component-item"
            draggable="true"
            @dragstart="dragStart(component)"
          >
            <el-icon><component :is="component.icon" /></el-icon>
            <span>{{ component.name }}</span>
          </div>
        </div>
      </div>

      <!-- 中间画布 -->
      <div class="editor-canvas" @dragover.prevent @drop="drop">
        <div class="page-container" :style="canvasStyle">
          <!-- 当前页面 -->
          <div
            v-for="element in currentPageElements"
            :key="element.id"
            class="element"
            :style="getElementStyle(element)"
            @click="selectElement(element)"
            @dblclick="editElement(element)"
          >
            <!-- 文本元素 -->
            <div v-if="element.type === 'text'" class="text-element">
              {{ element.content }}
            </div>

            <!-- 图片元素 -->
            <div v-if="element.type === 'image'" class="image-element">
              <img :src="element.src" :alt="element.alt" />
            </div>

            <!-- 引用元素 -->
            <div v-if="element.type === 'quote'" class="quote-element">
              <blockquote>{{ element.content }}</blockquote>
              <cite v-if="element.author">—— {{ element.author }}</cite>
            </div>
          </div>

          <!-- 选中框 -->
          <div
            v-if="selectedElement"
            class="selection-box"
            :style="getSelectionBoxStyle(selectedElement)"
          >
            <div
              class="resize-handle top-left"
              @mousedown="startResize('top-left')"
            ></div>
            <div
              class="resize-handle top-right"
              @mousedown="startResize('top-right')"
            ></div>
            <div
              class="resize-handle bottom-left"
              @mousedown="startResize('bottom-left')"
            ></div>
            <div
              class="resize-handle bottom-right"
              @mousedown="startResize('bottom-right')"
            ></div>
          </div>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="property-panel">
        <h4>属性设置</h4>
        <div v-if="selectedElement" class="property-form">
          <el-form label-width="80px">
            <!-- 位置大小 -->
            <el-form-item label="位置X">
              <el-input-number
                v-model="selectedElement.x"
                :min="0"
                :max="canvasWidth"
              />
            </el-form-item>
            <el-form-item label="位置Y">
              <el-input-number
                v-model="selectedElement.y"
                :min="0"
                :max="canvasHeight"
              />
            </el-form-item>
            <el-form-item label="宽度">
              <el-input-number
                v-model="selectedElement.width"
                :min="10"
                :max="canvasWidth"
              />
            </el-form-item>
            <el-form-item label="高度">
              <el-input-number
                v-model="selectedElement.height"
                :min="10"
                :max="canvasHeight"
              />
            </el-form-item>

            <!-- 文本属性 -->
            <template v-if="selectedElement.type === 'text'">
              <el-form-item label="内容">
                <el-input
                  v-model="selectedElement.content"
                  type="textarea"
                  :rows="3"
                />
              </el-form-item>
              <el-form-item label="字体大小">
                <el-input-number
                  v-model="selectedElement.fontSize"
                  :min="8"
                  :max="72"
                />
              </el-form-item>
              <el-form-item label="字体颜色">
                <el-color-picker v-model="selectedElement.color" />
              </el-form-item>
            </template>

            <!-- 图片属性 -->
            <template v-if="selectedElement.type === 'image'">
              <el-form-item label="图片URL">
                <el-input v-model="selectedElement.src" />
                <el-button @click="selectImage">选择图片</el-button>
              </el-form-item>
            </template>
          </el-form>
        </div>
        <div v-else class="no-selection">
          <p>请选择一个元素进行编辑</p>
        </div>
      </div>
    </div>

    <!-- 图片选择弹窗 -->
    <el-dialog v-model="imageDialogVisible" title="选择图片">
      <div class="image-selector">
        <div class="image-upload">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :show-file-list="false"
          >
            <el-button type="primary">上传新图片</el-button>
          </el-upload>
        </div>
        <div class="image-list">
          <div
            v-for="image in imageList"
            :key="image.id"
            class="image-item"
            @click="selectImageItem(image)"
          >
            <img
              :src="image.thumbnailUrl || image.imageUrl"
              :alt="image.imageName"
            />
            <p>{{ image.imageName }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import {
  ElButton,
  ElSelect,
  ElOption,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElColorPicker,
  ElDialog,
  ElUpload,
} from "element-plus";
import {
  Plus,
  Upload,
  Image as ImageIcon,
  Edit,
  Text,
  Quote,
} from "@element-plus/icons-vue";
import { memorialApi } from "@/api/memorial";
import type { MemorialContentDTO } from "@/api/memorial";

// 定义类型
interface Page {
  id: number;
  pageNum: number;
  elements: Element[];
}

interface Element {
  id: string;
  type: "text" | "image" | "quote";
  x: number;
  y: number;
  width: number;
  height: number;
  content?: string;
  src?: string;
  alt?: string;
  author?: string;
  fontSize?: number;
  color?: string;
}

interface Component {
  type: string;
  name: string;
  icon: any;
}

interface ImageItem {
  id: number;
  imageUrl: string;
  thumbnailUrl?: string;
  imageName: string;
}

// Props
const props = defineProps<{
  memorialId?: number;
  initialContent?: any;
}>();

// Emits
const emit = defineEmits<{
  (e: "save", data: any): void;
  (e: "publish"): void;
}>();

// State
const currentPage = ref<number>(1);
const pages = ref<Page[]>([]);
const components = ref<Component[]>([
  { type: "text", name: "文本", icon: Text },
  { type: "image", name: "图片", icon: ImageIcon },
  { type: "quote", name: "引用", icon: Quote },
]);
const selectedElement = ref<Element | null>(null);
const imageDialogVisible = ref(false);
const imageList = ref<ImageItem[]>([]);
const saving = ref(false);
const publishing = ref(false);
const canvasWidth = ref(800);
const canvasHeight = ref(1000);
const draggedComponent = ref<Component | null>(null);

// Computed
const currentPageElements = computed(() => {
  const page = pages.value.find((p) => p.id === currentPage.value);
  return page?.elements || [];
});

const canvasStyle = computed(() => ({
  width: `${canvasWidth.value}px`,
  height: `${canvasHeight.value}px`,
}));

// Axios baseURL already adds /api
const uploadUrl = computed(() => {
  const id = props.memorialId ?? "temp";
  return `/memorial/upload-image/${id}`;
});

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${sessionStorage.getItem("token")}`,
}));

// Methods
const dragStart = (component: Component) => {
  draggedComponent.value = component;
};

const drop = (e: DragEvent) => {
  if (!draggedComponent.value) return;

  const canvas = document.querySelector(".editor-canvas") as HTMLElement;
  const rect = canvas.getBoundingClientRect();
  const x = e.clientX - rect.left - 50;
  const y = e.clientY - rect.top - 50;

  // 创建新元素
  const newElement: Element = {
    id: Date.now().toString(),
    type: draggedComponent.value.type as any,
    x: Math.max(0, Math.min(x, canvasWidth.value - 100)),
    y: Math.max(0, Math.min(y, canvasHeight.value - 100)),
    width: 100,
    height: 100,
    content: draggedComponent.value.type === "text" ? "双击编辑文本" : "",
    fontSize: 16,
    color: "#333333",
  };

  // 添加到当前页
  const pageIndex = pages.value.findIndex((p) => p.id === currentPage.value);
  if (pageIndex !== -1) {
    pages.value[pageIndex].elements.push(newElement);
  }

  draggedComponent.value = null;
};

const getElementStyle = (element: Element) => ({
  position: "absolute",
  left: `${element.x}px`,
  top: `${element.y}px`,
  width: `${element.width}px`,
  height: `${element.height}px`,
  fontSize: element.fontSize ? `${element.fontSize}px` : "16px",
  color: element.color || "#333333",
  cursor: "move",
});

const getSelectionBoxStyle = (element: Element) => ({
  position: "absolute",
  left: `${element.x - 5}px`,
  top: `${element.y - 5}px`,
  width: `${element.width + 10}px`,
  height: `${element.height + 10}px`,
  border: "2px dashed #409EFF",
  borderRadius: "4px",
  pointerEvents: "none",
});

const selectElement = (element: Element) => {
  selectedElement.value = { ...element };
};

const editElement = (element: Element) => {
  // 可以打开详细编辑弹窗
  selectedElement.value = { ...element };
};

const addPage = () => {
  const newPageNum = pages.value.length + 1;
  pages.value.push({
    id: newPageNum,
    pageNum: newPageNum,
    elements: [],
  });
  currentPage.value = newPageNum;
};

const uploadImage = () => {
  imageDialogVisible.value = true;
  loadImageList();
};

const selectImage = () => {
  imageDialogVisible.value = true;
};

const loadImageList = async () => {
  if (!props.memorialId) return;
  try {
    const res = await memorialApi.getMemorialImages(props.memorialId);
    imageList.value = res.data.data;
  } catch (error) {
    console.error("加载图片列表失败", error);
  }
};

const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    imageList.value.push(response.data);
  }
};

const selectImageItem = (image: ImageItem) => {
  if (selectedElement.value && selectedElement.value.type === "image") {
    selectedElement.value.src = image.imageUrl;
    // 更新到页面元素
    updateElementInPage(selectedElement.value);
  }
  imageDialogVisible.value = false;
};

const selectImage = () => {
  imageDialogVisible.value = true;
};

const updateElementInPage = (updatedElement: Element) => {
  const pageIndex = pages.value.findIndex((p) => p.id === currentPage.value);
  if (pageIndex === -1) return;

  const elementIndex = pages.value[pageIndex].elements.findIndex(
    (e) => e.id === updatedElement.id
  );
  if (elementIndex !== -1) {
    pages.value[pageIndex].elements[elementIndex] = updatedElement;
  }
};

const saveDraft = async () => {
  if (!props.memorialId) return;

  saving.value = true;
  try {
    const contentData = {
      pages: pages.value,
    };

    const data: MemorialContentDTO = {
      memorialId: props.memorialId,
      contentData,
    };

    await memorialApi.saveContent(data);
    ElMessage.success("草稿保存成功");
    emit("save", contentData);
  } catch (error) {
    console.error("保存草稿失败", error);
    ElMessage.error("保存草稿失败，请重试");
  } finally {
    saving.value = false;
  }
};

const publish = async () => {
  if (!props.memorialId) return;

  publishing.value = true;
  try {
    const contentData = {
      pages: pages.value,
    };

    // 先保存内容
    await memorialApi.saveContent({
      memorialId: props.memorialId,
      contentData,
    });

    // 再发布
    await memorialApi.publishMemorial({
      memorialId: props.memorialId,
      isPublic: true,
    });

    ElMessage.success("发布成功");
    emit("publish");
  } catch (error) {
    console.error("发布失败", error);
    ElMessage.error("发布失败，请重试");
  } finally {
    publishing.value = false;
  }
};

const preview = () => {
  // 打开预览窗口
  const previewData = {
    pages: pages.value,
  };
  window.open(
    `/preview?data=${encodeURIComponent(JSON.stringify(previewData))}`,
    "_blank"
  );
};

const startResize = (e: MouseEvent) => {
  // 实现调整大小逻辑
  e.preventDefault();
  // 这里可以添加拖拽调整元素大小的逻辑
};

// Watch selectedElement changes to update page elements
watch(selectedElement, (newVal) => {
  if (newVal) {
    updateElementInPage(newVal);
  }
});

// Watch currentPage changes
watch(currentPage, (newVal) => {
  selectedElement.value = null;
});

onMounted(() => {
  // 初始化页面
  if (props.initialContent && props.initialContent.pages) {
    pages.value = props.initialContent.pages;
    currentPage.value = pages.value[0]?.id || 1;
  } else {
    pages.value = [
      {
        id: 1,
        pageNum: 1,
        elements: [],
      },
    ];
    currentPage.value = 1;
  }
});
</script>

<style scoped lang="scss">
.memorial-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  padding: 10px 20px;
  background-color: #f5f5f5;
  border-bottom: 1px solid #e5e5e5;

  .toolbar-left,
  .toolbar-right {
    display: flex;
    gap: 10px;
    align-items: center;
  }
}

.editor-container {
  display: flex;
  flex: 1;
  height: calc(100vh - 60px);
  overflow: hidden;
}

.component-panel {
  width: 200px;
  border-right: 1px solid #e5e5e5;
  padding: 15px;
  overflow-y: auto;

  h4 {
    margin-bottom: 15px;
    color: #333;
  }

  .component-list {
    display: grid;
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .component-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    border: 1px solid #e5e5e5;
    border-radius: 4px;
    cursor: move;

    &:hover {
      background-color: #f0f9ff;
      border-color: #91d5ff;
    }
  }
}

.editor-canvas {
  flex: 1;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fafafa;
  overflow: auto;

  .page-container {
    background-color: white;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
    position: relative;
  }

  .element {
    user-select: none;

    &.selected {
      outline: 2px solid #409eff;
    }
  }

  .text-element {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .image-element {
    width: 100%;
    height: 100%;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .quote-element {
    width: 100%;
    height: 100%;
    padding: 20px;

    blockquote {
      margin: 0;
      padding-left: 15px;
      border-left: 3px solid #ccc;
    }

    cite {
      display: block;
      margin-top: 5px;
      text-align: right;
      color: #666;
    }
  }

  .selection-box {
    .resize-handle {
      position: absolute;
      width: 15px;
      height: 15px;
      background-color: #409eff;
      border-radius: 50%;
      bottom: -7px;
      right: -7px;
      cursor: se-resize;
    }
  }
}

.property-panel {
  width: 300px;
  border-left: 1px solid #e5e5e5;
  padding: 15px;
  overflow-y: auto;

  h4 {
    margin-bottom: 15px;
    color: #333;
  }

  .property-form {
    .el-form-item {
      margin-bottom: 15px;
    }
  }

  .no-selection {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #999;
  }
}

.image-selector {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 500px;
  overflow: hidden;

  .image-upload {
    margin-bottom: 10px;
  }

  .image-list {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
    overflow-y: auto;
    max-height: 400px;

    .image-item {
      border: 1px solid #e5e5e5;
      border-radius: 4px;
      padding: 10px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #409eff;
      }

      img {
        width: 100%;
        height: 100px;
        object-fit: cover;
        border-radius: 4px;
      }

      p {
        text-align: center;
        margin-top: 5px;
        font-size: 12px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}
</style>
