package com.edu.registration.service;

import com.edu.registration.config.ClassScheduleConflictProperties;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.CourseClassDtos.KetQuaKiemTraXungDotDto;
import com.edu.registration.dto.CourseClassDtos.KiemTraXungDotRequest;
import com.edu.registration.dto.CourseClassDtos.XungDotLichDto;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service kiểm tra và phát hiện xung đột lịch giảng viên và phòng học khi mở lớp (QLSV-78 - TASK-AD-03).
 * Đảm bảo API và UI sử dụng cùng một thuật toán nhất quán ở backend.
 */
@Service
@Transactional(readOnly = true)
public class ClassScheduleConflictService {

    private final CourseClassRepository lopHocPhanRepository;
    private final ClassScheduleConflictProperties properties;

    public ClassScheduleConflictService(CourseClassRepository lopHocPhanRepository,
                                        ClassScheduleConflictProperties properties) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.properties = properties;
    }

    /**
     * Kiểm tra xung đột lịch giảng viên và phòng học cho một lớp học phần dự kiến (tạo mới hoặc sửa).
     *
     * @param request Dữ liệu lịch học cần kiểm tra
     * @param xacNhanGhiDe Cờ xác nhận ghi đè lưu nếu chính sách cấu hình cho phép
     * @return Kết quả kiểm tra xung đột chi tiết
     */
    public KetQuaKiemTraXungDotDto kiemTraXungDot(KiemTraXungDotRequest request, Boolean xacNhanGhiDe) {
        if (request == null || request.hocKyId() == null) {
            return new KetQuaKiemTraXungDotDto(
                    false, 0, false, false, List.of(), List.of(),
                    properties.getEnforcementPolicy().name(), true,
                    properties.isOfficialRegulation(), properties.getPolicyNote()
            );
        }

        int reqStart = request.tietBatDau();
        int reqEnd = request.tietBatDau() + request.soTiet() - 1;
        String reqPhong = request.phong() != null ? request.phong().trim() : "";
        Long reqGvId = request.giangVienId();
        int reqThu = request.thu();
        Long boQuaId = request.lopHocPhanId();

        List<CourseClass> lopCungHocKy = lopHocPhanRepository.findByHocKyIdOrderByMaAsc(request.hocKyId());
        List<XungDotLichDto> danhSachXungDot = new ArrayList<>();
        List<String> moTaCanhBao = new ArrayList<>();
        boolean xungDotGv = false;
        boolean xungDotPhong = false;

        for (CourseClass lop : lopCungHocKy) {
            // Tránh tự báo một lớp xung đột với chính nó khi sửa
            if (boQuaId != null && boQuaId.equals(lop.getId())) {
                continue;
            }

            // Chỉ so sánh các lớp thuộc cùng học kỳ và cùng thứ
            if (lop.getThu() != reqThu) {
                continue;
            }

            int lopStart = lop.getTietBatDau();
            int lopEnd = lop.getTietKetThuc();

            // Tính giao nhau theo khoảng tiết:
            // Hai lớp kết thúc và bắt đầu liền kề mà không dùng chung tiết thì không được xem là trùng
            boolean giaoNhau = reqStart <= lopEnd && lopStart <= reqEnd;
            if (!giaoNhau) {
                continue;
            }

            int tietGiaoBatDau = Math.max(reqStart, lopStart);
            int tietGiaoKetThuc = Math.min(reqEnd, lopEnd);
            String tietGiaoStr = tietGiaoBatDau == tietGiaoKetThuc
                    ? "tiết " + tietGiaoBatDau
                    : "tiết " + tietGiaoBatDau + "-" + tietGiaoKetThuc;
            String thuStr = reqThu == 8 ? "Chủ nhật" : "Thứ " + reqThu;

            // Kiểm tra trùng phòng học
            if (!reqPhong.isEmpty() && lop.getPhong() != null && lop.getPhong().trim().equalsIgnoreCase(reqPhong)) {
                xungDotPhong = true;
                String moTa = "Trùng phòng " + lop.getPhong() + " với lớp " + lop.getMa()
                        + " (" + (lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "") + ") vào "
                        + thuStr + ", " + tietGiaoStr;
                moTaCanhBao.add(moTa);
                danhSachXungDot.add(new XungDotLichDto(
                        "PHONG",
                        moTa,
                        lop.getId(),
                        lop.getMa(),
                        lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "",
                        lop.getPhong(),
                        lop.getThu(),
                        lop.getTietBatDau(),
                        lop.getSoTiet(),
                        lop.getTietKetThuc(),
                        lop.getGiangVien() != null ? lop.getGiangVien().getId() : null,
                        lop.getGiangVien() != null ? lop.getGiangVien().getHoTen() : "",
                        tietGiaoBatDau,
                        tietGiaoKetThuc
                ));
            }

            // Kiểm tra trùng lịch giảng viên
            if (reqGvId != null && lop.getGiangVien() != null && reqGvId.equals(lop.getGiangVien().getId())) {
                xungDotGv = true;
                String tenGv = lop.getGiangVien().getHoTen();
                String moTa = "Giảng viên " + tenGv + " đã có lịch phụ trách lớp " + lop.getMa()
                        + " (" + (lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "") + ") vào "
                        + thuStr + ", " + tietGiaoStr;
                moTaCanhBao.add(moTa);
                danhSachXungDot.add(new XungDotLichDto(
                        "GIANG_VIEN",
                        moTa,
                        lop.getId(),
                        lop.getMa(),
                        lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "",
                        lop.getPhong(),
                        lop.getThu(),
                        lop.getTietBatDau(),
                        lop.getSoTiet(),
                        lop.getTietKetThuc(),
                        lop.getGiangVien().getId(),
                        tenGv,
                        tietGiaoBatDau,
                        tietGiaoKetThuc
                ));
            }
        }

        boolean coXungDot = !danhSachXungDot.isEmpty();
        boolean choPhepLuu;
        ClassScheduleConflictProperties.EnforcementPolicy policy = properties.getEnforcementPolicy();

        if (!coXungDot) {
            choPhepLuu = true;
        } else {
            switch (policy) {
                case WARN_ONLY -> choPhepLuu = true;
                case ALLOW_OVERRIDE_WITH_CONFIRMATION -> choPhepLuu = Boolean.TRUE.equals(xacNhanGhiDe);
                case BLOCK_ON_CONFLICT -> choPhepLuu = false;
                default -> choPhepLuu = false;
            }
        }

        return new KetQuaKiemTraXungDotDto(
                coXungDot,
                danhSachXungDot.size(),
                xungDotGv,
                xungDotPhong,
                danhSachXungDot,
                moTaCanhBao,
                policy.name(),
                choPhepLuu,
                properties.isOfficialRegulation(),
                properties.getPolicyNote()
        );
    }

    /**
     * Xác thực và áp dụng chính sách khi lưu (tạo hoặc sửa). Nếu có xung đột và chính sách không cho phép lưu,
     * ném ngoại lệ 409 Conflict chặn đứng request ở backend.
     */
    public KetQuaKiemTraXungDotDto kiemTraVaApDungKhiLuu(CourseClassDtos.LopHocPhanRequest request, Long boQuaId) {
        KiemTraXungDotRequest checkReq = new KiemTraXungDotRequest(
                boQuaId,
                request.hocKyId(),
                request.monHocId(),
                request.giangVienId(),
                request.phong(),
                request.thu(),
                request.tietBatDau(),
                request.soTiet()
        );

        KetQuaKiemTraXungDotDto ketQua = kiemTraXungDot(checkReq, request.xacNhanLuuDuXungDot());

        if (ketQua.coXungDot() && !ketQua.choPhepLuu()) {
            String chiTiet = String.join("; ", ketQua.moTaCanhBao());
            throw ApiException.conflict("Phát hiện xung đột lịch học (" + ketQua.soLuongXungDot() + " xung đột): "
                    + chiTiet + ". Chính sách hiện tại (" + ketQua.chinhSachEnforcement()
                    + ") không cho phép lưu để tránh trùng lịch giảng viên/phòng học.");
        }

        return ketQua;
    }

    public ClassScheduleConflictProperties getProperties() {
        return properties;
    }
}
