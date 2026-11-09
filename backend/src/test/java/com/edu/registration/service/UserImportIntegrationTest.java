package com.edu.registration.service;

import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.AdministrativeClass;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class UserImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private AdministrativeClassRepository classRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LecturerRepository lecturerRepository;

    @Autowired
    private UserRepository userRepository;

    private String adminToken;
    private String studentToken;
    private Long khoaCnttId;
    private Long nganhCpmId;
    private Long lopCpm1Id;
    private Long khoaDtvtId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
        studentToken = login("sv01", "123456");

        // Tìm khoa CNTT và ngành, lớp có sẵn từ DataSeeder
        Faculty khoaCntt = facultyRepository.findAll().stream()
                .filter(k -> k.getMa().equalsIgnoreCase("CNTT"))
                .findFirst().orElseThrow();
        khoaCnttId = khoaCntt.getId();

        Faculty khoaDtvt = facultyRepository.findAll().stream()
                .filter(k -> !k.getId().equals(khoaCnttId))
                .findFirst().orElseThrow();
        khoaDtvtId = khoaDtvt.getId();

        Major nganhCpm = majorRepository.findAll().stream()
                .filter(n -> n.getKhoa().getId().equals(khoaCnttId))
                .findFirst().orElseThrow();
        nganhCpmId = nganhCpm.getId();

        AdministrativeClass lopCpm1 = classRepository.findAll().stream()
                .filter(l -> l.getNganh().getId().equals(nganhCpmId))
                .findFirst().orElseThrow();
        lopCpm1Id = lopCpm1.getId();
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
    @DisplayName("Tải template import sinh viên và giảng viên thành công")
    void testDownloadTemplates() throws Exception {
        mockMvc.perform(get("/api/admin/sinh-vien/import/template")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=mau-import-sinh-vien.csv"));

        mockMvc.perform(get("/api/admin/giang-vien/import/template")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=mau-import-giang-vien.csv"));
    }

    @Test
    @DisplayName("Chỉ quản trị mới được quyền import; sinh viên bị chặn 403")
    void testPhanQuyenAdmin() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "ma,hoTen\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/sinh-vien/import")
                        .file(file)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/admin/giang-vien/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("File sai cấu trúc header bị từ chối ngay, không xử lý file một phần")
    void testInvalidHeaderRejection() throws Exception {
        String invalidCsv = "ma,hoTen\nSV_ERR_1,Nguyen Van Error\n";
        MockMultipartFile file = new MockMultipartFile("file", "err.csv", "text/csv", invalidCsv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/sinh-vien/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cấu trúc header không hợp lệ")));
    }

    @Test
    @DisplayName("Quy trình Import Giảng viên: Preview -> Xác nhận (Confirm) -> Không trùng lặp (Idempotent)")
    void testLuongImportGiangVienPreviewVaConfirm() throws Exception {
        String maGv1 = "GV_" + (System.currentTimeMillis() % 1000000000L);
        String maGv2 = "GV_" + ((System.currentTimeMillis() + 1) % 1000000000L);

        String csvContent = "ma,hoTen,email,soDienThoai,khoaId\n"
                + maGv1 + ",Thầy Giáo Ba," + maGv1.toLowerCase() + "@univ.edu.vn,0987654321," + khoaCnttId + "\n"
                + maGv2 + ",Cô Giáo Bốn," + maGv2.toLowerCase() + "@univ.edu.vn,0988776655," + khoaCnttId + "\n";

        MockMultipartFile file = new MockMultipartFile("file", "giang-vien.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // 1. Preview
        MvcResult previewRes = mockMvc.perform(multipart("/api/admin/giang-vien/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tongSoDong").value(2))
                .andExpect(jsonPath("$.soDongHopLe").value(2))
                .andExpect(jsonPath("$.soDongLoi").value(0))
                .andExpect(jsonPath("$.coTheXacNhan").value(true))
                .andExpect(jsonPath("$.transactionPolicy").value("ROW_BY_ROW"))
                .andExpect(jsonPath("$.idempotencyPolicy").value("SKIP_IF_EXISTS"))
                .andReturn();

        JsonNode previewJson = json(previewRes);
        String previewToken = previewJson.get("previewToken").asText();
        String fileSha256 = previewJson.get("fileSha256").asText();
        assertNotNull(previewToken);

        // Đảm bảo không chứa mật khẩu trong kết quả preview
        assertFalse(previewJson.toString().contains("123456"));
        assertFalse(previewJson.toString().contains("password"));

        // 2. Confirm
        mockMvc.perform(post("/api/admin/giang-vien/import/confirm")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("previewToken", previewToken, "fileSha256", fileSha256))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soTaoMoi").value(2))
                .andExpect(jsonPath("$.soThatBai").value(0))
                .andExpect(jsonPath("$.transactionPolicy").value("ROW_BY_ROW"));

        // Kiểm tra giảng viên đã được tạo trong DB
        assertTrue(lecturerRepository.existsByMa(maGv1));
        assertTrue(lecturerRepository.existsByMa(maGv2));
        assertTrue(userRepository.existsByUsername(maGv1));

        // 3. Xác nhận lại lần 2 bằng token đã dùng -> Phải báo lỗi phiên đã được dùng
        mockMvc.perform(post("/api/admin/giang-vien/import/confirm")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("previewToken", previewToken, "fileSha256", fileSha256))))
                .andExpect(status().isBadRequest());

        // 4. Import lặp lại cùng file -> Idempotency check: báo lỗi trùng, không tạo tài khoản trùng
        MvcResult repeatRes = mockMvc.perform(multipart("/api/admin/giang-vien/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soDongHopLe").value(0))
                .andExpect(jsonPath("$.soDongLoi").value(2))
                .andExpect(jsonPath("$.coTheXacNhan").value(false))
                .andReturn();

        JsonNode repeatJson = json(repeatRes);
        assertTrue(repeatJson.toString().contains("đã tồn tại"));
    }

    @Test
    @DisplayName("Validate chi tiết từng dòng: ID tham chiếu không tồn tại hoặc không khớp quan hệ khoa/ngành/lớp")
    void testValidationThamChieuVaLoiTungDong() throws Exception {
        long nonExistentId = 999999L;
        String maSvValid = "SV_" + (System.currentTimeMillis() % 1000000000L);
        String maSvBadKhoa = "SVK_" + (System.currentTimeMillis() % 1000000000L);
        String maSvBadNganh = "SVN_" + (System.currentTimeMillis() % 1000000000L);
        String maSvBadRelation = "SVR_" + (System.currentTimeMillis() % 1000000000L);

        String csv = "ma,hoTen,email,khoaId,nganhId,lopId\n"
                // Dòng 2: Hợp lệ
                + maSvValid + ",Tran Van Hop Le,sv_valid@student.edu.vn," + khoaCnttId + "," + nganhCpmId + "," + lopCpm1Id + "\n"
                // Dòng 3: Khoa không tồn tại
                + maSvBadKhoa + ",Khoa Khong Ton Tai,sv_k@student.edu.vn," + nonExistentId + ",,\n"
                // Dòng 4: Ngành không tồn tại
                + maSvBadNganh + ",Nganh Khong Ton Tai,sv_n@student.edu.vn," + khoaCnttId + "," + nonExistentId + ",\n"
                // Dòng 5: Ngành không thuộc khoa (Khoa ĐTVT nhưng Ngành CPM thuộc CNTT)
                + maSvBadRelation + ",Sai Quan He,sv_rel@student.edu.vn," + khoaDtvtId + "," + nganhCpmId + ",\n";

        MockMultipartFile file = new MockMultipartFile("file", "sinh-vien-test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/admin/sinh-vien/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tongSoDong").value(4))
                .andExpect(jsonPath("$.soDongHopLe").value(1))
                .andExpect(jsonPath("$.soDongLoi").value(3))
                .andReturn();

        JsonNode resJson = json(result);
        JsonNode danhSachDong = resJson.get("danhSachDong");
        assertEquals(4, danhSachDong.size());

        // Dòng 2 hợp lệ
        assertTrue(danhSachDong.get(0).get("hopLe").asBoolean());
        // Dòng 3 lỗi khoa
        assertFalse(danhSachDong.get(1).get("hopLe").asBoolean());
        assertTrue(danhSachDong.get(1).get("errors").toString().contains("Khoa với ID " + nonExistentId + " không tồn tại"));
        // Dòng 4 lỗi ngành
        assertFalse(danhSachDong.get(2).get("hopLe").asBoolean());
        assertTrue(danhSachDong.get(2).get("errors").toString().contains("Ngành với ID " + nonExistentId + " không tồn tại"));
        // Dòng 5 lỗi quan hệ
        assertFalse(danhSachDong.get(3).get("hopLe").asBoolean());
        assertTrue(danhSachDong.get(3).get("errors").toString().contains("không thuộc khoa"));

        // Xuất file lỗi từ danh sách lỗi
        mockMvc.perform(post("/api/admin/sinh-vien/import/export-errors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(danhSachDong)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=loi-import-sinh-vien.csv"));
    }
}
