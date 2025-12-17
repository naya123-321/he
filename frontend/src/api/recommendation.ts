import request from "./request";

export type VisitorPetProfile = {
  petType: string;
  petAge: number;
  deathCause: string;
  budgetMin?: number;
  budgetMax?: number;
  participants?: number;
};

export type PackageRecommendationRequest = VisitorPetProfile & {
  // 预留：后续可传更多特征
};

export type PackageRecommendationResult = {
  recommendedPackageId?: number;
  recommendedPackageName?: string;
  score?: number; // 0-100
  similarUsers?: number;
  analysis?: string[];
  algorithm?: string;
  ruleHitSummary?: string[];
  warning?: string;
};

export const recommendationApi = {
  getPackageRecommendation: (data: PackageRecommendationRequest) => {
    return request.post("/recommendation/package", data);
  },
};


