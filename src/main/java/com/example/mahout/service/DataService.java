package com.example.mahout.service;

import com.example.mahout.entity.Requirement;
import com.example.mahout.entity.RequirementList;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationService.class);
    public static final String TRUE_NEGATIVES = "true_negatives";

    public List<Requirement> preprocess(List<Requirement> requirements) throws Exception {
        List<Requirement> filteredRequirements;
        filteredRequirements = removeEmptyRequirements(requirements);
        return applyNPLProcess(filteredRequirements);
    }

    public List<Requirement> removeHeaders(List<Requirement> requirements) {
        List<Requirement> filteredRequirements = new ArrayList<>();
        for (int i = 0; i < requirements.size(); ++i) {
            Requirement requirement = requirements.get(i);
            if (requirement.getText() != null && !requirement.getText().isEmpty())
                filteredRequirements.add(requirement);
        }
        logger.info("Input: " + requirements.size() + " requirements, " + filteredRequirements.size() + " after filtering (header sections)");
        return filteredRequirements;
    }

    private List<Requirement> applyNPLProcess(List<Requirement> requirements) throws IOException {

        
        for (int i = 0; i < requirements.size(); ++i) {
            String text = requirements.get(i).getText();

            //Apply Capitalization
            text = text.toLowerCase(Locale.ENGLISH);

            TokenStream tokenStream = new StandardTokenizer(
                    Version.LUCENE_46, new StringReader(text));

            //Apply stopword filter
            tokenStream = new StopFilter(Version.LUCENE_46, tokenStream, EnglishAnalyzer.getDefaultStopSet());

            //Apply stem filter
            tokenStream = new PorterStemFilter(tokenStream);
            tokenStream.reset();

            StringBuilder sb = new StringBuilder();
            CharTermAttribute charTermAttr = tokenStream.getAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(charTermAttr.toString());
            }

            requirements.get(i).setText(sb.toString());

        }
        return requirements;
    }

    private List<Requirement> removeEmptyRequirements(List<Requirement> requirements) {
        List<Requirement> filteredRequirements = new ArrayList<>();
        for (int i = 0; i < requirements.size(); ++i) {
            Requirement requirement = requirements.get(i);
            String text = requirement.getText();
            if (text != null && !text.trim().isEmpty()) {
                filteredRequirements.add(requirement);
            }
        }
        logger.info("Input: " + requirements.size() + " requirements, " + filteredRequirements.size() + " after filtering (empty req)");
        return filteredRequirements;
    }

    public String getMessage(BufferedReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ( (line = reader.readLine()) != null) {
            builder.append(line + "\n");
        }

        return builder.toString();
    }

    public HashMap<String,RequirementList> mapByDomain(RequirementList request, String property) throws Exception {
        HashMap<String, RequirementList> domainRequirementsMap = new HashMap<>();
        for (Requirement r : request.getRequirements()) {
            String[] domains = r.getReqDomains(property).split("\n");
            for (String domain : domains) {
                if (!domain.trim().isEmpty()) {
                    if (domainRequirementsMap.containsKey(domain)) {
                        domainRequirementsMap.get(domain).getRequirements().add(r);
                    } else {
                        RequirementList requirementList = new RequirementList();
                        requirementList.getRequirements().add(r);
                        domainRequirementsMap.put(domain, requirementList);
                    }
                }
            }
        }
        return domainRequirementsMap;
    }
}
