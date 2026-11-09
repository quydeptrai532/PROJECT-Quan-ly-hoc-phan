package com.edu.registration.service;

import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.util.CsvUtils;
import com.edu.registration.util.ExportSecuritySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ClassStudentExportService {
    public static final String HANH_DONG_AUDIT = "COURSE_STUDENT_LIST_EXPORTED";
    private static final String LOAI_DOI_TUONG = "COURSE_SECTION";
    private static final Set<String> SAP_XEP_HOP_LE = Set.of("ma", "-ma", "hoTen", "-hoTen");
    private static final Logger log = LoggerFactory.getLogger(ClassStudentExportService.class);

    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final LecturerRepository giangVienRepository;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;
    private final boolean choPhepEmail;

    public ClassStudentExportService(CourseClassRepository lopHocPhanRepository,
                                    CourseRegistrationRepository dangKyHocPhanRepository,
                                    LecturerRepository giangVienRepository, CurrentUser currentUser,
                                    AuditLogService auditLogService,
                                    @Value("${app.export.danh-sach-lop.cho-phep-email:true}") boolean choPhepEmail) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.giangVienRepository = giangVienRepository;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
        this.choPhepEmail = choPhepEmail;
    }

    /**
     * @param tuKhoa lọc theo mã/họ tên (và email nếu được phép xuất email); null hoặc rỗng là không lọc
     * @param sapXep một trong ma, -ma, hoTen, -hoTen; null là sắp theo mã tăng dần như màn hình
     */
    @Transactional
    public CsvFile xuatDanhSach(Long lopHocPhanId, String tuKhoa, String sapXep) {
        String thuTu = chuanHoaSapXep(sapXep);
        String tuKhoaChuan = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT);

        return auditLogService.execute(HANH_DONG_AUDIT, LOAI_DOI_TUONG, lopHocPhanId, audit -> {
            Lecturer giangVien = giangVienRepository.findByUser(currentUser.get())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
            if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
                throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
            }

            List<Student> danhSach = dangKyHocPhanRepository
                    .findDanhSachXuat(lopHocPhanId, RegistrationStatus.DA_DANG_KY).stream()
                    .map(CourseRegistration::getSinhVien)
                    .filter(sinhVien -> khopTuKhoa(sinhVien, tuKhoaChuan))
                    .sorted(boSoSanh(thuTu))
                    .toList();

            ExportSecuritySupport.validateRowCount(danhSach.size(), "danh sách sinh viên lớp học phần");
            byte[] noiDung = taoCsv(danhSach).getBytes(StandardCharsets.UTF_8);
            // Audit chỉ lưu định danh lớp và số bản ghi, không lưu dữ liệu cá nhân của sinh viên.
            audit.changes(Map.of(), Map.of("lopHocPhanId", lopHocPhanId, "maLop", lop.getMa(),
                    "soBanGhi", danhSach.size(), "tuKhoa", tuKhoaChuan.isEmpty() ? "KHONG_LOC" : tuKhoaChuan, "sapXep", thuTu));
            log.info("Giảng viên {} xuất danh sách {} sinh viên của lớp {}",
                    giangVien.getMa(), danhSach.size(), lop.getMa());
            return new CsvFile(tenTep(lop.getMa()), noiDung, danhSach.size());
        });
    }

    private String taoCsv(List<Student> danhSach) {
        List<String> tieuDe = new ArrayList<>(List.of("STT", "Mã sinh viên", "Họ tên", "Lớp hành chính"));
        if (choPhepEmail) {
            tieuDe.add("Email");
        }
        StringBuilder csv = new StringBuilder(CsvUtils.BOM);
        csv.append(CsvUtils.row(tieuDe));
        int stt = 1;
        for (Student sinhVien : danhSach) {
            List<String> dong = new ArrayList<>();
            dong.add(String.valueOf(stt++));
            dong.add(sinhVien.getMa());
            dong.add(sinhVien.getHoTen());
            dong.add(sinhVien.getLopHanhChinh() == null ? "" : sinhVien.getLopHanhChinh().getMa());
            if (choPhepEmail) {
                dong.add(sinhVien.getEmail());
            }
            csv.append(CsvUtils.row(dong));
        }
        return csv.toString();
    }

    private boolean khopTuKhoa(Student sinhVien, String tuKhoa) {
        if (tuKhoa.isEmpty()) {
            return true;
        }
        return chua(sinhVien.getMa(), tuKhoa) || chua(sinhVien.getHoTen(), tuKhoa)
                || (choPhepEmail && chua(sinhVien.getEmail(), tuKhoa));
    }

    private boolean chua(String giaTri, String tuKhoa) {
        return giaTri != null && giaTri.toLowerCase(Locale.ROOT).contains(tuKhoa);
    }

    private Comparator<Student> boSoSanh(String thuTu) {
        boolean giamDan = thuTu.startsWith("-");
        Comparator<Student> boSoSanh;
        if (thuTu.endsWith("hoTen")) {
            Collator collator = Collator.getInstance(Locale.forLanguageTag("vi"));
            boSoSanh = Comparator.comparing(Student::getHoTen, Comparator.nullsLast(collator));
        } else {
            boSoSanh = Comparator.comparing(Student::getMa, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return giamDan ? boSoSanh.reversed() : boSoSanh;
    }

    private String chuanHoaSapXep(String sapXep) {
        if (sapXep == null || sapXep.isBlank()) {
            return "ma";
        }
        String giaTri = sapXep.trim();
        if (!SAP_XEP_HOP_LE.contains(giaTri)) {
            throw ApiException.badRequest("Giá trị sắp xếp không hợp lệ, chỉ chấp nhận: ma, -ma, hoTen, -hoTen");
        }
        return giaTri;
    }

    private String tenTep(String maLop) {
        return "danh-sach-sinh-vien-" + ExportSecuritySupport.sanitizeFilename(maLop) + ".csv";
    }

    public record CsvFile(String tenTep, byte[] noiDung, int soDong) {
    }
}
