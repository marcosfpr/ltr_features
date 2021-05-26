package ltr.features;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class FeatureNormalizer {
    public static HashMap<String, Feature> normalize(Map<String, Feature> features) {

        HashMap<String, Feature> normalizedFeatures = new HashMap<>();
        double sum = 0.0;

        for(Map.Entry<String, Feature> ent: features.entrySet())
            sum += ent.getValue().getFeatureValue();

        for(Map.Entry<String, Feature> ent : features.entrySet())
        {
            Feature f = ent.getValue();
            if(sum > 0)
                f.setFeatureValue(ent.getValue().getFeatureValue() / sum);
            else
                f.setFeatureValue(0D);
            normalizedFeatures.put(ent.getKey(), f);
        }
        return normalizedFeatures;
    }
    public static  Map<Integer,Map<String,Feature>> oneQueryNormalizer(Map<Integer,Map<String, Feature>> features){

        Map<Integer,Map<String,Feature>> NormalizedFeatures = new TreeMap<>();
        for(Map.Entry<Integer,Map<String,Feature>> ent : features.entrySet())
        {
            NormalizedFeatures.put(ent.getKey(), normalize(ent.getValue()));
        }
        return NormalizedFeatures;

    }
}
