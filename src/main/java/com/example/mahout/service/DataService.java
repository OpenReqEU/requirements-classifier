package com.example.mahout.service;

import com.example.mahout.entity.Requirement;
import com.example.mahout.entity.RequirementList;
import com.example.mahout.entity.RequirementType;
import com.example.mahout.entity.Stats;
import com.example.mahout.entity.siemens.SiemensRequirement;
import com.example.mahout.entity.siemens.SiemensRequirementList;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Service
public class DataService {

    public List<Requirement> preprocess(List<Requirement> requirements) throws Exception {
        List<Requirement> filteredRequirements;
        filteredRequirements = removeEmptyRequirements(requirements);
//        filteredRequirements = removeHeaders(filteredRequirements);
        filteredRequirements = applyNPLProcess(filteredRequirements);
        return filteredRequirements;
    }

    public List<Requirement> removeHeaders(List<Requirement> requirements) {
        List<Requirement> filteredRequirements = new ArrayList<>();
        for (int i = 0; i < requirements.size(); ++i) {
            Requirement requirement = requirements.get(i);
            /*if (requirement.getRequirement_type() != null &&
                    !requirement.getRequirement_type().equals("Heading"))
                filteredRequirements.add(requirement);
            else if (requirement.getRequirement_type() == null)
                filteredRequirements.add(requirement);*/
            //FIXME headers parsing: now only if text is not null
            if (requirement.getText() != null && !requirement.getText().isEmpty())
                filteredRequirements.add(requirement);
        }
        System.out.println("Input: " + requirements.size() + " requirements, " + filteredRequirements.size() + " after filtering (header sections)");
        return filteredRequirements;
    }

    private List<Requirement> applyNPLProcess(List<Requirement> requirements) throws Exception {
ls
        
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

    private List<Requirement> removeEmptyRequirements(List<Requirement> requirements) throws Exception {
        List<Requirement> filteredRequirements = new ArrayList<>();
        for (int i = 0; i < requirements.size(); ++i) {
            Requirement requirement = requirements.get(i);
            String text = requirement.getText().trim();
            if (text != null && !text.isEmpty())
                filteredRequirements.add(requirement);
        }
        System.out.println("Input: " + requirements.size() + " requirements, " + filteredRequirements.size() + " after filtering (empty req)");
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
                requirement.setRequirement_type("Heading");
            } else {
                requirement.setText(siemensRequirement.getText());
                requirement.setRequirement_type(siemensRequirement.getReqType());
            }
            requirement.setReqDomains("reqDomains", siemensRequirement.getReqDomains());
            requirements.add(requirement);
        }
        requirementList.setRequirements(requirements);
        return requirementList;
    }

    public HashMap<String, Double> applyStats(List<Requirement> reqToTrain, List<Requirement> reqToTrainFiltered,
                                                     List<Requirement> reqToTest, List<Requirement> reqToTestFiltered,
                                                     HashMap<String, Double> stats) {

        //Get old data
        double tp = stats.get("true_positives");
        double fp = stats.get("false_positives");
        double fn = stats.get("false_negatives");
        double tn = stats.get("true_negatives");
        double headings = reqToTest.size() - reqToTestFiltered.size();
        double new_total = tp + fp + fn + tn + headings;

        //Update accuracy: add headings as TN
        double accuracy = (tp + tn + headings) / new_total;
        stats.put("accuracy", accuracy);
        stats.put("true_negatives", tn + headings);

        //Update reliability: use new accuracy
//        stats.put("reliability", ((tn + headings) / (tn + headings + fp)) /2);

        //Update weighted precision
        double precision_def = tp + fp != 0 ? tp / (tp + fp) : 0;
        double precision_prose = tn + headings + fn != 0 ? (tn + headings) / (tn + headings + fn) : 0;
        double weighted_precision = precision_def * (tp + fn) / new_total + precision_prose * (fp + tn + headings) / new_total;
        stats.put("weighted_precision", weighted_precision);

        //Update weighted recall
        double recall_def = tp + fn != 0 ? tp / (tp + fn) : 0;
        double recall_prose = tn + headings + fp != 0 ? (tn + headings) / (tn + headings + fp) : 0;
        double weighted_recall = recall_def * (tp + fn) / new_total + recall_prose * (fp + tn + headings) / new_total;
        stats.put("weighted_recall", weighted_recall);

        //Update weighted f1 score
        double f1_score_def = precision_def + recall_def != 0 ? 2 * precision_def * recall_def / (precision_def + recall_def) : 0;
        double f1_score_prose = precision_prose + recall_prose != 0 ? 2 * precision_prose * recall_prose / (precision_prose + recall_prose) : 0;
        double weighted_f1_score = f1_score_def * (tp + fn) / new_total + f1_score_prose * (fp + tn + headings) / new_total;
        stats.put("weighted_f1_score", weighted_f1_score);

        //Update kappa
        double a = tp + tn + headings;
        double b = (tp + fp) * (tp + fn) + (tn + headings + fn) * (tn + headings + fp);
        double kappa = (new_total * a - b) / (new_total * new_total - b);
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

    public HashMap<String, Double> getStats(String statistics, String positives_negatives_matrix) throws IOException {        /*Get true and false positives splitted*/
        String[] positives_negatives =  positives_negatives_matrix.split("\n");
        String[] positives=positives_negatives[0].split("\t");

        String[] negatives=positives_negatives[1].split("\t");

        /*Parse the matrix result*/
        int true_positives = Integer.parseInt(positives[0].replaceAll("\\D+",""));
        int false_negatives = Integer.parseInt(positives[1].replaceAll("\\D+",""));
        int false_positives = Integer.parseInt(negatives[0].replaceAll("\\D+",""));
        int true_negatives = Integer.parseInt(negatives[1].replaceAll("\\D+",""));

        /* Get array with every stat we want*/
        String[] stats = statistics.split("\n");

        /* Get stat individually */
        String kappa = stats[0];
        String accuracy = stats[1];
        String reliability = stats[2];
        String reliability_std_deviation = stats[3];
        String weighted_precision = stats[4];
        String weighted_recall = stats[5];
        String weighted_f1_score = stats[6];

        /* Get every element of the Stat array splitted */
        String[] kappa_arr = kappa.split(" ");
        String[] accuracy_arr = accuracy.split(" ");
        String[] reliability_arr = reliability.split(" ");
        String[] reliability_std_deviation_arr = reliability_std_deviation.split(" ");
        String[] weighted_precision_arr = weighted_precision.split(" ");
        String[] weighted_recall_arr = weighted_recall.split(" ");
        String[] weighted_f1_score_arr = weighted_f1_score.split(" ");

        /* Get value of statistic and change commas with points for later float transformation */
        String kappa_num =kappa_arr[kappa_arr.length-1].replace(",", ".");
        String accuracy_num = accuracy_arr[accuracy_arr.length-1].replace(",", ".").replace("%", "");
        String reliability_num = reliability_arr[reliability_arr.length-1].replace(",", ".").replace("%", "");
        String reliability_std_deviation_num = reliability_std_deviation_arr[reliability_std_deviation_arr.length-1].replaceAll(" ", "").replace(",", ".");
        String weighted_precision_num = weighted_precision_arr[weighted_precision_arr.length-1].replace(",", ".");
        String weighted_recall_num = weighted_recall_arr[weighted_recall_arr.length-1].replace(",", ".");
        String weighted_f1_score_num = weighted_f1_score_arr[weighted_f1_score_arr.length-1].replace(",", ".");

        /*Transform values to double*/
        double kappa_value = Double.parseDouble(kappa_num);
        double accuracy_value = Double.parseDouble(accuracy_num);
        double reliability_value = Double.parseDouble(reliability_num);
        double reliability_std_deviation_value = Double.parseDouble(reliability_std_deviation_num);
        double weighted_precision_value = Double.parseDouble(weighted_precision_num);
        double weighted_recall_value = Double.parseDouble(weighted_recall_num);
        double weighted_f1_score_value = Double.parseDouble(weighted_f1_score_num);


        HashMap<String, Double> results = new HashMap<String, Double>();

        results.put("kappa", kappa_value);
        results.put("accuracy", accuracy_value);
        results.put("reliability", reliability_value);
        results.put("reliability_std_deviation", reliability_std_deviation_value);
        results.put("weighted_precision", weighted_precision_value);
        results.put("weighted_recall", weighted_recall_value);
        results.put("weighted_f1_score", weighted_f1_score_value);
        results.put("true_positives", (double) true_positives);
        results.put("false_positives", (double) false_positives);
        results.put("false_negatives", (double) false_negatives);
        results.put("true_negatives", (double) true_negatives);

        return results;
    }

    public HashMap<String,RequirementList> mapByDomain(RequirementList request, String property) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = new HashMap<>();
        for (Requirement r : request.getRequirements()) {
            String domains[] = r.getReqDomains(property).split("\n");
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

    @Deprecated
    public Stats getWeightedStats(HashMap<String, Stats> stats, HashMap<String, Integer> domainSize) {
        Stats globalStats = new Stats();
        Integer domainGlobalSize = domainSize.values().stream().mapToInt(Integer::intValue).sum();

        double kappa, accuracy, reliability, reliability_std_deviation, weighted_precision, weighted_recall,
                weighted_f1_score;
        kappa = accuracy = reliability = reliability_std_deviation = weighted_precision = weighted_recall =
                weighted_f1_score = 0.;
        for (String domain : stats.keySet()) {
            double factor = (double) domainSize.get(domain) / (double) domainGlobalSize;
            Stats domainStats = stats.get(domain);
            kappa += domainStats.getKappa() * factor;
            accuracy += domainStats.getAccuracy() * factor;
            reliability += domainStats.getReliability() * factor;
            reliability_std_deviation += domainStats.getReliability_std_deviation() * factor;
            weighted_precision += domainStats.getWeighted_precision() * factor;
            weighted_recall += domainStats.getWeighted_recall() * factor;
            weighted_f1_score += domainStats.getWeighted_f1_score() * factor;
        }
        globalStats.setAccuracy(accuracy);
        globalStats.setKappa(kappa);
        globalStats.setReliability(reliability);
        globalStats.setReliability_std_deviation(reliability_std_deviation);
        globalStats.setWeighted_precision(weighted_precision);
        globalStats.setWeighted_recall(weighted_recall);
        globalStats.setWeighted_f1_score(weighted_f1_score);


        //Aggregation
        Integer tp, tn, fp, fn;
        tp = tn = fp = fn = 0;
        for (String domain : stats.keySet()) {
            Stats domainStats = stats.get(domain);
            tp += domainStats.getTrue_positives();
            tn += domainStats.getTrue_negatives();
            fp += domainStats.getFalse_positives();
            fn += domainStats.getFalse_negatives();
            System.out.println("Domain " + domain + "\nTP = " + domainStats.getTrue_positives() + "\nFN = " + domainStats.getFalse_negatives() + "\nFP = " + domainStats.getFalse_positives() + "\nTN = " + domainStats.getTrue_negatives() + "\n");
        }

        //Mean
//        Double tp, tn, fp, fn;
//        tp = tn = fp = fn = 0.;
//        for (String domain : stats.keySet()) {
//            double factor = (double) domainSize.get(domain) / (double) domainGlobalSize;
//            Stats domainStats = stats.get(domain);
//            tp += domainStats.getTrue_positives() * factor;
//            tn += domainStats.getTrue_negatives() * factor;
//            fn += domainStats.getFalse_negatives() * factor;
//            fp += domainStats.getFalse_positives() * factor;
//            System.out.println("Domain " + domain + "\nTP = " + domainStats.getTrue_positives() + "\nFN = " + domainStats.getFalse_negatives() + "\nFP = " + domainStats.getFalse_positives() + "\nTN = " + domainStats.getTrue_negatives() + "\n");
//        }

        globalStats.setTrue_positives(tp.intValue());
        globalStats.setTrue_negatives(tn.intValue());
        globalStats.setFalse_positives(fp.intValue());
        globalStats.setFalse_negatives(fn.intValue());

        return globalStats;
    }
}
