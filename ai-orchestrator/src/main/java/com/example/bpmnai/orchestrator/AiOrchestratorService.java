package com.example.bpmnai.orchestrator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bpmnai.core.domain.ApiEndpoint;
import com.example.bpmnai.core.domain.BpmnProcess;
import com.example.bpmnai.core.domain.BpmnTask;
import com.example.bpmnai.core.domain.OpenApiAnalysisResult;
import com.example.bpmnai.core.domain.TestExecution;
import com.example.bpmnai.llm.GPT2DataGenerator;
import com.example.test_executor.MatchedTask;
import com.example.test_executor.TestExecutorRunner;

@Service
public class AiOrchestratorService {

    private final AdvancedNLPSemanticMatcher semanticMatcher;
    private final GPT2DataGenerator gpt2DataGenerator;

    public AiOrchestratorService(AdvancedNLPSemanticMatcher semanticMatcher, 
                            GPT2DataGenerator gpt2DataGenerator) {
        this.semanticMatcher = semanticMatcher;
        this.gpt2DataGenerator = gpt2DataGenerator;
    }

    public List<TaskEndpointMapping> mapTasksToEndpoints(BpmnProcess bpmnResult,
                                                        OpenApiAnalysisResult openApiResult) {
        if (bpmnResult == null || openApiResult == null) {
            return new ArrayList<>();
        }

        List<TaskEndpointMapping> mappings =
                mapTasksToEndpoints(bpmnResult.getTasks(), openApiResult.getEndpoints());
        
        // ✅ ДОБАВЬ ГЕНЕРАЦИЮ ДАННЫХ ДЛЯ КАЖДОЙ ЗАДАЧИ
        System.out.println("🧠 ГЕНЕРАЦИЯ ТЕСТОВЫХ ДАННЫХ:");
        for (TaskEndpointMapping mapping : mappings) {
            if (mapping.getApiEndpoint() != null) {
                String taskName = mapping.getBpmnTask().getName();
                String endpoint = mapping.getApiEndpoint().getPath();
                String method = mapping.getApiEndpoint().getMethod();
                
                String generatedData = gpt2DataGenerator.generateTestData(taskName, endpoint, method);
                
                // ✅ СОХРАНИ ДАННЫЕ В МAPPING
                mapping.setGeneratedTestData(generatedData);
                
                System.out.println("   ✅ " + taskName + " -> " + generatedData);
            }
        }

        Path file = Paths.get("test-executor", "src", "main", "resources",
                    "matched", "matchedTasks.json")
                        .normalize()
                        .toAbsolutePath();
        String filePath = file.toString();

        MatchedTaskSaver.saveMatchedTasks(mappings, bpmnResult.getId(), filePath);

        runApiTestingWithMappings(mappings, bpmnResult.getId());

        return mappings;
    }

    public void demonstrateAIDataGeneration(List<TaskEndpointMapping> mappings) {
        System.out.println("🧠 ДЕМОНСТРАЦИЯ ГЕНЕРАЦИИ ДАННЫХ ИИ (Phi-3):");
        
        for (TaskEndpointMapping mapping : mappings) {
            if (mapping.getApiEndpoint() != null) {
                String taskName = mapping.getBpmnTask().getName();
                String endpoint = mapping.getApiEndpoint().getPath();
                String method = mapping.getApiEndpoint().getMethod();
                
                System.out.println("\n🔹 Задача: " + taskName);
                String generatedData = gpt2DataGenerator.generateTestData(taskName, endpoint, method);
                System.out.println("   Сгенерированные данные: " + generatedData);
            }
        }
    }

    public void runApiTestingWithMappings(List<TaskEndpointMapping> mappings, String processId) {
        System.out.println("Запускаю тестирование API с новыми данными...");
        
        try {
            // Преобразуем mappings в matched tasks
            List<MatchedTask> matchedTasks = new ArrayList<>();
            for (TaskEndpointMapping mapping : mappings) {
                MatchedTask task = new MatchedTask();
                task.setTaskName(mapping.getBpmnTask().getName());
                if (mapping.getApiEndpoint() != null) {
                    task.setHttpMethod(mapping.getApiEndpoint().getMethod());
                    task.setEndpointUrl(mapping.getApiEndpoint().getPath());
                }
                task.setProcessId(processId);
                
                // ✅ ПЕРЕДАЕМ СГЕНЕРИРОВАННЫЕ ДАННЫЕ
                if (mapping.getGeneratedTestData() != null) {
                    task.setTestData(mapping.getGeneratedTestData());
                    System.out.println("   📦 Данные для " + task.getTaskName() + ": " + mapping.getGeneratedTestData());
                }
                
                matchedTasks.add(task);
            }
            
            // Запускаем тесты напрямую с данными из памяти
            TestExecution result = TestExecutorRunner.runTestsWithData(matchedTasks);
            
            if (result != null) {
                System.out.println("Тестирование завершено со статусом: " + result.getStatus());
            } else {
                System.out.println("Тестирование завершилось с ошибкой");
            }
        } catch (Exception e) {
            System.out.println("❌ Ошибка запуска тестов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<TaskEndpointMapping> mapTasksToEndpoints(List<BpmnTask> tasks,
                                                        List<ApiEndpoint> endpoints) {
        List<TaskEndpointMapping> mappings = new ArrayList<>();
        if (tasks == null || endpoints == null) return mappings;

        for (BpmnTask task : tasks) {
            ApiEndpoint matchedEndpoint = findSemanticMatch(task, endpoints);
            TaskEndpointMapping mapping = new TaskEndpointMapping();
            mapping.setBpmnTask(task);
            mapping.setApiEndpoint(matchedEndpoint);
            mapping.setMatchConfidence(calculateMatchConfidence(task, matchedEndpoint));
            mappings.add(mapping);
        }
        

        return mappings;
    }

    public void printMappingsToConsole(List<TaskEndpointMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) return;

        System.out.println("=== СОПОСТАВЛЕНИЕ BPMN И OPENAPI ===");
        for (TaskEndpointMapping mapping : mappings) {
            BpmnTask task = mapping.getBpmnTask();
            ApiEndpoint endpoint = mapping.getApiEndpoint();

            System.out.println("BPMN задача: " + task.getName());
            if (endpoint != null) {
                System.out.println("→ API endpoint: " + endpoint.getMethod() + " " + endpoint.getPath());
                System.out.println("  Уверенность: " + String.format("%.1f", mapping.getMatchConfidence() * 100) + "%");
            } else {
                System.out.println("→ Не найден подходящий endpoint");
            }
            System.out.println();
        }
    }

    private ApiEndpoint findSemanticMatch(BpmnTask task, List<ApiEndpoint> endpoints) {
        String normalizedTaskName = normalizeTaskName(task.getName());
        ApiEndpoint bestMatch = null;
        double bestScore = 0.7; // минимальный порог

        System.out.println("🔍 Поиск endpoint для: " + task.getName());
        System.out.println("🤖 Использую NLP-сопоставление...");
        System.out.println("   Нормализованное имя задачи: '" + normalizedTaskName + "'");

        for (ApiEndpoint endpoint : endpoints) {
            String endpointText = buildEndpointText(endpoint);
            double similarity = semanticMatcher.calculateSemanticSimilarity(normalizedTaskName, endpointText);

            // бонус за совпадение HTTP-метода
            double methodBonus = hasMatchingMethod(task.getName(), endpoint) ? 0.2 : 0.0;
            double totalScore = similarity + methodBonus;

            System.out.println("   Сравнение с " + endpoint.getMethod() + " " + endpoint.getPath() +
                               " -> score: " + String.format("%.2f", totalScore));

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestMatch = endpoint;
            }
        }

        if (bestMatch != null) {
            System.out.println("   🎯 Лучшее соответствие: " + bestMatch.getMethod() + " " +
                               bestMatch.getPath() + " (score: " + String.format("%.2f", bestScore) + ")");
        } else {
            System.out.println("   ❌ Нет подходящих endpoint'ов (лучший score < 0.3)");
        }

        return bestMatch;
    }

    private String normalizeTaskName(String taskName) {
        return taskName.replaceAll("(POST|GET|PUT|DELETE|PATCH)\\s+", "")
                       .replaceAll("\\s+", " ")
                       .trim();
    }

    private String buildEndpointText(ApiEndpoint endpoint) {
        return (endpoint.getMethod() + " " + endpoint.getPath() + " " +
                (endpoint.getOperationId() != null ? endpoint.getOperationId() : ""))
                .toLowerCase();
    }

    private boolean hasMatchingMethod(String taskName, ApiEndpoint endpoint) {
        String taskMethod = extractMethod(taskName);
        return taskMethod != null && taskMethod.equals(endpoint.getMethod());
    }

    private String extractMethod(String taskName) {
        if (taskName.contains("POST")) return "POST";
        if (taskName.contains("GET")) return "GET";
        if (taskName.contains("PUT")) return "PUT";
        if (taskName.contains("DELETE")) return "DELETE";
        return null;
    }

    private double calculateMatchConfidence(BpmnTask task, ApiEndpoint endpoint) {
        if (endpoint == null || task.getName() == null) return 0.0;

        String normalizedTaskName = normalizeTaskName(task.getName());
        String endpointText = buildEndpointText(endpoint);
        double semanticScore = semanticMatcher.calculateSemanticSimilarity(normalizedTaskName, endpointText);
        double methodBonus = hasMatchingMethod(task.getName(), endpoint) ? 0.2 : 0.0;

        return Math.min(1.0, semanticScore + methodBonus);
    }
}
