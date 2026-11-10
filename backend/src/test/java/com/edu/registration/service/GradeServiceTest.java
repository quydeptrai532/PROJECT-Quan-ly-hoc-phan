package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeSheetStateHistory;
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
import com.edu.registration.repository.GradeSheetStateHistoryRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private CourseRegistrationRepository dangKyHocPhanRepository;
    @Mock
    private GradeRepository diemRepository;
    @Mock
    private CourseClassRepository lopHocPhanRepository;
    @Mock
    private StudentRepository sinhVienRepository;
    @Mock
    private LecturerRepository giangVienRepository;
    @Mock
    private SemesterRepository hocKyRepository;
    @Mock
    private GradeSheetStateHistoryRepository lichSuTrangThaiRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private DtoMapper mapper;
    @Mock
    private NotificationService thongBaoService;

    private GradeService diemService;

    private User user;
    private User adminUser;
    private Lecturer giangVien;
    private Student sinhVien;
    private CourseClass lopHocPhan;
    private CourseRegistration dangKy;

    @BeforeEach
    void setUp() {
        diemService = new GradeService(
                dangKyHocPhanRepository,
                diemRepository,
                lopHocPhanRepository,
                sinhVienRepository,
                giangVienRepository,
                hocKyRepository,
                currentUser,
                mapper,
                null,
                thongBaoService,
                lichSuTrangThaiRepository
        );

        user = new User();
        user.setId(1L);
        user.setUsername("gv01");
        user.setRole(Role.GIANG_VIEN);

        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.QUAN_TRI);

        giangVien = new Lecturer();
        giangVien.setId(7L);
        giangVien.setUser(user);
        giangVien.setMa("GV01");
        giangVien.setHoTen("TS. Nguyễn Văn An");

        Semester hocKy = new Semester();
        hocKy.setId(1L);
        hocKy.setTen("Học kỳ 1");
        hocKy.setNamHoc("2025-2026");
        hocKy.setNgayBatDau(LocalDate.of(2025, 9, 1));

        Subject monHoc = new Subject();
        monHoc.setId(10L);
        monHoc.setMa("INT201");
        monHoc.setTen("Cấu trúc dữ liệu");
        monHoc.setSoTinChi(3);

        lopHocPhan = new CourseClass();
        lopHocPhan.setId(100L);
        lopHocPhan.setMa("INT201.01");
        lopHocPhan.setMonHoc(monHoc);
        lopHocPhan.setHocKy(hocKy);
        lopHocPhan.setGiangVien(giangVien);

        sinhVien = new Student();
        sinhVien.setId(1L);
        sinhVien.setMa("SV001");
        sinhVien.setHoTen("Trần Minh Anh");

        dangKy = new CourseRegistration();
        dangKy.setId(300L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(RegistrationStatus.DA_DANG_KY);
    }

    private void dangNhapVoiGiangVien() {
        lenient().when(currentUser.get()).thenReturn(user);
        lenient().when(giangVienRepository.findByUser(user)).thenReturn(Optional.of(giangVien));
    }

    private void dangNhapVoiQuanTri() {
        lenient().when(currentUser.get()).thenReturn(adminUser);
    }

    private void dangNhapVoiSinhVien() {
        lenient().when(currentUser.get()).thenReturn(user);
        lenient().when(sinhVienRepository.findByUser(user)).thenReturn(Optional.of(sinhVien));
    }

    @Test
    void nhapDiemTinhDiemTongKetTheoTrongSoVaGiuTrangThaiNhapKhongPhatThongBao() {
        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));
        when(diemRepository.findByDangKyId(300L)).thenReturn(Optional.empty());
        when(diemRepository.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.0, 9.0));

        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        verify(diemRepository).save(captor.capture());
        assertEquals(8.6, captor.getValue().getDiemTongKet());
        assertEquals(GradeSheetStatus.NHAP, lopHocPhan.getTrangThaiBangDiem());
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());
    }

    @Test
    void khongTheCongBoKhiBangDiemConNhapHoacChoDuyetVaChiThongBaoKhiDaDuyetSangDaCongBo() {
        lopHocPhan.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));

        // 1. Admin cannot publish while in NHAP
        dangNhapVoiQuanTri();
        ApiException draftEx = assertThrows(ApiException.class, () -> diemService.congBoBangDiem(100L));
        assertTrue(draftEx.getMessage().contains("cần được phê duyệt trước khi công bố"));
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());

        // 2. Lecturer submits for approval -> CHO_DUYET (no notification)
        dangNhapVoiGiangVien();
        Grade diem = new Grade();
        diem.setDangKy(dangKy);
        diem.setDiemQuaTrinh(8.0);
        diem.setDiemCuoiKy(9.0);
        diem.setDiemTongKet(8.6);
        when(dangKyHocPhanRepository.findByLopHocPhan(100L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dangKy));
        when(diemRepository.findByDangKyIdIn(List.of(300L))).thenReturn(List.of(diem));

        RegistrationDtos.TrangThaiBangDiemDto submitted = diemService.guiDuyetBangDiem(100L);
        assertEquals("CHO_DUYET", submitted.trangThaiBangDiem());
        assertFalse(submitted.choPhepSuaDiem());
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());

        // 3. Admin cannot publish while in CHO_DUYET
        dangNhapVoiQuanTri();
        ApiException pendingEx = assertThrows(ApiException.class, () -> diemService.congBoBangDiem(100L));
        assertTrue(pendingEx.getMessage().contains("cần được phê duyệt trước khi công bố"));
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());

        // 4. Admin approves -> DA_DUYET (still no notification)
        RegistrationDtos.TrangThaiBangDiemDto approved = diemService.pheDuyetBangDiem(100L);
        assertEquals("DA_DUYET", approved.trangThaiBangDiem());
        verify(thongBaoService, never()).gradesPublished(any(), any(), anyInt(), any());

        // 5. Admin publishes after approval -> DA_CONG_BO (emits notification with version 2)
        RegistrationDtos.TrangThaiBangDiemDto published = diemService.congBoBangDiem(100L);

        assertEquals("DA_CONG_BO", published.trangThaiBangDiem());
        assertEquals(2, published.phienBanCongBo());
        assertTrue(published.daCongBo());

        verify(thongBaoService, times(1)).gradesPublished(
                eq(lopHocPhan),
                eq(List.of(dangKy)),
                eq(2),
                any()
        );
    }

    @Test
    void luongTraVeChinhSuaSuaDiemGuiDuyetLaiKhoaVaMoKhoaCoLyDoBatBuoc() {
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));
        Grade diem = new Grade();
        diem.setId(500L);
        diem.setDangKy(dangKy);
        diem.setDiemQuaTrinh(7.0);
        diem.setDiemCuoiKy(8.0);
        diem.setDiemTongKet(7.6);
        when(dangKyHocPhanRepository.findByLopHocPhan(100L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dangKy));
        when(diemRepository.findByDangKyIdIn(List.of(300L))).thenReturn(List.of(diem));
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));
        when(diemRepository.findByDangKyId(300L)).thenReturn(Optional.of(diem));
        when(diemRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Giảng viên gửi duyệt từ NHAP -> CHO_DUYET
        dangNhapVoiGiangVien();
        RegistrationDtos.TrangThaiBangDiemDto s1 = diemService.guiDuyetBangDiem(100L);
        assertEquals("CHO_DUYET", s1.trangThaiBangDiem());
        assertEquals(1L, s1.nguoiGuiDuyetId());

        // Khi đang CHO_DUYET, giảng viên không được sửa điểm
        ApiException editWhilePending = assertThrows(ApiException.class, () ->
                diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.5, 9.0)));
        assertEquals(HttpStatus.CONFLICT, editWhilePending.getStatus());

        // 2. Phòng Đào tạo (QUAN_TRI) trả về không có lý do -> 400 Bad Request
        dangNhapVoiQuanTri();
        ApiException missingReason = assertThrows(ApiException.class, () ->
                diemService.traVeBangDiem(100L, "   "));
        assertEquals(HttpStatus.BAD_REQUEST, missingReason.getStatus());

        // Trả về có lý do hợp lệ -> TRA_VE
        RegistrationDtos.TrangThaiBangDiemDto s2 = diemService.traVeBangDiem(
                100L, "Cần đối chiếu lại điểm quá trình SV001");
        assertEquals("TRA_VE", s2.trangThaiBangDiem());
        assertEquals("Cần đối chiếu lại điểm quá trình SV001", s2.lyDoTraVe());
        assertEquals(99L, s2.nguoiTraVeId());
        assertTrue(s2.choPhepSuaDiem());

        // 3. Giảng viên sửa điểm ở trạng thái TRA_VE và gửi duyệt lại -> CHO_DUYET
        dangNhapVoiGiangVien();
        diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.5, 9.0));
        assertEquals(GradeSheetStatus.TRA_VE, lopHocPhan.getTrangThaiBangDiem());
        assertEquals(8.8, diem.getDiemTongKet());

        RegistrationDtos.TrangThaiBangDiemDto s3 = diemService.guiDuyetBangDiem(100L);
        assertEquals("CHO_DUYET", s3.trangThaiBangDiem());

        // 4. Phòng Đào tạo duyệt -> công bố -> khóa bảng điểm (DA_KHOA)
        dangNhapVoiQuanTri();
        diemService.pheDuyetBangDiem(100L);
        diemService.congBoBangDiem(100L);
        RegistrationDtos.TrangThaiBangDiemDto sLocked = diemService.khoaBangDiem(
                100L, "Chốt sổ điểm cuối học kỳ");
        assertEquals("DA_KHOA", sLocked.trangThaiBangDiem());
        assertTrue(sLocked.daCongBo());
        assertTrue(sLocked.daKhoa());
        assertFalse(sLocked.choPhepSuaDiem());

        // Sau khi khóa: mọi thao tác sửa điểm hoặc gửi duyệt/công bố lại đều bị chặn
        dangNhapVoiGiangVien();
        ApiException editLocked = assertThrows(ApiException.class, () ->
                diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(9.0, 9.0)));
        assertEquals(HttpStatus.CONFLICT, editLocked.getStatus());
        assertTrue(editLocked.getMessage().contains("đã bị khóa"));

        // 5. Mở khóa bắt buộc có lý do và quyền QUAN_TRI
        ApiException lecturerUnlock = assertThrows(ApiException.class, () ->
                diemService.moKhoaBangDiem(100L, "Tự mở khóa"));
        assertEquals(HttpStatus.FORBIDDEN, lecturerUnlock.getStatus());

        dangNhapVoiQuanTri();
        ApiException emptyUnlockReason = assertThrows(ApiException.class, () ->
                diemService.moKhoaBangDiem(100L, ""));
        assertEquals(HttpStatus.BAD_REQUEST, emptyUnlockReason.getStatus());

        RegistrationDtos.TrangThaiBangDiemDto sUnlocked = diemService.moKhoaBangDiem(
                100L, "Phúc khảo điểm cuối kỳ theo quyết định số 12/QĐ-ĐàoTạo");
        assertEquals("TRA_VE", sUnlocked.trangThaiBangDiem());
        assertEquals("Phúc khảo điểm cuối kỳ theo quyết định số 12/QĐ-ĐàoTạo", sUnlocked.lyDoMoKhoa());
        assertTrue(sUnlocked.choPhepSuaDiem());
        assertFalse(sUnlocked.daKhoa());

        // Kiểm tra tất cả các lần chuyển trạng thái đều lưu lịch sử vào lichSuTrangThaiRepository
        verify(lichSuTrangThaiRepository, times(7)).save(any(GradeSheetStateHistory.class));
    }

    @Test
    void chanChuyenTrangThaiTraiPhepTheoVaiTroOBackend() {
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));

        // 1. Quản trị viên không được phép gọi guiDuyetBangDiem (chỉ giảng viên phụ trách mới được gửi duyệt)
        dangNhapVoiQuanTri();
        ApiException adminSubmit = assertThrows(ApiException.class, () -> diemService.guiDuyetBangDiem(100L));
        assertEquals(HttpStatus.FORBIDDEN, adminSubmit.getStatus());

        // 2. Giảng viên không được phép gọi pheDuyetBangDiem, traVeBangDiem, congBoBangDiem, khoaBangDiem, moKhoaBangDiem
        lopHocPhan.setTrangThaiBangDiem(GradeSheetStatus.CHO_DUYET);
        dangNhapVoiGiangVien();

        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> diemService.pheDuyetBangDiem(100L)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> diemService.traVeBangDiem(100L, "Lý do")).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> diemService.congBoBangDiem(100L)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> diemService.khoaBangDiem(100L, null)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> diemService.moKhoaBangDiem(100L, "Lý do")).getStatus());
    }

    @Test
    void khongTheNhapDiemChoLopKhongPhuTrach() {
        Lecturer nguoiKhac = new Lecturer();
        nguoiKhac.setId(8L);
        nguoiKhac.setHoTen("ThS. Trần Thị Bình");
        lopHocPhan.setGiangVien(nguoiKhac);

        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));

        ApiException ex = assertThrows(ApiException.class, () -> diemService.nhapDiem(
                300L, new RegistrationDtos.NhapDiemRequest(8.0, 9.0)));

        assertEquals("Bạn không phụ trách lớp học phần này", ex.getMessage());
        verify(diemRepository, never()).save(any());
    }

    @Test
    void lichSuHocTapBaoToanNhieuLanHocVaAnDiemChuaCongBo() {
        dangNhapVoiSinhVien();

        Semester hk1 = new Semester();
        hk1.setId(1L);
        hk1.setTen("Học kỳ 1");
        hk1.setNamHoc("2025-2026");
        hk1.setNgayBatDau(LocalDate.of(2025, 9, 1));

        Semester hk2 = new Semester();
        hk2.setId(2L);
        hk2.setTen("Học kỳ 2");
        hk2.setNamHoc("2025-2026");
        hk2.setNgayBatDau(LocalDate.of(2026, 2, 1));

        Semester hk3 = new Semester();
        hk3.setId(3L);
        hk3.setTen("Học kỳ 1");
        hk3.setNamHoc("2026-2027");
        hk3.setNgayBatDau(LocalDate.of(2026, 9, 1));

        Subject int101 = new Subject();
        int101.setId(11L);
        int101.setMa("INT101");
        int101.setTen("Lập trình cơ bản");
        int101.setSoTinChi(3);

        Subject mat101 = new Subject();
        mat101.setId(12L);
        mat101.setMa("MAT101");
        mat101.setTen("Toán rời rạc");
        mat101.setSoTinChi(3);

        CourseClass lopLan1 = taoLop(201L, "INT101.01-HK1", int101, hk1);
        CourseClass lopLan2 = taoLop(202L, "INT101.01-HK2", int101, hk2);
        lopLan2.setTrangThaiBangDiem(GradeSheetStatus.DA_KHOA); // Đã khóa vẫn hiển thị điểm cho sinh viên
        CourseClass lopChuaCongBo = taoLop(203L, "MAT101.01-HK3", mat101, hk3);
        lopChuaCongBo.setTrangThaiBangDiem(GradeSheetStatus.CHO_DUYET);

        CourseRegistration dkLan1 = taoDangKy(401L, lopLan1, LocalDateTime.of(2025, 8, 20, 8, 0));
        CourseRegistration dkLan2 = taoDangKy(402L, lopLan2, LocalDateTime.of(2026, 1, 20, 8, 0));
        CourseRegistration dkChuaCongBo = taoDangKy(403L, lopChuaCongBo, LocalDateTime.of(2026, 8, 20, 8, 0));

        Grade diemTruot = new Grade();
        diemTruot.setDangKy(dkLan1);
        diemTruot.setDiemQuaTrinh(4.0);
        diemTruot.setDiemCuoiKy(4.0);
        diemTruot.setDiemTongKet(4.0);

        Grade diemDat = new Grade();
        diemDat.setDangKy(dkLan2);
        diemDat.setDiemQuaTrinh(7.5);
        diemDat.setDiemCuoiKy(8.0);
        diemDat.setDiemTongKet(7.8);

        Grade diemNhapChuaCongBo = new Grade();
        diemNhapChuaCongBo.setDangKy(dkChuaCongBo);
        diemNhapChuaCongBo.setDiemQuaTrinh(9.0);
        diemNhapChuaCongBo.setDiemCuoiKy(8.5);
        diemNhapChuaCongBo.setDiemTongKet(8.7);

        when(dangKyHocPhanRepository.findAllActiveOfStudent(1L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dkChuaCongBo, dkLan2, dkLan1));
        when(diemRepository.findByDangKyIdIn(any()))
                .thenReturn(List.of(diemTruot, diemDat, diemNhapChuaCongBo));

        RegistrationDtos.LichSuHocTapDto ketQua = diemService.lichSuHocTap(null, null);

        assertEquals(3, ketQua.items().size());
        assertTrue(ketQua.coNhieuLanHoc());
        assertEquals(1, ketQua.soMonDat());
        assertEquals(1, ketQua.soMonChuaDat());
        assertEquals(1, ketQua.soMonChuaCoDiem());
        assertEquals(1, ketQua.monChuaDat().size());

        RegistrationDtos.MucLichSuHocTapDto muc1 = ketQua.items().get(0);
        assertEquals(401L, muc1.dangKyId());
        assertEquals(1, muc1.lanHoc());
        assertEquals(2, muc1.tongSoLanHoc());
        assertEquals("CHUA_DAT", muc1.trangThaiKetQua());
        assertEquals("Chưa đạt", muc1.tenTrangThaiKetQua());
        assertEquals(4.0, muc1.diemTongKet());

        RegistrationDtos.MucLichSuHocTapDto muc2 = ketQua.items().get(1);
        assertEquals(402L, muc2.dangKyId());
        assertEquals(2, muc2.lanHoc());
        assertEquals(2, muc2.tongSoLanHoc());
        assertEquals("DAT", muc2.trangThaiKetQua());
        assertEquals("Đạt", muc2.tenTrangThaiKetQua());
        assertEquals(7.8, muc2.diemTongKet());

        RegistrationDtos.MucLichSuHocTapDto muc3 = ketQua.items().get(2);
        assertEquals(403L, muc3.dangKyId());
        assertEquals(1, muc3.lanHoc());
        assertEquals(1, muc3.tongSoLanHoc());
        assertEquals("CHUA_CO_DIEM", muc3.trangThaiKetQua());
        assertEquals("Chưa có điểm", muc3.tenTrangThaiKetQua());
        assertNull(muc3.diemQuaTrinh());
        assertNull(muc3.diemCuoiKy());
        assertNull(muc3.diemTongKet());

        // Filter by HK2 and status DAT still preserves lanHoc = 2 for the retake record
        when(hocKyRepository.existsById(2L)).thenReturn(true);
        RegistrationDtos.LichSuHocTapDto locHk2 = diemService.lichSuHocTap(2L, "DAT");
        assertEquals(1, locHk2.items().size());
        assertEquals(2, locHk2.items().get(0).lanHoc());
        assertEquals(2, locHk2.items().get(0).tongSoLanHoc());
    }

    @Test
    void lichSuHocTapTuChoiThamSoLocKhongHopLe() {
        ApiException invalidStatus = assertThrows(ApiException.class,
                () -> diemService.lichSuHocTap(null, "KHONG_HOP_LE"));
        assertTrue(invalidStatus.getMessage().contains("Trạng thái kết quả không hợp lệ"));

        ApiException negativeSemester = assertThrows(ApiException.class,
                () -> diemService.lichSuHocTap(0L, null));
        assertEquals("Học kỳ không hợp lệ", negativeSemester.getMessage());

        when(hocKyRepository.existsById(999L)).thenReturn(false);
        ApiException missingSemester = assertThrows(ApiException.class,
                () -> diemService.lichSuHocTap(999L, null));
        assertEquals("Học kỳ không tồn tại", missingSemester.getMessage());
    }

    @Test
    void nhapDiemTuChoiDiemNgoaiMienGiaTriVaTraVeChiTietCot() {
        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));

        // Quá trình < 0
        ApiException ex1 = assertThrows(ApiException.class, () ->
                diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(-1.0, 8.0)));
        assertEquals("Điểm phải từ 0 đến 10", ex1.getMessage());
        assertEquals(1, ex1.getErrors().size());
        assertEquals("diemQuaTrinh", ex1.getErrors().get(0).cot());
        assertEquals(300L, ex1.getErrors().get(0).dangKyId());

        // Cuối kỳ > 10
        ApiException ex2 = assertThrows(ApiException.class, () ->
                diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.0, 10.5)));
        assertEquals("Điểm phải từ 0 đến 10", ex2.getMessage());
        assertEquals(1, ex2.getErrors().size());
        assertEquals("diemCuoiKy", ex2.getErrors().get(0).cot());

        verify(diemRepository, never()).save(any());
    }

    @Test
    void nhapDiemTuChoiDiemQuaNhieuChuSoThapPhan() {
        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));

        ApiException ex = assertThrows(ApiException.class, () ->
                diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.125, 9.0)));
        assertEquals("Điểm chỉ được có tối đa 2 chữ số thập phân", ex.getMessage());
        assertEquals(1, ex.getErrors().size());
        assertEquals("diemQuaTrinh", ex.getErrors().get(0).cot());
        verify(diemRepository, never()).save(any());
    }

    @Test
    void nhapDiemChoPhepGiaTriNullTrongTrangThaiNhapVaDiemTongKetNull() {
        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));
        when(diemRepository.findByDangKyId(300L)).thenReturn(Optional.empty());
        when(diemRepository.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Nhập mỗi điểm quá trình, điểm cuối kỳ null -> điểm tổng kết null
        diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(7.5, null));
        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        verify(diemRepository).save(captor.capture());
        assertEquals(7.5, captor.getValue().getDiemQuaTrinh());
        assertNull(captor.getValue().getDiemCuoiKy());
        assertNull(captor.getValue().getDiemTongKet());
    }

    @Test
    void nhapDiemTuChoiKhiBangDiemDaCongBoHoacDaKhoa() {
        dangNhapVoiGiangVien();
        when(dangKyHocPhanRepository.findById(300L)).thenReturn(Optional.of(dangKy));

        lopHocPhan.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);
        ApiException ex1 = assertThrows(ApiException.class, () ->
                diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.0, 9.0)));
        assertTrue(ex1.getMessage().contains("đã được công bố"));

        lopHocPhan.setTrangThaiBangDiem(GradeSheetStatus.DA_KHOA);
        ApiException ex2 = assertThrows(ApiException.class, () ->
                diemService.nhapDiem(300L, new RegistrationDtos.NhapDiemRequest(8.0, 9.0)));
        assertTrue(ex2.getMessage().contains("đã bị khóa"));

        verify(diemRepository, never()).save(any());
    }

    @Test
    void capNhatDiemHangLoatThanhCongVaRollbackKhiCoLoi() {
        dangNhapVoiGiangVien();
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));

        CourseRegistration dk2 = new CourseRegistration();
        dk2.setId(301L);
        Student sv2 = new Student();
        sv2.setId(2L);
        sv2.setMa("SV002");
        sv2.setHoTen("Lê Hoàng Nam");
        dk2.setSinhVien(sv2);
        dk2.setLopHocPhan(lopHocPhan);
        dk2.setTrangThai(RegistrationStatus.DA_DANG_KY);

        when(dangKyHocPhanRepository.findByLopHocPhan(100L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dangKy, dk2));

        // 1. Trường hợp có lỗi validation ở dòng 2 -> rollback toàn bộ, không lưu dòng nào
        RegistrationDtos.CapNhatDiemHangLoatRequest badReq = new RegistrationDtos.CapNhatDiemHangLoatRequest(
                List.of(
                        new RegistrationDtos.CapNhatDiemItemRequest(300L, 8.5, 9.0),
                        new RegistrationDtos.CapNhatDiemItemRequest(301L, 7.0, 11.0)
                )
        );
        ApiException badEx = assertThrows(ApiException.class, () -> diemService.capNhatDiemHangLoat(100L, badReq));
        assertEquals(1, badEx.getErrors().size());
        assertEquals("diemCuoiKy", badEx.getErrors().get(0).cot());
        assertEquals(301L, badEx.getErrors().get(0).dangKyId());
        assertEquals(2, badEx.getErrors().get(0).dong());
        verify(diemRepository, never()).saveAll(any());

        // 2. Trường hợp hợp lệ -> lưu toàn bộ thành công
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of());
        RegistrationDtos.CapNhatDiemHangLoatRequest goodReq = new RegistrationDtos.CapNhatDiemHangLoatRequest(
                List.of(
                        new RegistrationDtos.CapNhatDiemItemRequest(300L, 8.0, 9.0),
                        new RegistrationDtos.CapNhatDiemItemRequest(301L, 7.5, 8.5)
                )
        );
        diemService.capNhatDiemHangLoat(100L, goodReq);
        verify(diemRepository).saveAll(any());
    }

    private CourseClass taoLop(Long id, String ma, Subject mon, Semester hocKy) {
        CourseClass lop = new CourseClass();
        lop.setId(id);
        lop.setMa(ma);
        lop.setMonHoc(mon);
        lop.setHocKy(hocKy);
        lop.setGiangVien(giangVien);
        return lop;
    }

    private CourseRegistration taoDangKy(Long id, CourseClass lop, LocalDateTime thoiGian) {
        CourseRegistration dk = new CourseRegistration();
        dk.setId(id);
        dk.setSinhVien(sinhVien);
        dk.setLopHocPhan(lop);
        dk.setThoiGianDangKy(thoiGian);
        dk.setTrangThai(RegistrationStatus.DA_DANG_KY);
        return dk;
    }
}
