package com.example.mahout.entity;

import io.swagger.annotations.ApiModel;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

@ApiModel(value = "Company Model")
public class CompanyModel implements Serializable {

    private String companyName;

    private String property;

    private byte[] model;
    private byte[] labelindex;
    private byte[] dictionary;
    private byte[] frequencies;


    private byte[] convertFiletoBlob(File file) throws IOException {
        byte[] fileContent = null;
        try {
            fileContent = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            throw new IOException("Unable to convert file " + file.getName() + " to byte array." + e.getMessage());
        }
        return fileContent;
    }

    public CompanyModel(String companyName, String property, byte[] model, byte[] labelindex, byte[] dictionary, byte[] frequency) throws IOException {
        this.companyName = companyName;
        this.property = property;
        this.model = model;
        this.labelindex = labelindex;
        this.dictionary = dictionary;
        this.frequencies = frequency;
    }

    public CompanyModel(String companyName, String property, File model, File labelindex, File dictionary, File frequency) throws IOException {
        this.companyName = companyName;
        this.property = property;
        this.model = convertFiletoBlob(model);
        this.labelindex = convertFiletoBlob(labelindex);
        this.dictionary = convertFiletoBlob(dictionary);
        this.frequencies = convertFiletoBlob(frequency);
    }
    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getCompanyName()  {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public byte[] getModel() {
        return model;
    }

    public byte[] getLabelindex() {
        return labelindex;
    }

    public byte[] getDictionary() {
        return dictionary;
    }

    public byte[] getFrequencies() {
        return frequencies;
    }

}
