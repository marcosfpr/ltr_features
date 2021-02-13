package ltr.collections.rotten;

import java.util.List;
import java.util.Objects;

public class RottenConcept {
    private String id;
    private String title;
    private String text;
    private List<String> docs;

    public RottenConcept(String id, String text, String title, List<String> docs) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.docs = docs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public void setDocs(List<String> docs) {
        this.docs = docs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RottenConcept that = (RottenConcept) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EuroVocConcept{" +
                "id='" + id + '\'' +
                '}';
    }
}
