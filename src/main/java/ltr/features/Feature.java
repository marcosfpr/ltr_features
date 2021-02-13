package ltr.features;

import java.util.Objects;

/**
 * Define uma característica (valor com significado implícito)
 */
public class Feature {
    private String featureName;
    private Double featureValue;
    private String docID;
    private String classID;
    private Integer classRankForDocument;
    private String label;

    public Feature(String featureName, Double featureValue, String docID, String classID, Integer classRankForDocument) {
        this.featureName = featureName;
        this.featureValue = featureValue;
        this.docID = docID;
        this.classID = classID;
        this.classRankForDocument = classRankForDocument;
    }

    public Feature(String featureName, Double featureValue, String docID, String classID, String label) {
        this.featureName = featureName;
        this.featureValue = featureValue;
        this.docID = docID;
        this.classID = classID;
        this.label = label;
    }

    public Feature(String featureName, Double featureValue, String docID) {
        this.featureName = featureName;
        this.featureValue = featureValue;
        this.docID = docID;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public Double getFeatureValue() {
        return featureValue;
    }

    public void setFeatureValue(Double featureValue) {
        this.featureValue = featureValue;
    }

    public String getDocID() {
        return docID;
    }

    public void setDocID(String docID) {
        this.docID = docID;
    }

    public String getClassID() {
        return classID;
    }

    public void setClassID(String classID) {
        this.classID = classID;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Feature feature = (Feature) o;
        return Objects.equals(featureName, feature.featureName) &&
                Objects.equals(featureValue, feature.featureValue) &&
                Objects.equals(docID, feature.docID) &&
                Objects.equals(classID, feature.classID) &&
                Objects.equals(classRankForDocument, feature.classRankForDocument) &&
                Objects.equals(label, feature.label);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.docID);
        hash = 29 * hash + Objects.hashCode(this.classID);
        return hash;
    }
}
