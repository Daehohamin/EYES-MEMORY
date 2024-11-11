package com.example.eyesmemory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WordPair {
    private String word;
    private List<String> options;
    private String answer;

    public WordPair(JSONObject jsonObject) throws JSONException {
        this.word = jsonObject.getString("word");
        this.answer = jsonObject.getString("answer");
        this.options = new ArrayList<>();
        JSONArray optionsArray = jsonObject.getJSONArray("options");
        for (int i = 0; i < optionsArray.length(); i++) {
            this.options.add(optionsArray.getString(i));
        }
    }

    // Getters
    public String getWord() { return word; }
    public List<String> getOptions() { return options; }
    public String getAnswer() { return answer; }
}