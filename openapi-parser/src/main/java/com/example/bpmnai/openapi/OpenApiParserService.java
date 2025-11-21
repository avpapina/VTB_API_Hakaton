package com.example.bpmnai.openapi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.bpmnai.core.domain.ApiEndpoint;
import com.example.bpmnai.core.domain.OpenApiAnalysisResult;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

@Service
public class OpenApiParserService {

    public void printAnalysisToConsole(OpenApiAnalysisResult result) {
        System.out.println("=== OpenAPI Analysis ===");
        result.getEndpoints().forEach(ep -> {
            System.out.println(ep.getMethod() + " " + ep.getPath() + " (" + ep.getOperationId() + ")");
        });
    }


    public OpenApiAnalysisResult parseOpenApiFile(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(content);
            if (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty()) {
                System.out.println("WARN OpenAPI parser messages: " + parseResult.getMessages());
            }

            OpenAPI openAPI = parseResult.getOpenAPI();
            if (openAPI == null) {
                throw new RuntimeException("Не удалось распарсить OpenAPI. Файл повреждён или формат неверный.");
            }

            return analyzeOpenApi(openAPI);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга OpenAPI файла: " + e.getMessage(), e);
        }
    }

    private OpenApiAnalysisResult analyzeOpenApi(OpenAPI openAPI) {
        OpenApiAnalysisResult result = new OpenApiAnalysisResult();
        List<ApiEndpoint> endpoints = new ArrayList<>();

        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();

                analyzeOperation("GET", path, pathItem.getGet(), endpoints, openAPI);
                analyzeOperation("POST", path, pathItem.getPost(), endpoints, openAPI);
                analyzeOperation("PUT", path, pathItem.getPut(), endpoints, openAPI);
                analyzeOperation("DELETE", path, pathItem.getDelete(), endpoints, openAPI);
                analyzeOperation("PATCH", path, pathItem.getPatch(), endpoints, openAPI);
            }
        }

        result.setEndpoints(endpoints);
        return result;
    }

    private void analyzeOperation(String method, String path, Operation op, List<ApiEndpoint> endpoints, OpenAPI openAPI) {
        if (op == null) return;

        ApiEndpoint endpoint = new ApiEndpoint();
        endpoint.setMethod(method);
        endpoint.setPath(path);
        endpoint.setOperationId(op.getOperationId());
        endpoint.setSummary(op.getSummary());
        endpoint.setDescription(op.getDescription());

        // Параметры (path/query/header)
        List<Map<String, Object>> paramsList = new ArrayList<>();
        if (op.getParameters() != null) {
            for (Parameter p : op.getParameters()) {
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("name", p.getName());
                paramMap.put("in", p.getIn());
                paramMap.put("required", p.getRequired());
                paramMap.put("description", p.getDescription());
                paramMap.put("schema", convertSchema(p.getSchema()));
                paramsList.add(paramMap);
            }
        }
        endpoint.setParameters(paramsList);

        // Request body
        if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("description", op.getRequestBody().getDescription());
            reqMap.put("required", op.getRequestBody().getRequired());

            Map<String, Object> contentMap = new HashMap<>();
            op.getRequestBody().getContent().forEach((type, mediaType) -> {
                if (mediaType.getSchema() != null) {
                    contentMap.put(type, convertSchema(mediaType.getSchema()));
                }
            });
            reqMap.put("content", contentMap);
            endpoint.setRequestBody(reqMap);
        }

        // Responses
        Map<String, Object> respMap = new HashMap<>();
        if (op.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> entry : op.getResponses().entrySet()) {
                Map<String, Object> r = new HashMap<>();
                r.put("description", entry.getValue().getDescription());
                if (entry.getValue().getContent() != null) {
                    Map<String, Object> contentMap = new HashMap<>();
                    entry.getValue().getContent().forEach((type, mediaType) -> {
                        if (mediaType.getSchema() != null) {
                            contentMap.put(type, convertSchema(mediaType.getSchema()));
                        }
                    });
                    r.put("content", contentMap);
                }
                respMap.put(entry.getKey(), r);
            }
        }
        endpoint.setResponses(respMap);

        // Security
        List<Map<String, Object>> securityList = new ArrayList<>();
        if (op.getSecurity() != null) {
            for (SecurityRequirement sec : op.getSecurity()) {
                Map<String, Object> secMap = new HashMap<>();
                sec.forEach((key, scopes) -> {
                    Map<String, Object> s = new HashMap<>();
                    s.put("type", openAPI.getComponents().getSecuritySchemes().get(key).getType());
                    s.put("scheme", openAPI.getComponents().getSecuritySchemes().get(key).getScheme());
                    s.put("scopes", scopes);
                    secMap.put(key, s);
                });
                securityList.add(secMap);
            }
        }
        endpoint.setSecurity(securityList);

        endpoints.add(endpoint);
    }

    // Рекурсивное преобразование Schema в Map (тип, формат, required, свойства)
    private Map<String, Object> convertSchema(Schema<?> schema) {
        if (schema == null) return null;

        Map<String, Object> map = new HashMap<>();
        map.put("type", schema.getType());
        map.put("format", schema.getFormat());
        map.put("enum", schema.getEnum());
        map.put("example", schema.getExample());

        // Свойства объектов
        if (schema.getProperties() != null) {
            Map<String, Object> props = new HashMap<>();
            schema.getProperties().forEach((k, v) -> props.put(k, convertSchema((Schema<?>) v)));
            map.put("properties", props);
        }

        // Required поля
        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            map.put("required", schema.getRequired());
        }

        // Items для массивов
        if (schema.getItems() != null) {
            map.put("items", convertSchema(schema.getItems()));
        }

        return map;
    }
}
