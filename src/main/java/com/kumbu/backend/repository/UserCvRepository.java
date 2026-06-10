package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.UserCv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserCvRepository extends JpaRepository<UserCv, UUID> {

    List<UserCv> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
