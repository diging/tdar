package org.tdar.search.query.part;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.bean.coverage.LatitudeLongitudeBox;
import org.tdar.search.index.TdarIndexNumberFormatter;
import org.tdar.search.query.QueryFieldNames;

import com.opensymphony.xwork2.TextProvider;

/**
 * $Id$
 * 
 * Given bounding box search limits this creates a query statement which
 * searches for any item with an overlapping bounding area. To account for
 * boxes that cross the Prime Meridian we extend the min and max longitude to
 * -540 and 540 respectively and use the computed longitude projections (for
 * objects whose min longitude was recorded as larger than it's max longitude).
 * 
 * 
 * @see {@link LatLongClassBridge}
 * @see {@link TdarIndexNumberFormatter}
 * @author <a href="mailto:matt.cordial@asu.edu">Matt Cordial</a>
 * @version $Rev$
 */
public class SpatialQueryPart extends FieldQueryPart<LatitudeLongitudeBox> {

    public static final int SCALE_RANGE = 2;
    private static final String MIN_LUCENE = TdarIndexNumberFormatter.format(-540);
    private static final String MAX_LUCENE = TdarIndexNumberFormatter.format(540);

    private Operator operator = Operator.AND;

    @Transient
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    /*
     * trying to say either:
     * (a) minX and minY are inside the bouding box OR
     * (b) minX is before and maxX is after (i.e. item completely covers)
     * 
     * NOTE: this is brittle, and only setup on a bounding box that does not cover the dateline.
     * 
     * FIXME: replace with spatial lucene
     */
    private static final String SPATIAL_QUERY_FORMAT = "((" + QueryFieldNames.MINX + ":[%1$s TO %2$s]^1 AND " + QueryFieldNames.MAXX + ":[%1$s TO %2$s]^1) " +
            " OR (" + QueryFieldNames.MINX + ":[" + MIN_LUCENE + " TO %2$s] AND " + QueryFieldNames.MAXX + ":[%1$s TO " + MAX_LUCENE + "])) " +
            "AND ((" + QueryFieldNames.MINY + ":[%3$s TO %4$s]^1 AND " + QueryFieldNames.MAXY + ":[%3$s TO %4$s]^1) " +
            "OR (" + QueryFieldNames.MINY + ":[" + MIN_LUCENE + " TO %4$s] AND " + QueryFieldNames.MAXY + ":[%3$s TO " + MAX_LUCENE + "]))";

    private static final String SPATIAL_QUERY_FORMAT_PRIME = "((((" + QueryFieldNames.MINX + ":[%6$s TO %2$s]) AND (" + QueryFieldNames.MAXX
            + ":[%1$s TO %5$s])) " +
            "OR ((" + QueryFieldNames.MINXPRIME + ":[%6$s TO %2$s]) AND (" + QueryFieldNames.MAXX + ":[%1$s TO %5$s])) " +
            "OR ((" + QueryFieldNames.MINX + ":[%6$s TO %2$s]) AND (" + QueryFieldNames.MAXXPRIME + ":[%1$s TO %5$s]))) " +
            "AND (" + QueryFieldNames.MINY + ":[%6$s TO %4$s]) AND (" + QueryFieldNames.MAXY + ":[%3$s TO %5$s]))";

    public SpatialQueryPart() {

    }

    public SpatialQueryPart(LatitudeLongitudeBox... boxes) {
        add(boxes);
    }

    public SpatialQueryPart(Collection<LatitudeLongitudeBox> boxes) {
        this(boxes.toArray(new LatitudeLongitudeBox[0]));
    }

    /*
     * ((
     * (
     * ((minx:[00999999460 TO 00999999887.966919]) AND (maxx:[00999999887.8872681 TO 10000000540]))
     * OR ((minxPrime:[00999999460 TO 00999999887.966919]) AND (maxx:[00999999887.8872681 TO 10000000540]))
     * OR ((minx:[00999999460 TO 00999999887.966919]) AND (maxxPrime:[00999999887.8872681 TO 10000000540]))
     * )
     * AND (miny:[00999999460 TO 10000000033.46581674573002]) AND (maxy:[10000000033.42571077612917 TO 10000000540])))
     * AND (( status:(draft) ) )
     */

    @Override
    public String generateQueryString() {
        StringBuilder q = new StringBuilder();

        for (LatitudeLongitudeBox spatialLimit : getFieldValues()) {
            if (!spatialLimit.isInitialized()) {
                continue;
            }
            if (q.length() > 0) {
                q.append(" " + operator.name() + " ");
            }

            String format = SPATIAL_QUERY_FORMAT;

            logger.trace("crosses primeMeridian: {}, crosses antiMeridian: {}", spatialLimit.crossesPrimeMeridian(), spatialLimit.crossesDateline());
            // If the search bounds cross the antimeridian, we need to split up the spatial limit into two separate
            // boxes because the degrees change from positive to negative.
            if (spatialLimit.crossesDateline() && !spatialLimit.crossesPrimeMeridian()) {
                format = SPATIAL_QUERY_FORMAT_PRIME;
                q.append(
                        String.format(
                                format,
                                TdarIndexNumberFormatter.format(spatialLimit.getMinObfuscatedLongitude()),
                                TdarIndexNumberFormatter.format(180d),
                                TdarIndexNumberFormatter.format(spatialLimit.getMinObfuscatedLatitude()),
                                TdarIndexNumberFormatter.format(spatialLimit.getMaxObfuscatedLatitude()),
                                MAX_LUCENE,
                                MIN_LUCENE
                                ));
                q.append(" OR ");
                q.append(
                        String.format(
                                format,
                                TdarIndexNumberFormatter.format(-180d),
                                TdarIndexNumberFormatter.format(spatialLimit.getMaxObfuscatedLongitude()),
                                TdarIndexNumberFormatter.format(spatialLimit.getMinObfuscatedLatitude()),
                                TdarIndexNumberFormatter.format(spatialLimit.getMaxObfuscatedLatitude()),
                                MAX_LUCENE,
                                MIN_LUCENE
                                ));

            } else {
                q.append(
                        String.format(
                                format,
                                TdarIndexNumberFormatter.format(spatialLimit.getMinObfuscatedLongitude()),
                                TdarIndexNumberFormatter.format(spatialLimit.getMaxObfuscatedLongitude()),
                                TdarIndexNumberFormatter.format(spatialLimit.getMinObfuscatedLatitude()),
                                TdarIndexNumberFormatter.format(spatialLimit.getMaxObfuscatedLatitude()),
                                MAX_LUCENE,
                                MIN_LUCENE
                                )
                        );
            }
            q.append(String.format(" AND %s:[%s TO %s] ", QueryFieldNames.SCALE, TdarIndexNumberFormatter.MIN_ALLOWED,
                    TdarIndexNumberFormatter.format(spatialLimit.getScale() + SCALE_RANGE)));

        }
        return null;
    }

    @Override
    public String getDescription(TextProvider provider) {
        if (getFieldValues().isEmpty()) {
            return provider.getText("spatialQueryPart.empty_description");
        }

        List<String> latlongs = new ArrayList<String>();
        for (LatitudeLongitudeBox box : getFieldValues()) {
            latlongs.add(box.toString());
        }
        String seperator = " " + operator.name().toLowerCase() + " ";
        List<String> vals = new ArrayList<>();
        vals.add(StringUtils.join(latlongs, seperator));
        String fmt = provider.getText("spatialQueryPart.resource_located", vals);
        return fmt;
    }

    @Override
    public String getDescriptionHtml(TextProvider provider) {
        return StringEscapeUtils.escapeHtml4(getDescription(provider));
    }

    @Override
    public void add(LatitudeLongitudeBox... values) {
        for (LatitudeLongitudeBox box : values) {
            if (box.isInitialized()) {
                super.add(box);
            }
        }
    }

    public String getFilter() {
        if (CollectionUtils.isNotEmpty(getFieldValues())) {
        LatitudeLongitudeBox box = getFieldValues().get(0);
        return String.format("%s:[%s,%s TO %s,%s]", QueryFieldNames.ACTIVE_LATITUDE_LONGITUDE_BOXES, box.getMinObfuscatedLatitude(), box.getMinObfuscatedLongitude(), 
                box.getMaxObfuscatedLatitude(), box.getMaxObfuscatedLongitude());
        } return "";
    }
}
