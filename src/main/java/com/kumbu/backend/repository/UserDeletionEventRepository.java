package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.UserDeletionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeletionEventRepository extends JpaRepository<UserDeletionEvent, Long> {
}
