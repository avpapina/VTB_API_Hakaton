package com.example.test_executor;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchedTask {
    private String processId;
    private String taskName;
    private String endpointUrl;
    private String httpMethod;
    private double matchConfidence;
    private String testData; 

    // Конструктор по умолчанию
    public MatchedTask() {}

    // Геттеры и сеттеры для JSON полей
    @JsonProperty("processId")
    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    @JsonProperty("taskName")
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    @JsonProperty("endpointUrl")
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    @JsonProperty("httpMethod")
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    @JsonProperty("matchConfidence")
    public double getMatchConfidence() { return matchConfidence; }
    public void setMatchConfidence(double matchConfidence) { this.matchConfidence = matchConfidence; }

    // Методы для обратной совместимости
    public String getBpmnTaskId() { 
        // Генерируем ID из processId и taskName
        return "Task_" + processId + "_" + taskName.hashCode();
    }
    
    public String getBpmnTaskName() { 
        return taskName; // Используем taskName как bpmnTaskName
    }
    
    public String getOpenApiEndpoint() { 
        return endpointUrl; // Используем endpointUrl как openApiEndpoint
    }
    
    public double getConfidence() { 
        return matchConfidence; // Используем matchConfidence как confidence
    }

        public String getTestData() {
        return testData;
    }

    public void setTestData(String testData) {
        this.testData = testData;
    }
}