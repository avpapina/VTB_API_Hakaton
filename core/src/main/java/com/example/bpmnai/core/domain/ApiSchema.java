package com.example.bpmnai.core.domain;

import lombok.Data;
import java.util.Map;

@Data
public class ApiSchema {
    private String name;
    private String type; // "object", "array", "string", etc.
    private Map<String, Object> properties;
    private Map<String, Object> example;
    private boolean required;
}