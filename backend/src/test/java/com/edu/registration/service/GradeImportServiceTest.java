package com.edu.registration.service;

import com.edu.registration.config.GradeImportProperties;
import com.edu.registration.dto.GradeImportDtos;
import com.edu.registration.dto.ValidationErrorDetail;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.util.XlsxWorkbookSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeImportServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private CourseClassRepository lopHocPhanRepository;
    @Mock
    private CourseRegistrationRepository dangKyHocPhanRepository;
    @Mock
    private GradeRepository diemRepository;
    @Mock
    private StudentRepository sinhVienRepository;
    @Mock
    private LecturerRepository giangVienRepository;
    @Mock
    private SemesterRepository hocKyRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private DtoMapper mapper;
    @Mock
    private NotificationService thongBaoService;

    private GradeService gradeService;
    private GradeImportService gradeImportService;
    private GradeImportProperties properties;
    private Clock baseClock;

    private User userGv1;
    private Lecturer gv1;
    private User userGv2;
    private Lecturer gv2;
    private CourseClass lop;
    private Student sv1;
    private Student sv2;
    private CourseRegistration dk1;
    private CourseRegistration dk2;

    @BeforeEach
    void setUp() {
        baseClock = Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneOffset.UTC);
        properties = new GradeImportProperties(
                tempDir.toString(),
                1024L * 1024L, // 1 MB
                5,             // maxRows = 5 for testing limits easily
                1,             // maxSheets = 1
                5000L,
                900L,          // 15 minutes
                5L * 1024L * 1024L,
                40,
                "ALL_OR_NOTHING",
                "OVERWRITE_EXISTING",
                null
        );

        gradeService = new GradeService(
                dangKyHocPhanRepository,
                diemRepository,
                lopHocPhanRepository,
                sinhVienRepository,
                giangVienRepository,
                hocKyRepository,
                currentUser,
                mapper,
                null,
                thongBaoService
        );

        gradeImportService = new GradeImportService(
                lopHocPhanRepository,
                dangKyHocPhanRepository,
                diemRepository,
                gradeService,
                currentUser,
                null,
                properties,
                baseClock
        );

        userGv1 = new User();
        userGv1.setId(10L);
        userGv1.setUsername("gv01");
        userGv1.setRole(Role.GIANG_VIEN);

        gv1 = new Lecturer();
        gv1.setId(1L);
        gv1.setMa("GV01");
        gv1.setHoTen("TS. Nguyễn Văn An");
        gv1.setUser(userGv1);

        userGv2 = new User();
        userGv2.setId(20L);
        userGv2.setUsername("gv02");
        userGv2.setRole(Role.GIANG_VIEN);

        gv2 = new Lecturer();
        gv2.setId(2L);
        gv2.setMa("GV02");
        gv2.setHoTen("ThS. Lê Thị Bình");
        gv2.setUser(userGv2);

        Subject mon = new Subject();
        mon.setId(100L);
        mon.setMa("INT201");
        mon.setTen("Cấu trúc dữ liệu và giải thuật");
        mon.setSoTinChi(3);

        Semester ky = new Semester();
        ky.setId(1L);
        ky.setTen("Học kỳ 1");
        ky.setNamHoc("2025-2026");

        lop = new CourseClass();
        lop.setId(500L);
        lop.setMa("INT201.01");
        lop.setMonHoc(mon);
        lop.setHocKy(ky);
        lop.setGiangVien(gv1);
        lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);

        sv1 = new Student();
        sv1.setId(1001L);
        sv1.setMa("SV001");
        sv1.setHoTen("Trần Minh Anh");

        sv2 = new Student();
        sv2.setId(1002L);
        sv2.setMa("SV002");
        sv2.setHoTen("Lê Hoàng Nam");

        dk1 = new CourseRegistration();
        dk1.setId(3001L);
        dk1.setSinhVien(sv1);
        dk1.setLopHocPhan(lop);
        dk1.setTrangThai(RegistrationStatus.DA_DANG_KY);

        dk2 = new CourseRegistration();
        dk2.setId(3002L);
        dk2.setSinhVien(sv2);
        dk2.setLopHocPhan(lop);
        dk2.setTrangThai(RegistrationStatus.DA_DANG_KY);
    }

    private void loginAs(User user, Lecturer lecturer) {
        lenient().when(currentUser.get()).thenReturn(user);
        lenient().when(giangVienRepository.findByUser(user)).thenReturn(Optional.of(lecturer));
    }

    private void assertTempDirClean() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            assertEquals(0L, files.count(), "Thư mục tạm phải được dọn sạch hoàn toàn sau mỗi thao tác");
        }
    }

    @Test
    void taiTemplateXlsxThanhCongChoGiangVienPhuTrachVaTuChoiGiangVienKhac() throws IOException {
        when(lopHocPhanRepository.findWithDetailsById(500L)).thenReturn(Optional.of(lop));
        when(dangKyHocPhanRepository.findByLopHocPhan(500L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dk2, dk1));
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of());

        // 1. Giảng viên phụ trách tải thành công
        loginAs(userGv1, gv1);
        GradeImportService.XlsxFile template = gradeImportService.xuatTemplateXlsx(500L);
        assertEquals("mau-nhap-diem-INT201.01.xlsx", template.tenTep());
        assertEquals(2, template.soSinhVien());
        assertTrue(template.noiDung().length > 100);

        // Kiểm tra template tải về có thể đọc lại ngay bằng XlsxWorkbookSupport
        Path checkPath = tempDir.resolve("check-template.xlsx");
        Files.write(checkPath, template.noiDung());
        XlsxWorkbookSupport.ParsedSheet parsed = XlsxWorkbookSupport.parseWorkbook(checkPath, properties);
        Files.deleteIfExists(checkPath);

        assertEquals("BangDiem", parsed.sheetName());
        assertEquals("maSinhVien", parsed.headerRow().cellAt(0).rawText());
        assertEquals("hoTen", parsed.headerRow().cellAt(1).rawText());
        assertEquals("diemQuaTrinh", parsed.headerRow().cellAt(2).rawText());
        assertEquals("diemCuoiKy", parsed.headerRow().cellAt(3).rawText());
        assertEquals(2, parsed.dataRows().size());
        assertEquals("SV001", parsed.dataRows().get(0).cellAt(0).rawText());
        assertEquals("SV002", parsed.dataRows().get(1).cellAt(0).rawText());

        // 2. Giảng viên không phụ trách lớp bị từ chối 403
        loginAs(userGv2, gv2);
        ApiException forbidden = assertThrows(ApiException.class, () -> gradeImportService.xuatTemplateXlsx(500L));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatus());
        assertTrue(forbidden.getMessage().contains("không phụ trách"));
    }

    @Test
    void tuChoiCsvDinhDangKhacTepGiaMaoVaTepVuotDungLuongHoacSoSheetSoDong() throws IOException {
        loginAs(userGv1, gv1);
        when(lopHocPhanRepository.findWithDetailsById(500L)).thenReturn(Optional.of(lop));

        // 1. Từ chối tệp .csv
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "diem.csv", "text/csv",
                "maSinhVien,diemQuaTrinh,diemCuoiKy\nSV001,8,9".getBytes(StandardCharsets.UTF_8));
        ApiException csvEx = assertThrows(ApiException.class, () -> gradeImportService.xemTruocImport(500L, csvFile));
        assertTrue(csvEx.getMessage().contains("Không chấp nhận tệp CSV"));
        assertTempDirClean();

        // 2. Từ chối nội dung CSV/text đổi tên thành .xlsx (không tin tên tệp hay MIME client)
        MockMultipartFile fakeXlsx = new MockMultipartFile(
                "file", "diem.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE,
                "maSinhVien,diemQuaTrinh,diemCuoiKy\nSV001,8,9".getBytes(StandardCharsets.UTF_8));
        ApiException fakeEx = assertThrows(ApiException.class, () -> gradeImportService.xemTruocImport(500L, fakeXlsx));
        assertTrue(fakeEx.getMessage().contains("không phải là workbook .xlsx hợp lệ"));
        assertTempDirClean();

        // 3. Từ chối tệp .xls OLE2 cũ đổi tên thành .xlsx
        byte[] ole2Bytes = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0x01, 0x02, 0x03, 0x04};
        MockMultipartFile ole2File = new MockMultipartFile(
                "file", "old-excel.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE, ole2Bytes);
        ApiException ole2Ex = assertThrows(ApiException.class, () -> gradeImportService.xemTruocImport(500L, ole2File));
        assertTrue(ole2Ex.getMessage().contains(".xls"));
        assertTempDirClean();

        // 4. Từ chối workbook vượt số sheet cho phép (maxSheets = 1)
        Map<String, List<List<Object>>> twoSheets = new LinkedHashMap<>();
        twoSheets.put("Sheet1", List.of(List.of("maSinhVien", "diemQuaTrinh", "diemCuoiKy"), List.of("SV001", 8.0, 9.0)));
        twoSheets.put("Sheet2", List.of(List.of("maSinhVien", "diemQuaTrinh", "diemCuoiKy")));
        MockMultipartFile multiSheetFile = new MockMultipartFile(
                "file", "two-sheets.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE,
                XlsxWorkbookSupport.createWorkbookBytes(twoSheets));
        ApiException sheetEx = assertThrows(ApiException.class, () -> gradeImportService.xemTruocImport(500L, multiSheetFile));
        assertTrue(sheetEx.getMessage().contains("vượt quá giới hạn tối đa cho phép (1 sheet)"));
        assertTempDirClean();

        // 5. Từ chối workbook sai template header
        Map<String, List<List<Object>>> wrongTemplate = Map.of(
                "BangDiem", List.of(
                        List.of("maSinhVien", "cotKhongHopLe"),
                        List.of("SV001", 8.0)
                )
        );
        MockMultipartFile wrongHeaderFile = new MockMultipartFile(
                "file", "wrong-template.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE,
                XlsxWorkbookSupport.createWorkbookBytes(wrongTemplate));
        ApiException tplEx = assertThrows(ApiException.class, () -> gradeImportService.xemTruocImport(500L, wrongHeaderFile));
        assertTrue(tplEx.getMessage().contains("không đúng định dạng template"));
        assertTempDirClean();

        // 6. Từ chối workbook vượt quá số dòng tối đa (maxRows = 5)
        Map<String, List<List<Object>>> tooManyRows = Map.of(
                "BangDiem", List.of(
                        List.of("maSinhVien", "diemQuaTrinh", "diemCuoiKy"),
                        List.of("SV001", 8.0, 8.0),
                        List.of("SV002", 8.0, 8.0),
                        List.of("SV003", 8.0, 8.0),
                        List.of("SV004", 8.0, 8.0),
                        List.of("SV005", 8.0, 8.0),
                        List.of("SV006", 8.0, 8.0)
                )
        );
        MockMultipartFile maxRowsFile = new MockMultipartFile(
                "file", "too-many-rows.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE,
                XlsxWorkbookSupport.createWorkbookBytes(tooManyRows));
        ApiException rowsEx = assertThrows(ApiException.class, () -> gradeImportService.xemTruocImport(500L, maxRowsFile));
        assertTrue(rowsEx.getMessage().contains("vượt quá giới hạn tối đa cho phép (5 dòng)"));
        assertTempDirClean();
    }

    @Test
    void phatHienChinhXacSheetDongCotKhiTrungMaSvKhongThuocLopDiemSaiMienHoacChuaCongThuc() throws IOException {
        loginAs(userGv1, gv1);
        when(lopHocPhanRepository.findWithDetailsById(500L)).thenReturn(Optional.of(lop));
        when(dangKyHocPhanRepository.findByLopHocPhan(500L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dk1, dk2));
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of());

        Map<String, List<List<Object>>> sheetData = Map.of(
                "BangDiem", List.of(
                        List.of("maSinhVien", "hoTen", "diemQuaTrinh", "diemCuoiKy"),
                        List.of("SV001", "Trần Minh Anh", 8.0, 9.0),
                        List.of("SV001", "Trần Minh Anh", 11.5, 8.0),
                        List.of("SV999", "Người Lạ", 7.5, 8.125),
                        List.of("SV002", "Lê Hoàng Nam", "FORMULA:4+4", "abc")
                )
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "loi-chi-tiet.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE,
                XlsxWorkbookSupport.createWorkbookBytes(sheetData));

        GradeImportDtos.PreviewResponse preview = gradeImportService.xemTruocImport(500L, file);
        assertTempDirClean();

        assertEquals(4, preview.tongSoDong());
        assertEquals(1, preview.soDongHopLe());
        assertEquals(3, preview.soDongLoi());
        assertEquals(1, preview.soTaoMoiDuKien());
        assertEquals(0, preview.soCapNhatDuKien());
        assertFalse(preview.coTheXacNhan());
        assertEquals(6, preview.errors().size());

        verify(diemRepository, never()).save(any());
        verify(diemRepository, never()).saveAll(any());

        ValidationErrorDetail errRow3ColA = preview.errors().get(0);
        assertEquals("BangDiem", errRow3ColA.sheet());
        assertEquals(3, errRow3ColA.dong());
        assertEquals("maSinhVien", errRow3ColA.cot());
        assertEquals("A", errRow3ColA.cotExcel());
        assertTrue(errRow3ColA.message().contains("trùng lặp"));

        ValidationErrorDetail errRow3ColC = preview.errors().get(1);
        assertEquals("BangDiem", errRow3ColC.sheet());
        assertEquals(3, errRow3ColC.dong());
        assertEquals("diemQuaTrinh", errRow3ColC.cot());
        assertEquals("C", errRow3ColC.cotExcel());
        assertEquals("Điểm phải từ 0 đến 10", errRow3ColC.message());

        ValidationErrorDetail errRow4ColA = preview.errors().get(2);
        assertEquals(4, errRow4ColA.dong());
        assertEquals("A", errRow4ColA.cotExcel());
        assertTrue(errRow4ColA.message().contains("không thuộc danh sách đăng ký"));

        ValidationErrorDetail errRow4ColD = preview.errors().get(3);
        assertEquals(4, errRow4ColD.dong());
        assertEquals("diemCuoiKy", errRow4ColD.cot());
        assertEquals("D", errRow4ColD.cotExcel());
        assertEquals("Điểm chỉ được có tối đa 2 chữ số thập phân", errRow4ColD.message());

        ValidationErrorDetail errRow5ColC = preview.errors().get(4);
        assertEquals(5, errRow5ColC.dong());
        assertEquals("C", errRow5ColC.cotExcel());
        assertTrue(errRow5ColC.message().contains("công thức"));

        ValidationErrorDetail errRow5ColD = preview.errors().get(5);
        assertEquals(5, errRow5ColD.dong());
        assertEquals("D", errRow5ColD.cotExcel());
        assertEquals("Điểm không đúng định dạng số", errRow5ColD.message());

        ApiException confirmEx = assertThrows(ApiException.class, () ->
                gradeImportService.xacNhanImport(500L,
                        new GradeImportDtos.ConfirmImportRequest(preview.previewToken(), preview.fileSha256())));
        assertEquals(HttpStatus.BAD_REQUEST, confirmEx.getStatus());
        assertEquals(6, confirmEx.getErrors().size());
        verify(diemRepository, never()).saveAll(any());
    }

    @Test
    void luongPreviewXacNhanVaUploadLaiKhongTaoDuLieuTrung() throws IOException {
        loginAs(userGv1, gv1);
        when(lopHocPhanRepository.findWithDetailsById(500L)).thenReturn(Optional.of(lop));
        when(lopHocPhanRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(lop));
        when(dangKyHocPhanRepository.findByLopHocPhan(500L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dk1, dk2));

        Grade existingSv1Grade = new Grade();
        existingSv1Grade.setId(9001L);
        existingSv1Grade.setDangKy(dk1);
        existingSv1Grade.setDiemQuaTrinh(6.0);
        existingSv1Grade.setDiemCuoiKy(7.0);
        existingSv1Grade.setDiemTongKet(6.6);

        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of(existingSv1Grade));

        Map<String, List<List<Object>>> sheetData = Map.of(
                "BangDiem", List.of(
                        List.of("maSinhVien", "hoTen", "diemQuaTrinh", "diemCuoiKy"),
                        List.of("SV001", "Trần Minh Anh", 8.0, 9.0),
                        List.of("SV002", "Lê Hoàng Nam", 7.5, 8.5)
                )
        );
        byte[] xlsxBytes = XlsxWorkbookSupport.createWorkbookBytes(sheetData);
        MockMultipartFile file = new MockMultipartFile(
                "file", "bang-diem-hop-le.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE, xlsxBytes);

        // 1. Preview
        GradeImportDtos.PreviewResponse preview = gradeImportService.xemTruocImport(500L, file);
        assertTempDirClean();

        assertTrue(preview.coTheXacNhan());
        assertEquals(2, preview.tongSoDong());
        assertEquals(2, preview.soDongHopLe());
        assertEquals(0, preview.soDongLoi());
        assertEquals(1, preview.soTaoMoiDuKien());
        assertEquals(1, preview.soCapNhatDuKien());
        verify(diemRepository, never()).saveAll(any());

        // 2. Confirm
        GradeImportDtos.ConfirmImportResponse result = gradeImportService.xacNhanImport(
                500L, new GradeImportDtos.ConfirmImportRequest(preview.previewToken(), preview.fileSha256()));

        assertEquals(2, result.tongSoDong());
        assertEquals(1, result.soTaoMoi());
        assertEquals(1, result.soCapNhat());
        assertEquals(0, result.soThatBai());
        assertEquals("ALL_OR_NOTHING", result.transactionPolicy());
        assertEquals("OVERWRITE_EXISTING", result.updatePolicy());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Grade>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(diemRepository, times(1)).saveAll(saveCaptor.capture());
        List<Grade> savedGrades = saveCaptor.getValue();
        assertEquals(2, savedGrades.size());

        assertEquals(9001L, savedGrades.get(0).getId());
        assertEquals(8.6, savedGrades.get(0).getDiemTongKet());
        assertEquals(8.1, savedGrades.get(1).getDiemTongKet());

        // 3. Không cho phép dùng lại cùng previewToken lần 2
        ApiException replayEx = assertThrows(ApiException.class, () ->
                gradeImportService.xacNhanImport(
                        500L, new GradeImportDtos.ConfirmImportRequest(preview.previewToken(), preview.fileSha256())));
        assertEquals(HttpStatus.BAD_REQUEST, replayEx.getStatus());

        // 4. Upload lại cùng file lần 2 ở trạng thái TRA_VE vẫn cho phép import và giữ nguyên TRA_VE
        lop.setTrangThaiBangDiem(GradeSheetStatus.TRA_VE);
        Grade savedSv2Grade = savedGrades.get(1);
        savedSv2Grade.setId(9002L);
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of(existingSv1Grade, savedSv2Grade));

        GradeImportDtos.PreviewResponse preview2 = gradeImportService.xemTruocImport(500L, file);
        assertEquals(0, preview2.soTaoMoiDuKien());
        assertEquals(2, preview2.soCapNhatDuKien());

        GradeImportDtos.ConfirmImportResponse result2 = gradeImportService.xacNhanImport(
                500L, new GradeImportDtos.ConfirmImportRequest(preview2.previewToken(), preview2.fileSha256()));
        assertEquals(0, result2.soTaoMoi());
        assertEquals(2, result2.soCapNhat());
        assertEquals(GradeSheetStatus.TRA_VE, lop.getTrangThaiBangDiem());
    }

    @Test
    void chanImportKhiBangDiemDangChoDuyetDaDuyetDaCongBoHoacDaKhoa() {
        loginAs(userGv1, gv1);
        when(lopHocPhanRepository.findWithDetailsById(500L)).thenReturn(Optional.of(lop));

        Map<String, List<List<Object>>> sheetData = Map.of(
                "BangDiem", List.of(
                        List.of("maSinhVien", "diemQuaTrinh", "diemCuoiKy"),
                        List.of("SV001", 8.0, 9.0)
                )
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "diem.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE,
                XlsxWorkbookSupport.createWorkbookBytes(sheetData));

        for (GradeSheetStatus blockedStatus : List.of(
                GradeSheetStatus.CHO_DUYET,
                GradeSheetStatus.DA_DUYET,
                GradeSheetStatus.DA_CONG_BO,
                GradeSheetStatus.DA_KHOA)) {
            lop.setTrangThaiBangDiem(blockedStatus);
            ApiException ex = assertThrows(ApiException.class,
                    () -> gradeImportService.xemTruocImport(500L, file));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }
    }

    @Test
    void rangBuocPreviewTokenTheoLopGiangVienVaThoiHan() {
        loginAs(userGv1, gv1);
        when(lopHocPhanRepository.findWithDetailsById(500L)).thenReturn(Optional.of(lop));
        when(lopHocPhanRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(lop));
        when(dangKyHocPhanRepository.findByLopHocPhan(500L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dk1));
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of());

        Map<String, List<List<Object>>> sheetData = Map.of(
                "BangDiem", List.of(
                        List.of("maSinhVien", "diemQuaTrinh", "diemCuoiKy"),
                        List.of("SV001", 8.0, 9.0)
                )
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "diem.xlsx", XlsxWorkbookSupport.XLSX_MEDIA_TYPE,
                XlsxWorkbookSupport.createWorkbookBytes(sheetData));

        GradeImportDtos.PreviewResponse preview = gradeImportService.xemTruocImport(500L, file);
        assertNotNull(preview.previewToken());

        // 1. Xác nhận nhầm sang lớp học phần khác -> 403 Forbidden
        CourseClass lopKhac = new CourseClass();
        lopKhac.setId(501L);
        lopKhac.setMa("INT201.02");
        lopKhac.setGiangVien(gv1);
        lopKhac.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
        when(lopHocPhanRepository.findByIdForUpdate(501L)).thenReturn(Optional.of(lopKhac));

        ApiException wrongClassEx = assertThrows(ApiException.class, () ->
                gradeImportService.xacNhanImport(501L,
                        new GradeImportDtos.ConfirmImportRequest(preview.previewToken(), preview.fileSha256())));
        assertEquals(HttpStatus.FORBIDDEN, wrongClassEx.getStatus());

        // 2. Sai mã băm SHA-256 -> 400 Bad Request
        ApiException wrongHashEx = assertThrows(ApiException.class, () ->
                gradeImportService.xacNhanImport(500L,
                        new GradeImportDtos.ConfirmImportRequest(preview.previewToken(), "0000000000")));
        assertEquals(HttpStatus.BAD_REQUEST, wrongHashEx.getStatus());

        // 3. Quá thời hạn TTL (sau 16 phút) -> 400 Bad Request
        gradeImportService.setClock(Clock.offset(baseClock, Duration.ofMinutes(16)));
        ApiException expiredEx = assertThrows(ApiException.class, () ->
                gradeImportService.xacNhanImport(500L,
                        new GradeImportDtos.ConfirmImportRequest(preview.previewToken(), preview.fileSha256())));
        assertEquals(HttpStatus.BAD_REQUEST, expiredEx.getStatus());
        assertTrue(expiredEx.getMessage().contains("hết hạn"));
        verify(diemRepository, never()).saveAll(any());
    }

    @Test
    void testXuatFileLoiCsv() {
        List<ValidationErrorDetail> errors = List.of(
                new ValidationErrorDetail(10L, 20L, "SV001", "BangDiem", 2, "diemQuaTrinh", "C", "diemQuaTrinh", "15", "Điểm phải nằm trong khoảng [0, 10]"),
                new ValidationErrorDetail(11L, 21L, "SV002", "BangDiem", 3, "diemCuoiKy", "D", "diemCuoiKy", "=SUM(A1)", "Không được chứa công thức")
        );

        byte[] csv = gradeImportService.xuatFileLoiCsv(500L, errors);
        assertNotNull(csv);
        String text = new String(csv, StandardCharsets.UTF_8);
        assertTrue(text.contains("SV001"));
        assertTrue(text.contains("SV002"));
        assertTrue(text.contains("Điểm phải nằm trong khoảng"));
    }

    @Test
    void testXemTruocChặnPathTraversalFilename() {
        loginAs(userGv1, gv1);
        when(lopHocPhanRepository.findWithDetailsById(500L)).thenReturn(Optional.of(lop));

        MockMultipartFile badFile = new MockMultipartFile(
                "file",
                "../../evil.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] { 0x50, 0x4B, 0x03, 0x04 }
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                gradeImportService.xemTruocImport(500L, badFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Path Traversal"));
    }
}
