package ltr.features;

import java.util.List;

/**
 * Define uma interface de conceito de documento
 */
public interface QueryDocument {
    public String getId();
    
    public String getTitle();

    public String getText();

    public List<String> getLabels();


}
