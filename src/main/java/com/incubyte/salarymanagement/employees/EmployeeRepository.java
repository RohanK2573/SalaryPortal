package com.incubyte.salarymanagement.employees;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmployeeCode(String employeeCode);
    Optional<Employee> findByEmployeeCode(String employeeCode);
}
