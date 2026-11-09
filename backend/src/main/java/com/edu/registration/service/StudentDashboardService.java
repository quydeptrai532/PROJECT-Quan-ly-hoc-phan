package com.edu.registration.service;

import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudentDashboardService {

    private final StudentRepository sinhVienRepository;
    private final CourseRegistrationRepository dangKyRepository;
    private final GradeRepository diemRepository;
    private final SemesterRepository hocKyRepository;
    private final CurrentUser currentUser;
    private final int upcomingWindowDays;

    public StudentDashboardService(StudentRepository sinhVienRepository,
                                   CourseRegistrationRepository dangKyRepository,
                                   GradeRepository diemRepository,
                                   SemesterRepository hocKyRepository,
                                   CurrentUser currentUser,
                                   @Value("${app.dashboard.upcoming-window-days:14}") int upcomingWindowDays) {
        this.sinhVienRepository = sinhVienRepository;
        this.dangKyRepository = dangKyRepository;
        this.diemRepository = diemRepository;
        this.hocKyRepository = hocKyRepository;
        this.currentUser = currentUser;
        this.upcomingWindowDays = Math.max(0, upcomingWindowDays);
    }

    public List<CanhBaoDto> canhBao() {
        Student sinhVien = sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        List<CourseRegistration> dangKy = dangKyRepository.findAllActiveOfStudent(
                sinhVien.getId(), RegistrationStatus.DA_DANG_KY);
        Map<Long, Grade> diemTheoDangKy = dangKy.isEmpty() ? Map.of() : diemRepository
                .findByDangKyIdIn(dangKy.stream().map(CourseRegistration::getId).toList()).stream()
                .collect(Collectors.toMap(d -> d.getDangKy().getId(), Function.identity()));

        List<CanhBaoDto> canhBao = new ArrayList<>();
        themMonChuaDat(dangKy, diemTheoDangKy, canhBao);
        themMocDangKy(canhBao);
        return canhBao;
    }

    private void themMonChuaDat(List<CourseRegistration> dangKy, Map<Long, Grade> diemTheoDangKy,
                                List<CanhBaoDto> canhBao) {
        Map<Long, CourseRegistration> ketQuaMoiNhatTheoMon = new HashMap<>();
        for (CourseRegistration dk : dangKy) {
            Grade diem = diemTheoDangKy.get(dk.getId());
            if (diem == null || diem.getDiemTongKet() == null || !dk.getLopHocPhan().isBangDiemDaCongBo()) continue;
            Long monHocId = dk.getLopHocPhan().getMonHoc().getId();
            CourseRegistration hienTai = ketQuaMoiNhatTheoMon.get(monHocId);
            if (hienTai == null || soSanhLanHoc(dk, hienTai) > 0) ketQuaMoiNhatTheoMon.put(monHocId, dk);
        }

        ketQuaMoiNhatTheoMon.values().stream()
                .filter(dk -> diemTheoDangKy.get(dk.getId()).getDiemTongKet() < AcademicRules.DIEM_DAT)
                .sorted(Comparator.comparing(dk -> dk.getLopHocPhan().getMonHoc().getMa()))
                .forEach(dk -> {
                    var lop = dk.getLopHocPhan();
                    var mon = lop.getMonHoc();
                    Semester ky = lop.getHocKy();
                    double diem = diemTheoDangKy.get(dk.getId()).getDiemTongKet();
                    canhBao.add(new CanhBaoDto("MON_CHUA_DAT", "Cần học lại: " + mon.getTen(),
                            "Điểm tổng kết " + diem + " dưới ngưỡng đạt " + AcademicRules.DIEM_DAT + ".",
                            ky.getId(), ky.getTen(), ky.getNamHoc(), "/diem"));
                });
    }

    private int soSanhLanHoc(CourseRegistration a, CourseRegistration b) {
        int theoKy = a.getLopHocPhan().getHocKy().getNgayBatDau()
                .compareTo(b.getLopHocPhan().getHocKy().getNgayBatDau());
        if (theoKy != 0) return theoKy;
        return a.getThoiGianDangKy().compareTo(b.getThoiGianDangKy());
    }

    private void themMocDangKy(List<CanhBaoDto> canhBao) {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(upcomingWindowDays);
        for (Semester ky : hocKyRepository.findAllByOrderByNgayBatDauDesc()) {
            LocalDate opens = ky.getDangKyTu();
            LocalDate closes = ky.getDangKyDen();
            String kyName = ky.getTen() + " " + ky.getNamHoc();
            if (opens != null && !opens.isBefore(today) && !opens.isAfter(windowEnd)) {
                canhBao.add(new CanhBaoDto("DANG_KY_SAP_MO", "Sắp mở đăng ký: " + kyName,
                        "Đợt đăng ký dự kiến mở ngày " + opens + ".", ky.getId(), ky.getTen(),
                        ky.getNamHoc(), "/dang-ky"));
            }
            if (opens != null && closes != null && !today.isBefore(opens)
                    && !closes.isBefore(today) && !closes.isAfter(windowEnd)) {
                canhBao.add(new CanhBaoDto("DANG_KY_SAP_DONG", "Sắp đóng đăng ký: " + kyName,
                        "Hạn đăng ký là ngày " + closes + ".", ky.getId(), ky.getTen(),
                        ky.getNamHoc(), "/dang-ky"));
            }
        }
    }

    public record CanhBaoDto(String loai, String tieuDe, String lyDo, Long hocKyId,
                             String tenHocKy, String namHoc, String lienKet) { }
}
