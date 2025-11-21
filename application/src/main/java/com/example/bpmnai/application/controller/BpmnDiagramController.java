package com.example.bpmnai.application.controller;

import com.example.bpmnai.application.BpmnDiagramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/visualization")
public class BpmnDiagramController {

    @Autowired
    private BpmnDiagramService bpmnDiagramService;

    @GetMapping
    public String showVisualizationPage(Model model) {
        try {

            String filePath = "data/template.bpmn";
            Resource resource = new ClassPathResource(filePath);

            if (resource.exists()) {
                String bpmnContent = new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
                model.addAttribute("bpmnXml", bpmnContent);
            } else {
                // Альтернативно используем абсолютный путь
                String absolutePath = "data/template.bpmn";
                if (Files.exists(Paths.get(absolutePath))) {
                    String bpmnContent = bpmnDiagramService.getBpmnXmlContent(absolutePath);
                    model.addAttribute("bpmnXml", bpmnContent);
                } else {
                    model.addAttribute("bpmnXml", "<!-- BPMN file not found -->");
                    model.addAttribute("error", "BPMN file not found at: " + absolutePath);
                }
            }
        } catch (IOException e) {
            model.addAttribute("bpmnXml", "<!-- Error reading BPMN file -->");
            model.addAttribute("error", "Error reading file: " + e.getMessage());
        }
        return "visualization";
    }

    @PostMapping("/upload")
    public String uploadBpmnFile(@RequestParam("bpmnFile") MultipartFile file, Model model) {
        try {
            if (!file.isEmpty() && file.getOriginalFilename() != null && file.getOriginalFilename().endsWith(".bpmn")) {
                String bpmnContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                model.addAttribute("bpmnXml", bpmnContent);

                // Сохраняем файл для будущего использования
                Path uploadDir = Paths.get("data/uploads");
                Files.createDirectories(uploadDir);

                String fileName =  "downloadedFile_" + file.getOriginalFilename();
                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, bpmnContent.getBytes());

                model.addAttribute("success", "File uploaded successfully: " + fileName);
            } else {
                model.addAttribute("error", "Please select a valid BPMN file");
            }
        } catch (IOException e) {
            model.addAttribute("error", "Error uploading file: " + e.getMessage());
        }
        return "visualization";
    }
}