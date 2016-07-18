package org.tdar.search.converter;

import org.apache.solr.common.SolrInputDocument;
import org.tdar.core.bean.collection.CollectionType;
import org.tdar.core.bean.collection.HierarchicalCollection;
import org.tdar.core.bean.collection.ListCollection;
import org.tdar.core.bean.collection.SharedCollection;
import org.tdar.core.bean.collection.VisibleCollection;
import org.tdar.core.bean.resource.Status;
import org.tdar.search.index.LookupSource;
import org.tdar.search.query.QueryFieldNames;

public class CollectionDocumentConverter extends AbstractSolrDocumentConverter {

    public static SolrInputDocument convert(VisibleCollection collection) {

        SolrInputDocument doc = convertPersistable(collection);
        VisibleCollection props = (VisibleCollection) collection;
        doc.setField(QueryFieldNames.NAME, props.getName());
        doc.setField(QueryFieldNames.NAME_SORT, props.getTitleSort());
        doc.setField(QueryFieldNames.COLLECTION_HIDDEN, ((VisibleCollection) collection).isHidden());
        doc.setField(QueryFieldNames.DESCRIPTION, props.getDescription());
        doc.setField(QueryFieldNames.ALL, props.getAllFieldSearch());
        doc.setField(QueryFieldNames.SUBMITTER_ID, collection.getOwner().getId());
        doc.setField(QueryFieldNames.RESOURCE_IDS, collection.getResourceIds());
        if (collection instanceof HierarchicalCollection) {
            HierarchicalCollection hier = (HierarchicalCollection)collection;
            doc.setField(QueryFieldNames.COLLECTION_PARENT, hier.getParentId());
            doc.setField(QueryFieldNames.COLLECTION_PARENT_LIST, hier.getParentIds());
            doc.setField(QueryFieldNames.TOP_LEVEL, hier.isTopLevel());
        } else {
            doc.setField(QueryFieldNames.TOP_LEVEL, false);
        }
        if (collection instanceof SharedCollection) {
            SharedCollection shared = (SharedCollection)collection;
            CollectionRightsExtractor extractor = new CollectionRightsExtractor(shared);
            doc.setField(QueryFieldNames.RESOURCE_USERS_WHO_CAN_MODIFY, extractor.getUsersWhoCanModify());
            doc.setField(QueryFieldNames.COLLECTION_USERS_WHO_CAN_ADMINISTER, extractor.getUsersWhoCanAdminister());
            doc.setField(QueryFieldNames.COLLECTION_USERS_WHO_CAN_VIEW, extractor.getUsersWhoCanView());
            doc.setField(QueryFieldNames.COLLECTION_TYPE, CollectionType.SHARED.name());
        }
        if (collection instanceof ListCollection) {
            doc.setField(QueryFieldNames.COLLECTION_TYPE, CollectionType.LIST.name());
        }
        doc.setField(QueryFieldNames.STATUS, Status.ACTIVE);
        
        doc.setField(QueryFieldNames.OBJECT_TYPE, LookupSource.COLLECTION.name());
        return doc;
    }
    
    
}
