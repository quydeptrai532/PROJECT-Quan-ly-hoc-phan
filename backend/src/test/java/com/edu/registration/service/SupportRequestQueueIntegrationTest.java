package com.edu.registration.service;

import com.edu.registration.config.SupportRequestProperties;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.SupportRequest;
import com.edu.registration.entity.User;
import com.edu.registration.repository.SupportRequestRepository;
import com.edu.registration.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SupportRequestQueueIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SupportRequestRepository yeuCauRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SupportRequestProperties properties;
    @Autowired private SupportBusinessCalendarService calendarService;

    private String tokenSv01;
    private String tokenSv02;
    private String tokenAdmin;
    private User adminUser;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("token").asText();
    }

    @BeforeEach
    void setUp() throws Exception {
        tokenSv01 = login("sv01", "123456");
        tokenSv02 = login("sv02", "123456");
        tokenAdmin = login("admin", "admin123");
        adminUser = userRepository.findByUsername("admin").orElseThrow();
    }

    @AfterEach
    void tearDown() {
        File dir = new File(properties.storageDir());
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".pdf")) {
                        f.delete();
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("QLSV-87: Danh mục hỗ trợ trả về đầy đủ mức ưu tiên, danh sách người xử lý và ghi chú SLA dự kiến")
    void testSupportCatalogContainsPrioritiesHandlersAndSlaNote() throws Exception {
        mockMvc.perform(get("/api/yeu-cau-ho-tro/danh-muc")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mucUuTien").isArray())
                .andExpect(jsonPath("$.mucUuTien[0].ma").value("THAP"))
                .andExpect(jsonPath("$.danhSachNguoiXuLy").isArray())
                .andExpect(jsonPath("$.officialSlaEnabled").value(false))
                .andExpect(jsonPath("$.slaPolicyNote").isNotEmpty())
                .andExpect(jsonPath("$.workingCalendarNote").isNotEmpty());
    }

    @Test
    @DisplayName("QLSV-87: Quản trị viên xem hàng đợi và tóm tắt KPI hàng đợi SLA")
    void testSupportQueueSummaryAndQueueFiltering() throws Exception {
        mockMvc.perform(get("/api/admin/yeu-cau-ho-tro/queue/summary")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.officialSlaEnabled").value(false))
                .andExpect(jsonPath("$.slaNote").isNotEmpty())
                .andExpect(jsonPath("$.workingCalendarNote").isNotEmpty());

        mockMvc.perform(get("/api/admin/yeu-cau-ho-tro/queue")
                        .param("scope", "ALL")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("QLSV-87: Sinh viên không được phép truy cập hàng đợi của nhân viên (403 Forbidden)")
    void testStudentCannotAccessQueue() throws Exception {
        mockMvc.perform(get("/api/admin/yeu-cau-ho-tro/queue"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/yeu-cau-ho-tro/queue")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("QLSV-87: Toàn bộ vòng đời xử lý hàng đợi: phân loại, gán người xử lý, mức ưu tiên, hạn xử lý, trao đổi nội bộ vs công khai, yêu cầu bổ sung minh chứng, giải quyết và đóng")
    void testCompleteQueueLifecycle() throws Exception {
        // 1. Sinh viên tạo phiếu hỗ trợ
        byte[] pdfBytes = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
        MockMultipartFile file = new MockMultipartFile("files", "minh-chung.pdf", "application/pdf", pdfBytes);

        String createResp = mockMvc.perform(multipart("/api/yeu-cau-ho-tro")
                        .file(file)
                        .param("loaiYeuCau", "HOC_PHAN")
                        .param("tieuDe", "Đăng ký bổ sung lớp học phần Toán cao cấp A1")
                        .param("noiDung", "Em bị trùng lịch đăng ký nên kính đề nghị Phòng Đào tạo hỗ trợ ghép lớp bổ sung.")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.maPhieu").isNotEmpty())
                .andExpect(jsonPath("$.trangThai").value("MOI_TAO"))
                .andExpect(jsonPath("$.mucUuTien").value("TRUNG_BINH"))
                .andExpect(jsonPath("$.hanXuLy").isNotEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode createdNode = objectMapper.readTree(createResp);
        long ticketId = createdNode.get("id").asLong();
        String maPhieu = createdNode.get("maPhieu").asText();

        // 2. Phân công người xử lý
        String phanCongPayload = """
                {
                    "handlerUserId": %d,
                    "ghiChu": "Phân công chuyên viên tiếp nhận xử lý"
                }
                """.formatted(adminUser.getId());

        mockMvc.perform(patch("/api/admin/yeu-cau-ho-tro/" + ticketId + "/phan-cong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(phanCongPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nguoiXuLyUsername").value("admin"))
                .andExpect(jsonPath("$.trangThai").value("DANG_XU_LY"));

        // 3. Phân loại, điều chỉnh mức ưu tiên và hạn xử lý (Triage)
        LocalDateTime customDeadline = LocalDateTime.now().plusDays(5);
        String triagePayload = """
                {
                    "mucUuTien": "CAO",
                    "hanXuLy": "%s",
                    "ghiChu": "Học phần sắp kết thúc đợt đăng ký nên nâng mức ưu tiên Cao"
                }
                """.formatted(customDeadline.toString());

        mockMvc.perform(patch("/api/admin/yeu-cau-ho-tro/" + ticketId + "/triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(triagePayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mucUuTien").value("CAO"))
                .andExpect(jsonPath("$.tenMucUuTien").value("Cao"));

        // 4. Trao đổi: Nhân viên viết ghi chú nội bộ (Sinh viên KHÔNG được thấy)
        String internalNotePayload = """
                {
                    "noiDung": "Đã kiểm tra sĩ số lớp Toán A1 còn 2 chỗ, chờ xác nhận giảng viên phụ trách.",
                    "isNoiBo": true
                }
                """;
        mockMvc.perform(post("/api/yeu-cau-ho-tro/" + ticketId + "/trao-doi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(internalNotePayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isNoiBo").value(true));

        // Kiểm tra: Sinh viên xem chi tiết phiếu -> KHÔNG thấy ghi chú nội bộ!
        String studentViewResp = mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId)
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode studentView = objectMapper.readTree(studentViewResp);
        JsonNode exchangesForStudent = studentView.get("traoDoi");
        assertEquals(0, exchangesForStudent.size(), "Sinh viên không được thấy phản hồi nội bộ!");

        // 5. Yêu cầu bổ sung minh chứng
        String reqEvidencePayload = """
                {
                    "noiDung": "Vui lòng đính kèm đơn xin ghép lớp có chữ ký đồng ý của Giảng viên phụ trách môn.",
                    "hanBoSung": "%s"
                }
                """.formatted(LocalDateTime.now().plusDays(2).toString());

        mockMvc.perform(post("/api/admin/yeu-cau-ho-tro/" + ticketId + "/yeu-cau-minh-chung")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqEvidencePayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("CHO_PHAN_HOI_SINH_VIEN"))
                .andExpect(jsonPath("$.yeuCauBoSungMinhChung").value(true))
                .andExpect(jsonPath("$.sla.slaStatus").value("PAUSED"));

        // 6. Sinh viên bổ sung minh chứng
        MockMultipartFile secondFile = new MockMultipartFile("files", "don-dong-y-gv.pdf", "application/pdf", pdfBytes);
        mockMvc.perform(multipart("/api/yeu-cau-ho-tro/" + ticketId + "/bo-sung-minh-chung")
                        .file(secondFile)
                        .param("noiDung", "Em đã xin được chữ ký xác nhận của Thầy phụ trách, em xin gửi đính kèm ạ.")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("DANG_XU_LY"))
                .andExpect(jsonPath("$.yeuCauBoSungMinhChung").value(false))
                .andExpect(jsonPath("$.tepDinhKem.length()").value(2));

        // IDOR Check: Sinh viên khác không được xem phiếu
        mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId)
                        .header("Authorization", "Bearer " + tokenSv02))
                .andExpect(status().isForbidden());

        // 7. Giải quyết phiếu (Resolve)
        String resolvePayload = """
                {
                    "trangThai": "DA_GIAI_QUYET",
                    "ghiChu": "Đã thêm sinh viên vào danh sách lớp học phần Toán cao cấp A1 thành công."
                }
                """;
        mockMvc.perform(patch("/api/admin/yeu-cau-ho-tro/" + ticketId + "/trang-thai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolvePayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("DA_GIAI_QUYET"))
                .andExpect(jsonPath("$.sla.resolvedAt").isNotEmpty())
                .andExpect(jsonPath("$.sla.slaStatus").value("RESOLVED_ON_TIME"));

        // 8. Đóng phiếu (Close)
        String closePayload = """
                {
                    "trangThai": "DA_DONG",
                    "ghiChu": "Hoàn tất đóng hồ sơ yêu cầu."
                }
                """;
        mockMvc.perform(patch("/api/admin/yeu-cau-ho-tro/" + ticketId + "/trang-thai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closePayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("DA_DONG"));

        // Kiểm tra lịch sử trạng thái & thay đổi không bị xóa hay ghi đè
        String finalAdminViewResp = mockMvc.perform(get("/api/admin/yeu-cau-ho-tro/" + ticketId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode finalView = objectMapper.readTree(finalAdminViewResp);
        JsonNode history = finalView.get("lichSuTrangThai");
        assertTrue(history.size() >= 5, "Mọi sự kiện thay đổi phải được lưu lịch sử đầy đủ");

        // Kiểm tra bảo mật tệp: Tệp đính kèm không công khai
        long fileId = finalView.get("tepDinhKem").get(0).get("id").asLong();
        mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId + "/tep-dinh-kem/" + fileId))
                .andExpect(status().isUnauthorized());

        // Sinh viên khác không tải được
        mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId + "/tep-dinh-kem/" + fileId)
                        .header("Authorization", "Bearer " + tokenSv02))
                .andExpect(status().isForbidden());

        // Sinh viên sở hữu tải được tệp với các header bảo mật an toàn
        mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId + "/tep-dinh-kem/" + fileId)
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; sandbox"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    @DisplayName("QLSV-87: Tính toán SLA bằng lịch làm việc không tính cuối tuần và giờ nghỉ trưa")
    void testBusinessCalendarCalculations() {
        LocalDateTime fri16 = LocalDateTime.of(2026, 11, 13, 16, 0); // Thứ Sáu
        LocalDateTime mon09 = LocalDateTime.of(2026, 11, 16, 9, 0);  // Thứ Hai

        long workingMinutes = calendarService.calculateWorkingMinutes(fri16, mon09);
        assertEquals(120L, workingMinutes, "Thời gian làm việc giữa Thứ 6 16h và Thứ 2 9h phải là 120 phút (2 giờ)");

        LocalDateTime fri1130 = LocalDateTime.of(2026, 11, 13, 11, 30);
        LocalDateTime fri1330 = LocalDateTime.of(2026, 11, 13, 13, 30);
        long lunchExcluded = calendarService.calculateWorkingMinutes(fri1130, fri1330);
        assertEquals(60L, lunchExcluded, "Thời gian làm việc qua giờ trưa phải trừ giờ nghỉ trưa 12:00 - 13:00");
    }
}
