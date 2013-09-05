package org.tdar.struts.action.resource;

import java.util.Collections;
import java.util.Set;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.Geospatial;
import org.tdar.core.bean.resource.InformationResourceFile.FileAction;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.data.FileProxy;

/**
 * $Id$
 * 
 * <p>
 * Manages requests to create/delete/edit an Image and its associated metadata.
 * </p>
 * 
 * 
 * @author <a href='mailto:Adam.Brin@asu.edu'>Adam Brin</a>
 * @version $Revision$
 */
@ParentPackage("secured")
@Component
@Scope("prototype")
@Namespace("/geospatial")
@Result(name = TdarActionSupport.INPUT, location = "edit.ftl")
public class GeospatialController extends AbstractDatasetController<Geospatial> {


    private static final long serialVersionUID = 6576781526708737335L;

    @Override
    protected String save(Geospatial persistable) throws TdarActionException {
        super.saveBasicResourceMetadata();

        super.saveInformationResourceProperties();
//        getDatasetService().saveOrUpdate(persistable);
        // HACK: implicitly cache fullUsers via call to getProjectAsJson() as workaround for TDAR-1162. This is the software equivalent of turning the radio up
        // to mask weird sounds your engine is making

        handleUploadedFiles();
        boolean fileChanged = false;
        for (FileProxy proxy : getFileProxies()) {
            if (proxy.getAction().equals(FileAction.ADD) || proxy.getAction().equals(FileAction.REPLACE)) {
                fileChanged = true;
            }
        }

        resolvePostSaveAction(getPersistable());
        return SUCCESS;
    }

    @Override
    public Class<Geospatial> getPersistableClass() {
        return Geospatial.class;
    }

    public Geospatial getGeospatial() {
        return getPersistable();
    }

    public void setGeospatial(Geospatial dataset) {
        setPersistable(dataset);
    }
    
    @Override
    public Set<String> getValidFileExtensions() {
        Set<String> extensionsForTypes = analyzer.getExtensionsForTypes(getPersistable().getResourceType(), ResourceType.DATASET, ResourceType.IMAGE);
        //FIXME: these should come from the analyzer
        //Note: aux.xml and shp.xml omitted because we know view layer logic will accept any .xml (so will server, for that matter)
        String[] geoexts  =  {"shp", "shx", "dbf", "sbn", "sbx", "fbn", "fbx", "ain", "aih", "atx", "ixs", "mxs", "prj", "xml", "cpg", "jpw", "tfw", "aux", "aux","ovr","rrd"};
        Collections.addAll(extensionsForTypes, geoexts);

//        extensionsForTypes.add("shx");
//        extensionsForTypes.add("shp.xml");
//        extensionsForTypes.add("sbx");
//        extensionsForTypes.add("xml");
//        extensionsForTypes.add("dbf");
//        extensionsForTypes.add("sbn");
        return extensionsForTypes;
    }

    @Override
    public Geospatial getResource() {
        if (getPersistable() == null)
            setPersistable(createPersistable());
        return getPersistable();
    }


    @Override
    public boolean isMultipleFileUploadEnabled() {
        return true;
    }
    
    public Geospatial getDataset() {
        return getPersistable();
    }

    public void setDataset(Geospatial geospatial) {
        setGeospatial(geospatial);
    }
}
