package com.edu.registration.security;

import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.UserRepository;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test bộ quy chuẩn và rà soát phân quyền toàn diện cho QLSV-88 (TASK-OP-01).
 * Kiểm thử:
 * 1. Khóa tài khoản: Chặn truy cập API cần xác thực kể cả khi token JWT còn hạn (403).
 * 2. Phạm vi bản ghi Sinh viên: Chỉ truy cập dữ liệu của chính mình, chặn IDOR.
 * 3. Phạm vi bản ghi Giảng viên: Chỉ truy cập lớp và sinh viên được phân công.
 * 4. Quyền Quản trị viên: Chặn sinh viên/giảng viên gọi API quản trị.
 * 5. Chuẩn hóa hành vi HTTP status code 401 / 403 / 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AuthorizationReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LecturerRepository lecturerRepository;

    @Autowired
    private CourseClassRepository courseClassRepository;

    @Autowired
    private CourseRegistrationRepository courseRegistrationRepository;

    @Autowired
    private UserAuthCache userAuthCache;

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
    @DisplayName("QLSV-88: Tài khoản bị khóa không được gọi API xác thực kể cả token còn hạn (HTTP 403)")
    void lockedAccountCannotAccessAuthenticatedApisEvenWithValidToken() throws Exception {
        // 1. Đăng nhập lấy token hợp lệ khi tài khoản còn hoạt động
        String tokenSv01 = login("sv01", "123456");

        // Gọi API bình thường khi tài khoản đang mở
        mockMvc.perform(get("/api/sinh-vien/me/diem")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk());

        // 2. Quản trị viên thực hiện khóa tài khoản sinh viên
        String tokenAdmin = login("admin", "admin123");
        Student sv01 = studentRepository.findByMa("SV001").orElseThrow();
        mockMvc.perform(patch("/api/admin/sinh-vien/" + sv01.getId() + "/trang-thai")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("trangThai", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value(false));

        // 3. Dùng token cũ (còn hạn) để gọi API -> Phải bị từ chối với HTTP 403 Forbidden
        mockMvc.perform(get("/api/sinh-vien/me/diem")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo"));

        mockMvc.perform(get("/api/sinh-vien/me/thoi-khoa-bieu")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo"));

        // 4. Mở khóa lại tài khoản -> Có thể đăng nhập và truy cập lại bình thường
        mockMvc.perform(patch("/api/admin/sinh-vien/" + sv01.getId() + "/trang-thai")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("trangThai", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value(true));

        String tokenSv01New = login("sv01", "123456");
        mockMvc.perform(get("/api/sinh-vien/me/diem")
                        .header("Authorization", "Bearer " + tokenSv01New))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("QLSV-88: Chuẩn hóa 401 khi không gửi token hoặc token sai định dạng/hết hạn")
    void unauthenticatedOrInvalidTokenReturnsStandard401() throws Exception {
        // Không có header Authorization
        mockMvc.perform(get("/api/sinh-vien/me/diem"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập không hợp lệ"));

        // Token giả mạo hoặc sai chữ ký
        mockMvc.perform(get("/api/sinh-vien/me/diem")
                        .header("Authorization", "Bearer invalid.token.payload"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập không hợp lệ"));
    }

    @Test
    @DisplayName("QLSV-88: Giảng viên chỉ truy cập điểm danh sinh viên thuộc lớp phụ trách (IDOR test)")
    void lecturerCannotAccessAttendanceOfUnassignedStudent() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // Tìm một sinh viên đang học trong lớp của gv01
        Lecturer gv01 = lecturerRepository.findByMa("GV01").orElseThrow();
        List<Student> assignedStudents = courseRegistrationRepository.findAssignedStudentsOfLecturer(gv01.getId(), null, RegistrationStatus.DA_DANG_KY);
        assertTrue(!assignedStudents.isEmpty(), "gv01 có sinh viên phụ trách");
        Student assignedStudent = assignedStudents.get(0);

        // 1. Giảng viên truy cập sinh viên thuộc lớp mình -> 200 OK
        mockMvc.perform(get("/api/giang-vien/sinh-vien/" + assignedStudent.getId() + "/chuyen-can")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk());

        // 2. Tạo một sinh viên hoàn toàn không học bất kỳ lớp nào của gv01
        String tokenAdmin = login("admin", "admin123");
        String unique = String.valueOf(System.currentTimeMillis() % 100000);
        MvcResult res = mockMvc.perform(post("/api/admin/sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ma", "SV_UNASSIGNED_" + unique,
                                "hoTen", "Sinh Vien Ngoai Lop",
                                "email", "sv_ngoai_" + unique + "@student.edu.vn",
                                "username", "sv_ngoai_" + unique,
                                "matKhau", "123456"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        long unassignedStudentId = json(res).get("id").asLong();

        // 3. gv01 cố tình truy vấn điểm danh của sinh viên không thuộc lớp mình -> 403 Forbidden!
        mockMvc.perform(get("/api/giang-vien/sinh-vien/" + unassignedStudentId + "/chuyen-can")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền xem thông tin sinh viên này do không phụ trách lớp của sinh viên"));
    }

    @Test
    @DisplayName("QLSV-88: Giảng viên không được thao tác trên lớp của giảng viên khác (IDOR test)")
    void lecturerCannotOperateOnAnotherLecturersClass() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // Tìm một lớp do giảng viên khác phụ trách (ví dụ GV002)
        Lecturer gv01 = lecturerRepository.findByMa("GV01").orElseThrow();
        CourseClass otherClass = courseClassRepository.findAll().stream()
                .filter(c -> c.getGiangVien() != null && !c.getGiangVien().getId().equals(gv01.getId()))
                .findFirst().orElseThrow();

        // 1. Thử gửi thông báo lớp học phần của giảng viên khác -> 403 Forbidden
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + otherClass.getId() + "/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", "Thông báo trái phép",
                                "noiDung", "Nội dung trái phép"
                        ))))
                .andExpect(status().isForbidden());

        // 2. Thử xuất danh sách sinh viên lớp của giảng viên khác -> 403 Forbidden
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + otherClass.getId() + "/sinh-vien/csv")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        // 3. Thử xem danh sách sinh viên và bảng điểm lớp của giảng viên khác -> 403 Forbidden
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + otherClass.getId() + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        // 4. Thử xem thông kê điểm lớp của giảng viên khác -> 403 Forbidden
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + otherClass.getId() + "/thong-ke-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("QLSV-88: Giảng viên không được tự duyệt hoặc công bố bảng điểm (chỉ QUAN_TRI)")
    void lecturerCannotApproveOrPublishGradeSheet() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        Lecturer gv01 = lecturerRepository.findByMa("GV01").orElseThrow();
        CourseClass myClass = courseClassRepository.findByGiangVienIdOrderByMaAsc(gv01.getId()).stream()
                .findFirst().orElseThrow();

        // Giảng viên gọi API phê duyệt điểm -> 403 Forbidden
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + myClass.getId() + "/phe-duyet-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        // Giảng viên gọi API công bố điểm -> 403 Forbidden
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + myClass.getId() + "/cong-bo-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("QLSV-88: Sinh viên không được truy cập API quản trị và API giảng viên")
    void studentCannotAccessAdminOrLecturerEndpoints() throws Exception {
        String tokenSv01 = login("sv01", "123456");

        // Gọi API quản trị người dùng -> 403 Forbidden
        mockMvc.perform(get("/api/admin/sinh-vien")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));

        // Gọi API quản trị thống kê -> 403 Forbidden
        mockMvc.perform(get("/api/admin/thong-ke/tong-quan")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());

        // Gọi API quản trị audit log -> 403 Forbidden
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());

        // Gọi API dành riêng cho giảng viên -> 403 Forbidden
        mockMvc.perform(get("/api/giang-vien/me/dashboard")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("QLSV-88: Sinh viên không được hủy đăng ký của sinh viên khác (IDOR test)")
    void studentCannotCancelAnotherStudentsRegistration() throws Exception {
        String tokenSv01 = login("sv01", "123456");

        // Lấy danh sách đăng ký của một sinh viên khác (SV002)
        Student sv02 = studentRepository.findByMa("SV002").orElseThrow();
        List<CourseRegistration> sv02Regs = courseRegistrationRepository.findAllActiveOfStudent(sv02.getId(), RegistrationStatus.DA_DANG_KY);
        assertTrue(!sv02Regs.isEmpty(), "SV002 có đăng ký học phần");
        Long sv02RegId = sv02Regs.get(0).getId();

        // SV001 cố tình DELETE đăng ký của SV002 -> 404 Not Found (che giấu sự tồn tại của đăng ký đối với SV001)
        mockMvc.perform(delete("/api/dang-ky/" + sv02RegId)
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Đăng ký không tồn tại"));
    }

    @Test
    @DisplayName("QLSV-88: Sinh viên không được xem thông báo lớp khi chưa đăng ký lớp đó (IDOR test)")
    void studentCannotViewClassAnnouncementsWithoutActiveEnrollment() throws Exception {
        String tokenSv01 = login("sv01", "123456");
        Student sv01 = studentRepository.findByMa("SV001").orElseThrow();

        // Tìm một lớp mà SV001 CHƯA đăng ký
        List<Long> enrolledClassIds = courseRegistrationRepository.findAllActiveOfStudent(sv01.getId(), RegistrationStatus.DA_DANG_KY)
                .stream().map(r -> r.getLopHocPhan().getId()).toList();
        CourseClass unEnrolledClass = courseClassRepository.findAll().stream()
                .filter(c -> !enrolledClassIds.contains(c.getId()))
                .findFirst().orElseThrow();

        // SV001 truy cập thông báo lớp chưa học -> 403 Forbidden
        mockMvc.perform(get("/api/sinh-vien/lop-hoc-phan/" + unEnrolledClass.getId() + "/thong-bao")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không đăng ký lớp học phần này"));
    }
}
