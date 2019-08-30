package com.example.mahout;

import com.example.mahout.entity.Requirement;
import com.example.mahout.entity.RequirementList;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testB() throws Exception {

        RequirementList requirementList = generateRequirementDataset(10);

        this.mockMvc.perform(post("/upc/classifier-component/model")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requirementList))
                .param("company", "UPC")
                .param("property", "DEF")
                .param("url", "http://google.com"))
                .andExpect(status().isOk());
    }

    @Test
    @Ignore
    public void testC() throws Exception {
        RequirementList requirementList = generateRequirementDataset(10);

        this.mockMvc.perform(post("/upc/classifier-component/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requirementList))
                .param("company", "UPC")
                .param("property", "DEF")
                .param("url", "http://google.com"))
                .andExpect(status().isOk());
    }

    @Test
    public void testA() throws Exception {

        RequirementList requirementList = generateRequirementDataset(10);

        try {
            this.mockMvc.perform(post("/upc/classifier-component/train&test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(requirementList))
                    .param("k", "1")
                    .param("property", "DEF"))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            //TODO handle exception
        }
    }

    @Test
    public void testE() throws Exception {

        this.mockMvc.perform(delete("/upc/classifier-component/model")
                .contentType(MediaType.APPLICATION_JSON)
                .param("company", "UPC")
                .param("property", "DEF"))
                .andExpect(status().isNotFound());
    }

    private String[] sentences =
            {
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                    "Integer nec odio",
                    "Praesent libero",
                    "Sed cursus ante dapibus diam",
                    "Sed nisi",
                    "Nulla quis sem at nibh elementum imperdiet",
                    "Duis sagittis ipsum",
                    "Praesent mauris",
                    "Fusce nec tellus sed augue semper porta",
                    "Mauris massa."
            };

    private RequirementList generateRequirementDataset(int n) {
        RequirementList requirementList = new RequirementList();
        String reqType1 = "DEF";
        String reqType2 = "Prose";
        for (int i = 0; i < n; ++i) {
            Requirement r = new Requirement();

            //Id
            r.setId(String.valueOf(i));

            //Type
            if (i % 2 == 0) r.setRequirement_type(reqType1);
            else r.setRequirement_type(reqType2);

            //Text
            r.setText(sentences[i%sentences.length]);
            r.setDocumentPositionOrder(i+1);

            requirementList.getRequirements().add(r);
        }

        return requirementList;
    }

    private String toJson(Object list) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(list);
    }

}
