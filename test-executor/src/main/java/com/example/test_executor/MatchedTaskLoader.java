package com.example.test_executor;

import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MatchedTaskLoader {

    public static List<MatchedTask> load() {
        try {
            // Абсолютный путь в classpath
            InputStream is = MatchedTaskLoader.class.getResourceAsStream("/matched/matchedTasks.json");

            if (is == null) {
                throw new RuntimeException("Файл matchedTasks.json не найден в пакете matched");
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, new TypeReference<List<MatchedTask>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить matchedTasks.json: " + e.getMessage(), e);
        }
    }
}
