package com.edu.registration.repository;
import com.edu.registration.entity.PersonalPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PersonalPlanRepository extends JpaRepository<PersonalPlan,Long> {
 @EntityGraph(attributePaths={"mon","mon.lopHocPhan","mon.lopHocPhan.monHoc","mon.lopHocPhan.hocKy"}) List<PersonalPlan> findBySinhVienIdOrderByCapNhatLucDesc(Long sinhVienId);
 @EntityGraph(attributePaths={"mon","mon.lopHocPhan","mon.lopHocPhan.monHoc","mon.lopHocPhan.hocKy"}) Optional<PersonalPlan> findByIdAndSinhVienId(Long id,Long sinhVienId);
}
