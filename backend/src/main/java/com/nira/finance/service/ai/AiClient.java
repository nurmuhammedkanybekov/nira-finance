package com.nira.finance.service.ai;

import com.nira.finance.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * Thin abstraction over whichever LLM provider you use (Anthropic, OpenAI,
 * a local model, ...). Keeping this as an interface means the rest of the
 * app never depends on a specific vendor's SDK, and it's trivial to mock
 * in tests.
 */
public interface AiClient {

    /** Ask the model which of the user's existing categories best fits this transaction. */
    Optional<Long> categorize(String description, String merchant, List<Category> categories);

    /** Embed a piece of text into a vector for similarity search (RAG). */
    float[] embed(String text);

    /** Answer a free-form question given retrieved context chunks. */
    String answerWithContext(String question, List<String> contextChunks);
}
