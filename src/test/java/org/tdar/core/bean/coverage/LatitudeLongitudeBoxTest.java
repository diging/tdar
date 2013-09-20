package org.tdar.core.bean.coverage;

import static org.junit.Assert.*;

import org.junit.Test;

public class LatitudeLongitudeBoxTest {

    /**
     * Should always be true.
     */
    @SuppressWarnings("static-method")
    @Test
    public void isActuallyObfuscatedByDefault() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(Double.valueOf(0), Double.valueOf(0), Double.valueOf(0), Double.valueOf(0));
        assertTrue(llb.isActuallyObfuscated());
    }

    /**
     * This might seem like a silly test, but we do need to know that the obfuscation is stable from one call to the next.
     * This test does not ensure that that the obfuscation is stable when the lat long box is persisted and restored, though
     */
    @SuppressWarnings("static-method")
    @Test
    public void obfuscationIsFixed() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(0.0, 0.0, 0.0, 0.0);
        Double maxObfuscatedLatitude = llb.getMaxObfuscatedLatitude();
        Double minObfuscatedLatitude = llb.getMinObfuscatedLatitude();
        Double maxObfuscatedLongitude = llb.getMaxObfuscatedLongitude();
        Double minObfuscatedLongitude = llb.getMinObfuscatedLongitude();
        assertTrue(maxObfuscatedLatitude.equals(llb.getMaxObfuscatedLatitude()));
        assertTrue(minObfuscatedLatitude.equals(llb.getMinObfuscatedLatitude()));
        assertTrue(maxObfuscatedLongitude.equals(llb.getMaxObfuscatedLongitude()));
        assertTrue(minObfuscatedLongitude.equals(llb.getMinObfuscatedLongitude()));
    }

    @SuppressWarnings("static-method")
    @Test
    public void doesNotObfuscateAboveOneMile() {
        Double slightlyMoreThanOneMile = LatitudeLongitudeBox.ONE_MILE_IN_DEGREE_MINUTES + 0.00001d;
        Double zero = 0.0;
        assertTrue(slightlyMoreThanOneMile.equals(LatitudeLongitudeBox.obfuscate(slightlyMoreThanOneMile, zero, LatitudeLongitudeBox.LATITUDE)));
        assertTrue(slightlyMoreThanOneMile.equals(LatitudeLongitudeBox.obfuscate(slightlyMoreThanOneMile, zero, LatitudeLongitudeBox.LONGITUDE)));
        assertTrue(zero.equals(LatitudeLongitudeBox.obfuscate(zero, slightlyMoreThanOneMile, LatitudeLongitudeBox.LATITUDE)));
        assertTrue(zero.equals(LatitudeLongitudeBox.obfuscate(zero, slightlyMoreThanOneMile, LatitudeLongitudeBox.LONGITUDE)));
    }

    @SuppressWarnings("static-method")
    @Test
    public void doesObfuscateAtLessThanOneMile() {
        Double slightlyLessThanOneMile = LatitudeLongitudeBox.ONE_MILE_IN_DEGREE_MINUTES - 0.00001d;
        Double zero = 0.0;
        assertFalse(slightlyLessThanOneMile.equals(LatitudeLongitudeBox.obfuscate(slightlyLessThanOneMile, zero, LatitudeLongitudeBox.LATITUDE)));
        assertFalse(slightlyLessThanOneMile.equals(LatitudeLongitudeBox.obfuscate(slightlyLessThanOneMile, zero, LatitudeLongitudeBox.LONGITUDE)));
        assertFalse(zero.equals(LatitudeLongitudeBox.obfuscate(zero, slightlyLessThanOneMile, LatitudeLongitudeBox.LATITUDE)));
        assertFalse(zero.equals(LatitudeLongitudeBox.obfuscate(zero, slightlyLessThanOneMile, LatitudeLongitudeBox.LONGITUDE)));
    }

    /**
     * and if an original lat long coordinate is changed, the obfuscation for it should be redone (surely).
     */
    @SuppressWarnings("static-method")
    @Test
    public void doesReObfuscateOnNewCoordinates() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(0.0, 0.0, 0.0, 0.0);
        Double maxObfuscatedLatitude = llb.getMaxObfuscatedLatitude();
        Double minObfuscatedLatitude = llb.getMinObfuscatedLatitude();
        Double maxObfuscatedLongitude = llb.getMaxObfuscatedLongitude();
        Double minObfuscatedLongitude = llb.getMinObfuscatedLongitude();
        llb.setMaximumLatitude(0.1);
        llb.setMinimumLatitude(0.1);
        llb.setMaximumLongitude(0.1);
        llb.setMinimumLongitude(0.1);
        assertFalse(maxObfuscatedLatitude.equals(llb.getMaxObfuscatedLatitude()));
        assertFalse(minObfuscatedLatitude.equals(llb.getMinObfuscatedLatitude()));
        assertFalse(maxObfuscatedLongitude.equals(llb.getMaxObfuscatedLongitude()));
        assertFalse(minObfuscatedLongitude.equals(llb.getMinObfuscatedLongitude()));
    }
    
    /**
     * and if an original xy coordinate is changed, the obfuscation for it should be redone (surely).
     */
    @SuppressWarnings("static-method")
    @Test
    public void doesReObfuscateOnNewMaxXandYs() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(0.0, 0.0, 0.0, 0.0);
        Double maxObfuscatedLatitude = llb.getMaxObfuscatedLatitude();
        Double minObfuscatedLatitude = llb.getMinObfuscatedLatitude();
        Double maxObfuscatedLongitude = llb.getMaxObfuscatedLongitude();
        Double minObfuscatedLongitude = llb.getMinObfuscatedLongitude();
        llb.setMaxx(0.1);
        llb.setMinx(0.1);
        llb.setMaxy(0.1);
        llb.setMiny(0.1);
        assertFalse(maxObfuscatedLatitude.equals(llb.getMaxObfuscatedLatitude()));
        assertFalse(minObfuscatedLatitude.equals(llb.getMinObfuscatedLatitude()));
        assertFalse(maxObfuscatedLongitude.equals(llb.getMaxObfuscatedLongitude()));
        assertFalse(minObfuscatedLongitude.equals(llb.getMinObfuscatedLongitude()));
    }

    @SuppressWarnings({ "static-method", "deprecation" })
    @Test
    public void doesObfuscateAccordingToService() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(0.0, 0.0, 0.0, 0.0);
        Double maxObfuscatedLatitude = llb.getMaxObfuscatedLatitude();
        Double minObfuscatedLatitude = llb.getMinObfuscatedLatitude();
        Double maxObfuscatedLongitude = llb.getMaxObfuscatedLongitude();
        Double minObfuscatedLongitude = llb.getMinObfuscatedLongitude();
        assertFalse(llb.isObfuscated());
        llb.obfuscate();
        assertTrue(llb.isObfuscated());
        assertTrue(maxObfuscatedLatitude.equals(llb.getMaximumLatitude()));
        assertTrue(minObfuscatedLatitude.equals(llb.getMinimumLatitude()));
        assertTrue(maxObfuscatedLongitude.equals(llb.getMaximumLongitude()));
        assertTrue(minObfuscatedLongitude.equals(llb.getMinimumLongitude()));
        assertFalse(llb.isActuallyObfuscated());
    }

    @SuppressWarnings({ "static-method"})
    @Test
    public void doesReturnCenterIfBoxGreaterThanOneMile() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(0.0, 0.0, LatitudeLongitudeBox.ONE_MILE_IN_DEGREE_MINUTES + 0.00002,
                LatitudeLongitudeBox.ONE_MILE_IN_DEGREE_MINUTES + 0.00002);
        assertTrue(Double.valueOf(0.00737).equals(llb.getCenterLatitudeIfNotObfuscated()));
        assertTrue(Double.valueOf(0.00737).equals(llb.getCenterLongitudeIfNotObfuscated()));
    }
    
    @SuppressWarnings({ "static-method"})
    @Test
    public void doesNotReturnCenterIfLessThanOneMile() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(0.0, 0.0, LatitudeLongitudeBox.ONE_MILE_IN_DEGREE_MINUTES - 0.00001,
                LatitudeLongitudeBox.ONE_MILE_IN_DEGREE_MINUTES + 0.00001);
        assertTrue(null == llb.getCenterLatitudeIfNotObfuscated());
        assertTrue(null == llb.getCenterLongitudeIfNotObfuscated());
    }
    
    @SuppressWarnings("static-method")
    @Test
    public void doesObfuscatedCenterIfBoxLessThanOneMile() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(0.0, 0.0, 0.0, 0.0);
        assertFalse(Double.valueOf(0.0).equals(llb.getCenterLatitude()));
        assertFalse(Double.valueOf(0.0).equals(llb.getCenterLongitude()));
    }

    @SuppressWarnings({ "static-method"})
    @Test
    public void doesObfuscateCenterIfBoxGreaterThanOneMile() {
        LatitudeLongitudeBox llb = new LatitudeLongitudeBox(0.0, 0.0, LatitudeLongitudeBox.ONE_MILE_IN_DEGREE_MINUTES + 0.00002,
                LatitudeLongitudeBox.ONE_MILE_IN_DEGREE_MINUTES + 0.00002);
        assertTrue(Double.valueOf(0.00737).equals(llb.getCenterLatitude()));
        assertTrue(Double.valueOf(0.00737).equals(llb.getCenterLongitude()));
    }

}
