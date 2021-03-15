package ltr.collections.jrc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ltr.features.QueryDocument;

/***
 * Estrutura básica de um documento da coleção EuroVoc JRC-Acquis.
 */
public class EuroVocDoc implements QueryDocument {
    private String id;
    private String n;
    private String lang;
    private String creationDate;
    private String title;
    private String url;
    private String note;
    private String text;
    private ArrayList<String> classes;

    public EuroVocDoc(String id, String n, String lang, String creationDate, String title, String url, String note,
                      String text, ArrayList<String> classes) {
        this.id = id;
        this.n = n;
        this.lang = lang;
        this.creationDate = creationDate;
        this.title = title;
        this.url = url;
        this.note = note;
        this.text = text;
        this.classes = classes ;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EuroVocDoc that = (EuroVocDoc) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EuroVocDoc{" + "id='" + id + '\'' + "}";
    }

    @Override
    public List<String> getLabels() {
        return getClasses();
    }
}
