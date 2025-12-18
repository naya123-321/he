import request from "./request";

export type OrderForecastPoint = {
  date: string; // YYYY-MM-DD
  predicted: number;
  lower?: number;
  upper?: number;
};

export type OrderForecastResult = {
  generatedAt: string;
  historyDays: number;
  model?: {
    type: string;
    order?: number[];
    confidence?: number;
    alpha?: number;
  };
  forecast: OrderForecastPoint[];
  warning?: string;
};

export const dashboardApi = {
  getOrderForecast: (days = 3, historyDays = 60, force = false) => {
    return request.get<ApiResponse<OrderForecastResult>>("/dashboard/order-forecast", {
      params: { days, historyDays, force },
    });
  },
};



