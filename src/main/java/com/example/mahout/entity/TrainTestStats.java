package com.example.mahout.entity;

import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)

public class TrainTestStats implements Serializable {

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

}
