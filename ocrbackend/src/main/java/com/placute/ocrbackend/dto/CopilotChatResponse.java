package com.placute.ocrbackend.dto;

import java.util.List;
import java.util.Map;

public class CopilotChatResponse {

    private String answer;
    private String intent;
    private String tool;
    private String deepLink;
    private Map<String, Object> data;
    private List<String> suggestions;
    private boolean usedAiPlanner;

    public CopilotChatResponse() {
    }

    public CopilotChatResponse(
            String answer,
            String intent,
            String tool,
            String deepLink,
            Map<String, Object> data,
            List<String> suggestions,
            boolean usedAiPlanner
    ) {
        this.answer = answer;
        this.intent = intent;
        this.tool = tool;
        this.deepLink = deepLink;
        this.data = data;
        this.suggestions = suggestions;
        this.usedAiPlanner = usedAiPlanner;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public boolean isUsedAiPlanner() {
        return usedAiPlanner;
    }

    public void setUsedAiPlanner(boolean usedAiPlanner) {
        this.usedAiPlanner = usedAiPlanner;
    }
}
