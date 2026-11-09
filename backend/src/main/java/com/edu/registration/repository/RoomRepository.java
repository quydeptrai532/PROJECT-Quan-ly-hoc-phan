
package com.edu.registration.repository;

import com.edu.registration.entity.Room;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByOrderByMaAsc();

    Optional<Room> findByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCase(String ma);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Room p where upper(p.ma) = upper(:ma)")
    Optional<Room> findByMaForUpdate(@Param("ma") String ma);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Room p where p.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
