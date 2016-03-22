package org.tdar.struts.action.resource;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.service.resource.DataTableService;

public abstract class AbstractSupportingResourceViewAction<R extends InformationResource> extends AbstractResourceViewAction<R>{

	private static final long serialVersionUID = -1581233578894577541L;

	@Autowired
    private transient DataTableService dataTableService;


    public void setRelatedResources(ArrayList<Resource> relatedResources) {
        this.relatedResources = relatedResources;
    }

    private ArrayList<Resource> relatedResources;

    public List<Resource> getRelatedResources() {
        if (relatedResources == null) {
            relatedResources = new ArrayList<Resource>();
            for (DataTable table : dataTableService.findDataTablesUsingResource(getPersistable())) {
                if (!table.getDataset().isDeleted()) {
                    relatedResources.add(table.getDataset());
                }
            }
        }
        return relatedResources;
    }

}
