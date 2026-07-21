package com.kushan.aut_svc.repository;

import com.kushan.aut_svc.Model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

	Optional<Session> findByJti(String jti);

	List<Session> findByUserIdAndActiveTrueOrderByLastSeenDesc(Long userId);

	long countByUserIdAndActiveTrue(Long userId);

	void deleteByUserIdAndActiveTrue(Long userId);

	long countByJtiAndActiveTrue(String jti);
}
