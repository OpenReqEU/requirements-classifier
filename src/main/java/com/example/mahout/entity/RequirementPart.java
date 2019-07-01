package com.example.mahout.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

@ApiModel(value = "Requirement Part", description = "A part of which a Requirement is composed of (not a sub-requirement). RequirementParts are an extension mechanism for specifying additional information related to a Requirement. For example, RequirementPart can be used to include fragments of Requirement content or additional  properties of Requirement.")
public class RequirementPart implements Serializable {

    @ApiModelProperty(value = "ID of the requirement part. Use your attribute property key for multi-label classification",
            example = "domain")
    private String id;
    @ApiModelProperty(value = "Content of a requirement part. Use the value of your property for multi-label classification. In case" +
            " of multiple values, use a \\n separated list", example = "Medical\nHealthcare\nAutomotive")
    private String text;

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
}
