<template>
  <div class="visitor-pet-info">
    <el-page-header @back="goBack" content="智能套餐推荐 - 信息采集" />

    <el-card class="card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="title">填写宠物信息（无需登录）</div>
          <div class="sub">
            我们会基于历史用户选择数据（协同过滤）+ 匹配规则，为您推荐更合适的套餐
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="form">
        <el-form-item label="宠物类型" prop="petType">
          <el-select v-model="form.petType" placeholder="请选择宠物类型" filterable>
            <el-option label="猫" value="cat" />
            <el-option label="小型犬" value="dog_small" />
            <el-option label="中型犬" value="dog_medium" />
            <el-option label="大型犬" value="dog_large" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="宠物年龄" prop="petAge">
          <el-input-number v-model="form.petAge" :min="0" :max="30" controls-position="right" />
          <span class="hint">岁</span>
        </el-form-item>

        <el-form-item label="离世原因" prop="deathCause">
          <el-select v-model="form.deathCause" placeholder="请选择离世原因" filterable allow-create>
            <el-option label="疾病" value="disease" />
            <el-option label="意外" value="accident" />
            <el-option label="自然衰老" value="aging" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item class="actions">
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">
            进入服务套餐并获取推荐
          </el-button>
        </el-form-item>
      </el-form>

      <div class="tip">
        提示：信息仅用于推荐，保存在当前浏览器会话中（刷新仍在，关闭浏览器标签页后可能消失）。
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, type FormInstance, type FormItemRule } from "element-plus";

type VisitorPetProfile = {
  petType: string;
  petAge: number;
  deathCause: string;
};

const router = useRouter();
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive<VisitorPetProfile>({
  petType: "",
  petAge: 0,
  deathCause: "",
});

const rules: Record<string, FormItemRule[]> = {
  petType: [{ required: true, message: "请选择宠物类型", trigger: "change" }],
  petAge: [{ required: true, message: "请输入宠物年龄", trigger: "change" }],
  deathCause: [{ required: true, message: "请选择离世原因", trigger: "change" }],
};

const STORAGE_KEY = "visitorPetProfile";

const goBack = () => router.back();

const reset = () => {
  form.petType = "";
  form.petAge = 0;
  form.deathCause = "";
};

const submit = async () => {
  if (!formRef.value) return;
  try {
    const ok = await formRef.value.validate();
    if (!ok) return;
    submitting.value = true;

    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ ...form }));
    ElMessage.success("已保存宠物信息，即将为您推荐套餐");
    router.push({ path: "/service-packages", query: { recommend: "1" } });
  } catch (e: any) {
    ElMessage.error(e?.message || "提交失败");
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return;
  try {
    const v = JSON.parse(raw) as VisitorPetProfile;
    if (v && typeof v === "object") {
      form.petType = v.petType || "";
      form.petAge = Number.isFinite(v.petAge) ? v.petAge : 0;
      form.deathCause = v.deathCause || "";
    }
  } catch {
    // ignore
  }
});
</script>

<style scoped lang="scss">
.visitor-pet-info {
  max-width: 980px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 100px);
}

.card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
  .title {
    font-weight: 700;
    font-size: 18px;
    color: #303133;
  }
  .sub {
    font-size: 13px;
    color: #909399;
  }
}

.form {
  margin-top: 10px;
}

.hint {
  margin-left: 8px;
  color: #909399;
}

.actions :deep(.el-form-item__content) {
  justify-content: flex-end;
  gap: 12px;
}

.tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>


