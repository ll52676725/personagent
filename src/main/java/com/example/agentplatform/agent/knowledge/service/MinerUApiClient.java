package com.example.agentplatform.agent.knowledge.service;

import com.example.agentplatform.agent.knowledge.config.MinerUProperties;
import com.example.agentplatform.agent.knowledge.dto.MinerUParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinerUApiClient {

    private final MinerUProperties properties;
    private final ObjectMapper objectMapper;

    private RestTemplate createRestTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }

    public MinerUParseResult parseFile(MultipartFile file) {
        if (!properties.isEnabled()) {
            log.warn("MinerU is disabled, falling back to basic parsing");
            return null;
        }

        try {
            String taskId = submitParseTask(file);
            if (taskId == null) {
                log.error("Failed to submit parse task");
                return null;
            }

            log.info("Parse task submitted, taskId: {}", taskId);

            MinerUParseResult result = pollForResult(taskId);
            if (result != null) {
                log.info("Parse completed successfully for file: {}, extracted {} images, {} tables",
                        file.getOriginalFilename(),
                        result.getImages() != null ? result.getImages().size() : 0,
                        result.getTables() != null ? result.getTables().size() : 0);
            }

            return result;

        } catch (Exception e) {
            log.error("MinerU parse failed for file: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    public MinerUParseResult parseUrl(String url) {
        if (!properties.isEnabled()) {
            log.warn("MinerU is disabled");
            return null;
        }

        try {
            String taskId = submitUrlParseTask(url);
            if (taskId == null) {
                log.error("Failed to submit URL parse task");
                return null;
            }

            log.info("URL parse task submitted, taskId: {}, url: {}", taskId, url);
            return pollForResult(taskId);

        } catch (Exception e) {
            log.error("MinerU URL parse failed for url: {}", url, e);
            return null;
        }
    }

    private String submitParseTask(MultipartFile file) throws Exception {
        String uploadUrl = getUploadUrl();
        if (uploadUrl == null) {
            return null;
        }

        log.debug("Uploading file to MinerU: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        RestTemplate restTemplate = createRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (properties.getApiToken() != null && !properties.getApiToken().isBlank()) {
            headers.setBearerAuth(properties.getApiToken());
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return extractTaskId(response.getBody());
    }

    private String submitUrlParseTask(String url) throws Exception {
        RestTemplate restTemplate = createRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiToken() != null && !properties.getApiToken().isBlank()) {
            headers.setBearerAuth(properties.getApiToken());
        }

        String requestBody = String.format("""
                {
                    "url": "%s",
                    "model_version": "%s"
                }
                """, url, properties.getModelVersion());

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        String apiUrl = properties.getApiBaseUrl() + "/v4/extract/task";

        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return extractTaskId(response.getBody());
    }

    private String getUploadUrl() throws Exception {
        RestTemplate restTemplate = createRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiToken() != null && !properties.getApiToken().isBlank()) {
            headers.setBearerAuth(properties.getApiToken());
        }

        String requestBody = String.format("""
                {
                    "model_version": "%s"
                }
                """, properties.getModelVersion());

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        String apiUrl = properties.getApiBaseUrl() + "/v4/file-urls/batch";

        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        if (root.has("data") && root.get("data").isArray() && root.get("data").size() > 0) {
            return root.get("data").get(0).get("upload_url").asText();
        }

        log.error("Failed to get upload URL, response: {}", response.getBody());
        return null;
    }

    private String extractTaskId(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.has("data")) {
            JsonNode data = root.get("data");
            if (data.has("task_id")) {
                return data.get("task_id").asText();
            }
            if (data.isTextual()) {
                return data.asText();
            }
        }
        log.error("Failed to extract task_id from response: {}", responseBody);
        return null;
    }

    private MinerUParseResult pollForResult(String taskId) throws Exception {
        RestTemplate restTemplate = createRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiToken() != null && !properties.getApiToken().isBlank()) {
            headers.setBearerAuth(properties.getApiToken());
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        String taskUrl = properties.getApiBaseUrl() + "/v4/extract/task/" + taskId;

        for (int attempt = 0; attempt < properties.getMaxPollingAttempts(); attempt++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    taskUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("data").path("status").asText("");

            log.debug("Polling attempt {} for task {}: status={}", attempt + 1, taskId, status);

            if ("completed".equals(status) || "success".equals(status)) {
                return parseResult(root.path("data"));
            } else if ("failed".equals(status) || "error".equals(status)) {
                String errorMsg = root.path("data").path("error").asText("Unknown error");
                log.error("Parse task failed: {}", errorMsg);
                return null;
            }

            Thread.sleep(properties.getPollingIntervalMs());
        }

        log.warn("Parse task timed out after {} attempts", properties.getMaxPollingAttempts());
        return null;
    }

    private MinerUParseResult parseResult(JsonNode dataNode) throws Exception {
        MinerUParseResult result = new MinerUParseResult();

        String markdownUrl = dataNode.path("markdown_url").asText(null);
        String jsonUrl = dataNode.path("json_url").asText(null);

        if (markdownUrl != null) {
            String markdown = downloadContent(markdownUrl);
            result.setMarkdownContent(markdown);
            result.setTextContent(extractTextFromMarkdown(markdown));
        }

        if (jsonUrl != null) {
            String jsonContent = downloadContent(jsonUrl);
            result.setJsonContent(jsonContent);
            parseJsonStructure(jsonContent, result);
        }

        return result;
    }

    private String downloadContent(String url) throws Exception {
        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);
        return response.getBody();
    }

    private String extractTextFromMarkdown(String markdown) {
        if (markdown == null) {
            return "";
        }
        String text = markdown
                .replaceAll("!\\[.*?\\]\\(.*?\\)", " ")
                .replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("#+\\s", " ")
                .replaceAll("\\*+", " ")
                .replaceAll("_+", " ")
                .replaceAll("`+", " ")
                .replaceAll("\\|.*?\\|", " ")
                .replaceAll("---+", " ")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                .replaceAll("\\s+", " ")
                .trim();
        return text;
    }

    private void parseJsonStructure(String jsonContent, MinerUParseResult result) throws Exception {
        JsonNode root = objectMapper.readTree(jsonContent);

        List<MinerUParseResult.MinerUImage> images = new ArrayList<>();
        List<MinerUParseResult.MinerUTable> tables = new ArrayList<>();
        List<MinerUParseResult.MinerUFormula> formulas = new ArrayList<>();

        traverseJson(root, images, tables, formulas);

        result.setImages(images);
        result.setTables(tables);
        result.setFormulas(formulas);
    }

    private void traverseJson(JsonNode node, List<MinerUParseResult.MinerUImage> images,
                              List<MinerUParseResult.MinerUTable> tables,
                              List<MinerUParseResult.MinerUFormula> formulas) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            String type = node.path("type").asText("");

            if (("image".equals(type) || "figure".equals(type)) && properties.isExtractImages()) {
                MinerUParseResult.MinerUImage image = new MinerUParseResult.MinerUImage();
                image.setUrl(node.path("url").asText(null));
                image.setPath(node.path("path").asText(null));
                image.setCaption(node.path("caption").asText(null));
                image.setAlt(node.path("alt").asText(null));
                if (image.getUrl() != null || image.getPath() != null) {
                    images.add(image);
                }
            } else if (("table".equals(type)) && properties.isExtractTables()) {
                MinerUParseResult.MinerUTable table = new MinerUParseResult.MinerUTable();
                table.setHtml(node.path("html").asText(null));
                table.setMarkdown(node.path("markdown").asText(null));
                table.setCaption(node.path("caption").asText(null));
                if (table.getHtml() != null || table.getMarkdown() != null) {
                    tables.add(table);
                }
            } else if (("formula".equals(type) || "equation".equals(type)) && properties.isExtractFormulas()) {
                MinerUParseResult.MinerUFormula formula = new MinerUParseResult.MinerUFormula();
                formula.setLatex(node.path("latex").asText(null));
                formula.setInline(node.path("inline").asBoolean(false));
                if (formula.getLatex() != null) {
                    formulas.add(formula);
                }
            }

            node.fields().forEachRemaining(entry -> traverseJson(entry.getValue(), images, tables, formulas));
        } else if (node.isArray()) {
            node.forEach(child -> traverseJson(child, images, tables, formulas));
        }
    }
}
