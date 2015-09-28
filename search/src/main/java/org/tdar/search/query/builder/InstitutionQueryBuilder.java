package org.tdar.search.query.builder;

import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.tdar.core.bean.entity.Institution;

public class InstitutionQueryBuilder extends QueryBuilder {

    public InstitutionQueryBuilder() {
        this.setClasses(new Class<?>[] { Institution.class });
    }

    public InstitutionQueryBuilder(Operator op) {
        this();
        setOperator(op);
    }
}
