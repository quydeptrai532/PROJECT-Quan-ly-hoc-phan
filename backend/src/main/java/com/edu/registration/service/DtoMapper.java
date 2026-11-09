package com.edu.registration.service;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.RegistrationPeriodDtos;
import com.edu.registration.dto.UserDtos;
import com.edu.registration.entity.AcademicCohort;
import com.edu.registration.entity.AdministrativeClass;
import com.edu.registration.entity.ClassTransferHistory;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.PrerequisiteSubject;
import com.edu.registration.entity.RegistrationPeriod;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.StudentAcademicStatusHistory;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.SubjectRelationType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DtoMapper {

    public CatalogDtos.KhoaDto khoa(Faculty khoa) {
        return new CatalogDtos.KhoaDto(khoa.getId(), khoa.getMa(), khoa.getTen(), khoa.isDangHieuLuc());
    }

    public CatalogDtos.NganhDto nganh(Major nganh) {
        return new CatalogDtos.NganhDto(nganh.getId(), nganh.getMa(), nganh.getTen(),
                nganh.getKhoa().getId(), nganh.getKhoa().getTen(), nganh.isDangHieuLuc());
    }

    public CatalogDtos.LopHanhChinhDto lopHanhChinh(AdministrativeClass lop) {
        return new CatalogDtos.LopHanhChinhDto(lop.getId(), lop.getMa(), lop.getTen(),
                lop.getNganh().getId(), lop.getNganh().getTen(),
                lop.getNganh().getKhoa().getId(), lop.getNganh().getKhoa().getTen(),
                lop.isDangHieuLuc());
    }

    public CatalogDtos.MonHocDto monHoc(Subject monHoc) {
        return new CatalogDtos.MonHocDto(
                monHoc.getId(),
                monHoc.getMa(),
                monHoc.getTen(),
                monHoc.getSoTinChi(),
                monHoc.getKhoa() != null ? monHoc.getKhoa().getId() : null,
                monHoc.getKhoa() != null ? monHoc.getKhoa().getTen() : null,
                monHoc.isDangHieuLuc(),
                monHoc.getNgayLuuTru(),
                monHoc.getLyDoLuuTru(),
                monHoc.getNguoiTaoId(),
                monHoc.getNguoiTaoUsername(),
                monHoc.getTaoLuc(),
                monHoc.getNguoiCapNhatId(),
                monHoc.getNguoiCapNhatUsername(),
                monHoc.getCapNhatLuc());
    }

    public CatalogDtos.MonTienQuyetDto monTienQuyet(PrerequisiteSubject lienKet) {
        Subject monHoc = lienKet.getMonHoc();
        Subject tienQuyet = lienKet.getMonTienQuyet();
        SubjectRelationType loai = lienKet.getLoaiQuanHeHieuLuc();
        Semester hkTu = lienKet.getHocKyHieuLucTu();
        Semester hkDen = lienKet.getHocKyHieuLucDen();
        return new CatalogDtos.MonTienQuyetDto(
                lienKet.getId(),
                monHoc != null ? monHoc.getId() : null,
                monHoc != null ? monHoc.getMa() : null,
                monHoc != null ? monHoc.getTen() : null,
                monHoc != null ? monHoc.getSoTinChi() : 0,
                tienQuyet != null ? tienQuyet.getId() : null,
                tienQuyet != null ? tienQuyet.getId() : null,
                tienQuyet != null ? tienQuyet.getMa() : null,
                tienQuyet != null ? tienQuyet.getTen() : null,
                tienQuyet != null ? tienQuyet.getSoTinChi() : 0,
                loai.name(),
                loai.getTenHienThi(),
                lienKet.getDieuKienDat(),
                lienKet.isYeuCauDatHieuLuc(),
                lienKet.getMaNhomTuChon(),
                lienKet.getTinChiTuChonYeuCau(),
                lienKet.getKhoaNhapHoc(),
                hkTu != null ? hkTu.getId() : null,
                hkTu != null ? (hkTu.getTen() + " (" + hkTu.getNamHoc() + ")") : null,
                hkDen != null ? hkDen.getId() : null,
                hkDen != null ? (hkDen.getTen() + " (" + hkDen.getNamHoc() + ")") : null,
                lienKet.getHieuLucTu(),
                lienKet.getHieuLucDen(),
                lienKet.isDangHieuLucHieuLuc(),
                loai.isQuyCheDaChot(),
                loai.getMoTaQuyCheHienHanh(),
                loai.getPhanCanChuDuAnChot(),
                lienKet.getGhiChu(),
                lienKet.getNguoiTaoId(),
                lienKet.getNguoiTaoUsername(),
                lienKet.getTaoLuc(),
                lienKet.getNguoiCapNhatId(),
                lienKet.getNguoiCapNhatUsername(),
                lienKet.getCapNhatLuc());
    }

    public CatalogDtos.KhoaHocDto khoaHoc(AcademicCohort cohort) {
        Semester hkBatDau = cohort.getHocKyBatDau();
        Semester hkKetThuc = cohort.getHocKyKetThuc();
        return new CatalogDtos.KhoaHocDto(
                cohort.getId(),
                cohort.getMa(),
                cohort.getTen(),
                cohort.getNienKhoa(),
                cohort.getNamBatDau(),
                cohort.getNamKetThuc(),
                hkBatDau != null ? hkBatDau.getId() : null,
                hkBatDau != null ? (hkBatDau.getTen() + " (" + hkBatDau.getNamHoc() + ")") : null,
                hkKetThuc != null ? hkKetThuc.getId() : null,
                hkKetThuc != null ? (hkKetThuc.getTen() + " (" + hkKetThuc.getNamHoc() + ")") : null,
                cohort.getHieuLucTu(),
                cohort.getHieuLucDen(),
                cohort.isDangHieuLuc(),
                cohort.getMoTa(),
                cohort.getNguoiTaoId(),
                cohort.getNguoiTaoUsername(),
                cohort.getTaoLuc(),
                cohort.getNguoiCapNhatId(),
                cohort.getNguoiCapNhatUsername(),
                cohort.getCapNhatLuc());
    }

    public CatalogDtos.HocKyDto hocKy(Semester hocKy) {
        return new CatalogDtos.HocKyDto(hocKy.getId(), hocKy.getTen(), hocKy.getNamHoc(),
                hocKy.getNgayBatDau(), hocKy.getNgayKetThuc(), hocKy.getDangKyTu(), hocKy.getDangKyDen(),
                hocKy.getSoTinChiToiThieu(), hocKy.getSoTinChiToiDa(), hocKy.dangMoDangKy());
    }

    public RegistrationPeriodDtos.DotDangKyDto dotDangKy(RegistrationPeriod dot, LocalDateTime now) {
        LocalDateTime at = now != null ? now : LocalDateTime.now();
        boolean dangMo = dot.dangMo(at);
        String trangThaiHienThi;
        if (dot.isDongSom()) {
            trangThaiHienThi = "DONG_SOM";
        } else if (!dot.isDangHieuLuc()) {
            trangThaiHienThi = "DA_DONG";
        } else if (at.isBefore(dot.getThoiGianBatDau())) {
            trangThaiHienThi = "SAP_MO";
        } else if (at.isAfter(dot.getThoiGianKetThuc())) {
            trangThaiHienThi = "DA_DONG";
        } else {
            trangThaiHienThi = "DANG_MO";
        }

        StringBuilder doiTuong = new StringBuilder();
        if (dot.laToanTruong()) {
            doiTuong.append("Toàn trường");
        } else {
            java.util.List<String> parts = new java.util.ArrayList<>();
            if (dot.getKhoaHoc() != null) {
                parts.add("Khóa " + dot.getKhoaHoc().getMa());
            } else if (dot.getKhoaNhapHoc() != null && !dot.getKhoaNhapHoc().isBlank()) {
                parts.add("Khóa " + dot.getKhoaNhapHoc());
            }
            if (dot.getNganh() != null) {
                parts.add("Ngành " + dot.getNganh().getMa() + " - " + dot.getNganh().getTen());
            }
            if (dot.getLopHanhChinh() != null) {
                parts.add("Lớp " + dot.getLopHanhChinh().getMa());
            }
            if (dot.getNhomDoiTuong() != null && !dot.getNhomDoiTuong().isBlank()
                    && !dot.getNhomDoiTuong().equalsIgnoreCase("TOAN_TRUONG")) {
                parts.add("Nhóm: " + dot.getNhomDoiTuong());
            }
            doiTuong.append(String.join(", ", parts));
        }

        return new RegistrationPeriodDtos.DotDangKyDto(
                dot.getId(),
                dot.getHocKy() != null ? dot.getHocKy().getId() : null,
                dot.getHocKy() != null ? dot.getHocKy().getTen() : null,
                dot.getHocKy() != null ? dot.getHocKy().getNamHoc() : null,
                dot.getMaDot(),
                dot.getTenDot(),
                dot.getLoaiDot(),
                dot.getKhoaHoc() != null ? dot.getKhoaHoc().getId() : null,
                dot.getKhoaHoc() != null ? dot.getKhoaHoc().getMa() : null,
                dot.getKhoaHoc() != null ? dot.getKhoaHoc().getTen() : null,
                dot.getKhoaNhapHoc(),
                dot.getNganh() != null ? dot.getNganh().getId() : null,
                dot.getNganh() != null ? dot.getNganh().getMa() : null,
                dot.getNganh() != null ? dot.getNganh().getTen() : null,
                dot.getLopHanhChinh() != null ? dot.getLopHanhChinh().getId() : null,
                dot.getLopHanhChinh() != null ? dot.getLopHanhChinh().getMa() : null,
                dot.getLopHanhChinh() != null ? dot.getLopHanhChinh().getTen() : null,
                dot.getNhomDoiTuong(),
                dot.getThuTuUuTien(),
                dot.getThoiGianBatDau(),
                dot.getThoiGianKetThuc(),
                dot.isDangHieuLuc(),
                dot.isDongSom(),
                dot.getThoiDiemDongSom(),
                dot.getLyDoDongSom(),
                dot.getThoiGianKetThucGoc(),
                dot.getLyDoDieuChinh(),
                dangMo,
                trangThaiHienThi,
                doiTuong.toString(),
                dot.getNguoiTaoId(),
                dot.getNguoiTaoUsername(),
                dot.getTaoLuc(),
                dot.getNguoiCapNhatId(),
                dot.getNguoiCapNhatUsername(),
                dot.getCapNhatLuc(),
                dot.getGhiChu());
    }

    public CourseClassDtos.LopHocPhanDto lopHocPhan(CourseClass lop) {
        return new CourseClassDtos.LopHocPhanDto(lop.getId(), lop.getMa(),
                lop.getMonHoc().getId(), lop.getMonHoc().getMa(), lop.getMonHoc().getTen(),
                lop.getMonHoc().getSoTinChi(), lop.getHocKy().getId(), lop.getHocKy().getTen(),
                lop.getGiangVien().getId(), lop.getGiangVien().getHoTen(), lop.getPhong(),
                lop.getThu(), lop.getTietBatDau(), lop.getSoTiet(),
                lop.getSiSoToiDa(), lop.getSiSoHienTai(), lop.getConLai(),
                lop.getTrangThaiBangDiemHieuLuc().name(), lop.getPhienBanCongBo(),
                lop.getGuiDuyetBangDiemLuc(),
                lop.getTraVeBangDiemLuc(),
                lop.getLyDoTraVeBangDiem(),
                lop.getDuyetBangDiemLuc(),
                lop.getCongBoBangDiemLuc(),
                lop.getKhoaBangDiemLuc(),
                lop.getMoKhoaBangDiemLuc(),
                lop.getLyDoMoKhoaBangDiem(),
                lop.isChoPhepSuaDiem(),
                lop.isBangDiemDaKhoa(),
                lop.isDaHuy(),
                lop.getThoiGianHuy(),
                lop.getLyDoHuy(),
                lop.getNguoiHuyId(),
                lop.getNguoiHuyUsername());
    }

    public RegistrationDtos.DangKyDto dangKy(CourseRegistration dangKy, Grade diem) {
        CourseClass lop = dangKy.getLopHocPhan();
        boolean daCongBo = lop != null && lop.isBangDiemDaCongBo() && diem != null && diem.getDiemTongKet() != null;
        return new RegistrationDtos.DangKyDto(dangKy.getId(), lop.getId(), lop.getMa(),
                lop.getMonHoc().getId(), lop.getMonHoc().getMa(), lop.getMonHoc().getTen(),
                lop.getMonHoc().getSoTinChi(), lop.getGiangVien().getHoTen(), lop.getPhong(),
                lop.getThu(), lop.getTietBatDau(), lop.getSoTiet(),
                lop.getHocKy().getId(), lop.getHocKy().getTen(), lop.getHocKy().getNamHoc(),
                dangKy.getThoiGianDangKy(), dangKy.getTrangThai().name(),
                daCongBo ? diem.getDiemQuaTrinh() : null,
                daCongBo ? diem.getDiemCuoiKy() : null,
                daCongBo ? diem.getDiemTongKet() : null);
    }

    public RegistrationDtos.DiemDto diem(CourseRegistration dangKy, Grade diem) {
        return new RegistrationDtos.DiemDto(dangKy.getId(), dangKy.getSinhVien().getId(),
                dangKy.getSinhVien().getMa(), dangKy.getSinhVien().getHoTen(),
                dangKy.getSinhVien().getEmail(),
                diem != null ? diem.getDiemQuaTrinh() : null,
                diem != null ? diem.getDiemCuoiKy() : null,
                diem != null ? diem.getDiemTongKet() : null);
    }

    public RegistrationDtos.ThoiKhoaBieuDto thoiKhoaBieu(CourseRegistration dangKy) {
        CourseClass lop = dangKy.getLopHocPhan();
        return new RegistrationDtos.ThoiKhoaBieuDto(lop.getId(), lop.getMa(), lop.getMonHoc().getTen(),
                lop.getPhong(), lop.getThu(), lop.getTietBatDau(), lop.getSoTiet(),
                lop.getGiangVien().getHoTen());
    }

    public UserDtos.SinhVienDto sinhVien(Student sinhVien) {
        return new UserDtos.SinhVienDto(sinhVien.getId(), sinhVien.getMa(), sinhVien.getHoTen(),
                sinhVien.getEmail(), sinhVien.getSoDienThoai(),
                sinhVien.getKhoa() != null ? sinhVien.getKhoa().getId() : null,
                sinhVien.getKhoa() != null ? sinhVien.getKhoa().getTen() : null,
                sinhVien.getNganh() != null ? sinhVien.getNganh().getId() : null,
                sinhVien.getNganh() != null ? sinhVien.getNganh().getTen() : null,
                sinhVien.getLopHanhChinh() != null ? sinhVien.getLopHanhChinh().getId() : null,
                sinhVien.getLopHanhChinh() != null ? sinhVien.getLopHanhChinh().getTen() : null,
                sinhVien.getUser().getUsername(), sinhVien.getUser().isTrangThai(),
                sinhVien.getTrangThaiHocTap(),
                sinhVien.getTrangThaiHocTap() != null ? sinhVien.getTrangThaiHocTap().getMoTa() : null);
    }

    public UserDtos.StudentAcademicStatusHistoryDto lichSuTrangThaiHocTap(StudentAcademicStatusHistory history) {
        return new UserDtos.StudentAcademicStatusHistoryDto(
                history.getId(),
                history.getSinhVien() != null ? history.getSinhVien().getId() : null,
                history.getSinhVien() != null ? history.getSinhVien().getMa() : null,
                history.getSinhVien() != null ? history.getSinhVien().getHoTen() : null,
                history.getTrangThaiTruoc(),
                history.getTrangThaiTruoc() != null ? history.getTrangThaiTruoc().getMoTa() : null,
                history.getTrangThaiSau(),
                history.getTrangThaiSau() != null ? history.getTrangThaiSau().getMoTa() : null,
                history.getNgayHieuLuc(),
                history.getGhiChu(),
                history.getNguoiThucHien() != null ? history.getNguoiThucHien().getId() : null,
                history.getNguoiThucHien() != null ? history.getNguoiThucHien().getUsername() : null,
                history.getNguoiThucHien() != null ? history.getNguoiThucHien().getUsername() : null,
                history.getThoiDiem()
        );
    }

    public CourseClassDtos.LichSuChuyenLopDto lichSuChuyenLop(ClassTransferHistory history) {
        return new CourseClassDtos.LichSuChuyenLopDto(
                history.getId(),
                history.getLopNguon().getId(),
                history.getLopNguon().getMa(),
                history.getMonHocNguon().getTen(),
                history.getLopDich().getId(),
                history.getLopDich().getMa(),
                history.getMonHocDich().getTen(),
                history.getSinhVien().getId(),
                history.getSinhVien().getMa(),
                history.getSinhVien().getHoTen(),
                history.getNguoiThucHien().getId(),
                history.getNguoiThucHien().getUsername(),
                history.getThoiGianChuyen(),
                history.getLyDo(),
                history.getLoaiQuanHeMon(),
                history.getGhiChu()
        );
    }

    public UserDtos.GiangVienDto giangVien(Lecturer giangVien) {
        return new UserDtos.GiangVienDto(giangVien.getId(), giangVien.getMa(), giangVien.getHoTen(),
                giangVien.getEmail(), giangVien.getSoDienThoai(),
                giangVien.getKhoa() != null ? giangVien.getKhoa().getId() : null,
                giangVien.getKhoa() != null ? giangVien.getKhoa().getTen() : null,
                giangVien.getUser().getUsername(), giangVien.getUser().isTrangThai());
    }
}
