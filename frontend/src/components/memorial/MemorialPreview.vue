<template>
  <el-dialog
    v-model="dialogVisible"
    :title="previewTitle"
    width="95%"
    top="2vh"
    fullscreen
    :close-on-click-modal="false"
  >
    <div class="preview-toolbar">
      <div class="toolbar-left">
        <el-tag type="info" size="small">共 {{ totalPages }} 页</el-tag>
      </div>
      <div class="toolbar-right">
        <el-button-group>
          <el-button @click="prevPage" :disabled="currentPageIndex === 0">上一页</el-button>
          <el-button type="info" plain>
            第 {{ currentPageIndex + 1 }} / {{ Math.max(totalPages, 1) }} 页
          </el-button>
          <el-button @click="nextPage" :disabled="currentPageIndex >= totalPages - 1">下一页</el-button>
        </el-button-group>

        <el-dropdown @command="handleExportCommand" style="margin-left: 12px">
          <el-button type="primary">
            导出
            <el-icon style="margin-left: 6px"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="pdf">导出为PDF</el-dropdown-item>
              <el-dropdown-item command="image">导出为图片</el-dropdown-item>
              <el-dropdown-item command="print">打印</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="preview-body">
      <div class="thumbnail">
        <div class="thumbnail-title">页面导航</div>
        <el-scrollbar height="calc(100vh - 180px)">
          <div
            v-for="(p, idx) in pages"
            :key="p?.id ?? idx"
            class="thumb-item"
            :class="{ active: idx === currentPageIndex }"
            @click="goToPage(idx)"
          >
            <div class="thumb-no">第 {{ idx + 1 }} 页</div>
            <div class="thumb-name">{{ p?.title || `第${idx + 1}页` }}</div>
            </div>
        </el-scrollbar>
      </div>

      <div class="main">
        <el-empty
          v-if="totalPages === 0"
          description="暂无页面内容"
          :image-size="160"
        />

        <template v-else>
          <div class="page-title">
            {{ currentPage?.title || `第${currentPageIndex + 1}页` }}
                        </div>
          <div class="page-canvas">
            <el-empty
              v-if="currentPageElements.length === 0"
              description="该页面暂无元素"
              :image-size="140"
            />

            <div v-else class="elements-list">
              <div v-for="(el, i) in currentPageElements" :key="el?.id ?? i" class="element-item">
                <div class="el-name">{{ el?.name || el?.type || "元素" }}</div>
                <div class="el-meta">ID：{{ el?.id ?? "-" }}</div>
            </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { ArrowDown } from "@element-plus/icons-vue";

const props = defineProps<{
  modelValue: boolean;
  memorialInfo: any;
  pages: any[];
  elements: any[];
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  export: [type: string];
}>();

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit("update:modelValue", v),
});

const totalPages = computed(() => props.pages?.length || 0);
const previewTitle = computed(() => `预览纪念册：${props.memorialInfo?.title || "未命名"}`);

const currentPageIndex = ref(0);
watch(
  () => totalPages.value,
  (n) => {
    if (n <= 0) currentPageIndex.value = 0;
    else if (currentPageIndex.value > n - 1) currentPageIndex.value = n - 1;
  }
);

const currentPage = computed(() => props.pages?.[currentPageIndex.value]);

const currentPageElements = computed(() => {
  const pageId = currentPage.value?.id;
  const elements = props.elements || [];
  if (pageId == null) return [];
  // 兼容常见字段：pageId / page_id
  return elements.filter((e: any) => e?.pageId === pageId || e?.page_id === pageId);
});

function prevPage() {
  if (currentPageIndex.value > 0) currentPageIndex.value -= 1;
}

function nextPage() {
  if (currentPageIndex.value < totalPages.value - 1) currentPageIndex.value += 1;
}

function goToPage(idx: number) {
  if (idx >= 0 && idx < totalPages.value) currentPageIndex.value = idx;
}

function handleExportCommand(command: string | number) {
  emit("export", String(command));
}
</script>

<style scoped lang="scss">
.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.preview-body {
    display: flex;
  gap: 16px;
  height: calc(100vh - 170px);
}

.thumbnail {
  width: 260px;
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  flex-shrink: 0;
}

.thumbnail-title {
  font-weight: 600;
  margin-bottom: 10px;
        color: #303133;
}

.thumb-item {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid transparent;
        cursor: pointer;
  transition: all 0.15s ease;
  margin-bottom: 8px;
  background: #f9fafb;

        &:hover {
    border-color: rgba(64, 158, 255, 0.35);
    background: #f0f9ff;
        }

        &.active {
    border-color: #409eff;
    background: #ecf5ff;
  }

  .thumb-no {
            font-size: 12px;
            color: #909399;
            margin-bottom: 4px;
          }

  .thumb-name {
    font-size: 13px;
            color: #303133;
    font-weight: 600;
  }
}

.main {
    flex: 1;
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px;
    overflow: hidden;
  display: flex;
  flex-direction: column;
}

   .page-title { 
  font-weight: 700;
             color: #303133; 
  margin-bottom: 10px;
}

.page-canvas {
     flex: 1; 
  border-radius: 10px;
  border: 1px dashed #dcdfe6;
  background: #f8fafc;
  padding: 14px;
     overflow: auto; 
}

.elements-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.element-item {
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;

  .el-name {
    font-weight: 700;
    color: #303133;
    margin-bottom: 6px;
  }

  .el-meta {
    color: #909399;
                   font-size: 12px; 
  }
}

@media (max-width: 900px) {
  .thumbnail {
         display: none; 
       } 
}
</style>


