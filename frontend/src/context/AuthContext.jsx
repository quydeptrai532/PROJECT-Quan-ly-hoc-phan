import { createContext, useContext, useMemo, useState } from "react";
import api from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("user")) || null;
    } catch {
      return null;
    }
  });

  const login = async (username, password) => {
    const { data } = await api.post("/auth/login", { username, password });
    const profile = {
      username: data.username,
      role: data.role,
      hoTen: data.hoTen,
      ma: data.ma,
    };
    localStorage.setItem("token", data.token);
    localStorage.setItem("user", JSON.stringify(profile));
    setUser(profile);
    return profile;
  };

  const logout = async () => {
    try {
      await api.post("/auth/logout");
    } catch {}
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
  };

  const updateUser = (patch) => {
    setUser((current) => {
      if (!current) return current;
      const next = { ...current, ...patch };
      localStorage.setItem("user", JSON.stringify(next));
      return next;
    });
  };

  const value = useMemo(() => ({ user, login, logout, updateUser }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}

export const ROLE_LABEL = {
  SINH_VIEN: "Sinh viên",
  GIANG_VIEN: "Giảng viên",
  QUAN_TRI: "Quản trị",
};

export function homePath(role) {
  if (role === "QUAN_TRI") return "/quan-tri";
  return "/tong-quan";
}
