package com.example.bpmnai.core.domain;

import lombok.Data;

@Data
public class ApiParameter {
    private String name;
    private String in; // "query", "path", "header", "body"
    private String type;
    private boolean required;
    private Object example;
}