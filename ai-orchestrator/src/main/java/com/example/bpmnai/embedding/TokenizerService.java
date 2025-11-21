package com.example.bpmnai.embedding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TokenizerService {
    
    private final Map<String, Integer> vocab;
    private final Map<String, Integer> specialTokens;
    private final ObjectMapper objectMapper;
    
    public TokenizerService() {
        this.objectMapper = new ObjectMapper();
        try {
            this.vocab = loadVocabulary();
            this.specialTokens = loadSpecialTokens();
            System.out.println("✅ Токенизатор успешно загружен! Словарь: " + vocab.size() + " токенов");
        } catch (Exception e) {
            System.err.println("❌ Критическая ошибка инициализации токенизатора: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось инициализировать токенизатор", e);
        }
    }
    
    public List<Integer> tokenize(String text) {
        List<Integer> tokenIds = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return tokenIds;
        }
        
        try {
            String normalized = text.toLowerCase()
                                  .replaceAll("[^a-zа-яё0-9\\s\\-\\/]", " ")
                                  .replaceAll("\\s+", " ")
                                  .trim();
            
            if (normalized.isEmpty()) {
                return tokenIds;
            }
            
            String[] tokens = normalized.split("\\s+");
            
            for (String token : tokens) {
                if (token.length() > 1) {
                    Integer tokenId = vocab.get(token);
                    if (tokenId != null) {
                        tokenIds.add(tokenId);
                    } else {
                        Integer unkId = specialTokens.get("[UNK]");
                        if (unkId == null) unkId = specialTokens.get("<unk>");
                        if (unkId == null) unkId = 3; // По умолчанию ID для UNK
                        tokenIds.add(unkId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка токенизации: " + e.getMessage());
        }
        
        return tokenIds;
    }
    
    private Map<String, Integer> loadVocabulary() throws Exception {
        ClassPathResource resource = new ClassPathResource("spring_model/tokenizer.json");
        
        if (!resource.exists()) {
            System.err.println("❌ Файл tokenizer.json не найден в classpath!");
            return createFallbackVocabulary();
        }
        
        Map<String, Object> tokenizerConfig = objectMapper.readValue(
            resource.getInputStream(), Map.class);
        
        Map<String, Integer> vocabulary = new HashMap<>();
        
        // Обрабатываем added_tokens
        if (tokenizerConfig.containsKey("added_tokens")) {
            List<Map<String, Object>> addedTokens = (List<Map<String, Object>>) tokenizerConfig.get("added_tokens");
            for (Map<String, Object> token : addedTokens) {
                if (token.containsKey("content") && token.containsKey("id")) {
                    String content = (String) token.get("content");
                    Integer id = ((Number) token.get("id")).intValue();
                    vocabulary.put(content, id);
                }
            }
        }
        
        // Обрабатываем основной словарь
        if (tokenizerConfig.containsKey("model") && tokenizerConfig.get("model") instanceof Map) {
            Map<String, Object> model = (Map<String, Object>) tokenizerConfig.get("model");
            
            if (model.containsKey("vocab") && model.get("vocab") instanceof List) {
                List<Object> vocabList = (List<Object>) model.get("vocab");
                System.out.println("Загружаем vocab из ArrayList с " + vocabList.size() + " элементами");
                
                // ДИАГНОСТИКА: посмотрим на первые 10 элементов
                System.out.println("=== ДИАГНОСТИКА СТРУКТУРЫ VOCAB ===");
                for (int i = 0; i < Math.min(20, vocabList.size()); i++) {
                    Object element = vocabList.get(i);
                    System.out.println("Элемент " + i + ": " + element + " (тип: " + 
                                    (element != null ? element.getClass().getSimpleName() : "null") + ")");
                    
                    if (element instanceof List) {
                        List<?> subList = (List<?>) element;
                        System.out.println("  Вложенный список: " + subList);
                    }
                }
                System.out.println("=== КОНЕЦ ДИАГНОСТИКИ ===");
                
                // Пробуем разные варианты парсинга
                parseVocabArrayList(vocabList, vocabulary);
            }
        }
        
        if (vocabulary.isEmpty()) {
            System.out.println("⚠️ Словарь пуст, использую fallback");
            return createFallbackVocabulary();
        }
        
        System.out.println("✅ Итоговый словарь: " + vocabulary.size() + " токенов");
        return vocabulary;
    }

    private void parseVocabArrayList(List<Object> vocabList, Map<String, Integer> vocabulary) {
        // Вариант 1: Каждый элемент - это [token, id]
        for (int i = 0; i < vocabList.size(); i++) {
            Object element = vocabList.get(i);
            
            if (element instanceof List) {
                List<?> pair = (List<?>) element;
                if (pair.size() >= 2) {
                    try {
                        String token = pair.get(0).toString();
                        Object idObj = pair.get(1);
                        Integer id = parseId(idObj);
                        if (id != null) {
                            vocabulary.put(token, id);
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка парсинга пары " + i + ": " + e.getMessage());
                    }
                }
            } else if (element instanceof String && i + 1 < vocabList.size()) {
                // Вариант 2: Элементы идут парами [String, Number]
                String token = (String) element;
                Object idObj = vocabList.get(i + 1);
                Integer id = parseId(idObj);
                if (id != null) {
                    vocabulary.put(token, id);
                    i++; // Пропускаем следующий элемент
                }
            }
        }
    }

    private Integer parseId(Object idObj) {
        if (idObj instanceof Number) {
            return ((Number) idObj).intValue();
        } else if (idObj instanceof String) {
            try {
                return Integer.parseInt((String) idObj);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (idObj instanceof List) {
            // Если ID тоже список, берем первый элемент
            List<?> idList = (List<?>) idObj;
            if (!idList.isEmpty() && idList.get(0) instanceof Number) {
                return ((Number) idList.get(0)).intValue();
            }
        }
        return null;
    }
    
    private Map<String, Integer> loadSpecialTokens() throws Exception {
        ClassPathResource resource = new ClassPathResource("spring_model/special_tokens_map.json");
        
        if (!resource.exists()) {
            System.err.println("❌ Файл special_tokens_map.json не найден!");
            return createDefaultSpecialTokens();
        }
        
        Map<String, Object> specialTokensMap = objectMapper.readValue(
            resource.getInputStream(), Map.class);
        
        Map<String, Integer> result = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : specialTokensMap.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> tokenInfo = (Map<String, Object>) entry.getValue();
                if (tokenInfo.containsKey("content")) {
                    String token = (String) tokenInfo.get("content");
                    Object idObj = tokenInfo.get("id");
                    if (idObj instanceof Number) {
                        Integer id = ((Number) idObj).intValue();
                        result.put(token, id);
                    }
                }
            }
        }
        
        return result;
    }
    
    private Map<String, Integer> createFallbackVocabulary() {
        Map<String, Integer> fallback = new HashMap<>();
        // Минимальный словарь для тестирования
        String[] words = {"create", "get", "post", "lead", "application", "product", "account", "balance"};
        for (int i = 0; i < words.length; i++) {
            fallback.put(words[i], i + 100);
        }
        return fallback;
    }
    
    private Map<String, Integer> createDefaultSpecialTokens() {
        Map<String, Integer> defaultTokens = new HashMap<>();
        defaultTokens.put("[UNK]", 3);
        defaultTokens.put("<unk>", 3);
        defaultTokens.put("[PAD]", 1);
        defaultTokens.put("<pad>", 1);
        defaultTokens.put("[CLS]", 0);
        defaultTokens.put("<s>", 0);
        defaultTokens.put("[SEP]", 2);
        defaultTokens.put("</s>", 2);
        return defaultTokens;
    }
}