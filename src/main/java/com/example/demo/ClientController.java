package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client")
public class ClientController {

    private final EmployeeClientService employeeClientService;

    public ClientController(EmployeeClientService employeeClientService) {
        this.employeeClientService = employeeClientService;
    }

    // Existing endpoint
    @GetMapping("/employees")
    public String getAllEmployees() {
        return employeeClientService.getEmployees();
    }

    @GetMapping("/employees/{id}")
    public String getEmployeeById(@PathVariable String id) {
        return employeeClientService.getEmployeeById(id);
    }
}