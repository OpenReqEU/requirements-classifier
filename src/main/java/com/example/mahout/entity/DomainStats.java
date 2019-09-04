package com.example.mahout.entity;

import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainStats implements Serializable {

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
    @ApiModelProperty(value = "Confusion matrix")
    HashMap<String, ConfusionMatrixStats> confusionMatrix;

    public DomainStats() {
        this.confusionMatrix = new HashMap<>();
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

    public HashMap<String, ConfusionMatrixStats> getConfusionMatrix() {
        return confusionMatrix;
    }

    public void setConfusionMatrix(HashMap<String, ConfusionMatrixStats> confusionMatrix) {
        this.confusionMatrix = confusionMatrix;
    }
}
