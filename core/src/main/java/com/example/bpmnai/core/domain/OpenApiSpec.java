package com.example.bpmnai.core.domain;

import lombok.Data;
import java.util.List;

@Data
public class OpenApiSpec {
    private String id;
    private String title;
    private String version;
    private List<ApiEndpoint> endpoints;
}