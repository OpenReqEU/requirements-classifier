package com.example.mahout.entity;

import io.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyPropertyKey implements Serializable {

    @ApiModelProperty(value = "Company", example = "UPC")
    String company;
    @ApiModelProperty(value = "Property of the classifier", example = "requirement")
    String property;

    public CompanyPropertyKey(String company, String property) {
        this.company = company;
        this.property = property;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}
