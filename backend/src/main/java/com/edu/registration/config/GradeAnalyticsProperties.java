package com.edu.registration.config;

import com.edu.registration.service.AcademicRules;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cấu hình tập trung cho thống kê điểm lớp và phân bố điểm (QLSV-75 / TASK-LC-10).
 * Khoảng phân bố điểm và chính sách xử lý bản ghi chưa có điểm đang áp dụng cấu hình tập trung dự kiến
 * và chờ Chủ dự án chốt chính thức (không gọi là quy chế chính thức).
 */
@ConfigurationProperties(prefix = "app.grade-analytics")
public class GradeAnalyticsProperties {

    public static final String DEFAULT_MISSING_GRADE_POLICY = "EXCLUDE_FROM_DENOMINATOR";
    public static final String DEFAULT_POLICY_NOTE =
            "Khoảng phân bố điểm ([0.0, 4.0), [4.0, 5.0), [5.0, 6.5), [6.5, 8.0), [8.0, 10.0]) và chính sách "
                    + "loại bản ghi chưa có điểm tổng kết công bố khỏi mẫu số tính điểm trung bình / tỷ lệ đạt-trượt "
                    + "(EXCLUDE_FROM_DENOMINATOR — không tự gán 0.0) đang áp dụng theo cấu hình tập trung dự kiến và "
                    + "chờ Chủ dự án chốt chính thức (không phải quy chế chính thức). Ngưỡng đạt lấy từ AcademicRules.DIEM_DAT = 5.0.";

    private String missingGradePolicy = DEFAULT_MISSING_GRADE_POLICY;
    private boolean officialRegulation = false;
    private String policyNote = DEFAULT_POLICY_NOTE;
    private List<BucketDef> buckets = defaultBuckets();

    public GradeAnalyticsProperties() {
    }

    public GradeAnalyticsProperties(String missingGradePolicy,
                                    boolean officialRegulation,
                                    String policyNote,
                                    List<BucketDef> buckets) {
        if (missingGradePolicy != null && !missingGradePolicy.isBlank()) {
            this.missingGradePolicy = missingGradePolicy.trim();
        }
        this.officialRegulation = officialRegulation;
        if (policyNote != null && !policyNote.isBlank()) {
            this.policyNote = policyNote.trim();
        }
        if (buckets != null && !buckets.isEmpty()) {
            this.buckets = List.copyOf(buckets);
        }
    }

    public static List<BucketDef> defaultBuckets() {
        return List.of(
                new BucketDef(
                        "KEM",
                        "Dưới 4.0",
                        "[0.0, 4.0)",
                        new BigDecimal("0.0"),
                        new BigDecimal("4.0"),
                        true,
                        false
                ),
                new BucketDef(
                        "YEU",
                        "4.0 đến dưới 5.0",
                        "[4.0, 5.0)",
                        new BigDecimal("4.0"),
                        new BigDecimal("5.0"),
                        true,
                        false
                ),
                new BucketDef(
                        "TRUNG_BINH",
                        "5.0 đến dưới 6.5",
                        "[5.0, 6.5)",
                        new BigDecimal("5.0"),
                        new BigDecimal("6.5"),
                        true,
                        false
                ),
                new BucketDef(
                        "KHA",
                        "6.5 đến dưới 8.0",
                        "[6.5, 8.0)",
                        new BigDecimal("6.5"),
                        new BigDecimal("8.0"),
                        true,
                        false
                ),
                new BucketDef(
                        "GIOI_XUAT_SAC",
                        "8.0 đến 10.0",
                        "[8.0, 10.0]",
                        new BigDecimal("8.0"),
                        new BigDecimal("10.0"),
                        true,
                        true
                )
        );
    }

    public String getMissingGradePolicy() {
        return missingGradePolicy;
    }

    public void setMissingGradePolicy(String missingGradePolicy) {
        if (missingGradePolicy != null && !missingGradePolicy.isBlank()) {
            this.missingGradePolicy = missingGradePolicy.trim();
        }
    }

    public boolean isOfficialRegulation() {
        return officialRegulation;
    }

    public void setOfficialRegulation(boolean officialRegulation) {
        this.officialRegulation = officialRegulation;
    }

    public String getPolicyNote() {
        return policyNote;
    }

    public void setPolicyNote(String policyNote) {
        if (policyNote != null && !policyNote.isBlank()) {
            this.policyNote = policyNote.trim();
        }
    }

    public List<BucketDef> getBuckets() {
        return buckets == null || buckets.isEmpty() ? defaultBuckets() : buckets;
    }

    public void setBuckets(List<BucketDef> buckets) {
        if (buckets != null && !buckets.isEmpty()) {
            this.buckets = List.copyOf(buckets);
        }
    }

    public record BucketDef(
            String maKhoang,
            String tenKhoang,
            String kyHieuKhoang,
            BigDecimal tuDiem,
            BigDecimal denDiem,
            boolean baoGomTuDiem,
            boolean baoGomDenDiem
    ) {
        public boolean contains(BigDecimal score) {
            if (score == null) {
                return false;
            }
            int cmpMin = score.compareTo(tuDiem);
            int cmpMax = score.compareTo(denDiem);
            boolean lowerOk = baoGomTuDiem ? cmpMin >= 0 : cmpMin > 0;
            boolean upperOk = baoGomDenDiem ? cmpMax <= 0 : cmpMax < 0;
            return lowerOk && upperOk;
        }

        public boolean isLaKhoangDat() {
            return tuDiem.compareTo(BigDecimal.valueOf(AcademicRules.DIEM_DAT)) >= 0;
        }
    }
}
