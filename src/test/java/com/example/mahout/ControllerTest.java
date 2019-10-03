package com.example.mahout;

import com.example.mahout.entity.Requirement;
import com.example.mahout.entity.RequirementList;
import com.example.mahout.service.ClassificationService;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationService.class);

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void trainAndTest() throws Exception {

        RequirementList requirementList = generateRequirementDataset(50);

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
    public void CRUDModelAndClassify() throws Exception {

        RequirementList requirementList = generateRequirementDataset(50);

        this.mockMvc.perform(post("/upc/classifier-component/model")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requirementList))
                .param("company", "UPC-test")
                .param("property", "DEF")
                .param("url", "http://localhost:8080"))
                .andExpect(status().isOk());

        generateThreadWait();

        this.mockMvc.perform(put("/upc/classifier-component/model")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requirementList))
                .param("company", "UPC-test")
                .param("property", "DEF")
                .param("url","http://localhost:8080"))
                .andExpect(status().isOk());

        generateThreadWait();

        this.mockMvc.perform(post("/upc/classifier-component/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requirementList))
                .param("company", "UPC-test")
                .param("property", "DEF"))
                .andExpect(status().isOk());

        this.mockMvc.perform(delete("/upc/classifier-component/model")
                .contentType(MediaType.APPLICATION_JSON)
                .param("company", "UPC-test")
                .param("property", "DEF"))
                .andExpect(status().isOk());

    }

    private String[] sentences =
            {
                    "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
                    "Wow, does that work?",
                    "Check back tomorrow; I will see if the book has arrived.",
                    "I'd rather be a bird than a fish.",
                    "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
                    "Writing a list of random sentences is harder than I initially thought it would be.",
                    "This is a Japanese doll.",
                    "If I don’t like something, I’ll stay away from it.",
                    "She wrote him a long letter, but he didn't read it.",
                    "The sky is clear; the stars are twinkling.",
                    "Christmas is coming.",
                    "I will never be this young again. Ever. Oh damn… I just got older.",
                    "The book is in front of the table.",
                    "She always speaks to him in a loud voice.",
                    "A song can make or ruin a person’s day if they let it get to them.",
                    "There was no ice cream in the freezer, nor did they have money to go to the store.",
                    "She was too short to see over the fence.",
                    "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
                    "The memory we used to share is no longer coherent.",
                    "Rock music approaches at high velocity.",
                    "We need to rent a room for our party.",
                    "The mysterious diary records the voice.",
                    "Don't step on the broken glass.",
                    "Hurry!",
                    "Malls are great places to shop; I can find everything I need under one roof.",
                    "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
                    "Lets all be unique together until we realise we are all the same.",
                    "She did her best to help him.",
                    "She borrowed the book from him many years ago and hasn't yet returned it.",
                    "I love eating toasted cheese and tuna sandwiches.",
                    "He said he was not there yesterday; however, many people saw him there.",
                    "I am never at home on Sundays.",
                    "He didn’t want to go to the dentist, yet he went anyway.",
                    "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
                    "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
                    "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
                    "        Yeah, I think it's a good environment for learning English.",
                    "Let me help you with your baggage.",
                    "I want to buy a onesie… but know it won’t suit me.",
                    "If Purple People Eaters are real… where do they find purple people to eat?",
                    "She folded her handkerchief neatly.",
                    "Please wait outside of the house.",
                    "Joe made the sugar cookies; Susan decorated them.",
                    "The body may perhaps compensates for the loss of a true metaphysics.",
                    "        Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
                    "This is the last random sentence I will be writing and I am going to stop mid-sent",
                    "I am counting my calories, yet I really want dessert.",
                    "My Mum tries to be cool by saying that she likes all the same things that I do.",
                    "A glittering gem is not enough.",
                    "We have a lot of rain in June."
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

    private synchronized void generateThreadWait() {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        try {
            logger.info("Inside synchronized block entry..." + dtf.format(LocalDateTime.now()));
            this.wait(60000);
            logger.info("Inside synchronized block exit..." + dtf.format(LocalDateTime.now()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
