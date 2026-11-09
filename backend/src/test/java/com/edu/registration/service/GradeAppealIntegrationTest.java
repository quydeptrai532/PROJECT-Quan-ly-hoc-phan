package com.edu.registration.service;

import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeAppeal;
import com.edu.registration.entity.GradeAppealStatus;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeAppealRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class GradeAppealIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    @Autowired
    private CourseRegistrationRepository dangKyRepository;

    @Autowired
    private GradeRepository diemRepository;

    @Autowired
    private GradeAppealRepository appealRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenSv01;
    private String tokenGv01;

    @BeforeEach
    void setupTokens() throws Exception {
        tokenAdmin = login("admin", "admin123");
        tokenGv01 = login("gv01", "123456");
        tokenSv01 = login("sv01", "123456");
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
    void kiemTraPhanQuyenAdminChoDuyetVaCauHinhPhucKhao() throws Exception {
        // 1. Admin gọi API lấy danh sách lớp chờ duyệt
        MvcResult resChoDuyet = mockMvc.perform(get("/api/admin/lop-hoc-phan/cho-duyet")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode choDuyetList = json(resChoDuyet);
        assertTrue(choDuyetList.isArray());

        // 2. Sinh viên hoặc Giảng viên gọi API admin bị 403 Forbidden
        mockMvc.perform(get("/api/admin/lop-hoc-phan/cho-duyet")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/lop-hoc-phan/cho-duyet")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        // 3. Admin lấy cấu hình phúc khảo
        MvcResult resConfig = mockMvc.perform(get("/api/admin/phuc-khao/cau-hinh")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode configNode = json(resConfig);
        assertTrue(configNode.has("submissionWindowDays"));
        assertTrue(configNode.has("slaNote"));
        assertFalse(configNode.get("officialRegulation").asBoolean());
    }

    @Test
    void luongPhucKhaoDayDu_SinhVienNop_QuanTriTiepNhanVaXuLyChapThuan() throws Exception {
        // Tìm sinh viên sv01
        User userSv01 = userRepository.findByUsername("sv01").orElseThrow();
        Student sv01 = studentRepository.findByUser(userSv01).orElseThrow();

        // Chuẩn bị 1 lớp học phần đã công bố điểm cho sv01
        CourseClass lop = lopHocPhanRepository.findAll().get(0);
        lop.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);
        lop.setCongBoBangDiemLuc(LocalDateTime.now().minusDays(1));
        CourseClass targetLop = lopHocPhanRepository.save(lop);
        CourseRegistration dk = dangKyRepository.findBySinhVienIdAndLopHocPhanId(sv01.getId(), targetLop.getId())
                .orElseGet(() -> {
                    CourseRegistration r = new CourseRegistration();
                    r.setSinhVien(sv01);
                    r.setLopHocPhan(targetLop);
                    r.setTrangThai(RegistrationStatus.DA_DANG_KY);
                    return dangKyRepository.save(r);
                });

        Grade diem = diemRepository.findByDangKyId(dk.getId()).orElseGet(() -> {
            Grade g = new Grade();
            g.setDangKy(dk);
            g.setDiemQuaTrinh(6.5);
            g.setDiemCuoiKy(6.0);
            g.setDiemTongKet(6.2);
            return diemRepository.save(g);
        });

        // 1. Sinh viên truy cập danh mục phúc khảo
        MvcResult resDanhMuc = mockMvc.perform(get("/api/sinh-vien/phuc-khao/danh-muc")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dmNode = json(resDanhMuc);
        assertTrue(dmNode.get("danhSachDiem").isArray());

        // 2. Sinh viên nộp hồ sơ phúc khảo
        Map<String, Object> reqTao = Map.of(
                "dangKyId", dk.getId(),
                "loaiDiem", "DIEM_CUOI_KY",
                "diemMongMuon", 8.0,
                "lyDo", "Chấm sót phần trả lời câu hỏi 3 tự luận, đề nghị phúc khảo bài thi cuối kỳ"
        );
        MvcResult resTao = mockMvc.perform(post("/api/sinh-vien/phuc-khao")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqTao)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode taoNode = json(resTao);
        long phucKhaoId = taoNode.get("id").asLong();
        assertEquals("MOI_TAO", taoNode.get("trangThai").asText());
        assertEquals("SV001", taoNode.get("maSinhVien").asText());

        // 3. Admin tra cứu danh sách phúc khảo
        MvcResult resAdminList = mockMvc.perform(get("/api/admin/phuc-khao")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode adminList = json(resAdminList);
        assertTrue(adminList.isArray());
        boolean found = false;
        for (JsonNode item : adminList) {
            if (item.get("id").asLong() == phucKhaoId) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        // 4. Admin tiếp nhận hồ sơ
        MvcResult resTiepNhan = mockMvc.perform(post("/api/admin/phuc-khao/" + phucKhaoId + "/tiep-nhan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ghiChu", "Chuyển tổ chuyên môn chấm thẩm định"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tiepNhanNode = json(resTiepNhan);
        assertEquals("DANG_XU_LY", tiepNhanNode.get("trangThai").asText());

        // 5. Admin xử lý chấp thuận và điều chỉnh điểm
        Map<String, Object> reqXuLy = Map.of(
                "trangThai", "CHAP_THUAN",
                "ketQuaXuLy", "Tổ chấm kiểm tra xác nhận sót câu 3b, tăng điểm cuối kỳ từ 6.0 lên 8.0",
                "diemQuaTrinhMoi", 6.5,
                "diemCuoiKyMoi", 8.0
        );
        MvcResult resXuLy = mockMvc.perform(post("/api/admin/phuc-khao/" + phucKhaoId + "/xu-ly")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqXuLy)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode xuLyNode = json(resXuLy);
        assertEquals("CHAP_THUAN", xuLyNode.get("trangThai").asText());
        assertEquals(8.0, xuLyNode.get("diemCuoiKyMoi").asDouble());
        assertEquals(7.4, xuLyNode.get("diemTongKetMoi").asDouble());

        // 6. Kiểm tra bản ghi điểm gốc được cập nhật chính xác qua luồng điều chỉnh có audit
        Grade diemSauXuLy = diemRepository.findByDangKyId(dk.getId()).orElseThrow();
        assertEquals(8.0, diemSauXuLy.getDiemCuoiKy());
        assertEquals(7.4, diemSauXuLy.getDiemTongKet());

        // 7. Sinh viên xem lại chi tiết hồ sơ của mình kèm lịch sử
        MvcResult resDetail = mockMvc.perform(get("/api/sinh-vien/phuc-khao/" + phucKhaoId)
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detailNode = json(resDetail);
        assertEquals("CHAP_THUAN", detailNode.get("trangThai").asText());
        assertTrue(detailNode.get("lichSu").isArray());
        assertTrue(detailNode.get("lichSu").size() >= 3); // TAO_MOI, TIEP_NHAN, CHAP_THUAN
    }
}
