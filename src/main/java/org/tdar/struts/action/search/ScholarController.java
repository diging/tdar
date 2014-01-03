package org.tdar.struts.action.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.Facetable;
import org.tdar.core.bean.resource.Status;
import org.tdar.search.query.SortOption;
import org.tdar.search.query.builder.QueryBuilder;
import org.tdar.search.query.builder.ResourceQueryBuilder;
import org.tdar.struts.data.DateRange;
import org.tdar.struts.data.FacetGroup;
import org.tdar.struts.interceptor.HttpOnlyIfUnauthenticated;

@SuppressWarnings("rawtypes")
@Namespace("/scholar")
@ParentPackage("default")
@Component
@Scope("prototype")
@HttpOnlyIfUnauthenticated
public class ScholarController extends AbstractLookupController {

    private static final long serialVersionUID = -4680630242612817779L;
    private int year;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    @Actions({
            @Action(value = "scholar", results = { @Result(name = SUCCESS, location = "scholar.ftl") }),
    })
    public String execute() {
        ReservedSearchParameters param = new ReservedSearchParameters();
        List<Status> statuses = new ArrayList<>();
        statuses.add(Status.ACTIVE);
        param.setStatuses(statuses);
        setSortField(SortOption.DATE_UPDATED);
        setRecordsPerPage(250);
        DateTime dt = new DateTime();
        DateTime start = dt.withYear(getYear()).withDayOfYear(1);
        DateTime end = dt.withYear(getYear() +1).withDayOfYear(1);
        param.getRegisteredDates().add(new DateRange(start.toDate(), end.toDate()));
        QueryBuilder queryBuilder = new ResourceQueryBuilder();
        queryBuilder.setOperator(QueryParser.Operator.AND);
        queryBuilder.append(param.toQueryPartGroup(this));
        try {
            handleSearch(queryBuilder);
        } catch (ParseException e) {
            addActionErrorWithException("could not generate page",e);
            return INPUT;
        }
        return SUCCESS;
    }

    @Override
    public List<FacetGroup<? extends Facetable>> getFacetFields() {
        return null;
    }
}
