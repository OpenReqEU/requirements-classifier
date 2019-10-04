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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationService.class);

    public static final String SEQ_FILES = "./seqFiles/";
    public static final String BIN_BASH = "/bin/bash";
    public static final String CONFIG_ENVIRONMENT_TXT = "./config/environment.txt";
    public static final String $_HADOOP_HOME_BIN_HADOOP_FS_GET = "$HADOOP_HOME/bin/hadoop fs -get /";
    public static final String PROSE = "Prose";

    private CompanyModelDAOMySQL fileModelSQL;


    @Autowired
    private DataService dataService;

    public Stats trainAndTest(RequirementList request, String property, int n, Boolean context) throws Exception {
        String enterpriseName = "train_and_test";

        /* Preprocess data */
        List<Requirement> reqToTest = dataService.preprocess(request.getRequirements());

        /* Create testSets and trainSets*/
        HashMap<String, ArrayList<List<Requirement>>> sets = SamplesCreator.generateTestSets(reqToTest,n);
        ArrayList<List<Requirement>> trainSets = sets.get("train_sets");
        ArrayList<List<Requirement>> testSets = sets.get("test_sets");
        logger.info("Test sets generated");

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

        List<Stats> partialStats = new ArrayList<>();

        for (int i = 0 ; i < trainSets.size(); i++) {

            String pathToSeq = ClassificationService.SEQ_FILES + enterpriseName + i;

            RequirementList train = new RequirementList(trainSets.get(i));
            RequirementList test = new RequirementList(testSets.get(i));
            train(train, property, enterpriseName + i);
            RecommendationList recommendations = classify(test, property, enterpriseName + i, context);
            delete(new CompanyPropertyKey(enterpriseName + i,property));
            partialStats.add(new Stats(recommendations, test, property));

            /* Delete sequential file */
            File sequential = new File(pathToSeq);
            FileUtils.deleteDirectory(sequential);

            File tmpFiles = new File("./tmpFiles/"+enterpriseName +i);
            FileUtils.deleteDirectory(tmpFiles);
        }

        logger.info("Total results calculated");

        for (Stats stat : partialStats) {
            total_results.put("kappa", total_results.get("kappa") + stat.getKappa() / n);
            total_results.put("accuracy", total_results.get("accuracy") + stat.getAccuracy() / n);
            total_results.put("reliability", total_results.get("reliability") + stat.getReliability() * 1 / n);
            total_results.put("reliability_std_deviation", total_results.get("reliability_std_deviation") + stat.getReliability_std_deviation()/ n);
            total_results.put("weighted_precision", total_results.get("weighted_precision") + stat.getWeighted_precision()/ n);
            total_results.put("weighted_recall", total_results.get("weighted_recall") + stat.getWeighted_recall() / n);
            total_results.put("weighted_f1_score", total_results.get("weighted_f1_score") + stat.getWeighted_f1_score()/ n);
            total_results.put("true_positives", total_results.get("true_positives") + (double) stat.getTrue_positives()/ n);
            total_results.put("false_positives", total_results.get("false_positives") + (double) stat.getFalse_positives() / n);
            total_results.put("false_negatives", total_results.get("false_negatives") + (double) stat.getFalse_negatives() / n);
            total_results.put("true_negatives", total_results.get("true_negatives") + (double) stat.getTrue_negatives() / n);
        }

        Stats result = new Stats(total_results.get("kappa"), total_results.get("accuracy"), total_results.get("reliability"),
                total_results.get("reliability_std_deviation"), total_results.get("weighted_precision"), total_results.get("weighted_recall"),
                total_results.get("weighted_f1_score"), total_results.get("true_positives").intValue(), total_results.get("false_positives").intValue(),
                total_results.get("false_negatives").intValue(), total_results.get("true_negatives").intValue());

        Gson gson = new Gson();
        logger.info("Total results transformed to JSON:\n" + gson.toJson(result));

        logger.info("Directories deleted, train&test functionality finished.");
        return result;
    }

    public ResultId trainAsync(RequirementList request, String property, String enterpriseName, String url) {
        ResultId id = AsyncService.getId();
        //New thread
        Thread thread = new Thread(() -> {
            Response response = new Response();
            try {
                train(request, property, enterpriseName);
                response.setMessage("Train successful");
                response.setCode(200);
                response.setId(id.getId());
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
                response.setMessage(e.getMessage());
                response.setCode(500);
                response.setId(id.getId());
            }
            finally {
                AsyncService.updateClient(response, url);
            }
        });

        thread.start();
        return id;
    }

    public void train(RequirementList request, String property, String enterpriseName) throws Exception {
        /* Parse the body of the request */
        List<Requirement> reqToTrain = dataService.removeHeaders(dataService.preprocess(request.getRequirements()));

        fileModelSQL = new CompanyModelDAOMySQL();

        String pathToSeq = "./seqFiles/" + enterpriseName;


        /* Create sequential file */
        ToSeqFile.ReqToSeq(reqToTrain,pathToSeq);

        /*Create the process and execute it in order to train mahout and get the results */
        ProcessBuilder pb_upload_files =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -put " +pathToSeq + " /" + enterpriseName);
        ProcessBuilder pb_generate_vectors =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout seq2sparse -i /" +enterpriseName+" -o /"+enterpriseName);
        ProcessBuilder pb_train =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout trainnb -i /"+enterpriseName+"/tfidf-vectors -li /"+enterpriseName+"/labelindex -o /"+enterpriseName+"/model -ow -c");
        ProcessBuilder pb_download_model =  new ProcessBuilder(BIN_BASH, "-c", "mkdir -p ./data/"+enterpriseName+" && $HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/model ./data/"+enterpriseName+"/");
        ProcessBuilder pb_download_labelindex =  new ProcessBuilder(BIN_BASH, "-c", $_HADOOP_HOME_BIN_HADOOP_FS_GET +enterpriseName+"/labelindex ./data/"+enterpriseName+"/");
        ProcessBuilder pb_download_dictionary =  new ProcessBuilder(BIN_BASH, "-c", $_HADOOP_HOME_BIN_HADOOP_FS_GET+enterpriseName+"/dictionary.file-0 ./data/"+enterpriseName+"/dictionary.file-0");
        ProcessBuilder pb_download_frequencies =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/df-count ./data/"+enterpriseName+"/df-count");
        ProcessBuilder pb_delete_hadoop_files = new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -rm -r /"+enterpriseName);
        logger.info("Logging for " + enterpriseName);

        /* Set the enviroment configuration */
        try(BufferedReader environmentFile = new BufferedReader(new FileReader(new File(CONFIG_ENVIRONMENT_TXT)))) {
            String line;
            while ((line = environmentFile.readLine()) != null) {
                String env_var[] = line.split(",");
                pb_upload_files.environment().put(env_var[0], env_var[1]);
                pb_generate_vectors.environment().put(env_var[0], env_var[1]);
                pb_train.environment().put(env_var[0], env_var[1]);
                pb_download_model.environment().put(env_var[0], env_var[1]);
                pb_download_labelindex.environment().put(env_var[0], env_var[1]);
                pb_download_dictionary.environment().put(env_var[0], env_var[1]);
                pb_download_frequencies.environment().put(env_var[0], env_var[1]);
                pb_delete_hadoop_files.environment().put(env_var[0], env_var[1]);

            }

            logger.info("Run Mahout for " + enterpriseName);

            /* Execute all processes and wait for them to finish */
            Process upload_files = pb_upload_files.start();
            int exit_upload = upload_files.waitFor();
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(upload_files.getInputStream()))));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(upload_files.getErrorStream()))));

            Process generate_vectors = pb_generate_vectors.start();
            int exit_generate_vectors = generate_vectors.waitFor();
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(generate_vectors.getInputStream()))));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(generate_vectors.getErrorStream()))));

            Process train = pb_train.start();
            int exit_train = train.waitFor();
            BufferedReader error = new BufferedReader(new InputStreamReader(train.getErrorStream()));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(train.getInputStream()))));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(train.getErrorStream()))));

            Process download_model = pb_download_model.start();
            int exit_dw_model = download_model.waitFor();
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(download_model.getInputStream()))));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(download_model.getErrorStream()))));

            Process download_labelindex = pb_download_labelindex.start();
            int exit_dw_labelindex = download_labelindex.waitFor();
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(download_labelindex.getInputStream()))));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(download_labelindex.getErrorStream()))));


            Process download_dictionary = pb_download_dictionary.start();
            int exit_dw_dictionary = download_dictionary.waitFor();
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(download_dictionary.getInputStream()))));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(download_dictionary.getErrorStream()))));


            Process download_frequencies = pb_download_frequencies.start();
            int exit_dw_frequencies = download_frequencies.waitFor();
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(download_frequencies.getInputStream()))));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(download_frequencies.getErrorStream()))));

            Process delete_hadoop_files = pb_delete_hadoop_files.start();
            int exit_delete_hdfs_files = delete_hadoop_files.waitFor();
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(delete_hadoop_files.getInputStream()))));
            logger.info(dataService.getMessage(new BufferedReader(new InputStreamReader(delete_hadoop_files.getErrorStream()))));


            exitProcess(upload_files, exit_upload, exit_train, error);

            /* Check if all went correctly*/
            if (exit_upload == 0 && exit_generate_vectors == 0 && exit_train == 0 && exit_dw_model == 0
                    && exit_dw_labelindex == 0 && exit_dw_dictionary == 0 && exit_dw_frequencies == 0
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
                java.nio.file.Files.delete(model.toPath());
                java.nio.file.Files.delete(labelindex.toPath());
                java.nio.file.Files.delete(dictionary.toPath());
                java.nio.file.Files.delete(frequencies.toPath());

                FileUtils.deleteDirectory(new File(dataPath));
            }

            /* Delete sequential file */
            File sequential = new File(pathToSeq);
            FileUtils.deleteDirectory(sequential);

            /*Deleting the directory containing the data:*/
            FileUtils.deleteDirectory(new File("./data" + enterpriseName));
        } catch(Exception e) {
            logger.error("Error in " + enterpriseName);
            logger.error(e.getLocalizedMessage());
        }
    }

    public RecommendationList classify(RequirementList request, String property, String enterpriseName, Boolean context) throws Exception {

        Document document = new Document(request.getRequirements());
        List<Requirement> requirements = dataService.removeHeaders(dataService.preprocess(request.getRequirements()));

        Classifier classifier = new Classifier();

        logger.info("Starting classifier");

        ResultId resultId = AsyncService.getId();

        /* Classify the requirements with the model of the company */
        ArrayList<Pair<String, Pair<String, Double>>> recomendations = classifier.classify(enterpriseName, requirements, property,
                resultId);
        HashMap<String, Recommendation> recommendationMap = new HashMap<>();

        for(int i = 0; i < recomendations.size(); ++i) {
            Recommendation recomendation = new Recommendation();
            Pair<String,Pair<String, Double>> element = recomendations.get(i);
            recomendation.setRequirement(element.getFirst());
            recomendation.setRequirement_type(element.getSecond().getFirst());
            recomendation.setConfidence(element.getSecond().getSecond());
            if (!recommendationMap.containsKey(element.getFirst()))
                recommendationMap.put(element.getFirst(), recomendation);
        }

        if (context) {
            for (Recommendation r : recommendationMap.values()) {
                List<Requirement> children = document.getChildren(r.getRequirement());
                if (children != null && !children.isEmpty())
                    markListWithMostCommonTag(children, recommendationMap, property);
            }
        }

        RecommendationList allRecommendations = new RecommendationList();
        allRecommendations.setRecommendations(new ArrayList(recommendationMap.values()));
        return allRecommendations;
    }

    private void markListWithMostCommonTag(List<Requirement> children, HashMap<String, Recommendation> recommendationMap,
                                           String property) {

        if (!areBulletList(children)) return;

        Integer positiveTag = 0;
        Integer negativeTag = 0;
        for (Requirement r : children) {
            if (recommendationMap.get(r.getId()).getRequirement_type().equals(property)) ++positiveTag;
            else ++negativeTag;
        }

        String tag;
        if (positiveTag > negativeTag) tag = property;
        else tag = PROSE;

        for (Requirement r : children) {
            recommendationMap.get(r.getId()).setRequirement_type(tag);
        }

    }

    private boolean areBulletList(List<Requirement> children) {
        boolean isList = true;
        int i = 0;
        while (isList && i < children.size()) {
            isList = isBulletItem(children.get(i).getText());
            ++i;
        }
        return isList;
    }

    private static final String GRAMMAR_EXP = "^([a-z|A-Z]+|^[MDCLXVI]+$|[0-9]+)?[)|.|\\-|·]";

    private static boolean isBulletItem(String text) {
        Pattern pattern = Pattern.compile(GRAMMAR_EXP);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    public void updateMulti(RequirementList request, String property, String enterpriseName, List<String> modelList) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, property);

        for (Map.Entry<String, RequirementList> domainRequirements : domainRequirementsMap.entrySet()) {
            String domain = domainRequirements.getKey();
            if (!domain.trim().isEmpty()) {
                updateDomainModel(request, domainRequirements.getValue(), enterpriseName, property, domain);
            }
        }

        logger.info("Done");
    }

    private void updateDomainModel(RequirementList request, RequirementList requirementList, String enterpriseName, String property, String domain) throws Exception {
        for (Requirement requirement : request.getRequirements()) {
            if (requirementList.getRequirements().contains(requirement)) {
                requirement.setRequirement_type(property + "#" + domain);
            }
            else requirement.setRequirement_type("Prose");
        }
        logger.info("Updating " + domain + " model...");
        update(request, property + "#" + domain, enterpriseName);
        logger.info("Done");
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
            ProcessBuilder pb_upload_files =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -put " +pathToSeq + " /" + enterpriseName);
            ProcessBuilder pb_generate_vectors =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout seq2sparse -i /" +enterpriseName+" -o /"+enterpriseName);
            ProcessBuilder pb_train =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout trainnb -i /"+enterpriseName+"/tfidf-vectors -li /"+enterpriseName+"/labelindex -o /"+enterpriseName+"/model -ow -c");
            ProcessBuilder pb_download_model =  new ProcessBuilder(BIN_BASH, "-c", "mkdir -p ./data/"+enterpriseName+" && $HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/model ./data/"+enterpriseName+"/");
            ProcessBuilder pb_download_labelindex =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/labelindex ./data/"+enterpriseName+"/");
            ProcessBuilder pb_download_dictionary =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/dictionary.file-0 ./data/"+enterpriseName+"/dictionary.file-0");
            ProcessBuilder pb_download_frequencies =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/df-count ./data/"+enterpriseName+"/df-count");
            ProcessBuilder pb_delete_hadoop_files = new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -rm -r /"+enterpriseName);


            /* Set the enviroment configuration */
            try (BufferedReader environmentFile = new BufferedReader(new FileReader(new File("./config/environment.txt")))) {
                String line;
                while ((line = environmentFile.readLine()) != null) {
                    String env_var[] = line.split(",");
                    pb_upload_files.environment().put(env_var[0], env_var[1]);
                    pb_generate_vectors.environment().put(env_var[0], env_var[1]);
                    pb_train.environment().put(env_var[0], env_var[1]);
                    pb_download_model.environment().put(env_var[0], env_var[1]);
                    pb_download_labelindex.environment().put(env_var[0], env_var[1]);
                    pb_download_dictionary.environment().put(env_var[0], env_var[1]);
                    pb_download_frequencies.environment().put(env_var[0], env_var[1]);
                    pb_delete_hadoop_files.environment().put(env_var[0], env_var[1]);

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

                exitProcess(upload_files, exit_upload, exit_train, error);

                /* Check if everything went well */
                if (exit_upload == 0 && exit_generate_vectors == 0 && exit_train == 0 && exit_dw_model == 0
                        && exit_dw_labelindex == 0 && exit_dw_dictionary == 0 && exit_dw_frequencies == 0
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
                    Files.delete(model.toPath());
                    Files.delete(labelindex.toPath());
                    Files.delete(dictionary.toPath());
                    Files.delete(frequencies.toPath());

                    FileUtils.deleteDirectory(new File(dataPath));
                }

                /* Delete sequential file */
                File sequential = new File(pathToSeq);
                FileUtils.deleteDirectory(sequential);

                /*Deleting the directory containing the data:*/
                FileUtils.deleteDirectory(new File("./data" + enterpriseName));

                return "Update successful";
            }
        }
        else {
            return "Error, company " + enterpriseName + " doesn't have any classifier for the property " + property + " registered";
        }
    }

    private void exitProcess(Process upload_files, int exit_upload, int exit_train, BufferedReader error) throws IOException {
        if (exit_train != 0)
            logger.info(dataService.getMessage(error));
        else if(exit_upload != 0) {
            BufferedReader error2 = new BufferedReader(new InputStreamReader(upload_files.getErrorStream()));
            logger.info(dataService.getMessage(error2));
        }
    }

    public String delete(CompanyPropertyKey request) throws Exception {
        String enterpriseName = request.getCompany();
        String property = request.getProperty();

        logger.info("Request parsed, searching for model to delete it.");
        if (fileModelSQL == null) fileModelSQL = new CompanyModelDAOMySQL();
        boolean b;
        if (request.getCompany().equals("ALL"))
            b = fileModelSQL.deleteAll();
        else if (request.getProperty().equals("ALL"))
            b = fileModelSQL.deleteByCompany(enterpriseName);
        else
            b = fileModelSQL.delete(enterpriseName,property);

        return getResult(b);
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
        return getResult(b);
    }

    private String getResult(boolean b) {
        if (b) {
            logger.info("Model(s) deleted");
            return "Files deleted correctly";
        } else {
            logger.info("Error");
            return "Model(s) not found";
        }
    }

    public ResultId trainByDomainAsync(RequirementList requirementList, String enterpriseName, String property,
                                       List<String> modelList, String url) {
        ResultId id = AsyncService.getId();
        //New thread
        Thread thread = new Thread(() -> {
            Response response = new Response();
            try {
                trainByDomain(requirementList, enterpriseName, property, modelList);
                response.setMessage("Train successful");
                response.setCode(200);
                response.setId(id.getId());
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
                response.setMessage(e.getMessage());
                response.setCode(500);
                response.setId(id.getId());
            }
            finally {
                AsyncService.updateClient(response, url);
            }
        });

        thread.start();
        return id;
    }

    public void trainByDomain(RequirementList request, String enterprise, String propertyKey, List<String> modelList) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, propertyKey);
        for (String domain : domainRequirementsMap.keySet()) {
            if (domainRequirementsMap.get(domain).getRequirements().size() == 0) {
                logger.info("Model not created for property " + domain + ": not enough data");
            }
            else if (!domain.trim().isEmpty()) {
                if (modelList == null || modelList.isEmpty()) createDomainModel(request, domainRequirementsMap.get(domain), enterprise, propertyKey, domain);
                else if (modelList.contains(domain)) createDomainModel(request, domainRequirementsMap.get(domain), enterprise, propertyKey, domain);
            }
        }
        logger.info("Done");
    }

    private void createDomainModel(RequirementList request, RequirementList requirementDomainList, String enterprise, String propertyKey, String domain) throws Exception {
        for (Requirement requirement : request.getRequirements()) {
            if (requirementDomainList.getRequirements().contains(requirement)) {
                requirement.setRequirement_type(propertyKey + "#" + domain);
            }
            else requirement.setRequirement_type("Prose");
        }
        logger.info("Creating " + domain + " model...");
        train(request, propertyKey + "#" + domain, enterprise);
        logger.info("Done");
    }

    public RecommendationList classifyByDomain(RequirementList request, String enterpriseName, String property, List<String> modelList, Boolean context) throws Exception {
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
            RecommendationList recommendationList = classify(request, model, enterpriseName, context);
            for (Recommendation r : recommendationList.getRecommendations()) {
                if (!r.getRequirement_type().equals("Prose")) {
                    r.setRequirement_type(r.getRequirement_type().split("#")[1]);
                    globalList.getRecommendations().add(r);
                }
            }
        }

        return globalList;
    }

    public DomainStats trainAndTestByDomain(RequirementList request, int n, String propertyKey, List<String> modelList, Boolean context) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, propertyKey);
        DomainStats domainStats = new DomainStats();

        Integer total = 0;
        HashMap<String, Integer> domainSize = new HashMap<>();
        HashMap<String, Stats> statsMap = new HashMap<>();

        for (String domain : domainRequirementsMap.keySet()) {
            if (!domain.trim().isEmpty()) {
                total = trainAndTestDomain(request, n, propertyKey, domainStats, total, domainSize, statsMap, domain, context);
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

    private Integer trainAndTestDomain(RequirementList request, int n, String propertyKey, DomainStats domainStats, Integer total,
                                       HashMap<String, Integer> domainSize, HashMap<String, Stats> statsMap, String domain,
                                       Boolean context) throws Exception {
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

        Stats s = trainAndTest(request, propertyKey, n, context);

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

    public List<String> getMultilabelValues(String enterpriseName, String property) {
        try {
            if (fileModelSQL == null)
                fileModelSQL = new CompanyModelDAOMySQL();
            List<String> models =  fileModelSQL.findAllMulti(enterpriseName, property).stream().map(CompanyModel::getProperty)
                    .collect(Collectors.toList());
            for (int n = 0; n < models.size(); n++) {
                models.set(n, models.get(n).split("#")[1]);
            }
            return models;
        } catch (SQLException | IOException e) {
            logger.error(e.getLocalizedMessage());
        }
        return null;
    }
}
