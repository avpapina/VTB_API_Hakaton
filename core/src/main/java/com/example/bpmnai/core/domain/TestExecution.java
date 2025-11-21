package com.example.bpmnai.core.domain;

import java.util.ArrayList;
import java.util.List;

public class TestExecution {
    private String id;
    private TestScenario scenario;
    private TestStatus status;
    private List<StepExecution> stepExecutions = new ArrayList<>();
    private String errorMessage;
    
    // геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public TestScenario getScenario() { return scenario; }
    public void setScenario(TestScenario scenario) { this.scenario = scenario; }
    
    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus status) { this.status = status; }
    
    public List<StepExecution> getStepExecutions() { return stepExecutions; }
    public void setStepExecutions(List<StepExecution> stepExecutions) { this.stepExecutions = stepExecutions; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}