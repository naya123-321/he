<template>
  <div class="template-editor">
    <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="模板名称" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="请输入模板名称"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="分类" prop="category">
        <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
          <el-option label="简约风格" value="simple" />
          <el-option label="温馨风格" value="warm" />
          <el-option label="经典风格" value="classic" />
          <el-option label="现代风格" value="modern" />
          <el-option label="复古风格" value="vintage" />
          <el-option label="文艺风格" value="literary" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>

      <el-form-item label="模板图">
        <div class="template-images-upload">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
            v-model:file-list="fileList"
            list-type="picture-card"
            accept="image/*"
            multiple
            class="template-uploader"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">只能上传jpg/png文件，且不超过5MB，可上传多张图片</div>
        </div>
      </el-form-item>

      <el-form-item label="描述">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入模板描述（100字以内）"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from "vue";
import { ElMessage } from "element-plus";
import { Plus, Delete } from "@element-plus/icons-vue";
import type { UploadFile, UploadFiles } from "element-plus";

const props = defineProps<{
  modelValue: {
    name: string;
    description?: string;
    category?: string;
    previewImage?: string;
    templateImages?: string[];
    styleConfig?: string;
    layoutConfig?: string;
  };
}>();

const emit = defineEmits<{
  "update:modelValue": [value: any];
}>();

const formRef = ref();

const formData = reactive({
  name: props.modelValue?.name || "",
  description: props.modelValue?.description || "",
  category: props.modelValue?.category || "",
  previewImage: props.modelValue?.previewImage || "",
  templateImages: props.modelValue?.templateImages || [],
});

// 文件列表
const fileList = ref<UploadFile[]>([]);

// 初始化文件列表
const initFileList = () => {
  if (props.modelValue?.templateImages && props.modelValue.templateImages.length > 0) {
    fileList.value = props.modelValue.templateImages.map((url, index) => ({
      uid: `template-image-${index}-${Date.now()}`,
      name: `模板图${index + 1}`,
      status: "success",
      url: url,
    }));
  } else if (props.modelValue?.previewImage) {
    // 兼容旧数据：如果有previewImage，转换为templateImages
    fileList.value = [{
      uid: `template-image-0-${Date.now()}`,
      name: "模板图1",
      status: "success",
      url: props.modelValue.previewImage,
    }];
    formData.templateImages = [props.modelValue.previewImage];
  } else {
    fileList.value = [];
  }
};

// 初始化
initFileList();

// 注意：不要对 props.modelValue 做 deep watch 再反向初始化 fileList，
// 否则每次输入都会触发重置 fileList（含 Date.now uid），容易导致弹窗内下拉框/Popper 点击异常。
// 编辑/新增时由父组件通过变更 key 强制重挂载来完成初始化。

const rules = {
  name: [{ required: true, message: "请输入模板名称", trigger: "blur" }],
  category: [{ required: true, message: "请选择分类", trigger: "change" }],
};

const uploadAction = "/api/template/upload-preview";
const uploadHeaders = {
  get Authorization() {
    const token = sessionStorage.getItem("token");
    return token ? `Bearer ${token}` : "";
  },
};

// 监听文件列表变化
watch(
  fileList,
  (newList) => {
    formData.templateImages = newList
      .filter((file) => file.status === "success" && file.url)
      .map((file) => file.url as string);
    // 兼容性：第一张图作为previewImage
    formData.previewImage = formData.templateImages[0] || "";
  },
  { deep: true }
);

// 监听表单数据变化，同步到父组件
watch(
  formData,
  () => {
    emit("update:modelValue", {
      ...formData,
      styleConfig: "",
      layoutConfig: "",
    });
  },
  { deep: true }
);

// 上传前验证
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith("image/");
  const isLt5M = file.size / 1024 / 1024 < 5;

  if (!isImage) {
    ElMessage.error("只能上传图片文件!");
    return false;
  }
  if (!isLt5M) {
    ElMessage.error("图片大小不能超过 5MB!");
    return false;
  }
  return true;
};

// 上传成功
const handleUploadSuccess = (response: any, uploadFile: UploadFile) => {
  if (response && response.code === 200) {
    // 处理图片URL：如果是相对路径，需要转换为完整URL
    let imageUrl = response.data;
    if (imageUrl && imageUrl.startsWith("/")) {
      // 相对路径，通过API代理访问
      // 如果后端配置了静态资源映射，可以直接使用
      // 否则需要通过代理访问
      imageUrl = imageUrl.startsWith("/api") ? imageUrl : imageUrl;
    }
    uploadFile.url = imageUrl;
    uploadFile.status = "success";
    ElMessage.success("上传成功");
  } else {
    uploadFile.status = "fail";
    ElMessage.error(response?.message || "上传失败");
  }
};

// 删除图片
const handleRemove = (uploadFile: UploadFile) => {
  const index = fileList.value.findIndex((file) => file.uid === uploadFile.uid);
  if (index > -1) {
    fileList.value.splice(index, 1);
    ElMessage.success("删除成功");
  }
};

// 暴露验证方法
defineExpose({
  validate: () => formRef.value?.validate(),
  resetFields: () => formRef.value?.resetFields(),
});
</script>

<style scoped lang="scss">
.template-editor {
  .template-images-upload {
    .template-uploader {
      :deep(.el-upload) {
        width: 280px;
        height: 280px;
        border: 1px dashed #d9d9d9;
        border-radius: 6px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        transition: all 0.3s;
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: #fafafa;
        color: #8c939d;

        &:hover {
          border-color: #409eff;
          color: #409eff;
        }

        .el-icon {
          font-size: 40px;
        }
      }

      :deep(.el-upload-list) {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
      }

      :deep(.el-upload-list__item) {
        width: 280px;
        height: 280px;
        margin: 0;
        border-radius: 6px;
        overflow: hidden;
        position: relative;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .el-upload-list__item-actions {
          position: absolute;
          top: 0;
          right: 0;
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          background-color: rgba(0, 0, 0, 0.5);
          opacity: 0;
          transition: opacity 0.3s;

          &:hover {
            opacity: 1;
          }

          .el-upload-list__item-delete {
            color: #fff;
            font-size: 14px;
            width: 20px;
            height: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 3px;
            transition: all 0.3s;
            cursor: pointer;

            &:hover {
              background-color: rgba(255, 77, 79, 0.8);
            }

            .el-icon {
              font-size: 14px;
            }
          }
        }
      }
    }

    .upload-tip {
      margin-top: 10px;
      font-size: 12px;
      color: #999;
    }
  }
}
</style>


