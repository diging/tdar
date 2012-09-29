package org.tdar.core.bean.coverage;

import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.ContainedIn;
import org.tdar.core.bean.HasResource;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.exception.TdarRuntimeException;
import org.tdar.index.LatLongClassBridge;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * $Id$
 * 
 * Encapsulates min/max lat-long pairs representing the approximate spatial
 * coverage of a Resource.
 * 
 * @author <a href='Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */

@Entity
@Table(name = "latitude_longitude")
@XStreamAlias("latitudeLongitude")
@ClassBridge(impl = LatLongClassBridge.class)
public class LatitudeLongitudeBox extends Persistable.Base implements HasResource<Resource> {

    private static final String PSQL_POLYGON = "POLYGON((%1$s %2$s,%3$s %2$s,%3$s %4$s,%1$s %4$s,%1$s %2$s))";

    private static final String PSQL_MULTIPOLYGON_DATELINE = "MULTIPOLYGON(((%1$s %2$s,%1$s %3$s, -180 %3$s, -180 %2$s,%1$s %2$s)), ((180 %3$s, %4$s %3$s,%4$s %2$s,180 %2$s,180 %3$s)))";
    private static final long serialVersionUID = 2605563277326422859L;

    public static final double MAX_LATITUDE = 90d;
    public static final double MIN_LATITUDE = -90d;

    public static final double MAX_LONGITUDE = 180d;
    public static final double MIN_LONGITUDE = -180d;
    public static final int LATITUDE = 1;
    public static final int LONGITUDE = 2;

    public static final double ONE_MILE_IN_DEGREE_MINUTES = 0.01472d;

    @ManyToOne(optional = false)
    @ContainedIn
    private Resource resource;

    // ranges from -90 (South) to +90 (North)
    @Column(nullable = false, name = "minimum_latitude")
    private Double minimumLatitude;

    @Column(nullable = false, name = "maximum_latitude")
    private Double maximumLatitude;

    // ranges from -180 (West) to +180 (East)
    @Column(nullable = false, name = "minimum_longitude")
    private Double minimumLongitude;

    @Column(nullable = false, name = "maximum_longitude")
    private Double maximumLongitude;

    public Double getMinimumLatitude() {
        return minimumLatitude;
    }

    /*
     * This randomize function is used when displaying lat/longs on a map. It is
     * passed the max and the min lat or long and then uses a salt to randomize.
     * The salt here is approx 1 miles. If the distance between num1 and num2 is
     * less than 1 miles, then this should expand the box by 1 miles + some random
     * 1 mile quantity.
     * 
     * If larger than the "salt" then don't do anything for that "side"
     * 
     * NOTE: the box should always be bigger than the original.
     * 
     * http://www.movable-type.co.uk/scripts/latlong.html
     */

    public static Double obfuscate(Double num, Double num2, int type) {
        Random r = new Random();
        double salt = ONE_MILE_IN_DEGREE_MINUTES;
        double add = 0;

        if (Math.abs(num.doubleValue() - num2.doubleValue()) < salt) {
            add += salt / 2;
        } else {
            return num;
        }

        if (num < num2) { // -5 < -3
            add *= -1;
            salt *= -1;
        }
        // -5 - .05 - .02
        double ret = num.doubleValue() + add + salt * r.nextDouble();
        if (type == LONGITUDE) {
            if (ret > MAX_LONGITUDE)
                ret -= 360;
            if (ret < MIN_LONGITUDE)
                ret += 360;
        }

        // NOTE: Ideally, this should do something different, but in reality, how
        // many
        // archaeological sites are really going to be in this area???
        if (type == LATITUDE) {
            if (Math.abs(ret) > MAX_LATITUDE)
                ret = MAX_LATITUDE;
        }

        return new Double(ret);
    }

    public Double getMinObfuscatedLatitude() {
        return obfuscate(minimumLatitude, maximumLatitude, LATITUDE);
    }

    public Double getMaxObfuscatedLatitude() {
        return obfuscate(maximumLatitude, minimumLatitude, LATITUDE);
    }

    public Double getMinObfuscatedLongitude() {
        return obfuscate(minimumLongitude, maximumLongitude, LONGITUDE);
    }

    public Double getMaxObfuscatedLongitude() {
        return obfuscate(maximumLongitude, minimumLongitude, LONGITUDE);
    }

    public void setMinimumLatitude(Double minimumLatitude) {
        if (isValidLatitude(minimumLatitude)) {
            this.minimumLatitude = minimumLatitude;
        } else {
            throw new TdarRuntimeException("specified latitude is not a valid latitude");
        }
    }

    public Double getMaximumLatitude() {
        return maximumLatitude;
    }

    public void setMaximumLatitude(Double maximumLatitude) {
        if (isValidLatitude(maximumLatitude)) {
            this.maximumLatitude = maximumLatitude;
        } else {
            throw new TdarRuntimeException("specified latitude is not a valid latitude");
        }
    }

    public Double getMinimumLongitude() {
        return minimumLongitude;
    }

    public void setMinimumLongitude(Double minimumLongitude) {
        if (isValidLongitude(minimumLongitude)) {
            this.minimumLongitude = minimumLongitude;
        } else {
            throw new TdarRuntimeException("specified latitude is not a valid longitude");
        }
    }

    public Double getMaximumLongitude() {
        return maximumLongitude;
    }

    public void setMaximumLongitude(Double maximumLongitude) {
        if (isValidLongitude(maximumLongitude)) {
            this.maximumLongitude = maximumLongitude;
        } else {
            throw new TdarRuntimeException("specified latitude is not a valid longitude");
        }
    }

    @Transient
    public boolean isValidLatitude(Double latitude) {
        // FIXME: verify that this works for extreme cases (+/- 180)
        return latitude != null && latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE;
    }

    @Transient
    public boolean isValidLongitude(Double longitude) {
        // FIXME: verify that this works at extreme case (+/- 90)
        return longitude != null && longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
    }

    @XmlTransient
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public boolean isValid() {
        return maximumLatitude != null && minimumLatitude != null && maximumLongitude != null && minimumLongitude != null;
    }

    public String toString() {
        if (isValid()) {
            return String.format("Latitude [%s to %s], Longitude [%s to %s]", minimumLatitude, maximumLatitude, minimumLongitude, maximumLongitude);
        }
        return super.toString();
    }

    public void copyValuesFrom(LatitudeLongitudeBox otherBox) {
        setMinimumLatitude(otherBox.minimumLatitude);
        setMaximumLatitude(otherBox.maximumLatitude);
        setMinimumLongitude(otherBox.minimumLongitude);
        setMaximumLongitude(otherBox.maximumLongitude);
    }

    public String convertToPolygonBox() {
        // if we've got something that goes over the dateline, then we need to split
        // into a multipolygon instead of a standard one. The multipolygon is two polygons
        // each one being on either side of the dateline
        if (crossesDateline()) {
            return String.format(PSQL_MULTIPOLYGON_DATELINE, getMinObfuscatedLongitude(), getMinObfuscatedLatitude(),
                    getMaxObfuscatedLatitude(), getMaxObfuscatedLongitude()).toString();
        }
        return String.format(PSQL_POLYGON, getMaxObfuscatedLongitude(), getMaxObfuscatedLatitude(),
                getMinObfuscatedLongitude(), getMinObfuscatedLatitude()).toString();
    }

    public double getArea() {
        return Math.abs((getMaxObfuscatedLatitude() - getMinObfuscatedLatitude()) * (getMaxObfuscatedLongitude() - getMinimumLongitude()));
    }

    /**
     * @return
     */
    private boolean crossesDateline() {
        return (getMinObfuscatedLongitude() < -100f && getMaxObfuscatedLongitude() > 100f);
    }

}
