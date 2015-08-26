package org.tdar.utils.jaxb.converters;

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.ReflectionService;
import org.tdar.utils.PersistableUtils;

/**
 * This class is meant to help with serialization of ResourceCollections it provides a method for handling different cases separately:
 *  * Internal Resource Collections
 *  * Un-persisted Resource Collections
 *  * Persisted Resource Collections
 *  
 *  Persisted resource collections can be given a "reference" instead of a full XML representation.
 *   
 * @author abrin
 *
 */
@Component
public class JaxbResourceCollectionRefConverter extends javax.xml.bind.annotation.adapters.XmlAdapter<Persistable, ResourceCollection> {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    GenericService genericService;

    @Autowired
    ReflectionService reflectionService;

    public Persistable marshal(ResourceCollection d) throws Exception {
        if (d == null) {
            return null;
        }
        ResourceCollection rc = d;
        if (HibernateProxy.class.isAssignableFrom(d.getClass())) {
            rc = (ResourceCollection) ((HibernateProxy) d).getHibernateLazyInitializer().getImplementation();
        }
        if (rc.isInternal() || PersistableUtils.isTransient(rc)) {
            return rc;
        }
        return new JAXBPersistableRef(rc.getId(), d.getClass());
    }

    @Override
    public ResourceCollection unmarshal(Persistable ref_) throws Exception {
        if (ref_ instanceof ResourceCollection) {
            return (ResourceCollection)ref_;
        }
        
        if (ref_ == null || !(ref_ instanceof JAXBPersistableRef)) {
            return null;
        }
        JAXBPersistableRef ref = (JAXBPersistableRef)ref_;
        Class<Persistable> cls = reflectionService.getMatchingClassForSimpleName(ref.getType());
        ResourceCollection rc = (ResourceCollection) genericService.find(cls, ref.getId());
        ResourceCollection rc_ = new ResourceCollection();
        rc_.setType(rc.getType());
        rc_.setId(ref.getId());
        rc = null;
        return rc_;
    }


}
