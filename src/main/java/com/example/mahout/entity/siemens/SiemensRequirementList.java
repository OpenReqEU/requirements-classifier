package com.example.mahout.entity.siemens;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SiemensRequirementList implements Serializable {

    private List<SiemensRequirement> reqs;

    public SiemensRequirementList() {
        reqs = new ArrayList<>();
    }

    public List<SiemensRequirement> getReqs() {
        return reqs;
    }

    public void setReqs(List<SiemensRequirement> reqs) {
        this.reqs = reqs;
    }
}
