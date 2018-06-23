package org.tdar.fileprocessing.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;
import org.tdar.db.conversion.converters.AbstractDatabaseConverter;
import org.tdar.db.conversion.converters.ShapeFileDatabaseConverter;
import org.tdar.fileprocessing.tasks.ConvertDatasetTask;
import org.tdar.fileprocessing.tasks.ImageThumbnailTask;
import org.tdar.fileprocessing.tasks.IndexableTextExtractionTask;
import org.tdar.filestore.FileType;

/**
 * $Id$
 * 
 * @author Adam Brin
 * @version $Revision$
 */
@Component
public class GeospatialWorkflow extends BaseWorkflow implements HasDatabaseConverter {

    public GeospatialWorkflow() {

        RequiredOptionalPairs geotiff = new RequiredOptionalPairs(GeospatialWorkflow.class);
        geotiff.getRequired().add("tif");
        geotiff.getOptional().add("tfw");
        RequiredOptionalPairs geojpg = new RequiredOptionalPairs(GeospatialWorkflow.class);
        geojpg.getRequired().add("jpg");
        geojpg.getOptional().add("jfw");
        getRequiredOptionalPairs().add(geojpg);
        getRequiredOptionalPairs().add(geotiff);

        List<RequiredOptionalPairs> shapePairs = new ArrayList<>();
        RequiredOptionalPairs shapefile = new RequiredOptionalPairs(GenericColumnarDataWorkflow.class);
        shapefile.getRequired().addAll(Arrays.asList("shp", "shx", "dbf"));
        shapefile.getOptional().addAll(Arrays.asList("sbn", "sbx", "fbn", "fbx", "ain", "aih", "atx", "ixs", "mxs", "prj", "cbg", "ixs", "rrd"));
        shapePairs.add(shapefile);
        RequiredOptionalPairs layer = new RequiredOptionalPairs();
        layer.getRequired().addAll(Arrays.asList("lyr", "jpg"));
        layer.getOptional().add("mxd");
        shapePairs.add(layer);

        addTask(ImageThumbnailTask.class, WorkflowPhase.CREATE_DERIVATIVE);
        addTask(ConvertDatasetTask.class, WorkflowPhase.CREATE_DERIVATIVE);
        addTask(IndexableTextExtractionTask.class, WorkflowPhase.CREATE_DERIVATIVE);
    }

    @Override
    public FileType getInformationResourceFileType() {
        return FileType.GEOSPATIAL;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Class<? extends AbstractDatabaseConverter> getDatabaaseConverterForExtension(String ext) {
        switch (ext) {
            case "shp":
                return ShapeFileDatabaseConverter.class;
        }
        return null;

    }
}
