package ltr.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;

import ltr.lucene.Frequency;
import ltr.lucene.TermStats;

/**
 * Classe geradora de estatísticas sobre o índice
 */
public class IndexInfo {
    static final Logger logger = Logger.getLogger(IndexInfo.class.getName());

    private TermStats[] topTermsTF = null;
    private IndexReader indexReader;

    public IndexInfo(IndexReader indexReader) {
        this.indexReader = indexReader;
    }

    /**
     * Obtem uma lista com os termos de maior tf
     * @param field
     * @param numTerms
     * @return
     */
    public List<String> getTopTermsTF(String field, Integer numTerms) {
        List<String> topTF = new ArrayList<String>();
        if (topTermsTF == null){
            Frequency terms = new Frequency(indexReader);
            topTermsTF = terms.extractTF(field, numTerms);
        }
        for(TermStats ts : topTermsTF) {
            topTF.add(ts.getTermText());
        }
        return topTF;
    }

    public Long getDocumentLength(int docId, String field){
        long length = 0L;
        try {
            Terms terms = indexReader.getTermVector(docId, field);
            if (terms != null && terms.size() > 0) {
                TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
                while (termsEnum.next() != null) {// explore the terms for this field
                    DocsEnum docsEnum = termsEnum.docs(null, null); // in this case only one document
                    while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        length += docsEnum.freq();
                    }
                }
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return length;
    }
}
