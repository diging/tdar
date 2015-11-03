package org.tdar.utils.json;

import java.io.Serializable;

import org.tdar.core.bean.coverage.LatitudeLongitudeBox;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.service.RssService.GeoRssMode;

public class LatitudeLongitudeBoxWrapper implements Serializable {

    private static final long serialVersionUID = 9175448938321937035L;

    private double minLatitude;
    private double maxLatitude;
    private double minLongitude;
    private double maxLongitude;
    private double centerLatitude;
    private double centerLongitude;
    private GeoRssMode mode = GeoRssMode.ENVELOPE; 
    private Resource resource;

    private boolean spatial;

    public LatitudeLongitudeBoxWrapper(Resource resource) {
        if (resource != null) {
            this.resource = resource;
            LatitudeLongitudeBox llb = resource.getFirstActiveLatitudeLongitudeBox();
            if (llb != null) {
                setSpatial(true);
                this.minLatitude = llb.getMinObfuscatedLatitude();
                this.minLongitude = llb.getMinObfuscatedLongitude();
                this.maxLatitude = llb.getMaxObfuscatedLatitude();
                this.maxLongitude = llb.getMaxObfuscatedLongitude();
                this.centerLatitude = llb.getCenterLatitude();
                this.centerLongitude = llb.getCenterLongitude();
            }
        }
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public void setMinLatitude(double minLatitude) {
        this.minLatitude = minLatitude;
    }

    public double getMaxLatitude() {
        return maxLatitude;
    }

    public void setMaxLatitude(double maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public void setMinLongitude(double minLongitude) {
        this.minLongitude = minLongitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }

    public void setMaxLongitude(double maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public GeoRssMode getMode() {
        return mode;
    }

    public void setMode(GeoRssMode mode) {
        this.mode = mode;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public boolean isSpatial() {
        return spatial;
    }

    public void setSpatial(boolean spatial) {
        this.spatial = spatial;
    }
    
}
