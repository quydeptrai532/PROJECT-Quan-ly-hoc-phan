package com.edu.registration.entity;

public final class NotificationEventType {

    private NotificationEventType() {
    }

    // TASK-ST-07: Registration Events
    public static final String COURSE_REGISTRATION_CONFIRMED = "COURSE_REGISTRATION_CONFIRMED";
    public static final String COURSE_REGISTRATION_CANCELLED = "COURSE_REGISTRATION_CANCELLED";
    public static final String COURSE_SECTION_CANCELLED = "COURSE_SECTION_CANCELLED";
    public static final String COURSE_SESSION_CANCELLED = "COURSE_SESSION_CANCELLED";
    public static final String COURSE_SECTION_SCHEDULE_CHANGED = "COURSE_SECTION_SCHEDULE_CHANGED";
    public static final String COURSE_SECTION_ASSIGNMENT_CHANGED = "COURSE_SECTION_ASSIGNMENT_CHANGED";
    public static final String REGISTRATION_WINDOW_CLOSING = "REGISTRATION_WINDOW_CLOSING";
    public static final String CLASS_STUDENT_TRANSFERRED = "CLASS_STUDENT_TRANSFERRED";

    // TASK-ST-09: Grade Publication Events
    public static final String GRADES_PUBLISHED = "GRADES_PUBLISHED";
    public static final String GRADE_APPEAL_PROCESSED = "GRADE_APPEAL_PROCESSED";

    // TASK-LC-03: Class Announcements
    public static final String CLASS_ANNOUNCEMENT = "CLASS_ANNOUNCEMENT";

    // Attendance Events
    public static final String ATTENDANCE_THRESHOLD_ALERT = "ATTENDANCE_THRESHOLD_ALERT";
    public static final String ATTENDANCE_ALERT_RESOLVED = "ATTENDANCE_ALERT_RESOLVED";

    // Support Request Events
    public static final String SUPPORT_REQUEST_ASSIGNED = "SUPPORT_REQUEST_ASSIGNED";
    public static final String SUPPORT_REQUEST_STATUS_CHANGED = "SUPPORT_REQUEST_STATUS_CHANGED";
    public static final String SUPPORT_REQUEST_EVIDENCE_REQUESTED = "SUPPORT_REQUEST_EVIDENCE_REQUESTED";
    public static final String SUPPORT_REQUEST_COMMENT_ADDED = "SUPPORT_REQUEST_COMMENT_ADDED";
}
