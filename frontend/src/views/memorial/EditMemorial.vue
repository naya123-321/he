<template>
  <div class="memorial-edit-container">
    <div class="page-header">
      <h1>编辑纪念册</h1>
      <div class="header-actions">
        <el-button @click="goBack">返回</el-button>
        <el-button type="primary" @click="saveMemorial">保存</el-button>
      </div>
    </div>

    <div class="edit-content">
      <!-- 左侧工具栏 -->
      <div class="editor-left">
        <div class="tool-panel">
          <h3>工具箱</h3>
          <div class="tool-group">
            <h4>基础元素</h4>
            <div class="tool-list">
              <div class="tool-item" @click="addElement('text')">
                <el-icon><Document /></el-icon>
                <span>文本</span>
              </div>
              <div class="tool-item" @click="addElement('image')">
                <el-icon><Picture /></el-icon>
                <span>图片</span>
              </div>
              <div class="tool-item" @click="addElement('quote')">
                <el-icon><Stamp /></el-icon>
                <span>引用</span>
              </div>
              <div class="tool-item" @click="addElement('timeline')">
                <el-icon><Timer /></el-icon>
                <span>时间线</span>
              </div>
            </div>
          </div>

          <div class="tool-group">
            <h4>布局</h4>
            <div class="tool-list">
              <div class="tool-item" @click="addElement('grid')">
                <el-icon><Folder /></el-icon>
                <span>网格</span>
              </div>
              <div class="tool-item" @click="addElement('divider')">
                <el-icon><Minus /></el-icon>
                <span>分隔线</span>
              </div>
            </div>
          </div>
        </div>

        <div class="page-panel">
          <h3>页面管理</h3>
          <div class="page-list">
            <div
              v-for="(page, index) in pages"
              :key="page.id"
              class="page-item"
              :class="{ active: currentPageIndex === index }"
              @click="switchPage(index)"
            >
              <div class="page-thumbnail">
                <img :src="page.thumbnail" :alt="`页面${index + 1}`" />
              </div>
              <div class="page-info">
                <span>页面 {{ index + 1 }}</span>
                <el-button
                  v-if="pages.length > 1"
                  size="small"
                  type="danger"
                  link
                  @click.stop="deletePage(index)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
          <el-button class="add-page-btn" @click="addPage">
            <el-icon><Plus /></el-icon>
            添加页面
          </el-button>
        </div>
      </div>

      <!-- 中间画布区域 -->
      <div class="editor-center">
        <div class="canvas-toolbar">
          <div class="zoom-controls">
            <el-button size="small" @click="zoomOut">
              <el-icon><Minus /></el-icon>
            </el-button>
            <span class="zoom-info">{{ Math.round(zoomLevel * 100) }}%</span>
            <el-button size="small" @click="zoomIn">
              <el-icon><Plus /></el-icon>
            </el-button>
            <el-button size="small" @click="resetZoom">
              <el-icon><CaretRight /></el-icon>
            </el-button>
          </div>

          <div class="view-controls">
            <el-button size="small" @click="toggleGrid">
              <el-icon><View /></el-icon>
              网格
            </el-button>
            <el-button size="small" @click="toggleRuler">
              <el-icon><Document /></el-icon>
              标尺
            </el-button>
          </div>
        </div>

        <div class="canvas-container" ref="canvasContainer">
          <div
            class="canvas-wrapper"
            :style="{ transform: `scale(${zoomLevel})` }"
          >
            <div
              class="canvas"
              ref="canvas"
              :style="{
                width: `${canvasWidth}px`,
                height: `${canvasHeight}px`,
                backgroundColor: pageStyle.backgroundColor,
                backgroundImage: pageStyle.backgroundImage
                  ? `url(${pageStyle.backgroundImage})`
                  : 'none',
                backgroundSize: 'cover',
                backgroundPosition: 'center',
              }"
              @click="clearSelection"
            >
              <!-- 网格背景 -->
              <div v-if="showGrid" class="grid-overlay"></div>

              <!-- 渲染元素 -->
              <div
                v-for="element in currentPageElements"
                :key="element.id"
                class="element"
                :class="{
                  selected: selectedElement?.id === element.id,
                  'element-text': element.type === 'text',
                  'element-image': element.type === 'image',
                  'element-quote': element.type === 'quote',
                  'element-timeline': element.type === 'timeline',
                }"
                :style="{
                  position: 'absolute',
                  left: `${element.x}px`,
                  top: `${element.y}px`,
                  width: `${element.width}px`,
                  height: `${element.height}px`,
                  transform: `rotate(${element.rotate || 0}deg)`,
                  zIndex: element.zIndex,
                }"
                @click.stop="selectElement(element)"
                @mousedown="startDrag($event, element)"
              >
                <!-- 文本元素 -->
                <div v-if="element.type === 'text'" class="element-text">
                  <div
                    :style="{
                      fontFamily: element.fontFamily,
                      fontSize: `${element.fontSize}px`,
                      color: element.color,
                      textAlign: element.textAlign,
                      lineHeight: element.lineHeight,
                    }"
                  >
                    {{ element.content }}
                  </div>
                </div>

                <!-- 图片元素 -->
                <div v-if="element.type === 'image'" class="element-image">
                  <img
                    :src="element.src"
                    :style="{
                      objectFit: element.objectFit,
                      borderWidth: `${element.borderWidth}px`,
                      borderColor: element.borderColor,
                      borderRadius: `${element.borderRadius}px`,
                    }"
                  />
                  <div class="image-overlay">
                    <el-button size="small" @click.stop="replaceImage(element)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                  </div>
                </div>

                <!-- 引用元素 -->
                <div v-if="element.type === 'quote'" class="element-quote">
                  <blockquote>
                    {{ element.content }}
                  </blockquote>
                  <cite>{{ element.author }}</cite>
                </div>

                <!-- 时间线元素 -->
                <div
                  v-if="element.type === 'timeline'"
                  class="element-timeline"
                >
                  <div class="timeline-title">{{ element.title }}</div>
                  <div>{{ element.content }}</div>
                  <div class="timeline-date">{{ element.date }}</div>
                </div>

                <!-- 选中元素的调整手柄 -->
                <div
                  v-if="selectedElement?.id === element.id"
                  class="resize-handles"
                >
                  <div
                    class="handle top-left"
                    @mousedown.stop="startResize($event, element, 'top-left')"
                  ></div>
                  <div
                    class="handle top-right"
                    @mousedown.stop="startResize($event, element, 'top-right')"
                  ></div>
                  <div
                    class="handle bottom-left"
                    @mousedown.stop="
                      startResize($event, element, 'bottom-left')
                    "
                  ></div>
                  <div
                    class="handle bottom-right"
                    @mousedown.stop="
                      startResize($event, element, 'bottom-right')
                    "
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="editor-right">
        <div class="property-panel" v-if="selectedElement">
          <h3>元素属性</h3>
          <el-tabs v-model="activePropertyTab" class="property-tabs">
            <!-- 基础属性 -->
            <el-tab-pane label="基础" name="basic">
              <el-form label-width="80px">
                <el-form-item label="位置X">
                  <el-input-number
                    v-model="selectedElement!.x"
                    :min="0"
                    :max="canvasWidth"
                    @change="updateElementPosition"
                  />
                </el-form-item>

                <el-form-item label="位置Y">
                  <el-input-number
                    v-model="selectedElement!.y"
                    :min="0"
                    :max="canvasHeight"
                    @change="updateElementPosition"
                  />
                </el-form-item>

                <el-form-item label="宽度">
                  <el-input-number
                    v-model="selectedElement!.width"
                    :min="20"
                    :max="canvasWidth"
                    @change="updateElementSize"
                  />
                </el-form-item>

                <el-form-item label="高度">
                  <el-input-number
                    v-model="selectedElement!.height"
                    :min="20"
                    :max="canvasHeight"
                    @change="updateElementSize"
                  />
                </el-form-item>

                <el-form-item label="旋转角度">
                  <el-input-number
                    v-model="selectedElement!.rotate"
                    :min="0"
                    :max="360"
                    @change="updateElementTransform"
                  />
                  <template #append>°</template>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <!-- 样式属性 -->
            <el-tab-pane
              label="样式"
              name="style"
              v-if="
                selectedElement?.type === 'text' ||
                selectedElement?.type === 'quote'
              "
            >
              <el-form label-width="80px">
                <el-form-item label="字体">
                  <el-select
                    v-model="selectedElement!.fontFamily"
                    @change="updateElementStyle"
                  >
                    <el-option label="微软雅黑" value="Microsoft YaHei" />
                    <el-option label="宋体" value="SimSun" />
                    <el-option label="黑体" value="SimHei" />
                    <el-option label="楷体" value="KaiTi" />
                    <el-option label="Arial" value="Arial" />
                  </el-select>
                </el-form-item>

                <el-form-item label="字号">
                  <el-input-number
                    v-model="selectedElement!.fontSize"
                    :min="8"
                    :max="72"
                    @change="updateElementStyle"
                  />
                </el-form-item>

                <el-form-item label="颜色">
                  <el-color-picker
                    v-model="selectedElement!.color"
                    @change="updateElementStyle"
                  />
                </el-form-item>

                <el-form-item label="对齐">
                  <el-radio-group
                    v-model="selectedElement!.textAlign"
                    @change="updateElementStyle"
                  >
                    <el-radio label="left">左对齐</el-radio>
                    <el-radio label="center">居中</el-radio>
                    <el-radio label="right">右对齐</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="行高">
                  <el-slider
                    v-model="selectedElement!.lineHeight"
                    :min="1"
                    :max="3"
                    :step="0.1"
                    @change="updateElementStyle"
                  />
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 图片属性 -->
            <el-tab-pane
              label="图片"
              name="image"
              v-if="selectedElement?.type === 'image'"
            >
              <el-form label-width="80px">
                <el-form-item label="图片源">
                  <el-input v-model="selectedElement!.src" readonly>
                    <template #append>
                      <el-button @click="replaceImage(selectedElement!)">
                        替换
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="显示方式">
                  <el-radio-group
                    v-model="selectedElement!.objectFit"
                    @change="updateElementStyle"
                  >
                    <el-radio label="fill">填充</el-radio>
                    <el-radio label="contain">包含</el-radio>
                    <el-radio label="cover">覆盖</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="边框">
                  <el-input-number
                    v-model="selectedElement!.borderWidth"
                    :min="0"
                    :max="20"
                    @change="updateElementStyle"
                  />
                  <template #append>px</template>
                </el-form-item>

                <el-form-item label="边框颜色">
                  <el-color-picker
                    v-model="selectedElement!.borderColor"
                    @change="updateElementStyle"
                  />
                </el-form-item>

                <el-form-item label="圆角">
                  <el-input-number
                    v-model="selectedElement!.borderRadius"
                    :min="0"
                    :max="50"
                    @change="updateElementStyle"
                  />
                  <template #append>px</template>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="layer-panel">
          <h3>图层管理</h3>
          <div class="layer-list">
            <div
              v-for="(element, index) in currentPageElements"
              :key="element.id"
              class="layer-item"
              :class="{ active: selectedElement?.id === element.id }"
              @click="selectElement(element)"
            >
              <div class="layer-info">
                <el-icon>
                  <Document v-if="element.type === 'text'" />
                  <Picture v-else-if="element.type === 'image'" />
                  <Stamp v-else-if="element.type === 'quote'" />
                  <Timer v-else-if="element.type === 'timeline'" />
                  <Folder v-else />
                </el-icon>
                <span class="layer-name">{{ getElementName(element) }}</span>
              </div>
              <div class="layer-actions">
                <el-button
                  size="small"
                  @click.stop="moveLayerUp(index)"
                  :disabled="index === 0"
                >
                  <el-icon><CaretRight /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  @click.stop="moveLayerDown(index)"
                  :disabled="index === currentPageElements.length - 1"
                >
                  <el-icon><CaretRight /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  @click.stop="deleteElement(element)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片上传对话框 -->
    <el-dialog v-model="imageUploadVisible" title="选择图片" width="600px">
      <div class="image-upload-container">
        <el-upload
          class="image-uploader"
          action="#"
          :show-file-list="false"
          :before-upload="beforeImageUpload"
          :http-request="uploadImage"
        >
          <el-button type="primary">点击上传</el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持 jpg/png/gif 格式，大小不超过 5MB
            </div>
          </template>
        </el-upload>

        <div class="image-library">
          <h4>图片库</h4>
          <div class="image-grid">
            <div
              v-for="image in imageLibrary"
              :key="image.id"
              class="image-item"
              @click="selectImage(image)"
            >
              <img :src="image.url" :alt="image.name" />
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, Ref } from "vue";
import { memorialApi } from "@/api/memorial";
import { ElMessageBox, ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import {
  Folder,
  View,
  Edit,
  Delete,
  Plus,
  Picture,
  FullScreen,
  Document,
  Minus,
  CaretRight,
  Stamp,
  Timer,
} from "@element-plus/icons-vue";

// 定义类型
interface MemorialInfo {
  id: string;
  title: string;
  status: number;
  statusText: string;
  // 其他纪念册信息字段
}

interface Element {
  id: string;
  type: "text" | "image" | "quote" | "timeline" | "grid" | "divider";
  x: number;
  y: number;
  width: number;
  height: number;
  rotate?: number;
  zIndex: number;
  // 文本元素属性
  content?: string;
  fontFamily?: string;
  fontSize?: number;
  color?: string;
  textAlign?: "left" | "center" | "right";
  lineHeight?: number;
  // 图片元素属性
  src?: string;
  objectFit?: "fill" | "contain" | "cover";
  borderWidth?: number;
  borderColor?: string;
  borderRadius?: number;
  // 引用元素属性
  author?: string;
  // 时间线元素属性
  title?: string;
  date?: string;
}

interface Page {
  id: string;
  elements: Element[];
  thumbnail: string;
}

// 路由相关
const route = useRoute();
const router = useRouter();
const memorialId = route.query.id as string;

// 响应式数据
const memorialInfo = ref<MemorialInfo>({
  id: "",
  title: "",
  status: 0,
  statusText: "",
});

const pages = ref<Page[]>([
  {
    id: "page-1",
    elements: [],
    thumbnail: "",
  },
]);

const currentPageIndex = ref(0);
const selectedElement = ref<Element | null>(null);
const activePropertyTab = ref("basic");

// 画布相关
const canvasWidth = 800;
const canvasHeight = 1000;
const zoomLevel = ref(1);
const showGrid = ref(false);
const showRuler = ref(false);
const canvasContainer = ref<HTMLElement | null>(null);
const canvas = ref<HTMLElement | null>(null);

// 页面样式
const pageStyle = reactive({
  backgroundColor: "#ffffff",
  backgroundImage: "",
});

// 图片上传相关
const imageUploadVisible = ref(false);
const imageLibrary = ref([
  { id: "1", name: "示例图片1", url: "/images/sample1.jpg" },
  { id: "2", name: "示例图片2", url: "/images/sample2.jpg" },
  { id: "3", name: "示例图片3", url: "/images/sample3.jpg" },
]);

// 拖拽相关
const isDragging = ref(false);
const isResizing = ref(false);
const dragStartX = ref(0);
const dragStartY = ref(0);
const elementStartX = ref(0);
const elementStartY = ref(0);
const resizeHandle = ref("");

// 计算属性
const currentPage = computed(() => pages.value[currentPageIndex.value]);
const currentPageElements = computed(() => currentPage.value?.elements || []);

// 初始化
onMounted(() => {
  if (memorialId) {
    loadMemorialData();
  }
});

// 加载纪念册数据
const loadMemorialData = async () => {
  try {
    const res = await memorialApi.getMemorialDetail(memorialId);
    if (res.data.code === 200) {
      memorialInfo.value = res.data.data;
      // 加载页面数据
      // 这里需要根据实际API返回的数据结构来处理
    }
  } catch (error) {
    console.error("加载纪念册数据失败:", error);
    ElMessage.error("加载纪念册数据失败");
  }
};

// 保存纪念册
const saveMemorial = async () => {
  try {
    // 构建保存数据
    const saveData = {
      id: memorialInfo.value.id,
      title: memorialInfo.value.title,
      pages: pages.value,
    };

    const res = await memorialApi.updateMemorial(saveData);
    if (res.data.code === 200) {
      ElMessage.success("保存成功");
    }
  } catch (error) {
    console.error("保存失败:", error);
    ElMessage.error("保存失败");
  }
};

// 返回
const goBack = () => {
  router.back();
};

// 添加元素
const addElement = (type: Element["type"]) => {
  const newElement: Element = {
    id: `element-${Date.now()}`,
    type,
    x: 100,
    y: 100,
    width: type === "text" ? 200 : 150,
    height: type === "text" ? 50 : 150,
    zIndex: currentPageElements.value.length,
    // 设置默认属性
    content:
      type === "text" ? "请输入文本" : type === "quote" ? "请输入引用内容" : "",
    fontFamily: "Microsoft YaHei",
    fontSize: 16,
    color: "#333333",
    textAlign: "left",
    lineHeight: 1.5,
    src: type === "image" ? "/default-image.jpg" : undefined,
    objectFit: "cover",
    borderWidth: 0,
    borderColor: "#000000",
    borderRadius: 0,
    author: type === "quote" ? "作者" : "",
    title: type === "timeline" ? "事件标题" : "",
    date: type === "timeline" ? new Date().toISOString().split("T")[0] : "",
  };

  currentPage.value.elements.push(newElement);
  selectElement(newElement);
};

// 选择元素
const selectElement = (element: Element) => {
  selectedElement.value = element;
};

// 清除选择
const clearSelection = () => {
  selectedElement.value = null;
};

// 删除元素
const deleteElement = (element: Element) => {
  ElMessageBox.confirm("确定要删除此元素吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      const index = currentPageElements.value.findIndex(
        (el) => el.id === element.id
      );
      if (index !== -1) {
        currentPageElements.value.splice(index, 1);
        if (selectedElement.value?.id === element.id) {
          selectedElement.value = null;
        }
        ElMessage.success("删除成功");
      }
    })
    .catch(() => {
      // 用户取消
    });
};

// 开始拖拽
const startDrag = (event: MouseEvent, element: Element) => {
  isDragging.value = true;
  dragStartX.value = event.clientX;
  dragStartY.value = event.clientY;
  elementStartX.value = element.x;
  elementStartY.value = element.y;

  document.addEventListener("mousemove", onDrag);
  document.addEventListener("mouseup", stopDrag);
};

// 拖拽中
const onDrag = (event: MouseEvent) => {
  if (!isDragging.value || !selectedElement.value) return;

  const deltaX = (event.clientX - dragStartX.value) / zoomLevel.value;
  const deltaY = (event.clientY - dragStartY.value) / zoomLevel.value;

  selectedElement.value.x = Math.max(
    0,
    Math.min(
      canvasWidth - selectedElement.value.width,
      elementStartX.value + deltaX
    )
  );
  selectedElement.value.y = Math.max(
    0,
    Math.min(
      canvasHeight - selectedElement.value.height,
      elementStartY.value + deltaY
    )
  );
};

// 停止拖拽
const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener("mousemove", onDrag);
  document.removeEventListener("mouseup", stopDrag);
};

// 开始调整大小
const startResize = (event: MouseEvent, element: Element, handle: string) => {
  isResizing.value = true;
  resizeHandle.value = handle;
  dragStartX.value = event.clientX;
  dragStartY.value = event.clientY;
  elementStartX.value = element.x;
  elementStartY.value = element.y;

  document.addEventListener("mousemove", onResize);
  document.addEventListener("mouseup", stopResize);
};

// 调整大小中
const onResize = (event: MouseEvent) => {
  if (!isResizing.value || !selectedElement.value) return;

  const deltaX = (event.clientX - dragStartX.value) / zoomLevel.value;
  const deltaY = (event.clientY - dragStartY.value) / zoomLevel.value;

  switch (resizeHandle.value) {
    case "top-left":
      selectedElement.value.x = Math.min(
        elementStartX.value + deltaX,
        elementStartX.value + selectedElement.value.width - 20
      );
      selectedElement.value.y = Math.min(
        elementStartY.value + deltaY,
        elementStartY.value + selectedElement.value.height - 20
      );
      selectedElement.value.width = Math.max(
        20,
        selectedElement.value.width - deltaX
      );
      selectedElement.value.height = Math.max(
        20,
        selectedElement.value.height - deltaY
      );
      break;
    case "top-right":
      selectedElement.value.y = Math.min(
        elementStartY.value + deltaY,
        elementStartY.value + selectedElement.value.height - 20
      );
      selectedElement.value.width = Math.max(
        20,
        selectedElement.value.width + deltaX
      );
      selectedElement.value.height = Math.max(
        20,
        selectedElement.value.height - deltaY
      );
      break;
    case "bottom-left":
      selectedElement.value.x = Math.min(
        elementStartX.value + deltaX,
        elementStartX.value + selectedElement.value.width - 20
      );
      selectedElement.value.width = Math.max(
        20,
        selectedElement.value.width - deltaX
      );
      selectedElement.value.height = Math.max(
        20,
        selectedElement.value.height + deltaY
      );
      break;
    case "bottom-right":
      selectedElement.value.width = Math.max(
        20,
        selectedElement.value.width + deltaX
      );
      selectedElement.value.height = Math.max(
        20,
        selectedElement.value.height + deltaY
      );
      break;
  }
};

// 停止调整大小
const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener("mousemove", onResize);
  document.removeEventListener("mouseup", stopResize);
};

// 更新元素位置
const updateElementPosition = () => {
  // 位置更新已通过v-model绑定自动完成
};

// 更新元素大小
const updateElementSize = () => {
  // 大小更新已通过v-model绑定自动完成
};

// 更新元素变换
const updateElementTransform = () => {
  // 变换更新已通过v-model绑定自动完成
};

// 更新元素样式
const updateElementStyle = () => {
  // 样式更新已通过v-model绑定自动完成
};

// 替换图片
const replaceImage = (element: Element) => {
  selectedElement.value = element;
  imageUploadVisible.value = true;
};

// 上传图片前检查
const beforeImageUpload = (file: File) => {
  const isJPG =
    file.type === "image/jpeg" ||
    file.type === "image/png" ||
    file.type === "image/gif";
  const isLt5M = file.size / 1024 / 1024 < 5;

  if (!isJPG) {
    ElMessage.error("上传图片只能是 JPG/PNG/GIF 格式!");
    return false;
  }
  if (!isLt5M) {
    ElMessage.error("上传图片大小不能超过 5MB!");
    return false;
  }

  return true;
};

// 上传图片
const uploadImage = async (options: any) => {
  const { file } = options;

  try {
    // 这里应该调用实际的图片上传API
    // const res = await uploadApi.uploadImage(file);
    // const imageUrl = res.data.url;

    // 模拟上传成功
    const imageUrl = URL.createObjectURL(file);

    if (selectedElement.value && selectedElement.value.type === "image") {
      selectedElement.value.src = imageUrl;
    }

    imageUploadVisible.value = false;
    ElMessage.success("图片上传成功");
  } catch (error) {
    console.error("图片上传失败:", error);
    ElMessage.error("图片上传失败");
  }
};

// 选择图片库中的图片
const selectImage = (image: { id: string; name: string; url: string }) => {
  if (selectedElement.value && selectedElement.value.type === "image") {
    selectedElement.value.src = image.url;
  }
  imageUploadVisible.value = false;
};

// 切换页面
const switchPage = (index: number) => {
  currentPageIndex.value = index;
  selectedElement.value = null;
};

// 添加页面
const addPage = () => {
  const newPage: Page = {
    id: `page-${Date.now()}`,
    elements: [],
    thumbnail: "",
  };

  pages.value.push(newPage);
  currentPageIndex.value = pages.value.length - 1;
  selectedElement.value = null;
};

// 删除页面
const deletePage = (index: number) => {
  ElMessageBox.confirm("确定要删除此页面吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      pages.value.splice(index, 1);
      if (currentPageIndex.value >= pages.value.length) {
        currentPageIndex.value = Math.max(0, pages.value.length - 1);
      }
      selectedElement.value = null;
      ElMessage.success("删除成功");
    })
    .catch(() => {
      // 用户取消
    });
};

// 上移图层
const moveLayerUp = (index: number) => {
  if (index <= 0) return;

  const elements = currentPageElements.value;
  const element = elements[index];
  const prevElement = elements[index - 1];

  // 交换zIndex
  const tempZIndex = element.zIndex;
  element.zIndex = prevElement.zIndex;
  prevElement.zIndex = tempZIndex;

  // 交换位置
  elements[index] = prevElement;
  elements[index - 1] = element;
};

// 下移图层
const moveLayerDown = (index: number) => {
  if (index >= currentPageElements.value.length - 1) return;

  const elements = currentPageElements.value;
  const element = elements[index];
  const nextElement = elements[index + 1];

  // 交换zIndex
  const tempZIndex = element.zIndex;
  element.zIndex = nextElement.zIndex;
  nextElement.zIndex = tempZIndex;

  // 交换位置
  elements[index] = nextElement;
  elements[index + 1] = element;
};

// 获取元素名称
const getElementName = (element: Element) => {
  switch (element.type) {
    case "text":
      return element.content?.substring(0, 10) || "文本";
    case "image":
      return "图片";
    case "quote":
      return element.content?.substring(0, 10) || "引用";
    case "timeline":
      return element.title || "时间线";
    case "grid":
      return "网格";
    case "divider":
      return "分隔线";
    default:
      return "未知元素";
  }
};

// 缩放控制
const zoomIn = () => {
  zoomLevel.value = Math.min(2, zoomLevel.value + 0.1);
};

const zoomOut = () => {
  zoomLevel.value = Math.max(0.5, zoomLevel.value - 0.1);
};

const resetZoom = () => {
  zoomLevel.value = 1;
};

// 切换网格
const toggleGrid = () => {
  showGrid.value = !showGrid.value;
};

// 切换标尺
const toggleRuler = () => {
  showRuler.value = !showRuler.value;
};
</script>

<style scoped lang="scss">
.memorial-edit-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  h1 {
    margin: 0;
    font-size: 20px;
    color: #333;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.edit-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-left {
  width: 240px;
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow-y: auto;

  .tool-panel {
    padding: 16px;
    border-bottom: 1px solid #e4e7ed;

    h3 {
      margin: 0 0 16px;
      font-size: 16px;
      color: #303133;
    }

    .tool-group {
      margin-bottom: 20px;

      h4 {
        margin: 0 0 8px;
        font-size: 14px;
        color: #606266;
      }

      .tool-list {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 8px;

        .tool-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 12px 8px;
          border: 1px solid #dcdfe6;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            border-color: #409eff;
            background-color: #ecf5ff;
          }

          .el-icon {
            margin-bottom: 4px;
            font-size: 20px;
            color: #606266;
          }

          span {
            font-size: 12px;
            color: #606266;
          }
        }
      }
    }
  }

  .page-panel {
    padding: 16px;

    h3 {
      margin: 0 0 16px;
      font-size: 16px;
      color: #303133;
    }

    .page-list {
      margin-bottom: 16px;

      .page-item {
        display: flex;
        align-items: center;
        padding: 8px;
        margin-bottom: 8px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover,
        &.active {
          border-color: #409eff;
          background-color: #ecf5ff;
        }

        .page-thumbnail {
          width: 40px;
          height: 50px;
          margin-right: 8px;
          border: 1px solid #dcdfe6;
          border-radius: 2px;
          overflow: hidden;

          img {
            width: 100%;
            height: 100%;
            object-fit: cover;
          }
        }

        .page-info {
          flex: 1;
          display: flex;
          justify-content: space-between;
          align-items: center;

          span {
            font-size: 12px;
            color: #606266;
          }
        }
      }
    }

    .add-page-btn {
      width: 100%;
    }
  }
}

.editor-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.canvas-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;

  .zoom-controls {
    display: flex;
    align-items: center;
    gap: 8px;

    .zoom-info {
      font-size: 12px;
      color: #606266;
      min-width: 40px;
      text-align: center;
    }
  }

  .view-controls {
    display: flex;
    gap: 8px;
  }
}

.canvas-container {
  flex: 1;
  padding: 20px;
  overflow: auto;
  display: flex;
  justify-content: center;
  align-items: flex-start;

  .canvas-wrapper {
    transform-origin: center top;
    transition: transform 0.2s;
  }

  .canvas {
    position: relative;
    background-color: #fff;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);

    .grid-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-image: linear-gradient(to right, #f0f0f0 1px, transparent 1px),
        linear-gradient(to bottom, #f0f0f0 1px, transparent 1px);
      background-size: 20px 20px;
      pointer-events: none;
    }

    .element {
      cursor: move;

      &.selected {
        outline: 2px solid #409eff;
      }

      .element-text {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        padding: 8px;
        box-sizing: border-box;
      }

      .element-image {
        width: 100%;
        height: 100%;
        position: relative;
        overflow: hidden;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .image-overlay {
          position: absolute;
          bottom: 0;
          right: 0;
          background: rgba(0, 0, 0, 0.5);
          border-radius: 4px 0 0 0;
        }
      }

      .element-quote {
        width: 100%;
        height: 100%;
        padding: 16px;
        box-sizing: border-box;

        blockquote {
          margin: 0;
          padding-left: 16px;
          border-left: 4px solid #409eff;
          font-style: italic;
        }

        cite {
          display: block;
          margin-top: 8px;
          text-align: right;
          color: #666;
        }
      }

      .element-timeline {
        width: 100%;
        height: 100%;
        padding: 16px;
        box-sizing: border-box;

        .timeline-title {
          font-weight: bold;
          margin-bottom: 8px;
        }

        .timeline-date {
          margin-top: 8px;
          color: #666;
          font-size: 12px;
        }
      }

      .resize-handles {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        pointer-events: none;

        .handle {
          position: absolute;
          width: 8px;
          height: 8px;
          background-color: #409eff;
          border: 1px solid #fff;
          pointer-events: all;

          &.top-left {
            top: -4px;
            left: -4px;
            cursor: nw-resize;
          }

          &.top-right {
            top: -4px;
            right: -4px;
            cursor: ne-resize;
          }

          &.bottom-left {
            bottom: -4px;
            left: -4px;
            cursor: sw-resize;
          }

          &.bottom-right {
            bottom: -4px;
            right: -4px;
            cursor: se-resize;
          }
        }
      }
    }
  }
}

.editor-right {
  width: 280px;
  background: white;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;

  .property-panel {
    padding: 16px;
    border-bottom: 1px solid #e4e7ed;

    h3 {
      margin: 0 0 12px;
      font-size: 14px;
      color: #303133;
    }

    :deep(.el-form-item) {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .property-tabs {
      :deep(.el-tabs__header) {
        margin-bottom: 16px;
      }
    }
  }

  .layer-panel {
    flex: 1;
    padding: 16px;
    overflow-y: auto;

    h3 {
      margin: 0 0 12px;
      font-size: 14px;
      color: #303133;
    }

    .layer-list {
      .layer-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 8px 12px;
        margin-bottom: 4px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          border-color: #409eff;
          background: #f5f7fa;
        }

        &.active {
          border-color: #409eff;
          background: #ecf5ff;
        }

        .layer-info {
          display: flex;
          align-items: center;
          gap: 8px;

          .el-icon {
            font-size: 14px;
            color: #909399;
          }

          .layer-name {
            font-size: 12px;
            color: #606266;
          }
        }

        .layer-actions {
          display: flex;
          gap: 2px;

          .el-button {
            padding: 4px;
          }
        }
      }
    }
  }
}

.image-upload-container {
  .image-uploader {
    margin-bottom: 20px;
    text-align: center;
  }

  .image-library {
    h4 {
      margin: 0 0 12px;
      font-size: 16px;
      color: #303133;
    }

    .image-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;

      .image-item {
        height: 100px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        overflow: hidden;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          border-color: #409eff;
        }

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
    }
  }
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .edit-content .content-editor {
    .editor-left,
    .editor-right {
      display: none;
    }
  }
}
</style>
