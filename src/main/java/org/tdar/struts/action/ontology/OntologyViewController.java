package org.tdar.struts.action.ontology;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.OntologyNode;
import org.tdar.core.exception.StatusCode;
import org.tdar.core.service.resource.CodingSheetService;
import org.tdar.core.service.resource.OntologyNodeService;
import org.tdar.core.service.resource.OntologyService;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.interceptor.annotation.HttpOnlyIfUnauthenticated;

@Component
@Scope("prototype")
@ParentPackage("default")
@Namespace("/ontology")
@Results(value = {
        @Result(name = OntologyViewController.REDIRECT_IRI, type = TdarActionSupport.REDIRECT, location = "${redirectIri}",params = { "ignoreParams", "id,slug" })
})
public class OntologyViewController extends AbstractOntologyViewAction {

    public static final String REDIRECT_IRI = "redirect_iri";
    private static final long serialVersionUID = -826507251116794622L;
    @Autowired
    private transient OntologyNodeService ontologyNodeService;
    @Autowired
    private transient OntologyService ontologyService;
    @Autowired
    private transient CodingSheetService codingSheetService;

    @Override
    public void prepare() throws TdarActionException {
        super.prepare();
        // prepare calls handleSlug
        // handle slug will check if there's an IRI that's in a slug (fallback from older behavior)
        getLogger().trace("{} -- {}", getNode(), getRedirectIri());
        if (getRedirectIri() != null && getNode() == null) {
            abort(StatusCode.NOT_FOUND, getText("ontologyController.node_not_found", Arrays.asList(getIri())));
        }
    }

    @Override
    @HttpOnlyIfUnauthenticated
    @Actions(value = {
            @Action(value = "{id}/{slug}"),
            @Action(value = "{id}/"),
            @Action(value = "{id}")
    })
    public String view() throws TdarActionException {
        getLogger().trace("redirect iri: {}", getRedirectIri());
        // redirect if we don't have an IRI and have a bad slug
        if (isRedirectBadSlug() && StringUtils.isBlank(getRedirectIri())) {
            return BAD_SLUG;
        }

        // redirect if we have a IRI
        if (getRedirectIri() != null) {
            return REDIRECT_IRI;
        }
        return super.view();
    }

    /**
     * History -- the following URLs need to be managed:
     * http://localhost:8080/ontology/3656 --> redirect to slug (or just work)
     * http://localhost:8080/ontology/3656/taxon --> just work
     * http://localhost:8080/ontology/3656/Kangaroo_Rat_or_Dipodomys_sp
     * http://localhost:8080/ontology/3656/node/Kangaroo_Rat_or_Dipodomys_sp
     */
    @Override
    protected void handleSlug() {
        // if slug != actual
        if (!Objects.equals(getSlug(), getPersistable().getSlug())) {
            getLogger().trace("processing slug request: {}", getSlug());
            // if completely blank, then redirect the slug
            if (StringUtils.isBlank(getSlug())) {
                getLogger().trace("passing upward: {}", getSlug());
                super.handleSlug();
                return;
            }
            
            // otherwise, try to see if the slug is an IRI
            String normalizeIri = OntologyNode.normalizeIri(getSlug());
            getLogger().trace("iri:{} --> {}", getIri(), normalizeIri);
            OntologyNode node_ = getOntology().getNodeByIri(normalizeIri);
            // handle struts differences /\\'\\,\\./ ...
            if (node_ == null) {
                node_ = fallbackCheckForIri(normalizeIri);
            }

            // redirect if we have an actual node match to the new home
            if (node_ != null) {
                setIri(node_.getIri());
                getLogger().trace("redirecting by iri: {}", node_.getIri());
                setNode(node_);
                setRedirectIri(String.format("/ontology/%s/node/%s", getId(), node_.getIri()));
                return;
            }
        }
        // handle slug
        super.handleSlug();
    }
}
