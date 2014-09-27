package org.tdar.struts.action.resource;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.Audio;


@Component
@Scope("prototype")
@ParentPackage("default")
@Namespace("/image")
public class AudioViewAction extends AbstractResourceViewAction<Audio> {

    /**
     * 
     */
    private static final long serialVersionUID = -59400140841882295L;

}
