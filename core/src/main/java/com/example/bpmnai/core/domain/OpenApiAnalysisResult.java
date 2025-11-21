package com.example.bpmnai.core.domain;

import java.util.List;

public class OpenApiAnalysisResult {
    private List<ApiEndpoint> endpoints;

    public List<ApiEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<ApiEndpoint> endpoints) {
        this.endpoints = endpoints;
    }
}