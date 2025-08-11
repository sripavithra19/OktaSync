package com.example.demo;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
 
import java.util.List;
import java.util.stream.Collectors;
 
@RestController
@RequestMapping("/employees")
public class EmployeeController {
    @Autowired
    private EmployeeRepository employeeRepository;
    @GetMapping
    public List<Employee> getAllEmployees(JwtAuthenticationToken authn) {
        System.out.println(((Jwt)authn.getPrincipal()).getClaims());
        return employeeRepository.findAll().stream()
            .map(this::transformToApiFormat)
            .collect(Collectors.toList());
    }
    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable int id) {
        Employee dbEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        return transformToApiFormat(dbEmployee);
    }
    private Employee transformToApiFormat(Employee dbEmployee) {
        Employee apiEmployee = new Employee(
            dbEmployee.getId(),
            dbEmployee.getLogin(),
            dbEmployee.getFirstName(),
            dbEmployee.getLastName(),
            dbEmployee.getEmail()
        );
    return apiEmployee;
    }
}
