package com.example.mahout;

import com.example.mahout.service.ClassificationService;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;

public class TestLucene {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationService.class);

    public static void main(String args[]) throws Exception {
        PorterStemmer stemmer = new PorterStemmer();
        //TODO load and store file
        File file = new File("data/conventionall(DEF-Prose)");
        InputStream inputStream= new FileInputStream(file);
        JSONObject jsonObject = new JSONObject(IOUtils.toString(inputStream));
        JSONArray jsonArray = jsonObject.getJSONArray("requirements");
        logger.info("Loaded file content. Start preprocessing...");
        Calendar start = Calendar.getInstance();


        for (int i = 0; i < 10; ++i) {
            String text = jsonArray.getJSONObject(i).getString("text");
            System.out.println("From --> " + text);
            //Apply Capitalization
            text = text.toLowerCase(Locale.ENGLISH);
            TokenStream tokenStream = new StandardTokenizer(
                    Version.LUCENE_46, new StringReader(text));

            //Apply stopword filter
            tokenStream = new StopFilter(Version.LUCENE_46, tokenStream, EnglishAnalyzer.getDefaultStopSet());

            //Apply stem filter
            tokenStream = new PorterStemFilter(tokenStream);
            tokenStream.reset();

            StringBuilder sb = new StringBuilder();
            CharTermAttribute charTermAttr = tokenStream.getAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(charTermAttr.toString());
            }
            System.out.println("To --> " + sb.toString() + "\n");
            jsonArray.getJSONObject(i).put("text", sb.toString());
        }
        Calendar stop = Calendar.getInstance();
    }

}
