<template>
  <el-dialog
    v-model="visible"
    title="服务评价"
    width="500px"
    :before-close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="评分" prop="rating">
        <el-rate v-model="form.rating" :max="5" />
      </el-form-item>
      <el-form-item label="评价内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          placeholder="请输入您的评价内容"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from "vue";
import { ElMessage, type FormInstance } from "element-plus";

interface ReviewForm {
  rating: number;
  content: string;
}

interface Props {
  modelValue: boolean;
  orderId?: string;
}

interface Emits {
  (e: "update:modelValue", value: boolean): void;
  (e: "success"): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const visible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<ReviewForm>({
  rating: 5,
  content: "",
});

const rules = {
  rating: [{ required: true, message: "请选择评分", trigger: "blur" }],
  content: [{ required: true, message: "请输入评价内容", trigger: "blur" }],
};

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val;
  }
);

watch(visible, (val) => {
  emit("update:modelValue", val);
  if (!val) {
    resetForm();
  }
});

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields();
  }
  form.rating = 5;
  form.content = "";
};

const handleClose = () => {
  visible.value = false;
};

const submitReview = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    // 这里应该调用评价API
    // await reviewApi.createReview({
    //   orderId: props.orderId,
    //   rating: form.rating,
    //   content: form.content
    // });

    ElMessage.success("评价提交成功");
    emit("success");
    handleClose();
  } catch (error) {
    console.error("评价提交失败:", error);
  }
};
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
