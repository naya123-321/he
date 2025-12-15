import { defineStore } from "pinia";
import {
  memorialApi,
  type MemorialVO,
  type TemplateVO,
  type MemorialQueryDTO,
} from "@/api/memorial";

export const useMemorialStore = defineStore("memorial", {
  state: () => ({
    // 纪念册列表
    memorials: [] as MemorialVO[],
    // 当前纪念册
    currentMemorial: null as MemorialVO | null,
    // 模板列表
    templates: [] as TemplateVO[],
    // 分页信息
    pagination: {
      current: 1,
      pageSize: 10,
      total: 0,
      pages: 0,
    },
    // 加载状态
    loading: false,
    // 保存状态
    saving: false,
  }),

  getters: {
    // 获取已发布的纪念册
    publishedMemorials: (state) => {
      return state.memorials.filter((m) => m.status === 2);
    },

    // 获取编辑中的纪念册
    editingMemorials: (state) => {
      return state.memorials.filter((m) => m.status === 0 || m.status === 1);
    },
  },

  actions: {
    // 获取纪念册列表
    async fetchMemorials(params?: MemorialQueryDTO) {
      // 防止重复加载
      if (this.loading) {
        console.warn("Store: 纪念册列表正在加载中，跳过重复请求");
        return this.memorials;
      }
      
      this.loading = true;
      try {
        const queryParams = {
          pageNum: this.pagination.current,
          pageSize: this.pagination.pageSize,
          ...params,
        };

        console.log("Store: 请求纪念册列表，参数:", queryParams);
        const res = await memorialApi.getMemorialList(queryParams);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          this.memorials = res.data?.records || [];
          this.pagination = {
            current: res.data?.current || 1,
            pageSize: res.data?.size || 10,
            total: res.data?.total || 0,
            pages: res.data?.pages || 0,
          };
          console.log("Store: 纪念册列表加载成功，数量:", this.memorials.length);
          return this.memorials;
        }
        throw new Error(res?.message || '获取纪念册列表失败');
      } catch (error: any) {
        console.error("Store: 获取纪念册列表失败:", error);
        // 确保出错时清空数据
        this.memorials = [];
        this.pagination = {
          current: 1,
          pageSize: 10,
          total: 0,
          pages: 0,
        };
        throw error;
      } finally {
        this.loading = false;
        console.log("Store: 纪念册列表加载完成，loading状态已重置");
      }
    },

    // 获取纪念册详情
    async fetchMemorialDetail(id: number) {
      this.loading = true;
      try {
        const res = await memorialApi.getMemorialDetail(id);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          this.currentMemorial = res.data;
          return this.currentMemorial;
        }
        throw new Error(res?.message || '获取纪念册详情失败');
      } catch (error) {
        console.error("获取纪念册详情失败:", error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    // 创建纪念册
    async createMemorial(data: any) {
      this.loading = true;
      try {
        const res = await memorialApi.createMemorial(data);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          this.currentMemorial = res.data;
          return this.currentMemorial;
        }
        throw new Error(res?.message || '创建纪念册失败');
      } catch (error: any) {
        console.error("创建纪念册失败:", error);
        // 如果错误信息已经通过拦截器显示，这里只记录日志
        throw error;
      } finally {
        this.loading = false;
      }
    },

    // 保存纪念册内容
    async saveContent(data: any) {
      this.saving = true;
      try {
        const res = await memorialApi.saveContent(data);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          return true;
        }
        throw new Error(res?.message || '保存纪念册内容失败');
      } catch (error) {
        console.error("保存纪念册内容失败:", error);
        throw error;
      } finally {
        this.saving = false;
      }
    },

    // 自动保存
    async autoSave(id: number, contentData: any) {
      try {
        const res = await memorialApi.autoSave(id, contentData);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          return true;
        }
        throw new Error(res?.message || '自动保存失败');
      } catch (error) {
        console.error("自动保存失败:", error);
        throw error;
      }
    },

    // 发布纪念册
    async publishMemorial(data: any) {
      this.loading = true;
      try {
        const res = await memorialApi.publishMemorial(data);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          // 更新当前纪念册状态
          if (this.currentMemorial) {
            this.currentMemorial = res.data;
          }
          return this.currentMemorial;
        }
        throw new Error(res?.message || '发布纪念册失败');
      } catch (error) {
        console.error("发布纪念册失败:", error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    // 获取模板列表
    async fetchTemplates(category?: string) {
      this.loading = true;
      try {
        const res = await memorialApi.getTemplates(category);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          this.templates = res.data || [];
          return this.templates;
        }
        throw new Error(res?.message || '获取模板列表失败');
      } catch (error: any) {
        console.error("获取模板列表失败:", error);
        // 如果错误信息已经通过拦截器显示，这里只记录日志
        throw error;
      } finally {
        this.loading = false;
      }
    },

    // 上传图片
    async uploadImage(memorialId: number, file: File) {
      try {
        const res = await memorialApi.uploadImage(memorialId, file);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          return res.data;
        }
        throw new Error(res?.message || '上传图片失败');
      } catch (error) {
        console.error("上传图片失败:", error);
        throw error;
      }
    },

    // 点赞纪念册
    async likeMemorial(id: number) {
      try {
        const res = await memorialApi.likeMemorial(id);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          // 更新当前纪念册点赞数
          if (this.currentMemorial && this.currentMemorial.id === id) {
            this.currentMemorial.likeCount++;
          }
          // 更新列表中的点赞数
          const memorial = this.memorials.find((m) => m.id === id);
          if (memorial) {
            memorial.likeCount++;
          }
          return true;
        }
        throw new Error(res?.message || '点赞失败');
      } catch (error) {
        console.error("点赞失败:", error);
        throw error;
      }
    },

    // 取消点赞
    async unlikeMemorial(id: number) {
      try {
        const res = await memorialApi.unlikeMemorial(id);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          // 更新当前纪念册点赞数
          if (this.currentMemorial && this.currentMemorial.id === id) {
            this.currentMemorial.likeCount--;
          }
          // 更新列表中的点赞数
          const memorial = this.memorials.find((m) => m.id === id);
          if (memorial) {
            memorial.likeCount--;
          }
          return true;
        }
        throw new Error(res?.message || '取消点赞失败');
      } catch (error) {
        console.error("取消点赞失败:", error);
        throw error;
      }
    },

    // 收藏纪念册
    async favoriteMemorial(id: number) {
      try {
        const res = await memorialApi.favoriteMemorial(id);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          // 更新当前纪念册收藏状态
          if (this.currentMemorial && this.currentMemorial.id === id) {
            this.currentMemorial.isFavorite = true;
          }
          // 更新列表中的收藏状态
          const memorial = this.memorials.find((m) => m.id === id);
          if (memorial) {
            memorial.isFavorite = true;
          }
          return true;
        }
        throw new Error(res?.message || '收藏失败');
      } catch (error) {
        console.error("收藏失败:", error);
        throw error;
      }
    },

    // 取消收藏
    async unfavoriteMemorial(id: number) {
      try {
        const res = await memorialApi.unfavoriteMemorial(id);
        // 响应拦截器已经解包，res 直接是 { code, data, message } 结构
        if (res && res.code === 200) {
          // 更新当前纪念册收藏状态
          if (this.currentMemorial && this.currentMemorial.id === id) {
            this.currentMemorial.isFavorite = false;
          }
          // 更新列表中的收藏状态
          const memorial = this.memorials.find((m) => m.id === id);
          if (memorial) {
            memorial.isFavorite = false;
          }
          return true;
        }
        throw new Error(res?.message || '取消收藏失败');
      } catch (error) {
        console.error("取消收藏失败:", error);
        throw error;
      }
    },

    // 清空当前纪念册
    clearCurrentMemorial() {
      this.currentMemorial = null;
    },

    // 重置分页
    resetPagination() {
      this.pagination = {
        current: 1,
        pageSize: 10,
        total: 0,
        pages: 0,
      };
    },
  },
});
