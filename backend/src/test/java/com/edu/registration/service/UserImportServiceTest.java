package com.edu.registration.service;

import com.edu.registration.config.UserImportProperties;
import com.edu.registration.dto.UserDtos;
import com.edu.registration.dto.ValidationErrorDetail;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentRepository sinhVienRepository;
    @Mock
    private LecturerRepository giangVienRepository;
    @Mock
    private FacultyRepository khoaRepository;
    @Mock
    private MajorRepository nganhRepository;
    @Mock
    private AdministrativeClassRepository lopHanhChinhRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private UserImportService userImportService;
    private UserImportProperties properties;

    @BeforeEach
    void setUp() {
        properties = new UserImportProperties(
                1024L * 1024L, // 1 MB
                2,             // maxRows = 2 for quick limit testing
                900L,
                "ROW_BY_ROW",
                "SKIP_IF_EXISTS",
                "Ghi chú kiểm thử"
        );

        userImportService = new UserImportService(
                userRepository,
                sinhVienRepository,
                giangVienRepository,
                khoaRepository,
                nganhRepository,
                lopHanhChinhRepository,
                passwordEncoder,
                auditLogService,
                transactionTemplate,
                properties
        );
    }

    @Test
    @DisplayName("Từ chối tệp CSV có số dòng vượt quá giới hạn cấu hình max-rows")
    void testRejectWhenRowsExceedMaxRowsLimit() {
        String csv3Rows = "ma,hoTen,email,khoaId\n"
                + "SV001,Nguyen A,a@edu.vn,1\n"
                + "SV002,Nguyen B,b@edu.vn,1\n"
                + "SV003,Nguyen C,c@edu.vn,1\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sinh-vien.csv",
                "text/csv",
                csv3Rows.getBytes(StandardCharsets.UTF_8)
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                userImportService.preview(UserImportService.TargetType.SINH_VIEN, file));

        assertTrue(ex.getMessage().contains("vượt quá giới hạn tối đa cho phép (2 dòng)"));
    }

    @Test
    @DisplayName("Từ chối tên tệp chứa Path Traversal")
    void testRejectPathTraversalFilename() {
        MockMultipartFile dangerousFile = new MockMultipartFile(
                "file",
                "../../hack.csv",
                "text/csv",
                "ma,hoTen,email,khoaId\nSV001,Nguyen A,a@edu.vn,1\n".getBytes(StandardCharsets.UTF_8)
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                userImportService.preview(UserImportService.TargetType.SINH_VIEN, dangerousFile));

        assertTrue(ex.getMessage().contains("Path Traversal"));
    }

    @Test
    @DisplayName("Lấy cấu hình import sinh viên và giảng viên đầy đủ metadata")
    void testLayCauHinhImportMetadata() {
        UserDtos.UserImportConfigDto svConfig = userImportService.layCauHinhImport(UserImportService.TargetType.SINH_VIEN);
        assertEquals("SINH_VIEN", svConfig.loaiImport());
        assertEquals("CSV", svConfig.dinhDangTep());
        assertEquals(".csv", svConfig.phanMoRong());
        assertEquals(2, svConfig.maxRows());
        assertEquals("ROW_BY_ROW", svConfig.transactionPolicy());
        assertEquals("SKIP_IF_EXISTS", svConfig.idempotencyPolicy());
        assertNotNull(svConfig.danhSachCot());
        assertTrue(svConfig.danhSachCot().size() >= 6);

        UserDtos.UserImportConfigDto gvConfig = userImportService.layCauHinhImport(UserImportService.TargetType.GIANG_VIEN);
        assertEquals("GIANG_VIEN", gvConfig.loaiImport());
        assertEquals("CSV", gvConfig.dinhDangTep());
        assertEquals(2, gvConfig.maxRows());
        assertEquals("ROW_BY_ROW", gvConfig.transactionPolicy());
        assertEquals("SKIP_IF_EXISTS", gvConfig.idempotencyPolicy());
        assertNotNull(gvConfig.danhSachCot());
        assertTrue(gvConfig.danhSachCot().size() >= 5);
    }

    @Test
    @DisplayName("Xuất báo cáo lỗi CSV chuẩn UTF-8 có BOM")
    void testXuatFileLoiCsv() {
        ValidationErrorDetail err = new ValidationErrorDetail(
                null, null, "SV001", null, 2, "email", null, "email", "invalid-email", "Email sai định dạng"
        );
        UserDtos.UserImportRowDetailDto rowDto = new UserDtos.UserImportRowDetailDto(
                2, "SV001", "Nguyen A", "invalid-email", "0912345678",
                1L, "CNTT", null, null, null, null, "SV001",
                false, "LOI", "BO_QUA", List.of(err)
        );

        byte[] errorCsv = userImportService.xuatFileLoiCsv(List.of(rowDto));
        assertNotNull(errorCsv);
        String content = new String(errorCsv, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("\uFEFF"));
        assertTrue(content.contains("SV001"));
        assertTrue(content.contains("Email sai định dạng"));
    }

    @Test
    @DisplayName("Từ chối import trực tiếp khi bộ phân tích trả về null")
    void testDirectImportRejectsNullParsedData() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sinh-vien.csv",
                "text/csv",
                "ma,hoTen,email,khoaId\nSV001,Nguyen A,a@edu.vn,1\n".getBytes(StandardCharsets.UTF_8)
        );

        when(transactionTemplate.execute(any())).thenReturn(null);

        assertThrows(ApiException.class, () ->
                userImportService.directImport(UserImportService.TargetType.SINH_VIEN, file));
    }
}
