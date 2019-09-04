package com.example.mahout.service;

import com.example.mahout.entity.Requirement;
import com.example.mahout.entity.RequirementList;
import com.example.mahout.entity.Stats;
import com.example.mahout.entity.siemens.SiemensRequirement;
import com.example.mahout.entity.siemens.SiemensRequirementList;
import com.example.mahout.util.Control;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

@Service
public class DataService {

    public List<Requirement> preprocess(List<Requirement> requirements) throws Exception {
        List<Requirement> filteredRequirements;
        filteredRequirements = removeEmptyRequirements(requirements);
        applyNPLProcess(filteredRequirements);
        return filteredRequirements;
    }

    public List<Requirement> removeHeaders(List<Requirement> requirements) {
        List<Requirement> filteredRequirements = new ArrayList<>();
        for (int i = 0; i < requirements.size(); ++i) {
            Requirement requirement = requirements.get(i);
            //TODO FIXME headers parsing: now only if text is not null
            if (requirement.getText() != null && !requirement.getText().isEmpty())
                filteredRequirements.add(requirement);
        }
        Control.getInstance().showInfoMessage("Input: " + requirements.size() + " requirements, " + filteredRequirements.size() + " after filtering (header sections)");
        return filteredRequirements;
    }

    private List<Requirement> applyNPLProcess(List<Requirement> requirements) throws IOException {

        
        for (int i = 0; i < requirements.size(); ++i) {
            String text = requirements.get(i).getText();

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

            requirements.get(i).setText(sb.toString());

        }
        return requirements;
    }

    private List<Requirement> removeEmptyRequirements(List<Requirement> requirements) {
        List<Requirement> filteredRequirements = new ArrayList<>();
        for (int i = 0; i < requirements.size(); ++i) {
            Requirement requirement = requirements.get(i);
            String text = requirement.getText();
            if (text != null && !text.trim().isEmpty()) {
                filteredRequirements.add(requirement);
            }
        }
        Control.getInstance().showInfoMessage("Input: " + requirements.size() + " requirements, " + filteredRequirements.size() + " after filtering (empty req)");
        return filteredRequirements;
    }

    public RequirementList parseSiemensToOpenReq(SiemensRequirementList siemensRequirementList) {
        RequirementList requirementList = new RequirementList();
        List<Requirement> requirements = new ArrayList<>();
        for (SiemensRequirement siemensRequirement : siemensRequirementList.getReqs()) {
            Requirement requirement = new Requirement();
            requirement.setId(siemensRequirement.getToolId());
            if (!siemensRequirement.getHeading().isEmpty()) {
                requirement.setText(siemensRequirement.getHeading());
                requirement.setRequirementType("Heading");
            } else {
                requirement.setText(siemensRequirement.getText());
                requirement.setRequirementType(siemensRequirement.getReqType());
            }
            requirement.setReqDomains("reqDomains", siemensRequirement.getReqDomains());
            requirements.add(requirement);
        }
        requirementList.setRequirements(requirements);
        return requirementList;
    }

    public Map<String, Double> applyStats(List<Requirement> reqToTest, List<Requirement> reqToTestFiltered,
                                          Map<String, Double> stats) {

        //Get old data
        double tp = stats.get("true_positives");
        double fp = stats.get("false_positives");
        double fn = stats.get("false_negatives");
        double tn = stats.get("true_negatives");
        double headings = (double) reqToTest.size() - reqToTestFiltered.size();
        double newTotal = tp + fp + fn + tn + headings;

        //Update accuracy: add headings as TN
        double accuracy = (tp + tn + headings) / newTotal;
        stats.put("accuracy", accuracy);
        stats.put("true_negatives", tn + headings);

        //Update weighted precision
        double precisionDef = tp + fp != 0 ? tp / (tp + fp) : 0;
        double precisionProse = tn + headings + fn != 0 ? (tn + headings) / (tn + headings + fn) : 0;
        double weightedPrecision = precisionDef * (tp + fn) / newTotal + precisionProse * (fp + tn + headings) / newTotal;
        stats.put("weighted_precision", weightedPrecision);

        //Update weighted recall
        double recallDef = tp + fn != 0 ? tp / (tp + fn) : 0;
        double recallProse = tn + headings + fp != 0 ? (tn + headings) / (tn + headings + fp) : 0;
        double weightedRecall = recallDef * (tp + fn) / newTotal + recallProse * (fp + tn + headings) / newTotal;
        stats.put("weighted_recall", weightedRecall);

        //Update weighted f1 score
        double f1ScoreDef = precisionDef + recallDef != 0 ? 2 * precisionDef * recallDef / (precisionDef + recallDef) : 0;
        double f1ScoreProse = precisionProse + recallProse != 0 ? 2 * precisionProse * recallProse / (precisionProse + recallProse) : 0;
        double weightedF1Score = f1ScoreDef * (tp + fn) / newTotal + f1ScoreProse * (fp + tn + headings) / newTotal;
        stats.put("weighted_f1_score", weightedF1Score);

        //Update kappa
        double a = tp + tn + headings;
        double b = (tp + fp) * (tp + fn) + (tn + headings + fn) * (tn + headings + fp);
        double kappa = (newTotal * a - b) / (newTotal * newTotal - b);
        stats.put("kappa", kappa);

        return stats;
    }

    public String getMessage(BufferedReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ( (line = reader.readLine()) != null) {
            builder.append(line + "\n");
        }

        return builder.toString();
    }

    public Map<String, Double> getStats(String statistics, String positivesNegativesMatrix) {        /*Get true and false positives splitted*/
        String[] positives_negatives =  positivesNegativesMatrix.split("\n");
        String[] positives=positives_negatives[0].split("\t");

        String[] negatives=positives_negatives[1].split("\t");

        /*Parse the matrix result*/
        int truePositives = Integer.parseInt(positives[0].replaceAll("\\D+",""));
        int falseNegatives = Integer.parseInt(positives[1].replaceAll("\\D+",""));
        int falsePositives = Integer.parseInt(negatives[0].replaceAll("\\D+",""));
        int trueNegatives = Integer.parseInt(negatives[1].replaceAll("\\D+",""));

        /* Get array with every stat we want*/
        String[] stats = statistics.split("\n");

        /* Get stat individually */
        String kappa = stats[0];
        String accuracy = stats[1];
        String reliability = stats[2];
        String reliabilityStdDeviation = stats[3];
        String weightedPrecision = stats[4];
        String weightedRecall = stats[5];
        String weightedF1Score = stats[6];

        /* Get every element of the Stat array splitted */
        String[] kappaArr = kappa.split(" ");
        String[] accuracyArr = accuracy.split(" ");
        String[] reliabilityArr = reliability.split(" ");
        String[] reliabilityStdDeviationArr = reliabilityStdDeviation.split(" ");
        String[] weightedPrecisionArr = weightedPrecision.split(" ");
        String[] weightedRecallArr = weightedRecall.split(" ");
        String[] weightedF1ScoreArr = weightedF1Score.split(" ");

        /* Get value of statistic and change commas with points for later float transformation */
        String kappaNum =kappaArr[kappaArr.length-1].replace(",", ".");
        String accuracyNum = accuracyArr[accuracyArr.length-1].replace(",", ".").replace("%", "");
        String reliabilityNum = reliabilityArr[reliabilityArr.length-1].replace(",", ".").replace("%", "");
        String reliabilityStdDeviationNum = reliabilityStdDeviationArr[reliabilityStdDeviationArr.length-1].replaceAll(" ", "").replace(",", ".");
        String weightedPrecisionNum = weightedPrecisionArr[weightedPrecisionArr.length-1].replace(",", ".");
        String weightedRecallNum = weightedRecallArr[weightedRecallArr.length-1].replace(",", ".");
        String weightedF1ScoreNum = weightedF1ScoreArr[weightedF1ScoreArr.length-1].replace(",", ".");

        /*Transform values to double*/
        double kappaValue = Double.parseDouble(kappaNum);
        double accuracyValue = Double.parseDouble(accuracyNum);
        double reliabilityValue = Double.parseDouble(reliabilityNum);
        double reliabilityStdDeviationValue = Double.parseDouble(reliabilityStdDeviationNum);
        double weightedPrecisionValue = Double.parseDouble(weightedPrecisionNum);
        double weightedRecallValue = Double.parseDouble(weightedRecallNum);
        double weightedF1ScoreValue = Double.parseDouble(weightedF1ScoreNum);


        HashMap<String, Double> results = new HashMap<>();

        results.put("kappa", kappaValue);
        results.put("accuracy", accuracyValue);
        results.put("reliability", reliabilityValue);
        results.put("reliability_std_deviation", reliabilityStdDeviationValue);
        results.put("weighted_precision", weightedPrecisionValue);
        results.put("weighted_recall", weightedRecallValue);
        results.put("weighted_f1_score", weightedF1ScoreValue);
        results.put("true_positives", (double) truePositives);
        results.put("false_positives", (double) falsePositives);
        results.put("false_negatives", (double) falseNegatives);
        results.put("true_negatives", (double) trueNegatives);

        return results;
    }

    public Map<String,RequirementList> mapByDomain(RequirementList request, String property) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = new HashMap<>();
        for (Requirement r : request.getRequirements()) {
            String[] domains = r.getReqDomains(property).split("\n");
            for (String domain : domains) {
                if (!domain.trim().isEmpty()) {
                    if (domainRequirementsMap.containsKey(domain)) {
                        domainRequirementsMap.get(domain).getRequirements().add(r);
                    } else {
                        RequirementList requirementList = new RequirementList();
                        requirementList.getRequirements().add(r);
                        domainRequirementsMap.put(domain, requirementList);
                    }
                }
            }
        }
        return domainRequirementsMap;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Stats getWeightedStats(Map<String, Stats> stats, Map<String, Integer> domainSize) {
        Stats globalStats = new Stats();
        Integer domainGlobalSize = domainSize.values().stream().mapToInt(Integer::intValue).sum();

        double kappa;
        double accuracy;
        double reliability;
        double reliabilityStdDeviation;
        double weightedPrecision;
        double weightedRecall;
        double weightedF1Score;
        kappa = accuracy = reliability = reliabilityStdDeviation = weightedPrecision = weightedRecall =
                weightedF1Score = 0.;
        for (Map.Entry<String,Stats> entry : stats.entrySet()) {
            String domain = entry.getKey();
            Stats domainStats = entry.getValue();
            double factor = (double) domainSize.get(domain) / (double) domainGlobalSize;
            kappa += domainStats.getKappa() * factor;
            accuracy += domainStats.getAccuracy() * factor;
            reliability += domainStats.getReliability() * factor;
            reliabilityStdDeviation += domainStats.getReliabilityStdDeviation() * factor;
            weightedPrecision += domainStats.getWeightedPrecision() * factor;
            weightedRecall += domainStats.getWeightedRecall() * factor;
            weightedF1Score += domainStats.getWeightedF1Score() * factor;
        }
        globalStats.setAccuracy(accuracy);
        globalStats.setKappa(kappa);
        globalStats.setReliability(reliability);
        globalStats.setReliabilityStdDeviation(reliabilityStdDeviation);
        globalStats.setWeightedPrecision(weightedPrecision);
        globalStats.setWeightedRecall(weightedRecall);
        globalStats.setWeightedF1Score(weightedF1Score);


        //Aggregation
        Integer tp;
        Integer tn;
        Integer fp;
        Integer fn;
        tp = tn = fp = fn = 0;
        for (Map.Entry<String,Stats> entry : stats.entrySet()) {
            String domain = entry.getKey();
            Stats domainStats = entry.getValue();
            tp += domainStats.getTruePositives();
            tn += domainStats.getTrueNegatives();
            fp += domainStats.getFalsePositives();
            fn += domainStats.getFalseNegatives();
            Control.getInstance().showInfoMessage("Domain " + domain + "\nTP = " + domainStats.getTruePositives() + "\nFN = " + domainStats.getFalseNegatives() + "\nFP = " + domainStats.getFalsePositives() + "\nTN = " + domainStats.getTrueNegatives() + "\n");
        }

        globalStats.setTruePositives(tp);
        globalStats.setTrueNegatives(tn);
        globalStats.setFalsePositives(fp);
        globalStats.setFalseNegatives(fn);

        return globalStats;
    }
}
