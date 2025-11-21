// test-executor/src/main/java/com/example/test_executor/TestExecutorService.java
package com.example.test_executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.example.bpmnai.core.domain.StepExecution;
import com.example.bpmnai.core.domain.TestExecution;
import com.example.bpmnai.core.domain.TestScenario;
import com.example.bpmnai.core.domain.TestStatus;
import com.example.bpmnai.core.domain.TestStep;

public class TestExecutorService {
    private static final String CLIENT_ID = "team145";
    private static final String CLIENT_SECRET = "9sigM7yvmBbSQm3Za0UxeLpbsWYdF1js";
    private static final String AUTH_URL = "https://auth.bankingapi.ru/auth/realms/kubernetes/protocol/openid-connect/token";
    private static final String API_BASE = "https://api.bankingapi.ru";
    
    private String accessToken;
    private DataExtractor dataExtractor = new DataExtractor();
    
    public TestExecution executeScenario(TestScenario scenario) {
        TestExecution execution = new TestExecution();
        execution.setScenario(scenario);
        execution.setStatus(TestStatus.RUNNING);
        
        System.out.println("\n🎯 НАЧАЛО ВЫПОЛНЕНИЯ СЦЕНАРИЯ: " + scenario.getName());
        
        try {
            // Шаг 1: Аутентификация
            StepExecution authStep = executeAuthentication(scenario.getSteps().get(0));
            execution.getStepExecutions().add(authStep);
            
            if (!authStep.isSuccess()) {
                execution.setStatus(TestStatus.FAILED);
                execution.setErrorMessage("Аутентификация не удалась: " + authStep.getErrorMessage());
                return execution;
            }
            
            // Выполняем остальные шаги с обработкой ошибок
            boolean hasFailures = false;
            for (int i = 1; i < scenario.getSteps().size(); i++) {
                TestStep step = scenario.getSteps().get(i);
                System.out.println("\n--- Шаг " + i + ": " + step.getName() + " ---");
                
                StepExecution stepExecution = executeStep(step);
                execution.getStepExecutions().add(stepExecution);
                
                if (!stepExecution.isSuccess()) {
                    hasFailures = true;
                    System.out.println("❌ Шаг завершился с ошибкой, но продолжаем выполнение...");
                    // НЕ прерываем выполнение - собираем все ошибки
                }
            }
            
            // Определяем финальный статус
            if (hasFailures) {
                execution.setStatus(TestStatus.FAILED);
                execution.setErrorMessage("Некоторые шаги завершились с ошибками");
            } else {
                execution.setStatus(TestStatus.SUCCESS);
            }
            
        } catch (Exception e) {
            execution.setStatus(TestStatus.ERROR);
            execution.setErrorMessage("Критическая ошибка выполнения: " + e.getMessage());
        }
        
        System.out.println("\n🎉 ВЫПОЛНЕНИЕ ЗАВЕРШЕНО. Статус: " + execution.getStatus());
        return execution;
    }
    
    private StepExecution executeAuthentication(TestStep authStep) {
        StepExecution execution = new StepExecution();
        execution.setStep(authStep);
        execution.setStepName(authStep.getName());
        execution.setEndpoint(AUTH_URL);
        execution.setMethod("POST");
        
        System.out.println("🔐 ВЫПОЛНЕНИЕ АУТЕНТИФИКАЦИИ...");
        
        try {
            this.accessToken = getAccessToken();
            if (accessToken != null) {
                execution.setSuccess(true);
                execution.setResponseStatus(200);
                execution.setResponseData("Токен получен успешно");
                System.out.println("✅ Аутентификация успешна");
            } else {
                execution.setSuccess(false);
                execution.setResponseStatus(401);
                execution.setErrorMessage("Ошибка аутентификации: не удалось получить токен");
                System.out.println("❌ Ошибка аутентификации");
            }
        } catch (Exception e) {
            execution.setSuccess(false);
            execution.setResponseStatus(500);
            execution.setErrorMessage("Ошибка при аутентификации: " + e.getMessage());
            System.out.println("❌ Критическая ошибка аутентификации: " + e.getMessage());
        }
        
        return execution;
    }
    
    private StepExecution executeStep(TestStep step) {
        StepExecution execution = new StepExecution();
        execution.setStep(step);
        execution.setStepName(step.getName());
        
        try {
            // Подготавливаем URL с помощью DataExtractor
            String resolvedUrl = dataExtractor.resolveUrl(step.getUrl());
            execution.setEndpoint(resolvedUrl);
            execution.setMethod(step.getMethod());
            
            // Подготавливаем тело запроса
            String requestBody = prepareRequestBody(step);
            String resolvedBody = dataExtractor.resolveRequestBody(step.getName(), requestBody);
            execution.setRequestData(resolvedBody);
            
            HttpClient client = HttpClient.newHttpClient();
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + resolvedUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json");
            
            // Добавляем тело запроса для POST
            if ("POST".equalsIgnoreCase(step.getMethod()) && resolvedBody != null) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(resolvedBody));
                System.out.println("📤 POST запрос с телом: " + resolvedBody);
            } else {
                requestBuilder.GET();
                System.out.println("📤 GET запрос");
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            execution.setResponseStatus(response.statusCode());
            execution.setResponseData(response.body());
            execution.setSuccess(response.statusCode() >= 200 && response.statusCode() < 300);
            
            System.out.println("📥 Ответ: " + response.statusCode() + " - " + 
                (response.body().length() > 100 ? response.body().substring(0, 100) + "..." : response.body()));
            
            // Извлекаем данные из ответа для следующих шагов
            if (execution.isSuccess()) {
                dataExtractor.extractDataFromResponse(step.getName(), response.body());
            } else {
                System.out.println("⚠️ Шаг завершился с ошибкой, данные не извлекаются");
            }
            
        } catch (Exception e) {
            execution.setSuccess(false);
            execution.setResponseStatus(500);
            execution.setErrorMessage("Ошибка выполнения шага: " + e.getMessage());
            System.out.println("❌ Ошибка выполнения: " + e.getMessage());
        }
        
        return execution;
    }
    
    private String prepareRequestBody(TestStep step) {
        if (step.getName().contains("списание") || step.getName().contains("redemption")) {
            return "{" +
                "\"catalogId\": \"C9AP78DS9K\"," +
                "\"programId\": \"A7DV56B\"," + 
                "\"redeemPoints\": 2000," +
                "\"currencyCode\": \"RUB\"" +
                "}";
        }
        return null;
    }
    
    private String getAccessToken() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        String formData = "grant_type=client_credentials" +
            "&client_id=" + CLIENT_ID +
            "&client_secret=" + CLIENT_SECRET;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(AUTH_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formData))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            String responseBody = response.body();
            if (responseBody.contains("access_token")) {
                return responseBody.split("\"access_token\":\"")[1].split("\"")[0];
            }
        }
        
        throw new RuntimeException("Ошибка аутентификации: " + response.statusCode() + " - " + response.body());
    }
}