package org.example.jwtjavaeight.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/** API 文档辅助控制器 用于导出 Schemas 等信息，方便复制给 AI 分析 */
@Hidden
@RestController
@RequestMapping("/api/doc")
public class ApiDocController {

  private final ObjectMapper objectMapper;

  public ApiDocController(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** 获取 OpenAPI 文档 */
  private JsonNode getOpenApiDoc() throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    String apiDocsUrl = "http://localhost:8080/v3/api-docs";
    String json = restTemplate.getForObject(apiDocsUrl, String.class);
    return objectMapper.readTree(json);
  }

  /** 导出所有 Schemas 定义 访问: http://localhost:8080/api/doc/schemas */
  @GetMapping(value = "/schemas", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> getSchemas() {
    try {
      JsonNode openApiDoc = getOpenApiDoc();
      JsonNode componentsNode = openApiDoc.path("components");
      JsonNode schemasNode = componentsNode.path("schemas");

      if (schemasNode.isMissingNode() || schemasNode.isEmpty()) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "Schemas 为空");
        error.put("tip", "请检查 Controller 是否使用了 @RequestBody/@ResponseBody");
        return error;
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("title", "项目数据对象结构 (Schemas)");
      result.put("totalCount", schemasNode.size());
      result.put("schemas", objectMapper.convertValue(schemasNode, Map.class));
      result.put("usage", "复制 schemas 字段内容给 AI，让其了解项目的数据结构");

      return result;
    } catch (Exception e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("error", "无法获取 schemas 信息");
      error.put("message", e.getMessage());
      error.put("exceptionType", e.getClass().getName());
      return error;
    }
  }

  /** 导出完整的 OpenAPI 文档 访问: http://localhost:8080/api/doc/full */
  @GetMapping(value = "/full", produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode getFullApiDoc() {
    try {
      return getOpenApiDoc();
    } catch (Exception e) {
      Map<String, String> error = new LinkedHashMap<>();
      error.put("error", "无法获取 OpenAPI 文档");
      error.put("message", e.getMessage());
      return objectMapper.valueToTree(error);
    }
  }

  /** 导出精简的 Schemas（仅包含字段定义，更适合 AI 阅读） 访问: http://localhost:8080/api/doc/schemas-simple */
  @GetMapping(value = "/schemas-simple", produces = MediaType.TEXT_PLAIN_VALUE)
  public String getSchemasSimple() {
    try {
      JsonNode openApiDoc = getOpenApiDoc();
      JsonNode schemasNode = openApiDoc.path("components").path("schemas");

      if (schemasNode.isMissingNode() || schemasNode.isEmpty()) {
        return "错误: Schemas 为空\n\n"
            + "可能原因:\n"
            + "1. Controller 中没有使用 @RequestBody 或 @ResponseBody\n"
            + "2. DTO 类没有被 SpringDoc 扫描到";
      }

      StringBuilder sb = new StringBuilder();
      sb.append("# 项目数据对象结构\n\n");
      sb.append("共 ").append(schemasNode.size()).append(" 个数据对象\n\n");
      sb.append("---\n\n");

      schemasNode
          .fields()
          .forEachRemaining(
              entry -> {
                String name = entry.getKey();
                JsonNode schema = entry.getValue();

                sb.append("## ").append(name).append("\n\n");

                JsonNode description = schema.path("description");
                if (!description.isMissingNode()) {
                  sb.append("描述: ").append(description.asText()).append("\n\n");
                }

                JsonNode properties = schema.path("properties");
                if (!properties.isMissingNode() && properties.isObject()) {
                  sb.append("字段:\n");
                  properties
                      .fields()
                      .forEachRemaining(
                          prop -> {
                            String propName = prop.getKey();
                            JsonNode propSchema = prop.getValue();

                            sb.append("- **").append(propName).append("**");

                            JsonNode type = propSchema.path("type");
                            if (!type.isMissingNode()) {
                              sb.append(" (").append(type.asText()).append(")");
                            }

                            JsonNode format = propSchema.path("format");
                            if (!format.isMissingNode()) {
                              sb.append(" [").append(format.asText()).append("]");
                            }

                            JsonNode propDesc = propSchema.path("description");
                            if (!propDesc.isMissingNode()) {
                              sb.append(": ").append(propDesc.asText());
                            }

                            JsonNode example = propSchema.path("example");
                            if (!example.isMissingNode()) {
                              sb.append(" (示例: ").append(example.asText()).append(")");
                            }

                            sb.append("\n");
                          });
                  sb.append("\n");
                }

                JsonNode required = schema.path("required");
                if (!required.isMissingNode() && required.isArray() && required.size() > 0) {
                  sb.append("必填字段: ");
                  for (int i = 0; i < required.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(required.get(i).asText());
                  }
                  sb.append("\n\n");
                }

                sb.append("---\n\n");
              });

      return sb.toString();
    } catch (Exception e) {
      return "错误: "
          + e.getMessage()
          + "\n\n"
          + "异常类型: "
          + e.getClass().getName()
          + "\n\n"
          + "提示: 请确保应用已完全启动";
    }
  }
}
