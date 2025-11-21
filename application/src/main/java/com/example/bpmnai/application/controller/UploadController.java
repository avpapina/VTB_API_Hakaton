package com.example.bpmnai.application.controller;

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
import com.example.bpmnai.core.domain.TestScenario;
import com.example.bpmnai.openapi.OpenApiParserService;
import com.example.bpmnai.orchestrator.AiOrchestratorService;
import com.example.bpmnai.orchestrator.TaskEndpointMapping;

@Controller
@RequestMapping("/upload")
public class UploadController {

    // Статическое поле для хранения последних сгенерированных тестов
    public static TestScenario lastTestScenario;

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
            // 1. Парсинг BPMN
            BpmnProcess bpmnResult = bpmnParserService.parseBpmnFile(bpmnFile);
            bpmnParserService.printAnalysisToConsole(bpmnResult);

            // 2. Парсинг OpenAPI
            OpenApiAnalysisResult openApiAnalysisResult = openApiParserService.parseOpenApiFile(openapiFile);
            openApiParserService.printAnalysisToConsole(openApiAnalysisResult);

            // // 5. Опционально: сопоставление задач и endpoints (если нужно)
            List<TaskEndpointMapping> mappings = aiOrchestratorService.mapTasksToEndpoints(bpmnResult, openApiAnalysisResult);
            aiOrchestratorService.printMappingsToConsole(mappings);

            // 6. Передача данных на страницу upload.html
            model.addAttribute("bpmnResult", bpmnResult);
            model.addAttribute("openApiResult", openApiAnalysisResult);

            model.addAttribute("mappings", mappings);

        } catch (Exception e) {
            String errorMessage = "❌ Ошибка при обработке файлов: " + e.getMessage();
            model.addAttribute("message", errorMessage);
            e.printStackTrace();
        }

        return "upload";
    }
}
