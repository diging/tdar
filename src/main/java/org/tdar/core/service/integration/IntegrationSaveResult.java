package org.tdar.core.service.integration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.tdar.utils.json.JsonIntegrationFilter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonView;

@JsonAutoDetect
public class IntegrationSaveResult implements Serializable {

    private static final long serialVersionUID = 1012921456837760027L;
    private Long id;
    private String status;
    private List<String> errors = new ArrayList<>();

    @JsonView(JsonIntegrationFilter.class)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonView(JsonIntegrationFilter.class)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonView(JsonIntegrationFilter.class)
    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

}
