package com.note.service;

import com.note.entity.SensitiveWord;
import com.note.mapper.SensitiveWordMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")

@Service
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    private final Map<Character, Map> dfaTree = new ConcurrentHashMap<>();

    public SensitiveWordService(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @PostConstruct
    public void init() {
        loadSensitiveWords();
    }

    public void loadSensitiveWords() {
        List<SensitiveWord> words = sensitiveWordMapper.selectActiveWords();
        dfaTree.clear();
        for (SensitiveWord word : words) {
            addWordToDfa(word.getWord());
        }
    }

    private void addWordToDfa(String word) {
        Map currentMap = dfaTree;
        for (char c : word.toCharArray()) {
            currentMap = (Map) currentMap.computeIfAbsent(c, k -> new ConcurrentHashMap<>());
        }
        currentMap.put("isEnd", true);
    }

    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            Map currentMap = dfaTree;
            for (int j = i; j < text.length(); j++) {
                char c = text.charAt(j);
                if (!currentMap.containsKey(c)) {
                    break;
                }
                currentMap = (Map) currentMap.get(c);
                if (currentMap.containsKey("isEnd")) {
                    return true;
                }
            }
        }
        return false;
    }

    public void refreshCache() {
        loadSensitiveWords();
    }
}
