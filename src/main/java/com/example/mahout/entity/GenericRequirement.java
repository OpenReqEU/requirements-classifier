package com.example.mahout.entity;

import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class GenericRequirement implements Serializable {

    @ApiModelProperty(value = "ID of the requirement")
    String id;
    @ApiModelProperty(value = "Text with the requirement information")
    String text;
    @ApiModelProperty(value = "The position of the Requirement as ascending number when Requirements are ordered and order has relevance, such as in a document file.")
    Integer documentPositionOrder;
    @ApiModelProperty(value = "The parent Requirement of the current Requirement for hierarchical structure in which the parent and child are tied together and cannot be understood without each other.")
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
