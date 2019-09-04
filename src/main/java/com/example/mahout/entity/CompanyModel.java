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

    private File convertBlobtoFile(byte[] blob) throws IOException {
        File file = new File("name");
        try {
            FileUtils.writeByteArrayToFile(file, blob);
        } catch (IOException e) {
            throw new IOException("Unable to convert byte array to file." + e.getMessage());
        }
        return file;
    }

    public CompanyModel(String companyName, String property, byte[] model, byte[] labelindex, byte[] dictionary, byte[] frequency) {
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

    /** Returns the file corresponding to the name of the file, there are 4 possibilities:
     * model: returns the model
     * labelindex: returns the labelindex
     * dictionary: returns the dictionary file
     * frequencies: returns the frequencies file
     *
     * */
    public File getFile(String name) throws IOException {
        File file = null;
        switch (name) {
            case "model":
                file = convertBlobtoFile(model);
                break;
            case "labelindex":
                file = convertBlobtoFile(labelindex);
                break;
            case "dictionary":
                file = convertBlobtoFile(dictionary);
                break;
            case "frequencies":
                file = convertBlobtoFile(frequencies);
                break;
            default:
                break;
        }
        return file;

    }
}
