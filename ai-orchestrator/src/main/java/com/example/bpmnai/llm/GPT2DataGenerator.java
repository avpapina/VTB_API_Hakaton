package com.example.bpmnai.llm;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

@Service
public class GPT2DataGenerator {
    
    private OrtEnvironment environment;
    private OrtSession session;
    private final GPT2Tokenizer tokenizer;
    
    public GPT2DataGenerator(GPT2Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        initializeModel();
    }
    
    private void initializeModel() {
        try {
            this.environment = OrtEnvironment.getEnvironment();
            
            String modelPath = getResourcePath("gpt2-ONNX/model.onnx");
            if (modelPath == null) {
                System.out.println("❌ Модель GPT2 не найдена");
                return;
            }
            
            File modelFile = new File(modelPath);
            System.out.println("📁 Размер модели GPT2: " + modelFile.length() + " bytes");
            
            if (!modelFile.exists()) {
                System.out.println("❌ Файл модели не существует");
                return;
            }
            
            // Пробуем загрузить с разными настройками
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            
            this.session = environment.createSession(modelPath, sessionOptions);
            System.out.println("✅ GPT2 модель успешно загружена!");
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка загрузки GPT2 модели: " + e.getMessage());
            this.session = null;
            this.environment = null;
        }
    }
    
    public String generateTestData(String taskDescription, String endpoint, String httpMethod) {
        System.out.println("🧠 Генерация данных для: " + taskDescription);
        System.out.println("   Endpoint: " + httpMethod + " " + endpoint);
        
        if (session == null) {
            System.out.println("⚠️ Использую умный fallback (модель не загружена)");
            return generateSmartFallbackData(taskDescription, endpoint, httpMethod);
        }
        
        try {
            // Создаем промпт для GPT2
            String prompt = createGPT2Prompt(taskDescription, endpoint, httpMethod);
            System.out.println("🤖 GPT2 промпт: " + prompt);
            
            // Генерируем данные
            String generatedText = generateWithGPT2(prompt);
            System.out.println("📝 GPT2 ответ: " + generatedText);
            
            // Извлекаем JSON
            String jsonData = extractJsonFromText(generatedText);
            
            if (!jsonData.equals("{}")) {
                System.out.println("✅ GPT2 сгенерировал JSON: " + jsonData);
                return jsonData;
            } else {
                System.out.println("⚠️ GPT2 не сгенерировал JSON, использую fallback");
                return generateSmartFallbackData(taskDescription, endpoint, httpMethod);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка генерации GPT2: " + e.getMessage());
            return generateSmartFallbackData(taskDescription, endpoint, httpMethod);
        }
    }
    
    private String createGPT2Prompt(String taskDescription, String endpoint, String httpMethod) {
        return String.format(
            "Generate realistic test data in JSON format for this API request:\n" +
            "Task: %s\n" +
            "Method: %s\n" +
            "Endpoint: %s\n\n" +
            "Return only valid JSON without any explanations:\n" +
            "{",
            taskDescription, httpMethod, endpoint
        );
    }
    
    private String generateWithGPT2(String prompt) throws OrtException {
        long[] inputIds = tokenizer.tokenize(prompt);
        long[][] inputIdsArray = {inputIds};
        
        // Создаем все необходимые входные данные
        long[][] attentionMask = createAttentionMask(inputIds);
        long[][] positionIds = createPositionIds(inputIds);
        
        // ✅ СОЗДАЕМ past_key_values (12 слоев для GPT2)
        int numLayers = 12; // GPT2 имеет 12 слоев
        int hiddenSize = 768; // Размерность GPT2
        int sequenceLength = inputIds.length;
        
        // Создаем пустые past_key_values для всех слоев
        Map<String, OnnxTensor> inputs = new HashMap<>();
        
        for (int i = 0; i < numLayers; i++) {
            // past_key (пустые тензоры)
            float[][][] pastKey = new float[1][1][hiddenSize]; // [batch, past_seq_len, hidden_size]
            inputs.put("past_key_values." + i + ".key", OnnxTensor.createTensor(environment, pastKey));
            
            // past_value (пустые тензоры)  
            float[][][] pastValue = new float[1][1][hiddenSize]; // [batch, past_seq_len, hidden_size]
            inputs.put("past_key_values." + i + ".value", OnnxTensor.createTensor(environment, pastValue));
        }
        
        // Добавляем основные входные данные
        inputs.put("input_ids", OnnxTensor.createTensor(environment, inputIdsArray));
        inputs.put("attention_mask", OnnxTensor.createTensor(environment, attentionMask));
        inputs.put("position_ids", OnnxTensor.createTensor(environment, positionIds));
        
        // Выполняем модель
        OrtSession.Result outputs = session.run(inputs);
        
        // Получаем сгенерированные токены
        long[][] outputTokens = (long[][]) outputs.get(0).getValue();
        
        // Детокенизируем в текст
        return tokenizer.detokenize(outputTokens[0]);
    }

    // ✅ ДОБАВЬ ЭТИ МЕТОДЫ
    private long[][] createPositionIds(long[] inputIds) {
        long[][] positionIds = new long[1][inputIds.length];
        for (int i = 0; i < inputIds.length; i++) {
            positionIds[0][i] = i; // Простая последовательность 0, 1, 2, 3...
        }
        return positionIds;
    }

    private long[][] createAttentionMask(long[] inputIds) {
        long[][] mask = new long[1][inputIds.length];
        for (int i = 0; i < inputIds.length; i++) {
            mask[0][i] = inputIds[i] != 0 ? 1L : 0L; // 1 для реальных токенов, 0 для padding
        }
        return mask;
    }
        
    private String extractJsonFromText(String text) {
        // Ищем JSON в тексте
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}") + 1;
        
        if (start >= 0 && end > start) {
            String json = text.substring(start, end);
            
            // Проверяем что это валидный JSON
            if (isValidJson(json)) {
                return json;
            }
        }
        
        return "{}";
    }
    
    private boolean isValidJson(String json) {
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String generateSmartFallbackData(String taskDescription, String endpoint, String httpMethod) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = new HashMap<>();
            
            String context = (taskDescription + " " + endpoint).toLowerCase();
            
            if (context.contains("lead") || context.contains("лид")) {
                data.put("firstName", getRandomRussianName());
                data.put("lastName", getRandomRussianLastName());
                data.put("phone", generateRussianPhone());
                data.put("email", generateEmail());
                data.put("birthDate", generateBirthDate());
            } 
            else if (context.contains("auth") || context.contains("token")) {
                data.put("client_id", "test_client_" + System.currentTimeMillis());
                data.put("client_secret", "secret_" + new Random().nextInt(10000));
                data.put("grant_type", "client_credentials");
            }
            else if (context.contains("product") || context.contains("продукт")) {
                data.put("productId", "prod_" + new Random().nextInt(1000));
                data.put("productName", "Test Product " + new Random().nextInt(100));
                data.put("interestRate", 8.5 + new Random().nextDouble() * 7);
                data.put("maxAmount", 50000 + new Random().nextInt(950000));
            }
            else {
                data.put("id", "test_" + System.currentTimeMillis());
                data.put("description", taskDescription);
                data.put("endpoint", endpoint);
                data.put("method", httpMethod);
            }
            
            return mapper.writeValueAsString(data);
            
        } catch (Exception e) {
            return "{\"error\": \"data_generation_failed\"}";
        }
    }
    
    // Вспомогательные методы
    private String getRandomRussianName() {
        String[] names = {"Иван", "Алексей", "Сергей", "Дмитрий", "Михаил", "Андрей", "Александр"};
        return names[new Random().nextInt(names.length)];
    }
    
    private String getRandomRussianLastName() {
        String[] lastNames = {"Иванов", "Петров", "Сидоров", "Смирнов", "Кузнецов", "Попов", "Васильев"};
        return lastNames[new Random().nextInt(lastNames.length)];
    }
    
    private String generateRussianPhone() {
        return "+79" + String.format("%09d", new Random().nextInt(1000000000));
    }
    
    private String generateEmail() {
        return "test" + new Random().nextInt(10000) + "@example.com";
    }
    
    private String generateBirthDate() {
        int year = 1980 + new Random().nextInt(30);
        int month = 1 + new Random().nextInt(12);
        int day = 1 + new Random().nextInt(28);
        return String.format("%04d-%02d-%02d", year, month, day);
    }
    
    private String getResourcePath(String resourceName) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            java.net.URL resourceUrl = classLoader.getResource(resourceName);
            
            if (resourceUrl != null) {
                String path = resourceUrl.getPath();
                if (path.startsWith("file:")) {
                    path = path.substring(5);
                }
                if (path.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("win")) {
                    path = path.substring(1);
                }
                return path;
            }
            
            String projectPath = System.getProperty("user.dir");
            String fullPath = projectPath + "/src/main/resources/" + resourceName;
            
            if (Files.exists(Paths.get(fullPath))) {
                return fullPath;
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
}