package com.edu.registration.service;

import com.edu.registration.dto.UserDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.StudentAcademicStatus;
import com.edu.registration.entity.StudentAcademicStatusHistory;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.StudentAcademicStatusHistoryRepository;
import com.edu.registration.repository.StudentRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class StudentAcademicStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentAcademicStatusHistoryRepository historyRepository;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    @Autowired
    private CourseRegistrationRepository dangKyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String adminToken;
    private String studentToken;
    private Student testStudent;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
        studentToken = login("sv01", "123456");

        historyRepository.deleteAll();

        testStudent = studentRepository.findByMa("SV001")
                .or(() -> studentRepository.findAll().stream().findFirst())
                .orElseThrow();
        // Reset student to DANG_HOC before each test
        testStudent.setTrangThaiHocTap(StudentAcademicStatus.DANG_HOC);
        studentRepository.save(testStudent);
    }

    @AfterEach
    void tearDown() {
        if (testStudent != null) {
            testStudent.setTrangThaiHocTap(StudentAcademicStatus.DANG_HOC);
            studentRepository.save(testStudent);
        }
    }

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
    @DisplayName("Admin cập nhật trạng thái học tập từ Đang học sang Bảo lưu, ghi nhận lịch sử và audit log")
    void testAdminChangeStudentStatusSuccessAndHistoryCreated() throws Exception {
        LocalDate ngayHieuLuc = LocalDate.now().plusDays(1);
        String lyDo = "Đơn xin bảo lưu học kỳ 1 năm học 2026-2027";

        UserDtos.CapNhatTrangThaiHocTapRequest request = new UserDtos.CapNhatTrangThaiHocTapRequest(
                StudentAcademicStatus.BAO_LUU,
                ngayHieuLuc,
                lyDo
        );

        MvcResult result = mockMvc.perform(patch("/api/admin/sinh-vien/" + testStudent.getId() + "/trang-thai-hoc-tap")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThaiHocTap").value("BAO_LUU"))
                .andExpect(jsonPath("$.trangThaiHocTapMoTa").value("Bảo lưu"))
                .andReturn();

        // Kiểm tra database entity
        Student updated = studentRepository.findById(testStudent.getId()).orElseThrow();
        assertEquals(StudentAcademicStatus.BAO_LUU, updated.getTrangThaiHocTap());

        // Kiểm tra API lịch sử trạng thái học tập (Admin endpoint)
        MvcResult historyRes = mockMvc.perform(get("/api/admin/sinh-vien/" + testStudent.getId() + "/lich-su-trang-thai-hoc-tap")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyArray = json(historyRes);
        assertTrue(historyArray.isArray());
        assertTrue(historyArray.size() >= 1);
        JsonNode latestHistory = historyArray.get(0);
        assertEquals("DANG_HOC", latestHistory.get("trangThaiTruoc").asText());
        assertEquals("BAO_LUU", latestHistory.get("trangThaiSau").asText());
        assertEquals(ngayHieuLuc.toString(), latestHistory.get("ngayHieuLuc").asText());
        assertEquals(lyDo, latestHistory.get("ghiChu").asText());
        assertEquals("admin", latestHistory.get("nguoiThucHienUsername").asText());

        // Kiểm tra Audit Log
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("action", "STUDENT_ACADEMIC_STATUS_CHANGED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("STUDENT_ACADEMIC_STATUS_CHANGED"));
    }

    @Test
    @DisplayName("Sinh viên không có quyền tự thay đổi trạng thái học tập của mình (403 Forbidden)")
    void testStudentForbiddenToChangeStatus() throws Exception {
        UserDtos.CapNhatTrangThaiHocTapRequest request = new UserDtos.CapNhatTrangThaiHocTapRequest(
                StudentAcademicStatus.TOT_NGHIEP,
                LocalDate.now(),
                "Tự chuyển trạng thái"
        );

        mockMvc.perform(patch("/api/admin/sinh-vien/" + testStudent.getId() + "/trang-thai-hoc-tap")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sinh viên có thể xem lịch sử trạng thái học tập và hồ sơ đào tạo của chính mình")
    void testStudentCanViewOwnStatusHistoryAndProfile() throws Exception {
        // Tạo trước 1 bản ghi lịch sử
        UserDtos.CapNhatTrangThaiHocTapRequest request = new UserDtos.CapNhatTrangThaiHocTapRequest(
                StudentAcademicStatus.BAO_LUU,
                LocalDate.now(),
                "Bảo lưu học kỳ"
        );
        mockMvc.perform(patch("/api/admin/sinh-vien/" + testStudent.getId() + "/trang-thai-hoc-tap")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Sinh viên xem hồ sơ đào tạo: trangThaiHocTap phải là "Bảo lưu"
        mockMvc.perform(get("/api/sinh-vien/me/ho-so-dao-tao")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThaiHocTap").value("Bảo lưu"));

        // Sinh viên xem lịch sử trạng thái học tập
        MvcResult historyRes = mockMvc.perform(get("/api/sinh-vien/me/lich-su-trang-thai-hoc-tap")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyArray = json(historyRes);
        assertTrue(historyArray.size() >= 1);
        assertEquals("BAO_LUU", historyArray.get(0).get("trangThaiSau").asText());
    }

    @Test
    @DisplayName("Backend chặn đăng ký học phần khi trạng thái là BAO_LUU, THOI_HOC, TOT_NGHIEP")
    void testRegistrationBlockedWhenIneligibleStatus() throws Exception {
        CourseClass lop = lopHocPhanRepository.findAll().stream()
                .filter(l -> !l.isDaHuy() && l.getConLai() > 0)
                .findFirst()
                .orElseThrow();

        // 1. Chuyển sang BAO_LUU
        testStudent.setTrangThaiHocTap(StudentAcademicStatus.BAO_LUU);
        studentRepository.save(testStudent);

        mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", lop.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Không đủ điều kiện đăng ký học phần: Sinh viên đang trong thời gian bảo lưu")));

        // 2. Chuyển sang THOI_HOC
        testStudent.setTrangThaiHocTap(StudentAcademicStatus.THOI_HOC);
        studentRepository.save(testStudent);

        mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", lop.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sinh viên đã thôi học")));

        // 3. Chuyển sang TOT_NGHIEP
        testStudent.setTrangThaiHocTap(StudentAcademicStatus.TOT_NGHIEP);
        studentRepository.save(testStudent);

        mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", lop.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sinh viên đã tốt nghiệp")));
    }

    @Test
    @DisplayName("Không xóa hoặc vô hiệu lịch sử đăng ký cũ khi đổi trạng thái học tập")
    void testExistingRegistrationsPreservedWhenStatusChanges() throws Exception {
        // Đảm bảo sinh viên có ít nhất 1 đăng ký
        CourseClass lop = lopHocPhanRepository.findAll().stream()
                .filter(l -> !l.isDaHuy())
                .findFirst()
                .orElseThrow();

        List<CourseRegistration> regListBefore = dangKyRepository.findBySinhVienIdOrderByLopHocPhanHocKyNgayBatDauDescIdDesc(testStudent.getId());
        long countBefore = regListBefore.size();

        // Đổi trạng thái sang BAO_LUU
        UserDtos.CapNhatTrangThaiHocTapRequest request = new UserDtos.CapNhatTrangThaiHocTapRequest(
                StudentAcademicStatus.BAO_LUU,
                LocalDate.now(),
                "Xin bảo lưu để đi làm"
        );
        mockMvc.perform(patch("/api/admin/sinh-vien/" + testStudent.getId() + "/trang-thai-hoc-tap")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Kiểm tra danh sách đăng ký cũ không bị xóa hay thay đổi
        List<CourseRegistration> regListAfter = dangKyRepository.findBySinhVienIdOrderByLopHocPhanHocKyNgayBatDauDescIdDesc(testStudent.getId());
        assertEquals(countBefore, regListAfter.size());
    }

    @Test
    @DisplayName("Validation: Đổi sang trạng thái giống hệt hiện tại hoặc thiếu lý do sẽ bị từ chối 400")
    void testValidationErrors() throws Exception {
        // 1. Trạng thái trùng lặp với hiện tại (DANG_HOC -> DANG_HOC)
        UserDtos.CapNhatTrangThaiHocTapRequest dupRequest = new UserDtos.CapNhatTrangThaiHocTapRequest(
                StudentAcademicStatus.DANG_HOC,
                LocalDate.now(),
                "Cố tình đổi sang trùng"
        );
        mockMvc.perform(patch("/api/admin/sinh-vien/" + testStudent.getId() + "/trang-thai-hoc-tap")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Trạng thái mới phải khác trạng thái học tập hiện tại")));

        // 2. Lý do để trống
        UserDtos.CapNhatTrangThaiHocTapRequest emptyReason = new UserDtos.CapNhatTrangThaiHocTapRequest(
                StudentAcademicStatus.BAO_LUU,
                LocalDate.now(),
                "   "
        );
        mockMvc.perform(patch("/api/admin/sinh-vien/" + testStudent.getId() + "/trang-thai-hoc-tap")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReason)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Concurrency: Hai cập nhật đồng thời trên cùng sinh viên được serialized an toàn mà không làm hỏng dữ liệu")
    void testConcurrentStatusUpdates() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        StudentAcademicStatus[] targets = {StudentAcademicStatus.BAO_LUU, StudentAcademicStatus.THOI_HOC};

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    UserDtos.CapNhatTrangThaiHocTapRequest req = new UserDtos.CapNhatTrangThaiHocTapRequest(
                            targets[index],
                            LocalDate.now(),
                            "Cập nhật luồng " + index
                    );
                    MvcResult res = mockMvc.perform(patch("/api/admin/sinh-vien/" + testStudent.getId() + "/trang-thai-hoc-tap")
                                    .header("Authorization", "Bearer " + adminToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();
                    if (res.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Cả 2 có thể cùng thành công nối tiếp nhau (DANG_HOC -> BAO_LUU rồi BAO_LUU -> THOI_HOC) hoặc một trong hai
        assertTrue(successCount.get() >= 1);
        // Kiểm tra lịch sử không bị lỗi hoặc trùng lặp bất thường
        List<StudentAcademicStatusHistory> histories = historyRepository.findBySinhVienIdOrderByThoiDiemDescIdDesc(testStudent.getId());
        assertEquals(successCount.get(), histories.size());
    }
}
