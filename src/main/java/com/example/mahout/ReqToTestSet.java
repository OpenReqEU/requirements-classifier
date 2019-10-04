package com.example.mahout;

import com.google.common.collect.Multiset;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.Map;

public class ReqToTestSet {

    static int getWordCount(Map<String, Integer> dictionary, Multiset<String> words, TokenStream ts, CharTermAttribute termAtt, int wordCount) throws IOException {
        while(ts.incrementToken()) {
            if (termAtt.length()> 0) {
                String word = ts.getAttribute(CharTermAttribute.class).toString();
                Integer wordId = dictionary.get(word);
                /* if the word is not in the dictionary, skip it */
                if (wordId != null) {
                    words.add(word);
                    wordCount++;
                }
            }
        }
        return wordCount;
    }
}
