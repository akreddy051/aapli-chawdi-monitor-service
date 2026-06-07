package com.example.aapliChawdi.repository;

import com.example.aapliChawdi.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepositoryInterface extends JpaRepository<UserSession, Long> {
}
