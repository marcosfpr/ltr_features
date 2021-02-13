package ltr.collections.rotten.indexer;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

@SuppressWarnings("deprecation")
/**
 * Analizador textual dos documentos Rottten
 */
public class RottenAnalyzer {
    static final Logger logger = Logger.getLogger(RottenAnalyzer.class.getName());
    private boolean stemming;
    private boolean stopwords;
    private CharArraySet stopList = null;

    public RottenAnalyzer(Boolean stemming, List<String> stopList) {
        this(stemming);
        if(stopList != null) {
            this.stopList =  new CharArraySet(Version.LUCENE_4_9, stopList,true);
            this.stopwords = true;
        }
    }
    public RottenAnalyzer(Boolean stemming){ //In case of no stopword removing
        this.stopwords = false;
        this.stemming = stemming;
    }

    public Analyzer getEuroVocAnalyzer(){
        return new AnalyzerWrapper() {
            @Override
            protected Analyzer getWrappedAnalyzer(String s) {
                return new StandardAnalyzer(Version.LUCENE_4_9);
            }

            @Override
            protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
                TokenStream tokenStream = resolveTokenStream(components);
                return new StandardAnalyzer.TokenStreamComponents(components.getTokenizer(), tokenStream);
            }
        };

    }

    private TokenStream resolveTokenStream(Analyzer.TokenStreamComponents components) {
        TokenStream tokenStream = new StandardFilter(Version.LUCENE_4_9, components.getTokenStream());
        tokenStream = new LowerCaseFilter(Version.LUCENE_4_9, tokenStream);
        if (stemming)
            tokenStream = new PorterStemFilter(tokenStream);
        if (stopwords)
            tokenStream = new StopFilter(Version.LUCENE_4_9, tokenStream, stopList);
        return tokenStream;
    }

    @SuppressWarnings({ "resource" })
    public Analyzer getAnalyzer(String language) {
        Analyzer analyzer = new SimpleAnalyzer(Version.LUCENE_4_9);
        if(language.equalsIgnoreCase("EN"))
            analyzer = getEuroVocAnalyzer();
        return analyzer;
    }
}