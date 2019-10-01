package com.example.mahout.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Stats", description = "Stats results of the classifier test")
public class Stats extends ConfusionMatrixStats {

    @ApiModelProperty(value = "Kappa")
    Double kappa;
    @ApiModelProperty(value = "Accuracy")
    Double accuracy;
    @ApiModelProperty(value = "Reliability")
    Double reliability;
    @ApiModelProperty(value = "Reliability standard deviation")
    Double reliability_std_deviation;
    @ApiModelProperty(value = "Weighted precision")
    Double weighted_precision;
    @ApiModelProperty(value = "Weighted recall")
    Double weighted_recall;
    @ApiModelProperty(value = "Weighted F1 score")
    Double weighted_f1_score;

    public Stats(Double kappa, Double accuracy, Double reliability, Double reliability_std_deviation, Double weighted_precision,
                 Double weighted_recall, Double weighted_f1_score, Integer true_positives, Integer false_positives,
                 Integer false_negatives, Integer true_negatives) {
        this.kappa = kappa;
        this.accuracy = accuracy;
        this.reliability = reliability;
        this.reliability_std_deviation = reliability_std_deviation;
        this.weighted_precision = weighted_precision;
        this.weighted_recall = weighted_recall;
        this.weighted_f1_score = weighted_f1_score;
        this.true_positives = true_positives;
        this.false_positives = false_positives;
        this.false_negatives = false_negatives;
        this.true_negatives = true_negatives;
    }

    public Stats(Double kappa, Double accuracy, Double reliability, Double reliability_std_deviation, Double weighted_precision,
                 Double weighted_recall, Double weighted_f1_score) {
        this.kappa = kappa;
        this.accuracy = accuracy;
        this.reliability = reliability;
        this.reliability_std_deviation = reliability_std_deviation;
        this.weighted_precision = weighted_precision;
        this.weighted_recall = weighted_recall;
        this.weighted_f1_score = weighted_f1_score;
    }

    public Stats() {

    }

    public Stats(RecommendationList recommendationList, RequirementList classifyList, String property) {
        //confusion matrix
        true_positives = true_negatives = false_positives = false_negatives = 0;
        for (Recommendation r : recommendationList.getRecommendations()) {
            //Classified as P
            Requirement originalReq = classifyList.find(r.getRequirement());
            if (r.getRequirement_type().equals(property)) {
                //Original P
                if (r.getRequirement_type().equals(originalReq.getRequirement_type())) {
                    ++true_positives;
                }
                //Original N
                else ++false_positives;
            }
            //Classified as N
            else {
                //Original N
                if (r.getRequirement_type().equals(originalReq.getRequirement_type())) {
                    ++true_negatives;
                }
                //Original P
                else ++false_negatives;
            }
        }

        //stats evaluation
        accuracy = 100.00 * (true_negatives + true_positives) / recommendationList.getRecommendations().size();
        double a = true_negatives + false_negatives > 0 ? true_negatives / (true_negatives + false_negatives) : 0;
        double b = true_positives + false_positives > 0 ? true_positives / (true_positives + false_positives) : 0;
        reliability = (a + b) * 100.00 / 2;
        //TODO

    }

    public Double getKappa() {
        return kappa;
    }

    public void setKappa(Double kappa) {
        this.kappa = kappa;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Double getReliability() {
        return reliability;
    }

    public void setReliability(Double reliability) {
        this.reliability = reliability;
    }

    public Double getReliability_std_deviation() {
        return reliability_std_deviation;
    }

    public void setReliability_std_deviation(Double reliability_std_deviation) {
        this.reliability_std_deviation = reliability_std_deviation;
    }

    public Double getWeighted_precision() {
        return weighted_precision;
    }

    public void setWeighted_precision(Double weighted_precision) {
        this.weighted_precision = weighted_precision;
    }

    public Double getWeighted_recall() {
        return weighted_recall;
    }

    public void setWeighted_recall(Double weighted_recall) {
        this.weighted_recall = weighted_recall;
    }

    public Double getWeighted_f1_score() {
        return weighted_f1_score;
    }

    public void setWeighted_f1_score(Double weighted_f1_score) {
        this.weighted_f1_score = weighted_f1_score;
    }

}
