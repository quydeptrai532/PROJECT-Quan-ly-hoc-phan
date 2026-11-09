package com.edu.registration.service;

import com.edu.registration.dto.AuditLogDtos;
import com.edu.registration.entity.AuditLog;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final Set<String> SAFE_DETAIL_FIELDS = Set.of(
            "maLop", "monHocId", "hocKyId", "giangVienId", "phong", "thu", "tietBatDau", "soTiet",
            "siSoToiDa", "siSoHienTai", "conLai", "tenHocKy", "namHoc", "dangKyTu", "dangKyDen", "soTinChiToiThieu",
            "soTinChiToiDa", "trangThai", "dangKyId", "lopHocPhanId", "active", "maMon", "tenMon", "soTinChi",
            "loaiDangKy", "khoaId", "nganhId", "lopId", "monTienQuyetId", "lyDoHuy", "soBanGhi",
            "maPhieu", "loaiYeuCau", "trangThaiCu", "trangThaiMoi", "soTepDinhKem", "diemId", "traoDoiId",
            "trangThaiBangDiem", "phienBanCongBo", "phienBanBangDiem", "soSinhVienDangHoc", "soSinhVienDuDieuKien", "soBanGhiDiem", "soNguoiNhan", "thongBaoLopId", "tieuDe",
            "buoiHocId", "ngayHoc", "caHoc", "soSinhVienDiemDanh", "daKhoa", "thaoTacDiemDanh",
            "canhBaoId", "sinhVienId", "soBuoiVang", "tongBuoiHopLe", "tyLeVang", "loaiNguong", "giaTriNguong",
            "trangThaiCanhBao", "nguongSoBuoiVang", "nguongTyLeVang",
            "kichHoat", "nguong", "cuaSoPhut", "mucDo",
            "soTaoMoi", "soCapNhat", "soThatBai", "tongSoDong", "transactionPolicy", "updatePolicy", "tenTep",
            "lyDo", "lyDoTraVe", "lyDoMoKhoa", "hanhDong", "nguoiThucHienId", "choPhepSuaDiem",
            "loaiQuanHe", "monLienQuanId", "maMonLienQuan", "dieuKienDat", "yeuCauDat",
            "maNhomTuChon", "tinChiTuChonYeuCau", "khoaNhapHoc", "hocKyHieuLucTuId", "hocKyHieuLucDenId",
            "hieuLucTu", "hieuLucDen", "dangHieuLuc", "maKhoaHoc", "tenKhoaHoc", "nienKhoa",
            "namBatDau", "namKetThuc", "hocKyBatDauId", "hocKyKetThucId", "nguoiTaoUsername", "nguoiCapNhatUsername",
            "maDot", "tenDot", "loaiDot", "thoiGianBatDau", "thoiGianKetThuc", "thuTuUuTien",
            "lopHanhChinhId", "khoaHocId", "nhomDoiTuong", "dongSom", "thoiDiemDongSom",
            "lyDoDongSom", "thoiGianKetThucGoc", "lyDoDieuChinh", "dotDangKyId", "thoiGianKetThucMoi", "xacNhanLuuDuXungDot", "lyDoXacNhan", "soLuongXungDot",
            "daHuy", "thoiGianHuy", "nguoiHuyId", "nguoiHuyUsername", "lopNguonId", "maLopNguon", "lopDichId", "maLopDich", "soLuongChuyen", "loaiQuanHeMon", "soDangKyBiHuy", "phongHocId", "maPhong", "tenPhong", "toaNha", "tang", "viTri", "sucChua", "loaiPhong", "thietBi", "trangThaiPhong", "ghiChu",
            "trangThaiHocTap", "trangThaiHocTapTruoc", "trangThaiHocTapSau", "ngayHieuLuc",
            "phucKhaoId", "maHoSo", "diemQuaTrinhGoc", "diemCuoiKyGoc", "diemTongKetGoc", "diemQuaTrinhMoi", "diemCuoiKyMoi", "diemTongKetMoi", "loaiDiemPhucKhao", "ketQuaXuLy", "hanNop", "ghiChuXuLy", "diemMongMuon",
            "mucUuTien", "mucUuTienCu", "mucUuTienMoi", "hanXuLy", "hanXuLyCu", "hanXuLyMoi", "nguoiXuLyId", "nguoiXuLyCuId", "nguoiXuLyMoiId", "nguoiXuLyUsername", "nguoiXuLyCuUsername", "nguoiXuLyMoiUsername", "loaiThayDoi", "giaTriCu", "giaTriMoi", "yeuCauBoSungMinhChung", "noiDungYeuCauMinhChung", "congKhai", "isNoiBo"
    );

    private final AuditLogRepository repository;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate failureTransaction;

    public AuditLogService(AuditLogRepository repository, CurrentUser currentUser,
                           ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.failureTransaction = new TransactionTemplate(transactionManager);
        this.failureTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Execute an audited operation. Successful rows join the business transaction; failures use a new transaction. */
    public <T> T execute(String action, String entityType, Object initialEntityId,
                         Function<AuditContext, T> operation) {
        User actor = currentUser.get();
        AuditContext context = new AuditContext(initialEntityId == null ? null : String.valueOf(initialEntityId));
        try {
            T value = operation.apply(context);
            save(actor, action, entityType, context, "SUCCESS", null);
            return value;
        } catch (RuntimeException failure) {
            try {
                failureTransaction.executeWithoutResult(status ->
                        save(actor, action, entityType, context, "FAILURE", failureCode(failure)));
            } catch (RuntimeException auditFailure) {
                // The business operation still fails; keep a server-side signal if even the failure row cannot persist.
                log.error("Không ghi được audit failure cho action={} entityType={} entityId={}",
                        action, entityType, context.entityId, auditFailure);
            }
            throw failure;
        }
    }

    public Page<AuditLogDtos.AuditLogDto> search(LocalDateTime fromTime, LocalDateTime toTime,
                                                 String actor, String action, String entityType,
                                                 String entityId, int page, int size) {
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw ApiException.badRequest("Thời điểm bắt đầu phải trước thời điểm kết thúc");
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Sort sort = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));
        return repository.search(fromTime, toTime, normalize(actor), normalize(action), normalize(entityType),
                        normalize(entityId), PageRequest.of(safePage, safeSize, sort))
                .map(this::toDto);
    }

    /** Xem một bản ghi audit theo id (dùng để truy ngược từ cảnh báo bất thường). */
    public AuditLogDtos.AuditLogDto get(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bản ghi audit"));
    }

    /**
     * Ghi nhận một lần đăng nhập thất bại. Lúc này chưa có người dùng đăng nhập nên không dùng execute();
     * ghi trong transaction riêng để không bị rollback cùng yêu cầu đăng nhập, và không bao giờ làm hỏng luồng đăng nhập.
     */
    public void recordLoginFailure(User knownUser, String attemptedUsername, String reason) {
        String name = attemptedUsername == null ? "" : attemptedUsername.trim();
        if (name.isEmpty()) {
            name = "unknown";
        }
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        final String actorName = name;
        try {
            failureTransaction.executeWithoutResult(status -> {
                AuditLog row = new AuditLog();
                row.setActorUserId(knownUser == null ? 0L : knownUser.getId());
                row.setActorUsername(actorName);
                row.setActorRole(knownUser == null ? "ANONYMOUS" : knownUser.getRole().name());
                row.setAction("LOGIN_FAILED");
                row.setEntityType("AUTH");
                row.setEntityId(null);
                row.setOccurredAt(LocalDateTime.now());
                row.setResult("FAILURE");
                row.setCorrelationId(correlationId());
                row.setDetailsJson(serialize(Map.of("errorCode", reason)));
                repository.saveAndFlush(row);
            });
        } catch (RuntimeException ex) {
            log.error("Không ghi được audit đăng nhập thất bại cho user={}", actorName, ex);
        }
    }

    private void save(User actor, String action, String entityType, AuditContext context,
                      String result, String errorCode) {
        AuditLog row = new AuditLog();
        row.setActorUserId(actor.getId());
        row.setActorUsername(actor.getUsername());
        row.setActorRole(actor.getRole().name());
        row.setAction(action);
        row.setEntityType(entityType);
        row.setEntityId(context.entityId);
        row.setOccurredAt(LocalDateTime.now());
        row.setResult(result);
        row.setCorrelationId(correlationId());
        row.setDetailsJson(serialize(context.details(errorCode)));
        repository.saveAndFlush(row);
    }

    private AuditLogDtos.AuditLogDto toDto(AuditLog row) {
        try {
            JsonNode details = objectMapper.readTree(row.getDetailsJson());
            return new AuditLogDtos.AuditLogDto(row.getId(), row.getActorUserId(), row.getActorUsername(),
                    row.getActorRole(), row.getAction(), row.getEntityType(), row.getEntityId(),
                    row.getOccurredAt(), row.getResult(), row.getCorrelationId(), details);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Audit log chứa JSON không hợp lệ", ex);
        }
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể tuần tự hóa audit log", ex);
        }
    }

    private static Map<String, Object> safeSnapshot(Map<String, ?> input) {
        Map<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (SAFE_DETAIL_FIELDS.contains(key) && (value == null
                    || value instanceof Number || value instanceof Boolean || value instanceof String)) {
                String safeValue = value instanceof String string ? string.substring(0, Math.min(string.length(), 160)) : null;
                safe.put(key, safeValue == null ? value : safeValue);
            }
        });
        return safe;
    }

    private String failureCode(RuntimeException failure) {
        if (failure instanceof ApiException apiException) {
            return "HTTP_" + apiException.getStatus().value();
        }
        if (failure instanceof DataAccessException) {
            return "PERSISTENCE_ERROR";
        }
        return "OPERATION_FAILED";
    }

    private String correlationId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        String header = attributes.getRequest().getHeader("X-Correlation-ID");
        return header != null && header.matches("[A-Za-z0-9._:-]{1,100}") ? header : null;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static final class AuditContext {
        private String entityId;
        private Map<String, ?> before = Map.of();
        private Map<String, ?> after = Map.of();

        public AuditContext(String entityId) {
            this.entityId = entityId;
        }

        public void entityId(Object id) {
            if (id != null) {
                entityId = String.valueOf(id);
            }
        }

        public void changes(Map<String, ?> before, Map<String, ?> after) {
            this.before = before == null ? Map.of() : before;
            this.after = after == null ? Map.of() : after;
        }

        private Map<String, Object> details(String errorCode) {
            Map<String, Object> details = new LinkedHashMap<>();
            if (!before.isEmpty()) {
                details.put("before", safeSnapshot(before));
            }
            if (!after.isEmpty()) {
                details.put("after", safeSnapshot(after));
            }
            if (errorCode != null) {
                details.put("errorCode", errorCode);
            }
            return details;
        }
    }
}
