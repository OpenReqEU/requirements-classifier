package com.example.mahout;


import com.example.mahout.entity.ExtractedWords;
import com.example.mahout.entity.Requirement;
import com.example.mahout.util.Control;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.vectorizer.TFIDF;
import org.codehaus.jettison.json.JSONException;


import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ReqToTestSet {

    private ReqToTestSet() {
        //utility class
    }

    private static Map<String, Integer> readDictionary(Configuration conf, Path dictionnaryPath) {
        Map<String, Integer> dictionnary = new HashMap<>();
        SequenceFileIterable<Text, IntWritable> seq = new SequenceFileIterable<>(dictionnaryPath, true, conf);

        Iterator<Pair<Text, IntWritable>> iterator = seq.iterator();
        while (iterator.hasNext()) {
            Pair<Text, IntWritable> pair = iterator.next();
            dictionnary.put(pair.getFirst().toString(), pair.getSecond().get());
        }
        return dictionnary;
    }

    private static Map<Integer, Long> readDocumentFrequency(Configuration conf, Path documentFrequencyPath) {
        Map<Integer, Long> documentFrequency = new HashMap<>();
        SequenceFileIterable<IntWritable, LongWritable> seq = new SequenceFileIterable<>(documentFrequencyPath, true, conf);

        Iterator<Pair<IntWritable, LongWritable>> iterator = seq.iterator();
        while (iterator.hasNext()) {
            Pair<IntWritable, LongWritable> pair = iterator.next();
            documentFrequency.put(pair.getFirst().get(), pair.getSecond().get());
        }
        return documentFrequency;
    }

    public static ExtractedWords extractWordsFromReq(Analyzer analyzer, String req, Map<String, Integer> dictionary) throws IOException {
        Multiset<String> words = ConcurrentHashMultiset.create();

        TokenStream ts = analyzer.tokenStream("text", new StringReader(req));
        CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        int wordCount = 0;
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
        ts.close();
        return new ExtractedWords(words,wordCount);
    }

    public static void createTestSet(String freqPath, String dictionaryPath, List<Requirement> requirements, String destinyPath) throws IOException, JSONException {
        Configuration configuration = new Configuration();
        FileSystem fs = FileSystem.get(configuration);

        Map<String, Integer> dictionary = readDictionary(configuration, new Path(dictionaryPath));
        Map<Integer, Long> frequency = readDocumentFrequency(configuration, new Path(freqPath));

        Control.getInstance().showInfoMessage(frequency.size()+"");
        int documentCount = frequency.get(-1).intValue();

        SequenceFile.Writer writer = new SequenceFile.Writer(fs, configuration, new Path(destinyPath+"/testSet"), Text.class, VectorWritable.class);
        Text key = new Text();
        VectorWritable value = new VectorWritable();

        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
        for (int i = 0; i < requirements.size(); i++) {
            Requirement requirement = requirements.get(i);

            String id = requirement.getId();
            String category = requirement.getRequirementType();
            String req = requirement.getText();

            key.set("/" + category + "/" + id);

            ExtractedWords extractedWords = extractWordsFromReq(analyzer,req,dictionary);
            Multiset<String> words = extractedWords.getWords();

            /* Create vector wordId ==> weight using tfidf */
            Vector vector = new RandomAccessSparseVector(10000);
            TFIDF tfidf = new TFIDF();
            for (Multiset.Entry<String> entry:words.entrySet()) {
                String word = entry.getElement();
                int count = entry.getCount();
                Integer wordId = dictionary.get(word);
                Long freq = frequency.get(wordId);
                /* Add only if frequency of the word is present*/
                if(freq != null) {
                    double tfIdfValue = tfidf.calculate(count, freq.intValue(), extractedWords.getWordCount(), documentCount);
                    vector.setQuick(wordId, tfIdfValue);
                }
            }
            value.set(vector);

            writer.append(key, value);
        }
        writer.close();

    }
}
