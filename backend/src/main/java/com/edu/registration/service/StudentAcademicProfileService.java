package com.edu.registration.service;

import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.dto.UserDtos;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.StudentAcademicStatus;
import com.edu.registration.entity.StudentAcademicStatusHistory;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.StudentAcademicStatusHistoryRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class StudentAcademicProfileService {

    private static final String NOT_ASSIGNED = "Chưa cập nhật";

    private final CurrentUser currentUser;
    private final StudentRepository sinhVienRepository;
    private final LecturerRepository giangVienRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final StudentAcademicStatusHistoryRepository historyRepository;
    private final DtoMapper mapper;

    public StudentAcademicProfileService(CurrentUser currentUser, StudentRepository sinhVienRepository) {
        this(currentUser, sinhVienRepository, null, null, null, null, null);
    }

    @Autowired
    public StudentAcademicProfileService(CurrentUser currentUser,
                                         StudentRepository sinhVienRepository,
                                         LecturerRepository giangVienRepository,
                                         CourseClassRepository lopHocPhanRepository,
                                         CourseRegistrationRepository dangKyHocPhanRepository,
                                         @Autowired(required = false) StudentAcademicStatusHistoryRepository historyRepository,
                                         @Autowired(required = false) DtoMapper mapper) {
        this.currentUser = currentUser;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.historyRepository = historyRepository;
        this.mapper = mapper;
    }

    public StudentAcademicProfileService(CurrentUser currentUser,
                                         StudentRepository sinhVienRepository,
                                         LecturerRepository giangVienRepository,
                                         CourseClassRepository lopHocPhanRepository,
                                         CourseRegistrationRepository dangKyHocPhanRepository) {
        this(currentUser, sinhVienRepository, giangVienRepository, lopHocPhanRepository, dangKyHocPhanRepository, null, null);
    }

    @Transactional(readOnly = true)
    public StudentAcademicProfileDto getMyProfile() {
        Student student = sinhVienRepository.findWithAcademicProfileByUserId(currentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        return toAcademicProfileDto(student);
    }

    @Transactional(readOnly = true)
    public List<UserDtos.StudentAcademicStatusHistoryDto> getMyStatusHistory() {
        Student student = sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        if (historyRepository == null || mapper == null) {
            return List.of();
        }
        return historyRepository.findBySinhVienIdOrderByThoiDiemDescIdDesc(student.getId())
                .stream()
                .map(mapper::lichSuTrangThaiHocTap)
                .toList();
    }

    /**
     * Get minimal academic profile of a student for the authenticated lecturer (QLSV-67 / TASK-LC-02).
     * Strictly verifies teaching assignment: the student must be currently registered in a class
     * section assigned to the lecturer. Never relies on client-supplied IDs without server verification.
     */
    @Transactional(readOnly = true)
    public LecturerDtos.StudentMinimalProfileDto getMinimalProfileForLecturer(Long studentId, Long lopHocPhanId) {
        Student student = xacThucQuyenXemSinhVien(studentId, lopHocPhanId);
        return toStudentMinimalProfileDto(student);
    }

    /**
     * Get academic profile DTO of an assigned student for the authenticated lecturer.
     */
    @Transactional(readOnly = true)
    public StudentAcademicProfileDto getProfileDtoForLecturer(Long studentId, Long lopHocPhanId) {
        Student student = xacThucQuyenXemSinhVien(studentId, lopHocPhanId);
        return toAcademicProfileDto(student);
    }



    /**
     * List and search assigned students across the lecturer's teaching scope.
     * Prevents any data leakage outside assigned course sections.
     */
    @Transactional(readOnly = true)
    public List<LecturerDtos.StudentMinimalProfileDto> getAssignedStudentsForLecturer(Long lopHocPhanId, String keyword) {
        Lecturer giangVien = giangVienHienTai();
        if (lopHocPhanId != null) {
            CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
            if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
                throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
            }
        }

        List<Student> danhSach = dangKyHocPhanRepository.findAssignedStudentsOfLecturer(
                giangVien.getId(), lopHocPhanId, RegistrationStatus.DA_DANG_KY);

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase(Locale.ROOT);
            danhSach = danhSach.stream()
                    .filter(s -> (s.getMa() != null && s.getMa().toLowerCase(Locale.ROOT).contains(kw))
                            || (s.getHoTen() != null && s.getHoTen().toLowerCase(Locale.ROOT).contains(kw))
                            || (s.getEmail() != null && s.getEmail().toLowerCase(Locale.ROOT).contains(kw)))
                    .toList();
        }

        return danhSach.stream()
                .map(this::toStudentMinimalProfileDto)
                .toList();
    }

    private Student xacThucQuyenXemSinhVien(Long studentId, Long lopHocPhanId) {
        Lecturer giangVien = giangVienHienTai();

        if (lopHocPhanId != null) {
            CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
            if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
                throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
            }

            Student student = sinhVienRepository.findWithAcademicProfileById(studentId)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

            boolean enrolledInClass = dangKyHocPhanRepository.existsBySinhVienIdAndLopHocPhanIdAndGiangVienIdAndTrangThai(
                    student.getId(), lop.getId(), giangVien.getId(), RegistrationStatus.DA_DANG_KY);
            if (!enrolledInClass) {
                throw ApiException.forbidden("Sinh viên không thuộc lớp học phần mà bạn phụ trách");
            }

            return student;
        }

        Student student = sinhVienRepository.findWithAcademicProfileById(studentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        boolean assignedToLecturer = dangKyHocPhanRepository.existsBySinhVienIdAndGiangVienIdAndTrangThai(
                student.getId(), giangVien.getId(), RegistrationStatus.DA_DANG_KY);
        if (!assignedToLecturer) {
            throw ApiException.forbidden("Bạn không có quyền xem thông tin sinh viên này do không phụ trách lớp của sinh viên");
        }

        return student;
    }

    private Lecturer giangVienHienTai() {
        if (giangVienRepository == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống chưa cấu hình kho dữ liệu giảng viên");
        }
        return giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }

    public StudentAcademicProfileDto toAcademicProfileDto(Student student) {
        return new StudentAcademicProfileDto(
                student.getKhoa() != null ? student.getKhoa().getMa() : NOT_ASSIGNED,
                student.getKhoa() != null ? student.getKhoa().getTen() : NOT_ASSIGNED,
                student.getNganh() != null ? student.getNganh().getMa() : NOT_ASSIGNED,
                student.getNganh() != null ? student.getNganh().getTen() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getKhoaNhapHoc() : NOT_ASSIGNED,
                student.getLopHanhChinh() != null ? student.getLopHanhChinh().getMa() : NOT_ASSIGNED,
                student.getLopHanhChinh() != null ? student.getLopHanhChinh().getTen() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getKhoaNhapHoc() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getPhienBan() : NOT_ASSIGNED,
                student.getTrangThaiHocTap() != null ? student.getTrangThaiHocTap().getMoTa() : StudentAcademicStatus.DANG_HOC.getMoTa());
    }

    public LecturerDtos.StudentMinimalProfileDto toStudentMinimalProfileDto(Student student) {
        return new LecturerDtos.StudentMinimalProfileDto(
                student.getId(),
                student.getMa(),
                student.getHoTen(),
                student.getEmail(),
                student.getKhoa() != null ? student.getKhoa().getMa() : NOT_ASSIGNED,
                student.getKhoa() != null ? student.getKhoa().getTen() : NOT_ASSIGNED,
                student.getNganh() != null ? student.getNganh().getMa() : NOT_ASSIGNED,
                student.getNganh() != null ? student.getNganh().getTen() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getKhoaNhapHoc() : NOT_ASSIGNED,
                student.getLopHanhChinh() != null ? student.getLopHanhChinh().getMa() : NOT_ASSIGNED,
                student.getLopHanhChinh() != null ? student.getLopHanhChinh().getTen() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getKhoaNhapHoc() : NOT_ASSIGNED,
                student.getChuongTrinh() != null ? student.getChuongTrinh().getPhienBan() : NOT_ASSIGNED,
                student.getTrangThaiHocTap() != null ? student.getTrangThaiHocTap().getMoTa() : StudentAcademicStatus.DANG_HOC.getMoTa());
    }
}
