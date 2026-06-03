package com.example.agentplatform.tools.service;

import com.example.agentplatform.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.example.agentplatform.tools.common.JsonUtils.extractJson;

/**
 * 统一AI调用服务
 * <p>封装 Spring AI ChatClient，提供统一的 AI 分析接口
 * <p>支持降级模式，当 AI 服务不可用时自动切换到本地规则引擎
 * <p>提供泛型的分析方法，支持自定义解析器和降级策略
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAIService {

    /**
     * AI聊天客户端，用于调用大语言模型进行智能分析
     */
    private final ChatClient chatClient;

    /**
     * JSON对象映射器，用于序列化和反序列化数据
     */
    private final ObjectMapper objectMapper;

    /**
     * AI模型名称，从配置文件读取，默认值：spring-ai
     */
    @Value("${agent.platform.default-model:spring-ai}")
    private String modelName;

    /**
     * 是否启用AI降级模式，从配置文件读取，默认启用
     * <p>当AI服务不可用时，自动切换到本地规则引擎生成分析结果
     */
    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    /**
     * AI分析响应包装类
     * <p>封装AI分析结果，包含数据、模型信息、token消耗等
     * 
     * @param <T> 分析结果的数据类型
     */
    public static class AIResponse<T> {
        /**
         * 分析结果数据
         */
        private final T data;
        /**
         * 使用的AI模型名称
         */
        private final String model;
        /**
         * 消耗的token数量
         */
        private final Integer tokens;
        /**
         * 是否为降级模式返回的结果
         */
        private final boolean fallback;

        /**
         * 构造AI响应对象
         * 
         * @param data 分析结果数据
         * @param model 使用的AI模型名称
         * @param tokens 消耗的token数量
         * @param fallback 是否为降级模式
         */
        public AIResponse(T data, String model, Integer tokens, boolean fallback) {
            this.data = data;
            this.model = model;
            this.tokens = tokens;
            this.fallback = fallback;
        }

        /**
         * 获取分析结果数据
         * 
         * @return 分析结果数据
         */
        public T getData() {
            return data;
        }

        /**
         * 获取使用的AI模型名称
         * 
         * @return 模型名称
         */
        public String getModel() {
            return model;
        }

        /**
         * 获取消耗的token数量
         * 
         * @return token数量
         */
        public Integer getTokens() {
            return tokens;
        }

        /**
         * 是否为降级模式返回的结果
         * 
         * @return true表示降级模式，false表示正常AI响应
         */
        public boolean isFallback() {
            return fallback;
        }
    }

    /**
     * 调用AI聊天API
     * <p>构建系统消息和用户消息，调用ChatClient获取响应
     * 
     * @param systemPrompt 系统提示词，定义AI角色和任务
     * @param userPrompt 用户提示词，包含具体数据和请求
     * @return AI响应对象
     */
    public ChatResponse callChatApi(String systemPrompt, String userPrompt) {
        List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        );
        Prompt prompt = new Prompt(messages);
        return chatClient.call(prompt);
    }

    /**
     * 调用AI聊天API（多模态，支持图片）
     * <p>构建系统消息和包含图片的用户消息，调用ChatClient获取响应
     * <p>图片通过Spring AI的Media对象以Resource形式传递，AI模型可真正"看到"图片内容
     * 
     * @param systemPrompt 系统提示词，定义AI角色和任务
     * @param userPrompt 用户提示词，文字描述部分
     * @param imageBase64 图片的Base64编码数据（不含data:前缀）
     * @param imageMimeType 图片的MIME类型，如image/jpeg
     * @return AI响应对象
     */
    public ChatResponse callChatApiWithImage(String systemPrompt, String userPrompt,
                                              String imageBase64, String imageMimeType) {
        byte[] imageBytes = java.util.Base64.getDecoder().decode(imageBase64);
        ByteArrayResource imageResource = new ByteArrayResource(imageBytes);
        MimeType mimeType = MimeType.valueOf(imageMimeType);
        Media imageMedia = new Media(mimeType, imageResource);

        List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt, List.of(imageMedia))
        );
        Prompt prompt = new Prompt(messages);
        return chatClient.call(prompt);
    }

    /**
     * 使用AI进行多模态分析（支持图片），自动降级
     * <p>发送包含图片的多模态消息给AI模型进行分析
     * <p>当AI服务不可用时，如果启用降级模式且提供了降级策略，则返回降级结果
     * 
     * @param <T> 分析结果的类型
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户文本提示词
     * @param imageBase64 图片Base64数据（不含data:前缀）
     * @param imageMimeType 图片MIME类型
     * @param parser JSON解析函数
     * @param fallbackSupplier 降级策略提供者
     * @return AI分析响应包装对象
     * @throws BusinessException AI分析失败且未启用降级时抛出
     */
    public <T> AIResponse<T> analyzeWithImage(
            String systemPrompt,
            String userPrompt,
            String imageBase64,
            String imageMimeType,
            Function<JsonNode, T> parser,
            Supplier<T> fallbackSupplier) {

        long startTime = System.currentTimeMillis();

        try {
            ChatResponse response = callChatApiWithImage(systemPrompt, userPrompt, imageBase64, imageMimeType);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                    response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            String jsonContent = extractJson(content);
            JsonNode root = objectMapper.readTree(jsonContent);

            T result = parser.apply(root);

            log.debug("多模态AI分析完成，耗时: {}ms, tokens: {}", System.currentTimeMillis() - startTime, tokens);

            return new AIResponse<>(result, modelName, tokens, false);

        } catch (Exception e) {
            log.error("多模态AI分析失败", e);
            if (fallbackEnabled && fallbackSupplier != null) {
                log.warn("多模态AI分析失败，使用降级模式返回结果");
                return new AIResponse<>(fallbackSupplier.get(), "fallback", null, true);
            }
            throw new BusinessException("AI分析失败: " + e.getMessage());
        }
    }

    /**
     * 使用AI进行分析，支持自定义解析器和降级策略
     * <p>自动提取AI响应中的JSON内容并使用自定义解析器转换为目标类型
     * <p>当AI服务不可用时，如果启用降级模式且提供了降级策略，则返回降级结果
     * 
     * @param <T> 分析结果的类型
     * @param systemPrompt 系统提示词，定义AI角色和任务
     * @param userPrompt 用户提示词，包含具体数据和请求
     * @param parser JSON解析函数，将JsonNode转换为目标类型
     * @param fallbackSupplier 降级策略提供者，AI失败时调用
     * @return AI分析响应包装对象
     * @throws BusinessException AI分析失败且未启用降级时抛出
     */
    public <T> AIResponse<T> analyzeWithAI(
            String systemPrompt,
            String userPrompt,
            Function<JsonNode, T> parser,
            Supplier<T> fallbackSupplier) {

        long startTime = System.currentTimeMillis();

        try {
            ChatResponse response = callChatApi(systemPrompt, userPrompt);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                    response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            String jsonContent = extractJson(content);
            JsonNode root = objectMapper.readTree(jsonContent);

            T result = parser.apply(root);

            log.debug("AI分析完成，耗时: {}ms, tokens: {}", System.currentTimeMillis() - startTime, tokens);

            return new AIResponse<>(result, modelName, tokens, false);

        } catch (Exception e) {
            log.error("AI分析失败", e);
            if (fallbackEnabled && fallbackSupplier != null) {
                log.warn("使用降级模式返回结果");
                return new AIResponse<>(fallbackSupplier.get(), "fallback", null, true);
            }
            throw new BusinessException("AI分析失败: " + e.getMessage());
        }
    }

    /**
     * 使用AI进行分析，直接转换为指定类型
     * <p>使用 Jackson 的 convertValue 直接将 JsonNode 转换为目标类
     * 
     * @param <T> 分析结果的类型
     * @param systemPrompt 系统提示词，定义AI角色和任务
     * @param userPrompt 用户提示词，包含具体数据和请求
     * @param clazz 目标类的Class对象
     * @param fallbackSupplier 降级策略提供者，AI失败时调用
     * @return AI分析响应包装对象
     * @throws BusinessException AI分析失败且未启用降级时抛出
     */
    public <T> AIResponse<T> analyzeWithAI(
            String systemPrompt,
            String userPrompt,
            Class<T> clazz,
            Supplier<T> fallbackSupplier) {

        return analyzeWithAI(systemPrompt, userPrompt,
                root -> objectMapper.convertValue(root, clazz),
                fallbackSupplier);
    }

    /**
     * 使用AI进行分析，提取指定数组节点并转换为列表
     * <p>从AI响应中提取指定的数组节点，转换为目标类型列表
     * 
     * @param <T> 列表元素的类型
     * @param systemPrompt 系统提示词，定义AI角色和任务
     * @param userPrompt 用户提示词，包含具体数据和请求
     * @param arrayNodeName 数组节点的名称
     * @param typeRef 类型引用，用于描述泛型列表类型
     * @param fallbackSupplier 降级策略提供者，AI失败时调用
     * @return AI分析响应包装对象，包含解析后的列表
     * @throws BusinessException AI分析失败且未启用降级时抛出
     */
    public <T> AIResponse<List<T>> analyzeWithAIList(
            String systemPrompt,
            String userPrompt,
            String arrayNodeName,
            TypeReference<List<T>> typeRef,
            Supplier<List<T>> fallbackSupplier) {

        return analyzeWithAI(systemPrompt, userPrompt,
                root -> {
                    JsonNode arrayNode = root.path(arrayNodeName);
                    return objectMapper.convertValue(arrayNode, typeRef);
                },
                fallbackSupplier);
    }
}
