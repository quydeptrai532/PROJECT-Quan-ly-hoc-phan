package com.edu.registration.service;

import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.repository.CourseClassRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class GradeAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json(res).get("token").asText();
    }

    private JsonNode json(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void thongKeDiemLopPhanQuyenChinhXacTachBietCongBoVaChuaCongBoKhongChuaPiiSinhVien() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        String tokenGv02 = login("gv02", "123456");
        String tokenAdmin = login("admin", "admin123");
        String tokenSv01 = login("sv01", "123456");

        // 1. Sinh viên không được phép truy cập bất kỳ API thống kê điểm lớp/môn nào
        mockMvc.perform(get("/api/giang-vien/thong-ke-diem")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/thong-ke/diem-lop")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());

        // 2. Giảng viên không được gọi API thống kê của Quản trị
        mockMvc.perform(get("/api/admin/thong-ke/diem-lop")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        // 3. Lấy danh sách lớp của gv01 và gv02
        JsonNode lopGv01 = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode lopGv02 = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isOk())
                .andReturn());
        assertTrue(lopGv01.isArray() && !lopGv01.isEmpty());
        assertTrue(lopGv02.isArray() && !lopGv02.isEmpty());

        long idLopGv02 = lopGv02.get(0).get("id").asLong();

        // 4. Giảng viên gv01 truy cập thống kê lớp của gv02 phải bị chặn 403 Forbidden
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + idLopGv02 + "/thong-ke-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/giang-vien/thong-ke-diem")
                        .param("lopHocPhanId", String.valueOf(idLopGv02))
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        // 5. Tìm 1 lớp NHAP của gv01 có sinh viên đăng ký để kiểm chứng tách biệt dữ liệu chưa công bố vs đã công bố
        Long targetLopId = null;
        JsonNode danhSachSv = null;
        for (JsonNode lop : lopGv01) {
            long id = lop.get("id").asLong();
            if (!"NHAP".equals(lop.path("trangThaiBangDiem").asText("NHAP"))) {
                continue;
            }
            JsonNode svList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + id + "/sinh-vien")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk())
                    .andReturn());
            if (svList.isArray() && !svList.isEmpty()) {
                targetLopId = id;
                danhSachSv = svList;
                break;
            }
        }
        assertNotNull(targetLopId);
        final long lopId = targetLopId;

        try {
            // Nhập điểm ở trạng thái NHAP cho các sinh viên trong lớp
            List<Map<String, Object>> items = new ArrayList<>();
            int idx = 0;
            for (JsonNode sv : danhSachSv) {
                double diem = (idx % 2 == 0) ? 8.5 : 4.0;
                items.add(Map.of(
                        "dangKyId", sv.get("dangKyId").asLong(),
                        "diemQuaTrinh", diem,
                        "diemCuoiKy", diem
                ));
                idx++;
            }
            mockMvc.perform(put("/api/giang-vien/lop-hoc-phan/" + lopId + "/diem")
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("danhSach", items))))
                    .andExpect(status().isOk());

            // 6. Khi lớp còn ở trạng thái NHAP:
            // - thongKeCongBo.soBanGhiDuocTinh == 0 (không trộn điểm chưa công bố vào thống kê chính thức)
            // - thongKeTamTinhChuaCongBo có dữ liệu tạm tính cho giảng viên phụ trách và gắn nhãn trạng thái rõ ràng
            MvcResult resNhap = mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/thong-ke-diem")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lopHocPhanId").value(lopId))
                    .andExpect(jsonPath("$.daCongBo").value(false))
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("NHAP"))
                    .andExpect(jsonPath("$.thongKeCongBo.soBanGhiDuocTinh").value(0))
                    .andExpect(jsonPath("$.thongKeCongBo.soSinhVienChuaCongBoDiem").value(danhSachSv.size()))
                    .andExpect(jsonPath("$.thongKeTamTinhChuaCongBo.phamViDuLieu").value(GradeAnalyticsService.PHAM_VI_TAM_TINH))
                    .andExpect(jsonPath("$.thongKeTamTinhChuaCongBo.soBanGhiDuocTinh").value(danhSachSv.size()))
                    .andReturn();

            String rawJsonNhap = resNhap.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertFalse(rawJsonNhap.contains("maSinhVien"), "Response thống kê không được rò rỉ mã sinh viên");
            assertFalse(rawJsonNhap.contains("sinhVienId"), "Response thống kê không được rò rỉ ID sinh viên");

            // 7. Gửi duyệt -> Duyệt -> Công bố bảng điểm
            mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/gui-duyet-diem")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/phe-duyet-diem")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/cong-bo-diem")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk());

            // 8. Sau khi công bố: thongKeCongBo có đầy đủ chỉ số và tổng các nhóm phân bố khớp soBanGhiDuocTinh
            JsonNode tkSauCongBo = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/thong-ke-diem")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.daCongBo").value(true))
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("DA_CONG_BO"))
                    .andExpect(jsonPath("$.thongKeCongBo.soBanGhiDuocTinh").value(danhSachSv.size()))
                    .andExpect(jsonPath("$.thongKeCongBo.tongSoTrongPhanBo").value(danhSachSv.size()))
                    .andReturn());

            long sumBuckets = 0;
            for (JsonNode b : tkSauCongBo.path("thongKeCongBo").path("phanBoDiem")) {
                sumBuckets += b.path("soLuong").asLong();
            }
            assertEquals(danhSachSv.size(), sumBuckets, "Tổng số lượng ở các khoảng điểm phải khớp số bản ghi hợp lệ");

            // 9. Kiểm tra API báo cáo tổng hợp của Quản trị và Giảng viên với bộ lọc lopHocPhanId
            mockMvc.perform(get("/api/admin/thong-ke/diem-lop")
                            .param("lopHocPhanId", String.valueOf(lopId))
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cauHinh.nguongDat").value(5.0))
                    .andExpect(jsonPath("$.cauHinh.laQuyCheChinhThuc").value(false))
                    .andExpect(jsonPath("$.tongSoLopTrongPhamVi").value(1))
                    .andExpect(jsonPath("$.soLopDaCongBo").value(1))
                    .andExpect(jsonPath("$.tongHopCongBo.soBanGhiDuocTinh").value(danhSachSv.size()));

            mockMvc.perform(get("/api/giang-vien/thong-ke-diem")
                            .param("lopHocPhanId", String.valueOf(lopId))
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tongSoLopTrongPhamVi").value(1))
                    .andExpect(jsonPath("$.tongHopCongBo.soBanGhiDuocTinh").value(danhSachSv.size()));
        } finally {
            // Khôi phục trạng thái lớp về NHAP để không ảnh hưởng dữ liệu mẫu
            CourseClass lop = lopHocPhanRepository.findById(lopId).orElse(null);
            if (lop != null) {
                lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
                lopHocPhanRepository.save(lop);
            }
        }
    }
}
