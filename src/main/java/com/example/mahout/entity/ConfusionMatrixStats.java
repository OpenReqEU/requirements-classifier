package com.example.mahout.entity;

import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfusionMatrixStats implements Serializable {

    @ApiModelProperty(value = "True positives")
    Integer true_positives;
    @ApiModelProperty(value = "False positives")
    Integer false_positives;
    @ApiModelProperty(value = "False negatives")
    Integer false_negatives;
    @ApiModelProperty(value = "True negatives")
    Integer true_negatives;

    public Integer getTrue_positives() {
        return true_positives;
    }

    public void setTrue_positives(Integer true_positives) {
        this.true_positives = true_positives;
    }

    public Integer getFalse_positives() {
        return false_positives;
    }

    public void setFalse_positives(Integer false_positives) {
        this.false_positives = false_positives;
    }

    public Integer getFalse_negatives() {
        return false_negatives;
    }

    public void setFalse_negatives(Integer false_negatives) {
        this.false_negatives = false_negatives;
    }

    public Integer getTrue_negatives() {
        return true_negatives;
    }

    public void setTrue_negatives(Integer true_negatives) {
        this.true_negatives = true_negatives;
    }
}
