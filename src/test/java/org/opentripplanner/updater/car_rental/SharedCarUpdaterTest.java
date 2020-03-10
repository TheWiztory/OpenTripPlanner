package org.opentripplanner.updater.car_rental;

import junit.framework.TestCase;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.rentedgetype.RentCarAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.updater.vehicle_sharing.SharedCarUpdater;
import org.opentripplanner.updater.vehicle_sharing.VehiclePosition;
import org.opentripplanner.updater.vehicle_sharing.VehiclePositionsDiff;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SharedCarUpdaterTest extends TestCase {
    public void testProjectingVehicles() {
        float long1 = (float) -77.0;
        float long2 = (float) -77.005;
        float long3 = (float) -77.01;
        float lat1 = (float) 38.0;
        float lat2 = (float) 38.005;
        float lat3 = (float) -38.01;

        float vehLong1 = (float) (0.3 * long1 + 0.7 * long2);
        float vehLat1 = (float) (0.3 * lat1 + 0.7 * lat2);

//      We need to initialize graph.
        Graph graph = new Graph();
        StreetVertex v1 = new IntersectionVertex(graph, "v1", long1, lat1, "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", long2, lat2, "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", long3, lat3, "v3");

        @SuppressWarnings("unused")
        Edge walk = new StreetEdge(v1, v2, GeometryUtils.makeLineString(long1, lat1,
                long2, lat2), "e1", 87, StreetTraversalPermission.PEDESTRIAN, false);

        @SuppressWarnings("unused")
        Edge mustCar = new StreetEdge(v2, v3, GeometryUtils.makeLineString(long2, lat2,
                long3, lat3), "e2", 87, StreetTraversalPermission.CAR, false);

        @SuppressWarnings("unused")
        RentCarAnywhereEdge car1 = new RentCarAnywhereEdge(v1, 1, 2);
        RentCarAnywhereEdge car2 = new RentCarAnywhereEdge(v2, 1, 2);
        RentCarAnywhereEdge car3 = new RentCarAnywhereEdge(v3, 1, 2);

        SharedCarUpdater sharedCarUpdater = new SharedCarUpdater(null, TraverseMode.RENT);


        try {
            sharedCarUpdater.setup(graph);
        } catch (Exception e) {
            assertTrue(true);
        }

//        One vehicle appeared.
        List<VehiclePosition> appeared = new ArrayList<VehiclePosition>() {
            {
                add(new VehiclePosition(vehLong1, vehLat1));
            }
        };

        List<VehiclePosition> disappeared = new ArrayList<VehiclePosition>() {
            {

            }
        };

        VehiclePositionsDiff vehiclePositionsDiff = new VehiclePositionsDiff(appeared, disappeared, 0L, 0L);

        List<Edge> disappearedEdges = sharedCarUpdater.prepareDisappearedEdge(vehiclePositionsDiff);
        List<Edge> appearedEdges = sharedCarUpdater.prepareAppearedEdge(vehiclePositionsDiff);

        assertEquals(1, appearedEdges.size());
        assertEquals(0, disappearedEdges.size());
        assertEquals(car1, appearedEdges.get(0));


        vehiclePositionsDiff = new VehiclePositionsDiff(disappeared, appeared, 0L, 0L);

        disappearedEdges = sharedCarUpdater.prepareDisappearedEdge(vehiclePositionsDiff);
        appearedEdges = sharedCarUpdater.prepareAppearedEdge(vehiclePositionsDiff);

        assertEquals(0, appearedEdges.size());
        assertEquals(1, disappearedEdges.size());
        assertEquals(car1, disappearedEdges.get(0));

    }
}
