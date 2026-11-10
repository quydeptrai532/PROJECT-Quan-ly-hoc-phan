package com.edu.registration.dto;

import com.edu.registration.entity.VerifiableTranscriptStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Các DTO phục vụ nghiệp vụ bảng điểm điện tử xác thực (QLSV-99 / TASK-OP-12).
 */
public final class VerifiableTranscriptDtos {

    private VerifiableTranscriptDtos() {
    }

    /**
     * DTO trả về cho cổng tra cứu / xác thực công khai.
     * TUÂN THỦ CHẶT CHẼ NGUYÊN TẮC BẢO MẬT:
     * - Chỉ trả thông tin tối thiểu theo chính sách đã duyệt (không công khai điểm chi tiết từng môn).
     * - Khi mã không tồn tại, hết hạn hoặc bị thu hồi: trả trạng thái tương ứng, TUYỆT ĐỐI KHÔNG TIẾT LỘ THÔNG TIN CÁ NHÂN.
     */
    public record VerifiableTranscriptPublicVerifyDto(
            String trangThaiXacMinh,
            String thongDiep,
            String maXacMinhMasked,
            String maSinhVien,
            String hoTen,
            String khoa,
            String nganh,
            String lopHanhChinh,
            String khoaNhapHoc,
            String phamVi,
            Integer phienBan,
            LocalDateTime ngayPhatHanh,
            String donViPhatHanh,
            Integer tongSoHocPhan,
            Integer tongTinChiTichLuy,
            Double gpa10,
            Double gpa4,
            String fileSha256,
            String chinhSachBaoMat,
            LocalDateTime ngayThuHoi,
            String lyDoThuHoi
    ) {
        public static VerifiableTranscriptPublicVerifyDto hopLe(
                String maXacMinhMasked,
                String maSinhVien,
                String hoTen,
                String khoa,
                String nganh,
                String lopHanhChinh,
                String khoaNhapHoc,
                String phamVi,
                Integer phienBan,
                LocalDateTime ngayPhatHanh,
                String donViPhatHanh,
                Integer tongSoHocPhan,
                Integer tongTinChiTichLuy,
                Double gpa10,
                Double gpa4,
                String fileSha256,
                String chinhSachBaoMat) {
            return new VerifiableTranscriptPublicVerifyDto(
                    "HOP_LE",
                    "Bảng điểm điện tử hợp lệ và chính thức được cấp bởi Hệ thống Quản lý Đào tạo.",
                    maXacMinhMasked,
                    maSinhVien,
                    hoTen,
                    khoa,
                    nganh,
                    lopHanhChinh,
                    khoaNhapHoc,
                    phamVi,
                    phienBan,
                    ngayPhatHanh,
                    donViPhatHanh,
                    tongSoHocPhan,
                    tongTinChiTichLuy,
                    gpa10,
                    gpa4,
                    fileSha256,
                    chinhSachBaoMat,
                    null,
                    null
            );
        }

        public static VerifiableTranscriptPublicVerifyDto khongHopLe(String thongDiep) {
            return new VerifiableTranscriptPublicVerifyDto(
                    "KHONG_HOP_LE",
                    thongDiep != null ? thongDiep : "Mã xác minh không tồn tại hoặc không hợp lệ. Vui lòng kiểm tra lại mã hoặc quét lại mã QR trên bảng điểm chính thức.",
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null
            );
        }

        public static VerifiableTranscriptPublicVerifyDto daThuHoi(LocalDateTime ngayThuHoi, String lyDoThuHoi, Integer phienBan) {
            return new VerifiableTranscriptPublicVerifyDto(
                    "DA_THU_HOI",
                    "Bảng điểm điện tử tương ứng với mã xác minh này đã bị THU HỒI / HỦY BỎ hiệu lực.",
                    null, null, null, null, null, null, null, null, phienBan, null, null, null, null, null, null, null,
                    "Bảng điểm không còn giá trị pháp lý học vụ kể từ thời điểm thu hồi.",
                    ngayThuHoi, lyDoThuHoi
            );
        }

        public static VerifiableTranscriptPublicVerifyDto daThayThe(Integer phienBan) {
            return new VerifiableTranscriptPublicVerifyDto(
                    "DA_THAY_THE",
                    "Bảng điểm điện tử tương ứng với phiên bản này đã được THAY THẾ bởi phiên bản phát hành mới hơn.",
                    null, null, null, null, null, null, null, null, phienBan, null, null, null, null, null, null, null,
                    "Vui lòng sử dụng bản in / tệp điện tử của phiên bản mới nhất để xác thực.",
                    null, null
            );
        }
    }

    /**
     * DTO danh sách tổng quan bản ghi bảng điểm xác thực.
     */
    public record VerifiableTranscriptSummaryDto(
            Long id,
            String maXacMinhMasked,
            String maSinhVien,
            String hoTenSinhVien,
            Long hocKyId,
            String tenHocKy,
            Integer phienBan,
            VerifiableTranscriptStatus trangThai,
            String trangThaiMoTa,
            String fileSha256,
            Integer soMonDaCongBo,
            Integer tongTinChiCoDiem,
            Integer tongTinChiTichLuy,
            Double gpa10,
            Double gpa4,
            LocalDateTime thoiDiemPhatHanh,
            String nguoiPhatHanhUsername,
            LocalDateTime thoiDiemThuHoi,
            String nguoiThuHoiUsername,
            String lyDoThuHoi,
            Long banGhiGocId,
            Long banGhiThayTheId
    ) {
    }

    /**
     * DTO chi tiết bản ghi (dành cho quản trị viên xem chi tiết bản ghi và đối soát snapshot).
     */
    public record VerifiableTranscriptDetailDto(
            Long id,
            String maXacMinhMasked,
            String maSinhVien,
            String hoTenSinhVien,
            Long hocKyId,
            String tenHocKy,
            Integer phienBan,
            VerifiableTranscriptStatus trangThai,
            String trangThaiMoTa,
            String fileSha256,
            Integer soMonDaCongBo,
            Integer tongTinChiCoDiem,
            Integer tongTinChiTichLuy,
            Double gpa10,
            Double gpa4,
            String donViPhatHanh,
            LocalDateTime thoiDiemPhatHanh,
            String nguoiPhatHanhUsername,
            String nguoiPhatHanhRole,
            LocalDateTime thoiDiemThuHoi,
            String nguoiThuHoiUsername,
            String nguoiThuHoiRole,
            String lyDoThuHoi,
            Long banGhiGocId,
            Long banGhiThayTheId,
            RegistrationDtos.BangDiemPdfDto snapshot,
            List<VerifiableTranscriptSummaryDto> lichSuPhienBan
    ) {
    }

    /**
     * Phản hồi khi cấp mới hoặc cấp lại thành công bảng điểm xác thực.
     * Cung cấp token xác minh dùng một lần để tải về hoặc in ấn.
     */
    public record IssueVerifiableTranscriptResponse(
            Long id,
            String maXacMinh,
            String maXacMinhMasked,
            Integer phienBan,
            String fileSha256,
            String verificationUrl,
            LocalDateTime thoiDiemPhatHanh,
            String tenTep
    ) {
    }

    /**
     * Yêu cầu thu hồi bảng điểm điện tử.
     */
    public record RevokeTranscriptRequest(
            @NotBlank(message = "Lý do thu hồi bảng điểm không được để trống")
            @Size(max = 500, message = "Lý do thu hồi không được vượt quá 500 ký tự")
            String lyDo
    ) {
    }

    /**
     * Yêu cầu cấp lại bảng điểm điện tử.
     */
    public record ReissueTranscriptRequest(
            @NotBlank(message = "Lý do cấp lại bảng điểm không được để trống")
            @Size(max = 500, message = "Lý do cấp lại không được vượt quá 500 ký tự")
            String lyDo
    ) {
    }
}
