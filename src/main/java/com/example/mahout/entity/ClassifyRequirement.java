package com.example.mahout.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Classify Requirement", description = "A project requirement")
public class ClassifyRequirement implements Serializable {

    @ApiModelProperty(value = "ID of the requirement", example = "REQ002")
    String id;
    @ApiModelProperty(value = "Text with the requirement information", example = "The system must be implemented using " +
            "last Java version")
    String text;
    @ApiModelProperty(value = "The position of the Requirement as ascending number when Requirements are ordered and " +
            "order has relevance, such as in a document file.", example = "24")
    Integer documentPositionOrder;
    @ApiModelProperty(value = "The parent Requirement of the current Requirement for hierarchical structure in which " +
            "the parent and child are tied together and cannot be understood without each other.", example = "REQ001")
    String requirementParent;

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

}
