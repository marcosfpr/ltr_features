package ltr.collections.reuters;

import java.util.ArrayList;
import java.util.List;

import ltr.features.QueryDocument;

public class ReutersDoc  implements QueryDocument{
    private String id;
    private String text;
    private ArrayList<String> classes;
    
    public ReutersDoc(String id, String text, ArrayList<String> classes) {
        this.id = id;
        this.text = text;
        this.classes = classes;
    }

    public ReutersDoc(String id, String text) {
        this.id = id;
        this.text = text;
        this.classes = new ArrayList<String>();
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

    public ArrayList<String> getClasses() {
        return classes;
    }

    public void setClasses(ArrayList<String> classes) {
        this.classes = classes;
    }

    @Override
    public String getTitle() {
        return this.text;
    }

    @Override
    public List<String> getLabels() {
        return this.classes;
    }

    @Override
    public String toString() {
        return id;
    }

    
}
