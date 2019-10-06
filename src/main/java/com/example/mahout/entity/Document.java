package com.example.mahout.entity;

import java.util.*;

public class Document {

    private List<Requirement> rootItems;
    private HashMap<String,List<Requirement>> tree;


    public Document(List<Requirement> requirements) {
        Collections.sort(requirements);

        rootItems = new ArrayList<>();
        tree = new HashMap<>();

        for (Requirement r : requirements) {
            if (r.getRequirementParent() == null || r.getRequirementParent().equals("")) {
                rootItems.add(r);
            } else {
                if (!tree.containsKey(r.getRequirementParent()))
                    tree.put(r.getRequirementParent(), new ArrayList<>());
                tree.get(r.getRequirementParent()).add(new Requirement(r.getId(), r.getRequirement_type(), r.getText(), r.getDocumentPositionOrder(), r.getRequirementParent()));
            }
        }

    }

    public List<Requirement> getChildren(String requirement) {
        return tree.get(requirement);
    }
}
