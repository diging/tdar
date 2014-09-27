package org.tdar.struts.action.resource;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.Document;


@Component
@Scope("prototype")
@ParentPackage("default")
@Namespace("/document")
public class DocumentViewAction extends AbstractResourceViewAction<Document> {

    private static final long serialVersionUID = 2384325295193047858L;

}
