package com.example.bpmnai.core.domain;

public class BpmnTask {
    private String id;
    private String name;
    private String type;
    private String description;

    // Новые поля для сопоставления с OpenAPI
    private String apiMethod;    
    private String apiPath;      
    private String apiOperationId;

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getApiMethod() { return apiMethod; }
    public void setApiMethod(String apiMethod) { this.apiMethod = apiMethod; }

    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }

    public String getApiOperationId() { return apiOperationId; }
    public void setApiOperationId(String apiOperationId) { this.apiOperationId = apiOperationId; }
}
