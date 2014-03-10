package org.tdar.search.query.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.entity.Creator;
import org.tdar.core.bean.entity.Dedupable;
import org.tdar.core.bean.entity.ResourceCreator;
import org.tdar.core.bean.entity.ResourceCreatorRole;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.search.query.QueryFieldNames;
import org.tdar.struts.data.ResourceCreatorProxy;

import com.opensymphony.xwork2.TextProvider;

public class CreatorQueryPart<C extends Creator> extends
        AbstractHydrateableQueryPart<C> {

    private List<ResourceCreatorRole> roles = new ArrayList<ResourceCreatorRole>();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @SuppressWarnings("unchecked")
    public CreatorQueryPart(String fieldName, Class<C> creatorClass, C creator,
            List<ResourceCreatorProxy> proxyList) {
        // set default of "or"
        setOperator(Operator.OR);
        setActualClass(creatorClass);
        setFieldName(fieldName);
        // setDisplayName(getMessage("creatorQueryPart.label"));
        for (int i = 0; i < proxyList.size(); i++) {
            try {
                ResourceCreatorProxy proxy = proxyList.get(i);
                ResourceCreator rc = proxy.getResourceCreator();
                if (proxy.isValid()) {
                    List<Creator> creators = new ArrayList<Creator>();
                    if (rc.getCreator() instanceof Dedupable<?>) {
                        creators.addAll(((Dedupable<Creator>) rc.getCreator())
                                .getSynonyms());
                    }
                    creators.add(rc.getCreator());
                    for (Creator creator_ : creators) {
                        if (Persistable.Base.isTransient(creator_)) {
                            // user entered a complete-ish creator record but
                            // autocomplete callback did fire successfully
                            throw new TdarRecoverableRuntimeException(
                                    "creatorQueryPart.use_autocomplete",
                                    Arrays.asList(creator_.toString()));
                        }
                        this.roles.add(rc.getRole());
                        this.getFieldValues().add((C) creator_);
                    }
                }
            } catch (NullPointerException npe) {
                logger.trace("NPE in creator construction, skipping...", npe);
            }
        }
    }

    @Override
    public String generateQueryString() {
        QueryPartGroup group = new QueryPartGroup(Operator.OR);
        List<Integer> trans = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        // iterate through all of the values; if any of them are transient, put
        // those positions off to the side
        for (int i = 0; i < getFieldValues().size(); i++) {
            if (Persistable.Base.isNotNullOrTransient(getFieldValues().get(i))) {
                terms.add(formatValueAsStringForQuery(i));
            } else {
                trans.add(i);
            }
        }
        if (terms.size() > 0) {
            FieldQueryPart<String> fqp = new FieldQueryPart<>(getFieldName(), terms);
            fqp.setOperator(Operator.OR);
            group.append(fqp);
            if (QueryFieldNames.CREATOR_ROLE_IDENTIFIER.equals(getFieldName())) {
                FieldQueryPart<String> projectChildren = new FieldQueryPart<>(QueryFieldNames.IR_CREATOR_ROLE_IDENTIFIER, terms);
                projectChildren.setOperator(Operator.OR);
                group.append(projectChildren);
            }
        }
        return group.generateQueryString();
    }

    @Override
    protected String formatValueAsStringForQuery(int index) {
        Creator c = getFieldValues().get(index);
        ResourceCreatorRole r = roles.get(index);
        logger.trace("{} {} ", c, r);
        if (r == null) {
            return PhraseFormatter.WILDCARD.format(ResourceCreator
                    .getCreatorRoleIdentifier(c, r));
        }
        return ResourceCreator.getCreatorRoleIdentifier(c, r);
    };

    public List<ResourceCreatorRole> getRoles() {
        return roles;
    }

    public void setRoles(List<ResourceCreatorRole> roles) {
        this.roles = roles;
    }

    @Override
    public String getDescription(TextProvider provider) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < getFieldValues().size(); i++) {
            Creator creator = getFieldValues().get(i);
            ResourceCreatorRole role = getRoles().get(i);
            if (creator != null && !creator.hasNoPersistableValues()) {
                if (names.length() > 0) {
                    names.append(" " + getOperator().name().toLowerCase())
                            .append(" ");
                }
                names.append(creator.getProperName());
                if (role != null) {
                    names.append(" (").append(role.getLabel()).append(")");
                }
            }
        }
        List<String> vals = new ArrayList<>();
        vals.add(names.toString());
        return provider.getText("creatorQueryPart.with_creators", vals);
    }

    @Override
    public String getDescriptionHtml(TextProvider provider) {
        return StringEscapeUtils.escapeHtml4(getDescription(provider));
    }

}
