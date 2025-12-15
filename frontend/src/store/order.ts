import { defineStore } from "pinia";
import { orderApi, type OrderVO, type OrderQueryDTO } from "@/api/order";

export const useOrderStore = defineStore("order", {
  state: () => ({
    // 当前订单列表
    orders: [] as OrderVO[],
    // 分页信息
    pagination: {
      total: 0,
      pageSize: 10,
      current: 1,
    },
    // 当前选中的订单
    currentOrder: null as OrderVO | null,
    // 订单统计
    stats: {
      pending: 0,
      scheduled: 0,
      inService: 0,
      completed: 0,
      cancelled: 0,
    },
  }),

  getters: {
    // 获取不同状态的订单
    pendingOrders: (state) => state.orders.filter((o) => o.status === 0),
    scheduledOrders: (state) => state.orders.filter((o) => o.status === 1),
    inServiceOrders: (state) => state.orders.filter((o) => o.status === 2),
    completedOrders: (state) => state.orders.filter((o) => o.status === 3),
    cancelledOrders: (state) => state.orders.filter((o) => o.status === 4),
  },

  actions: {
    // 查询订单列表
    async fetchOrders(params: OrderQueryDTO = {}) {
      try {
        // request.ts 的响应拦截器已经处理了响应，如果 code !== 200 会 reject
        // 所以这里 res 已经是处理后的数据，结构是 { code, message, data }
        const res = await orderApi.getOrderList({
          pageNum: params.pageNum || this.pagination.current,
          pageSize: params.pageSize || this.pagination.pageSize,
          ...params,
        });

        if (res && res.code === 200 && res.data) {
          this.orders = res.data.records;
          this.pagination = {
            total: res.data.total,
            pageSize: res.data.size,
            current: res.data.current,
          };
        }
        return res.data;
      } catch (error) {
        console.error("获取订单列表失败:", error);
        throw error;
      }
    },

    // 为了兼容性，添加getOrders方法
    async getOrders(params: OrderQueryDTO = {}) {
      return this.fetchOrders(params);
    },

    // 获取订单详情
    async fetchOrderDetail(id: number) {
      try {
        // request.ts 的响应拦截器已经处理了响应，如果 code !== 200 会 reject
        // 所以这里 res 已经是处理后的数据，结构是 { code, message, data }
        const res = await orderApi.getOrderDetail(id);
        if (res && res.code === 200 && res.data) {
          this.currentOrder = res.data;
        }
        return res.data;
      } catch (error) {
        console.error("获取订单详情失败:", error);
        throw error;
      }
    },

    // 创建订单
    async createOrder(data: any) {
      try {
        // request.ts 的响应拦截器已经处理：
        // - 如果 code !== 200，会 reject Promise
        // - 如果 code === 200，会返回 res（整个响应对象 { code, message, data }）
        const res = await orderApi.createOrder(data);
        // 如果到达这里，说明 code === 200，res 的结构是 { code: 200, message: "...", data: {...} }
        // 创建成功后重新加载列表
        await this.fetchOrders();
        return res.data;
      } catch (error: any) {
        console.error("创建订单失败:", error);
        // 错误信息已经在 request.ts 的拦截器中显示了
        throw error;
      }
    },

    // 取消订单
    async cancelOrder(id: number, reason: string) {
      try {
        // request.ts 的响应拦截器已经处理了响应，如果 code !== 200 会 reject
        // 所以这里 res 已经是处理后的数据，结构是 { code, message, data }
        const res = await orderApi.cancelOrder(id, reason);
        if (res && res.code === 200) {
          // 更新本地数据
          const index = this.orders.findIndex((o) => o.id === id);
          if (index !== -1) {
            this.orders[index].status = 4;
            this.orders[index].statusText = "已取消";
          }
        }
        return res && res.code === 200;
      } catch (error) {
        console.error("取消订单失败:", error);
        throw error;
      }
    },

    // 更新订单状态
    async updateOrderStatus(data: {
      orderId: number;
      targetStatus: number;
      remark?: string;
    }) {
      try {
        // request.ts 的响应拦截器已经处理了响应，如果 code !== 200 会 reject
        // 所以这里 res 已经是处理后的数据，结构是 { code, message, data }
        const res = await orderApi.updateOrderStatus(data);
        if (res && res.code === 200) {
          // 更新本地数据
          const index = this.orders.findIndex((o) => o.id === data.orderId);
          if (index !== -1) {
            this.orders[index].status = data.targetStatus;
            // TODO: 更新状态文本
          }
        }
        return res && res.code === 200;
      } catch (error) {
        console.error("更新订单状态失败:", error);
        throw error;
      }
    },

    // 清空当前订单
    clearCurrentOrder() {
      this.currentOrder = null;
    },
  },
});
