<template>
  <div class="book-service-container">
    <el-page-header @back="goBack" content="预约服务" />

    <div class="booking-main-card">
      <div class="booking-intro">
        <h2>预约服务时间</h2>
        <p>请选择合适的服务时间，并填写宠物信息，我们会为爱宠安排一个庄重而温暖的告别仪式。</p>
      </div>

      <el-form
        ref="bookingFormRef"
        :model="bookingForm"
        :rules="bookingRules"
        class="booking-form"
      >
        <!-- 服务套餐信息 -->
        <el-form-item class="package-form-item">
          <el-card shadow="never" class="package-card">
            <div class="package-info">
              <div class="package-tag">已选择套餐</div>
              <h3>{{ selectedPackageName || "尚未选择套餐" }}</h3>
              <p v-if="selectedPackageId">套餐编号：{{ selectedPackageId }}</p>
              <p v-else class="package-hint">建议先在服务套餐页选择合适的套餐，再进行预约。</p>
            </div>
          </el-card>
        </el-form-item>

        <!-- 预约日期和时间 -->
        <el-form-item label="预约日期" prop="appointmentDate">
          <el-date-picker
            v-model="bookingForm.appointmentDate"
            type="date"
            placeholder="选择日期"
            :disabled-date="disabledDate"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="预约时间" prop="appointmentTime">
          <el-time-picker
            v-model="bookingForm.appointmentTime"
            placeholder="选择时间"
            :disabled-hours="disabledHours"
            :disabled-minutes="disabledMinutes"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 宠物信息 -->
        <el-form-item label="宠物姓名" prop="petName">
          <el-input v-model="bookingForm.petName" placeholder="请输入宠物姓名" />
        </el-form-item>

        <el-form-item label="宠物类型" prop="petType">
          <el-select 
            v-model="bookingForm.petType" 
            placeholder="请选择宠物类型"
            style="width: 100%"
            filterable
            @change="handlePetTypeChange"
          >
            <el-option-group v-for="g in PET_TYPE_GROUPS" :key="g.label" :label="g.label">
              <el-option v-for="o in g.options" :key="o.value" :label="o.label" :value="o.value">
                <span style="float: left">{{ o.emoji ? `${o.emoji} ` : "" }}{{ o.label }}</span>
              </el-option>
            </el-option-group>
          </el-select>
        </el-form-item>

        <el-form-item label="宠物品种" prop="petBreed">
          <el-autocomplete
            v-model="bookingForm.petBreed"
            :fetch-suggestions="queryBreedSuggestions"
            placeholder="请输入或选择宠物品种（选填）"
            style="width: 100%"
            clearable
          >
            <template #default="{ item }">
              <div>{{ item.value }}</div>
            </template>
          </el-autocomplete>
        </el-form-item>

        <el-form-item label="宠物年龄" prop="petAge">
          <el-input-number
            v-model="bookingForm.petAge"
            :min="0"
            :max="30"
            placeholder="请输入宠物年龄"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="宠物体重（kg）" prop="petWeight">
          <el-input-number
            v-model="bookingForm.petWeight"
            :min="0"
            :max="100"
            :precision="1"
            placeholder="请输入宠物体重"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="离世日期" prop="deathDate">
          <el-date-picker
            v-model="bookingForm.deathDate"
            type="date"
            placeholder="选择离世日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="离世原因" prop="deathCause">
          <el-input
            v-model="bookingForm.deathCause"
            type="textarea"
            :rows="3"
            placeholder="请简要描述宠物离世原因（选填）"
          />
        </el-form-item>

        <!-- 联系人信息 -->
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input
            v-model="bookingForm.contactName"
            placeholder="请输入联系人姓名"
          />
        </el-form-item>

        <el-form-item label="联系人电话" prop="contactPhone">
          <el-input
            v-model="bookingForm.contactPhone"
            placeholder="请输入联系人电话"
          />
        </el-form-item>

        <el-form-item label="服务地址" prop="address">
          <el-input
            v-model="bookingForm.address"
            type="textarea"
            :rows="2"
            placeholder="请输入服务地址"
          />
        </el-form-item>

        <el-form-item label="特殊要求" prop="specialRequirements" class="full-row-item">
          <el-alert
            v-if="bookingForm.specialRequirements"
            type="info"
            :closable="false"
            show-icon
            class="special-tip"
          >
            <template #title>
              <span>填写特殊要求后，系统将自动提交审核请求，管理员会在特殊需求管理页面进行审核。</span>
            </template>
          </el-alert>
          <el-select
            v-model="specialRequestType"
            placeholder="选择需求类型（选填）"
            style="width: 100%; margin-bottom: 10px"
            clearable
          >
            <el-option label="时间调整" value="时间调整" />
            <el-option label="服务方式" value="服务方式" />
            <el-option label="其他" value="其他" />
          </el-select>
          <el-input
            v-model="bookingForm.specialRequirements"
            type="textarea"
            :rows="4"
            placeholder="请详细描述您的特殊要求，例如：需要调整服务时间、特殊的服务方式、特殊注意事项等（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

      </el-form>

      <div class="action-buttons">
        <el-button @click="goBack">上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBooking">
          提交预约
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox, type FormInstance } from "element-plus";
import { useOrderStore } from "@/store/order";
import { serviceTypeApi, type ServiceTypeVO } from "@/api/order";
import type { OrderCreateDTO } from "@/api/order";
import { specialRequestApi } from "@/api/specialRequest";
import { PET_TYPE_GROUPS } from "@/constants/petTypes";

const router = useRouter();
const route = useRoute();
const bookingFormRef = ref<FormInstance>();
const submitting = ref(false);
const orderStore = useOrderStore();

// 获取URL参数中的套餐信息
const selectedPackageId = ref<string>((route.query.packageId as string) || "");
const selectedPackageName = ref<string>(
  (route.query.packageName as string) || ""
);

// 服务类型列表
const serviceTypes = ref<ServiceTypeVO[]>([]);
// 实际使用的服务类型ID
const actualServiceTypeId = ref<number | null>(null);

// 特殊需求类型
const specialRequestType = ref<string>("");

// 纪念册模板数据（模拟数据，实际应从API获取）
const memorialTemplates = ref([
  { id: 1, name: "简约风格" },
  { id: 2, name: "温馨风格" },
  { id: 3, name: "可爱风格" },
  { id: 4, name: "文艺风格" },
]);

// 宠物品种建议数据（根据宠物类型动态提供）
const breedSuggestions: Record<string, string[]> = {
  cat: ["英短", "美短", "布偶", "暹罗", "波斯", "加菲", "橘猫", "三花", "狸花", "金渐层", "银渐层", "无毛猫", "其他"],
  dog: ["金毛", "拉布拉多", "哈士奇", "泰迪", "比熊", "博美", "柯基", "柴犬", "边牧", "德牧", "萨摩耶", "阿拉斯加", "法斗", "英斗", "其他"],
  rabbit: ["垂耳兔", "侏儒兔", "安哥拉兔", "荷兰兔", "狮子兔", "其他"],
  hamster: ["金丝熊", "仓鼠", "三线", "一线", "其他"],
  bird: ["鹦鹉", "文鸟", "金丝雀", "画眉", "其他"],
  chinchilla: ["标准灰", "米色", "白色", "其他"],
  other: [],
};

// 处理宠物类型变化
const handlePetTypeChange = (value: string) => {
  // 清空品种输入，让用户重新选择
  bookingForm.petBreed = "";
};

// 查询品种建议
const queryBreedSuggestions = (queryString: string, cb: (suggestions: any[]) => void) => {
  const petType = bookingForm.petType;
  const suggestions: any[] = [];
  
  if (!petType) {
    cb(suggestions);
    return;
  }
  
  const breeds = breedSuggestions[petType] || [];
  
  if (queryString) {
    // 如果有输入，过滤匹配的品种
    const filtered = breeds
      .filter(breed => breed.toLowerCase().includes(queryString.toLowerCase()))
      .map(breed => ({ value: breed }));
    cb(filtered);
  } else {
    // 如果没有输入，显示所有建议
    const all = breeds.map(breed => ({ value: breed }));
    cb(all);
  }
};

// 预约表单数据
const bookingForm = reactive({
  // 服务信息
  packageId: selectedPackageId.value,

  // 预约信息
  appointmentDate: "",
  appointmentTime: "",

  // 宠物信息
  petName: "",
  petType: "",
  petBreed: "",
  petAge: undefined,
  petWeight: undefined,
  deathDate: "",
  deathCause: "",

  // 联系人信息
  contactName: "",
  contactPhone: "",
  address: "",
  specialRequirements: "",
});

// 表单验证规则
const bookingRules = reactive({
  appointmentDate: [
    { required: true, message: "请选择预约日期", trigger: "change" },
  ],
  appointmentTime: [
    { required: true, message: "请选择预约时间", trigger: "change" },
  ],
  petName: [{ required: true, message: "请输入宠物姓名", trigger: "blur" }],
  petType: [{ required: true, message: "请选择宠物类型", trigger: "change" }],
  deathDate: [{ required: true, message: "请选择离世日期", trigger: "change" }],
  contactName: [
    { required: true, message: "请输入联系人姓名", trigger: "blur" },
  ],
  contactPhone: [
    { required: true, message: "请输入联系人电话", trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号码",
      trigger: "blur",
    },
  ],
  address: [{ required: true, message: "请输入服务地址", trigger: "blur" }],
});

// 禁用过去的日期
const disabledDate = (time: Date) => {
  return time.getTime() < Date.now() - 8.64e7; // 只能选择今天及以后的日期
};

// 禁用的小时
const disabledHours = () => {
  const now = new Date();
  const currentHour = now.getHours();
  const disabled: number[] = [];

  // 禁用当前时间之前的小时
  for (let i = 0; i < currentHour; i++) {
    disabled.push(i);
  }

  // 只允许工作时间：9:00-18:00
  for (let i = 0; i < 9; i++) {
    disabled.push(i);
  }
  for (let i = 19; i < 24; i++) {
    disabled.push(i);
  }

  return disabled;
};

// 禁用的分钟
const disabledMinutes = (selectedHour: number) => {
  const now = new Date();
  const currentHour = now.getHours();
  const currentMinute = now.getMinutes();
  const disabled: number[] = [];

  // 如果选择的是当前小时，禁用当前分钟之前的分钟
  if (selectedHour === currentHour) {
    for (let i = 0; i <= currentMinute; i++) {
      disabled.push(i);
    }
  }

  return disabled;
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 提交预约
const submitBooking = async () => {
  if (!bookingFormRef.value) return;

  const valid = await bookingFormRef.value.validate();
  if (!valid) return;

  submitting.value = true;

  try {
    // 验证必填字段
    if (!bookingForm.deathDate) {
      ElMessage.error("请选择离世日期");
      submitting.value = false;
      return;
    }
    if (!bookingForm.appointmentDate || !bookingForm.appointmentTime) {
      ElMessage.error("请选择预约日期和时间");
      submitting.value = false;
      return;
    }

    // 构建订单创建数据
    // 使用 ISO 8601 格式（yyyy-MM-ddTHH:mm:ss），这是 Java LocalDateTime 的标准格式
    // 虽然后端有 @JsonFormat 注解，但使用 ISO 8601 格式更可靠
    const deathDateTime = `${bookingForm.deathDate}T00:00:00`;
    const appointmentDateTime = `${bookingForm.appointmentDate}T${bookingForm.appointmentTime}:00`;

    // 处理空字符串，转换为 undefined
    const cleanString = (value: string | undefined) => {
      return value && value.trim() ? value.trim() : undefined;
    };

    // 处理数字字段：如果是0或undefined，保持原值或设为undefined
    const cleanNumber = (value: number | undefined) => {
      return value !== undefined && value !== null && value !== 0 ? value : undefined;
    };

    // 确定使用的服务类型ID
    let serviceTypeIdToUse = actualServiceTypeId.value;
    
    // 如果没有找到匹配的服务类型，使用第一个可用的服务类型
    if (!serviceTypeIdToUse && serviceTypes.value.length > 0) {
      serviceTypeIdToUse = serviceTypes.value[0].id;
      console.log("使用第一个可用的服务类型:", serviceTypeIdToUse);
    }
    
    // 如果还是没有，尝试使用套餐ID作为降级方案
    if (!serviceTypeIdToUse && selectedPackageId.value) {
      serviceTypeIdToUse = Number(selectedPackageId.value);
      console.warn("使用套餐ID作为服务类型ID（降级方案）:", serviceTypeIdToUse);
      ElMessage.warning("将使用套餐ID作为服务类型，如果失败请联系管理员");
    }
    
    // 如果还是没有，报错
    if (!serviceTypeIdToUse) {
      ElMessage.error("未找到可用的服务类型，请先选择服务套餐或联系管理员");
      submitting.value = false;
      return;
    }
    
    console.log("最终使用的服务类型ID:", serviceTypeIdToUse);

    const orderData: any = {
      serviceTypeId: serviceTypeIdToUse,
      petName: bookingForm.petName.trim(),
      petType: bookingForm.petType,
      petBreed: cleanString(bookingForm.petBreed),
      petAge: cleanNumber(bookingForm.petAge),
      petWeight: cleanNumber(bookingForm.petWeight),
      deathDate: deathDateTime, // 格式: "yyyy-MM-dd HH:mm:ss"
      deathCause: cleanString(bookingForm.deathCause),
      appointmentTime: appointmentDateTime, // 格式: "yyyy-MM-dd HH:mm:ss"
      address: bookingForm.address.trim(),
      contactName: bookingForm.contactName.trim(),
      contactPhone: bookingForm.contactPhone.trim(),
      specialRequirements: cleanString(bookingForm.specialRequirements),
    };

    // 调试日志
    console.log("提交的订单数据:", JSON.stringify(orderData, null, 2));
    console.log("deathDate格式:", orderData.deathDate);
    console.log("appointmentTime格式:", orderData.appointmentTime);

    const order = await orderStore.createOrder(orderData);
    if (order) {
      // 如果填写了特殊要求，创建特殊需求记录
      if (bookingForm.specialRequirements && bookingForm.specialRequirements.trim()) {
        try {
          await specialRequestApi.createSpecialRequest({
            orderId: order.id,
            requestType: specialRequestType.value || "其他",
            description: bookingForm.specialRequirements.trim(),
          });
          ElMessage.success("预约成功！特殊需求已提交，等待管理员审核");
        } catch (error: any) {
          console.error("提交特殊需求失败:", error);
          ElMessage.warning("预约成功，但特殊需求提交失败：" + (error?.message || "未知错误"));
        }
      } else {
        ElMessage.success("预约成功！");
      }
      
      // 跳转到服务进度页面，传递订单ID
      router.push({
        path: "/pet-owner/service-progress",
        query: { orderId: order.id },
      });
    }
  } catch (error: any) {
    console.error("提交预约失败:", error);
    // 输出详细的错误信息用于调试
    if (error?.response?.data) {
      console.error("错误详情:", error.response.data);
      const errorMsg = error.response.data.message || error.response.data.error || JSON.stringify(error.response.data);
      ElMessage.error(`提交预约失败: ${errorMsg}`);
    } else if (error?.message) {
      console.error("错误信息:", error.message);
      ElMessage.error(`提交预约失败: ${error.message}`);
    } else {
      ElMessage.error("提交预约失败，请稍后重试");
    }
  } finally {
    submitting.value = false;
  }
};

// 页面加载时获取服务类型列表和纪念册模板列表
onMounted(async () => {
  // 获取服务类型列表
  try {
    const res = await serviceTypeApi.getServiceTypes();
    console.log("服务类型API响应:", res);
    
    if (res && res.code === 200 && res.data && Array.isArray(res.data) && res.data.length > 0) {
      serviceTypes.value = res.data;
      console.log("获取到服务类型列表:", serviceTypes.value);
      
      // 根据套餐名称或ID匹配服务类型
      if (selectedPackageName.value) {
        // 尝试根据套餐名称匹配服务类型
        const matchedServiceType = serviceTypes.value.find(
          (st) => st.name.includes(selectedPackageName.value) || 
                  selectedPackageName.value.includes(st.name)
        );
        if (matchedServiceType) {
          actualServiceTypeId.value = matchedServiceType.id;
          console.log("根据套餐名称匹配到服务类型:", actualServiceTypeId.value);
        }
      }
      
      // 如果没找到匹配的，尝试根据套餐ID匹配（如果数据库中的ID正好对应）
      if (!actualServiceTypeId.value && selectedPackageId.value) {
        const packageIdNum = Number(selectedPackageId.value);
        const matchedById = serviceTypes.value.find(st => st.id === packageIdNum);
        if (matchedById) {
          actualServiceTypeId.value = matchedById.id;
          console.log("根据套餐ID匹配到服务类型:", actualServiceTypeId.value);
        }
      }
      
      // 如果还是没找到，使用第一个服务类型
      if (!actualServiceTypeId.value && serviceTypes.value.length > 0) {
        actualServiceTypeId.value = serviceTypes.value[0].id;
        console.log("使用默认服务类型:", actualServiceTypeId.value, serviceTypes.value[0].name);
      }
    } else {
      // 如果没有服务类型数据，显示警告但不阻止提交
      console.warn("服务类型列表为空或格式不正确:", res);
      if (res && res.data && Array.isArray(res.data) && res.data.length === 0) {
        ElMessage.warning("当前没有可用的服务类型，请联系管理员");
      } else {
        // 尝试使用套餐ID作为服务类型ID（降级方案）
        if (selectedPackageId.value) {
          actualServiceTypeId.value = Number(selectedPackageId.value);
          console.warn("使用套餐ID作为服务类型ID（降级方案）:", actualServiceTypeId.value);
        }
      }
    }
  } catch (error: any) {
    console.error("获取服务类型列表失败:", error);
    // API调用失败时的降级处理
    if (selectedPackageId.value) {
      actualServiceTypeId.value = Number(selectedPackageId.value);
      console.warn("API调用失败，使用套餐ID作为服务类型ID（降级方案）:", actualServiceTypeId.value);
      ElMessage.warning("获取服务类型失败，将使用默认服务类型");
    } else {
      ElMessage.error("获取服务类型失败，请刷新页面重试");
    }
  }
  
  // 实际项目中，这里应该调用API获取模板列表
  // try {
  //   const res = await memorialApi.getTemplates();
  //   if (res.data.code === 200) {
  //     memorialTemplates.value = res.data.data;
  //   }
  // } catch (error) {
  //   ElMessage.error('获取模板列表失败');
  //   console.error('获取模板列表失败:', error);
  // }
});
</script>

<style scoped lang="scss">
// 色系与首页保持一致
$primary-color: #e07a5f; // 柔和珊瑚橙
$success-color: #81b29a; // 温柔绿
$warning-color: #f2cc8f; // 奶油杏
$light-bg: #fff7f2; // 暖白
$white: #ffffff;
$text-primary: #3d2c23; // 暖深棕
$text-secondary: #6e5a4f;
$border-color: #f1e3d8;

.book-service-container {
  min-height: calc(100vh - 60px);
  padding: 32px 16px 48px;
  background: radial-gradient(
      900px 500px at 75% 35%,
      rgba(224, 122, 95, 0.16) 0%,
      rgba(255, 247, 242, 0) 60%
    ),
    linear-gradient(135deg, #fff7f2 0%, #fff 100%);

  :deep(.el-page-header) {
    max-width: 960px;
    margin: 0 auto 16px;
  }
}

.booking-main-card {
  max-width: 960px;
  margin: 0 auto;
  background-color: $white;
  border-radius: 18px;
  padding: 28px 28px 32px;
  box-shadow: 0 14px 34px rgba(61, 44, 35, 0.12);
  border: 1px solid rgba(241, 227, 216, 0.9);
}

.booking-intro {
  text-align: left;
  margin-bottom: 24px;

  h2 {
    font-size: 24px;
    color: $text-primary;
    margin-bottom: 6px;
  }

  p {
    color: $text-secondary;
    font-size: 14px;
    line-height: 1.6;
  }
}

.booking-form {
  margin-top: 8px;

  .el-form-item {
    margin-bottom: 18px;
  }

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: $text-secondary;
  }

  // 容器内部控件统一圆角
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner),
  :deep(.el-select .el-input__wrapper),
  :deep(.el-input-number .el-input__wrapper),
  :deep(.el-date-editor),
  :deep(.el-time-picker) {
    border-radius: 10px;
  }

  .package-form-item {
    margin-bottom: 22px;
  }

  .package-card {
    background: linear-gradient(
      135deg,
      rgba(224, 122, 95, 0.08) 0%,
      rgba(255, 247, 242, 1) 40%,
      #fff 100%
    );
    border-radius: 12px;
    border: 1px solid $border-color;
  }

  .package-info {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .package-tag {
      display: inline-flex;
      align-items: center;
      padding: 2px 10px;
      border-radius: 999px;
      font-size: 12px;
      color: $primary-color;
      background-color: rgba(224, 122, 95, 0.12);
      border: 1px solid rgba(224, 122, 95, 0.35);
      width: fit-content;
      margin-bottom: 4px;
    }

    h3 {
      margin: 0;
      font-size: 18px;
      color: $text-primary;
    }

    p {
      margin: 0;
      color: $text-secondary;
      font-size: 13px;
    }

    .package-hint {
      color: $text-secondary;
      opacity: 0.9;
    }
  }

  // 宠物品种自动完成样式
  .el-autocomplete {
    width: 100%;
  }

  .special-tip {
    margin-bottom: 10px;
    border-radius: 8px;
  }
}

.full-row-item {
  grid-column: 1 / -1;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 28px;

  :deep(.el-button) {
    min-width: 110px;
    border-radius: 999px;
  }

  :deep(.el-button--primary) {
    background-color: $primary-color;
    border-color: $primary-color;

    &:hover {
      filter: brightness(1.03);
    }
  }
}

@media (max-width: 768px) {
  .booking-main-card {
    padding: 20px 16px 24px;
  }

  .booking-intro h2 {
    font-size: 20px;
  }
}
</style>
