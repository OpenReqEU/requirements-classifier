package com.example.mahout.service;

import com.example.mahout.Classifier;
import com.example.mahout.dao.CompanyModelDAO;
import com.example.mahout.dao.CompanyModelDAOMySQL;
import com.example.mahout.ReqToTestSet;
import com.example.mahout.SamplesCreator;
import com.example.mahout.ToSeqFile;
import com.example.mahout.entity.*;
import com.example.mahout.util.Control;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.mahout.common.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static final String SEQ_FILES = "./seqFiles/";
    public static final String TEST = "/test";
    public static final String BIN_BASH = "/bin/bash";
    public static final String CONFIG_ENVIRONMENT_TXT = "./config/environment.txt";
    private Control control = Control.getInstance();

    private static CompanyModelDAOMySQL fileModelSQL;


    @Autowired
    private DataService dataService;

    public Map<String, Double> testOne(List<Requirement> reqToTrain, List<Requirement> reqToTest, String enterpriseName, int test_num) throws Exception {
        control.showInfoMessage("Testing test set number " + test_num);

        String pathToSeq = SEQ_FILES + enterpriseName + TEST + test_num;

        List<Requirement> reqToTrainFiltered = dataService.removeHeaders(reqToTrain);
        List<Requirement> reqToTestFiltered = dataService.removeHeaders(reqToTest);

        /* Create sequential file */
        ToSeqFile.reqToSeq(reqToTrainFiltered,pathToSeq);

        /*Create the process and execute it in order to train mahout and get the results */
        ProcessBuilder pbUploadFiles =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -put " +pathToSeq + " /" + enterpriseName);
        ProcessBuilder pbGenerateVectors =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout seq2sparse -i /" +enterpriseName+" -o /"+enterpriseName);
        ProcessBuilder pbTrain =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout trainnb -i /"+enterpriseName+"/tfidf-vectors -li /"+enterpriseName+"/labelindex -o /"+enterpriseName+"/model -ow -c");
        ProcessBuilder pbDownloadDictionary =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/dictionary.file-0 ./tmpFiles/"+enterpriseName +"/test"+test_num+"/dictionary.file-0");
        ProcessBuilder pbDownloadFrequencies =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/df-count ./tmpFiles/"+enterpriseName +"/test"+test_num+"/df-count");
        ProcessBuilder pbUploadTestSet =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -put ./tmpFiles/"+enterpriseName+"/test"+test_num + "/testSet /"+enterpriseName+"/testSet");
        ProcessBuilder pbTest =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout testnb -i /"+enterpriseName+"/testSet -l /"+enterpriseName+"/labelindex -m /"+enterpriseName+"/model -ow -o /" + enterpriseName+"/results");
        ProcessBuilder pbDeleteHadoopFiles = new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -rm -r /"+enterpriseName);


        /* Set the enviroment configuration */
        try (BufferedReader environmentFile = new BufferedReader(new FileReader(new File(CONFIG_ENVIRONMENT_TXT)))) {
            String line;
            while ((line = environmentFile.readLine()) != null) {
                String[] env_var = line.split(",");
                pbUploadFiles.environment().put(env_var[0], env_var[1]);
                pbGenerateVectors.environment().put(env_var[0], env_var[1]);
                pbTrain.environment().put(env_var[0], env_var[1]);
                pbDownloadDictionary.environment().put(env_var[0], env_var[1]);
                pbDownloadFrequencies.environment().put(env_var[0], env_var[1]);
                pbUploadTestSet.environment().put(env_var[0], env_var[1]);
                pbTest.environment().put(env_var[0], env_var[1]);
                pbDeleteHadoopFiles.environment().put(env_var[0], env_var[1]);
            }
        }

        control.showInfoMessage("Process created and configured");

        /* Execute all processes one by one and wwait for them to finish */
        control.showInfoMessage("Uploading files");
        Process uploadFiles = pbUploadFiles.start();
        uploadFiles.waitFor();
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(uploadFiles.getInputStream()))));
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(uploadFiles.getErrorStream()))));
        control.showInfoMessage("Done");

        control.showInfoMessage("Generating vectors");
        Process generateVectors = pbGenerateVectors.start();
        generateVectors.waitFor();
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(generateVectors.getInputStream()))));
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(generateVectors.getErrorStream()))));
        control.showInfoMessage("Done");

        control.showInfoMessage("Training");
        Process train = pbTrain.start();
        train.waitFor();
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(train.getInputStream()))));
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(train.getErrorStream()))));
        control.showInfoMessage("Done");

        control.showInfoMessage("Downloading directory file");
        Process downloadDictionary = pbDownloadDictionary.start();
        downloadDictionary.waitFor();
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(downloadDictionary.getInputStream()))));
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(downloadDictionary.getErrorStream()))));
        control.showInfoMessage("Done");

        control.showInfoMessage("Downloading frequency file");
        Process downloadFrequencies = pbDownloadFrequencies.start();
        downloadFrequencies.waitFor();
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(downloadFrequencies.getInputStream()))));
        control.showInfoMessage(dataService.getMessage(new BufferedReader(new InputStreamReader(downloadFrequencies.getErrorStream()))));
        control.showInfoMessage("Done");

        control.showInfoMessage("Creating test set");
        String tmpPath = "./tmpFiles/" + enterpriseName + "/test" + test_num;
        String freqPath = tmpPath + "/df-count";
        String dictionaryPath = tmpPath + "/dictionary.file-0";
        ReqToTestSet.createTestSet(freqPath, dictionaryPath, reqToTestFiltered, tmpPath +"/");
        control.showInfoMessage("Done");

        control.showInfoMessage("Uploading testSet");
        Process uploadTestSet = pbUploadTestSet.start();
        uploadTestSet.waitFor();
        control.showInfoMessage("Done");

        control.showInfoMessage("Testing model");
        Process test = pbTest.start();
        train.waitFor();
        BufferedReader outputErrorTest = new BufferedReader(new InputStreamReader(test.getErrorStream()));
        control.showInfoMessage("Done");

        /* Parse the output of the process to get the disered results */
        control.showInfoMessage("Getting stats");
        StringBuilder builder = new StringBuilder();
        StringBuilder positivesNegativesBuilder= new StringBuilder();
        String line2;
        while ( (line2 = outputErrorTest.readLine()) != null) {
            if (line2.contains("Kappa") || line2.contains("Accuracy") || line2.contains("Reliability") ||
                    line2.contains("Weighted"))
                builder.append(line2 + "\n");
            if (line2.contains("Confusion Matrix")) {
                /* Skip the 2 lines we don't want*/
                outputErrorTest.readLine(); //NOSONAR
                outputErrorTest.readLine(); //NOSONAR
                /* Read the 2 lines containing the numbers */
                line2=outputErrorTest.readLine();
                positivesNegativesBuilder.append(line2+"\n");
                line2=outputErrorTest.readLine();
                positivesNegativesBuilder.append(line2+"\n");
            }
        }
        String statistics = builder.toString();
        String positivesNegativesMatrix = positivesNegativesBuilder.toString();


        control.showInfoMessage("Deleting hadoop files");
        Process deleteHadoopFiles = pbDeleteHadoopFiles.start();
        deleteHadoopFiles.waitFor();
        control.showInfoMessage("Done");

        return dataService.applyStats(reqToTest, reqToTestFiltered, dataService.getStats(statistics, positivesNegativesMatrix));


    }

    public Stats trainAndTest(RequirementList request, String property, int n, Boolean context) throws Exception {
        String enterpriseName = "train_test";

        /* Preprocess data */
        List<Requirement> reqToTest = dataService.preprocess(request.getRequirements());

        /* Create testSets and trainSets*/
        Map<String, List<List<Requirement>>> sets = SamplesCreator.generateTestSets(reqToTest,n);
        List<List<Requirement>> trainSets = sets.get("train_sets");
        List<List<Requirement>> testSets = sets.get("test_sets");
        control.showInfoMessage("Test sets generated");

        String pathToSeq = "./seqFiles/" + enterpriseName;

        /* Initialize hashMap of results */
        HashMap<String, Double> totalResults = new HashMap<>();
        totalResults.put("kappa", 0.0);
        totalResults.put("accuracy", 0.0);
        totalResults.put("reliability", 0.0);
        totalResults.put("reliability_std_deviation", 0.0);
        totalResults.put("weighted_precision", 0.0);
        totalResults.put("weighted_recall", 0.0);
        totalResults.put("weighted_f1_score", 0.0);
        totalResults.put("true_positives", 0.0);
        totalResults.put("false_positives", 0.0);
        totalResults.put("false_negatives", 0.0);
        totalResults.put("true_negatives", 0.0);

        List<Stats> partialStats = new ArrayList<>();

        for (int i = 0 ; i < trainSets.size(); i++) {
            RequirementList train = new RequirementList(trainSets.get(i));
            RequirementList test = new RequirementList(testSets.get(i));
            train(train, property, "train_and_test");
            RecommendationList recommendations = classify(test, property, "train_and_test", context);
            partialStats.add(new Stats(recommendations, test, property));
        }

        control.showInfoMessage("Total results calculated");

        Stats result = new Stats(totalResults.get("kappa"), totalResults.get("accuracy"), totalResults.get("reliability"),
                totalResults.get("reliability_std_deviation"), totalResults.get("weighted_precision"), totalResults.get("weighted_recall"),
                totalResults.get("weighted_f1_score"), totalResults.get("true_positives").intValue(), totalResults.get("false_positives").intValue(),
                totalResults.get("false_negatives").intValue(), totalResults.get("true_negatives").intValue());

        Gson gson = new Gson();
        control.showInfoMessage("Total results transformed to JSON:\n" + gson.toJson(result));

        /* Delete sequential file */
        File sequential = new File(pathToSeq);
        FileUtils.deleteDirectory(sequential);

        File tmpFiles = new File("./tmpFiles/"+enterpriseName);
        FileUtils.deleteDirectory(tmpFiles);

        control.showInfoMessage("Directories deleted, train&test functionality finished.");
        return result;
    }

    public ResultId trainAsync(RequirementList request, String property, String enterpriseName, String url) {
        ResultId id = AsyncService.getId();
        Thread thread = new ThreadAsync(request,property,enterpriseName,url,id,null,false);
        thread.start();
        return id;
    }

    public void train(RequirementList request, String property, String enterpriseName) throws Exception {
        /* Parse the body of the request */
        List<Requirement> reqToTrain = dataService.removeHeaders(dataService.preprocess(request.getRequirements()));
        fileModelSQL = new CompanyModelDAOMySQL(); //TODO FIX THIS
        String pathToSeq = "./seqFiles/" + enterpriseName;
        trainModel(reqToTrain,pathToSeq,property,enterpriseName,false);
    }

    public RecommendationList classify(RequirementList request, String property, String enterpriseName, Boolean context) throws Exception {

        Document document = new Document(request.getRequirements());
        List<Requirement> requirements = dataService.removeHeaders(dataService.preprocess(request.getRequirements()));

        Classifier classifier = new Classifier();

        control.showInfoMessage("Starting classifier");

        ResultId resultId = AsyncService.getId();

        /* Classify the requirements with the model of the company */
        List<Pair<String, Pair<String, Double>>> recomendations = classifier.classify(enterpriseName, requirements, property,
                resultId);
        HashMap<String, Recommendation> recommendationMap = new HashMap<>();

        for(int i = 0; i < recomendations.size(); ++i) {
            Recommendation recomendation = new Recommendation();
            Pair<String,Pair<String, Double>> element = recomendations.get(i);
            recomendation.setRequirement(element.getFirst());
            recomendation.setRequirementType(element.getSecond().getFirst());
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
            if (recommendationMap.get(r.getId()).getRequirementType().equals(property)) ++positiveTag;
            else ++negativeTag;
        }

        String tag;
        if (positiveTag > negativeTag) tag = property;
        else tag = "Prose";

        for (Requirement r : children) {
            recommendationMap.get(r.getId()).setRequirementType(tag);
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
        Map<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, property);
        for (Map.Entry<String,RequirementList> entry : domainRequirementsMap.entrySet()) {
            String domain = entry.getKey();
            RequirementList requirementList = entry.getValue();
            if (!domain.trim().isEmpty()) {
                if (modelList.isEmpty()) updateDomainModel(request, requirementList, enterpriseName, property, domain);
                else if (modelList.contains(domain)) updateDomainModel(request, requirementList, enterpriseName, property, domain);
            }
        }
        control.showInfoMessage("Done");
    }

    private void updateDomainModel(RequirementList request, RequirementList requirementList, String enterpriseName, String property, String domain) throws Exception {
        for (Requirement requirement : request.getRequirements()) {
            if (requirementList.getRequirements().contains(requirement)) {
                requirement.setRequirementType(property + "#" + domain);
            }
            else requirement.setRequirementType("Prose");
        }
        control.showInfoMessage("Updating " + domain + " model...");
        update(request, property + "#" + domain, enterpriseName);
        control.showInfoMessage("Done");
    }

    public String update(RequirementList request, String property, String enterpriseName) throws Exception {
        List<Requirement> reqToTrain = dataService.removeHeaders(dataService.preprocess(request.getRequirements()));

        fileModelSQL = new CompanyModelDAOMySQL(); //TODO FIX THIS

        /* Check if actual company exists*/
        if (fileModelSQL.exists(enterpriseName, property)) {
            String pathToSeq = "./seqFiles/" + enterpriseName;
            trainModel(reqToTrain,pathToSeq,property,enterpriseName,true);
            return "Update successful";
        }
        else {
            return "Error, company " + enterpriseName + " doesn't have any classifier for the property " + property + " registered";
        }
    }

    private void exitProcess(Process uploadFiles, int exitUpload, int exitTrain, BufferedReader error) throws IOException {
        if (exitTrain != 0)
            control.showInfoMessage(dataService.getMessage(error));
        else if(exitUpload != 0) {
            BufferedReader error2 = new BufferedReader(new InputStreamReader(uploadFiles.getErrorStream()));
            control.showInfoMessage(dataService.getMessage(error2));
        }
    }

    public String delete(CompanyPropertyKey request) throws SQLException {
        String enterpriseName = request.getCompany();
        String property = request.getProperty();

        control.showInfoMessage("Request parsed, searching for model to delete it.");
        if (fileModelSQL == null) fileModelSQL = new CompanyModelDAOMySQL(); //TODO FIX THIS
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
        if (fileModelSQL == null) fileModelSQL = new CompanyModelDAOMySQL(); //TODO FIX THIS
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
            control.showInfoMessage("Model(s) deleted");
            return "Files deleted correctly";
        } else {
            control.showInfoMessage("Error");
            return "Model(s) not found";
        }
    }

    public ResultId trainByDomainAsync(RequirementList requirementList, String enterpriseName, String property,
                                       List<String> modelList, String url) {
        ResultId id = AsyncService.getId();
        Thread thread = new ThreadAsync(requirementList,property,enterpriseName,url,id,modelList,true);
        thread.start();
        return id;
    }

    public void trainByDomain(RequirementList request, String enterprise, String propertyKey, List<String> modelList) throws Exception {
        Map<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, propertyKey);
        for (Map.Entry<String,RequirementList> entry : domainRequirementsMap.entrySet()) {
            String domain = entry.getKey();
            RequirementList requirementList = entry.getValue();
            if (requirementList.getRequirements().isEmpty()) {
                control.showInfoMessage("Model not created for property " + domain + ": not enough data");
            }
            else if (!domain.trim().isEmpty()) {
                if (modelList == null || modelList.isEmpty()) createDomainModel(request, requirementList, enterprise, propertyKey, domain);
                else if (modelList.contains(domain)) createDomainModel(request, requirementList, enterprise, propertyKey, domain);
            }
        }
        control.showInfoMessage("Done");
    }

    private void createDomainModel(RequirementList request, RequirementList requirementDomainList, String enterprise, String propertyKey, String domain) throws Exception {
        for (Requirement requirement : request.getRequirements()) {
            if (requirementDomainList.getRequirements().contains(requirement)) {
                requirement.setRequirementType(propertyKey + "#" + domain);
            }
            else requirement.setRequirementType("Prose");
        }
        control.showInfoMessage("Creating " + domain + " model...");
        train(request, propertyKey + "#" + domain, enterprise);
        control.showInfoMessage("Done");
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
                if (!r.getRequirementType().equals("Prose")) {
                    r.setRequirementType(r.getRequirementType().split("#")[1]);
                    globalList.getRecommendations().add(r);
                }
            }
        }

        return globalList;
    }

    public DomainStats trainAndTestByDomain(RequirementList request, int n, String propertyKey, List<String> modelList, Boolean context) throws Exception {
        Map<String, RequirementList> domainRequirementsMap = dataService.mapByDomain(request, propertyKey);
        DomainStats domainStats = new DomainStats();

        Integer total = 0;
        HashMap<String, Integer> domainSize = new HashMap<>();
        HashMap<String, Stats> statsMap = new HashMap<>();

        for (String domain : domainRequirementsMap.keySet()) {
            if (!domain.trim().isEmpty() && (modelList == null || modelList.isEmpty() || modelList.contains(domain))) {
                total = trainAndTestDomain(request, n, propertyKey, domainStats, total, domainSize, statsMap, domain, context);
            }
        }

        double kappa;
        double accuracy;
        double reliability;
        double reliabilityStdDeviation;
        double weightedPrecision;
        double weightedRecall;
        double weightedF1Score;
        kappa = accuracy = reliability = reliabilityStdDeviation = weightedPrecision = weightedRecall =
                weightedF1Score = 0.;

        for (Map.Entry<String, Stats> entry : statsMap.entrySet()) {
            String key = entry.getKey();
            Stats stats = entry.getValue();
            double factor = (double) domainSize.get(key) / (double) total;
            kappa += stats.getKappa() * factor;
            accuracy += stats.getAccuracy() * factor;
            reliability += stats.getReliability() * factor;
            reliabilityStdDeviation += stats.getReliabilityStdDeviation() * factor;
            weightedPrecision += stats.getWeightedPrecision() * factor;
            weightedRecall += stats.getWeightedRecall() * factor;
            weightedF1Score += stats.getWeightedF1Score() * factor;
        }

        domainStats.setAccuracy(accuracy);
        domainStats.setKappa(kappa);
        domainStats.setReliability(reliability);
        domainStats.setReliabilityStdDeviation(reliabilityStdDeviation);
        domainStats.setWeightedPrecision(weightedPrecision);
        domainStats.setWeightedRecall(weightedRecall);
        domainStats.setWeightedF1Score(weightedF1Score);

        return domainStats;
    }

    private Integer trainAndTestDomain(RequirementList request, int n, String propertyKey, DomainStats domainStats, Integer total,
                                       HashMap<String, Integer> domainSize, HashMap<String, Stats> statsMap, String domain,
                                       Boolean context) throws Exception {
        Integer domainPartialSize = 0;
        for (Requirement r : request.getRequirements()) {
            if (r.getReqDomains(propertyKey).contains(domain)) {
                r.setRequirementType(domain);
                ++domainPartialSize;
            }
            else if (r.getRequirementType()==null ||
                    (r.getRequirementType()!=null &&
                            !r.getRequirementType().equals("Heading")))
                r.setRequirementType("Prose");
        }

        Stats s = trainAndTest(request, propertyKey, n, context);

        ConfusionMatrixStats confusionMatrixStats = new ConfusionMatrixStats();
        confusionMatrixStats.setTruePositives(s.getTruePositives());
        confusionMatrixStats.setFalsePositives(s.getFalsePositives());
        confusionMatrixStats.setFalseNegatives(s.getFalseNegatives());
        confusionMatrixStats.setTrue_negatives(s.getTrueNegatives());

        domainStats.getConfusionMatrix().put(domain, confusionMatrixStats);

        total += domainPartialSize;

        domainSize.put(domain, domainPartialSize);
        Stats stats = new Stats(s.getKappa(), s.getAccuracy(), s.getReliability(), s.getReliabilityStdDeviation(),
                s.getWeightedPrecision(), s.getWeightedRecall(), s.getWeightedF1Score());
        statsMap.put(domain, stats);
        return total;
    }

    public List<String> getMultilabelValues(String enterpriseName, String property) {
        try {
            if (fileModelSQL == null)
                fileModelSQL = new CompanyModelDAOMySQL(); //TODO FIX THIS
            List<String> models =  fileModelSQL.findAllMulti(enterpriseName, property).stream().map(CompanyModel::getProperty)
                    .collect(Collectors.toList());
            for (int n = 0; n < models.size(); n++) {
                models.set(n, models.get(n).split("#")[1]);
            }
            return models;
        } catch (SQLException | IOException e) {
            Control.getInstance().showErrorMessage(e.getMessage());
        }
        return null;
    }

    private void trainModel(List<Requirement> reqToTrain, String pathToSeq, String property, String enterpriseName, boolean update) throws IOException, InterruptedException, SQLException {
        /* Create sequential file */
        ToSeqFile.reqToSeq(reqToTrain, pathToSeq);

        /*Create the process and execute it in order to train mahout and get the results */
        ProcessBuilder pbUploadFiles =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -put " +pathToSeq + " /" + enterpriseName);
        ProcessBuilder pbGenerateVectors =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout seq2sparse -i /" +enterpriseName+" -o /"+enterpriseName);
        ProcessBuilder pbTrain =  new ProcessBuilder(BIN_BASH, "-c", "$MAHOUT_HOME/bin/mahout trainnb -i /"+enterpriseName+"/tfidf-vectors -li /"+enterpriseName+"/labelindex -o /"+enterpriseName+"/model -ow -c");
        ProcessBuilder pbDownloadModel =  new ProcessBuilder(BIN_BASH, "-c", "mkdir -p ./data/"+enterpriseName+" && $HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/model ./data/"+enterpriseName+"/");
        ProcessBuilder pbDownloadLabelIndex =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/labelindex ./data/"+enterpriseName+"/");
        ProcessBuilder pbDownloadDictionary =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -get /"+enterpriseName+"/dictionary.file-0 ./data/"+enterpriseName+"/dictionary.file-0");
        ProcessBuilder pbDownloadFrequencies =  new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -getmerge /"+enterpriseName+"/df-count ./data/"+enterpriseName+"/df-count");
        ProcessBuilder pbDeleteHadoopFiles = new ProcessBuilder(BIN_BASH, "-c", "$HADOOP_HOME/bin/hadoop fs -rm -r /"+enterpriseName);


        /* Set the enviroment configuration */
        try(BufferedReader environmentFile = new BufferedReader(new FileReader(new File("./config/environment.txt")))) {
            String line;
            while ((line = environmentFile.readLine()) != null) {
                String[] envVar = line.split(",");
                pbUploadFiles.environment().put(envVar[0], envVar[1]);
                pbGenerateVectors.environment().put(envVar[0], envVar[1]);
                pbTrain.environment().put(envVar[0], envVar[1]);
                pbDownloadModel.environment().put(envVar[0], envVar[1]);
                pbDownloadLabelIndex.environment().put(envVar[0], envVar[1]);
                pbDownloadDictionary.environment().put(envVar[0], envVar[1]);
                pbDownloadFrequencies.environment().put(envVar[0], envVar[1]);
                pbDeleteHadoopFiles.environment().put(envVar[0], envVar[1]);

            }
        }

        /* Execute all processes one by one*/
        Process uploadFiles = pbUploadFiles.start();
        int exitUpload = uploadFiles.waitFor();

        Process generateVectors = pbGenerateVectors.start();
        int exitGenerateVectors = generateVectors.waitFor();

        Process train = pbTrain.start();
        int exitTrain = train.waitFor();
        BufferedReader error = new BufferedReader(new InputStreamReader(train.getErrorStream()));

        Process downloadModel = pbDownloadModel.start();
        int exitDwModel = downloadModel.waitFor();

        Process downloadLabelIndex = pbDownloadLabelIndex.start();
        int exitDwLabelIndex = downloadLabelIndex.waitFor();

        Process downloadDictionary = pbDownloadDictionary.start();
        int exitDwDictionary = downloadDictionary.waitFor();

        Process downloadFrequencies = pbDownloadFrequencies.start();
        int exitDwFrequencies = downloadFrequencies.waitFor();

        Process deleteHadoopFiles = pbDeleteHadoopFiles.start();
        int exitDeleteHdfsFiles = deleteHadoopFiles.waitFor();

        exitProcess(uploadFiles, exitUpload, exitTrain, error);

        /* Check if everything went well */
        if (exitUpload == 0 && exitGenerateVectors == 0 && exitTrain == 0 && exitDwModel == 0
                && exitDwLabelIndex== 0 && exitDwDictionary == 0 && exitDwFrequencies == 0
                && exitDeleteHdfsFiles == 0) {
            /*Process the stored result files*/
            /*All the files will be stored in a directory with the name of the file that generated the data!*/
            String dataPath = "./data/" + enterpriseName + "/";
            File model = new File(dataPath + "model/naiveBayesModel.bin");
            File labelIndex = new File(dataPath + "labelindex");
            File dictionary = new File(dataPath + "dictionary.file-0");
            File frequencies = new File(dataPath + "df-count");

            CompanyModel fileModel = new CompanyModel(enterpriseName, property, model, labelIndex, dictionary, frequencies);

            /* Update the model in the database*/
            if (update) fileModelSQL.update(fileModel);
            else {
                if (fileModelSQL == null) fileModelSQL = new CompanyModelDAOMySQL(); //TODO FIX THIS
                fileModelSQL.save(fileModel);
            }

            /*Once we stored the fileModel delete all files */
            Path modelFile = Paths.get(dataPath + "model/naiveBayesModel.bin");
            Path labelIndexFile = Paths.get(dataPath + "labelindex");
            Path dictionaryFile = Paths.get(dataPath + "dictionary.file-0");
            Path frequenciesFile = Paths.get(dataPath + "df-count");
            Files.delete(modelFile);
            Files.delete(labelIndexFile);
            Files.delete(dictionaryFile);
            Files.delete(frequenciesFile);

            FileUtils.deleteDirectory(new File(dataPath));
        }

        /* Delete sequential file */
        File sequential = new File(pathToSeq);
        FileUtils.deleteDirectory(sequential);

        /*Deleting the directory containing the data:*/
        FileUtils.deleteDirectory(new File("./data" + enterpriseName));
    }

    private class ThreadAsync extends Thread {

        private RequirementList request;
        private String property;
        private String enterpriseName;
        private String url;
        private ResultId id;
        private boolean trainByDomain;
        List<String> modelList;

        public ThreadAsync(RequirementList request, String property, String enterpriseName, String url, ResultId id, List<String> modelList, boolean trainByDomain) {
            this.request = request;
            this.property = property;
            this.enterpriseName = enterpriseName;
            this.url = url;
            this.id = id;
            this.modelList = modelList;
            this.trainByDomain = trainByDomain;
        }

        @Override
        public void run() {
            Response response = new Response();
            try {
                if (trainByDomain) trainByDomain(request,enterpriseName,property,modelList);
                else train(request, property, enterpriseName);
                response.setMessage("Train successful");
                response.setCode(200);
                response.setId(id.getId());
            } catch (Exception e) {
                Control.getInstance().showErrorMessage(e.getMessage());
                response.setMessage(e.getMessage());
                response.setCode(500);
                response.setId(id.getId());
            }
            finally {
                AsyncService.updateClient(response, url);
            }
        }
    }
}
