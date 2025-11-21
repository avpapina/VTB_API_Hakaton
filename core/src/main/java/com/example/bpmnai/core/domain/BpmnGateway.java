package com.example.bpmnai.core.domain;

public class BpmnGateway {
    private String id;
    private String name;
    private String type; 
    private String direction; 

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}