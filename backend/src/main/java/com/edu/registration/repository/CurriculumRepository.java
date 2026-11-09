package com.edu.registration.repository;
import com.edu.registration.entity.Curriculum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CurriculumRepository extends JpaRepository<Curriculum,Long> {
 @EntityGraph(attributePaths={"nganh","nhom","nhom.monHoc","nhom.monHoc.khoa"}) Optional<Curriculum> findWithDetailsById(Long id);
 @EntityGraph(attributePaths={"nganh","nhom","nhom.monHoc"}) List<Curriculum> findAll();
}
