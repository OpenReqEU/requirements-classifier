package com.example.mahout.entity;

import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashMap;

public class DomainStats extends Stats {

    @ApiModelProperty(value = "Confusion matrix")
    HashMap<String, ConfusionMatrixStats> confusion_matrix;

    public DomainStats() {
        this.confusion_matrix = new HashMap<>();
    }

    public HashMap<String, ConfusionMatrixStats> getConfusion_matrix() {
        return confusion_matrix;
    }

    public void setConfusion_matrix(HashMap<String, ConfusionMatrixStats> confusion_matrix) {
        this.confusion_matrix = confusion_matrix;
    }
}
