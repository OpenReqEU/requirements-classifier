package com.example.mahout.entity;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

public class ConfusionMatrixStats implements Serializable {

    @ApiModelProperty(value = "True positives")
    Integer truePositives;
    @ApiModelProperty(value = "False positives")
    Integer falsePositives;
    @ApiModelProperty(value = "False negatives")
    Integer falseNegatives;
    @ApiModelProperty(value = "True negatives")
    Integer trueNegatives;

    public void setTruePositives(Integer truePositives) {
        this.truePositives = truePositives;
    }

    public void setFalsePositives(Integer falsePositives) {
        this.falsePositives = falsePositives;
    }

    public void setFalseNegatives(Integer falseNegatives) {
        this.falseNegatives = falseNegatives;
    }

    public void setTrue_negatives(Integer trueNegatives) {
        this.trueNegatives = trueNegatives;
    }
}
