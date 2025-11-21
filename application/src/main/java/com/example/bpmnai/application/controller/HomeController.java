package com.example.bpmnai.application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Система запущена и готова к работе!");
        model.addAttribute("status", "Ожидание загрузки файлов");
        return "index";
    }


    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("message", "Отчеты по тестированию");
        model.addAttribute("testScenario", UploadController.lastTestScenario); // передаём последние тесты
        return "reports";
    }
}