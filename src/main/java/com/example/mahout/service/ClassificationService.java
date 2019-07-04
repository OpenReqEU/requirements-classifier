package com.example.mahout.service;

import com.example.mahout.Classifier;
import com.example.mahout.DAO.CompanyModelDAO;
import com.example.mahout.DAO.CompanyModelDAOMySQL;
import com.example.mahout.ReqToTestSet;
import com.example.mahout.SamplesCreator;
import com.example.mahout.ToSeqFile;
import com.example.mahout.entity.*;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.mahout.common.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class ClassificationService {

    private static CompanyModelDAOMySQL fileModelSQL;

    @Autowired
    private DataService dataService;

    public HashMap<String, Double> testOne(List<Requirement> reqToTrain, List<Requirement> reqToTest, String enterpriseName, int test_num) throws Exception {
        System.out.println("Testing test set number " + test_num);

        String pathToSeq = "./seqFiles/" + enterpriseName + "/test" + test_num;

        List<Requirement> reqToTrainFiltered = dataService.removeHeaders(reqToTrain);
        List<Requirement> reqToTestFiltered = dataService.removeHeaders(reqToTest);

        /* Create sequential file */
        ToSeqFile.ReqToSeq(reqToTrainFiltered,pathToSeq);

        /*Create the process and execute it in order to train mahout and get the results */
        ProcessBuilder pb_upload_files =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -put " +pathToSeq + " /" + enterpriseName);
        ProcessBuilder pb_generate_vectors =  new ProcessBuilder("/bin/bash", "-c", "$MAHOUT_HOME/bin/mahout seq2sparse -i /" +enterpriseName+" -o /"+enterpriseName);
        ProcessBuilder pb_train =  new ProcessBuilder("/bin/bash", "-c", "$MAHOUT_HOME/bin/mahout trainnb -i /"+enterpriseName+"/tfidf-vectors -li /"+enterpriseName+"/labelindex -o /"+enterpriseName+"/model -ow -c");
        ProcessBuilder pb_download_dictionary =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/dictionary.file-0 ./tmpFiles/"+enterpriseName +"/test"+test_num+"/dictionary.file-0");
        ProcessBuilder pb_download_frequencies =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/df-count ./tmpFiles/"+enterpriseName +"/test"+test_num+"/df-count");
        ProcessBuilder pb_upload_test_set =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -put ./tmpFiles/"+enterpriseName+"/test"+test_num + "/testSet /"+enterpriseName+"/testSet");
        ProcessBuilder pb_test =  new ProcessBuilder("/bin/bash", "-c", "$MAHOUT_HOME/bin/mahout testnb -i /"+enterpriseName+"/testSet -l /"+enterpriseName+"/labelindex -m /"+enterpriseName+"/model -ow -o /" + enterpriseName+"/results");
        ProcessBuilder pb_delete_hadoop_files = new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -rm -r /"+enterpriseName);


        /* Set the enviroment configuration */
        BufferedReader environmentFile = new BufferedReader(new FileReader(new File("./config/environment.txt")));
        String line;
        while ((line = environmentFile.readLine()) != null) {
            String env_var[] = line.split(",");
            pb_upload_files.environment().put(env_var[0],env_var[1]);
            pb_generate_vectors.environment().put(env_var[0],env_var[1]);
            pb_train.environment().put(env_var[0],env_var[1]);
            pb_download_dictionary.environment().put(env_var[0],env_var[1]);
            pb_download_frequencies.environment().put(env_var[0],env_var[1]);
            pb_upload_test_set.environment().put(env_var[0],env_var[1]);
            pb_test.environment().put(env_var[0],env_var[1]);
            pb_delete_hadoop_files.environment().put(env_var[0],env_var[1]);

        }

        System.out.println("Process created and configured");

        /* Execute all processes one by one and wwait for them to finish */
        System.out.println("Uploading files");
        Process upload_files = pb_upload_files.start();
        upload_files.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(upload_files.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(upload_files.getErrorStream()))));
        System.out.println("Done");

        System.out.println("Generating vectors");
        Process generate_vectors = pb_generate_vectors.start();
        generate_vectors.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(generate_vectors.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(generate_vectors.getErrorStream()))));
        System.out.println("Done");

        System.out.println("Training");
        Process train = pb_train.start();
        train.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(train.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(train.getErrorStream()))));
        System.out.println("Done");

        System.out.println("Downloading directory file");
        Process download_dictionary = pb_download_dictionary.start();
        download_dictionary.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_dictionary.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_dictionary.getErrorStream()))));
        System.out.println("Done");

        System.out.println("Downloading frequency file");
        Process download_frequencies = pb_download_frequencies.start();
        download_frequencies.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_frequencies.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_frequencies.getErrorStream()))));
        System.out.println("Done");

        System.out.println("Creating test set");
        String tmpPath = "./tmpFiles/" + enterpriseName + "/test" + test_num;
        String freqPath = tmpPath + "/df-count";
        String dictionaryPath = tmpPath + "/dictionary.file-0";
        ReqToTestSet.createTestSet(freqPath, dictionaryPath, reqToTestFiltered, tmpPath +"/");
        System.out.println("Done");

        System.out.println("Uploading testSet");
        Process upload_test_set = pb_upload_test_set.start();
        int exit_upload_test_set = upload_test_set.waitFor();
        System.out.println("Done");

        System.out.println("Testing model");
        Process test = pb_test.start();
        int exit_test = train.waitFor();
        BufferedReader output_error_test = new BufferedReader(new InputStreamReader(test.getErrorStream()));
        System.out.println("Done");

        /* Parse the output of the process to get the disered results */
        System.out.println("Getting stats");
        StringBuilder builder = new StringBuilder();
        StringBuilder positivess_negatives_builder= new StringBuilder();
        String line2 = null;
        while ( (line2 = output_error_test.readLine()) != null) {
            if (line2.contains("Kappa") || line2.contains("Accuracy") || line2.contains("Reliability") ||
                    line2.contains("Weighted"))
                builder.append(line2 + "\n");
            if (line2.contains("Confusion Matrix")) {
                /* Skip the 2 lines we don't want*/
                output_error_test.readLine();
                output_error_test.readLine();
                /* Read the 2 lines containing the numbers */
                line2=output_error_test.readLine();
                positivess_negatives_builder.append(line2+"\n");
                line2=output_error_test.readLine();
                positivess_negatives_builder.append(line2+"\n");
            }
        }
        String statistics = builder.toString();
        String positives_negatives_matrix = positivess_negatives_builder.toString();


        System.out.println("Deleting hadoop files");
        Process delete_hadoop_files = pb_delete_hadoop_files.start();
        int exit_delete_hdfs_files = delete_hadoop_files.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(delete_hadoop_files.getErrorStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(delete_hadoop_files.getInputStream()))));
        System.out.println("Done");

        //if (exit_upload == 0 && exit_generate_vectors == 0 && exit_train == 0 && exit_dw_dictionary == 0 && exit_dw_frequencies == 0 && exit_test == 0 && exit_delete_hdfs_files == 0)
        /*Get th
        e output of the process*/
        BufferedReader output = new BufferedReader(new InputStreamReader(test.getInputStream()));

        return dataService.applyStats(reqToTrain, reqToTrainFiltered, reqToTest, reqToTestFiltered, dataService.getStats(statistics, positives_negatives_matrix));


    }

    public Stats trainAndTest(RequirementList request, int n) throws Exception {
        String enterpriseName = "train_test";
//        JSONArray reqToTest = body.getJSONArray("requirements");

        /* Preprocess data */
        List<Requirement> reqToTest = dataService.preprocess(request.getRequirements());

        /* Create testSets and trainSets*/
        HashMap<String, ArrayList<List<Requirement>>> sets = SamplesCreator.generateTestSets(reqToTest,n);
        ArrayList<List<Requirement>> trainSets = sets.get("train_sets");
        ArrayList<List<Requirement>> testSets = sets.get("test_sets");
        System.out.println("Test sets generated");

        String pathToSeq = "./seqFiles/" + enterpriseName;

        /* Initialize hashMap of results */
        HashMap<String, Double> total_results = new HashMap<>();
        total_results.put("kappa", 0.0);
        total_results.put("accuracy", 0.0);
        total_results.put("reliability", 0.0);
        total_results.put("reliability_std_deviation", 0.0);
        total_results.put("weighted_precision", 0.0);
        total_results.put("weighted_recall", 0.0);
        total_results.put("weighted_f1_score", 0.0);
        total_results.put("true_positives", 0.0);
        total_results.put("false_positives", 0.0);
        total_results.put("false_negatives", 0.0);
        total_results.put("true_negatives", 0.0);


        /* Execute all tests one by one */
        ArrayList<String> values_keys = new ArrayList<>();
        values_keys.addAll(total_results.keySet());
        for (int i = 0; i < trainSets.size(); i++) {
            HashMap<String, Double> tmp_results = testOne(trainSets.get(i), testSets.get(i), enterpriseName, i);
            System.out.println("Done with test set number: "+i);

            /* Add partial results to the total */
            for (int j = 0; j < values_keys.size(); j++) {
                String key = values_keys.get(j);
                double tmp_value = total_results.get(key);
                tmp_value += tmp_results.get(key);
                total_results.put(key, tmp_value);
            }
            System.out.println("Partial results for test number "+i+":" +
                    "Kappa: " + tmp_results.get("kappa") +
                    "\naccuracy: " + tmp_results.get("accuracy") +
                    "\nreliability: " + tmp_results.get("reliability") +
                    "\nreliability_std_deviation: " + tmp_results.get("reliability_std_deviation") +
                    "\nweighted precision: " + tmp_results.get("weighted_precision") +
                    "\nweighted recall: " + tmp_results.get("weighted_recall") +
                    "\nweighted f1 score: " + tmp_results.get("weighted_f1_score"));

            System.out.println("Partial results for " + (i+1) + "tests computed\n");
        }

        /* Compute total results */
        for (int i = 0; i < total_results.size(); i++) {
            String key = values_keys.get(i);
            if(key.equals("true_positives") || key.equals("false_positives") || key.equals("false_negatives") || key.equals("true_negatives")) {
                total_results.put(values_keys.get(i), total_results.get(values_keys.get(i)));
            }
            else {
                double d = total_results.get(values_keys.get(i));
                d = n > 0 ? d / n : 0;
                total_results.put(values_keys.get(i), d);
            }
        }
        System.out.println("Total results calculated");

        /* create a JSONObject with the final results */
//        JSONObject result = new JSONObject();
//        result.put("kappa", total_results.get("kappa"));
//        result.put("accuracy", total_results.get("accuracy"));
//        result.put("reliability", total_results.get("reliability"));
//        result.put("reliability_std_deviation", total_results.get("reliability_std_deviation"));
//        result.put("weighted_precision", total_results.get("weighted_precision"));
//        result.put("weighted_recall", total_results.get("weighted_recall"));
//        result.put("weighted_f1_score", total_results.get("weighted_f1_score"));
//        result.put("true_positives", total_results.get("true_positives"));
//        result.put("false_positives", total_results.get("false_positives"));
//        result.put("false_negatives", total_results.get("false_negatives"));
//        result.put("true_negatives", total_results.get("true_negatives"));
        Stats result = new Stats(total_results.get("kappa"), total_results.get("accuracy"), total_results.get("reliability"),
                total_results.get("reliability_std_deviation"), total_results.get("weighted_precision"), total_results.get("weighted_recall"),
                total_results.get("weighted_f1_score"), total_results.get("true_positives").intValue(), total_results.get("false_positives").intValue(),
                total_results.get("false_negatives").intValue(), total_results.get("true_negatives").intValue());

        Gson gson = new Gson();
        System.out.println("Total results transformed to JSON:\n" + gson.toJson(result));

        /* Delete sequential file */
        File sequential = new File(pathToSeq);
        FileUtils.deleteDirectory(sequential);

        File tmpFiles = new File("./tmpFiles/"+enterpriseName);
        FileUtils.deleteDirectory(tmpFiles);

        System.out.println("Directories deleted, train&test functionality finished.");
        return result;
    }

    public void train(RequirementList request, String property, String enterpriseName) throws Exception {
        /* Parse the body of the request */
//        JSONObject body = new JSONObject(request);
        List<Requirement> reqToTrain = dataService.removeHeaders(dataService.preprocess(request.getRequirements()));

        fileModelSQL = new CompanyModelDAOMySQL();

        String pathToSeq = "./seqFiles/" + enterpriseName;


        /* Create sequential file */
        ToSeqFile.ReqToSeq(reqToTrain,pathToSeq);

        /*Create the process and execute it in order to train mahout and get the results */
        ProcessBuilder pb_upload_files =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -put " +pathToSeq + " /" + enterpriseName);
        ProcessBuilder pb_generate_vectors =  new ProcessBuilder("/bin/bash", "-c", "$MAHOUT_HOME/bin/mahout seq2sparse -i /" +enterpriseName+" -o /"+enterpriseName);
        ProcessBuilder pb_train =  new ProcessBuilder("/bin/bash", "-c", "$MAHOUT_HOME/bin/mahout trainnb -i /"+enterpriseName+"/tfidf-vectors -li /"+enterpriseName+"/labelindex -o /"+enterpriseName+"/model -ow -c");
        ProcessBuilder pb_download_model =  new ProcessBuilder("/bin/bash", "-c", "mkdir -p ./data/"+enterpriseName+" && $HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/model ./data/"+enterpriseName+"/");
        ProcessBuilder pb_download_labelindex =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/labelindex ./data/"+enterpriseName+"/");
        ProcessBuilder pb_download_dictionary =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/dictionary.file-0 ./data/"+enterpriseName+"/dictionary.file-0");
        ProcessBuilder pb_download_frequencies =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/df-count ./data/"+enterpriseName+"/df-count");
        ProcessBuilder pb_delete_hadoop_files = new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -rm -r /"+enterpriseName);


        /* Set the enviroment configuration */
        BufferedReader environmentFile = new BufferedReader(new FileReader(new File("./config/environment.txt")));
        String line;
        while ((line = environmentFile.readLine()) != null) {
            String env_var[] = line.split(",");
            pb_upload_files.environment().put(env_var[0],env_var[1]);
            pb_generate_vectors.environment().put(env_var[0],env_var[1]);
            pb_train.environment().put(env_var[0],env_var[1]);
            pb_download_model.environment().put(env_var[0],env_var[1]);
            pb_download_labelindex.environment().put(env_var[0],env_var[1]);
            pb_download_dictionary.environment().put(env_var[0],env_var[1]);
            pb_download_frequencies.environment().put(env_var[0],env_var[1]);
            pb_delete_hadoop_files.environment().put(env_var[0],env_var[1]);

        }


        /* Execute all processes and wait for them to finish */
        Process upload_files = pb_upload_files.start();
        int exit_upload = upload_files.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(upload_files.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(upload_files.getErrorStream()))));

        Process generate_vectors = pb_generate_vectors.start();
        int exit_generate_vectors = generate_vectors.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(generate_vectors.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(generate_vectors.getErrorStream()))));

        Process train = pb_train.start();
        int exit_train = train.waitFor();
        BufferedReader error = new BufferedReader(new InputStreamReader(train.getErrorStream()));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(train.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(train.getErrorStream()))));

        Process download_model = pb_download_model.start();
        int exit_dw_model = download_model.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_model.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_model.getErrorStream()))));

        Process download_labelindex = pb_download_labelindex.start();
        int exit_dw_labelindex = download_labelindex.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_labelindex.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_labelindex.getErrorStream()))));


        Process download_dictionary = pb_download_dictionary.start();
        int exit_dw_dictionary = download_dictionary.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_dictionary.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_dictionary.getErrorStream()))));


        Process download_frequencies = pb_download_frequencies.start();
        int exit_dw_frequencies = download_frequencies.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_frequencies.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(download_frequencies.getErrorStream()))));

        Process delete_hadoop_files = pb_delete_hadoop_files.start();
        int exit_delete_hdfs_files = delete_hadoop_files.waitFor();
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(delete_hadoop_files.getInputStream()))));
        System.out.println(dataService.getMessage(new BufferedReader(new InputStreamReader(delete_hadoop_files.getErrorStream()))));


        if (exit_train != 0)
            System.out.println(dataService.getMessage(error));
        else if(exit_upload != 0) {
            BufferedReader error2 = new BufferedReader(new InputStreamReader(upload_files.getErrorStream()));
            System.out.println(dataService.getMessage(error2));
        }

        /* Check if all went correctly*/
        if (exit_upload == 0 && exit_generate_vectors == 0 && exit_train == 0 && exit_dw_model == 0
                && exit_dw_labelindex== 0 && exit_dw_dictionary == 0 && exit_dw_frequencies == 0
                && exit_delete_hdfs_files == 0) {
            /*Process the stored result files*/
            /*All the files will be stored in a directory with the name of the file that generated the data!*/
            String dataPath = "./data/" + enterpriseName + "/";
            File model = new File(dataPath + "model/naiveBayesModel.bin");
            File labelindex = new File(dataPath + "labelindex");
            File dictionary = new File(dataPath + "dictionary.file-0");
            File frequencies = new File(dataPath + "df-count");

            CompanyModel fileModel = new CompanyModel(enterpriseName, property, model, labelindex, dictionary, frequencies);

            if (fileModelSQL == null) fileModelSQL = new CompanyModelDAOMySQL();
            fileModelSQL.save(fileModel);

            /*Once we stored the fileModel delete all files */
            model.delete();
            labelindex.delete();
            dictionary.delete();
            frequencies.delete();

            FileUtils.deleteDirectory(new File(dataPath));
        }

        /* Delete sequential file */
        File sequential = new File(pathToSeq);
        FileUtils.deleteDirectory(sequential);

        /*Deleting the directory containing the data:*/
        FileUtils.deleteDirectory(new File("./data"+enterpriseName));
    }

    public RecommendationList classify(RequirementList request, String property, String enterpriseName) throws Exception {
        /* Parse the body of the request */
//        JSONObject body = new JSONObject(request);
//        JSONArray requirements = body.getJSONArray("requirements");
        List<Requirement> requirements = dataService.removeHeaders(dataService.preprocess(request.getRequirements()));

        Classifier classifier = new Classifier();

        System.out.println("Starting classifier");

        /* Classify the requirements with the model of the company */
        ArrayList<Pair<String, Pair<String, Double>>> recomendations = classifier.classify(enterpriseName, requirements, property);
        List<Recommendation> list = new ArrayList<>();

        for(int i = 0; i < recomendations.size(); ++i) {
            Recommendation recomendation = new Recommendation();
            Pair<String,Pair<String, Double>> element = recomendations.get(i);
            recomendation.setRequirement(element.getFirst());
            recomendation.setRequirement_type(element.getSecond().getFirst());
            recomendation.setConfidence(element.getSecond().getSecond());
            list.add(recomendation);
        }
        for (Requirement r : request.getRequirements()) {
            if (r.getRequirement_type()!= null && r.getRequirement_type().equals("Heading")) {
                Recommendation recommendation = new Recommendation();
                recommendation.setRequirement(r.getId());
                recommendation.setRequirement_type("Prose");
                recommendation.setConfidence(100.0);
                list.add(recommendation);
            }
        }
        RecommendationList allRecommendations = new RecommendationList();
        allRecommendations.setRecommendations(list);
        return allRecommendations;
    }

    public String updateMulti(RequirementList request, String property, String enterpriseName, List<String> modelList) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, property);
        for (String domain : domainRequirementsMap.keySet()) {
            if (!domain.trim().isEmpty()) {
                if (modelList.isEmpty()) updateDomainModel(request, domainRequirementsMap.get(domain), enterpriseName, property, domain);
                else if (modelList.contains(domain)) updateDomainModel(request, domainRequirementsMap.get(domain), enterpriseName, property, domain);
            }
        }
        System.out.println("Done");
        return "Update succsesfull";
    }

    private void updateDomainModel(RequirementList request, RequirementList requirementList, String enterpriseName, String property, String domain) throws Exception {
        for (Requirement requirement : request.getRequirements()) {
            if (requirementList.getRequirements().contains(requirement)) {
                requirement.setRequirement_type(property + "#" + domain);
            }
            else requirement.setRequirement_type("Prose");
        }
        System.out.println("Updating " + domain + " model...");
        update(request, property + "#" + domain, enterpriseName);
        System.out.println("Done");
    }

    public String update(RequirementList request, String property, String enterpriseName) throws Exception {
        List<Requirement> reqToTrain = dataService.removeHeaders(dataService.preprocess(request.getRequirements()));

        fileModelSQL = new CompanyModelDAOMySQL();

        /* Check if actual company exists*/
        if (fileModelSQL.exists(enterpriseName, property)) {
            String pathToSeq = "./seqFiles/" + enterpriseName;

            /* Create sequential file */
            ToSeqFile.ReqToSeq(reqToTrain, pathToSeq);

            /*Create the process and execute it in order to train mahout and get the results */
            ProcessBuilder pb_upload_files =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -put " +pathToSeq + " /" + enterpriseName);
            ProcessBuilder pb_generate_vectors =  new ProcessBuilder("/bin/bash", "-c", "$MAHOUT_HOME/bin/mahout seq2sparse -i /" +enterpriseName+" -o /"+enterpriseName);
            ProcessBuilder pb_train =  new ProcessBuilder("/bin/bash", "-c", "$MAHOUT_HOME/bin/mahout trainnb -i /"+enterpriseName+"/tfidf-vectors -li /"+enterpriseName+"/labelindex -o /"+enterpriseName+"/model -ow -c");
            ProcessBuilder pb_download_model =  new ProcessBuilder("/bin/bash", "-c", "mkdir -p ./data/"+enterpriseName+" && $HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/model ./data/"+enterpriseName+"/");
            ProcessBuilder pb_download_labelindex =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/labelindex ./data/"+enterpriseName+"/");
            ProcessBuilder pb_download_dictionary =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/dictionary.file-0 ./data/"+enterpriseName+"/dictionary.file-0");
            ProcessBuilder pb_download_frequencies =  new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/df-count ./data/"+enterpriseName+"/df-count");
            ProcessBuilder pb_delete_hadoop_files = new ProcessBuilder("/bin/bash", "-c", "$HADOOP_HOME/bin/hadoop fs -rm -r /"+enterpriseName);


            /* Set the enviroment configuration */
            BufferedReader environmentFile = new BufferedReader(new FileReader(new File("./config/environment.txt")));
            String line;
            while ((line = environmentFile.readLine()) != null) {
                String env_var[] = line.split(",");
                pb_upload_files.environment().put(env_var[0],env_var[1]);
                pb_generate_vectors.environment().put(env_var[0],env_var[1]);
                pb_train.environment().put(env_var[0],env_var[1]);
                pb_download_model.environment().put(env_var[0],env_var[1]);
                pb_download_labelindex.environment().put(env_var[0],env_var[1]);
                pb_download_dictionary.environment().put(env_var[0],env_var[1]);
                pb_download_frequencies.environment().put(env_var[0],env_var[1]);
                pb_delete_hadoop_files.environment().put(env_var[0],env_var[1]);

            }

            /* Execute all processes one by one*/
            Process upload_files = pb_upload_files.start();
            int exit_upload = upload_files.waitFor();

            Process generate_vectors = pb_generate_vectors.start();
            int exit_generate_vectors = generate_vectors.waitFor();

            Process train = pb_train.start();
            int exit_train = train.waitFor();
            BufferedReader error = new BufferedReader(new InputStreamReader(train.getErrorStream()));

            Process download_model = pb_download_model.start();
            int exit_dw_model = download_model.waitFor();

            Process download_labelindex = pb_download_labelindex.start();
            int exit_dw_labelindex = download_labelindex.waitFor();

            Process download_dictionary = pb_download_dictionary.start();
            int exit_dw_dictionary = download_dictionary.waitFor();

            Process download_frequencies = pb_download_frequencies.start();
            int exit_dw_frequencies = download_frequencies.waitFor();

            Process delete_hadoop_files = pb_delete_hadoop_files.start();
            int exit_delete_hdfs_files = delete_hadoop_files.waitFor();

            if (exit_train != 0)
                System.out.println(dataService.getMessage(error));
            else if(exit_upload != 0) {
                BufferedReader error2 = new BufferedReader(new InputStreamReader(upload_files.getErrorStream()));
                System.out.println(dataService.getMessage(error2));
            }

            /* Check if everything went well */
            if (exit_upload == 0 && exit_generate_vectors == 0 && exit_train == 0 && exit_dw_model == 0
                    && exit_dw_labelindex== 0 && exit_dw_dictionary == 0 && exit_dw_frequencies == 0
                    && exit_delete_hdfs_files == 0) {
                /*Process the stored result files*/
                /*All the files will be stored in a directory with the name of the file that generated the data!*/
                String dataPath = "./data/" + enterpriseName + "/";
                File model = new File(dataPath + "model/naiveBayesModel.bin");
                File labelindex = new File(dataPath + "labelindex");
                File dictionary = new File(dataPath + "dictionary.file-0");
                File frequencies = new File(dataPath + "df-count");

                CompanyModel fileModel = new CompanyModel(enterpriseName, property, model, labelindex, dictionary, frequencies);

                /* Update the model in the database*/
                fileModelSQL.update(fileModel);

                /*Once we stored the fileModel delete all files */
                model.delete();
                labelindex.delete();
                dictionary.delete();
                frequencies.delete();

                FileUtils.deleteDirectory(new File(dataPath));
            }

            /* Delete sequential file */
            File sequential = new File(pathToSeq);
            FileUtils.deleteDirectory(sequential);

            /*Deleting the directory containing the data:*/
            FileUtils.deleteDirectory(new File("./data" + enterpriseName));

            return "Update succsesfull";
        }
        else {
            return "Error, company " + enterpriseName + " doesn't have any classifier for the property " + property + " registered";
        }
    }

    public String delete(CompanyPropertyKey request) throws Exception {
        String enterpriseName = request.getCompany();
        String property = request.getProperty();

        System.out.println("Request parsed, searching for model to delete it.");
        if (fileModelSQL == null) fileModelSQL = new CompanyModelDAOMySQL();
        boolean b;
        if (request.getCompany().equals("ALL"))
            b = fileModelSQL.deleteAll();
        else if (request.getProperty().equals("ALL"))
            b = fileModelSQL.deleteByCompany(enterpriseName);
        else
            b = fileModelSQL.delete(enterpriseName,property);

        if (b) {
            System.out.println("Model(s) deleted");
            return "Files deleted correctly";
        } else {
            System.out.println("Error");
            return "Model(s) not found";
        }
    }

    public String deleteMulti(CompanyPropertyKey request, List<String> modelList) throws SQLException {
        if (fileModelSQL == null) fileModelSQL = new CompanyModelDAOMySQL();
        boolean b = false;
        if (modelList == null || modelList.isEmpty()) {
            b = fileModelSQL.deleteAllMulti(request.getCompany(), request.getProperty());
        }
        else {
            for (String model : modelList) {
                b = fileModelSQL.delete(request.getCompany(), request.getProperty() + "#" + model);
                if (!b) break;
            }
        }
        if (b) {
            System.out.println("Model(s) deleted");
            return "Files deleted correctly";
        } else {
            System.out.println("Error");
            return "Model(s) not found";
        }
    }

    public void trainByDomain(RequirementList request, String enterprise, String propertyKey, List<String> modelList) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, propertyKey);
        for (String domain : domainRequirementsMap.keySet()) {
            if (!domain.trim().isEmpty()) {
                if (modelList == null || modelList.isEmpty()) createDomainModel(request, domainRequirementsMap.get(domain), enterprise, propertyKey, domain);
                else if (modelList.contains(domain)) createDomainModel(request, domainRequirementsMap.get(domain), enterprise, propertyKey, domain);
            }
        }
        System.out.println("Done");
    }

    private void createDomainModel(RequirementList request, RequirementList requirementDomainList, String enterprise, String propertyKey, String domain) throws Exception {
        for (Requirement requirement : request.getRequirements()) {
            if (requirementDomainList.getRequirements().contains(requirement)) {
                requirement.setRequirement_type(propertyKey + "#" + domain);
            }
            else requirement.setRequirement_type("Prose");
        }
        System.out.println("Creating " + domain + " model...");
        train(request, propertyKey + "#" + domain, enterprise);
        System.out.println("Done");
    }

    public RecommendationList classifyByDomain(RequirementList request, String enterpriseName, String property, List<String> modelList) throws Exception {
        RecommendationList globalList = new RecommendationList();

        CompanyModelDAO companyModelDAO = new CompanyModelDAOMySQL();
        List<String> classifyList = new ArrayList<>();

        if (modelList == null || modelList.isEmpty()) {
            List<CompanyModel> companyModels = companyModelDAO.findAllMulti(enterpriseName, property);
            for (CompanyModel cm : companyModels) {
                classifyList.add(cm.getProperty());
            }
        } else {
            for (String model : modelList) {
                classifyList.add(property + "#" + model);
            }
        }

        for (String model : classifyList) {
            RecommendationList recommendationList = classify(request, model, enterpriseName);
            for (Recommendation r : recommendationList.getRecommendations()) {
                if (!r.getRequirement_type().equals("Prose")) {
                    r.setRequirement_type(r.getRequirement_type().split("#")[1]);
                    globalList.getRecommendations().add(r);
                }
            }
        }

        return globalList;
    }

    public DomainStats trainAndTestByDomain(RequirementList request, int n, String propertyKey, List<String> modelList) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, propertyKey);
        DomainStats domainStats = new DomainStats();

        Integer total = 0;
        HashMap<String, Integer> domainSize = new HashMap<>();
        HashMap<String, Stats> statsMap = new HashMap<>();

        for (String domain : domainRequirementsMap.keySet()) {
            if (!domain.trim().isEmpty()) {
                if (modelList == null || modelList.isEmpty()) {
                    total = trainAndTestDomain(request, n, propertyKey, domainStats, total, domainSize, statsMap, domain);
                }
                else if (modelList.contains(domain)) {
                    total = trainAndTestDomain(request, n, propertyKey, domainStats, total, domainSize, statsMap, domain);
                }
            }
        }

        double kappa, accuracy, reliability, reliability_std_deviation, weighted_precision, weighted_recall,
                weighted_f1_score;
        kappa = accuracy = reliability = reliability_std_deviation = weighted_precision = weighted_recall =
                weighted_f1_score = 0.;

        for (String key : statsMap.keySet()) {
            Stats stats = statsMap.get(key);
            double factor = (double) domainSize.get(key) / (double) total;
            kappa += stats.getKappa() * factor;
            accuracy += stats.getAccuracy() * factor;
            reliability += stats.getReliability() * factor;
            reliability_std_deviation += stats.getReliability_std_deviation() * factor;
            weighted_precision += stats.getWeighted_precision() * factor;
            weighted_recall += stats.getWeighted_recall() * factor;
            weighted_f1_score += stats.getWeighted_f1_score() * factor;
        }

        domainStats.setAccuracy(accuracy);
        domainStats.setKappa(kappa);
        domainStats.setReliability(reliability);
        domainStats.setReliability_std_deviation(reliability_std_deviation);
        domainStats.setWeighted_precision(weighted_precision);
        domainStats.setWeighted_recall(weighted_recall);
        domainStats.setWeighted_f1_score(weighted_f1_score);

        return domainStats;
    }

    private Integer trainAndTestDomain(RequirementList request, int n, String propertyKey, DomainStats domainStats, Integer total, HashMap<String, Integer> domainSize, HashMap<String, Stats> statsMap, String domain) throws Exception {
        Integer domainPartialSize = 0;
        for (Requirement r : request.getRequirements()) {
            if (r.getReqDomains(propertyKey).contains(domain)) {
                r.setRequirement_type(domain);
                ++domainPartialSize;
            }
            else if (r.getRequirement_type()==null ||
                    (r.getRequirement_type()!=null &&
                            !r.getRequirement_type().equals("Heading")))
                r.setRequirement_type("Prose");
        }

        Stats s = trainAndTest(request, n);

        ConfusionMatrixStats confusionMatrixStats = new ConfusionMatrixStats();
        confusionMatrixStats.setTrue_positives(s.getTrue_positives());
        confusionMatrixStats.setFalse_positives(s.getFalse_positives());
        confusionMatrixStats.setFalse_negatives(s.getFalse_negatives());
        confusionMatrixStats.setTrue_negatives(s.getTrue_negatives());

        domainStats.getConfusion_matrix().put(domain, confusionMatrixStats);

        total += domainPartialSize;

        domainSize.put(domain, domainPartialSize);
        Stats stats = new Stats(s.getKappa(), s.getAccuracy(), s.getReliability(), s.getReliability_std_deviation(),
                s.getWeighted_precision(), s.getWeighted_recall(), s.getWeighted_f1_score());
        statsMap.put(domain, stats);
        return total;
    }

}
