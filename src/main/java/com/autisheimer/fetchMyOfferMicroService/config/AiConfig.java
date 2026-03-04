package com.autisheimer.fetchMyOfferMicroService.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions.TaskType;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class AiConfig {

    // As we use OpenAI provider for groq, it attached "extra_body" parameter with the request, groq is quite strict about it and rejects the request, to tackle it we strip the parameter away from the request

    // ── Interceptor: strips "extra_body" key before sending to Groq ──────────
    static class GroqCompatibilityInterceptor implements ClientHttpRequestInterceptor {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public ClientHttpResponse intercept(
                HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {

            try {
                // Parse the outgoing JSON body
                ObjectNode json = (ObjectNode) objectMapper.readTree(body);
                // Remove the offending field Groq doesn't understand
                json.remove("extra_body");
                // Re-serialize the cleaned body
                byte[] cleanBody = objectMapper.writeValueAsBytes(json);
                return execution.execute(request, cleanBody);
            } catch (Exception e) {
                // If parsing fails for any reason, send original body
                return execution.execute(request, body);
            }
        }
    }

    // ── Bean 1: Groq API with the stripping interceptor ───────────────────────
    @Bean
    public OpenAiApi groqApi(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl) {

        RestClient.Builder cleanRestClient = RestClient.builder()
                .requestInterceptor(new GroqCompatibilityInterceptor());

        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(cleanRestClient)
                .build();
    }

    // ── Bean 2: Groq Chat Model ───────────────────────────────────────────────
    @Bean
    @Primary
    public OpenAiChatModel groqChatModel(OpenAiApi groqApi) {
        return OpenAiChatModel.builder()
                .openAiApi(groqApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("llama-3.3-70b-versatile")
                        .temperature(0.2)
                        .build())
                .build();
    }

    // ── Bean 3: ChatClient wrapping Groq ─────────────────────────────────────
    @Bean
    public ChatClient groqChatClient(@Qualifier("groqChatModel") OpenAiChatModel groqChatModel) {
        return ChatClient.create(groqChatModel);
    }

    // ── Bean 4: Gemini Embedding ──────────────────────────────────────────────
    @Bean
    @Primary
    public EmbeddingModel geminiEmbeddingModel(
            @Value("${spring.ai.google.genai.api-key}") String apiKey) {

        GoogleGenAiEmbeddingConnectionDetails connectionDetails =
                GoogleGenAiEmbeddingConnectionDetails.builder()
                        .apiKey(apiKey)
                        .build();

        GoogleGenAiTextEmbeddingOptions options =
                GoogleGenAiTextEmbeddingOptions.builder()
                        .model("gemini-embedding-001")
                        .dimensions(768)
                        .taskType(TaskType.RETRIEVAL_DOCUMENT)
                        .build();

        return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
    }
}
