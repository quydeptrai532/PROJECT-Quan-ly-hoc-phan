package com.edu.registration.service;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Subject;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class CatalogArchivalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubjectRepository monHocRepository;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    @Autowired
    private FacultyRepository khoaRepository;

    @Autowired
    private MajorRepository nganhRepository;

    @Autowired
    private AdministrativeClassRepository lopHanhChinhRepository;

    @Autowired
    private SemesterRepository hocKyRepository;

    @Autowired
    private LecturerRepository giangVienRepository;

    private String tokenAdmin;
    private Faculty khoaDefault;
    private Semester hocKyDefault;
    private Lecturer giangVienDefault;

    private final List<Long> createdClassIds = new ArrayList<>();
    private final List<Long> createdSubjectIds = new ArrayList<>();

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.get("token").asText();
    }

    private JsonNode json(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        tokenAdmin = login("admin", "admin123");
        khoaDefault = khoaRepository.findAll().stream().findFirst().orElseThrow();
        hocKyDefault = hocKyRepository.findAll().stream().findFirst().orElseThrow();
        giangVienDefault = giangVienRepository.findAll().stream().findFirst().orElseThrow();
    }

    @AfterEach
    void tearDown() {
        for (Long classId : createdClassIds) {
            lopHocPhanRepository.findById(classId).ifPresent(lopHocPhanRepository::delete);
        }
        for (Long subjectId : createdSubjectIds) {
            monHocRepository.findById(subjectId).ifPresent(monHocRepository::delete);
        }
    }

    @Test
    void monHocCoThamChieu_KhiXoa_ChuyenSangLuuTruVaBaoToanLichSu() throws Exception {
        // 1. Tạo môn học mới
        CatalogDtos.MonHocRequest reqMon = new CatalogDtos.MonHocRequest(
                "MH_ARCH_01", "Môn Học Khảo Sát Lưu Trữ", 3, khoaDefault.getId()
        );
        MvcResult resMon = mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMon)))
                .andExpect(status().isOk())
                .andReturn();
        Long monId = json(resMon).get("id").asLong();
        createdSubjectIds.add(monId);

        // 2. Mở lớp học phần tham chiếu môn học này
        CourseClassDtos.LopHocPhanRequest reqLop = new CourseClassDtos.LopHocPhanRequest(
                "LHP_ARCH_01", monId, hocKyDefault.getId(), giangVienDefault.getId(),
                "A101", 2, 1, 3, 40
        );
        MvcResult resLop = mockMvc.perform(post("/api/admin/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLop)))
                .andExpect(status().isOk())
                .andReturn();
        Long lopId = json(resLop).get("lopHocPhan").get("id").asLong();
        createdClassIds.add(lopId);

        // 3. Thực hiện xóa môn học (chính sách Soft Delete / Archival)
        mockMvc.perform(delete("/api/admin/mon-hoc/" + monId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        // 4. Kiểm tra bản ghi môn học vẫn tồn tại trong database nhưng chuyển sang dangHieuLuc = false
        Subject afterSubject = monHocRepository.findById(monId).orElseThrow();
        assertFalse(afterSubject.isDangHieuLuc(), "Môn học phải chuyển sang ngừng hiệu lực (archived)");
        assertNotNull(afterSubject.getNgayLuuTru(), "Ngày lưu trữ phải được ghi nhận");

        // 5. Kiểm tra lớp học phần lịch sử vẫn hiển thị đúng mã và tên môn học
        CourseClass lopSauXoa = lopHocPhanRepository.findWithDetailsById(lopId).orElseThrow();
        assertNotNull(lopSauXoa.getMonHoc());
        assertEquals("MH_ARCH_01", lopSauXoa.getMonHoc().getMa());
        assertEquals("Môn Học Khảo Sát Lưu Trữ", lopSauXoa.getMonHoc().getTen());

        // 6. Kiểm tra danh sách thao tác thông thường mặc định ẩn môn học đã lưu trữ
        mockMvc.perform(get("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ma == 'MH_ARCH_01')]").doesNotExist());

        // 7. Khi lọc danh mục lưu trữ (dangHieuLuc=false), môn học hiển thị đầy đủ
        mockMvc.perform(get("/api/admin/mon-hoc?dangHieuLuc=false")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ma == 'MH_ARCH_01')]").exists())
                .andExpect(jsonPath("$[?(@.ma == 'MH_ARCH_01')].dangHieuLuc").value(false));

        // 8. Chặn mở lớp học phần mới cho môn học đã lưu trữ
        CourseClassDtos.LopHocPhanRequest reqLopMoi = new CourseClassDtos.LopHocPhanRequest(
                "LHP_ARCH_02", monId, hocKyDefault.getId(), giangVienDefault.getId(),
                "A102", 3, 1, 3, 40
        );
        mockMvc.perform(post("/api/admin/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLopMoi)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("lưu trữ/ngừng hoạt động")));
    }

    @Test
    void taoMonHocTrungMaDaLuuTru_XuLyUniqueConstraintRanhMach() throws Exception {
        // 1. Tạo môn học và lưu trữ
        CatalogDtos.MonHocRequest reqMon = new CatalogDtos.MonHocRequest(
                "MH_UNIQUE_01", "Môn Học Unique Test", 3, khoaDefault.getId()
        );
        MvcResult resMon = mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMon)))
                .andExpect(status().isOk())
                .andReturn();
        Long monId = json(resMon).get("id").asLong();
        createdSubjectIds.add(monId);

        // Lưu trữ môn học
        mockMvc.perform(put("/api/admin/mon-hoc/" + monId + "/luu-tru")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CatalogDtos.LuuTruRequest("Kiểm thử"))))
                .andExpect(status().isOk());

        // 2. Cố tình tạo mới lại mã "MH_UNIQUE_01" mà không khôi phục -> Phải bị từ chối 409 Conflict kèm hướng dẫn
        CatalogDtos.MonHocRequest reqTrung = new CatalogDtos.MonHocRequest(
                "MH_UNIQUE_01", "Tên Môn Mới", 4, khoaDefault.getId()
        );
        mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqTrung)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("đã tồn tại trong danh mục lưu trữ")));

        // 3. Tạo lại với tùy chọn khoiPhucNeuDaLuuTru = true -> Tự động khôi phục bản ghi cũ, cập nhật thông tin mới
        CatalogDtos.MonHocRequest reqKhoiPhuc = new CatalogDtos.MonHocRequest(
                "MH_UNIQUE_01", "Tên Môn Đã Khôi Phục", 4, khoaDefault.getId(), true, null, true
        );
        MvcResult resRestored = mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqKhoiPhuc)))
                .andExpect(status().isOk())
                .andReturn();

        Long restoredId = json(resRestored).get("id").asLong();
        assertEquals(monId, restoredId, "Bản ghi phải được tái sử dụng để bảo đảm toàn vẹn khóa ngoại lịch sử");

        Subject restoredSubject = monHocRepository.findById(monId).orElseThrow();
        assertTrue(restoredSubject.isDangHieuLuc());
        assertEquals("Tên Môn Đã Khôi Phục", restoredSubject.getTen());
        assertEquals(4, restoredSubject.getSoTinChi());

        // 4. Kiểm tra số lượng bản ghi mã "MH_UNIQUE_01" trong database chính xác bằng 1 (không tạo trùng lặp)
        long count = monHocRepository.findAll().stream()
                .filter(m -> m.getMa().equalsIgnoreCase("MH_UNIQUE_01"))
                .count();
        assertEquals(1, count, "Không được phép tạo trùng lặp bản ghi môn học");
    }

    @Test
    void monHocChuaCoThamChieu_DuocXoaVatLy() throws Exception {
        // Tạo môn học nháp không liên kết bất kỳ lớp hay CTĐT nào
        CatalogDtos.MonHocRequest reqMon = new CatalogDtos.MonHocRequest(
                "MH_CLEAN_99", "Môn Học Nháp Chưa Dùng", 2, khoaDefault.getId()
        );
        MvcResult resMon = mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMon)))
                .andExpect(status().isOk())
                .andReturn();
        Long monId = json(resMon).get("id").asLong();

        // Xóa môn học
        mockMvc.perform(delete("/api/admin/mon-hoc/" + monId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        // Phải được xóa vật lý hoàn toàn khỏi CSDL
        assertTrue(monHocRepository.findById(monId).isEmpty(), "Môn học chưa từng dùng được phép xóa vật lý");
    }

    @Test
    void monHocCoThamChieu_XoaPermanent_BiTuChoiKemHuongDan() throws Exception {
        CatalogDtos.MonHocRequest reqMon = new CatalogDtos.MonHocRequest(
                "MH_PERM_01", "Môn Học Permanent Test", 3, khoaDefault.getId()
        );
        MvcResult resMon = mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMon)))
                .andExpect(status().isOk())
                .andReturn();
        Long monId = json(resMon).get("id").asLong();
        createdSubjectIds.add(monId);

        CourseClassDtos.LopHocPhanRequest reqLop = new CourseClassDtos.LopHocPhanRequest(
                "LHP_PERM_01", monId, hocKyDefault.getId(), giangVienDefault.getId(),
                "A103", 4, 1, 3, 40
        );
        MvcResult resLop = mockMvc.perform(post("/api/admin/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLop)))
                .andExpect(status().isOk())
                .andReturn();
        Long lopId = json(resLop).get("lopHocPhan").get("id").asLong();
        createdClassIds.add(lopId);

        // Gọi xóa cứng (permanent=true)
        mockMvc.perform(delete("/api/admin/mon-hoc/" + monId + "?permanent=true")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Không thể xóa vật lý môn học")));
    }

    @Test
    void khoaNganhLopHanhChinhHocKy_ChanXoaVatLyKhiCoThamChieu() throws Exception {
        // 1. Khoa đang có ngành/môn học tham chiếu -> Cấm xóa vật lý
        mockMvc.perform(delete("/api/admin/khoa/" + khoaDefault.getId() + "?permanent=true")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Không thể xóa vật lý khoa")));

        // 2. Ngành đang có lớp/sinh viên tham chiếu -> Cấm xóa vật lý
        Major nganhCoThamChieu = nganhRepository.findAll().stream().findFirst().orElseThrow();
        mockMvc.perform(delete("/api/admin/nganh/" + nganhCoThamChieu.getId() + "?permanent=true")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Không thể xóa vật lý ngành")));

        // 3. Học kỳ có lớp học phần -> Cấm xóa vật lý
        mockMvc.perform(delete("/api/admin/hoc-ky/" + hocKyDefault.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Không thể xóa học kỳ")));
    }
}
