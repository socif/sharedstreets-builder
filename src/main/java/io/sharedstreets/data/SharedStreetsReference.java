package io.sharedstreets.data;


import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import io.sharedstreets.tools.builder.osm.model.Way;
import io.sharedstreets.tools.builder.model.BaseSegment;
import io.sharedstreets.tools.builder.model.WaySection;
import io.sharedstreets.tools.builder.tiles.TilableData;
import io.sharedstreets.tools.builder.util.UniqueId;
import io.sharedstreets.tools.builder.util.geo.Geography;
//import io.sharedstreets.tools.builder.util.UniqueId;
import io.sharedstreets.tools.builder.util.geo.TileId;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SharedStreetsReference implements TilableData, Serializable {

    public static double MAX_LPR_SEGMENT_LENGTH = 15000.0d; // meters
    public static double LPR_BEARING_OFFSET = 20.0d; // meters



    public enum FORM_OF_WAY {

        Undefined(0),
        Motorway(1),
        MultipleCarriageway(2),
        SingleCarriageway(3),
        Roundabout(4),
        TrafficSquare(5), // Like a roundabout but square? https://giphy.com/gifs/square-addis-meskel-GYb9s3Afw0cWA
        SlipRoad(6),
        Other(7);

        private final int value;

        FORM_OF_WAY(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }
    };

    public UniqueId id;
    public FORM_OF_WAY formOfWay;
    public SharedStreetsLocationReference[] locationReferences;

    public boolean backReference;

    public SharedStreetsGeometry geometry;

    private final static Geography GeoOp = new Geography();

    @Override
    public TileId getTileKey() {
        if(locationReferences == null || locationReferences.length < 2) {
            return null;
        }

        return TileId.lonLatToTileId(locationReferences[0].point.getX(), locationReferences[0].point.getY());
    }

    public static List<SharedStreetsReference> getSharedStreetsReferences(BaseSegment segment) {

        // generate single shared geometry for all references
        SharedStreetsGeometry geometry = new SharedStreetsGeometry(segment);

        if(geometry.id.toString().equals("Pg3HPufXqVFyzoXUbwkrEP"))
            System.out.print("Pg3HPufXqVFyzoXUbwkrEP");

        if(geometry.id.toString().equals("RQTdXHWuBPkji7BvNX2vi9"))
            System.out.print("RQTdXHWuBPkji7BvNX2vi9");

        FORM_OF_WAY formOfWay = SharedStreetsReference.getFormOfWay(segment);

        List<SharedStreetsReference> list = new ArrayList<>();

        SharedStreetsReference reference1 = new SharedStreetsReference();

        reference1.formOfWay = formOfWay;
        reference1.backReference = false;

        List<SharedStreetsLocationReference> lprList = SharedStreetsReference.getLocationReferences(geometry, false);
        reference1.locationReferences = lprList.toArray(new SharedStreetsLocationReference[lprList.size()]);
        reference1.id = SharedStreetsReference.generateId(reference1);

        geometry.forwardReferenceId = reference1.id;

        if(reference1.id.toString().equals("HXpwm5JYrHNaPEEE2fNrhA"))
            System.out.print("HXpwm5JYrHNaPEEE2fNrhA");

        if(reference1.id.toString().equals("HhE2Ed9ocwv6ZgiMEgjfdd"))
            System.out.print("HhE2Ed9ocwv6ZgiMEgjfdd");

        reference1.geometry = geometry;
        list.add(reference1);

        // if not one-way generate reverse segments
        if(!segment.oneWay) {

            SharedStreetsReference reference2 = new SharedStreetsReference();
            reference2.formOfWay = formOfWay;
            reference1.backReference = true;

            lprList = SharedStreetsReference.getLocationReferences(geometry, true);
            reference2.locationReferences = lprList.toArray(new SharedStreetsLocationReference[lprList.size()]);

            reference2.id = SharedStreetsReference.generateId(reference2);

            geometry.backReferenceId = reference2.id;

            if(reference2.id.toString().equals("HXpwm5JYrHNaPEEE2fNrhA"))
                System.out.print("HXpwm5JYrHNaPEEE2fNrhA");

            if(reference2.id.toString().equals("HhE2Ed9ocwv6ZgiMEgjfdd"))
                System.out.print("HhE2Ed9ocwv6ZgiMEgjfdd");

            reference2.geometry = geometry;

            list.add(reference2);
        }

        return list;
    }

    public static List<SharedStreetsLocationReference> getLocationReferences(SharedStreetsGeometry geometry, boolean reverse) {

        List<SharedStreetsLocationReference> referenceList = new ArrayList<>();

        Polyline path;
        if(reverse) {
            // make a copy so we can reverse
            path = (Polyline)geometry.geometry.copy();
            path.reverseAllPaths();
        }
        else
            path = (Polyline)geometry.geometry;

        double length = GeoOp.length(path);

        int lprCount;

        // segments longer than 15km get intermediary LPRs
        if(length > MAX_LPR_SEGMENT_LENGTH) {
            // add one new LPR for every 15km in length, but split evenly over path
            // at 16km path has LPRs at 0,8,16 -- 35km path has 0,11.6,22.3,35
            lprCount =  (int)Math.ceil(length / MAX_LPR_SEGMENT_LENGTH);
        }
        else
            lprCount = 2;

        double lprSegmentLength = length / (lprCount - 1);

        for(int i = 0; i < lprCount; i++){

            SharedStreetsLocationReference lpr = new SharedStreetsLocationReference();

            lpr.sequence = i + 1;

            double fraction = 0.0d;

            if(i > 0.0d)
                fraction = (lprCount - 1) / i;

            lpr.point = GeoOp.interpolate(path, fraction);

            // last ref doesn't have bearing or dist to next ref
            if(i + 1 < lprCount) {
                lpr.distanceToNextRef = lprSegmentLength;

                Point bearingPoint;

                // for segments shorter than LPR_BEARING_OFFSET just the bearing of the entire segment
                if(length > LPR_BEARING_OFFSET) {
                    // get point 20m further along line
                    double bearingPointOffset = LPR_BEARING_OFFSET / length;
                    bearingPoint = GeoOp.interpolate(path, (fraction + bearingPointOffset));
                }
                else
                    bearingPoint = GeoOp.interpolate(path, 1.0);

                // gets the bearing for the
                lpr.bearing = GeoOp.azimuth(lpr.point, bearingPoint, 1.0);
            }

            referenceList.add(lpr);
        }

        return referenceList;
    }

    public static FORM_OF_WAY getFormOfWay(BaseSegment segment){

        // links roads (turing channel / ramps) are FoW slip roads
        if (segment.link) {
            return FORM_OF_WAY.SlipRoad;
        }
        else if(segment.roundabout) {
            return FORM_OF_WAY.Roundabout;
        }
        else {
            // find class for all way sections
            Way.ROAD_CLASS roadClass = null;
            for(WaySection section : segment.waySections) {
                if(roadClass == null)
                    roadClass = section.roadClass;
                else if(roadClass != section.roadClass) {
                    // if section isn't the same as previous section return FORM_OF_WAY.Undefined
                    return FORM_OF_WAY.Undefined;
                }
            }

            if(roadClass == Way.ROAD_CLASS.ClassMotorway)
                return FORM_OF_WAY.Motorway;
            else if((roadClass == Way.ROAD_CLASS.ClassPrimary || roadClass == Way.ROAD_CLASS.ClassTrunk)
                    && segment.oneWay) {
                // if primary or trunk road and one way assume (?) multiple carriageway
                return FORM_OF_WAY.MultipleCarriageway;
            }
            else if(roadClass == Way.ROAD_CLASS.ClassTrunk ||
                    roadClass == Way.ROAD_CLASS.ClassPrimary ||
                    roadClass == Way.ROAD_CLASS.ClassSecondary ||
                    roadClass == Way.ROAD_CLASS.ClassTertiary ||
                    roadClass == Way.ROAD_CLASS.ClassResidential ||
                    roadClass == Way.ROAD_CLASS.ClassUnclassified) {
                return FORM_OF_WAY.SingleCarriageway;
            }
            else
                return FORM_OF_WAY.Other;
        }
    }

    // generate a stable ref
    public static UniqueId generateId(SharedStreetsReference ssr) {
        String hashString = new String();

        hashString = "" + ssr.formOfWay.value;

        for(SharedStreetsLocationReference lr : ssr.locationReferences) {
            hashString += String.format(",%.6f %.6f", lr.point.getX(), lr.point.getY());
            if(lr.bearing != null) {
                hashString += String.format(",%.1f", lr.bearing);
                hashString += String.format(",%.2f", lr.distanceToNextRef);
            }
        }

        return UniqueId.generateRandom();
    }

}