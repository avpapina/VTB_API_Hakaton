package com.example.bpmnai.application.controller;

import com.example.bpmnai.core.domain.StepExecution;
import com.example.bpmnai.core.domain.TestExecution;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        return "index";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        TestExecution testExecution = UploadController.lastTestExecution;

        if (testExecution != null) {
            // Генерация рекомендаций
            List<String> recommendations = new ArrayList<>();
            List<StepExecution> steps = testExecution.getStepExecutions();

            for (StepExecution step : steps) {
                if (!step.isSuccess()) {
                    if (step.getResponseStatus() == 404) {
                        recommendations.add("🔗 Неверный URL в шаге: " + step.getStepName() +
                                " - Проверьте путь API: " + step.getEndpoint());
                    }
                    if (step.getResponseStatus() == 401 || step.getResponseStatus() == 403) {
                        recommendations.add("🔐 Проблема аутентификации в шаге: " + step.getStepName());
                    }
                    if (step.getResponseStatus() == 400) {
                        recommendations.add("📝 Ошибка валидации в шаге: " + step.getStepName());
                    }
                }
            }

            // Общие рекомендации
            long successCount = steps.stream().filter(StepExecution::isSuccess).count();
            double successRate = steps.size() > 0 ? (double) successCount / steps.size() * 100 : 0;

            if (successRate == 100) {
                recommendations.add("🎉 Отличный результат! Все тесты прошли успешно!");
            } else if (successRate >= 70) {
                recommendations.add("👍 Хороший результат, но есть что улучшить");
            } else {
                recommendations.add("⚠️ Нужно поработать над улучшением тестов");
            }

            model.addAttribute("testExecution", testExecution);
            model.addAttribute("recommendations", recommendations);
            model.addAttribute("generatedChains", UploadController.generatedChains);
            model.addAttribute("generatedData", UploadController.generatedData);
        } else {
            model.addAttribute("message", "Нет данных для отчета");
        }

        return "reports";
    }


}