package com.edu.registration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class VerifiableTranscriptIntegrationTest {

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
    @DisplayName("Luồng trọn vẹn: Sinh viên xuất PDF xác thực -> Xác minh công khai -> Quản trị viên thu hồi -> Cấp lại -> Xác minh sau thu hồi")
    void testToanTrinhXacThucBangDiem() throws Exception {
        String tokenSv = login("sv01", "123456");
        String tokenAdmin = login("admin", "admin123");
        String tokenGv = login("gv01", "123456");

        // 1. Kiểm tra điểm cuối công khai: Không cần token xác thực
        MvcResult publicFake = mockMvc.perform(get("/api/public/bang-diem/xac-thuc/VT-NON-EXISTENT-CODE-1234-5678-9012"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andReturn();
        JsonNode fakeNode = json(publicFake);
        assertEquals("KHONG_HOP_LE", fakeNode.get("trangThaiXacMinh").asText());
        assertTrue(fakeNode.get("maSinhVien").isNull());
        assertTrue(fakeNode.get("hoTen").isNull());

        // 2. Sinh viên xuất bảng điểm điện tử xác thực PDF
        MvcResult exportRes = mockMvc.perform(get("/api/sinh-vien/me/bang-diem/pdf")
                        .header("Authorization", "Bearer " + tokenSv)
                        .param("xacThuc", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("X-Verification-Code"))
                .andExpect(header().exists("X-Verification-Url"))
                .andExpect(header().exists("X-Transcript-Version"))
                .andReturn();

        String code = exportRes.getResponse().getHeader("X-Verification-Code");
        assertNotNull(code);
        assertTrue(code.startsWith("VT-"));

        // 3. Xác minh công khai bảng điểm qua mã vừa cấp
        MvcResult verifyRes = mockMvc.perform(get("/api/public/bang-diem/xac-thuc/" + code))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode verifyNode = json(verifyRes);
        assertEquals("HOP_LE", verifyNode.get("trangThaiXacMinh").asText());
        assertNotNull(verifyNode.get("maSinhVien").asText());
        assertNotNull(verifyNode.get("hoTen").asText());
        assertNotNull(verifyNode.get("gpa10"));
        assertNotNull(verifyNode.get("fileSha256").asText());
        assertEquals(1, verifyNode.get("phienBan").asInt());

        // 4. Sinh viên xem danh sách bảng điểm xác thực của mình
        MvcResult svListRes = mockMvc.perform(get("/api/sinh-vien/me/bang-diem-xac-thuc")
                        .header("Authorization", "Bearer " + tokenSv))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode svList = json(svListRes);
        assertTrue(svList.isArray());
        assertTrue(svList.size() >= 1);
        long transcriptId = svList.get(0).get("id").asLong();

        // 5. Quản trị viên tra cứu danh sách
        MvcResult adminListRes = mockMvc.perform(get("/api/admin/bang-diem-xac-thuc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("maSinhVien", "sv01"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode adminPage = json(adminListRes);
        assertTrue(adminPage.get("totalElements").asInt() >= 1);

        // 6. Kiểm tra phân quyền bảo mật: Giảng viên hoặc Sinh viên không thể gọi API quản trị thu hồi
        mockMvc.perform(post("/api/admin/bang-diem-xac-thuc/" + transcriptId + "/thu-hoi")
                        .header("Authorization", "Bearer " + tokenGv)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lyDo", "Giảng viên thử thu hồi"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/bang-diem-xac-thuc/" + transcriptId + "/thu-hoi")
                        .header("Authorization", "Bearer " + tokenSv)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lyDo", "Sinh viên thử thu hồi"))))
                .andExpect(status().isForbidden());

        // 7. Quản trị viên thu hồi bảng điểm điện tử
        mockMvc.perform(post("/api/admin/bang-diem-xac-thuc/" + transcriptId + "/thu-hoi")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lyDo", "Đính chính thông tin theo biên bản hội đồng"))))
                .andExpect(status().isOk());

        // 8. Xác minh công khai mã đã bị thu hồi: Trả DA_THU_HOI và KHÔNG tiết lộ thông tin cá nhân
        MvcResult revokedVerify = mockMvc.perform(get("/api/public/bang-diem/xac-thuc/" + code))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode revokedNode = json(revokedVerify);
        assertEquals("DA_THU_HOI", revokedNode.get("trangThaiXacMinh").asText());
        assertEquals("Đính chính thông tin theo biên bản hội đồng", revokedNode.get("lyDoThuHoi").asText());
        assertTrue(revokedNode.get("maSinhVien").isNull());
        assertTrue(revokedNode.get("hoTen").isNull());
        assertTrue(revokedNode.get("gpa10").isNull());

        // 9. Quản trị viên cấp lại bảng điểm điện tử (v2)
        MvcResult reissueRes = mockMvc.perform(post("/api/admin/bang-diem-xac-thuc/" + transcriptId + "/cap-lai")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lyDo", "Cấp lại sau khi hoàn tất đính chính điểm"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("X-Verification-Code"))
                .andExpect(header().string("X-Transcript-Version", "2"))
                .andReturn();

        String newCode = reissueRes.getResponse().getHeader("X-Verification-Code");
        assertNotNull(newCode);
        assertFalse(newCode.equals(code), "Mã cấp lại phải hoàn toàn mới");

        // 10. Xác minh mã mới (v2) hợp lệ
        MvcResult verifyV2 = mockMvc.perform(get("/api/public/bang-diem/xac-thuc/" + newCode))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode v2Node = json(verifyV2);
        assertEquals("HOP_LE", v2Node.get("trangThaiXacMinh").asText());
        assertEquals(2, v2Node.get("phienBan").asInt());

        // 11. Quản trị viên tải PDF lưu trữ của bản ghi
        mockMvc.perform(get("/api/admin/bang-diem-xac-thuc/" + transcriptId + "/pdf")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}
