package com.example.mahout.entity;

import io.swagger.annotations.ApiModel;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Classify Requirement", description = "A project requirement")
public class ClassifyRequirement extends GenericRequirement {


}
