package com.example.mahout.entity;

import java.util.*;

public class Document {

    private List<Requirement> rootItems;
    private Map<String,List<Requirement>> tree;


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
                tree.get(r.getRequirementParent()).add(new Requirement(r.getId(), r.getRequirementType(), r.getText(), r.getDocumentPositionOrder(), r.getRequirementParent()));
            }
        }

    }

    public List<Requirement> getRootItems() {
        return rootItems;
    }

    public void setRootItems(List<Requirement> rootItems) {
        this.rootItems = rootItems;
    }

    public Map<String, List<Requirement>> getTree() {
        return tree;
    }

    public void setTree(Map<String, List<Requirement>> tree) {
        this.tree = tree;
    }

    public List<Requirement> getChildren(String requirement) {
        return tree.get(requirement);
    }
}
