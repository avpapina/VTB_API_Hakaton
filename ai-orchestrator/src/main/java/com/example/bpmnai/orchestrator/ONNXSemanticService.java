package com.example.bpmnai.orchestrator;

import org.springframework.stereotype.Service;

import com.example.bpmnai.embedding.ONNXEmbeddingService;

@Service
public class ONNXSemanticService {
    
    private final ONNXEmbeddingService embeddingService;
    
    public ONNXSemanticService(ONNXEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }
    
    public double calculateSemanticSimilarity(String text1, String text2) {
        return embeddingService.calculateSimilarity(text1, text2);
    }
    
    public boolean isSemanticMatch(double similarity) {
        // Порог для семантического соответствия
        return similarity >= 0.6; // Настроить по результатам тестов
    }
}