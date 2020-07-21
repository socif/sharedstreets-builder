package io.sharedstreets.tools.builder.osm.tools;

import org.apache.commons.net.util.Base64;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polyline;

import io.sharedstreets.tools.builder.model.WaySection;
import io.sharedstreets.tools.builder.osm.model.NodePosition;

public class GeometryTools {

    public static String toAscii(byte[] bytes) throws Exception {
        StringBuilder sb = new StringBuilder();
        String encodeBase64String = Base64.encodeBase64String(bytes);
        // remove extra \n
        String[] lines = encodeBase64String.split("\\r\\n");
        for (String s : lines) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static byte[] fromAscii(String ascii) throws Exception {
        return Base64.decodeBase64(ascii);
    }

    public static Polyline polylineFromAscii(String ascii) throws Exception {
        return (Polyline) GeometryEngine.geometryFromEsriShape(fromAscii(ascii), Geometry.Type.Polyline);
    }

    public static Geometry constructGeometryTool(WaySection[] waySections) {

        Polyline line = new Polyline();

        boolean firstPosition = true;

        long lastNodeId = -1;
        for (WaySection section : waySections) {
            for (NodePosition node : section.nodes) {
                if (firstPosition == true) {
                    line.startPath(node.lon, node.lat);
                    firstPosition = false;
                } else {
                    // don't write duplicate nodes twice for adjoining way sections
                    if (lastNodeId != node.nodeId)
                        line.lineTo(node.lon, node.lat);
                }

                lastNodeId = node.nodeId;
            }
        }

        return line;
    }
}