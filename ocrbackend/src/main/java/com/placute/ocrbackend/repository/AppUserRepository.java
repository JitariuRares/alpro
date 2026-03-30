package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findTopByUsernameOrderByIdDesc(String username);
    List<AppUser> findAllByUsernameOrderByIdDesc(String username);
    boolean existsByUsername(String username);
}
