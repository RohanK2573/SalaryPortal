package com.incubyte.salarymanagement.salaries;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, UUID> {
    @Query("select s from SalaryRecord s where s.employee.id in :employeeIds and s.effectiveDate = " +
            "(select max(s2.effectiveDate) from SalaryRecord s2 where s2.employee.id = s.employee.id and s2.effectiveDate <= :today)")
    List<SalaryRecord> findCurrentForEmployees(@Param("employeeIds") Collection<UUID> employeeIds,
            @Param("today") LocalDate today);

    @Query("select s from SalaryRecord s where s.employee.id = :employeeId and s.effectiveDate = " +
            "(select max(s2.effectiveDate) from SalaryRecord s2 where s2.employee.id = :employeeId and s2.effectiveDate <= :today)")
    Optional<SalaryRecord> findCurrentForEmployee(@Param("employeeId") UUID employeeId,
            @Param("today") LocalDate today);

    @Query("select s from SalaryRecord s where s.employee.id = :employeeId")
    Page<SalaryRecord> findByEmployee(@Param("employeeId") UUID employeeId, Pageable pageable);

    @Query("select s from SalaryRecord s where s.employee.id = :employeeId and s.id = :id")
    Optional<SalaryRecord> findForEmployee(@Param("employeeId") UUID employeeId, @Param("id") UUID id);

    @Query("select (count(s) > 0) from SalaryRecord s where s.employee.id = :employeeId and s.effectiveDate = :date")
    boolean existsForEmployeeOnDate(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date);

    @Query("select (count(s) > 0) from SalaryRecord s where s.employee.id = :employeeId and s.effectiveDate = :date and s.id <> :id")
    boolean existsForOtherRecordOnDate(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date,
            @Param("id") UUID id);
}
