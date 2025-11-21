package com.example.test_executor;

import java.util.List;

import com.example.bpmnai.core.domain.TestExecution;
import com.example.bpmnai.core.domain.TestScenario;

public class TestExecutorRunner {
    
    public static TestExecution runTestsWithData(List<MatchedTask> matchedTasks) {
        try {
            System.out.println("=== ЗАПУСК ТЕСТИРОВАНИЯ С ПЕРЕДАННЫМИ ДАННЫМИ ===\n");
            System.out.println("✅ Получено задач напрямую: " + matchedTasks.size());
            
            // Показываем какие задачи получили
            for (MatchedTask task : matchedTasks) {
                System.out.println("   - " + task.getTaskName() + " -> " + task.getHttpMethod() + " " + task.getEndpointUrl());
            }
            
            // Используем переданные данные вместо загрузки из файла
            TestScenario scenario = ScenarioGenerator.generateScenarioFromMatchedTasks(matchedTasks);
            System.out.println("✅ Сгенерирован сценарий: " + scenario.getName());
            
            TestExecutorService executor = new TestExecutorService();
            System.out.println("🎯 ВЫПОЛНЕНИЕ ТЕСТОВ...");
            TestExecution result = executor.executeScenario(scenario);
            
            System.out.println("📊 ОТЧЕТ О ТЕСТИРОВАНИИ:");
            ReportGenerator.printReport(result);
            
            return result;
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}