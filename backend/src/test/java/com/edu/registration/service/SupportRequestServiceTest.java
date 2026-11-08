package com.edu.registration.service;

import com.edu.registration.config.SupportRequestProperties;
import com.edu.registration.dto.SupportRequestDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.SupportRequestStatusHistory;
import com.edu.registration.entity.SupportRequestType;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.SupportRequestAttachment;
import com.edu.registration.entity.SupportRequestStatus;
import com.edu.registration.entity.SupportRequestComment;
import com.edu.registration.entity.User;
import com.edu.registration.entity.SupportRequest;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.SupportRequestStatusHistoryRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SupportRequestAttachmentRepository;
import com.edu.registration.repository.SupportRequestCommentRepository;
import com.edu.registration.repository.SupportRequestRepository;
import com.edu.registration.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest {

    @TempDir
    Path tempDir;

    @Mock private SupportRequestRepository yeuCauRepository;
    @Mock private SupportRequestCommentRepository traoDoiRepository;
    @Mock private SupportRequestStatusHistoryRepository lichSuTrangThaiRepository;
    @Mock private SupportRequestAttachmentRepository tepDinhKemRepository;
    @Mock private StudentRepository sinhVienRepository;
    @Mock private SubjectRepository monHocRepository;
    @Mock private SemesterRepository hocKyRepository;
    @Mock private GradeRepository diemRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUser currentUser;
    @Mock private PlatformTransactionManager transactionManager;

    private SupportRequestProperties properties;
    private SupportAttachmentStorageService storageService;
    private SupportRequestService yeuCauHoTroService;

    private User studentUserA;
    private Student sinhVienA;
    private User studentUserB;
    private Student sinhVienB;
    private User adminUser;
    private Subject monHoc;
    private Semester hocKy;
    private Grade diemA;
    private Grade diemB;

    @BeforeEach
    void setUp() {
        properties = new SupportRequestProperties(
                tempDir.toString(),
                1024L * 1024L, // 1 MB max file size for test
                3,
                List.of("pdf", "png", "jpg", "jpeg"),
                null,
                null,
                null,
                ""
        );
        storageService = new SupportAttachmentStorageService(properties);
        AuditLogService auditLogService = new AuditLogService(
                auditLogRepository, currentUser, new ObjectMapper(), transactionManager);
        yeuCauHoTroService = new SupportRequestService(
                yeuCauRepository,
                traoDoiRepository,
                lichSuTrangThaiRepository,
                tepDinhKemRepository,
                sinhVienRepository,
                monHocRepository,
                hocKyRepository,
                diemRepository,
                currentUser,
                storageService,
                auditLogService,
                properties
        );

        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        studentUserA = new User();
        studentUserA.setId(1L);
        studentUserA.setUsername("sv01");
        studentUserA.setRole(Role.SINH_VIEN);

        sinhVienA = new Student();
        sinhVienA.setId(101L);
        sinhVienA.setMa("SV001");
        sinhVienA.setHoTen("Trần Minh Anh");
        sinhVienA.setUser(studentUserA);

        studentUserB = new User();
        studentUserB.setId(2L);
        studentUserB.setUsername("sv02");
        studentUserB.setRole(Role.SINH_VIEN);

        sinhVienB = new Student();
        sinhVienB.setId(102L);
        sinhVienB.setMa("SV002");
        sinhVienB.setHoTen("Phạm Thu Hà");
        sinhVienB.setUser(studentUserB);

        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.QUAN_TRI);

        monHoc = new Subject();
        monHoc.setId(10L);
        monHoc.setMa("INT201");
        monHoc.setTen("Cấu trúc dữ liệu và giải thuật");
        monHoc.setSoTinChi(3);

        hocKy = new Semester();
        hocKy.setId(20L);
        hocKy.setTen("Học kỳ 1");
        hocKy.setNamHoc("2026-2027");

        CourseClass lop = new CourseClass();
        lop.setId(30L);
        lop.setMa("INT201.01");
        lop.setMonHoc(monHoc);
        lop.setHocKy(hocKy);

        CourseRegistration dkA = new CourseRegistration();
        dkA.setId(40L);
        dkA.setSinhVien(sinhVienA);
        dkA.setLopHocPhan(lop);

        diemA = new Grade();
        diemA.setId(50L);
        diemA.setDangKy(dkA);
        diemA.setDiemQuaTrinh(8.0);
        diemA.setDiemCuoiKy(8.5);
        diemA.setDiemTongKet(8.3);

        CourseRegistration dkB = new CourseRegistration();
        dkB.setId(41L);
        dkB.setSinhVien(sinhVienB);
        dkB.setLopHocPhan(lop);

        diemB = new Grade();
        diemB.setId(51L);
        diemB.setDangKy(dkB);
        diemB.setDiemTongKet(7.0);
    }

    @Test
    void studentCreatesTicketWithValidAttachmentAndInitialStatusHistory() throws Exception {
        when(currentUser.get()).thenReturn(studentUserA);
        when(sinhVienRepository.findByUser(studentUserA)).thenReturn(Optional.of(sinhVienA));
        when(diemRepository.findWithDetailsById(50L)).thenReturn(Optional.of(diemA));
        when(yeuCauRepository.existsByMaPhieu(any())).thenReturn(false);
        when(yeuCauRepository.saveAndFlush(any(SupportRequest.class))).thenAnswer(inv -> {
            SupportRequest entity = inv.getArgument(0);
            entity.setId(500L);
            return entity;
        });
        when(lichSuTrangThaiRepository.saveAndFlush(any(SupportRequestStatusHistory.class))).thenAnswer(inv -> {
            SupportRequestStatusHistory h = inv.getArgument(0);
            h.setId(600L);
            return h;
        });
        when(tepDinhKemRepository.saveAllAndFlush(any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<SupportRequestAttachment> list = inv.getArgument(0);
            list.get(0).setId(700L);
            return list;
        });

        byte[] validPdfBytes = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
        MockMultipartFile pdfFile = new MockMultipartFile(
                "files", "minh-chung-diem.pdf", "text/plain", validPdfBytes);

        SupportRequestDtos.TaoYeuCauRequest req = new SupportRequestDtos.TaoYeuCauRequest(
                "DIEM_SO",
                "Đề nghị kiểm tra lại điểm cuối kỳ INT201",
                "Em xin phúc khảo điểm cuối kỳ môn Cấu trúc dữ liệu và giải thuật lớp INT201.01.",
                null,
                50L,
                null
        );

        SupportRequestDtos.YeuCauChiTietDto created = yeuCauHoTroService.taoYeuCau(req, List.of(pdfFile));

        assertEquals(500L, created.id());
        assertEquals(101L, created.sinhVienId());
        assertEquals("SV001", created.maSinhVien());
        assertEquals("DIEM_SO", created.loaiYeuCau());
        assertEquals("MOI_TAO", created.trangThai());
        assertEquals("INT201", created.monHoc().ma());
        assertEquals("Học kỳ 1", created.hocKy().ten());
        assertEquals(1, created.tepDinhKem().size());
        assertEquals("minh-chung-diem.pdf", created.tepDinhKem().get(0).tenTepGoc());
        assertEquals("application/pdf", created.tepDinhKem().get(0).contentType());
        assertEquals(1, created.lichSuTrangThai().size());
        assertEquals("MOI_TAO", created.lichSuTrangThai().get(0).trangThaiMoi());
        assertEquals(1L, created.lichSuTrangThai().get(0).nguoiThucHienId());
        assertNotNull(created.lichSuTrangThai().get(0).thoiDiem());
    }

    @Test
    void studentACannotAccessCommentOrDownloadFromStudentBTicket() {
        SupportRequest ticketOfB = new SupportRequest();
        ticketOfB.setId(900L);
        ticketOfB.setMaPhieu("HT-20261105-BBBBBB");
        ticketOfB.setSinhVien(sinhVienB);
        ticketOfB.setNguoiTao(studentUserB);
        ticketOfB.setLoaiYeuCau(SupportRequestType.HOC_PHAN);
        ticketOfB.setTieuDe("Hỗ trợ đăng ký học phần");
        ticketOfB.setNoiDungGoc("Nội dung hỗ trợ của sinh viên B");
        ticketOfB.setTrangThai(SupportRequestStatus.MOI_TAO);

        when(currentUser.get()).thenReturn(studentUserA);
        when(yeuCauRepository.findWithDetailsById(900L)).thenReturn(Optional.of(ticketOfB));

        ApiException viewEx = assertThrows(ApiException.class, () -> yeuCauHoTroService.chiTiet(900L));
        assertEquals(HttpStatus.FORBIDDEN, viewEx.getStatus());

        ApiException commentEx = assertThrows(ApiException.class, () -> yeuCauHoTroService.themTraoDoi(
                900L, new SupportRequestDtos.ThemTraoDoiRequest("Thử chèn bình luận trái phép")));
        assertEquals(HttpStatus.FORBIDDEN, commentEx.getStatus());

        ApiException downloadEx = assertThrows(ApiException.class, () -> yeuCauHoTroService.taiTepDinhKem(900L, 1L));
        assertEquals(HttpStatus.FORBIDDEN, downloadEx.getStatus());
    }

    @Test
    void studentACannotReferenceStudentBGradeRecordWhenCreatingTicket() {
        when(currentUser.get()).thenReturn(studentUserA);
        when(sinhVienRepository.findByUser(studentUserA)).thenReturn(Optional.of(sinhVienA));
        when(diemRepository.findWithDetailsById(51L)).thenReturn(Optional.of(diemB));

        SupportRequestDtos.TaoYeuCauRequest req = new SupportRequestDtos.TaoYeuCauRequest(
                "DIEM_SO",
                "Khiếu nại điểm không thuộc quyền sở hữu",
                "Nội dung khiếu nại điểm cố tình truyền diemId của sinh viên B.",
                null,
                51L,
                null
        );

        ApiException ex = assertThrows(ApiException.class, () -> yeuCauHoTroService.taoYeuCau(req, List.of()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void rejectsSpoofedMimeExecutableDoubleExtensionPathTraversalAndOversizedFile() {
        // 1. Spoofed extension (.pdf with plain text content)
        MockMultipartFile spoofedPdf = new MockMultipartFile(
                "files", "fake.pdf", "application/pdf", "Not a real PDF content".getBytes(StandardCharsets.UTF_8));
        assertThrows(ApiException.class, () -> storageService.store(spoofedPdf));

        // 2. Executable double extension
        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        MockMultipartFile doubleExt = new MockMultipartFile(
                "files", "shell.php.png", "image/png", validPng);
        assertThrows(ApiException.class, () -> storageService.store(doubleExt));

        // 3. Path traversal in filename
        MockMultipartFile traversal = new MockMultipartFile(
                "files", "../../etc/passwd.png", "image/png", validPng);
        assertThrows(ApiException.class, () -> storageService.store(traversal));

        // 4. Windows MZ executable renamed to .pdf
        byte[] mzBytes = "MZ\u0090\u0000\u0003\u0000\u0000\u0000".getBytes(StandardCharsets.ISO_8859_1);
        MockMultipartFile exeAsPdf = new MockMultipartFile(
                "files", "malware.pdf", "application/pdf", mzBytes);
        assertThrows(ApiException.class, () -> storageService.store(exeAsPdf));

        // 5. Oversized file (> 1 MB configured in test)
        byte[] tooLarge = new byte[1024 * 1024 + 10];
        System.arraycopy(validPng, 0, tooLarge, 0, validPng.length);
        MockMultipartFile oversized = new MockMultipartFile(
                "files", "large.png", "image/png", tooLarge);
        assertThrows(ApiException.class, () -> storageService.store(oversized));

        // 6. Valid PNG stores with server-generated UUID filename and reads back identically
        MockMultipartFile goodPng = new MockMultipartFile(
                "files", "anh-minh-chung.png", "application/octet-stream", validPng);
        SupportAttachmentStorageService.StoredFileResult stored = storageService.store(goodPng);
        assertNotEquals("anh-minh-chung.png", stored.storedFileName());
        assertTrue(stored.storedFileName().endsWith(".png"));
        assertEquals("image/png", stored.contentType());
        assertArrayEquals(validPng, storageService.readStoredFile(stored.storedFileName()));
    }

    @Test
    void cleansUpOrphanFilesOnDiskWhenDatabasePersistenceFails() throws Exception {
        when(currentUser.get()).thenReturn(studentUserA);
        when(sinhVienRepository.findByUser(studentUserA)).thenReturn(Optional.of(sinhVienA));
        when(yeuCauRepository.existsByMaPhieu(any())).thenReturn(false);
        when(yeuCauRepository.saveAndFlush(any(SupportRequest.class)))
                .thenThrow(new DataAccessResourceFailureException("DB failure after storing file"));

        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02};
        MockMultipartFile pngFile = new MockMultipartFile(
                "files", "evidence.png", "image/png", validPng);

        SupportRequestDtos.TaoYeuCauRequest req = new SupportRequestDtos.TaoYeuCauRequest(
                "HOC_PHAN",
                "Lỗi đăng ký lớp học phần INT201",
                "Em gặp lỗi khi xác nhận đăng ký lớp học phần dù còn chỉ tiêu.",
                null,
                null,
                null
        );

        assertThrows(DataAccessResourceFailureException.class,
                () -> yeuCauHoTroService.taoYeuCau(req, List.of(pngFile)));

        try (Stream<Path> filesInStorage = Files.list(tempDir)) {
            assertEquals(0L, filesInStorage.count(), "File mồ côi phải được xóa khi giao dịch DB thất bại");
        }
    }

    @Test
    void statusTransitionsRecordActorAndTimestampAndPreserveOriginalContent() {
        SupportRequest ticket = new SupportRequest();
        ticket.setId(800L);
        ticket.setMaPhieu("HT-20261105-800800");
        ticket.setSinhVien(sinhVienA);
        ticket.setNguoiTao(studentUserA);
        ticket.setLoaiYeuCau(SupportRequestType.DIEM_SO);
        ticket.setTieuDe("Tiêu đề gốc không đổi");
        ticket.setNoiDungGoc("Nội dung gốc của sinh viên không bao giờ bị ghi đè.");
        ticket.setTrangThai(SupportRequestStatus.MOI_TAO);

        // Student cannot change status
        when(currentUser.get()).thenReturn(studentUserA);
        ApiException studentStatusEx = assertThrows(ApiException.class, () -> yeuCauHoTroService.doiTrangThai(
                800L, new SupportRequestDtos.DoiTrangThaiRequest("DA_GIAI_QUYET", "Tự duyệt")));
        assertEquals(HttpStatus.FORBIDDEN, studentStatusEx.getStatus());

        // Admin transitions MOI_TAO -> DANG_XU_LY
        when(currentUser.get()).thenReturn(adminUser);
        when(yeuCauRepository.findWithDetailsById(800L)).thenReturn(Optional.of(ticket));
        when(yeuCauRepository.saveAndFlush(any(SupportRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lichSuTrangThaiRepository.saveAndFlush(any(SupportRequestStatusHistory.class))).thenAnswer(inv -> {
            SupportRequestStatusHistory h = inv.getArgument(0);
            h.setId(801L);
            return h;
        });
        when(tepDinhKemRepository.findByYeuCauIdOrderByIdAsc(800L)).thenReturn(List.of());
        when(traoDoiRepository.findByYeuCauIdOrderByCreatedAtAscIdAsc(800L)).thenReturn(List.of());
        when(lichSuTrangThaiRepository.findByYeuCauIdOrderByThoiDiemAscIdAsc(800L)).thenAnswer(inv -> {
            SupportRequestStatusHistory h = new SupportRequestStatusHistory();
            h.setId(801L);
            h.setYeuCau(ticket);
            h.setTrangThaiCu(SupportRequestStatus.MOI_TAO);
            h.setTrangThaiMoi(SupportRequestStatus.DANG_XU_LY);
            h.setNguoiThucHien(adminUser);
            h.setTenNguoiThucHien("admin");
            h.setVaiTroNguoiThucHien(Role.QUAN_TRI);
            h.setGhiChu("Đã tiếp nhận và chuyển bộ phận khảo thí kiểm tra");
            return List.of(h);
        });

        SupportRequestDtos.YeuCauChiTietDto updated = yeuCauHoTroService.doiTrangThai(
                800L, new SupportRequestDtos.DoiTrangThaiRequest("DANG_XU_LY", "Đã tiếp nhận và chuyển bộ phận khảo thí kiểm tra"));

        assertEquals("DANG_XU_LY", updated.trangThai());
        assertEquals("Tiêu đề gốc không đổi", updated.tieuDe());
        assertEquals("Nội dung gốc của sinh viên không bao giờ bị ghi đè.", updated.noiDungGoc());
        assertEquals(1, updated.lichSuTrangThai().size());
        assertEquals("MOI_TAO", updated.lichSuTrangThai().get(0).trangThaiCu());
        assertEquals("DANG_XU_LY", updated.lichSuTrangThai().get(0).trangThaiMoi());
        assertEquals(99L, updated.lichSuTrangThai().get(0).nguoiThucHienId());
        assertEquals("admin", updated.lichSuTrangThai().get(0).tenNguoiThucHien());
        assertNotNull(updated.lichSuTrangThai().get(0).thoiDiem());

        // Student adds comment while in DANG_XU_LY -> stored separately, original content untouched
        when(currentUser.get()).thenReturn(studentUserA);
        when(traoDoiRepository.saveAndFlush(any(SupportRequestComment.class))).thenAnswer(inv -> {
            SupportRequestComment td = inv.getArgument(0);
            td.setId(850L);
            return td;
        });

        SupportRequestDtos.TraoDoiDto comment = yeuCauHoTroService.themTraoDoi(
                800L, new SupportRequestDtos.ThemTraoDoiRequest("Em bổ sung thêm thông tin ngày thi."));
        assertEquals(850L, comment.id());
        assertEquals("SINH_VIEN", comment.vaiTroNguoiGui());
        assertEquals("Em bổ sung thêm thông tin ngày thi.", comment.noiDung());
        assertEquals("Nội dung gốc của sinh viên không bao giờ bị ghi đè.", ticket.getNoiDungGoc());

        // Once closed (DA_DONG), neither comments nor further transitions are allowed
        ticket.setTrangThai(SupportRequestStatus.DA_DONG);
        assertThrows(ApiException.class, () -> yeuCauHoTroService.themTraoDoi(
                800L, new SupportRequestDtos.ThemTraoDoiRequest("Bình luận sau khi đóng")));

        when(currentUser.get()).thenReturn(adminUser);
        assertThrows(ApiException.class, () -> yeuCauHoTroService.doiTrangThai(
                800L, new SupportRequestDtos.DoiTrangThaiRequest("DANG_XU_LY", "Mở lại phiếu đã đóng")));
    }
}
