package com.autisheimer.fetchMyOfferMicroService.config;

import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions.TaskType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    // ── Bean 1: Groq API using the Builder (correct API for this version) ───
    @Bean
    public OpenAiApi groqApi(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    // ── Bean 2: Groq Chat Model using the Builder ────────────────────────────
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

    // ── Bean 4: Gemini Embedding (manual, marked Primary) ────────────────────
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

//package com.autisheimer.fetchMyOfferMicroService.config;
//
////import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions.TaskType;
////import org.springframework.ai.chat.client.ChatClient;
////import org.springframework.ai.chat.model.ChatModel; // 🛑 NEW IMPORT
////import org.springframework.ai.embedding.EmbeddingModel;
////import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
////import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
////import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
////import org.springframework.beans.factory.annotation.Value;
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////
////@Configuration
////public class AiConfig {
////
////    //  Inject the ChatModel directly instead of the Builder
////    @Bean
////    public ChatClient groqChatClient(ChatModel chatModel) {
////        // Create the builder manually using the injected model
////        return ChatClient.builder(chatModel).build();
////    }
////
////    //  Gemini Memory Model
////    @Bean
////    public EmbeddingModel geminiEmbeddingModel(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
////        GoogleGenAiEmbeddingConnectionDetails connectionDetails = GoogleGenAiEmbeddingConnectionDetails.builder()
////                .apiKey(apiKey).build();
////
////        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
////                .model("gemini-embedding-001")
////                .dimensions(768)
////                .taskType(TaskType.RETRIEVAL_DOCUMENT).build();
////
////        return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
////    }
////}
//////package com.autisheimer.fetchMyOfferMicroService.config;
//
//import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions.TaskType;
//import io.micrometer.observation.ObservationRegistry;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.google.genai.GoogleGenAiChatModel;
//import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
//import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
//import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
//import org.springframework.ai.model.SimpleApiKey;
//import org.springframework.ai.openai.OpenAiChatModel;
//import org.springframework.ai.openai.OpenAiChatOptions;
//import org.springframework.ai.openai.api.OpenAiApi;
//import org.springframework.ai.retry.RetryUtils;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.web.client.RestClient;
//
//
//
////
////import org.springframework.ai.chat.client.ChatClient;
////import org.springframework.ai.embedding.EmbeddingModel;
////import org.springframework.ai.google.genai.GoogleGenAiChatModel;
////import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
////import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
////import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
////import org.springframework.ai.openai.OpenAiChatModel;
////import org.springframework.beans.factory.annotation.Value;
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import org.springframework.context.annotation.Primary; // <-- 1. NEW IMPORT
////
////@Configuration
////public class AiConfig {
////
////    // 🧠 Bean 1: The Groq Brain (For strict job grading)
////    @Bean
////    public ChatClient groqChatClient(OpenAiChatModel groqChatModel) {
////        return ChatClient.create(groqChatModel);
////    }
////
////    // 🎨 Bean 2: The Gemini Brain (For creative summaries later)
////    @Bean
////    public ChatClient geminiChatClient(GoogleGenAiChatModel geminiChatModel) {
////        return ChatClient.create(geminiChatModel);
////    }
////
////    // 🗄️ Bean 3: The Manual Gemini Embedding Engine
////    @Bean
////    @Primary // <-- 2. THE MAGIC BULLET. This makes our custom bean the absolute winner!
////    public EmbeddingModel geminiEmbeddingModel(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
////
////        System.out.println("🔧 Manually building Google GenAI Embedding Client with API Key...");
////
////        GoogleGenAiEmbeddingConnectionDetails connectionDetails = GoogleGenAiEmbeddingConnectionDetails.builder()
////                .apiKey(apiKey)
////                .build();
////
////        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
////                .model("gemini-embedding-001")
////                .dimensions(768)
////                .build();
////
////        return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
////    }
////}
//
////package com.autisheimer.fetchMyOfferMicroService.config;
//
//import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions.TaskType;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.google.genai.GoogleGenAiChatModel;
//import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
//import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
//import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
//import org.springframework.ai.openai.OpenAiChatModel;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.ai.openai.OpenAiChatModel;
//import org.springframework.ai.openai.api.OpenAiApi;
//import org.springframework.ai.openai.OpenAiChatOptions;
//
//@Configuration
//public class AiConfig {
//
//    // 🧠 Manual Groq API configuration to fix the "extra_body" error
//    @Bean
//    public OpenAiApi groqApi(@Value("${spring.ai.openai.api-key}") String apiKey,
//                             @Value("${spring.ai.openai.base-url}") String baseUrl) {
//        return new OpenAiApi(baseUrl, apiKey);
//    }
//
////    @Bean
////    public OpenAiChatModel groqChatModel(OpenAiApi groqApi) {
////        // We set the model defaults here manually to ensure Groq is happy
////        return new OpenAiChatModel(groqApi, OpenAiChatOptions.builder()
////                .model("llama-3.3-70b-versatile")
////                .temperature(0.2)
////                .build());
////    }
//    @Bean
//    public OpenAiChatModel groqChatModel(OpenAiApi groqApi) {
//        return new OpenAiChatModel(groqApi, OpenAiChatOptions.builder()
//                .model("llama-3.3-70b-versatile") // Use .withModel in some versions
//                .temperature(0.2)
//                .build());
//    }
//
//    @Bean
//    public ChatClient groqChatClient(OpenAiChatModel groqChatModel) {
//        return ChatClient.create(groqChatModel);
//    }
//
//    // 🎨 Bean 2: The Gemini Brain (For creative summaries later)
//    @Bean
//    public ChatClient geminiChatClient(GoogleGenAiChatModel geminiChatModel) {
//        return ChatClient.create(geminiChatModel);
//    }
//
//    // 🗄️ Bean 3: Manual Gemini Embedding (Directly from Official Docs)
//    @Bean
//    @Primary // Ensures pgvector picks this model
//    public EmbeddingModel geminiEmbeddingModel(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
//
//        System.out.println("🔧 Building Google GenAI Embedding Client from Official Specs...");
//
//        // 1. Connection Details using ONLY the API Key
//        GoogleGenAiEmbeddingConnectionDetails connectionDetails = GoogleGenAiEmbeddingConnectionDetails.builder()
//                .apiKey(apiKey)
//                .build();
//
//        // 2. Options configured exactly as docs recommend for RAG
//        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
//                .model("gemini-embedding-001") // SOTA model from the docs
//                .dimensions(768)               // Force truncation to fit our database
//                .taskType(TaskType.RETRIEVAL_DOCUMENT) // Optimize vectors for document search
//                .build();
//
//        // 3. Return the fully configured embedding model
//        return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
//    }
//}