package com.example.bpmnai.core.domain;

import java.util.List;
import java.util.Map;

import sun.swing.LightweightContent;

public class ApiEndpoint {
    private String path;
    private String method;
    private String operationId;
    private String summary;
    private String description;

    
    private Map<String, Object> requestBody;   
    private List<Map<String, Object>> parameters; 
    private Map<String, Object> responses;    

    private List<Map<String, Object>> security;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getRequestBody() { return requestBody; }
    public void setRequestBody(Map<String, Object> requestBody) { this.requestBody = requestBody; }

    public List<Map<String, Object>> getParameters() { return parameters; }
    public void setParameters(List<Map<String, Object>> parameters) { this.parameters = parameters; }

    public Map<String, Object> getResponses() { return responses; }
    public void setResponses(Map<String, Object> responses) { this.responses = responses; }

    public List<Map<String, Object>> getSecurity() { return security; }
    public void setSecurity(List<Map<String, Object>> security) { this.security = security; }

}
