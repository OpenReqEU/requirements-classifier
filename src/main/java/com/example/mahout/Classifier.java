package com.example.mahout;

import com.example.mahout.DAO.CompanyModelDAO;
import com.example.mahout.DAO.CompanyModelDAOMySQL;
import com.example.mahout.entity.CompanyModel;
import com.example.mahout.entity.Requirement;
import com.example.mahout.entity.ResultId;
import com.example.mahout.service.ClassificationService;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.naivebayes.BayesUtils;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.classifier.naivebayes.StandardNaiveBayesClassifier;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.vectorizer.TFIDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class Classifier {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationService.class);

    public static final String TMP_FILES = "./tmpFiles/";
    private CompanyModelDAO companyModelDAO = new CompanyModelDAOMySQL();
    private String modelPath;
    private String labelindexPath;
    private String dictionaryPath;
    private String frequenciesPath;


    public Classifier() throws SQLException {
        //Default constructor for the DAO initialization
    }

    private static Map<String, Integer> readDictionary(Configuration conf, Path dictionnaryPath) {
        Map<String, Integer> dictionnary = new HashMap<String, Integer>();
        for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(dictionnaryPath, true, conf)) {
            dictionnary.put(pair.getFirst().toString(), pair.getSecond().get());
    }
    return dictionnary;
    }

    private static Map<Integer, Long> readDocumentFrequency(Configuration conf, Path documentFrequencyPath) {
        Map<Integer, Long> documentFrequency = new HashMap<Integer,Long>();
        for (Pair<IntWritable, LongWritable> pair: new SequenceFileIterable<IntWritable, LongWritable>(documentFrequencyPath, true, conf)) {
            documentFrequency.put(pair.getFirst().get(), pair.getSecond().get());
        }
        return documentFrequency;
    }

    private void createTmpFiles(CompanyModel companyModel, ResultId resultId) throws IOException {

        byte[] model_file_bytes = companyModel.getModel();
        byte[] labelindex_file_bytes = companyModel.getLabelindex();
        byte[] dictionary_file_bytes = companyModel.getDictionary();
        byte[] frequencies_file_bytes = companyModel.getFrequencies();

        Files.createDirectories(Paths.get(TMP_FILES + resultId.getId()));

        java.nio.file.Path tmp_model_file = Paths.get(TMP_FILES + resultId.getId() + "/naiveBayesModel.bin");
        Files.write(tmp_model_file, model_file_bytes);

        java.nio.file.Path tmp_labelindex_file = Paths.get(TMP_FILES + resultId.getId() + "/labelindex");
        Files.write(tmp_labelindex_file, labelindex_file_bytes);

        java.nio.file.Path tmp_dictionary_file = Paths.get(TMP_FILES+ resultId.getId() + "/dictionary.file-0");
        Files.write(tmp_dictionary_file, dictionary_file_bytes);

        java.nio.file.Path tmp_frequencies_file = Paths.get(TMP_FILES + resultId.getId()+ "/df-count");
        Files.write(tmp_frequencies_file, frequencies_file_bytes);

        modelPath = FilenameUtils.getPath(tmp_model_file.toString());
        labelindexPath = tmp_labelindex_file.toString();
        dictionaryPath = tmp_dictionary_file.toString();
        frequenciesPath = tmp_frequencies_file.toString();

        logger.info("Model Path: " + modelPath);
        logger.info("\nLabelindex Path: " + labelindexPath);
        logger.info("\nDictionary Path: " + dictionaryPath);
        logger.info("\nFrequencies Path: " + frequenciesPath + "\n");
    }

    private void deleteTmpFiles(ResultId resultId) {
        File model = new File(modelPath + "/naiveBayesModel.bin");
        File labelindex = new File(labelindexPath);
        File dictionary = new File(dictionaryPath);
        File frequencies = new File(frequenciesPath);

        try {
            Files.delete(model.toPath());
            Files.delete(labelindex.toPath());
            Files.delete(dictionary.toPath());
            Files.delete(frequencies.toPath());
            Files.delete(Paths.get(TMP_FILES + resultId.getId()));
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
        }

        logger.info("Files deleted correctly\n");
    }

    public  ArrayList<Pair<String, Pair<String, Double>>> classify(String companyName, List<Requirement> requirements, String property,
                                                                   ResultId resultId) throws Exception {

        /* Load the companyModel from the database */
        CompanyModel companyModel = companyModelDAO.findOne(companyName, property);

        if (companyModel == null)
            throw new Exception("Model not found");

        createTmpFiles(companyModel, resultId);

        Configuration configuration = new Configuration();


        //fileModel is a Matrix (wordId, labelId)
        NaiveBayesModel fileModel = NaiveBayesModel.materialize(new Path(modelPath), configuration);

        StandardNaiveBayesClassifier classifier = new StandardNaiveBayesClassifier(fileModel);

        //labels is a map label-->classId
        Map<Integer, String> labels = BayesUtils.readLabelIndex(configuration, new Path(labelindexPath));

        Map<String, Integer> dictionary = readDictionary(configuration, new Path(dictionaryPath));

        Map<Integer, Long> documentFrequency = readDocumentFrequency(configuration, new Path(frequenciesPath));


        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);

        int labelCount = labels.size();
        int documentCount = documentFrequency.get(-1).intValue();

        logger.info("Number of labels: " +  labelCount);
        logger.info("Number of documents in training set: " + documentCount);

        /* Return a list of pairs: Requirement, (Best caregory, Confidence) */
        ArrayList<Pair<String, Pair<String, Double>>> recomendations = new ArrayList<Pair<String,Pair<String, Double>>>();

        String csvFile = "";

        for (int k = 0; k < requirements.size() ;k++) {

            Requirement requirement = requirements.get(k);

            String reqId = requirement.getId();
            String req = requirement.getText();

            logger.info("Requirement: " + reqId + "\t" + req);

            Multiset<String> words = ConcurrentHashMultiset.create();

            /* Extract words from req. */
            TokenStream ts = analyzer.tokenStream("text", new StringReader(req));
            CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            int wordCount = 0;
            wordCount = ReqToTestSet.getWordCount(dictionary, words, ts, termAtt, wordCount);
            ts.close();
            Integer wordId = null;

            /* create vector wordId --> weight using tfdiff */
            Vector vector = new RandomAccessSparseVector(10000);
            TFIDF tfidf = new TFIDF();

            for (Multiset.Entry<String> entry: words.entrySet()) {
                String word = entry.getElement();
                int count = entry.getCount();
                wordId = dictionary.get(word);
                Long freq = documentFrequency.get(wordId);
                double tfIdfValue = tfidf.calculate(count, freq.intValue(), wordCount, documentCount);
                vector.setQuick(wordId, tfIdfValue);
            }

            /* With the classifier, we get one score for each label
              * The label with the highest score is the one the requirement is more likely to
              * be associated to*/
            Vector resultVector = classifier.classifyFull(vector);
            double bestScore = -Double.MAX_VALUE;
            int bestCategoryId = -1;
            for (int i = 0; i < resultVector.size(); i++) {
                Element element = resultVector.getElement(i);
                int categoryId = element.index();
                double score = element.get();
                if (score > bestScore) {
                    bestScore = score;
                    bestCategoryId = categoryId;
                }
                logger.info(" " + labels.get(categoryId) + ": " + score);
            }
            double confidence = 0;

            double defScore = resultVector.getElement(0).get();
            double proseScore = resultVector.getElement(1).get();
            double dif = Math.min(75.0, Math.abs(defScore - proseScore)) / 75.00;
            confidence = 50.0 + dif * 50.0 ;
            recomendations.add(new Pair(reqId, (new Pair(labels.get(bestCategoryId), confidence))));
            logger.info(" => " + labels.get(bestCategoryId) + "\tConfidence: "+ confidence);


        }

        deleteTmpFiles(resultId);
        return recomendations;
    }


}
