import axios from "axios";

const contactApi = {
  submitConsultation: async (data: any): Promise<void> => {
    // 实际项目中替换为真实API地址
    const response = await axios.post("/api/contact", data);
    return response.data;
  },
};

export { contactApi };
