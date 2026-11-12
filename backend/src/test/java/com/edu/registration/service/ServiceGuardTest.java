package com.edu.registration.service;

import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceGuardTest {

    @Test
    void dtoMapperRejectsRegistrationWithoutCourseClass() {
        DtoMapper mapper = new DtoMapper();
        CourseRegistration dangKy = new CourseRegistration();

        assertThrows(IllegalStateException.class, () -> mapper.dangKy(dangKy, null));
    }

    @Test
    void gradeServiceRejectsNullGradeRequest() {
        GradeService gradeService = new GradeService(null, null, null, null, null, null, null);

        assertThrows(ApiException.class, () -> gradeService.nhapDiem(1L, null));
    }
}
