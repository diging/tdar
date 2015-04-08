package org.tdar.search.index.analyzer;

import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.TrimFilter;
import org.tdar.core.configuration.TdarConfiguration;

public final class TdarCaseSensitiveStandardAnalyzer extends Analyzer {

    /*
     * Treats each term within the field as it's own token
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(java.lang.String, java.io.Reader)
     */
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        StandardTokenizer st = new StandardTokenizer(Version.LUCENE_36, reader);
        TrimFilter trimFilter = new TrimFilter(st, true);
        StopFilter stopFilter = new StopFilter(Version.LUCENE_36, trimFilter, TdarConfiguration.getInstance().getStopWords());
        ASCIIFoldingFilter filter = new ASCIIFoldingFilter(stopFilter);
        return filter;
    }

}
