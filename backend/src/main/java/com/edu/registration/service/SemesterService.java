package com.edu.registration.service;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.entity.Semester;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.SemesterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SemesterService {

    private final SemesterRepository hocKyRepository;
    private final DtoMapper mapper;

    public SemesterService(SemesterRepository hocKyRepository, DtoMapper mapper) {
        this.hocKyRepository = hocKyRepository;
        this.mapper = mapper;
    }

    public List<CatalogDtos.HocKyDto> danhSach() {
        return hocKyRepository.findAllByOrderByNgayBatDauDesc().stream().map(mapper::hocKy).toList();
    }

    public Semester hocKyHienTai() {
        LocalDate homNay = LocalDate.now();
        return hocKyRepository
                .findFirstByDangKyTuLessThanEqualAndDangKyDenGreaterThanEqualOrderByDangKyDenDesc(homNay, homNay)
                .or(() -> hocKyRepository.findFirstByOrderByDangKyDenDesc())
                .orElse(null);
    }

    public Semester lay(Long id) {
        return hocKyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
    }

    public Semester layHoacHienTai(Long id) {
        if (id != null) {
            return lay(id);
        }
        Semester hocKy = hocKyHienTai();
        if (hocKy == null) {
            throw ApiException.notFound("Chưa có học kỳ nào trong hệ thống");
        }
        return hocKy;
    }
}
