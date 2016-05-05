package org.tdar.core.bean.keyword;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;
import org.tdar.core.bean.AbstractPersistable;
import org.tdar.core.bean.FieldLength;
import org.tdar.core.bean.RelationType;
import org.tdar.core.bean.Validatable;

@Entity
@Table(name = "keyword_mapping")
public class ExternalKeywordMapping extends AbstractPersistable implements Validatable {

    private static final long serialVersionUID = 7035836586397546286L;

    public ExternalKeywordMapping() {
    }

    public ExternalKeywordMapping(String relationUrl, RelationType type) {
        this.relation = relationUrl;
        this.relationType = type;
    }

    
    @Column(name = "relation", nullable=false)
    @Length(max = FieldLength.FIELD_LENGTH_2048)
    private String relation;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable=false)
    private RelationType relationType;

    public RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    @Override
    public String toString() {
        return String.format("%s %s (%s)", relation, relationType, getId());
    }

    @Override
    public boolean isValidForController() {
        if (StringUtils.isBlank(relation) || relationType == null) {
        return false;
        }
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
