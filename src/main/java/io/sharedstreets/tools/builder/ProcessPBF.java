package io.sharedstreets.tools.builder;

import io.sharedstreets.tools.builder.outputs.GeoJSONOutputFormat;
import io.sharedstreets.tools.builder.transforms.Intersections;
import io.sharedstreets.data.osm.OSMDataStream;
import io.sharedstreets.tools.builder.transforms.Segments;
import org.apache.flink.api.java.ExecutionEnvironment;

import org.apache.flink.core.fs.FileSystem;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.net.URL;


public class ProcessPBF {

    static Logger LOG = LoggerFactory.getLogger(ProcessPBF.class);

    public static boolean DEBUG_OUTPUT = true;

    public static void main(String[] args) throws Exception {

        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();

        String inputFile = "data/nyc_extract.pbf";

        OSMDataStream dataStream = new OSMDataStream(inputFile, env);

        // create intersections
        Intersections intersections = new Intersections(dataStream);


        try {
            long count = intersections.intersections.count();
            LOG.info("Intersections count: {}", count);
        } catch (Exception e){
            e.printStackTrace();
        }


        // split ways
        Segments segments = new Segments(dataStream, intersections);

        long wayCount = dataStream.ways.count();

        LOG.info("Way count: {}", wayCount);


        long segmentCount = segments.segements.count();
        LOG.info("BaseSegment count: {}", segmentCount);

        segments.segements.write(new GeoJSONOutputFormat(), "/tmp/nyc_segments.geojson", FileSystem.WriteMode.OVERWRITE).setParallelism(1);

        env.execute();

        System.out.println(env.getExecutionPlan());

    }

}


