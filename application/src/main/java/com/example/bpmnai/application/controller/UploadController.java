package com.example.bpmnai.application.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.bpmnai.bpmn.BpmnParserService;
import com.example.bpmnai.core.domain.BpmnProcess;
import com.example.bpmnai.core.domain.OpenApiAnalysisResult;
import com.example.bpmnai.core.domain.TestExecution;
import com.example.bpmnai.openapi.OpenApiParserService;
import com.example.bpmnai.orchestrator.AiOrchestratorService;
import com.example.bpmnai.orchestrator.TaskEndpointMapping;

@Controller
@RequestMapping("/upload")
public class UploadController {

    public static TestExecution lastTestExecution;
    public static List<String> generatedChains = new ArrayList<>(); // для цепочек
    public static List<String> generatedData = new ArrayList<>(); // для сгенерированных данных

    private final BpmnParserService bpmnParserService;
    private final OpenApiParserService openApiParserService;
    private final AiOrchestratorService aiOrchestratorService;

    public UploadController(BpmnParserService bpmnParserService,
                            OpenApiParserService openApiParserService,
                            AiOrchestratorService aiOrchestratorService) {
        this.bpmnParserService = bpmnParserService;
        this.openApiParserService = openApiParserService;
        this.aiOrchestratorService = aiOrchestratorService;
    }

    @GetMapping
    public String uploadPage(Model model) {
        model.addAttribute("message", "Загрузите BPMN и OpenAPI файлы");
        return "upload";
    }

    @PostMapping
    public String handleFileUpload(
            @RequestParam("bpmnFile") MultipartFile bpmnFile,
            @RequestParam("openapiFile") MultipartFile openapiFile,
            Model model) {

        try {
            // Очищаем предыдущие данные
            generatedChains.clear();
            generatedData.clear();

            System.out.println("=== НАЧАЛО ОБРАБОТКИ ФАЙЛОВ ===");

            // 1. Парсинг BPMN
            System.out.println("1. Парсинг BPMN...");
            BpmnProcess bpmnResult = bpmnParserService.parseBpmnFile(bpmnFile);
            System.out.println("✅ BPMN распарсен: " + bpmnResult.getName());

            // 2. Парсинг OpenAPI
            System.out.println("2. Парсинг OpenAPI...");
            OpenApiAnalysisResult openApiAnalysisResult = openApiParserService.parseOpenApiFile(openapiFile);
            System.out.println("✅ OpenAPI распарсен: " + openApiAnalysisResult.getEndpoints().size() + " endpoints");

            // 3. Сопоставление задач и endpoints
            System.out.println("3. Сопоставление задач...");
            List<TaskEndpointMapping> mappings = aiOrchestratorService.mapTasksToEndpoints(
                    bpmnResult, openApiAnalysisResult, generatedData); // ← передаем список
            System.out.println("✅ Сопоставление завершено: " + mappings.size() + " маппингов");

            // 4. Запуск тестирования
            System.out.println("4. Запуск тестирования...");
            TestExecution testExecution = aiOrchestratorService.runApiTestingWithMappings(
                    mappings, bpmnResult.getId(), generatedChains); // ← передаем список
            System.out.println("✅ Тестирование завершено: " + (testExecution != null ? testExecution.getStatus() : "NULL"));

            // Сохраняем и передаем в модель
            UploadController.lastTestExecution = testExecution;
            model.addAttribute("testExecution", testExecution);
            model.addAttribute("bpmnResult", bpmnResult);
            model.addAttribute("openApiResult", openApiAnalysisResult);
            model.addAttribute("mappings", mappings);
            model.addAttribute("generatedChains", generatedChains);
            model.addAttribute("generatedData", generatedData);

            System.out.println("=== ОБРАБОТКА ЗАВЕРШЕНА УСПЕШНО ===");

        } catch (Exception e) {
            System.out.println("❌ ОШИБКА В UploadController: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("message", "❌ Ошибка: " + e.getMessage());
        }

        return "upload";
    }

    // Методы для добавления данных (будут вызываться из AiOrchestratorService)
    public static void addGeneratedChain(String chain) {
        generatedChains.add(chain);
    }

    public static void addGeneratedData(String data) {
        generatedData.add(data);
    }
}