package com.nira.finance.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nira.finance.model.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Talks to the Anthropic Messages API for categorization and Q&A.
 *
 * NOTE on embeddings: Anthropic doesn't offer an embeddings endpoint.
 * `embed()` below is a lightweight, dependency-free placeholder (a hashed
 * bag-of-words vector) so the RAG pipeline works end-to-end out of the box.
 * For real semantic search, swap it for a proper embeddings API such as
 * Voyage AI (Anthropic's recommended partner) or OpenAI embeddings - same
 * interface, just a different implementation of AiClient.embed().
 */
@Component
public class AnthropicAiClient implements AiClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final int EMBED_DIM = 1536;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String model;

    public AnthropicAiClient(
            @Value("${nira.ai.provider-api-key}") String apiKey,
            @Value("${nira.ai.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public Optional<Long> categorize(String description, String merchant, List<Category> categories) {
        if (categories.isEmpty()) return Optional.empty();

        String categoryList = categories.stream()
                .map(c -> c.getId() + ": " + c.getName())
                .collect(Collectors.joining("\n"));

        String prompt = """
                You categorize a personal-finance transaction into exactly one existing category.
                Reply with ONLY the numeric category id, nothing else.

                Transaction description: %s
                Merchant: %s

                Available categories (id: name):
                %s
                """.formatted(
                        description == null ? "(none)" : description,
                        merchant == null ? "(none)" : merchant,
                        categoryList);

        try {
            String reply = callMessages(prompt, 20).trim();
            long id = Long.parseLong(reply.replaceAll("[^0-9]", ""));
            boolean valid = categories.stream().anyMatch(c -> c.getId() == id);
            return valid ? Optional.of(id) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public String answerWithContext(String question, List<String> contextChunks) {
        String context = String.join("\n---\n", contextChunks);
        String prompt = """
                You are a helpful personal finance assistant. Answer the user's question
                using ONLY the financial context below. If the context doesn't contain
                enough information to answer, say so plainly instead of guessing.

                Context:
                %s

                Question: %s
                """.formatted(context, question);

        try {
            return callMessages(prompt, 600);
        } catch (Exception e) {
            return "Sorry, I couldn't reach the AI service right now. (" + e.getMessage() + ")";
        }
    }

    private String callMessages(String userPrompt, int maxTokens) throws Exception {
        String body = mapper.writeValueAsString(new MessagesRequest(model, maxTokens, userPrompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Anthropic API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        return root.path("content").get(0).path("text").asText();
    }

    @Override
    public float[] embed(String text) {
        // Deterministic hashed bag-of-words placeholder - see class javadoc.
        float[] vector = new float[EMBED_DIM];
        for (String token : text.toLowerCase().split("\\W+")) {
            if (token.isBlank()) continue;
            int bucket = Math.floorMod(hash(token), EMBED_DIM);
            vector[bucket] += 1f;
        }
        normalize(vector);
        return vector;
    }

    private int hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        } catch (Exception e) {
            return token.hashCode();
        }
    }

    private void normalize(float[] vector) {
        double sumSquares = 0;
        for (float v : vector) sumSquares += v * v;
        if (sumSquares == 0) return;
        float norm = (float) Math.sqrt(sumSquares);
        for (int i = 0; i < vector.length; i++) vector[i] /= norm;
    }

    private record MessagesRequest(String model, int max_tokens, java.util.List<Message> messages) {
        MessagesRequest(String model, int maxTokens, String userText) {
            this(model, maxTokens, java.util.List.of(new Message("user", userText)));
        }
    }

    private record Message(String role, String content) {}
}
