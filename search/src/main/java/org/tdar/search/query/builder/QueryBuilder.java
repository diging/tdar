package org.tdar.search.query.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.search.index.analyzer.NonTokenizingLowercaseKeywordAnalyzer;
import org.tdar.search.query.part.QueryPartGroup;
import org.tdar.search.service.SearchParameters;

import com.opensymphony.xwork2.TextProvider;

/**
 * 
 * $Id$
 * 
 * @author <a href="mailto:matt.cordial@asu.edu">Matt Cordial</a>
 * @version $Rev$
 * 
 */
public abstract class QueryBuilder extends QueryPartGroup {
    private static final String _AUTO = "_auto";
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    private Class<?>[] classes;
//    private List<DynamicQueryComponent> overrides = new ArrayList<DynamicQueryComponent>();
    // private List<String> omitContainedLabels = Arrays.asList("_auto");
    private Operator operator = Operator.AND;
    private QueryParser queryParser;
    private Query query;
    private String rawQuery;

//    public List<DynamicQueryComponent> getOverrides() {
//        return this.overrides;
//    }
//
//    public void setOverrides(List<DynamicQueryComponent> over) {
//        this.overrides = over;
//    }

    public void append(SearchParameters param, TextProvider provider) {
        if (param != null) {
            append(param.toQueryPartGroup(provider));
        }
    }

    /*
     * The buildQuery method is designed to extract all of the fields from the
     * classes specified by looking at the annotations on the classes and
     * extracting the @Field annotations out.
     */
    public Query buildQuery() throws ParseException {
        String qstring = rawQuery;
        if (StringUtils.isBlank(qstring)) {
            qstring = this.toString();
        }
        setQuery(new MatchAllDocsQuery());
        if (!qstring.isEmpty()) {
            setQuery(getQueryParser().parse(qstring));
            logger.trace(getQuery().toString());
        }
        return getQuery();
    }

    protected Map<String, Class<? extends Analyzer>> createPartialLabelOverrides() {
        Map<String, Class<? extends Analyzer>> map = new HashMap<String, Class<? extends Analyzer>>();
        map.put(_AUTO, NonTokenizingLowercaseKeywordAnalyzer.class);
        return map;
    }

    /*
     * Use setClasses to specify exactly which classes should be inspected to
     * identify fields. Java Reflection doesn't give any good ways to get at
     * subclasses, so it's important to specify ALL subclasses that you want to
     * inspect. Eg: InformationResource.class, Document.class will not find all
     * fields specify to Ontology.class
     */
    void setClasses(Class<?>[] classes) {
        this.classes = classes;
    }

    public Class<?>[] getClasses() {
        return classes;
    }

    public String stringContainedInLabel(String label) {
        if (createPartialLabelOverrides() == null) {
            return null;
        }
        Set<String> omitContainedLabels = getPartialLabelOverrides().keySet();

        for (String omitItem : omitContainedLabels) {
            if (StringUtils.containsIgnoreCase(label, omitItem)) {
                return omitItem;
            }
        }
        return null;
    }

    public Map<String, Class<? extends Analyzer>> getPartialLabelOverrides() {
        // TODO: see if it's safe to cache this
        return createPartialLabelOverrides();
    }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public void setOperator(Operator or) {
        this.operator = or;
    }

    public QueryParser getQueryParser() {
        return queryParser;
    }

    public void setQueryParser(QueryParser queryParser2) {
        this.queryParser = queryParser2;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public void setRawQuery(String rawQuery) {
        this.rawQuery = rawQuery;
    }

    public String getRawQuery() {
        return rawQuery;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && StringUtils.isBlank(getRawQuery());
    }

}
