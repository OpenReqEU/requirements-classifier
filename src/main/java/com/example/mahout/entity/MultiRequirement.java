package com.example.mahout.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Multiclassify Requirement", description = "A project requirement")
public class MultiRequirement implements Serializable {

    @ApiModelProperty(value = "ID of the requirement")
    String id;
    @ApiModelProperty(value = "Text with the requirement information")
    String text;
    @ApiModelProperty(value = "The position of the Requirement as ascending number when Requirements are ordered and order has relevance, such as in a document file.")
    Integer documentPositionOrder;
    @ApiModelProperty(value = "The parent Requirement of the current Requirement for hierarchical structure in which the parent and child are tied together and cannot be understood without each other.")
    String requirementParent;
    @ApiModelProperty(value = "Aggregation of RequirementParts out of which the requirement consists of. This aggregation provides a mechanism for specifying requirement fragments or additional information for the Requirement.")
    private List<RequirementPart> requirementParts;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getDocumentPositionOrder() {
        return documentPositionOrder;
    }

    public void setDocumentPositionOrder(Integer documentPositionOrder) {
        this.documentPositionOrder = documentPositionOrder;
    }

    public String getRequirementParent() {
        return requirementParent;
    }

    public void setRequirementParent(String requirementParent) {
        this.requirementParent = requirementParent;
    }

    public List<RequirementPart> getRequirementParts() {
        return requirementParts;
    }

    public void setRequirementParts(List<RequirementPart> requirementParts) {
        this.requirementParts = requirementParts;
    }

}
