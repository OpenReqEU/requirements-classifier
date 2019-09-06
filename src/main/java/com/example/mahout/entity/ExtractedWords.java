package com.example.mahout.entity;

import com.google.common.collect.Multiset;

public class ExtractedWords {

    private Multiset<String> words;
    private int wordCount = 0;

    public ExtractedWords(Multiset<String> words, int wordCount) {
        this.words = words;
        this.wordCount = wordCount;
    }

    public Multiset<String> getWords() {
        return words;
    }

    public int getWordCount() {
        return wordCount;
    }
}
