<template>
  <div class="memorial-annotate-container">
    <el-page-header @back="goBack" content="标注编辑（设计稿）" />

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-left">
        <el-tag type="info">当前：第 {{ currentIndex + 1 }} / {{ images.length || 0 }} 张</el-tag>
        <el-tag v-if="memorial?.title" type="success">{{ memorial.title }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-select v-model="tool" style="width: 160px">
          <el-option label="矩形" value="rect" />
          <el-option label="圆形" value="circle" />
          <el-option label="箭头" value="arrow" />
          <el-option label="文本" value="text" />
          <el-option label="橡皮擦（撤销上一步）" value="undo" />
        </el-select>
        <el-color-picker v-model="strokeColor" />
        <el-input-number v-model="strokeWidth" :min="1" :max="20" />
        <el-button @click="prev" :disabled="currentIndex <= 0">上一张</el-button>
        <el-button @click="next" :disabled="currentIndex >= images.length - 1">下一张</el-button>
        <el-button @click="resetCurrent" :disabled="!hasEdits">清空本张标注</el-button>
        <el-button type="primary" :loading="uploading" @click="uploadFeedback" :disabled="images.length === 0">
          上传给服务端
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="stage" v-loading="loading">
      <el-empty v-if="!loading && images.length === 0" description="暂无可标注的设计稿图片" />

      <div v-else class="canvas-wrap">
        <canvas
          ref="canvasRef"
          class="canvas"
          @mousedown="onDown"
          @mousemove="onMove"
          @mouseup="onUp"
          @mouseleave="onUp"
        ></canvas>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { memorialApi, type MemorialVO } from "@/api/memorial";

type Tool = "rect" | "circle" | "arrow" | "text" | "undo";
type Shape =
  | { type: "rect"; x: number; y: number; w: number; h: number; color: string; width: number }
  | { type: "circle"; x: number; y: number; r: number; color: string; width: number }
  | { type: "arrow"; x1: number; y1: number; x2: number; y2: number; color: string; width: number }
  | { type: "text"; x: number; y: number; text: string; color: string; size: number };

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const uploading = ref(false);
const memorial = ref<MemorialVO | null>(null);
const images = ref<string[]>([]);
const currentIndex = ref(0);

const tool = ref<Tool>("rect");
const strokeColor = ref("#ff3b30");
const strokeWidth = ref(4);

const canvasRef = ref<HTMLCanvasElement | null>(null);
let bgImg: HTMLImageElement | null = null;

// 每张图对应一组 shapes
const shapesByIndex = ref<Record<number, Shape[]>>({});
const hasEdits = computed(() => (shapesByIndex.value[currentIndex.value]?.length || 0) > 0);

let isDrawing = false;
let startX = 0;
let startY = 0;

function goBack() {
  router.back();
}

function prev() {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1;
    loadImageToCanvas();
  }
}

function next() {
  if (currentIndex.value < images.value.length - 1) {
    currentIndex.value += 1;
    loadImageToCanvas();
  }
}

function resetCurrent() {
  shapesByIndex.value[currentIndex.value] = [];
  redraw();
}

function ensureCanvasSize(img: HTMLImageElement) {
  const canvas = canvasRef.value!;
  // 固定最大宽度，按比例缩放
  const maxW = 1200;
  const ratio = img.width > maxW ? maxW / img.width : 1;
  canvas.width = Math.round(img.width * ratio);
  canvas.height = Math.round(img.height * ratio);
}

function drawBackground() {
  const canvas = canvasRef.value!;
  const ctx = canvas.getContext("2d")!;
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  if (!bgImg) return;
  ctx.drawImage(bgImg, 0, 0, canvas.width, canvas.height);
}

function drawArrow(ctx: CanvasRenderingContext2D, x1: number, y1: number, x2: number, y2: number, color: string, w: number) {
  ctx.strokeStyle = color;
  ctx.fillStyle = color;
  ctx.lineWidth = w;
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.lineTo(x2, y2);
  ctx.stroke();

  const headLen = 12 + w;
  const angle = Math.atan2(y2 - y1, x2 - x1);
  ctx.beginPath();
  ctx.moveTo(x2, y2);
  ctx.lineTo(x2 - headLen * Math.cos(angle - Math.PI / 6), y2 - headLen * Math.sin(angle - Math.PI / 6));
  ctx.lineTo(x2 - headLen * Math.cos(angle + Math.PI / 6), y2 - headLen * Math.sin(angle + Math.PI / 6));
  ctx.closePath();
  ctx.fill();
}

function redraw() {
  if (!canvasRef.value) return;
  const canvas = canvasRef.value;
  const ctx = canvas.getContext("2d")!;
  drawBackground();

  const shapes = shapesByIndex.value[currentIndex.value] || [];
  for (const s of shapes) {
    if (s.type === "rect") {
      ctx.strokeStyle = s.color;
      ctx.lineWidth = s.width;
      ctx.strokeRect(s.x, s.y, s.w, s.h);
    } else if (s.type === "circle") {
      ctx.strokeStyle = s.color;
      ctx.lineWidth = s.width;
      ctx.beginPath();
      ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
      ctx.stroke();
    } else if (s.type === "arrow") {
      drawArrow(ctx, s.x1, s.y1, s.x2, s.y2, s.color, s.width);
    } else if (s.type === "text") {
      ctx.fillStyle = s.color;
      ctx.font = `${s.size}px sans-serif`;
      ctx.fillText(s.text, s.x, s.y);
    }
  }
}

function canvasPoint(e: MouseEvent) {
  const canvas = canvasRef.value!;
  const rect = canvas.getBoundingClientRect();
  return {
    x: ((e.clientX - rect.left) / rect.width) * canvas.width,
    y: ((e.clientY - rect.top) / rect.height) * canvas.height,
  };
}

function onDown(e: MouseEvent) {
  if (!canvasRef.value) return;
  if (tool.value === "undo") {
    const shapes = shapesByIndex.value[currentIndex.value] || [];
    shapes.pop();
    shapesByIndex.value[currentIndex.value] = shapes;
    redraw();
    return;
  }

  const p = canvasPoint(e);
  startX = p.x;
  startY = p.y;
  isDrawing = true;

  if (tool.value === "text") {
    const t = window.prompt("请输入文本：");
    if (t && t.trim()) {
      const shapes = shapesByIndex.value[currentIndex.value] || [];
      shapes.push({ type: "text", x: startX, y: startY, text: t.trim(), color: strokeColor.value, size: 20 + strokeWidth.value });
      shapesByIndex.value[currentIndex.value] = shapes;
      redraw();
    }
    isDrawing = false;
  }
}

function onMove(e: MouseEvent) {
  if (!isDrawing || !canvasRef.value) return;
  if (!bgImg) return;

  // 预览绘制：临时绘制在画布上
  redraw();
  const p = canvasPoint(e);
  const ctx = canvasRef.value.getContext("2d")!;

  const color = strokeColor.value;
  const w = strokeWidth.value;

  if (tool.value === "rect") {
    ctx.strokeStyle = color;
    ctx.lineWidth = w;
    ctx.strokeRect(startX, startY, p.x - startX, p.y - startY);
  } else if (tool.value === "circle") {
    const r = Math.hypot(p.x - startX, p.y - startY);
    ctx.strokeStyle = color;
    ctx.lineWidth = w;
    ctx.beginPath();
    ctx.arc(startX, startY, r, 0, Math.PI * 2);
    ctx.stroke();
  } else if (tool.value === "arrow") {
    drawArrow(ctx, startX, startY, p.x, p.y, color, w);
  }
}

function onUp(e: MouseEvent) {
  if (!isDrawing || !canvasRef.value) return;
  isDrawing = false;
  const p = canvasPoint(e);
  const shapes = shapesByIndex.value[currentIndex.value] || [];

  const color = strokeColor.value;
  const w = strokeWidth.value;

  if (tool.value === "rect") {
    shapes.push({ type: "rect", x: startX, y: startY, w: p.x - startX, h: p.y - startY, color, width: w });
  } else if (tool.value === "circle") {
    const r = Math.hypot(p.x - startX, p.y - startY);
    shapes.push({ type: "circle", x: startX, y: startY, r, color, width: w });
  } else if (tool.value === "arrow") {
    shapes.push({ type: "arrow", x1: startX, y1: startY, x2: p.x, y2: p.y, color, width: w });
  }

  shapesByIndex.value[currentIndex.value] = shapes;
  redraw();
}

async function loadImageToCanvas() {
  if (!canvasRef.value) return;
  const url = images.value[currentIndex.value];
  if (!url) return;

  loading.value = true;
  try {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = url;
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve();
      img.onerror = () => reject(new Error("图片加载失败"));
    });
    bgImg = img;
    ensureCanvasSize(img);
    redraw();
  } catch (e: any) {
    ElMessage.error(e?.message || "加载图片失败");
  } finally {
    loading.value = false;
  }
}

async function canvasToJpegFile(filename: string, maxWidth = 1600, quality = 0.85): Promise<File> {
  const canvas = canvasRef.value!;
  // 先缩放到 maxWidth，避免导出过大
  const scale = canvas.width > maxWidth ? maxWidth / canvas.width : 1;
  const outCanvas = document.createElement("canvas");
  outCanvas.width = Math.round(canvas.width * scale);
  outCanvas.height = Math.round(canvas.height * scale);
  const ctx = outCanvas.getContext("2d")!;
  ctx.drawImage(canvas, 0, 0, outCanvas.width, outCanvas.height);

  const blob: Blob = await new Promise((resolve, reject) => {
    outCanvas.toBlob(
      (b) => (b ? resolve(b) : reject(new Error("导出失败"))),
      "image/jpeg",
      quality
    );
  });
  return new File([blob], filename, { type: "image/jpeg" });
}

async function uploadFeedback() {
  if (!canvasRef.value || images.value.length === 0) return;
  if (!memorial.value) return;

  uploading.value = true;
  try {
    // 只上传“有标注”的图片，且导出为压缩 JPEG，避免请求过大
    const files: File[] = [];
    for (let i = 0; i < images.value.length; i++) {
      const shapes = shapesByIndex.value[i] || [];
      if (shapes.length === 0) continue;
      // 切到对应图片绘制后导出
      currentIndex.value = i;
      await loadImageToCanvas();
      // 确保应用当前 shapes
      redraw();
      const file = await canvasToJpegFile(`annotated_${memorial.value.id}_${i + 1}.jpg`);
      files.push(file);
    }

    if (files.length === 0) {
      ElMessage.warning("当前没有任何标注内容，无需上传");
      return;
    }

    await memorialApi.ownerUploadFeedback(memorial.value.id, {
      images: files,
      message: "宠主在线标注修改稿（自动生成）",
    });

    ElMessage.success("已上传修改稿给服务端");
    router.push(`/memorial/preview/${memorial.value.id}`);
  } catch (e: any) {
    ElMessage.error(e?.message || "上传失败");
  } finally {
    uploading.value = false;
  }
}

async function load() {
  const id = Number(route.params.id);
  if (!id) return;

  loading.value = true;
  try {
    const res = await memorialApi.getMemorialDetail(id);
    memorial.value = res.data;
    images.value = (res.data.designDraftImages || []).slice();
    currentIndex.value = 0;
    // 初始化 shapes 容器
    shapesByIndex.value = {};
    if (images.value.length > 0) await loadImageToCanvas();
  } catch (e: any) {
    memorial.value = null;
    images.value = [];
    ElMessage.error(e?.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  load();
});
</script>

<style scoped lang="scss">
.memorial-annotate-container {
  padding: 16px;

  .toolbar {
    margin-top: 12px;
    margin-bottom: 12px;
    .toolbar-left {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      align-items: center;
      margin-bottom: 10px;
    }
    .toolbar-right {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      align-items: center;
    }
  }

  .stage {
    .canvas-wrap {
      display: flex;
      justify-content: center;
      padding: 12px;
      overflow: auto;
    }
    .canvas {
      max-width: 100%;
      border: 1px solid #ebeef5;
      border-radius: 10px;
      background: #fff;
      cursor: crosshair;
    }
  }
}
</style>


