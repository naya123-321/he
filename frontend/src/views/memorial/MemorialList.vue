<template>
  <div class="memorial-list">
    <div class="page-header">
      <div>
        <h2 class="title">我的纪念册</h2>
        <div class="subtitle">记录每一个美好的生命瞬间</div>
      </div>
      <div class="actions">
        <el-button type="primary" @click="goCreate">新建纪念册</el-button>
      </div>
    </div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <div class="status">
          <el-segmented
            v-model="status"
            :options="statusOptions"
            size="default"
          />
          </div>
        <div class="search">
              <el-input
            v-model="keyword"
                clearable
            placeholder="搜索纪念册标题"
            @keyup.enter="reload"
            @clear="reload"
              >
            <template #append>
              <el-button @click="reload">搜索</el-button>
                </template>
              </el-input>
          </div>
        </div>
      </el-card>

    <el-card shadow="never" class="content">
      <div v-loading="loading" class="list-wrap">
        <el-empty
          v-if="!loading && memorials.length === 0"
          description="暂无纪念册"
        />

        <el-row v-else :gutter="16">
          <el-col
            v-for="m in memorials"
            :key="m.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
          >
            <el-card shadow="hover" class="card">
              <div class="card-title" @click="openMemorial(m)">
                {{ m.title }}
              </div>
              <div class="card-meta">
                <span class="meta-item">宠物：{{ m.petName }}</span>
                <span v-if="m.petType" class="meta-item">({{ m.petType }})</span>
              </div>
              <div class="card-meta">
                <span class="meta-item">状态：{{ m.statusText || statusText(m.status) }}</span>
                </div>
              <div class="card-actions">
                <el-button
                  v-if="m.status === 0 || m.status === 1"
                  type="primary"
                  size="small"
                  @click="goEdit(m)"
                >
                  编辑
                </el-button>
                <el-button v-else type="success" size="small" @click="goPreview(m)">
                  查看
                </el-button>
                <el-button
                  type="danger"
                  plain
                  size="small"
                  @click="remove(m)"
                >
                  删除
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          :current-page="pagination.current"
          :page-size="pagination.pageSize"
          :page-sizes="[8, 12, 16, 24]"
          @update:current-page="handlePageChange"
          @update:page-size="handleSizeChange"
        />
                  </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { memorialApi, type MemorialVO } from "@/api/memorial";
import { useMemorialStore } from "@/store/memorial";

const router = useRouter();
const memorialStore = useMemorialStore();

const loading = computed(() => memorialStore.loading);
const memorials = computed(() => memorialStore.memorials);
const pagination = computed(() => memorialStore.pagination);

const status = ref<"all" | "0" | "1" | "2" | "3">("all");
const keyword = ref("");

const statusOptions = [
  { label: "全部", value: "all" },
  { label: "草稿", value: "0" },
  { label: "待审核", value: "1" },
  { label: "已完成", value: "2" },
  { label: "已下架", value: "3" },
];

function statusText(s: number) {
  switch (s) {
    case 0:
      return "草稿";
    case 1:
      return "待审核";
    case 2:
      return "已完成";
    case 3:
      return "已下架";
    default:
      return "未知";
  }
}

async function reload() {
  const statusNum =
    status.value === "all" ? undefined : Number.parseInt(status.value, 10);

  await memorialStore.fetchMemorials({
    status: Number.isNaN(statusNum as number) ? undefined : statusNum,
    title: keyword.value ? keyword.value : undefined,
  });
}

function goCreate() {
  router.push("/memorial/create");
}

function goEdit(m: MemorialVO) {
  router.push(`/memorial/edit/${m.id}`);
}

function goPreview(m: MemorialVO) {
  router.push(`/memorial/preview/${m.id}`);
}

function openMemorial(m: MemorialVO) {
  if (m.status === 0 || m.status === 1) goEdit(m);
  else goPreview(m);
}

async function remove(m: MemorialVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除纪念册“${m.title}”吗？此操作不可恢复。`,
      "删除确认",
      {
        confirmButtonText: "确定删除",
        cancelButtonText: "取消",
        type: "warning",
      }
    );

    const res = await memorialApi.deleteMemorial(m.id);
    if (res?.code === 200) {
      ElMessage.success("删除成功");
      await reload();
    } else {
      ElMessage.error(res?.message || "删除失败");
    }
  } catch (e) {
    // ignore cancel
  }
}

function handlePageChange(page: number) {
  memorialStore.pagination.current = page;
  reload();
}

function handleSizeChange(size: number) {
  memorialStore.pagination.pageSize = size;
  memorialStore.pagination.current = 1;
  reload();
}

onMounted(() => {
  reload();
});
</script>

<style scoped lang="scss">
  .memorial-list {
  padding: 16px;
}

.page-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
  margin-bottom: 12px;

  .title {
        margin: 0;
        font-size: 20px;
    font-weight: 700;
  }

  .subtitle {
    margin-top: 4px;
              color: #909399;
              font-size: 12px;
  }
}

.toolbar {
  margin-bottom: 12px;

  .toolbar-row {
    display: flex;
    gap: 12px;
    align-items: center;
      flex-wrap: wrap;
    justify-content: space-between;
  }

  .search {
    width: min(520px, 100%);
  }
}

.content {
  .list-wrap {
    min-height: 240px;
  }
}

.card {
  margin-bottom: 12px;
  cursor: default;

          .card-title {
    font-weight: 700;
    margin-bottom: 6px;
            cursor: pointer;
          }

          .card-meta {
            color: #606266;
    font-size: 12px;
    margin-bottom: 6px;
              display: flex;
    gap: 6px;
    flex-wrap: wrap;
          }

          .card-actions {
            display: flex;
    gap: 8px;
    justify-content: flex-end;
    margin-top: 8px;
  }
}

.pager {
  margin-top: 12px;
      display: flex;
      justify-content: flex-end;
}
</style>



