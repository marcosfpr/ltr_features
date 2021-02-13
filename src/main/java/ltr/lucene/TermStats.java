package ltr.lucene;

import org.apache.lucene.util.BytesRef;

/**
 * Estatísticas de um termo específico da coleção
 */
public final class TermStats {
    public BytesRef termText;
    public String field;
    public int docFreq;
    public long totalTermFreq;

    TermStats(String field, BytesRef termText, int df) {
        this.termText = (BytesRef)termText.clone();
        this.field = field;
        this.docFreq = df;
    }

    TermStats(String field, BytesRef termText, int df, long tf) {
        this.termText = (BytesRef)termText.clone();
        this.field = field;
        this.docFreq = df;
        this.totalTermFreq = tf;
    }

    public String getTermText() {
        return termText.utf8ToString();
    }

    public String toString() {
        return field + ":" + termText.utf8ToString() + ":" + docFreq + ":" + totalTermFreq;
    }
}