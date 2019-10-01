package com.example.mahout.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Multiclassify Requirement", description = "A project requirement")
public class MultiRequirement extends GenericRequirement {

    @ApiModelProperty(value = "Aggregation of RequirementParts out of which the requirement consists of. This aggregation provides a mechanism for specifying requirement fragments or additional information for the Requirement.")
    private List<RequirementPart> requirementParts;

    public MultiRequirement() {
    }

    public List<RequirementPart> getRequirementParts() {
        return requirementParts;
    }

    public void setRequirementParts(List<RequirementPart> requirementParts) {
        this.requirementParts = requirementParts;
    }

}
