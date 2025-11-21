package com.example.bpmnai.core.domain;

import java.util.List;

public class BpmnProcess {
    private String id;
    private String name;
    private List<BpmnTask> tasks;
    private List<BpmnSequenceFlow> sequenceFlows;
    private List<BpmnGateway> gateways;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<BpmnTask> getTasks() { return tasks; }
    public void setTasks(List<BpmnTask> tasks) { this.tasks = tasks; }

    public List<BpmnSequenceFlow> getSequenceFlows() { return sequenceFlows; }
    public void setSequenceFlows(List<BpmnSequenceFlow> sequenceFlows) { this.sequenceFlows = sequenceFlows; }

    public List<BpmnGateway> getGateways() { return gateways; }
    public void setGateways(List<BpmnGateway> gateways) { this.gateways = gateways; }
}