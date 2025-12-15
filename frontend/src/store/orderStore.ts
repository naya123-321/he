import { defineStore } from "pinia";
import { ref } from "vue";
import type { OrderVO } from "@/api/order";
import { orderApi } from "@/api/order";

export const useOrderStore = defineStore("order", () => {
  // 状态
  const orderList = ref<OrderVO[]>([]);
  const orderDetail = ref<OrderVO | null>(null);
  const loading = ref(false);

  // 获取订单列表
  const fetchOrderList = async (params?: any) => {
    loading.value = true;
    try {
      const res = await orderApi.getOrderList(params);
      if (res.data.code === 200) {
        orderList.value = res.data.data.records || [];
      }
      return res.data;
    } catch (error) {
      console.error("获取订单列表失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取订单详情
  const fetchOrderDetail = async (id: string) => {
    loading.value = true;
    try {
      const res = await orderApi.getOrderDetail(Number(id));
      if (res.data.code === 200) {
        orderDetail.value = res.data.data;
      }
      return res.data;
    } catch (error) {
      console.error("获取订单详情失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 创建订单
  const createOrder = async (orderData: any) => {
    loading.value = true;
    try {
      const res = await orderApi.createOrder(orderData);
      if (res.data.code === 200) {
        return res.data.data;
      }
      throw new Error(res.data.message || "创建订单失败");
    } catch (error) {
      console.error("创建订单失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 取消订单
  const cancelOrder = async (
    id: string,
    reason: string,
    description?: string
  ) => {
    loading.value = true;
    try {
      const res = await orderApi.cancelOrder(Number(id), reason);
      if (res.data.code === 200) {
        // 更新本地订单状态
        if (orderDetail.value && orderDetail.value.id === Number(id)) {
          orderDetail.value.status = 4; // CANCELLED status
        }
        return true;
      }
      throw new Error(res.data.message || "取消订单失败");
    } catch (error) {
      console.error("取消订单失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 评价订单
  const reviewOrder = async (id: string, rating: number, content: string) => {
    loading.value = true;
    try {
      const res = await orderApi.completeOrder(Number(id), rating, content);
      if (res.data.code === 200) {
        // 更新本地订单状态
        if (orderDetail.value && orderDetail.value.id === Number(id)) {
          orderDetail.value.status = 3; // COMPLETED status
          orderDetail.value.rating = rating;
          orderDetail.value.review = content;
        }
        return true;
      }
      throw new Error(res.data.message || "评价订单失败");
    } catch (error) {
      console.error("评价订单失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 添加服务记录
  const addServiceRecord = async (orderId: string, recordData: any) => {
    loading.value = true;
    try {
      const res = await orderApi.addServiceNote(
        Number(orderId),
        recordData.content || recordData.remark || ""
      );
      if (res.data.code === 200) {
        // 刷新订单详情
        if (orderDetail.value && orderDetail.value.id === Number(orderId)) {
          await fetchOrderDetail(orderId);
        }
        return res.data.data;
      }
      throw new Error(res.data.message || "添加服务记录失败");
    } catch (error) {
      console.error("添加服务记录失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  return {
    // 状态
    orderList,
    orderDetail,
    loading,

    // 方法
    fetchOrderList,
    fetchOrderDetail,
    createOrder,
    cancelOrder,
    reviewOrder,
    addServiceRecord,
  };
});
