package ltr.collections.jrc;

import java.util.ArrayList;
import java.util.Objects;

/***
 * Estrutura básica de um documento conceitual, responsável por agrupar o conteúdo de múltiplos
 * documentos pertencentes à mesma classe
 */
public class EuroVocConcept {
    private String id;
    private String title;
    private String text;
    private ArrayList<String> docs;

    public EuroVocConcept(String id, String text, String title, ArrayList<String> docs) {
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

    public ArrayList<String> getDocs() {
        return docs;
    }

    public void setDocs(ArrayList<String> docs) {
        this.docs = docs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EuroVocConcept that = (EuroVocConcept) o;
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
