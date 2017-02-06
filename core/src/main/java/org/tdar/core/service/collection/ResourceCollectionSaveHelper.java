package org.tdar.core.service.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.VisibleCollection;
import org.tdar.utils.PersistableUtils;

public class ResourceCollectionSaveHelper<C extends ResourceCollection> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());
    private Set<C> toDelete = new HashSet<>();
    private Set<C> toAdd = new HashSet<>();

    @SuppressWarnings("unchecked")
    public ResourceCollectionSaveHelper(Collection<C> incoming, Collection<? extends ResourceCollection> existing_, Class<C> cls) {

        
        Set<C> existing = new HashSet<>();
        Iterator<? extends ResourceCollection> iterator = existing_.iterator();
        while (iterator.hasNext()) {
            ResourceCollection c = iterator.next();
            if (c.getClass().isAssignableFrom(cls)) {
                existing.add((C)c);
            }
        }

        Map<Long, C> idMap = PersistableUtils.createIdMap(existing);
        for (C in : incoming) {
            if (in == null) {
                continue;
            }
            
            //NOTE: this may be aggressive for what we need, but it prevents useless collections from being created
            // if (in.isTransient() && CollectionUtils.isEmpty(in.getAuthorizedUsers()) && in instanceof VisibleCollection && StringUtils.isBlank(((VisibleCollection)in).getName())) {
            //     logger.debug("skipping transient/empty/null collection: {}", in);
            //     continue;
            // }

            if (!idMap.containsKey(in.getId())) {
                getToAdd().add(in);
            } else {
                idMap.remove(in.getId());
            }
        }
        for (Long id : idMap.keySet()) {
            getToDelete().add(idMap.get(id));
        }
    }

    public Set<C> getToAdd() {
        return toAdd;
    }

    public void setToAdd(Set<C> toAdd) {
        this.toAdd = toAdd;
    }

    public Set<C> getToDelete() {
        return toDelete;
    }

    public void setToDelete(Set<C> toDelete) {
        this.toDelete = toDelete;
    }
}
