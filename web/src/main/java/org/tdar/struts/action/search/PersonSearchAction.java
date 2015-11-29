package org.tdar.struts.action.search;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.SortOption;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.search.index.LookupSource;
import org.tdar.search.query.FacetGroup;
import org.tdar.search.query.builder.PersonQueryBuilder;
import org.tdar.search.service.CreatorSearchService;
import org.tdar.struts.action.AbstractLookupController;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.interceptor.annotation.HttpOnlyIfUnauthenticated;

@Namespace("/search")
@Component
@Scope("prototype")
@ParentPackage("default")
@HttpOnlyIfUnauthenticated
public class PersonSearchAction extends AbstractLookupController<Person> {

    private static final long serialVersionUID = -4399875145290579664L;

    private List<SortOption> sortOptions = SortOption.getOptionsForContext(Person.class);

    private String query;

    @Autowired
    private CreatorSearchService creatorSearchService;
    
    @Action(value = "people", results = {
            @Result(name = SUCCESS, location = "people.ftl"),
            @Result(name = INPUT, location = "person.ftl") })
    public String searchPeople() throws TdarActionException, SolrServerException, IOException {
        setSortOptions(SortOption.getOptionsForContext(Person.class));
        setMinLookupLength(0);
        setMode("PERSON");
        setLookupSource(LookupSource.PERSON);
        PersonQueryBuilder pqb = creatorSearchService.findPerson(getQuery());
        try {
            handleSearch(pqb);
        } catch (TdarRecoverableRuntimeException | ParseException trex) {
            addActionError(trex.getMessage());
            return INPUT;
        }
        return SUCCESS;
    }

    public List<SortOption> getSortOptions() {
        sortOptions.remove(SortOption.RESOURCE_TYPE);
        sortOptions.remove(SortOption.RESOURCE_TYPE_REVERSE);
        return sortOptions;
    }

    public void setSortOptions(List<SortOption> sortOptions) {
        this.sortOptions = sortOptions;
    }

    @Override
    public List<FacetGroup<? extends Enum>> getFacetFields() {
        return null;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public boolean isLeftSidebar() {
        return true;
    }
}
