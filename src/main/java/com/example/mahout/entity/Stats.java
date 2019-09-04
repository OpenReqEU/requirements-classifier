package com.example.mahout.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Stats", description = "Stats results of the classifier test")
public class Stats implements Serializable {

    @ApiModelProperty(value = "Kappa")
    Double kappa;
    @ApiModelProperty(value = "Accuracy")
    Double accuracy;
    @ApiModelProperty(value = "Reliability")
    Double reliability;
    @ApiModelProperty(value = "Reliability standard deviation")
    Double reliabilityStdDeviation;
    @ApiModelProperty(value = "Weighted precision")
    Double weightedPrecision;
    @ApiModelProperty(value = "Weighted recall")
    Double weightedRecall;
    @ApiModelProperty(value = "Weighted F1 score")
    Double weightedF1Score;
    @ApiModelProperty(value = "True positives")
    Integer truePositives;
    @ApiModelProperty(value = "False positives")
    Integer falsePositives;
    @ApiModelProperty(value = "False negatives")
    Integer falseNegatives;
    @ApiModelProperty(value = "True negatives")
    Integer trueNegatives;

    public Stats(Double kappa, Double accuracy, Double reliability, Double reliabilityStdDeviation, Double weightedPrecision,
                 Double weightedRecall, Double weightedF1Score, Integer truePositives, Integer falsePositives,
                 Integer falseNegatives, Integer trueNegatives) {
        this.kappa = kappa;
        this.accuracy = accuracy;
        this.reliability = reliability;
        this.reliabilityStdDeviation = reliabilityStdDeviation;
        this.weightedPrecision = weightedPrecision;
        this.weightedRecall = weightedRecall;
        this.weightedF1Score = weightedF1Score;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.falseNegatives = falseNegatives;
        this.trueNegatives = trueNegatives;
    }

    public Stats(Double kappa, Double accuracy, Double reliability, Double reliabilityStdDeviation, Double weightedPrecision,
                 Double weightedRecall, Double weightedF1Score) {
        this.kappa = kappa;
        this.accuracy = accuracy;
        this.reliability = reliability;
        this.reliabilityStdDeviation = reliabilityStdDeviation;
        this.weightedPrecision = weightedPrecision;
        this.weightedRecall = weightedRecall;
        this.weightedF1Score = weightedF1Score;
    }

    public Stats() {

    }

    public Stats(RecommendationList recommendationList, RequirementList classifyList, String property) {
        //confusion matrix
        truePositives = trueNegatives = falsePositives = falseNegatives = 0;
        for (Recommendation r : recommendationList.getRecommendations()) {
            //Classified as P
            Requirement originalReq = classifyList.find(r.getRequirement());
            if (r.getRequirementType().equals(property)) {
                //Original P
                if (r.getRequirementType().equals(originalReq.getRequirementType())) {
                    ++truePositives;
                }
                //Original N
                else ++falsePositives;
            }
            //Classified as N
            else {
                //Original N
                if (r.getRequirementType().equals(originalReq.getRequirementType())) {
                    ++trueNegatives;
                }
                //Original P
                else ++falseNegatives;
            }
        }

        //stats evaluation
        accuracy = 100.00 * (trueNegatives + truePositives) / recommendationList.getRecommendations().size();
        double a = trueNegatives + falseNegatives > 0 ? trueNegatives / (trueNegatives + falseNegatives) : 0;
        double b = truePositives + falsePositives > 0 ? truePositives / (truePositives + falsePositives) : 0;
        reliability = (a + b) * 100.00 / 2;

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

    public Double getReliabilityStdDeviation() {
        return reliabilityStdDeviation;
    }

    public void setReliabilityStdDeviation(Double reliabilityStdDeviation) {
        this.reliabilityStdDeviation = reliabilityStdDeviation;
    }

    public Double getWeightedPrecision() {
        return weightedPrecision;
    }

    public void setWeightedPrecision(Double weightedPrecision) {
        this.weightedPrecision = weightedPrecision;
    }

    public Double getWeightedRecall() {
        return weightedRecall;
    }

    public void setWeightedRecall(Double weightedRecall) {
        this.weightedRecall = weightedRecall;
    }

    public Double getWeightedF1Score() {
        return weightedF1Score;
    }

    public void setWeightedF1Score(Double weightedF1Score) {
        this.weightedF1Score = weightedF1Score;
    }

    public Integer getTruePositives() {
        return truePositives;
    }

    public void setTruePositives(Integer truePositives) {
        this.truePositives = truePositives;
    }

    public Integer getFalsePositives() {
        return falsePositives;
    }

    public void setFalsePositives(Integer falsePositives) {
        this.falsePositives = falsePositives;
    }

    public Integer getFalseNegatives() {
        return falseNegatives;
    }

    public void setFalseNegatives(Integer falseNegatives) {
        this.falseNegatives = falseNegatives;
    }

    public Integer getTrueNegatives() {
        return trueNegatives;
    }

    public void setTrueNegatives(Integer trueNegatives) {
        this.trueNegatives = trueNegatives;
    }
}
