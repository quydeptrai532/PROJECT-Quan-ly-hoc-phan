package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.ClassAbsenceNotice;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.ClassAbsenceNoticeRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ClassAbsenceNoticeService {

    private final ClassAbsenceNoticeRepository thongBaoNghiRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final LecturerRepository giangVienRepository;
    private final StudentRepository sinhVienRepository;
    private final SemesterService hocKyService;
    private final CurrentUser currentUser;
    private final NotificationService thongBaoService;
    private final AttendanceAlertService attendanceAlertService;

    public ClassAbsenceNoticeService(ClassAbsenceNoticeRepository thongBaoNghiRepository,
                               CourseClassRepository lopHocPhanRepository,
                               LecturerRepository giangVienRepository,
                               StudentRepository sinhVienRepository,
                               SemesterService hocKyService, CurrentUser currentUser) {
        this(thongBaoNghiRepository, lopHocPhanRepository, giangVienRepository, sinhVienRepository,
                hocKyService, currentUser, null);
    }

    public ClassAbsenceNoticeService(ClassAbsenceNoticeRepository thongBaoNghiRepository,
                               CourseClassRepository lopHocPhanRepository,
                               LecturerRepository giangVienRepository,
                               StudentRepository sinhVienRepository,
                               SemesterService hocKyService, CurrentUser currentUser,
                               NotificationService thongBaoService) {
        this(thongBaoNghiRepository, lopHocPhanRepository, giangVienRepository,
                sinhVienRepository, hocKyService, currentUser, thongBaoService, null);
    }

    @Autowired
    public ClassAbsenceNoticeService(ClassAbsenceNoticeRepository thongBaoNghiRepository,
                               CourseClassRepository lopHocPhanRepository,
                               LecturerRepository giangVienRepository,
                               StudentRepository sinhVienRepository,
                               SemesterService hocKyService, CurrentUser currentUser,
                               NotificationService thongBaoService,
                               AttendanceAlertService attendanceAlertService) {
        this.thongBaoNghiRepository = thongBaoNghiRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.giangVienRepository = giangVienRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.hocKyService = hocKyService;
        this.currentUser = currentUser;
        this.thongBaoService = thongBaoService;
        this.attendanceAlertService = attendanceAlertService;
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThongBaoNghiDto> cuaGiangVien(Long hocKyId) {
        Lecturer giangVien = giangVienHienTai();
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        return thongBaoNghiRepository.cuaGiangVien(giangVien.getId(), hocKy.getId())
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThongBaoNghiDto> cuaSinhVien(Long hocKyId) {
        Student sinhVien = sinhVienHienTai();
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        return thongBaoNghiRepository
                .cuaSinhVien(sinhVien.getId(), hocKy.getId(), RegistrationStatus.DA_DANG_KY)
                .stream().map(this::map).toList();
    }

    public RegistrationDtos.ThongBaoNghiDto tao(RegistrationDtos.ThongBaoNghiRequest request) {
        Lecturer giangVien = giangVienHienTai();
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(request.lopHocPhanId())
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
        }

        LocalDate ngayNghi = request.ngayNghi();
        Semester hocKy = lop.getHocKy();
        if (ngayNghi.isBefore(hocKy.getNgayBatDau()) || ngayNghi.isAfter(hocKy.getNgayKetThuc())) {
            throw ApiException.badRequest("Ngày nghỉ phải nằm trong thời gian của học kỳ");
        }
        if (ngayNghi.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Không thể thông báo nghỉ cho buổi học đã diễn ra");
        }

        int thu = ngayNghi.getDayOfWeek().getValue() + 1;
        if (thu != lop.getThu()) {
            throw ApiException.badRequest("Ngày nghỉ phải rơi vào " + thuLabel(lop.getThu())
                    + " (lịch học của lớp " + lop.getMa() + ")");
        }

        if (thongBaoNghiRepository.existsByLopHocPhanIdAndNgayNghi(lop.getId(), ngayNghi)) {
            throw ApiException.conflict("Đã có thông báo nghỉ cho buổi học này");
        }

        ClassAbsenceNotice thongBao = new ClassAbsenceNotice();
        thongBao.setLopHocPhan(lop);
        thongBao.setGiangVien(giangVien);
        thongBao.setNgayNghi(ngayNghi);
        thongBao.setLyDo(request.lyDo() == null ? null : request.lyDo().trim());
        thongBao.setThoiGianTao(LocalDateTime.now());
        ClassAbsenceNotice saved = thongBaoNghiRepository.save(thongBao);
        if (thongBaoService != null) {
            thongBaoService.classSessionCancelled(saved);
        }
        if (attendanceAlertService != null) {
            attendanceAlertService.kiemTraVaCapNhatCanhBaoChoLop(lop.getId());
        }
        return map(saved);
    }

    public void xoa(Long id) {
        Lecturer giangVien = giangVienHienTai();
        ClassAbsenceNotice thongBao = thongBaoNghiRepository
                .findByIdAndLopHocPhanGiangVienId(id, giangVien.getId())
                .orElseThrow(() -> ApiException.notFound("Thông báo nghỉ không tồn tại"));
        Long lopId = thongBao.getLopHocPhan().getId();
        thongBaoNghiRepository.delete(thongBao);
        if (attendanceAlertService != null) {
            attendanceAlertService.kiemTraVaCapNhatCanhBaoChoLop(lopId);
        }
    }

    private RegistrationDtos.ThongBaoNghiDto map(ClassAbsenceNotice thongBao) {
        CourseClass lop = thongBao.getLopHocPhan();
        return new RegistrationDtos.ThongBaoNghiDto(
                thongBao.getId(), lop.getId(), lop.getMa(), lop.getMonHoc().getTen(),
                thongBao.getNgayNghi(), thongBao.getLyDo(),
                thongBao.getGiangVien() != null ? thongBao.getGiangVien().getHoTen() : lop.getGiangVien().getHoTen(),
                thongBao.getThoiGianTao());
    }

    private String thuLabel(int thu) {
        return switch (thu) {
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            case 8 -> "Chủ nhật";
            default -> "thứ " + thu;
        };
    }

    private Lecturer giangVienHienTai() {
        return giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }

    private Student sinhVienHienTai() {
        return sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }
}
