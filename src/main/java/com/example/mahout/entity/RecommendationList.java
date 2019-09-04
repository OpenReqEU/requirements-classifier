package com.example.mahout.entity;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationList implements Serializable {

    private List<Recommendation> recommendations;

    public RecommendationList() {
        recommendations = new ArrayList<>();
    }

    public RecommendationList(List<Recommendation> values) {
        this.recommendations = values;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }
}
