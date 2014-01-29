package org.tdar.core.bean.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Index;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.tdar.core.bean.BulkImportField;
import org.tdar.core.bean.HasResource;
import org.tdar.core.bean.Obfuscatable;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.entity.Creator.CreatorType;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.configuration.JSONTransient;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.utils.MessageHelper;

/**
 * $Id$
 * 
 * This is the class to build the relationships between creators and resources. These relationships include a role, which may depend
 * on the resource type and creator type.
 * 
 * @author <a href='mailto:allen.lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */
@Entity
@Table(name = "resource_creator")
@org.hibernate.annotations.Table( appliesTo ="resource_creator", indexes = {
        @Index(name="creator_sequence", columnNames={"resource_id", "sequence_number", "creator_id"}),
        @Index(name = "creatorid", columnNames = {"creator_id"}),
        @Index(name = "rescreator_resid", columnNames = {"resource_id"})
})
public class ResourceCreator extends Persistable.Sequence<ResourceCreator> implements HasResource<Resource>,Obfuscatable {

    private static final long serialVersionUID = 7641781600023145104L;

    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE })
    @IndexedEmbedded
    @JoinColumn(nullable = false, name = "creator_id")
    @NotNull
    @BulkImportField(implementedSubclasses = { Person.class, Institution.class }, label = "Resource Creator", order = 1)
    private Creator creator;

    @Enumerated(EnumType.STRING)
    @Field
    @BulkImportField(label = "Resource Creator Role", comment = BulkImportField.CREATOR_ROLE_DESCRIPTION, order = 200)
    @Column(length = 255)
    private ResourceCreatorRole role;


    private transient Boolean obfuscatedObjectDifferent = false;

    private transient boolean obfuscated;

    public ResourceCreator(Creator creator, ResourceCreatorRole role) {
        setCreator(creator);
        setRole(role);
    }

    public ResourceCreator() {
    }

    @XmlElementRef
    public Creator getCreator() {
        return creator;
    }

    public void setCreator(Creator creator) {
        this.creator = creator;
    }

    @XmlAttribute
    public ResourceCreatorRole getRole() {
        return role;
    }

    public void setRole(ResourceCreatorRole role) {
        this.role = role;
    }

    @Transient
    public CreatorType getCreatorType() {
        if (getCreator() != null) {
            return getCreator().getCreatorType();
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", creator, role);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tdar.core.bean.Validatable#isValid()
     */
    @Override
    public boolean isValid() {
        if (role == null || creator == null) {
            logger.trace(String.format("role:%s creator:%s ", role, creator));
            return false;
        }
        return true;
    }

    public boolean isValidForResource(Resource resource) {
        try {
            boolean relevant = getRole().isRelevantFor(getCreatorType(), resource.getResourceType());
            if (!relevant) {
                Object[] tmp = { getRole(), resource, resource.getResourceType() };
                logger.debug("role {} is not relevant for resourceType {} for {}", tmp);
            }
            return relevant;
        } catch (Exception e) {
            logger.debug("an error occurred when trying to validate a ResourceCreator", e);
        }
        return false;
    }

    @Override
    public boolean isValidForController() {
        return true;
    }

    @Transient
    public final String getCreatorRoleIdentifier() {
        return getCreatorRoleIdentifier(this.getCreator(), this.getRole());
    }

    @Transient
    public static final String getCreatorRoleIdentifier(Creator creatorToFormat, ResourceCreatorRole creatorRole) {
        String toReturn = "";
        if (creatorToFormat != null && creatorToFormat.getCreatorType() != null) {
            String code = creatorToFormat.getCreatorType().getCode();
            String role = "";
            if (creatorRole != null) {
                role = creatorRole.name();
            }
            if (isNullOrTransient(creatorToFormat)) {
                throw new TdarRecoverableRuntimeException(MessageHelper.getMessage("resourceCreator.undefined_creator_id"));
            }
            toReturn = String.format("%s_%s_%s", code, creatorToFormat.getId(), role).toLowerCase();
        }
        return toReturn;
    }

    @Override
    @XmlTransient
    @JSONTransient
    public boolean isObfuscated() {
        return obfuscated;
    }

    @Override
    public List<Obfuscatable> obfuscate() {
        List<Obfuscatable> toObfuscate = new ArrayList<>();
        toObfuscate.add(getCreator());
        setObfuscated(true);
        return toObfuscate;
    }

    @Override
    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    @Override
    @XmlTransient
    @JSONTransient
    public Boolean getObfuscatedObjectDifferent() {
        return obfuscatedObjectDifferent;
    }

    @Override
    public void setObfuscatedObjectDifferent(Boolean value) {
        this.obfuscatedObjectDifferent = value;
    }
}
