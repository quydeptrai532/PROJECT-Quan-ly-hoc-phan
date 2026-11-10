package com.edu.registration.service;

import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Subject;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.SubjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ClassScheduleConflictIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    @Autowired
    private SemesterRepository hocKyRepository;

    @Autowired
    private SubjectRepository monHocRepository;

    @Autowired
    private LecturerRepository giangVienRepository;

    private String tokenAdmin;
    private Semester hocKy;
    private Subject monHoc;
    private Lecturer gv1;
    private Lecturer gv2;
    private final List<Long> createdClassIds = new ArrayList<>();

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

    @BeforeEach
    void setUp() throws Exception {
        tokenAdmin = login("admin", "admin123");
        hocKy = hocKyRepository.findAll().get(0);
        monHoc = monHocRepository.findAll().get(0);
        List<Lecturer> gvs = giangVienRepository.findAll();
        gv1 = gvs.get(0);
        gv2 = gvs.size() > 1 ? gvs.get(1) : gv1;
    }

    @AfterEach
    void tearDown() {
        for (Long id : createdClassIds) {
            lopHocPhanRepository.findById(id).ifPresent(lopHocPhanRepository::delete);
        }
        createdClassIds.clear();
    }

    private CourseClass taoLopMau(String ma, Lecturer gv, String phong, int thu, int tietBatDau, int soTiet) {
        CourseClass lop = new CourseClass();
        lop.setMa(ma);
        lop.setHocKy(hocKy);
        lop.setMonHoc(monHoc);
        lop.setGiangVien(gv);
        lop.setPhong(phong);
        lop.setThu(thu);
        lop.setTietBatDau(tietBatDau);
        lop.setSoTiet(soTiet);
        lop.setSiSoToiDa(40);
        lop.setSiSoHienTai(0);
        CourseClass saved = lopHocPhanRepository.save(lop);
        createdClassIds.add(saved.getId());
        return saved;
    }

    @Test
    void haiLopLienKeKhongDungChungTietKhongBiXemLaTrung() throws Exception {
        // Lớp 1: Thứ 2, tiết 1-3 (kết thúc ở tiết 3)
        taoLopMau("TEST_QLSV78_01", gv1, "P.A101", 2, 1, 3);

        // Lớp dự kiến kiểm tra: Thứ 2, tiết 4-6 (bắt đầu ở tiết 4, liền kề lớp 1 nhưng không dùng chung tiết nào)
        CourseClassDtos.KiemTraXungDotRequest req = new CourseClassDtos.KiemTraXungDotRequest(
                null,
                hocKy.getId(),
                monHoc.getId(),
                gv1.getId(),
                "P.A101",
                2,
                4,
                3
        );

        MvcResult res = mockMvc.perform(post("/api/admin/lop-hoc-phan/kiem-tra-xung-dot")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode result = json(res);
        assertFalse(result.get("coXungDot").asBoolean(), "Hai lớp liền kề (1-3 và 4-6) không được xem là trùng");
        assertEquals(0, result.get("soLuongXungDot").asInt());
        assertTrue(result.get("choPhepLuu").asBoolean());
    }

    @Test
    void phatHienTrungPhongVaGiaoNhauKhoangTietChinhXac() throws Exception {
        // Lớp 1: Thứ 3, tiết 2-4 (phòng P.LAB01, gv1)
        CourseClass lop1 = taoLopMau("TEST_QLSV78_ROOM1", gv1, "P.LAB01", 3, 2, 3);

        // Lớp 2 kiểm tra: Thứ 3, tiết 4-6 (phòng P.LAB01, gv2 khác gv1) -> Giao nhau ở tiết 4!
        CourseClassDtos.KiemTraXungDotRequest req = new CourseClassDtos.KiemTraXungDotRequest(
                null,
                hocKy.getId(),
                monHoc.getId(),
                gv2.getId(),
                "P.LAB01",
                3,
                4,
                3
        );

        MvcResult res = mockMvc.perform(post("/api/admin/lop-hoc-phan/kiem-tra-xung-dot")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode result = json(res);
        assertTrue(result.get("coXungDot").asBoolean());
        assertTrue(result.get("xungDotPhong").asBoolean());
        assertFalse(result.get("xungDotGiangVien").asBoolean());
        assertEquals(1, result.get("soLuongXungDot").asInt());

        JsonNode conflict = result.get("danhSachXungDot").get(0);
        assertEquals("PHONG", conflict.get("loaiXungDot").asText());
        assertEquals(lop1.getId(), conflict.get("lopLienQuanId").asLong());
        assertEquals("TEST_QLSV78_ROOM1", conflict.get("maLopLienQuan").asText());
        assertEquals(4, conflict.get("tietGiaoBatDau").asInt());
        assertEquals(4, conflict.get("tietGiaoKetThuc").asInt());
    }

    @Test
    void phatHienTrungGiangVienVaDongThoiTrungPhong() throws Exception {
        // Lớp 1: Thứ 4, tiết 1-4 (phòng P.B202, gv1)
        CourseClass lop1 = taoLopMau("TEST_QLSV78_BOTH1", gv1, "P.B202", 4, 1, 4);

        // Lớp 2 kiểm tra: Thứ 4, tiết 3-5 (cùng phòng P.B202, cùng gv1) -> Trùng cả phòng và giảng viên tiết 3-4!
        CourseClassDtos.KiemTraXungDotRequest req = new CourseClassDtos.KiemTraXungDotRequest(
                null,
                hocKy.getId(),
                monHoc.getId(),
                gv1.getId(),
                "P.B202",
                4,
                3,
                3
        );

        MvcResult res = mockMvc.perform(post("/api/admin/lop-hoc-phan/kiem-tra-xung-dot")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode result = json(res);
        assertTrue(result.get("coXungDot").asBoolean());
        assertTrue(result.get("xungDotPhong").asBoolean());
        assertTrue(result.get("xungDotGiangVien").asBoolean());
        assertEquals(2, result.get("soLuongXungDot").asInt());
        assertFalse(result.get("choPhepLuu").asBoolean());
        assertFalse(result.get("chinhSachDaChot").asBoolean());
        assertNotNull(result.get("ghiChuChinhSach").asText());
    }

    @Test
    void khiSuaLopKhongTuBaoXungDotVoiChinhNo() throws Exception {
        // Lớp đang có: Thứ 5, tiết 1-3 (phòng P.C301, gv1)
        CourseClass lop = taoLopMau("TEST_QLSV78_SELF", gv1, "P.C301", 5, 1, 3);

        // Gửi kiểm tra với lopHocPhanId của chính nó (ví dụ chỉ đổi sĩ số hoặc môn)
        CourseClassDtos.KiemTraXungDotRequest req = new CourseClassDtos.KiemTraXungDotRequest(
                lop.getId(),
                hocKy.getId(),
                monHoc.getId(),
                gv1.getId(),
                "P.C301",
                5,
                1,
                3
        );

        MvcResult res = mockMvc.perform(post("/api/admin/lop-hoc-phan/kiem-tra-xung-dot")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode result = json(res);
        assertFalse(result.get("coXungDot").asBoolean(), "Không được tự báo xung đột với chính lớp đang sửa");
        assertTrue(result.get("choPhepLuu").asBoolean());
    }

    @Test
    void tuChoiLuuKhiXungDotOBackendVoiChinhSachBlock() throws Exception {
        // Tạo lớp 1 làm mốc
        taoLopMau("TEST_QLSV78_BLOCK1", gv1, "P.D401", 6, 1, 3);

        // Cố gắng tạo lớp mới trùng phòng P.D401 cùng tiết 1-3 qua API POST /api/admin/lop-hoc-phan
        CourseClassDtos.LopHocPhanRequest postReq = new CourseClassDtos.LopHocPhanRequest(
                "TEST_QLSV78_BLOCK2",
                monHoc.getId(),
                hocKy.getId(),
                gv2.getId(),
                "P.D401",
                6,
                2,
                3,
                40
        );

        mockMvc.perform(post("/api/admin/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Phát hiện xung đột lịch học")));
    }
}
