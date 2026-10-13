package com.incubyte.salarymanagement.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrUserRepository extends JpaRepository<HrUser, Short> {
    Optional<HrUser> findByEmail(String email);
}
