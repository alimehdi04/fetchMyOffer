package com.autisheimer.fetchMyOfferMicroService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service
public class TelegramNotificationService {

    private final RestClient restClient;
    private final String botToken;
    private final String chatId;

    // Injecting the RestClient Builder and our Telegram credentials
    public TelegramNotificationService(
            RestClient.Builder restClientBuilder,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.chat-id}") String chatId) {

        // Base URL for the Telegram API
        this.restClient = restClientBuilder.baseUrl("https://api.telegram.org").build();
        this.botToken = botToken;
        this.chatId = chatId;
    }

    public void sendJobMatchNotification(String jobTitle, String companyName, String url, String aiReasoning) {
        // Format the message with basic HTML so it looks pretty on your phone
        String message = String.format("""
                🚨 <b>NEW JOB MATCH!</b> 🚨
                
                💼 <b>Job:</b> %s
                🏢 <b>Company:</b> %s
                
                🤖 <b>Groq's Evaluation:</b>
                %s
                
                🔗 <a href="%s">Apply Here</a>
                """, jobTitle, companyName, aiReasoning, url);

        // Telegram API expects a JSON payload
        Map<String, Object> payload = Map.of(
                "chat_id", chatId,
                "text", message,
                "parse_mode", "HTML",     // Tells Telegram to render the bold tags and links
                "disable_web_page_preview", true // Keeps the chat clean
        );

        try {
            // Fire the HTTP POST request to Telegram
            restClient.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("📱 Telegram notification sent to your phone!");
        } catch (Exception e) {
            System.err.println("❌ Failed to send Telegram notification: " + e.getMessage());
        }
    }
}