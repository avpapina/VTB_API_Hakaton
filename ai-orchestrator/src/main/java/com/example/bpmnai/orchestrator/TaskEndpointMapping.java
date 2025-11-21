package com.example.bpmnai.orchestrator;

import com.example.bpmnai.core.domain.ApiEndpoint;
import com.example.bpmnai.core.domain.BpmnTask;

public class TaskEndpointMapping {
    private BpmnTask bpmnTask;
    private ApiEndpoint apiEndpoint;
    private double matchConfidence;
    private String generatedTestData;
    
    public BpmnTask getBpmnTask() { return bpmnTask; }
    public void setBpmnTask(BpmnTask bpmnTask) { this.bpmnTask = bpmnTask; }
    
    public ApiEndpoint getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(ApiEndpoint apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    
    public double getMatchConfidence() { return matchConfidence; }
    public void setMatchConfidence(double matchConfidence) { this.matchConfidence = matchConfidence; }

    public String getGeneratedTestData() { return generatedTestData;}
    public void setGeneratedTestData(String generatedTestData) {this.generatedTestData = generatedTestData;}

}