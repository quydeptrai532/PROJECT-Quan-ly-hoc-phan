package com.edu.registration.service;

import com.edu.registration.config.VerifiableTranscriptConfigProperties;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.VerifiableTranscriptDtos;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.entity.VerifiableTranscript;
import com.edu.registration.entity.VerifiableTranscriptStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.VerifiableTranscriptRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.util.VerifiableCodeSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifiableTranscriptServiceTest {

    @Mock
    private VerifiableTranscriptRepository repository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private StudentTranscriptPdfService studentTranscriptPdfService;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditLogService auditLogService;

    private VerifiableTranscriptConfigProperties properties;
    private ObjectMapper objectMapper;
    private Clock fixedClock;
    private VerifiableTranscriptService service;

    private User studentUser;
    private User adminUser;
    private Student student;
    private RegistrationDtos.BangDiemPdfDto dummyBangDiemDto;

    @BeforeEach
    void setUp() {
        properties = new VerifiableTranscriptConfigProperties();
        properties.setVerificationBaseUrl("https://qldt.university.edu.vn");
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        fixedClock = Clock.fixed(Instant.parse("2026-11-10T02:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        service = new VerifiableTranscriptService(
                repository, studentRepository, semesterRepository, studentTranscriptPdfService,
                currentUser, auditLogService, properties, objectMapper, fixedClock);

        studentUser = new User();
        studentUser.setId(10L);
        studentUser.setUsername("sv001");
        studentUser.setRole(Role.SINH_VIEN);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.QUAN_TRI);

        student = new Student();
        student.setId(100L);
        student.setMa("SV001");
        student.setHoTen("Nguyễn Văn An");
        student.setUser(studentUser);

        RegistrationDtos.ThongTinSinhVienPdfDto thongTinSv = new RegistrationDtos.ThongTinSinhVienPdfDto(
                "SV001", "Nguyễn Văn An", "an.nv@student.edu.vn", "CNTT (Công nghệ thông tin)",
                "KTPM (Kỹ thuật phần mềm)", "CNPM01", "K2024", "2024.1");
        dummyBangDiemDto = new RegistrationDtos.BangDiemPdfDto(
                thongTinSv, "Trường Đại học Công nghệ", LocalDateTime.now(fixedClock),
                List.of(), 5, 15, 15, 8.2, 3.4, false);
    }

    @Test
    void testPhatHanhBangDiemChoSinhVienHienTai() {
        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findWithAcademicProfileByUserId(10L)).thenReturn(Optional.of(student));
        when(studentTranscriptPdfService.layDuLieuBangDiem(null)).thenReturn(dummyBangDiemDto);
        when(repository.findFirstBySinhVienIdAndHocKyIsNullOrderByPhienBanDesc(100L)).thenReturn(Optional.empty());
        when(studentTranscriptPdfService.taoFilePdf(any(), any(), any(), eq(1)))
                .thenReturn(new byte[]{1, 2, 3, 4, 5});
        when(repository.save(any(VerifiableTranscript.class))).thenAnswer(invocation -> {
            VerifiableTranscript vt = invocation.getArgument(0);
            vt.setId(500L);
            return vt;
        });

        StudentTranscriptPdfService.PdfFile result = service.xuatBangDiemDienTuSinhVien(null);

        assertNotNull(result);
        assertEquals(1, result.phienBan());
        assertNotNull(result.maXacMinh());
        assertTrue(result.maXacMinh().startsWith("VT-"));
        assertTrue(result.verificationUrl().startsWith("https://qldt.university.edu.vn/xac-thuc-bang-diem?code=VT-"));
        assertEquals("bang-diem-xac-thuc-SV001-v1.pdf", result.tenTep());

        verify(repository).save(any(VerifiableTranscript.class));
    }

    @Test
    void testXacThucCongKhai_HopLe() throws Exception {
        String rawCode = "VT-ABCD-EFGH-2345-6789-JKLM-NPQR-STUV-WXYZ";
        String normalized = VerifiableCodeSupport.normalizeCode(rawCode);
        String codeHash = VerifiableCodeSupport.sha256Hex(normalized);

        VerifiableTranscript vt = new VerifiableTranscript();
        vt.setId(10L);
        vt.setMaXacMinhHash(codeHash);
        vt.setMaXacMinhMasked(VerifiableCodeSupport.maskCode(rawCode));
        vt.setMaSinhVien("SV001");
        vt.setPhienBan(1);
        vt.setTrangThai(VerifiableTranscriptStatus.HIEU_LUC);
        vt.setFileSha256("abc123sha256");
        vt.setSoMonDaCongBo(5);
        vt.setTongTinChiCoDiem(15);
        vt.setTongTinChiTichLuy(15);
        vt.setGpa10(8.2);
        vt.setGpa4(3.4);
        vt.setThoiDiemPhatHanh(LocalDateTime.now(fixedClock));
        vt.setSnapshotJson(objectMapper.writeValueAsString(dummyBangDiemDto));

        when(repository.findByMaXacMinhHash(codeHash)).thenReturn(Optional.of(vt));

        VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto result = service.xacThucCongKhai(rawCode);

        assertEquals("HOP_LE", result.trangThaiXacMinh());
        assertEquals("SV001", result.maSinhVien());
        assertEquals("Nguyễn Văn An", result.hoTen());
        assertEquals(8.2, result.gpa10());
        assertEquals(3.4, result.gpa4());
        assertEquals(15, result.tongTinChiTichLuy());
        assertEquals(1, result.phienBan());
        assertEquals("abc123sha256", result.fileSha256());
        assertNotNull(result.chinhSachBaoMat());
    }

    @Test
    void testXacThucCongKhai_KhongTonTai_KhongTietLoThongTin() {
        when(repository.findByMaXacMinhHash(any())).thenReturn(Optional.empty());

        VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto result =
                service.xacThucCongKhai("VT-FAKE-CODE-1234-5678-9012-3456-7890-ABCD");

        assertEquals("KHONG_HOP_LE", result.trangThaiXacMinh());
        assertNull(result.maSinhVien(), "Tuyệt đối không tiết lộ mã sinh viên");
        assertNull(result.hoTen(), "Tuyệt đối không tiết lộ họ tên");
        assertNull(result.gpa10(), "Tuyệt đối không tiết lộ điểm");
    }

    @Test
    void testXacThucCongKhai_DaThuHoi_KhongTietLoThongTinCaNhan() {
        String rawCode = "VT-ABCD-EFGH-2345-6789-JKLM-NPQR-STUV-WXYZ";
        String normalized = VerifiableCodeSupport.normalizeCode(rawCode);
        String codeHash = VerifiableCodeSupport.sha256Hex(normalized);

        VerifiableTranscript vt = new VerifiableTranscript();
        vt.setId(10L);
        vt.setMaXacMinhHash(codeHash);
        vt.setTrangThai(VerifiableTranscriptStatus.DA_THU_HOI);
        vt.setThoiDiemThuHoi(LocalDateTime.now(fixedClock));
        vt.setLyDoThuHoi("Đính chính điểm theo quyết định phúc khảo");
        vt.setPhienBan(1);

        when(repository.findByMaXacMinhHash(codeHash)).thenReturn(Optional.of(vt));

        VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto result = service.xacThucCongKhai(rawCode);

        assertEquals("DA_THU_HOI", result.trangThaiXacMinh());
        assertEquals(1, result.phienBan());
        assertEquals("Đính chính điểm theo quyết định phúc khảo", result.lyDoThuHoi());
        assertNull(result.maSinhVien(), "Không tiết lộ mã sinh viên khi đã thu hồi");
        assertNull(result.hoTen(), "Không tiết lộ họ tên khi đã thu hồi");
        assertNull(result.gpa10(), "Không tiết lộ điểm khi đã thu hồi");
    }

    @Test
    void testXacThucCongKhai_DaThayThe_KhongTietLoThongTinCaNhan() {
        String rawCode = "VT-ABCD-EFGH-2345-6789-JKLM-NPQR-STUV-WXYZ";
        String normalized = VerifiableCodeSupport.normalizeCode(rawCode);
        String codeHash = VerifiableCodeSupport.sha256Hex(normalized);

        VerifiableTranscript vt = new VerifiableTranscript();
        vt.setId(10L);
        vt.setMaXacMinhHash(codeHash);
        vt.setTrangThai(VerifiableTranscriptStatus.DA_THAY_THE);
        vt.setPhienBan(1);

        when(repository.findByMaXacMinhHash(codeHash)).thenReturn(Optional.of(vt));

        VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto result = service.xacThucCongKhai(rawCode);

        assertEquals("DA_THAY_THE", result.trangThaiXacMinh());
        assertEquals(1, result.phienBan());
        assertNull(result.maSinhVien());
        assertNull(result.hoTen());
        assertNull(result.gpa10());
    }

    @Test
    void testThuHoiBangDiem_ThanhCong() {
        when(currentUser.get()).thenReturn(adminUser);

        VerifiableTranscript vt = new VerifiableTranscript();
        vt.setId(20L);
        vt.setMaSinhVien("SV001");
        vt.setPhienBan(1);
        vt.setMaXacMinhMasked("VT-****-WXYZ");
        vt.setTrangThai(VerifiableTranscriptStatus.HIEU_LUC);

        when(repository.findById(20L)).thenReturn(Optional.of(vt));
        when(repository.save(any(VerifiableTranscript.class))).thenAnswer(i -> i.getArgument(0));

        VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto result =
                service.thuHoiBangDiem(20L, "Phát hiện sai sót học phần");

        assertEquals(VerifiableTranscriptStatus.DA_THU_HOI, result.trangThai());
        assertEquals("Phát hiện sai sót học phần", result.lyDoThuHoi());
        assertEquals("admin", result.nguoiThuHoiUsername());
    }

    @Test
    void testThuHoiBangDiem_KhongPhaiQuanTri_Chan403() {
        when(currentUser.get()).thenReturn(studentUser);

        ApiException ex = assertThrows(ApiException.class, () ->
                service.thuHoiBangDiem(20L, "Thu hồi"));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void testThuHoiBangDiem_LyDoRong_Chan400() {
        when(currentUser.get()).thenReturn(adminUser);

        ApiException ex = assertThrows(ApiException.class, () ->
                service.thuHoiBangDiem(20L, "   "));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void testCapLaiBangDiem_ThanhCong() throws Exception {
        when(currentUser.get()).thenReturn(adminUser);

        VerifiableTranscript old = new VerifiableTranscript();
        old.setId(20L);
        old.setSinhVien(student);
        old.setMaSinhVien("SV001");
        old.setPhienBan(1);
        old.setTrangThai(VerifiableTranscriptStatus.HIEU_LUC);
        old.setSnapshotJson(objectMapper.writeValueAsString(dummyBangDiemDto));

        when(repository.findById(20L)).thenReturn(Optional.of(old));
        when(studentTranscriptPdfService.taoFilePdf(any(), any(), any(), eq(2)))
                .thenReturn(new byte[]{5, 6, 7, 8});
        when(repository.save(any(VerifiableTranscript.class))).thenAnswer(i -> {
            VerifiableTranscript vt = i.getArgument(0);
            if (vt.getId() == null) {
                vt.setId(21L);
            }
            return vt;
        });

        StudentTranscriptPdfService.PdfFile result = service.capLaiBangDiem(20L, "Cập nhật điểm phúc khảo");

        assertNotNull(result);
        assertEquals(2, result.phienBan());
        assertEquals(VerifiableTranscriptStatus.DA_THAY_THE, old.getTrangThai());
        assertEquals(21L, old.getBanGhiThayTheId());
    }
}
