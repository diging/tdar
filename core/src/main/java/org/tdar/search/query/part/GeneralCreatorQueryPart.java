package org.tdar.search.query.part;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.bean.entity.Creator;
import org.tdar.search.query.QueryFieldNames;

public class GeneralCreatorQueryPart extends FieldQueryPart<Creator> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean useProximity = true;
    protected static final float NAME_BOOST = 6f;
    protected static final float ANY_FIELD_BOOST = 2f;

    public GeneralCreatorQueryPart(Creator creator) {
        setAllowInvalid(true);
        add(creator);
    }

    @Override
    public String generateQueryString() {
        QueryPartGroup group = new QueryPartGroup(getOperator());
        for (Creator value : getFieldValues()) {
            group.append(this.getQueryPart(value));
        }
        return group.generateQueryString();
    }

    protected QueryPartGroup getQueryPart(Creator value) {
        String cleanedQueryString = getCleanedQueryString(value.getProperName());

        QueryPartGroup primary = new QueryPartGroup(Operator.OR);

        logger.trace(cleanedQueryString);

        FieldQueryPart<String> titlePart = new FieldQueryPart<String>(QueryFieldNames.NAME_TOKEN, cleanedQueryString);

        List<String> fields = new ArrayList<String>();
        for (String txt : StringUtils.split(cleanedQueryString)) {
            txt = txt.replace("\"", "");
            if (!ArrayUtils.contains(QueryPart.LUCENE_RESERVED_WORDS, txt)) {
                fields.add(txt);
            }
        }

        FieldQueryPart<String> allFieldsAsPart = new FieldQueryPart<String>(QueryFieldNames.NAME_TOKEN, fields).setBoost(ANY_FIELD_BOOST);
        allFieldsAsPart.setOperator(Operator.AND);
        allFieldsAsPart.setPhraseFormatters(PhraseFormatter.ESCAPED);

        if (cleanedQueryString.contains(" ")) {
            titlePart = new FieldQueryPart<String>(QueryFieldNames.NAME_PHRASE, cleanedQueryString);
            // FIXME: magic words
            if (useProximity) {
                titlePart.setProximity(3);
            }
        }

        primary.append(titlePart.setBoost(NAME_BOOST));
        primary.append(allFieldsAsPart);

        return primary;
    }
}
