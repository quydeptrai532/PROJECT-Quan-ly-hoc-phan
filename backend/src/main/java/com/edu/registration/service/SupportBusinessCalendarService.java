package com.edu.registration.service;

import com.edu.registration.config.SupportRequestProperties;
import com.edu.registration.dto.SupportRequestDtos;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.SupportRequest;
import com.edu.registration.entity.SupportRequestPriority;
import com.edu.registration.entity.SupportRequestStatus;
import com.edu.registration.entity.SupportRequestStatusHistory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class SupportBusinessCalendarService {

    private final SupportRequestProperties properties;

    public SupportBusinessCalendarService(SupportRequestProperties properties) {
        this.properties = properties != null ? properties : SupportRequestProperties.defaults();
    }

    public boolean isWorkingDay(LocalDate date) {
        if (date == null) {
            return false;
        }
        SupportRequestProperties.WorkingCalendarProperties cal = properties.workingCalendar();
        if (!cal.workingDays().contains(date.getDayOfWeek())) {
            return false;
        }
        return !cal.holidays().contains(date);
    }

    public long calculateWorkingMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0L;
        }

        SupportRequestProperties.WorkingCalendarProperties cal = properties.workingCalendar();
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        long totalMinutes = 0L;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (isWorkingDay(current)) {
                // Ca sáng: cal.startTime() -> cal.lunchStartTime()
                LocalDateTime morningStart = current.atTime(cal.startTime());
                LocalDateTime morningEnd = current.atTime(cal.lunchStartTime());
                totalMinutes += overlapMinutes(start, end, morningStart, morningEnd);

                // Ca chiều: cal.lunchEndTime() -> cal.endTime()
                LocalDateTime afternoonStart = current.atTime(cal.lunchEndTime());
                LocalDateTime afternoonEnd = current.atTime(cal.endTime());
                totalMinutes += overlapMinutes(start, end, afternoonStart, afternoonEnd);
            }
            current = current.plusDays(1);
        }

        return totalMinutes;
    }

    private long overlapMinutes(LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                LocalDateTime slotStart, LocalDateTime slotEnd) {
        LocalDateTime effectiveStart = rangeStart.isAfter(slotStart) ? rangeStart : slotStart;
        LocalDateTime effectiveEnd = rangeEnd.isBefore(slotEnd) ? rangeEnd : slotEnd;
        if (effectiveEnd.isAfter(effectiveStart)) {
            return Duration.between(effectiveStart, effectiveEnd).toMinutes();
        }
        return 0L;
    }

    public LocalDateTime addWorkingHours(LocalDateTime start, double workingHours) {
        if (start == null) {
            return null;
        }
        long minutesNeeded = Math.max(0, Math.round(workingHours * 60));
        if (minutesNeeded == 0) {
            return start;
        }

        SupportRequestProperties.WorkingCalendarProperties cal = properties.workingCalendar();
        LocalDateTime cursor = start;

        while (minutesNeeded > 0) {
            LocalDate day = cursor.toLocalDate();
            if (!isWorkingDay(day)) {
                cursor = day.plusDays(1).atTime(cal.startTime());
                continue;
            }

            LocalDateTime morningStart = day.atTime(cal.startTime());
            LocalDateTime morningEnd = day.atTime(cal.lunchStartTime());
            LocalDateTime afternoonStart = day.atTime(cal.lunchEndTime());
            LocalDateTime afternoonEnd = day.atTime(cal.endTime());

            if (cursor.isBefore(morningStart)) {
                cursor = morningStart;
            }

            // Nếu đang trong ca sáng
            if (cursor.isBefore(morningEnd)) {
                long available = Duration.between(cursor, morningEnd).toMinutes();
                if (minutesNeeded <= available) {
                    return cursor.plusMinutes(minutesNeeded);
                }
                minutesNeeded -= available;
                cursor = afternoonStart;
            } else if (cursor.isBefore(afternoonStart)) {
                cursor = afternoonStart;
            }

            // Nếu đang trong ca chiều
            if (cursor.isBefore(afternoonEnd)) {
                long available = Duration.between(cursor, afternoonEnd).toMinutes();
                if (minutesNeeded <= available) {
                    return cursor.plusMinutes(minutesNeeded);
                }
                minutesNeeded -= available;
                cursor = day.plusDays(1).atTime(cal.startTime());
            } else {
                cursor = day.plusDays(1).atTime(cal.startTime());
            }
        }

        return cursor;
    }

    public SupportRequestDtos.SupportRequestSlaDetailDto calculateSla(
            SupportRequest yeuCau,
            List<SupportRequestStatusHistory> lichSu,
            LocalDateTime now
    ) {
        if (yeuCau == null) {
            return null;
        }
        LocalDateTime currentTime = now != null ? now : LocalDateTime.now();

        List<SupportRequestStatusHistory> sortedHistory = new ArrayList<>(
                lichSu != null ? lichSu : List.of()
        );
        sortedHistory.sort(Comparator.comparing(SupportRequestStatusHistory::getThoiDiem)
                .thenComparing(SupportRequestStatusHistory::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        List<SupportRequestDtos.TrangThaiSlaThoiGianDto> thoiGianTheoTrangThai = new ArrayList<>();
        long pausedWorkingMinutes = 0L;

        if (sortedHistory.isEmpty()) {
            LocalDateTime start = yeuCau.getCreatedAt();
            long calMin = Math.max(0, ChronoUnit.MINUTES.between(start, currentTime));
            long workMin = calculateWorkingMinutes(start, currentTime);
            boolean paused = yeuCau.getTrangThai() == SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN
                    && properties.sla().pauseOnStudentWait();
            if (paused) {
                pausedWorkingMinutes += workMin;
            }
            thoiGianTheoTrangThai.add(new SupportRequestDtos.TrangThaiSlaThoiGianDto(
                    yeuCau.getTrangThai().name(),
                    yeuCau.getTrangThai().getMoTa(),
                    start,
                    currentTime,
                    calMin,
                    workMin,
                    paused
            ));
        } else {
            for (int i = 0; i < sortedHistory.size(); i++) {
                SupportRequestStatusHistory current = sortedHistory.get(i);
                SupportRequestStatus status = current.getTrangThaiMoi();
                LocalDateTime start = current.getThoiDiem();
                LocalDateTime end = (i + 1 < sortedHistory.size())
                        ? sortedHistory.get(i + 1).getThoiDiem()
                        : (isTerminal(yeuCau.getTrangThai())
                        ? (yeuCau.getThoiDiemGiaiQuyet() != null ? yeuCau.getThoiDiemGiaiQuyet()
                        : (yeuCau.getThoiDiemDong() != null ? yeuCau.getThoiDiemDong() : yeuCau.getUpdatedAt()))
                        : currentTime);

                if (end.isBefore(start)) {
                    end = start;
                }
                long calMin = Math.max(0, ChronoUnit.MINUTES.between(start, end));
                long workMin = calculateWorkingMinutes(start, end);
                boolean paused = status == SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN
                        && properties.sla().pauseOnStudentWait();
                if (paused) {
                    pausedWorkingMinutes += workMin;
                }
                thoiGianTheoTrangThai.add(new SupportRequestDtos.TrangThaiSlaThoiGianDto(
                        status.name(),
                        status.getMoTa(),
                        start,
                        end,
                        calMin,
                        workMin,
                        paused
                ));
            }
        }

        // Target due date
        LocalDateTime targetDueAt = yeuCau.getHanXuLy();
        if (targetDueAt == null) {
            SupportRequestPriority priority = yeuCau.getMucUuTien() != null
                    ? yeuCau.getMucUuTien() : SupportRequestPriority.TRUNG_BINH;
            double resolveHours = properties.sla().targetResolveHours()
                    .getOrDefault(priority, 72.0);
            targetDueAt = addWorkingHours(yeuCau.getCreatedAt(), resolveHours);
        }

        // First response
        LocalDateTime firstResponseAt = yeuCau.getThoiDiemPhanHoiDau();
        Long firstResponseWorkingMinutes = null;
        if (firstResponseAt == null) {
            for (SupportRequestStatusHistory entry : sortedHistory) {
                if (entry.getVaiTroNguoiThucHien() != null && entry.getVaiTroNguoiThucHien() != Role.SINH_VIEN) {
                    firstResponseAt = entry.getThoiDiem();
                    break;
                }
            }
        }
        if (firstResponseAt != null) {
            firstResponseWorkingMinutes = calculateWorkingMinutes(yeuCau.getCreatedAt(), firstResponseAt);
        }

        LocalDateTime resolvedAt = yeuCau.getThoiDiemGiaiQuyet();
        LocalDateTime effectiveEnd = resolvedAt != null ? resolvedAt
                : (yeuCau.getThoiDiemDong() != null ? yeuCau.getThoiDiemDong() : currentTime);

        long totalWorkingMinutes = calculateWorkingMinutes(yeuCau.getCreatedAt(), effectiveEnd);
        long activeWorkingMinutes = Math.max(0, totalWorkingMinutes - pausedWorkingMinutes);

        boolean isTerminal = isTerminal(yeuCau.getTrangThai());
        boolean isOverdue;
        String slaStatus;
        String tenSlaStatus;
        Long remainingWorkingMinutes = null;
        Long overdueWorkingMinutes = null;

        if (isTerminal) {
            LocalDateTime endForOverdue = resolvedAt != null ? resolvedAt
                    : (yeuCau.getThoiDiemDong() != null ? yeuCau.getThoiDiemDong() : yeuCau.getUpdatedAt());
            isOverdue = targetDueAt != null && endForOverdue.isAfter(targetDueAt);
            if (isOverdue) {
                slaStatus = "RESOLVED_OVERDUE";
                tenSlaStatus = "Đã giải quyết trễ hạn";
                overdueWorkingMinutes = calculateWorkingMinutes(targetDueAt, endForOverdue);
            } else {
                slaStatus = "RESOLVED_ON_TIME";
                tenSlaStatus = "Đã giải quyết đúng hạn";
                overdueWorkingMinutes = 0L;
            }
            remainingWorkingMinutes = 0L;
        } else if (yeuCau.getTrangThai() == SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN) {
            isOverdue = targetDueAt != null && currentTime.isAfter(targetDueAt);
            slaStatus = "PAUSED";
            tenSlaStatus = "Tạm dừng SLA (chờ sinh viên)";
            if (isOverdue) {
                overdueWorkingMinutes = calculateWorkingMinutes(targetDueAt, currentTime);
                remainingWorkingMinutes = 0L;
            } else if (targetDueAt != null) {
                remainingWorkingMinutes = calculateWorkingMinutes(currentTime, targetDueAt);
                overdueWorkingMinutes = 0L;
            }
        } else {
            isOverdue = targetDueAt != null && currentTime.isAfter(targetDueAt);
            if (isOverdue) {
                slaStatus = "OVERDUE";
                tenSlaStatus = "Quá hạn xử lý";
                overdueWorkingMinutes = calculateWorkingMinutes(targetDueAt, currentTime);
                remainingWorkingMinutes = 0L;
            } else {
                remainingWorkingMinutes = targetDueAt != null
                        ? calculateWorkingMinutes(currentTime, targetDueAt)
                        : null;
                overdueWorkingMinutes = 0L;
                double remainingHours = remainingWorkingMinutes != null
                        ? (remainingWorkingMinutes / 60.0) : 999.0;
                if (remainingHours <= properties.sla().nearingDueThresholdHours()) {
                    slaStatus = "NEARING_DUE";
                    tenSlaStatus = "Sắp đến hạn";
                } else {
                    slaStatus = "ON_TIME";
                    tenSlaStatus = "Đang trong hạn";
                }
            }
        }

        return new SupportRequestDtos.SupportRequestSlaDetailDto(
                targetDueAt,
                isOverdue,
                slaStatus,
                tenSlaStatus,
                firstResponseAt,
                firstResponseWorkingMinutes,
                resolvedAt,
                activeWorkingMinutes,
                pausedWorkingMinutes,
                remainingWorkingMinutes,
                overdueWorkingMinutes,
                thoiGianTheoTrangThai,
                properties.sla().officialSlaEnabled(),
                properties.sla().policyNote()
        );
    }

    private boolean isTerminal(SupportRequestStatus status) {
        return status == SupportRequestStatus.DA_GIAI_QUYET
                || status == SupportRequestStatus.TU_CHOI
                || status == SupportRequestStatus.DA_DONG;
    }
}
