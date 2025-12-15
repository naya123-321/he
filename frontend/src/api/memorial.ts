import request from "./request";

// 纪念册相关API
export const memorialApi = {
  // 获取模板列表（公开接口，用于宠主端选择）
  getTemplates: (category?: string) => {
    return request.get<ApiResponse<TemplateVO[]>>("/template/enabled", {
      params: { category },
    });
  },

  // 创建纪念册
  createMemorial: (data: MemorialCreateDTO) => {
    return request.post<ApiResponse<MemorialVO>>("/memorial/create", data);
  },

  // 保存内容
  saveContent: (data: MemorialContentDTO) => {
    return request.post<ApiResponse<boolean>>("/memorial/save-content", data);
  },

  // 自动保存
  autoSave: (id: number, contentData: any) => {
    return request.post<ApiResponse<boolean>>(
      `/memorial/auto-save/${id}`,
      contentData
    );
  },

  // 发布纪念册
  publishMemorial: (data: MemorialPublishDTO) => {
    return request.post<ApiResponse<MemorialVO>>("/memorial/publish", data);
  },

  // 生成PDF
  generatePdf: (id: number) => {
    return request.post<ApiResponse<string>>(`/memorial/generate-pdf/${id}`);
  },

  // 获取纪念册列表
  getMemorialList: (params: MemorialQueryDTO) => {
    return request.get<ApiResponse<PageResult<MemorialVO>>>("/memorial/list", {
      params,
    });
  },

  // 获取纪念册详情
  getMemorialDetail: (id: number) => {
    return request.get<ApiResponse<MemorialVO>>(`/memorial/detail/${id}`);
  },

  // 根据订单ID获取纪念册
  getMemorialByOrderId: (orderId: number) => {
    return request.get<ApiResponse<MemorialVO>>(`/memorial/by-order/${orderId}`);
  },

  // 上传图片
  uploadImage: (memorialId: number, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return request.post<ApiResponse<string>>(
      `/memorial/upload-image/${memorialId}`,
      formData
    );
  },

  // 点赞
  likeMemorial: (id: number) => {
    return request.post<ApiResponse<boolean>>(`/memorial/like/${id}`);
  },

  // 取消点赞
  unlikeMemorial: (id: number) => {
    return request.post<ApiResponse<boolean>>(`/memorial/unlike/${id}`);
  },

  // 收藏
  favoriteMemorial: (id: number) => {
    return request.post<ApiResponse<boolean>>(`/memorial/favorite/${id}`);
  },

  // 取消收藏
  unfavoriteMemorial: (id: number) => {
    return request.post<ApiResponse<boolean>>(`/memorial/unfavorite/${id}`);
  },

  // 删除纪念册
  deleteMemorial: (id: number) => {
    return request.delete<ApiResponse<boolean>>(`/memorial/${id}`);
  },
};

// 模板管理相关API（管理员和服务人员）
export const templateApi = {
  // 获取模板列表（管理员和服务人员）
  getTemplateList: (params?: {
    keyword?: string;
    status?: number;
    category?: string;
  }) => {
    return request.get<ApiResponse<TemplateVO[]>>("/template/list", {
      params,
    });
  },

  // 获取启用的模板（公开接口）
  getEnabledTemplates: (category?: string) => {
    return request.get<ApiResponse<TemplateVO[]>>("/template/enabled", {
      params: { category },
    });
  },

  // 根据ID获取模板详情
  getTemplateById: (id: number) => {
    return request.get<ApiResponse<TemplateVO>>(`/template/${id}`);
  },

  // 创建模板
  createTemplate: (data: TemplateCreateDTO) => {
    return request.post<ApiResponse<TemplateVO>>("/template/create", data);
  },

  // 更新模板
  updateTemplate: (id: number, data: TemplateCreateDTO) => {
    return request.put<ApiResponse<TemplateVO>>(`/template/${id}`, data);
  },

  // 删除模板（仅管理员）
  deleteTemplate: (id: number) => {
    return request.delete<ApiResponse<boolean>>(`/template/${id}`);
  },

  // 更新模板状态
  updateTemplateStatus: (id: number, status: number) => {
    return request.put<ApiResponse<TemplateVO>>(`/template/${id}/status`, null, {
      params: { status },
    });
  },

  // 上传模板预览图
  uploadPreviewImage: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return request.post<ApiResponse<string>>("/template/upload-preview", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  },
};

// 类型定义
export interface TemplateCreateDTO {
  name: string;
  description?: string;
  category?: string;
  previewImage?: string;
  templateImages?: string[];
  styleConfig?: string;
  layoutConfig?: string;
}

export interface TemplateVO {
  id: number;
  name: string;
  description?: string;
  category?: string;
  categoryText?: string;
  previewImage?: string;
  templateImages?: string[];
  styleConfig?: string;
  layoutConfig?: string;
  status?: number;
  statusText?: string;
  createTime?: string;
  updateTime?: string;
  createdBy?: number;
  createdByName?: string;
}

export interface MemorialCreateDTO {
  title: string;
  subtitle?: string;
  templateId: number;
  orderId?: number;
  petName: string;
  petType?: string;
  petBirthDate?: string;
  petDeathDate?: string;
  petMemory?: string;
}

export interface MemorialContentDTO {
  memorialId: number;
  contentData: any;
  styleConfig?: any;
}

export interface MemorialPublishDTO {
  memorialId: number;
  isPublic?: boolean;
  shareMessage?: string;
}

export interface MemorialQueryDTO {
  userId?: number;
  status?: number;
  petName?: string;
  title?: string;
  sortBy?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface MemorialVO {
  id: number;
  userId: number;
  username: string;
  avatar?: string;
  title: string;
  subtitle?: string;
  coverImage?: string;
  templateId: number;
  templateName: string;
  templateCategory: string;
  petName: string;
  petType?: string;
  petBirthDate?: string;
  petDeathDate?: string;
  petMemory?: string;
  status: number;
  statusText: string;
  viewCount: number;
  likeCount: number;
  shareCount: number;
  commentCount: number;
  isFavorite: boolean;
  isOwner: boolean;
  pdfUrl?: string;
  pdfSize?: number;
  lastEditTime: string;
  publishTime?: string;
  createTime: string;
  previewContent?: any;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}
