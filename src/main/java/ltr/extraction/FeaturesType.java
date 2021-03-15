package ltr.extraction;

/**
 * Features envolvidas
 */
public enum FeaturesType {
    POPULARITY(null), TF(null), IDF(null), BOOLEAN(null), CLASS_FREQUENCY(null),
    SIM_TXT_TXT_LMD(new String[]{"2000", "TEXT", "TEXT", "LMD"}),
    SIM_TXT_TXT_LMJ(new String[]{"0.6", "TEXT", "TEXT", "LMJ"}),
    SIM_TXT_TXT_BM25(new String[]{"0.75", "TEXT", "TEXT", "BM25"}),
    SIM_TXT_TXT_VM(new String[]{"0.0", "TEXT", "TEXT", "VM"}),
    SIM_TXT_TIT_LMD(new String[]{"2000", "TEXT", "TITLE", "LMD"}),
    SIM_TXT_TIT_LMJ(new String[]{"0.6", "TEXT", "TITLE", "LMJ"}),
    SIM_TXT_TIT_BM25(new String[]{"0.75", "TEXT", "TITLE", "BM25"}),
    SIM_TXT_TIT_VM(new String[]{"0.0", "TEXT", "TITLE", "VM"}),
    SIM_TIT_TXT_LMD(new String[]{"2000", "TITLE", "TEXT", "LMD"}),
    SIM_TIT_TXT_LMJ(new String[]{"0.6", "TITLE", "TEXT", "LMJ"}),
    SIM_TIT_TXT_BM25(new String[]{"0.75", "TITLE", "TEXT", "BM25"}),
    SIM_TIT_TXT_VM(new String[]{"0.0", "TITLE", "TEXT", "VM"}),
    SIM_TIT_TIT_LMD(new String[]{"2000", "TITLE", "TITLE", "LMD"}),
    SIM_TIT_TIT_LMJ(new String[]{"0.6", "TITLE", "TITLE", "LMJ"}),
    SIM_TIT_TIT_BM25(new String[]{"0.75", "TITLE", "TITLE", "BM25"}),
    SIM_TIT_TIT_VM(new String[]{"0.0", "TITLE", "TITLE", "VM"});

    private String[] params;

    FeaturesType(String[] params){
        this.params = params;
    }

    String[] getParams(){
        return this.params;
    }
}
