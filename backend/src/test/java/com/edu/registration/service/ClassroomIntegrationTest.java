package com.edu.registration.service;

import com.edu.registration.dto.ClassroomDtos;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.entity.Classroom;
import com.edu.registration.entity.ClassroomStatus;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Subject;
import com.edu.registration.repository.ClassroomRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class ClassroomIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    @Autowired
    private SemesterRepository hocKyRepository;

    @Autowired
    private SubjectRepository monHocRepository;

    @Autowired
    private LecturerRepository giangVienRepository;

    private String tokenAdmin;
    private String tokenStudent;
    private Semester hocKy;
    private Subject monHoc;
    private Lecturer giangVien;

    private final List<Long> createdRoomIds = new ArrayList<>();
    private final List<Long> createdClassIds = new ArrayList<>();

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
        tokenStudent = login("sv01", "123456");

        hocKy = hocKyRepository.findAll().stream().findFirst().orElseThrow();
        monHoc = monHocRepository.findAll().stream().findFirst().orElseThrow();
        giangVien = giangVienRepository.findAll().stream().findFirst().orElseThrow();
    }

    @AfterEach
    void tearDown() {
        for (Long classId : createdClassIds) {
            lopHocPhanRepository.findById(classId).ifPresent(lopHocPhanRepository::delete);
        }
        for (Long roomId : createdRoomIds) {
            classroomRepository.findById(roomId).ifPresent(classroomRepository::delete);
        }
    }

    @Test
    void crudPhongHocThanhCong_ChuanHoaMaVaSucChua() throws Exception {
        // 1. Tạo phòng mới với mã viết thường và có khoảng trắng: "  test-a201  "
        ClassroomDtos.PhongHocRequest reqCreate = new ClassroomDtos.PhongHocRequest(
                "  test-a201  ",
                "Phòng học lý thuyết A201",
                "Nhà A",
                2,
                "Tầng 2, Nhà A",
                75,
                "LY_THUYET",
                "Máy chiếu, Điều hòa, Micro & Loa",
                ClassroomStatus.HOAT_DONG,
                "Phòng tạo qua test"
        );

        MvcResult resCreate = mockMvc.perform(post("/api/admin/phong-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqCreate)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonCreate = json(resCreate);
        Long roomId = jsonCreate.get("id").asLong();
        createdRoomIds.add(roomId);

        // Kiểm tra chuẩn hóa mã sang uppercase và trim
        assertEquals("TEST-A201", jsonCreate.get("ma").asText());
        assertEquals("Phòng học lý thuyết A201", jsonCreate.get("ten").asText());
        assertEquals(75, jsonCreate.get("sucChua").asInt());
        assertEquals("HOAT_DONG", jsonCreate.get("trangThai").asText());
        assertEquals("Hoạt động", jsonCreate.get("trangThaiHienThi").asText());

        // 2. Tra cứu chi tiết
        mockMvc.perform(get("/api/admin/phong-hoc/" + roomId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ma").value("TEST-A201"))
                .andExpect(jsonPath("$.sucChua").value(75));

        // 3. Sửa thông tin phòng
        ClassroomDtos.PhongHocRequest reqUpdate = new ClassroomDtos.PhongHocRequest(
                "TEST-A201",
                "Phòng học A201 VIP",
                "Nhà A",
                2,
                "Tầng 2, Nhà A, Cạnh cầu thang",
                80,
                "DA_NANG",
                "Máy chiếu, Điều hòa, Bảng thông minh",
                ClassroomStatus.HOAT_DONG,
                "Đã nâng cấp phòng"
        );

        mockMvc.perform(put("/api/admin/phong-hoc/" + roomId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ten").value("Phòng học A201 VIP"))
                .andExpect(jsonPath("$.sucChua").value(80))
                .andExpect(jsonPath("$.loaiPhong").value("DA_NANG"));

        // 4. Kiểm tra validate: từ chối mã trùng lặp (409 Conflict)
        ClassroomDtos.PhongHocRequest reqDuplicate = new ClassroomDtos.PhongHocRequest(
                "test-a201", // trùng mã sau khi chuẩn hóa
                "Phòng trùng",
                "Nhà B",
                1,
                null,
                50,
                "LY_THUYET",
                null,
                ClassroomStatus.HOAT_DONG,
                null
        );

        mockMvc.perform(post("/api/admin/phong-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDuplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("đã tồn tại")));

        // 5. Kiểm tra validate: sức chứa <= 0 (400 Bad Request)
        ClassroomDtos.PhongHocRequest reqInvalidCapacity = new ClassroomDtos.PhongHocRequest(
                "TEST-INVALID",
                "Phòng lỗi sức chứa",
                "Nhà A",
                1,
                null,
                0,
                "LY_THUYET",
                null,
                ClassroomStatus.HOAT_DONG,
                null
        );

        mockMvc.perform(post("/api/admin/phong-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInvalidCapacity)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void timKiemVaLocPhongHocTheoNhieuTieuChi() throws Exception {
        Classroom r1 = new Classroom("TEST-LAB-91", "Phòng Lab AI", "Tòa CNTT", 4, "Tầng 4", 35, "THUC_HANH_MAY_TINH", "40 PC, Điều hòa", ClassroomStatus.HOAT_DONG, null);
        Classroom r2 = new Classroom("TEST-HT-92", "Hội trường 92", "Tòa Trung Tâm", 1, "Tầng 1", 200, "HOI_TRUONG", "LED, Âm thanh", ClassroomStatus.HOAT_DONG, null);
        Classroom r3 = new Classroom("TEST-LT-93", "Phòng học 93", "Tòa CNTT", 2, "Tầng 2", 60, "LY_THUYET", "Máy chiếu", ClassroomStatus.BAO_TRI, "Đang bảo dưỡng");

        r1 = classroomRepository.save(r1);
        r2 = classroomRepository.save(r2);
        r3 = classroomRepository.save(r3);
        createdRoomIds.addAll(List.of(r1.getId(), r2.getId(), r3.getId()));

        // Lọc theo loại phòng
        mockMvc.perform(get("/api/admin/phong-hoc?loaiPhong=THUC_HANH_MAY_TINH")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ma == 'TEST-LAB-91')]").exists())
                .andExpect(jsonPath("$[?(@.ma == 'TEST-HT-92')]").doesNotExist());

        // Lọc theo trạng thái BAO_TRI
        mockMvc.perform(get("/api/admin/phong-hoc?trangThai=BAO_TRI")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ma == 'TEST-LT-93')]").exists())
                .andExpect(jsonPath("$[?(@.ma == 'TEST-LAB-91')]").doesNotExist());

        // Lọc theo sức chứa >= 100
        mockMvc.perform(get("/api/admin/phong-hoc?sucChuaMin=100")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ma == 'TEST-HT-92')]").exists())
                .andExpect(jsonPath("$[?(@.ma == 'TEST-LAB-91')]").doesNotExist());

        // Tìm theo keyword "Lab AI"
        mockMvc.perform(get("/api/admin/phong-hoc?keyword=Lab AI")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ma == 'TEST-LAB-91')]").exists());
    }

    @Test
    void chanLopHocPhanChonPhongDangBaoTriHoacNgungSuDung() throws Exception {
        Classroom phongBaoTri = classroomRepository.save(new Classroom(
                "TEST-BT-80", "Phòng bảo trì", "Nhà B", 2, null, 50, "LY_THUYET", null, ClassroomStatus.BAO_TRI, "Đang sửa điện"
        ));
        Classroom phongNgungDung = classroomRepository.save(new Classroom(
                "TEST-NG-80", "Phòng ngừng dùng", "Nhà C", 3, null, 40, "LY_THUYET", null, ClassroomStatus.NGUNG_SU_DUNG, "Đã dẹp"
        ));
        Classroom phongHoatDong = classroomRepository.save(new Classroom(
                "TEST-HD-80", "Phòng hoạt động", "Nhà A", 1, null, 60, "LY_THUYET", null, ClassroomStatus.HOAT_DONG, "Tốt"
        ));
        createdRoomIds.addAll(List.of(phongBaoTri.getId(), phongNgungDung.getId(), phongHoatDong.getId()));

        // 1. Cố gắng tạo lớp mới với phòng đang bảo trì -> 400 Bad Request
        CourseClassDtos.LopHocPhanRequest reqBaoTri = new CourseClassDtos.LopHocPhanRequest(
                "TEST_LHP_BT_80",
                monHoc.getId(),
                hocKy.getId(),
                giangVien.getId(),
                phongBaoTri.getMa(),
                2, 1, 3, 50
        );

        mockMvc.perform(post("/api/admin/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBaoTri)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("đang bảo trì")));

        // 2. Cố gắng tạo lớp mới với phòng đã ngừng sử dụng -> 400 Bad Request
        CourseClassDtos.LopHocPhanRequest reqNgungDung = new CourseClassDtos.LopHocPhanRequest(
                "TEST_LHP_NG_80",
                monHoc.getId(),
                hocKy.getId(),
                giangVien.getId(),
                phongNgungDung.getMa(),
                3, 1, 3, 40
        );

        mockMvc.perform(post("/api/admin/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqNgungDung)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("đã ngừng sử dụng")));

        // 3. Tạo lớp với phòng đang hoạt động -> Thành công 200 OK
        CourseClassDtos.LopHocPhanRequest reqHoatDong = new CourseClassDtos.LopHocPhanRequest(
                "TEST_LHP_HD_80",
                monHoc.getId(),
                hocKy.getId(),
                giangVien.getId(),
                phongHoatDong.getMa(),
                4, 1, 3, 50
        );

        MvcResult resHd = mockMvc.perform(post("/api/admin/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqHoatDong)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonLop = json(resHd);
        Long classId = jsonLop.get("lopHocPhan").get("id").asLong();
        createdClassIds.add(classId);

        // Kiểm tra phòng học được liên kết trong thực thể CourseClass
        CourseClass savedClass = lopHocPhanRepository.findById(classId).orElseThrow();
        assertEquals("TEST-HD-80", savedClass.getPhong());
        assertNotNull(savedClass.getPhongHoc());
        assertEquals(phongHoatDong.getId(), savedClass.getPhongHoc().getId());
    }

    @Test
    void chanXoaCungPhongDaGanLich_ChoPhepNgungSuDung() throws Exception {
        Classroom phong = classroomRepository.save(new Classroom(
                "TEST-SCHED-80", "Phòng có lịch", "Nhà A", 1, null, 60, "LY_THUYET", null, ClassroomStatus.HOAT_DONG, null
        ));
        createdRoomIds.add(phong.getId());

        // Tạo lớp gắn với phòng này
        CourseClassDtos.LopHocPhanRequest req = new CourseClassDtos.LopHocPhanRequest(
                "TEST_LHP_SCHED_80",
                monHoc.getId(),
                hocKy.getId(),
                giangVien.getId(),
                phong.getMa(),
                5, 7, 3, 50
        );
        MvcResult resClass = mockMvc.perform(post("/api/admin/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        Long classId = json(resClass).get("lopHocPhan").get("id").asLong();
        createdClassIds.add(classId);

        // 1. Thao tác xóa cứng bị từ chối an toàn với 400 Bad Request
        mockMvc.perform(delete("/api/admin/phong-hoc/" + phong.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("đã được xếp lịch")))
                .andExpect(jsonPath("$.message").value(containsString("không thể xóa cứng")));

        // 2. Chuyển trạng thái sang NGUNG_SU_DUNG -> Cho phép thành công
        ClassroomDtos.DoiTrangThaiRequest reqStatus = new ClassroomDtos.DoiTrangThaiRequest(
                ClassroomStatus.NGUNG_SU_DUNG, "Ngừng sử dụng phòng do chuyển công năng"
        );

        mockMvc.perform(patch("/api/admin/phong-hoc/" + phong.getId() + "/trang-thai")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("NGUNG_SU_DUNG"))
                .andExpect(jsonPath("$.trangThaiHienThi").value("Ngừng sử dụng"));

        // Lớp học phần cũ tiếp tục hiển thị thông tin phòng đầy đủ
        CourseClass lopCu = lopHocPhanRepository.findById(classId).orElseThrow();
        assertEquals("TEST-SCHED-80", lopCu.getPhong());
    }

    @Test
    void xoaPhongChuaGanLichThanhCong() throws Exception {
        Classroom phongRanh = classroomRepository.save(new Classroom(
                "TEST-RANH-80", "Phòng trống", "Nhà D", 1, null, 30, "LY_THUYET", null, ClassroomStatus.HOAT_DONG, null
        ));

        // Xóa phòng chưa từng có lớp
        mockMvc.perform(delete("/api/admin/phong-hoc/" + phongRanh.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        assertFalse(classroomRepository.existsById(phongRanh.getId()));
    }

    @Test
    void phanQuyenVaTaxonomy() throws Exception {
        // Sinh viên không có quyền truy cập CRUD phòng học -> 403 Forbidden
        mockMvc.perform(get("/api/admin/phong-hoc")
                        .header("Authorization", "Bearer " + tokenStudent))
                .andExpect(status().isForbidden());

        // Lấy taxonomy danh mục loại phòng và thiết bị mở rộng
        mockMvc.perform(get("/api/admin/phong-hoc/taxonomy")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.danhSachLoaiPhong").isArray())
                .andExpect(jsonPath("$.danhSachThietBiPhoBien").isArray())
                .andExpect(jsonPath("$.danhSachTrangThai").isArray());

        // Endpoint chung lấy phòng hoạt động
        mockMvc.perform(get("/api/phong-hoc/hoat-dong")
                        .header("Authorization", "Bearer " + tokenStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
