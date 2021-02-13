package ltr.lucene;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;


/**
 * Classe que lida com a frequência dos termos
 */
public class Frequency {
    static final Logger logger = Logger.getLogger(Frequency.class.getName());
    private IndexReader indexReader = null;

    public Frequency(IndexReader indexReader) {
        this.indexReader = indexReader;
    }

    /**
     * Extrai a frequencia dos (numTerms) termos mais frequentes
     * @param field
     * @param numTerms
     * @return
     */
    public TermStats[] extractTF(String field, int numTerms) {
        TermStatsQueue tiq = null;
        TermsEnum te;
        try {
            tiq = new TermStatsQueue(numTerms);
            Terms terms = MultiFields.getTerms(indexReader, field);
            if (terms != null) {
                te = terms.iterator(null);
                this.fillQueue(te, tiq, field);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        TermStats[] result = new TermStats[tiq.size()];
        int count = tiq.size() - 1;
        while (tiq.size() != 0) {
            result[count] = tiq.pop();
            count--;
        }
        return result;
    }

    private void fillQueue(TermsEnum termsEnum, TermStatsQueue tiq, String field) throws Exception {
        BytesRef term;

        while ((term = termsEnum.next()) != null) {
            BytesRef bytes = new BytesRef();
            bytes.copyBytes(term);
            tiq.insertWithOverflow(new TermStats(field, bytes, termsEnum.docFreq(),
                                    totalTF(field, term)));

        }
    }

    /**
     * TF total de um termo em um determinado campo
     * @param field
     * @param text
     * @return
     */
    private Long totalTF(String field, BytesRef text) {
        Long tf = 0L;
        Term term = new Term(field, text);
        try {
            tf = this.indexReader.totalTermFreq(term);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        return tf;
    }

}

final class TermStatsQueue extends PriorityQueue<TermStats> {
    TermStatsQueue(int size) {
        super(size);
    }

    protected boolean lessThan(TermStats lterm, TermStats rterm) {
        return lterm.totalTermFreq < rterm.totalTermFreq;
    }
}
