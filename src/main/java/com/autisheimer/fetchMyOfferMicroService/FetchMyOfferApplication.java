package com.autisheimer.fetchMyOfferMicroService;

import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
//import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration;

// 🛑 The Absolute Kill Switch: Physically blocks the buggy auto-configs from loading
@SpringBootApplication(exclude = {
		// Block duplicate OpenAI beans
		OpenAiEmbeddingAutoConfiguration.class,
		OpenAiChatAutoConfiguration.class,          // ← ADD THIS
		// Block duplicate Gemini beans
		GoogleGenAiTextEmbeddingAutoConfiguration.class,
		GoogleGenAiEmbeddingConnectionAutoConfiguration.class,
		GoogleGenAiChatAutoConfiguration.class      // ← ADD THIS
})
public class FetchMyOfferApplication {

	public static void main(String[] args) {
		SpringApplication.run(FetchMyOfferApplication.class, args);
	}

}