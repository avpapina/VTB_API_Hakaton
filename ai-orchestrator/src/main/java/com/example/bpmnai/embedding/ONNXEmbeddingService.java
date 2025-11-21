package com.example.bpmnai.embedding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

@Service
public class ONNXEmbeddingService {
    
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final TokenizerService tokenizerService;
    
    public ONNXEmbeddingService(TokenizerService tokenizerService) throws Exception {
        this.environment = OrtEnvironment.getEnvironment();
        this.tokenizerService = tokenizerService;
        
        String modelPath = new ClassPathResource("spring_model/model.onnx")
                          .getFile().getAbsolutePath();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        this.session = environment.createSession(modelPath, options);
    }
    
    public float[] getEmbedding(String text) throws OrtException {
        List<Integer> tokenIds = tokenizerService.tokenize(text);
        
        if (tokenIds.isEmpty()) {
            return new float[384]; // размерность MiniLM
        }
        
        // Подготовка входных данных
        long[] inputIds = tokenIds.stream().mapToLong(Long::valueOf).toArray();
        long[] attentionMask = new long[tokenIds.size()];
        long[] tokenTypeIds = new long[tokenIds.size()];
        Arrays.fill(attentionMask, 1L);
        Arrays.fill(tokenTypeIds, 0L);
        
        // Создание батча размером 1
        long[][] inputIdsBatch = {inputIds};
        long[][] attentionMaskBatch = {attentionMask};
        long[][] tokenTypeIdsBatch = {tokenTypeIds};
        
        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", OnnxTensor.createTensor(environment, inputIdsBatch));
        inputs.put("attention_mask", OnnxTensor.createTensor(environment, attentionMaskBatch));
        inputs.put("token_type_ids", OnnxTensor.createTensor(environment, tokenTypeIdsBatch));
        
        // Выполнение модели
        try (OrtSession.Result results = session.run(inputs)) {
            OnnxValue output = results.get(0);
            
            // Обрабатываем разные возможные форматы вывода
            if (output instanceof OnnxTensor) {
                OnnxTensor tensor = (OnnxTensor) output;
                Object value = tensor.getValue();
                
                if (value instanceof float[][][]) {
                    // 3D тензор: [batch_size, sequence_length, hidden_size]
                    float[][][] embeddings3D = (float[][][]) value;
                    return extractEmbeddingFrom3D(embeddings3D, attentionMask);
                } else if (value instanceof float[][]) {
                    // 2D тензор: [batch_size, hidden_size] (уже pooled)
                    float[][] embeddings2D = (float[][]) value;
                    return embeddings2D[0]; // берем первый батч
                } else {
                    throw new OrtException("Неизвестный формат вывода: " + value.getClass());
                }
            } else {
                throw new OrtException("Вывод не является тензором");
            }
        }
    }
    
    private float[] extractEmbeddingFrom3D(float[][][] embeddings3D, long[] attentionMask) {
        // embeddings3D: [1, sequence_length, hidden_size]
        float[][] sequenceEmbeddings = embeddings3D[0]; // [sequence_length, hidden_size]
        int dimensions = sequenceEmbeddings[0].length;
        float[] result = new float[dimensions];
        int count = 0;
        
        // Mean pooling по токенам с учетом attention_mask
        for (int i = 0; i < sequenceEmbeddings.length && i < attentionMask.length; i++) {
            if (attentionMask[i] == 1) {
                for (int j = 0; j < dimensions; j++) {
                    result[j] += sequenceEmbeddings[i][j];
                }
                count++;
            }
        }
        
        // Усреднение
        if (count > 0) {
            for (int i = 0; i < dimensions; i++) {
                result[i] /= count;
            }
        }
        
        return result;
    }
    
    public double calculateSimilarity(String text1, String text2) {
        try {
            float[] emb1 = getEmbedding(text1);
            float[] emb2 = getEmbedding(text2);
            double rawSimilarity = cosineSimilarity(emb1, emb2);
            
            // АГРЕССИВНАЯ нормализация для высоких значений
            double normalizedSimilarity;
            if (rawSimilarity > 0.9) {
                normalizedSimilarity = 0.5 + (rawSimilarity - 0.9) * 2.0; // Сжимаем диапазон
            } else {
                normalizedSimilarity = rawSimilarity;
            }
            
            normalizedSimilarity = Math.max(0.0, Math.min(1.0, normalizedSimilarity));
            
            System.out.println("   🤖 Raw: " + String.format("%.3f", rawSimilarity) + 
                            " -> Normalized: " + String.format("%.3f", normalizedSimilarity));
            return normalizedSimilarity;
            
        } catch (Exception e) {
            System.err.println("   ❌ ONNX error: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += Math.pow(vec1[i], 2);
            norm2 += Math.pow(vec2[i], 2);
        }
        
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0.0 : Math.max(0.0, dotProduct / denominator);
    }
}