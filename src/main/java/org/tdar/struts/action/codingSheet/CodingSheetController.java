package org.tdar.struts.action.codingSheet;

import java.io.IOException;
import java.util.Set;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.FileProxy;
import org.tdar.core.bean.resource.CodingSheet;
import org.tdar.core.bean.resource.Ontology;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.service.resource.CodingSheetService;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.action.resource.AbstractSupportingInformationResourceController;
import org.tdar.utils.PersistableUtils;

/**
 * $Id$
 * 
 * <p>
 * Manages requests to create/delete/edit an CodingSheet and its associated metadata.
 * </p>
 * 
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@ParentPackage("secured")
@Component
@Scope("prototype")
@Namespace("/coding-sheet")
public class CodingSheetController extends AbstractSupportingInformationResourceController<CodingSheet> {

    private static final long serialVersionUID = 377533801938016848L;

    @Autowired
    private transient CodingSheetService codingSheetService;

    private Ontology ontology;

    @Override
    protected void loadCustomMetadata() throws TdarActionException {
        super.loadCustomMetadata();
        setOntology(getCodingSheet().getDefaultOntology());
    };

    /**
     * Save basic metadata of the registering concept.
     * 
     * @param concept
     * @throws TdarActionException
     */
    @Override
    protected String save(CodingSheet codingSheet) throws TdarActionException {
        if (!PersistableUtils.isNullOrTransient(ontology)) {
            // load the full hibernate entity and set it back on the incoming column
            ontology = getGenericService().find(Ontology.class, ontology.getId());
        }

        codingSheetService.reconcileOntologyReferencesOnRulesAndDataTableColumns(codingSheet, ontology);

        super.saveBasicResourceMetadata();
        super.saveInformationResourceProperties();
        super.saveCategories();

        // getGenericService().saveOrUpdate(codingSheet);
        handleUploadedFiles();
        return SUCCESS;
    }

    @Override
    protected FileProxy createUploadedFileProxy(String fileTextInput) throws IOException {
        String filename = getPersistable().getTitle() + ".csv";
        // ensure csv conversion
        return new FileProxy(filename, FileProxy.createTempFileFromString(fileTextInput), VersionType.UPLOADED);
    }

    /**
     * Get the current concept.
     * 
     * @return
     */
    public CodingSheet getCodingSheet() {
        return getPersistable();
    }

    public void setCodingSheet(CodingSheet codingSheet) {
        this.setPersistable(codingSheet);
    }

    @Override
    public Set<String> getValidFileExtensions() {
        return getAnalyzer().getExtensionsForType(ResourceType.CODING_SHEET);
    }

    @Override
    public Class<CodingSheet> getPersistableClass() {
        return CodingSheet.class;
    }

    public Ontology getOntology() {
        return ontology;
    }

    public void setOntology(Ontology ontology) {
        this.ontology = ontology;
    }

}
