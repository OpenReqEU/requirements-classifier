package com.example.mahout;

import com.example.mahout.entity.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mortbay.util.SingletonList;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EntityTest {

    @Test
    public void entityTest() {

        //ConfussionMatrixStats
        ConfusionMatrixStats confusionMatrixStats = new ConfusionMatrixStats();
        confusionMatrixStats.setTrue_negatives(15);
        confusionMatrixStats.setFalse_negatives(5);
        confusionMatrixStats.setFalse_positives(3);
        confusionMatrixStats.setTrue_positives(10);
        Assert.assertEquals(15, (int) confusionMatrixStats.getTrue_negatives());
        Assert.assertEquals(5, (int) confusionMatrixStats.getFalse_negatives());
        Assert.assertEquals(3, (int) confusionMatrixStats.getFalse_positives());
        Assert.assertEquals(10, (int) confusionMatrixStats.getTrue_positives());

        //DomainStats
        DomainStats domainStats = new DomainStats();
        domainStats.setAccuracy(0.9);
        domainStats.setKappa(0.8);
        domainStats.setReliability(0.7);
        domainStats.setReliability_std_deviation(0.6);
        domainStats.setWeighted_f1_score(0.5);
        domainStats.setWeighted_precision(0.4);
        domainStats.setWeighted_recall(0.3);
        domainStats.setConfusion_matrix(new HashMap<>());
        Assert.assertEquals(0.9, domainStats.getAccuracy(), 0.0);
        Assert.assertEquals(0.8, domainStats.getKappa(), 0.0);
        Assert.assertEquals(0.7, domainStats.getReliability(), 0.0);
        Assert.assertEquals(0.6, domainStats.getReliability_std_deviation(), 0.0);
        Assert.assertEquals(0.5, domainStats.getWeighted_f1_score(), 0.0);
        Assert.assertEquals(0.4, domainStats.getWeighted_precision(), 0.0);
        Assert.assertEquals(0.3, domainStats.getWeighted_recall(), 0.0);
        Assert.assertTrue(domainStats.getConfusion_matrix().isEmpty());

        //Stats
        Stats stats = new Stats(0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 15, 5, 3, 10);
        Assert.assertEquals(15, (int) stats.getTrue_positives());
        Assert.assertEquals(5, (int) stats.getFalse_positives());
        Assert.assertEquals(3, (int) stats.getFalse_negatives());
        Assert.assertEquals(10, (int) stats.getTrue_negatives());
        Assert.assertEquals(0.9, stats.getKappa(), 0.0);
        Assert.assertEquals(0.8, stats.getAccuracy(), 0.0);
        Assert.assertEquals(0.7, stats.getReliability(), 0.0);
        Assert.assertEquals(0.6, stats.getReliability_std_deviation(), 0.0);
        Assert.assertEquals(0.5, stats.getWeighted_precision(), 0.0);
        Assert.assertEquals(0.4, stats.getWeighted_recall(), 0.0);
        Assert.assertEquals(0.3, stats.getWeighted_f1_score(), 0.0);

        Stats stats1 = new Stats(0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3);
        Assert.assertEquals(0.9, stats1.getKappa(), 0.0);
        Assert.assertEquals(0.8, stats1.getAccuracy(), 0.0);
        Assert.assertEquals(0.7, stats1.getReliability(), 0.0);
        Assert.assertEquals(0.6, stats1.getReliability_std_deviation(), 0.0);
        Assert.assertEquals(0.5, stats1.getWeighted_precision(), 0.0);
        Assert.assertEquals(0.4, stats1.getWeighted_recall(), 0.0);
        Assert.assertEquals(0.3, stats1.getWeighted_f1_score(), 0.0);

        Requirement requirement = new Requirement();
        requirement.setId("R01");
        requirement.setRequirement_type("DEF");
        Recommendation recommendation = new Recommendation();
        recommendation.setConfidence(100.0);
        recommendation.setRequirement_type("DEF");
        recommendation.setRequirement("R01");

        RecommendationList recommendationList = new RecommendationList();
        recommendationList.setRecommendations(Collections.singletonList(recommendation));
        RequirementList requirementList = new RequirementList();
        requirementList.setRequirements(Collections.singletonList(requirement));
        Stats stats2 = new Stats(recommendationList, requirementList, "DEF");

    }

}
