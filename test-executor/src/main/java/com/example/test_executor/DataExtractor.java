
package com.example.test_executor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExtractor {
    private Map<String, String> context = new HashMap<>();
    
    public void extractDataFromResponse(String stepName, String responseBody) {
        if (responseBody == null) return;
        
        System.out.println("🔍 Извлечение данных из шага: " + stepName);
        
        try {
            // Извлекаем доступный баланс
            if (responseBody.contains("availableBalance")) {
                Pattern pattern = Pattern.compile("\"availableBalance\":\"?([0-9.]+\\.?[0-9]*)\"?"); 
                Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    String balance = matcher.group(1);
                    context.put("current_balance", balance);
                    System.out.println("   💰 Баланс: " + balance);
                }
            }
            
            // Извлекаем catalogId
            if (responseBody.contains("catalogId")) {
                Pattern pattern = Pattern.compile("\"catalogId\":\"([A-Z0-9]+)\"");
                Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    context.put("catalog_id", matcher.group(1));
                    System.out.println("   🏷️ Catalog ID: " + matcher.group(1));
                }
            }
            
            // Извлекаем programId  
            if (responseBody.contains("programId")) {
                Pattern pattern = Pattern.compile("\"programId\":\"([A-Z0-9]+)\"");
                Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    context.put("program_id", matcher.group(1));
                    System.out.println("   📋 Program ID: " + matcher.group(1));
                }
            }
            
            // Извлекаем минимальные баллы для списания
            if (responseBody.contains("minRedeemPoints")) {
                Pattern pattern = Pattern.compile("\"minRedeemPoints\":\"?([0-9.]+)\"?");
                Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    context.put("min_redeem_points", matcher.group(1));
                    System.out.println("   📊 Минимум для списания: " + matcher.group(1));
                }
            }
            
        } catch (Exception e) {
            System.out.println("   ⚠️ Ошибка извлечения данных: " + e.getMessage());
        }
    }
    
    public String resolveUrl(String urlTemplate) {
        if (urlTemplate == null) return null;
        
        String url = urlTemplate;
        
        // Заменяем плейсхолдеры
        url = url.replace("{externalAccountID}", "test123");
        url = url.replace("{account_id}", "test123");
        url = url.replace("{payment_id}", "payment_" + System.currentTimeMillis());
        
        // Добавляем базовый путь API если нужно
        if (!url.startsWith("/api/") && !url.contains("auth") && !url.startsWith("http")) {
            url = "/api/rb/rewardsPay/hackathon/v1" + url;
        }
        
        System.out.println("   🔗 Resolved URL: " + url);
        return url;
    }
    
    public String resolveRequestBody(String stepName, String currentBody) {
        if (currentBody == null) return null;
        
        String body = currentBody;
        
        // Подставляем извлеченные данные в тело запроса
        if (context.containsKey("catalog_id") && body.contains("C9AP78DS9K")) {
            body = body.replace("C9AP78DS9K", context.get("catalog_id"));
            System.out.println("   🔄 Заменен catalog_id в теле запроса");
        }
        
        if (context.containsKey("program_id") && body.contains("A7DV56B")) {
            body = body.replace("A7DV56B", context.get("program_id"));
            System.out.println("   🔄 Заменен program_id в теле запроса");
        }
        
        // Динамически рассчитываем сумму списания на основе баланса
        if (stepName.contains("списание") || stepName.contains("redemption")) {
            if (context.containsKey("current_balance") && context.containsKey("min_redeem_points")) {
                try {
                    double balance = Double.parseDouble(context.get("current_balance"));
                    double minPoints = Double.parseDouble(context.get("min_redeem_points"));
                    
                    if (balance >= minPoints) {
                        // Используем минимальную сумму для списания
                        String redeemPoints = String.valueOf((int)minPoints);
                        body = body.replaceFirst("\"redeemPoints\":\\s*\\d+", "\"redeemPoints\": " + redeemPoints);
                        System.out.println("   💳 Установлена сумма списания: " + redeemPoints);
                    }
                } catch (NumberFormatException e) {
                    // Используем значение по умолчанию
                }
            }
        }
        
        return body;
    }
    
    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }
    
    public void clearContext() {
        context.clear();
    }
}