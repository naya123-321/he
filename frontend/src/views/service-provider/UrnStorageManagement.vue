<template>
  <div class="urn-storage-management-container">
    <el-page-header @back="goBack" content="骨灰寄存管理（服务人员）" />

    <el-card shadow="never" style="margin-top: 16px">
      <el-empty description="该页面暂未实现，后续可接入寄存订单/寄存记录接口" :image-size="140" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();

function goBack() {
  router.back();
}
</script>

<style scoped lang="scss">
.urn-storage-management-container {
  padding: 20px;
}
</style>



