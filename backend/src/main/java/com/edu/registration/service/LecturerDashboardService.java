package com.edu.registration.service;

import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lecturer self-service profile and teaching dashboard (QLSV-66).
 * Every query is scoped to the lecturer bound to the authenticated user; no lecturer id is
 * accepted from the client.
 */
@Service
@Transactional(readOnly = true)
public class LecturerDashboardService {

    private static final String CHUA_CAP_NHAT = "Chưa cập nhật";

    private final LecturerRepository giangVienRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final GradeRepository diemRepository;
    private final SemesterService hocKyService;
    private final CurrentUser currentUser;

    public LecturerDashboardService(LecturerRepository giangVienRepository,
                                     CourseClassRepository lopHocPhanRepository,
                                     CourseRegistrationRepository dangKyHocPhanRepository,
                                     GradeRepository diemRepository,
                                     SemesterService hocKyService,
                                     CurrentUser currentUser) {
        this.giangVienRepository = giangVienRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.hocKyService = hocKyService;
        this.currentUser = currentUser;
    }

    public LecturerDtos.LecturerProfileDto hoSo() {
        User user = currentUser.get();
        Lecturer gv = giangVienHienTai(user);
        Faculty khoa = gv.getKhoa();
        return new LecturerDtos.LecturerProfileDto(
                user.getUsername(), user.getRole().name(), gv.getMa(), gv.getHoTen(),
                gv.getEmail(), gv.getSoDienThoai(),
                khoa != null ? khoa.getMa() : CHUA_CAP_NHAT,
                khoa != null ? khoa.getTen() : CHUA_CAP_NHAT,
                user.isTrangThai(),
                AuthService.SELF_EDITABLE_PROFILE_FIELDS);
    }

    public LecturerDtos.LecturerDashboardDto dashboard(Long hocKyId) {
        Lecturer gv = giangVienHienTai(currentUser.get());
        Semester hocKy = hocKyId != null ? hocKyService.lay(hocKyId) : hocKyService.hocKyHienTai();
        if (hocKy == null) {
            return new LecturerDtos.LecturerDashboardDto(null,
                    new LecturerDtos.TongQuanDto(0, 0, 0, 0, 0, 0, 0, 0), List.of(), List.of(), List.of());
        }

        List<CourseClass> danhSachLop = lopHocPhanRepository
                .findByGiangVienIdAndHocKyIdOrderByMaAsc(gv.getId(), hocKy.getId());

        Map<Long, List<CourseRegistration>> dangKyTheoLop = new HashMap<>();
        List<Long> tatCaDangKyId = new ArrayList<>();
        for (CourseClass lop : danhSachLop) {
            List<CourseRegistration> dangKy = dangKyHocPhanRepository.findByLopHocPhan(lop.getId(), RegistrationStatus.DA_DANG_KY);
            dangKyTheoLop.put(lop.getId(), dangKy);
            dangKy.forEach(dk -> tatCaDangKyId.add(dk.getId()));
        }
        Map<Long, Grade> diemTheoDangKy = tatCaDangKyId.isEmpty() ? Map.of() : diemRepository
                .findByDangKyIdIn(tatCaDangKyId).stream()
                .collect(Collectors.toMap(d -> d.getDangKy().getId(), Function.identity(), (a, b) -> a));

        List<LecturerDtos.LopPhuTrachDto> lopPhuTrach = new ArrayList<>();
        List<LecturerDtos.CongViecDto> congViec = new ArrayList<>();
        int tongSinhVien = 0;
        int tongSucChua = 0;
        Map<GradeSheetStatus, Integer> demTrangThai = new HashMap<>();

        for (CourseClass lop : danhSachLop) {
            List<CourseRegistration> dangKy = dangKyTheoLop.getOrDefault(lop.getId(), List.of());
            int soDangHoc = dangKy.size();
            int soDaCoDiem = (int) dangKy.stream()
                    .filter(dk -> GradeService.laDiemDayDu(diemTheoDangKy.get(dk.getId())))
                    .count();
            GradeSheetStatus trangThai = lop.getTrangThaiBangDiemHieuLuc();

            tongSinhVien += lop.getSiSoHienTai();
            tongSucChua += lop.getSiSoToiDa();
            demTrangThai.merge(trangThai, 1, Integer::sum);

            lopPhuTrach.add(new LecturerDtos.LopPhuTrachDto(lop.getId(), lop.getMa(),
                    lop.getMonHoc().getMa(), lop.getMonHoc().getTen(), lop.getMonHoc().getSoTinChi(),
                    lop.getPhong(), lop.getThu(), lop.getTietBatDau(), lop.getSoTiet(),
                    lop.getSiSoHienTai(), lop.getSiSoToiDa(), trangThai.name(),
                    soDangHoc, soDaCoDiem, lop.getGuiDuyetBangDiemLuc()));

            LecturerDtos.CongViecDto viec = congViecCuaLop(lop, trangThai, soDangHoc, soDaCoDiem);
            if (viec != null) {
                congViec.add(viec);
            }
        }

        List<RegistrationDtos.ThoiKhoaBieuDto> lichDay = danhSachLop.stream()
                .sorted(Comparator.comparingInt(CourseClass::getThu).thenComparingInt(CourseClass::getTietBatDau))
                .map(lop -> new RegistrationDtos.ThoiKhoaBieuDto(lop.getId(), lop.getMa(),
                        lop.getMonHoc().getTen(), lop.getPhong(), lop.getThu(), lop.getTietBatDau(),
                        lop.getSoTiet(), gv.getHoTen()))
                .toList();

        int soBangDiemNhapHoacTraVe = demTrangThai.getOrDefault(GradeSheetStatus.NHAP, 0)
                + demTrangThai.getOrDefault(GradeSheetStatus.TRA_VE, 0);
        int soBangDiemDaCongBoHoacKhoa = demTrangThai.getOrDefault(GradeSheetStatus.DA_CONG_BO, 0)
                + demTrangThai.getOrDefault(GradeSheetStatus.DA_KHOA, 0);

        LecturerDtos.TongQuanDto tongQuan = new LecturerDtos.TongQuanDto(
                danhSachLop.size(), tongSinhVien, tongSucChua, lichDay.size(),
                soBangDiemNhapHoacTraVe,
                demTrangThai.getOrDefault(GradeSheetStatus.CHO_DUYET, 0),
                demTrangThai.getOrDefault(GradeSheetStatus.DA_DUYET, 0),
                soBangDiemDaCongBoHoacKhoa);

        LecturerDtos.HocKyTomTatDto hocKyDto = new LecturerDtos.HocKyTomTatDto(hocKy.getId(), hocKy.getTen(),
                hocKy.getNamHoc(), hocKy.getNgayBatDau(), hocKy.getNgayKetThuc());
        return new LecturerDtos.LecturerDashboardDto(hocKyDto, tongQuan, lopPhuTrach, lichDay, congViec);
    }

    private LecturerDtos.CongViecDto congViecCuaLop(CourseClass lop, GradeSheetStatus trangThai,
                                                    int soDangHoc, int soDaCoDiem) {
        String lienKet = "/lop-cua-toi/" + lop.getId() + "/nhap-diem";
        String tenMon = lop.getMonHoc().getTen();
        return switch (trangThai) {
            case NHAP -> {
                if (soDangHoc == 0) {
                    yield null;
                }
                if (soDaCoDiem < soDangHoc) {
                    yield new LecturerDtos.CongViecDto("NHAP_DIEM", lop.getId(), lop.getMa(), tenMon,
                            "Bảng điểm nháp còn thiếu điểm",
                            "Đã nhập đủ điểm " + soDaCoDiem + "/" + soDangHoc + " sinh viên đang học.", lienKet);
                }
                yield new LecturerDtos.CongViecDto("GUI_DUYET", lop.getId(), lop.getMa(), tenMon,
                        "Bảng điểm nháp đã đủ điểm, chưa gửi duyệt",
                        "Đã nhập đủ điểm " + soDangHoc + "/" + soDangHoc + " sinh viên đang học.", lienKet);
            }
            case TRA_VE -> new LecturerDtos.CongViecDto("TRA_VE", lop.getId(), lop.getMa(), tenMon,
                    "Bảng điểm bị trả về chỉnh sửa",
                    lop.getLyDoTraVeBangDiem() != null && !lop.getLyDoTraVeBangDiem().isBlank()
                            ? "Lý do trả về: " + lop.getLyDoTraVeBangDiem()
                            : "Vui lòng kiểm tra điểm và gửi duyệt lại.",
                    lienKet);
            case CHO_DUYET -> new LecturerDtos.CongViecDto("CHO_DUYET", lop.getId(), lop.getMa(), tenMon,
                    "Bảng điểm đang chờ duyệt", "Bảng điểm đã được gửi duyệt và chưa được phê duyệt.", lienKet);
            case DA_DUYET -> new LecturerDtos.CongViecDto("CHO_CONG_BO", lop.getId(), lop.getMa(), tenMon,
                    "Bảng điểm đã duyệt, chưa công bố", "Sinh viên chưa xem được điểm cho đến khi bảng điểm được công bố.",
                    lienKet);
            case DA_CONG_BO, DA_KHOA -> null;
        };
    }

    private Lecturer giangVienHienTai(User user) {
        return giangVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }
}
