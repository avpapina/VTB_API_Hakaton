package com.example.bpmnai.orchestrator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.example.test_executor.MatchedTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class MatchedTaskSaver {
    public static void saveMatchedTasks(List<TaskEndpointMapping> mappings, String processId, String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            List<MatchedTask> matchedTasks = new ArrayList<>();
            for (TaskEndpointMapping mapping : mappings) {
                MatchedTask task = new MatchedTask();
                task.setTaskName(mapping.getBpmnTask().getName());
                if (mapping.getApiEndpoint() != null) {
                    task.setHttpMethod(mapping.getApiEndpoint().getMethod());
                    task.setEndpointUrl(mapping.getApiEndpoint().getPath());
                }
                task.setProcessId(processId);
                
                // ✅ СОХРАНЯЕМ ТЕСТОВЫЕ ДАННЫЕ В ФАЙЛ
                if (mapping.getGeneratedTestData() != null) {
                    task.setTestData(mapping.getGeneratedTestData());
                }
                
                matchedTasks.add(task);
            }
            
            mapper.writeValue(new File(filePath), matchedTasks);
            System.out.println("✅ Matched tasks saved to: " + filePath);
            
        } catch (Exception e) {
            System.out.println("❌ Error saving matched tasks: " + e.getMessage());
        }
    }
}
