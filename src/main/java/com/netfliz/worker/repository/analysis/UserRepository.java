package com.netfliz.worker.repository.analysis;

import com.netfliz.worker.entity.analysis.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByIsPremiumTrue();
}
