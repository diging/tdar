package org.tdar.index;

import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.TrimFilter;

public final class LowercaseWhiteSpaceStandardAnalyzer extends Analyzer {

    /*
     * Treats each term within the field as it's own token
     * (non-Javadoc)
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(java.lang.String, java.io.Reader)
     */
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        StandardTokenizer st = new StandardTokenizer(Version.LUCENE_31, reader);
        LowerCaseFilter stream = new LowerCaseFilter(Version.LUCENE_31,st);
        TrimFilter trimFilter = new TrimFilter(stream, true);
        ASCIIFoldingFilter filter = new ASCIIFoldingFilter(trimFilter);
        return filter;
    }

}
