package com.nira.finance.dto;

import java.util.List;

public class ChatResponse {
    private String answer;
    private List<String> sourcesUsed;

    public ChatResponse(String answer, List<String> sourcesUsed) {
        this.answer = answer;
        this.sourcesUsed = sourcesUsed;
    }
    public String getAnswer() { return answer; }
    public List<String> getSourcesUsed() { return sourcesUsed; }
}
