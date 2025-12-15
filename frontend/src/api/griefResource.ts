import request from "./request";

export interface ApiResponse<T> {
  code: number;
  data: T;
  message: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface GriefResourceCreateDTO {
  title: string;
  summary?: string;
  type: "article" | "video";
  content?: string;
  fileUrl?: string;
  coverImage?: string;
  author?: string;
  status?: number; // 1启用 0禁用
  memberCount?: number;
}

export interface GriefResourceVO {
  id: number;
  title: string;
  summary?: string;
  type: "article" | "video";
  typeText?: string;
  content?: string;
  fileUrl?: string;
  coverImage?: string;
  author?: string;
  status: number;
  statusText?: string;
  likeCount: number;
  createTime: string;
  updateTime?: string;
}

export const griefResourceApi = {
  // 管理端：分页查询资源
  getGriefResourceList: (params?: {
    type?: string;
    status?: number;
    pageNum?: number;
    pageSize?: number;
  }) => {
    return request.get<ApiResponse<PageResult<GriefResourceVO>>>(
      "/grief-resource/list",
      { params }
    );
  },

  // 管理端：详情
  getGriefResourceDetail: (id: number) => {
    return request.get<ApiResponse<GriefResourceVO>>(
      `/grief-resource/detail/${id}`
    );
  },

  // 管理端：创建
  createGriefResource: (data: GriefResourceCreateDTO) => {
    return request.post<ApiResponse<GriefResourceVO>>(
      "/grief-resource/create",
      data
    );
  },

  // 管理端：更新
  updateGriefResource: (id: number, data: GriefResourceCreateDTO) => {
    return request.put<ApiResponse<GriefResourceVO>>(`/grief-resource/${id}`, data);
  },

  // 管理端：删除
  deleteGriefResource: (id: number) => {
    return request.delete<ApiResponse<boolean>>(`/grief-resource/${id}`);
  },

  // 管理端：更新状态
  updateGriefResourceStatus: (id: number, status: number) => {
    return request.put<ApiResponse<GriefResourceVO>>(
      `/grief-resource/${id}/status`,
      null,
      { params: { status } }
    );
  },

  // 宠主端：获取启用资源（可按类型筛选）
  getEnabledGriefResources: (type?: string) => {
    return request.get<ApiResponse<GriefResourceVO[]>>("/grief-resource/enabled", {
      params: { type },
    });
  },
};



