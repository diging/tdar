package org.tdar.core.bean.resource.sensory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang.StringUtils;
import org.tdar.core.bean.HasResource;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.resource.SensoryData;

@Entity
@Table(name = "sensory_data_image")
public class SensoryDataImage extends Persistable.Sequence<SensoryDataImage> implements HasResource<SensoryData> {
    private static final long serialVersionUID = -9115746507586171584L;

    @Column(nullable = false)
    private String filename;

    @Column
    private String description;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @XmlTransient
    public boolean isValid() {
        return StringUtils.isNotBlank(filename);
    }

    @Override
    @XmlTransient
    public boolean isValidForController() {
        return true;
    }
}
