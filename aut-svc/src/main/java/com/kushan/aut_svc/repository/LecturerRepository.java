package com.kushan.aut_svc.repository;

import com.kushan.aut_svc.Model.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

    Optional<Lecturer> findByEmail(String email);

    List<Lecturer> findByStatus(String status);

    long countByStatus(String status);

    List<Lecturer> findByStatusNot(String status);
}