package com.edu.registration.service;

import com.edu.registration.util.XlsxWorkbookSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class GradeImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void luongEndToEndTaiTemplatePreviewConfirmVaKiemTraQuyenGiangVien() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        String tokenGv02 = login("gv02", "123456");

        // Tìm một lớp học phần của gv01 có trạng thái NHAP và có sinh viên đăng ký
        JsonNode lopCuaGv01 = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        Long targetLopId = null;
        JsonNode danhSachSv = null;
        for (JsonNode lop : lopCuaGv01) {
            long id = lop.get("id").asLong();
            String trangThaiBangDiem = lop.path("trangThaiBangDiem").asText("NHAP");
            if (!"NHAP".equals(trangThaiBangDiem)) {
                continue;
            }
            JsonNode svList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + id + "/sinh-vien")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk())
                    .andReturn());
            if (svList.isArray() && svList.size() >= 2) {
                targetLopId = id;
                danhSachSv = svList;
                break;
            }
        }
        assertNotNull(targetLopId, "Cần tìm thấy ít nhất 1 lớp nháp của gv01 có >= 2 sinh viên");

        // 1. Giảng viên khác (gv02) không được tải template hoặc preview lớp của gv01
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + targetLopId + "/diem/template-xlsx")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isForbidden());

        // 2. Giảng viên phụ trách (gv01) tải template thành công
        MvcResult tplRes = mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + targetLopId + "/diem/template-xlsx")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn();
        byte[] templateBytes = tplRes.getResponse().getContentAsByteArray();
        assertTrue(templateBytes.length > 100);

        // 3. Tạo workbook .xlsx hợp lệ cho 2 sinh viên đầu của lớp
        String maSv1 = danhSachSv.get(0).get("maSinhVien").asText();
        String hoTen1 = danhSachSv.get(0).get("hoTen").asText();
        String maSv2 = danhSachSv.get(1).get("maSinhVien").asText();
        String hoTen2 = danhSachSv.get(1).get("hoTen").asText();

        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("maSinhVien", "hoTen", "diemQuaTrinh", "diemCuoiKy"));
        rows.add(List.of(maSv1, hoTen1, 8.5, 9.0));
        rows.add(List.of(maSv2, hoTen2, 7.0, 8.0));

        byte[] uploadBytes = XlsxWorkbookSupport.createWorkbookBytes(Map.of("BangDiem", rows));
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "import-diem.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE, uploadBytes);

        // 4. Gọi API preview
        MvcResult previewRes = mockMvc.perform(multipart("/api/giang-vien/lop-hoc-phan/" + targetLopId + "/diem/import-xlsx/preview")
                        .file(multipartFile)
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tongSoDong").value(2))
                .andExpect(jsonPath("$.soDongHopLe").value(2))
                .andExpect(jsonPath("$.soDongLoi").value(0))
                .andExpect(jsonPath("$.coTheXacNhan").value(true))
                .andReturn();

        JsonNode previewJson = json(previewRes);
        String previewToken = previewJson.get("previewToken").asText();
        String fileSha256 = previewJson.get("fileSha256").asText();

        // 5. Giảng viên khác (gv02) không được dùng previewToken của gv01 để confirm
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + targetLopId + "/diem/import-xlsx/confirm")
                        .header("Authorization", "Bearer " + tokenGv02)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "previewToken", previewToken,
                                "fileSha256", fileSha256
                        ))))
                .andExpect(status().isForbidden());

        // 6. Giảng viên phụ trách (gv01) xác nhận import thành công
        MvcResult confirmRes = mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + targetLopId + "/diem/import-xlsx/confirm")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "previewToken", previewToken,
                                "fileSha256", fileSha256
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tongSoDong").value(2))
                .andExpect(jsonPath("$.soThatBai").value(0))
                .andExpect(jsonPath("$.transactionPolicy").value("ALL_OR_NOTHING"))
                .andReturn();

        JsonNode confirmJson = json(confirmRes);
        assertEquals(2, confirmJson.get("soTaoMoi").asInt() + confirmJson.get("soCapNhat").asInt());

        // 7. Kiểm tra điểm đã được lưu chính xác trong danh sách sinh viên của lớp
        JsonNode afterSvList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + targetLopId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        for (JsonNode sv : afterSvList) {
            if (maSv1.equals(sv.get("maSinhVien").asText())) {
                assertEquals(8.5, sv.get("diemQuaTrinh").asDouble(), 0.001);
                assertEquals(9.0, sv.get("diemCuoiKy").asDouble(), 0.001);
                assertEquals(8.8, sv.get("diemTongKet").asDouble(), 0.001);
            } else if (maSv2.equals(sv.get("maSinhVien").asText())) {
                assertEquals(7.0, sv.get("diemQuaTrinh").asDouble(), 0.001);
                assertEquals(8.0, sv.get("diemCuoiKy").asDouble(), 0.001);
                assertEquals(7.6, sv.get("diemTongKet").asDouble(), 0.001);
            }
        }
    }
}
