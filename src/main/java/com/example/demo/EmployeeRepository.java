package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    // Example custom query
    List<Employee> findByEmailContaining(String email);
    // Example native query
    @Query(value = "SELECT * FROM DPS_USER WHERE FIRST_NAME LIKE %:name%", nativeQuery = true)
    List<Employee> findByNameContaining(@Param("name") String name);
}
