package com.example.bpmnai.orchestrator;

import org.springframework.stereotype.Service;

import com.example.bpmnai.embedding.ONNXEmbeddingService;

@Service
public class AdvancedNLPSemanticMatcher {
    
    private final ONNXEmbeddingService embeddingService;
    
    public AdvancedNLPSemanticMatcher(ONNXEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }
    
    public double calculateSemanticSimilarity(String text1, String text2) {
        try {
            double similarity = embeddingService.calculateSimilarity(text1, text2);
            System.out.println("   🧠 Final similarity: " + String.format("%.3f", similarity));
            return similarity;
        } catch (Exception e) {
            System.err.println("   ❌ Semantic matcher error: " + e.getMessage());
            return 0.0;
        }
    }
}