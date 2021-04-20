package ltr.collections.reuters;

import java.util.ArrayList;
import java.util.List;

public class ReutersConcept {
    
    private String id;
    private String text;
    private List<String> docs;

    public ReutersConcept(String id, String text, List<String> docs) {
        this.id = id;
        this.text = text;
        this.docs = docs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getDocs() {
        return docs;
    }

    public void setDocs(ArrayList<String> docs) {
        this.docs = docs;
    }

    @Override
    public String toString() {
        return id;
    }

    
}
