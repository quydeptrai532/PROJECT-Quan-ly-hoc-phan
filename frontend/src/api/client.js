import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const code = error.response?.data?.code;

    if (status === 401 && code === "SESSION_REPLACED") {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      sessionStorage.setItem(
        "authNotice",
        error.response.data.message ||
          "Tài khoản đã được đăng nhập ở nơi khác. Vui lòng đăng nhập lại."
      );
      window.location.href = "/login?reason=session_replaced";
      return Promise.reject(error);
    }

    if (
      status === 401 &&
      !window.location.pathname.startsWith("/login") &&
      !window.location.pathname.startsWith("/xac-thuc-bang-diem")
    ) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export function getErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.message ||
    "Đã xảy ra lỗi, vui lòng thử lại"
  );
}

export default api;
