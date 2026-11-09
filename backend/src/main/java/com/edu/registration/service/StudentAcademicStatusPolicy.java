package com.edu.registration.service;

import com.edu.registration.entity.StudentAcademicStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cấu hình / Policy tập trung cho ma trận: Trạng thái học tập × Quyền đăng ký học phần (QLSV-84 / TASK-AD-09).
 *
 * <p>LƯU Ý: Ma trận này được thiết lập theo nguyên tắc mặc định an toàn (safe default)
 * do Jira chưa chốt ma trận trạng thái × quyền đăng ký chính thức từ chủ dự án:
 * <ul>
 *   <li>DANG_HOC: Được phép đăng ký học phần mới.</li>
 *   <li>BAO_LUU: Chặn đăng ký học phần mới (cần chủ dự án xác nhận trước khi áp dụng mở rộng nếu có ngoại lệ).</li>
 *   <li>THOI_HOC: Chặn đăng ký học phần.</li>
 *   <li>TOT_NGHIEP: Chặn đăng ký học phần.</li>
 * </ul>
 * Khi trạng thái thay đổi, hệ thống không xóa hoặc vô hiệu hóa lịch sử / kết quả đăng ký cũ của sinh viên.
 */
@Component
public class StudentAcademicStatusPolicy {

    public record PolicyRule(boolean choPhepDangKy, String lyDoTuChoi, boolean canChuDuAnXacNhan) {}

    private final Map<StudentAcademicStatus, PolicyRule> matrix;

    public StudentAcademicStatusPolicy() {
        Map<StudentAcademicStatus, PolicyRule> map = new EnumMap<>(StudentAcademicStatus.class);
        map.put(StudentAcademicStatus.DANG_HOC, new PolicyRule(
                true,
                null,
                false
        ));
        map.put(StudentAcademicStatus.BAO_LUU, new PolicyRule(
                false,
                "Sinh viên đang trong thời gian bảo lưu, không đủ điều kiện đăng ký học phần mới. (Chính sách mặc định an toàn - Chờ chủ dự án xác nhận quy chế mở rộng nếu có)",
                true
        ));
        map.put(StudentAcademicStatus.THOI_HOC, new PolicyRule(
                false,
                "Sinh viên đã thôi học, không được phép đăng ký học phần.",
                false
        ));
        map.put(StudentAcademicStatus.TOT_NGHIEP, new PolicyRule(
                false,
                "Sinh viên đã tốt nghiệp, không được phép đăng ký học phần mới.",
                false
        ));
        this.matrix = Collections.unmodifiableMap(map);
    }

    public boolean isEligibleForRegistration(StudentAcademicStatus status) {
        if (status == null) {
            return false;
        }
        PolicyRule rule = matrix.get(status);
        return rule != null && rule.choPhepDangKy();
    }

    public String getIneligibleReason(StudentAcademicStatus status) {
        if (status == null) {
            return "Trạng thái học tập của sinh viên chưa được xác định.";
        }
        PolicyRule rule = matrix.get(status);
        if (rule == null) {
            return "Trạng thái học tập không hợp lệ.";
        }
        return rule.lyDoTuChoi();
    }

    public Map<StudentAcademicStatus, PolicyRule> getMatrix() {
        return matrix;
    }
}
