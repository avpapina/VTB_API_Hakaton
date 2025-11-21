package com.example.bpmnai.orchestrator;

import com.example.bpmnai.core.domain.BpmnTask;

public class MatchedTaskJson {
    private String processId;
    private String taskName;
    private String endpointUrl;
    private String httpMethod;
    private double matchConfidence;

    public MatchedTaskJson() {}

    public MatchedTaskJson(String processId, BpmnTask task, TaskEndpointMapping mapping) {
        this.processId = processId;
        this.taskName = task.getName();
        this.endpointUrl = mapping.getApiEndpoint() != null ? mapping.getApiEndpoint().getPath() : null;
        this.httpMethod = mapping.getApiEndpoint() != null ? mapping.getApiEndpoint().getMethod() : null;
        this.matchConfidence = mapping.getMatchConfidence();
    }

    // геттеры/сеттеры
    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public double getMatchConfidence() { return matchConfidence; }
    public void setMatchConfidence(double matchConfidence) { this.matchConfidence = matchConfidence; }
}
