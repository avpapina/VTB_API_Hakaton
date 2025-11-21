package com.example.bpmnai.core.domain;

import lombok.Data;
import java.util.Map;

@Data
public class TestData {
    private String id;
    private String stepId;
    private Map<String, Object> data; // Гибкая структура тестовых данных
    private Map<String, String> variables; // Переменные для подстановки
}