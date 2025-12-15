/**
 * 纪念册进度计算工具函数
 * 用于统一计算纪念册制作进度，确保服务进度页面和我的纪念册页面进度同步
 */

export interface MemorialProgressInfo {
  currentStep: number; // 当前步骤：-1未开始, 0-4对应5个步骤
  progress: number; // 进度百分比：0-100
  color: string; // 进度条颜色
}

/**
 * 根据纪念册数据计算进度，与制作页面同步
 * 步骤0: 模板选择 (0% -> 20%)
 * 步骤1: 照片上传 (20% -> 40%)
 * 步骤2: 内容编辑 (40% -> 60%)
 * 步骤3: 设计审核 (60% -> 80%)
 * 步骤4: 制作完成 (80% -> 100%)
 */
export function calculateMemorialProgress(memorialData: any): MemorialProgressInfo {
  let currentStep = -1;
  let progress = 0;
  let color = "#909399"; // 默认灰色
  
  // 步骤0: 检查是否有模板
  if (memorialData.templateId) {
    currentStep = 0;
    progress = 20;
    color = "#409eff"; // 蓝色 - 进行中
    
    // 步骤1: 检查是否有照片（coverImage 或 previewContent 中的照片）
    const hasPhotos = memorialData.coverImage || 
                      (memorialData.previewContent && 
                       memorialData.previewContent.photos && 
                       memorialData.previewContent.photos.length > 0);
    if (hasPhotos) {
      currentStep = 1;
      progress = 40;
      
      // 步骤2: 检查是否有内容（title, petMemory, previewContent等）
      const hasContent = memorialData.title || 
                         memorialData.petMemory || 
                         (memorialData.previewContent && 
                          Object.keys(memorialData.previewContent).length > 0 &&
                          (memorialData.previewContent.content || memorialData.previewContent.text));
      if (hasContent) {
        currentStep = 2;
        progress = 60;
        
        // 步骤3: 检查是否提交审核（status === 1 待审核）
        if (memorialData.status === 1) {
          currentStep = 3;
          progress = 80;
          color = "#e6a23c"; // 橙色 - 待审核
          
          // 步骤4: 检查是否已完成（status === 2 已发布）
          if (memorialData.status === 2) {
            currentStep = 4;
            progress = 100;
            color = "#67c23a"; // 绿色 - 已完成
          }
        } else if (memorialData.status === 2) {
          // 如果直接是已发布状态，说明已完成所有步骤
          currentStep = 4;
          progress = 100;
          color = "#67c23a"; // 绿色 - 已完成
        }
      }
    }
  }
  
  // 如果已下架（status === 3）
  if (memorialData.status === 3) {
    color = "#f56c6c"; // 红色 - 已下架
  }
  
  return {
    currentStep,
    progress,
    color
  };
}

/**
 * 获取纪念册步骤列表
 */
export const memorialSteps = [
  { title: "模板选择", description: "选择纪念册模板", progress: 20 },
  { title: "照片上传", description: "上传宠物照片", progress: 40 },
  { title: "内容编辑", description: "编辑纪念册内容", progress: 60 },
  { title: "设计审核", description: "审核纪念册设计", progress: 80 },
  { title: "制作完成", description: "纪念册制作完成", progress: 100 },
];


