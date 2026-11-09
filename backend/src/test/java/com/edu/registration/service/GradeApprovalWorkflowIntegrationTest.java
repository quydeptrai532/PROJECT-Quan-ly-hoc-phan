package com.edu.registration.service;

import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.NotificationRepository;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class GradeApprovalWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();

        return json(res).get("token").asText();
    }

    private JsonNode json(MvcResult res) throws Exception {
        return objectMapper.readTree(
                res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void luongEndToEndGuiDuyetTraVeDuyetCongBoKhoaMoKhoaChongGhiDeDongThoiVaAnDiemTruocCongBo()
            throws Exception {

        String tokenGv01 = login("gv01", "123456");
        String tokenGv02 = login("gv02", "123456");
        String tokenAdmin = login("admin", "admin123");
        String tokenSv01 = login("sv01", "123456");

        // Tìm một lớp học phần của gv01 có trạng thái NHAP và có sinh viên đăng ký
        JsonNode lopCuaGv01 = json(mockMvc.perform(
                        get("/api/giang-vien/me/lop-hoc-phan")
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

            JsonNode svList = json(mockMvc.perform(
                            get("/api/giang-vien/lop-hoc-phan/" + id + "/sinh-vien")
                                    .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk())
                    .andReturn());

            if (svList.isArray() && !svList.isEmpty()) {
                targetLopId = id;
                danhSachSv = svList;
                break;
            }
        }

        assertNotNull(targetLopId,
                "Cần tìm thấy ít nhất 1 lớp nháp của gv01 có sinh viên đăng ký");

        final long lopId = targetLopId;

        // Lấy phiên bản công bố hiện tại để kiểm tra phiên bản sau khi công bố.
        // Dữ liệu H2 có thể vẫn giữ phiên bản từ lần chạy test trước.
        CourseClass lopBanDau = lopHocPhanRepository.findById(lopId).orElseThrow();

        int phienBanCongBoBanDau = lopBanDau.getPhienBanCongBo();
        int phienBanCongBoMongDoi = Math.max(1, phienBanCongBoBanDau + 1);

        try {
            // 1. Nhập đầy đủ điểm cho toàn bộ sinh viên đang học trong lớp ở trạng thái NHAP
            List<Map<String, Object>> bulkItems = new ArrayList<>();

            for (JsonNode sv : danhSachSv) {
                bulkItems.add(Map.of(
                        "dangKyId", sv.get("dangKyId").asLong(),
                        "diemQuaTrinh", 8.0,
                        "diemCuoiKy", 8.5
                ));
            }

            mockMvc.perform(put("/api/giang-vien/lop-hoc-phan/" + lopId + "/diem")
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("danhSach", bulkItems))))
                    .andExpect(status().isOk());

            long firstDangKyId = danhSachSv.get(0).get("dangKyId").asLong();
            String firstMaSv = danhSachSv.get(0).get("maSinhVien").asText();
            String firstHoTen = danhSachSv.get(0).get("hoTen").asText();

            // 2. Chặn chuyển trạng thái trái phép từ NHAP:
            // - Admin không được gửi duyệt thay giảng viên -> 403
            mockMvc.perform(post(
                            "/api/giang-vien/lop-hoc-phan/" + lopId + "/gui-duyet-diem")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isForbidden());

            // - Giảng viên khác (gv02) không được gửi duyệt lớp của gv01 -> 403
            mockMvc.perform(post(
                            "/api/giang-vien/lop-hoc-phan/" + lopId + "/gui-duyet-diem")
                            .header("Authorization", "Bearer " + tokenGv02))
                    .andExpect(status().isForbidden());

            // - Admin không được duyệt trực tiếp từ NHAP -> 409 Conflict
            mockMvc.perform(post(
                            "/api/admin/lop-hoc-phan/" + lopId + "/phe-duyet-diem")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isConflict());

            // 3. Giảng viên phụ trách (gv01) gửi duyệt: NHAP -> CHO_DUYET
            mockMvc.perform(post(
                            "/api/giang-vien/lop-hoc-phan/" + lopId + "/gui-duyet-diem")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("CHO_DUYET"))
                    .andExpect(jsonPath("$.choPhepSuaDiem").value(false));

            // Khi đang CHO_DUYET, giảng viên không được sửa điểm hay import Excel -> 409 Conflict
            mockMvc.perform(put("/api/diem/" + firstDangKyId)
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("diemQuaTrinh", 9.0, "diemCuoiKy", 9.0))))
                    .andExpect(status().isConflict());

            // Giảng viên không được tự phê duyệt hoặc tự trả về bảng điểm -> 403 Forbidden
            mockMvc.perform(post(
                            "/api/giang-vien/lop-hoc-phan/" + lopId + "/phe-duyet-diem")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(
                            "/api/giang-vien/lop-hoc-phan/" + lopId + "/tra-ve-diem")
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("lyDo", "Tự trả về"))))
                    .andExpect(status().isForbidden());

            // 4. Kiểm tra Pessimistic Locking chống 2 luồng duyệt đồng thời ghi đè nhau:
            // Hai luồng cùng gọi phê duyệt (CHO_DUYET -> DA_DUYET) đồng thời:
            // chỉ đúng 1 luồng 200 OK và 1 luồng 409 Conflict!
            CountDownLatch readyLatch = new CountDownLatch(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            Callable<Integer> approveTask = () -> {
                readyLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);

                return mockMvc.perform(post(
                                "/api/admin/lop-hoc-phan/" + lopId + "/phe-duyet-diem")
                                .header("Authorization", "Bearer " + tokenAdmin))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };

            Future<Integer> f1 = executor.submit(approveTask);
            Future<Integer> f2 = executor.submit(approveTask);

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
            startLatch.countDown();

            int status1 = f1.get(10, TimeUnit.SECONDS);
            int status2 = f2.get(10, TimeUnit.SECONDS);

            executor.shutdownNow();

            assertTrue(
                    (status1 == 200 && status2 == 409)
                            || (status1 == 409 && status2 == 200),
                    "Hai luồng duyệt đồng thời phải có đúng 1 luồng 200 OK "
                            + "và 1 luồng 409 Conflict, thực tế: "
                            + status1 + ", " + status2
            );

            // Từ DA_DUYET, Phòng Đào tạo trả về chỉnh sửa kèm lý do bắt buộc -> TRA_VE
            mockMvc.perform(post(
                            "/api/admin/lop-hoc-phan/" + lopId + "/tra-ve-diem")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("lyDo", "Cần kiểm tra lại cột điểm quá trình"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("TRA_VE"))
                    .andExpect(jsonPath("$.choPhepSuaDiem").value(true));

            CourseClass afterReturn = lopHocPhanRepository.findById(lopId).orElseThrow();

            assertEquals(GradeSheetStatus.TRA_VE, afterReturn.getTrangThaiBangDiem());
            assertEquals(
                    "Cần kiểm tra lại cột điểm quá trình",
                    afterReturn.getLyDoTraVeBangDiem()
            );

            // 5. Khi ở trạng thái TRA_VE, giảng viên phụ trách được phép sửa điểm và gửi duyệt lại
            mockMvc.perform(put("/api/diem/" + firstDangKyId)
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("diemQuaTrinh", 8.5, "diemCuoiKy", 9.0))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.diemTongKet").value(8.8));

            mockMvc.perform(post(
                            "/api/giang-vien/lop-hoc-phan/" + lopId + "/gui-duyet-diem")
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("CHO_DUYET"));

            // 6. Phòng Đào tạo (admin) phê duyệt: CHO_DUYET -> DA_DUYET
            long publishedNotifBefore = notificationRepository.findAll().stream()
                    .filter(n -> "GRADES_PUBLISHED".equals(n.getEventType()))
                    .count();

            mockMvc.perform(post(
                            "/api/admin/lop-hoc-phan/" + lopId + "/phe-duyet-diem")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("DA_DUYET"))
                    .andExpect(jsonPath("$.daCongBo").value(false));

            // Trước khi công bố: chưa phát thông báo GRADES_PUBLISHED
            // và sinh viên chưa nhìn thấy điểm lớp này
            long publishedNotifAfterApprove = notificationRepository.findAll().stream()
                    .filter(n -> "GRADES_PUBLISHED".equals(n.getEventType()))
                    .count();

            assertEquals(
                    publishedNotifBefore,
                    publishedNotifAfterApprove,
                    "Không được gửi thông báo công bố điểm khi mới ở trạng thái DA_DUYET"
            );

            JsonNode ketQuaTruocCongBo = json(mockMvc.perform(
                            get("/api/sinh-vien/me/diem")
                                    .header("Authorization", "Bearer " + tokenSv01))
                    .andExpect(status().isOk())
                    .andReturn());

            for (JsonNode row : ketQuaTruocCongBo.path("items")) {
                if (row.path("id").asLong() == firstDangKyId) {
                    assertTrue(
                            row.path("diemQuaTrinh").isNull(),
                            "Sinh viên không được xem điểm quá trình trước khi bảng điểm công bố"
                    );

                    assertTrue(
                            row.path("diemCuoiKy").isNull(),
                            "Sinh viên không được xem điểm cuối kỳ trước khi bảng điểm công bố"
                    );

                    assertTrue(
                            row.path("diemTongKet").isNull(),
                            "Sinh viên không được xem điểm tổng kết trước khi bảng điểm công bố"
                    );
                }
            }

            // 7. Phòng Đào tạo công bố bảng điểm: DA_DUYET -> DA_CONG_BO
            mockMvc.perform(post(
                            "/api/admin/lop-hoc-phan/" + lopId + "/cong-bo-diem")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("DA_CONG_BO"))
                    .andExpect(jsonPath("$.daCongBo").value(true))
                    .andExpect(jsonPath("$.phienBanCongBo").value(phienBanCongBoMongDoi));

            // Sau khi commit công bố: sự kiện thông báo GRADES_PUBLISHED
            // được phát cho các sinh viên trong lớp
            long publishedNotifAfterPublish = notificationRepository.findAll().stream()
                    .filter(n -> "GRADES_PUBLISHED".equals(n.getEventType()))
                    .count();

            assertTrue(
                    publishedNotifAfterPublish > publishedNotifBefore,
                    "Phải phát thông báo GRADES_PUBLISHED sau khi giao dịch công bố bảng điểm commit"
            );

            // 8. Phòng Đào tạo khóa bảng điểm: DA_CONG_BO -> DA_KHOA
            mockMvc.perform(post(
                            "/api/admin/lop-hoc-phan/" + lopId + "/khoa-diem")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("lyDo", "Khóa sổ điểm cuối kỳ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("DA_KHOA"))
                    .andExpect(jsonPath("$.daCongBo").value(true))
                    .andExpect(jsonPath("$.daKhoa").value(true))
                    .andExpect(jsonPath("$.choPhepSuaDiem").value(false));

            // 9. Sau khi khóa: mọi API nhập điểm đơn lẻ, lưu hàng loạt
            // và import Excel đều bị từ chối ở backend (409 Conflict)
            mockMvc.perform(put("/api/diem/" + firstDangKyId)
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("diemQuaTrinh", 10.0, "diemCuoiKy", 10.0))))
                    .andExpect(status().isConflict());

            mockMvc.perform(put("/api/giang-vien/lop-hoc-phan/" + lopId + "/diem")
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("danhSach", bulkItems))))
                    .andExpect(status().isConflict());

            byte[] xlsxBytes = XlsxWorkbookSupport.createWorkbookBytes(Map.of(
                    "BangDiem", List.of(
                            List.of("maSinhVien", "hoTen", "diemQuaTrinh", "diemCuoiKy"),
                            List.of(firstMaSv, firstHoTen, 9.0, 9.0)
                    )
            ));

            MockMultipartFile xlsxFile = new MockMultipartFile(
                    "file",
                    "locked.xlsx",
                    XlsxWorkbookSupport.XLSX_MEDIA_TYPE,
                    xlsxBytes
            );

            mockMvc.perform(multipart(
                            "/api/giang-vien/lop-hoc-phan/" + lopId + "/diem/import-xlsx/preview")
                            .file(xlsxFile)
                            .header("Authorization", "Bearer " + tokenGv01))
                    .andExpect(status().isConflict());

            // 10. Mở khóa bắt buộc có quyền QUAN_TRI và lý do hợp lệ: DA_KHOA -> TRA_VE
            mockMvc.perform(post(
                            "/api/admin/lop-hoc-phan/" + lopId + "/mo-khoa-diem")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("lyDo", "   "))))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post(
                            "/api/admin/lop-hoc-phan/" + lopId + "/mo-khoa-diem")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of(
                                            "lyDo",
                                            "Mở khóa để cập nhật điểm phúc khảo theo biên bản hội đồng"
                                    ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThaiBangDiem").value("TRA_VE"))
                    .andExpect(jsonPath("$.choPhepSuaDiem").value(true))
                    .andExpect(jsonPath("$.daKhoa").value(false));

            // 11. Kiểm tra API lịch sử chuyển trạng thái và AuditLog
            JsonNode historyList = json(mockMvc.perform(
                            get("/api/admin/lop-hoc-phan/" + lopId + "/lich-su-bang-diem")
                                    .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andReturn());

            assertTrue(
                    historyList.isArray() && historyList.size() >= 7,
                    "Lịch sử chuyển trạng thái bảng điểm phải ghi nhận đầy đủ các bước"
            );

            assertEquals("MO_KHOA", historyList.get(0).get("hanhDong").asText());
            assertEquals("DA_KHOA", historyList.get(0).get("trangThaiTruoc").asText());
            assertEquals("TRA_VE", historyList.get(0).get("trangThaiSau").asText());
            assertTrue(historyList.get(0).get("lyDo").asText().contains("phúc khảo"));

            long gradeSheetAuditCount = auditLogRepository.findAll().stream()
                    .filter(a -> "GRADE_SHEET".equals(a.getEntityType())
                            && String.valueOf(lopId).equals(a.getEntityId()))
                    .count();

            assertTrue(
                    gradeSheetAuditCount >= 7,
                    "Audit log phải ghi nhận đầy đủ các thao tác trên GRADE_SHEET"
            );

        } finally {
            // Khôi phục trạng thái lớp về NHAP để không ảnh hưởng tới các test tích hợp khác
            CourseClass lop = lopHocPhanRepository.findById(lopId).orElseThrow();
            lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
            lopHocPhanRepository.save(lop);
        }
    }
}