package org.tdar.core.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.file.TdarFile;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * provide a summary by user of what's changed since a given dayO
 * 
 * @author abrin
 *
 */
@JsonAutoDetect
public class RecentFileSummary implements Serializable {

    private static final long serialVersionUID = -375150144359217945L;
    Integer created = 0;
    Integer resource = 0;
    Integer curated = 0;
    Integer initialReviewed = 0;
    Integer reviewed = 0;
    Integer externalReviewed = 0;
    private List<FileStatsProxy> files = new ArrayList<>();
    private TdarUser person;
    private Date startDate;
    private Map<Long, Map<String, Object>> userMap = new HashMap<>();
    private Date endDate;

    @Override
    public String toString() {
        return String.format("(%s <--> %s) created: %s, resource: %s, curated:%s, initial: %s, reviewed:%s, external:%s", startDate, endDate, created, resource, curated, initialReviewed, reviewed, externalReviewed);
    }
    
    public RecentFileSummary(List<TdarFile> files, Date startDate, Date endDate, TdarUser person) {

        this.setEndDate(endDate);
        this.setStartDate(startDate);
        this.setPerson(person);
        for (TdarFile f : files) {
            addToUserMap(f);
            incrementCounters(person, f);
        }
    }

    private void incrementCounters(TdarUser person, TdarFile f) {
        FileStatsProxy fsp = new FileStatsProxy(f);
        files.add(fsp);
        if (between(f.getDateCreated()) && checkPerson(person, f.getUploader())) {
            fsp.setCreated(true);
            created++;
        }
        if (between(f.getDateResourceCreated()) && checkPerson(person, f.getResource().getSubmitter())) {
            fsp.setResource(true);
            resource++;
        }
        if (between(f.getDateCurated()) && checkPerson(person, f.getCuratedBy())) {
            fsp.setCurated(true);
            curated++;
        }
        if (between(f.getDateInitialReviewed()) && checkPerson(person, f.getInitialReviewedBy())) {
            fsp.setInitialReviewed(true);
            initialReviewed++;
        }
        if (between(f.getDateReviewed()) && checkPerson(person, f.getReviewedBy())) {
            fsp.setReviewed(true);
            reviewed++;
        }
        if (between(f.getDateExternalReviewed()) && checkPerson(person, f.getExternalReviewedBy())) {
            fsp.setExternalReviewed(true);
            externalReviewed++;
        }
    }

    private void addToUserMap(TdarFile f) {
        if (f.getUploader() != null) {
            addToUserMap(f.getUploader().getId(), f.getUploaderName());
        }
        if (f.getReviewedBy() != null) {
            addToUserMap(f.getReviewedBy().getId(), f.getUploaderName());
        }
        if (f.getResourceCreatedBy() != null) {
            addToUserMap(f.getResourceCreatedBy().getId(), f.getResourceCreatedByName());
        }

        if (f.getCuratedBy() != null) {
            addToUserMap(f.getCuratedBy().getId(), f.getCuratedByName());
        }

        if (f.getInitialReviewedBy() != null) {
            addToUserMap(f.getInitialReviewedBy().getId(), f.getInitialReviewedByName());
        }

        if (f.getExternalReviewedBy() != null) {
            addToUserMap(f.getExternalReviewedBy().getId(), f.getExternalReviewedByName());
        }
    }

    private void addToUserMap(Long id, String name) {
        if (getUserMap().containsKey(id)) {
            return;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        getUserMap().put(id, map);

    }

    private boolean between(Date date) {
        if (startDate == null) {
            return true;
        }

        if (date == null) {
            return false;
        }

        if (startDate.before(date)) {
            if (endDate != null && (endDate == date || endDate.after(date))) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPerson(TdarUser person, TdarUser externalReviewedBy) {
        if (person == null) {
            return true;
        }
        if (person.equals(externalReviewedBy)) {
            return true;
        }
        // TODO Auto-generated method stub
        return false;
    }

    public Integer getCreated() {
        return created;
    }

    public void setCreated(Integer created) {
        this.created = created;
    }

    public Integer getResource() {
        return resource;
    }

    public void setResource(Integer resource) {
        this.resource = resource;
    }

    public Integer getCurated() {
        return curated;
    }

    public void setCurated(Integer curated) {
        this.curated = curated;
    }

    public Integer getInitialReviewed() {
        return initialReviewed;
    }

    public void setInitialReviewed(Integer initialReviewed) {
        this.initialReviewed = initialReviewed;
    }

    public Integer getReviewed() {
        return reviewed;
    }

    public void setReviewed(Integer reviewed) {
        this.reviewed = reviewed;
    }

    public Integer getExternalReviewed() {
        return externalReviewed;
    }

    public void setExternalReviewed(Integer externalReviewed) {
        this.externalReviewed = externalReviewed;
    }

    public List<FileStatsProxy> getFiles() {
        return files;
    }

    public void setFiles(List<FileStatsProxy> files) {
        this.files = files;
    }

    public TdarUser getPerson() {
        return person;
    }

    public void setPerson(TdarUser person) {
        this.person = person;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Map<Long, Map<String, Object>> getUserMap() {
        return userMap;
    }

    public void setUserMap(Map<Long, Map<String, Object>> userMap) {
        this.userMap = userMap;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

}
