package com.task.intvw.controller;

import com.task.intvw.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @PostMapping("/getLongestPair")
    public String getLongestPair(@RequestParam("file") final MultipartFile file, final Model model) {
        return employeeService.getLongestPair(file, model);
    }
}

