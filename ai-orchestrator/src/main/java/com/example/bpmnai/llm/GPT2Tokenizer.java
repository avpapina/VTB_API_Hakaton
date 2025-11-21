package com.example.bpmnai.llm;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GPT2Tokenizer {
    
    private final ObjectMapper objectMapper;
    private final Map<String, Integer> vocab;
    private final Map<Integer, String> reverseVocab;
    
    public GPT2Tokenizer() {
        this.objectMapper = new ObjectMapper();
        Map<String, Integer> tempVocab = new HashMap<>();
        
        try {
            // Загружаем словарь из vocab.json
            String vocabPath = getResourcePath("gpt2-ONNX/vocab.json");
            if (vocabPath != null) {
                InputStream inputStream = Files.newInputStream(Paths.get(vocabPath));
                tempVocab = objectMapper.readValue(inputStream, 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class));
                inputStream.close();
                System.out.println("✅ GPT2 словарь загружен, размер: " + tempVocab.size());
            } else {
                System.out.println("⚠️ Файл vocab.json не найден, использую базовый словарь");
                tempVocab = createBasicVocab();
            }
        } catch (Exception e) {
            System.out.println("❌ Ошибка загрузки словаря GPT2: " + e.getMessage());
            tempVocab = createBasicVocab();
        }
        
        this.vocab = tempVocab;
        this.reverseVocab = createReverseVocab(tempVocab);
    }
    
    private Map<String, Integer> createBasicVocab() {
        Map<String, Integer> basicVocab = new HashMap<>();
        
        // Базовые токены для генерации JSON
        String[] basicTokens = {
            "{", "}", "\"", ":", ",", "[", "]",
            "name", "phone", "email", "id", "amount", "currency", "type",
            "first", "last", "client", "secret", "grant", "product", "application",
            "RUB", "USD", "test", "data", "value", "description", "status"
        };
        
        for (int i = 0; i < basicTokens.length; i++) {
            basicVocab.put(basicTokens[i], 1000 + i);
        }
        
        // Добавляем русские имена
        String[] russianNames = {"Иван", "Петр", "Сергей", "Алексей", "Мария", "Анна", "Елена"};
        for (int i = 0; i < russianNames.length; i++) {
            basicVocab.put(russianNames[i], 2000 + i);
        }
        
        // Добавляем цифры
        for (int i = 0; i < 10; i++) {
            basicVocab.put(String.valueOf(i), 3000 + i);
        }
        
        return basicVocab;
    }
    
    private Map<Integer, String> createReverseVocab(Map<String, Integer> vocab) {
        Map<Integer, String> reverse = new HashMap<>();
        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }
        return reverse;
    }
    
    public long[] tokenize(String text) {
        List<Long> tokens = new ArrayList<>();
        String[] words = text.toLowerCase().split("\\s+");
        
        for (String word : words) {
            // Пробуем найти полное слово в словаре
            if (vocab.containsKey(word)) {
                tokens.add(vocab.get(word).longValue());
            } else {
                // Разбиваем на подстроки
                boolean found = false;
                for (int len = word.length(); len > 0; len--) {
                    for (int i = 0; i <= word.length() - len; i++) {
                        String substr = word.substring(i, i + len);
                        if (vocab.containsKey(substr)) {
                            tokens.add(vocab.get(substr).longValue());
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                
                // Если не нашли, разбиваем на символы
                if (!found) {
                    for (char c : word.toCharArray()) {
                        String charStr = String.valueOf(c);
                        if (vocab.containsKey(charStr)) {
                            tokens.add(vocab.get(charStr).longValue());
                        } else {
                            tokens.add((long) c); // Используем ASCII код
                        }
                    }
                }
            }
            
            // Добавляем пробел между словами
            if (vocab.containsKey(" ")) {
                tokens.add(vocab.get(" ").longValue());
            } else {
                tokens.add((long) ' ');
            }
        }
        
        return limitLength(tokens);
    }
    
    public String detokenize(long[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (long token : tokens) {
            if (reverseVocab.containsKey((int) token)) {
                String word = reverseVocab.get((int) token);
                sb.append(word);
            } else if (token > 0 && token < 256) {
                sb.append((char) token);
            }
            // Добавляем пробел между словами
            sb.append(" ");
        }
        return sb.toString().trim().replaceAll("\\s+", " ");
    }
    
    private long[] limitLength(List<Long> tokens) {
        int maxLength = 512;
        
        if (tokens.size() > maxLength) {
            return tokens.subList(0, maxLength).stream().mapToLong(Long::longValue).toArray();
        }
        
        // Добиваем до maxLength нулями
        long[] result = new long[maxLength];
        for (int i = 0; i < tokens.size(); i++) {
            result[i] = tokens.get(i);
        }
        for (int i = tokens.size(); i < maxLength; i++) {
            result[i] = 0L;
        }
        
        return result;
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