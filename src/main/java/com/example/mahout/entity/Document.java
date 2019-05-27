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
                tree.get(r.getRequirementParent()).add(r);
            }
        }

    }

    public List<Requirement> getRootItems() {
        return rootItems;
    }

    public void setRootItems(List<Requirement> rootItems) {
        this.rootItems = rootItems;
    }

    public HashMap<String, List<Requirement>> getTree() {
        return tree;
    }

    public void setTree(HashMap<String, List<Requirement>> tree) {
        this.tree = tree;
    }

    public List<Requirement> getMarkedItems() {
        List<Requirement> reqs = new ArrayList<>();
        for (Requirement r : rootItems) {
            List<Requirement> allChildren = iGetMarkedItems(r);
            reqs.addAll(allChildren);
        }
        return reqs;
    }

    private List<Requirement> iGetMarkedItems(Requirement root) {
        if (!tree.containsKey(root.getId()) || tree.get(root.getId()).isEmpty()) {
            root.setRequirement_type("DEF");
            return Collections.singletonList(root);
        } else {
            List<Requirement> childs = new ArrayList<>();
            for (Requirement r : tree.get(root.getId())) {
                childs.addAll(iGetMarkedItems(r));
            }
            root.setRequirement_type("Prose");
            childs.add(root);
            return childs;
        }
    }
}
