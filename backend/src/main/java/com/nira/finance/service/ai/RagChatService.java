package com.nira.finance.service.ai;

import com.nira.finance.dto.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagChatService {

    private static final int TOP_K = 8;

    private final FinanceEmbeddingRepository embeddingRepository;
    private final AiClient aiClient;

    public RagChatService(FinanceEmbeddingRepository embeddingRepository, AiClient aiClient) {
        this.embeddingRepository = embeddingRepository;
        this.aiClient = aiClient;
    }

    public ChatResponse ask(Long userId, String question) {
        float[] queryEmbedding = aiClient.embed(question);
        List<String> context = embeddingRepository.findSimilarContent(userId, queryEmbedding, TOP_K);

        if (context.isEmpty()) {
            return new ChatResponse(
                    "I don't have any indexed financial data for you yet. Add some transactions " +
                            "and hit \"reindex\", then ask again.",
                    List.of());
        }

        String answer = aiClient.answerWithContext(question, context);
        return new ChatResponse(answer, context);
    }
}
