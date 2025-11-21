// test-executor/src/main/java/com/example/test_executor/ReportGenerator.java
package com.example.test_executor;

import java.util.List;

import com.example.bpmnai.core.domain.StepExecution;
import com.example.bpmnai.core.domain.TestExecution;
import com.example.bpmnai.core.domain.TestStatus;

public class ReportGenerator {

    public static void printReport(TestExecution execution) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 СТРУКТУРИРОВАННЫЙ ОТЧЕТ О ТЕСТИРОВАНИИ");
        System.out.println("=".repeat(60));
        
        System.out.println("Сценарий: " + execution.getScenario().getName());
        System.out.println("Финальный статус: " + getStatusWithIcon(execution.getStatus()));
        System.out.println("Всего шагов: " + execution.getStepExecutions().size());
        
        // Статистика
        long successCount = execution.getStepExecutions().stream()
            .filter(StepExecution::isSuccess)
            .count();
        long failedCount = execution.getStepExecutions().size() - successCount;
        
        System.out.println("Успешных: " + successCount + " | Ошибочных: " + failedCount);
        
        if (execution.getErrorMessage() != null) {
            System.out.println("❌ Общая ошибка: " + execution.getErrorMessage());
        }
        
        System.out.println("\n" + "-".repeat(60));
        System.out.println("ДЕТАЛЬНЫЕ РЕЗУЛЬТАТЫ ПО ШАГАМ:");
        System.out.println("-".repeat(60));

        List<StepExecution> steps = execution.getStepExecutions();
        for (int i = 0; i < steps.size(); i++) {
            StepExecution stepExec = steps.get(i);
            printStepReport(i + 1, stepExec);
        }
        
        // Рекомендации
        printRecommendations(execution);
    }
    
    private static void printStepReport(int stepNumber, StepExecution stepExec) {
        System.out.println("\n🔹 Шаг " + stepNumber + ": " + stepExec.getStepName());
        System.out.println("   Статус: " + (stepExec.isSuccess() ? "✅ УСПЕХ" : "❌ ОШИБКА"));
        
        if (stepExec.getEndpoint() != null) {
            System.out.println("   URL: " + stepExec.getMethod() + " " + stepExec.getEndpoint());
        }
        
        System.out.println("   Код ответа: " + stepExec.getResponseStatus());
        
        if (stepExec.getRequestData() != null) {
            System.out.println("   Тело запроса: " + 
                (stepExec.getRequestData().length() > 100 ? 
                 stepExec.getRequestData().substring(0, 100) + "..." : stepExec.getRequestData()));
        }
        
        if (stepExec.getResponseData() != null) {
            String responsePreview = stepExec.getResponseData().length() > 150 ? 
                stepExec.getResponseData().substring(0, 150) + "..." : stepExec.getResponseData();
            System.out.println("   Тело ответа: " + responsePreview);
        }
        
        if (stepExec.getErrorMessage() != null) {
            System.out.println("   ❌ Ошибка: " + stepExec.getErrorMessage());
        }
        
        System.out.println("   " + "-".repeat(40));
    }
    
    private static void printRecommendations(TestExecution execution) {
        System.out.println("\n💡 РЕКОМЕНДАЦИИ И АНАЛИЗ:");
        System.out.println("-".repeat(60));
        
        List<StepExecution> steps = execution.getStepExecutions();
        boolean hasAuthIssues = false;
        boolean has404Errors = false;
        boolean hasDataExtractionIssues = false;
        
        for (StepExecution step : steps) {
            if (!step.isSuccess()) {
                if (step.getResponseStatus() == 401 || step.getResponseStatus() == 403) {
                    hasAuthIssues = true;
                    System.out.println("🔐 Проблема аутентификации в шаге: " + step.getStepName());
                    System.out.println("   Рекомендация: Проверьте client_id и client_secret");
                }
                
                if (step.getResponseStatus() == 404) {
                    has404Errors = true;
                    System.out.println("🔗 Неверный URL в шаге: " + step.getStepName());
                    System.out.println("   Рекомендация: Проверьте путь API: " + step.getEndpoint());
                }
                
                if (step.getResponseStatus() == 400) {
                    System.out.println("📝 Ошибка валидации в шаге: " + step.getStepName());
                    System.out.println("   Рекомендация: Проверьте тело запроса и параметры");
                }
            }
        }
        
        // Общие рекомендации
        if (!hasAuthIssues && !has404Errors) {
            System.out.println("✅ Все основные проверки пройдены успешно");
        }
        
        System.out.println("\n📈 СТАТИСТИКА:");
        long successCount = steps.stream().filter(StepExecution::isSuccess).count();
        double successRate = (double) successCount / steps.size() * 100;
        System.out.printf("   Общий успех: %.1f%% (%d/%d шагов)%n", successRate, successCount, steps.size());
        
        if (successRate == 100) {
            System.out.println("   🎉 Отличный результат! Все тесты прошли успешно!");
        } else if (successRate >= 70) {
            System.out.println("   👍 Хороший результат, но есть что улучшить");
        } else {
            System.out.println("   ⚠️ Нужно поработать над улучшением тестов");
        }
    }
    
    private static String getStatusWithIcon(TestStatus status) {
        switch (status) {
            case SUCCESS: return "✅ SUCCESS";
            case FAILED: return "❌ FAILED"; 
            case ERROR: return "💥 ERROR";
            case RUNNING: return "🔄 RUNNING";
            default: return "📝 " + status;
        }
    }
}