package org.opentripplanner.updater.vehicle_sharing;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.lucene.util.SloppyMath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.RentCarAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.routing.core.TraverseMode.WALK;

public class SharedCarUpdater extends PollingGraphUpdater {
    SimpleStreetSplitter simpleStreetSplitter;
    GraphUpdaterManager graphUpdaterManager;
    VehiclePositionsGetter vehiclePositionsGetter;
    TraverseMode traverseMode;
    Graph graph;
    List<Vertex> rememberedVehicles = new LinkedList<>();

    public SharedCarUpdater(VehiclePositionsGetter vehiclePositionsGetter, TraverseMode traverseMode) {
        this.vehiclePositionsGetter = vehiclePositionsGetter;
        this.traverseMode = traverseMode;
    }

    private Vertex chooseCloser(float lat, float lon, Vertex vertex1, Vertex vertex2) {
        if (SloppyMath.haversin(lat, lon, vertex1.getLat(), vertex1.getLon()) <
                SloppyMath.haversin(lat, lon, vertex2.getLat(), vertex2.getLon())) {
            return vertex1;
        } else {
            return vertex2;
        }
    }

    Vertex findClosestVertex(VehiclePosition vehiclePosition, List<Vertex> vertexesToChoseFrom) {
        Stream<Vertex> stream;
        if (vertexesToChoseFrom == null) {
            Envelope envelope = new Envelope(new Coordinate(vehiclePosition.latitude, vehiclePosition.longitude));

            stream = simpleStreetSplitter.getIdx().query(envelope).stream().filter(streetEdge -> streetEdge instanceof StreetEdge).
                    map(edge -> (StreetEdge) edge).
                    filter(streetEdge -> streetEdge.canTraverse(new TraverseModeSet(WALK))).
                    map(Edge::getFromVertex);
        } else {
            stream = vertexesToChoseFrom.stream();
        }

        Vertex v0 = (Vertex)graph.getVertices().toArray()[0];

        Vertex closestVertex = stream.
                reduce(v0,
                        (previous_best, current) -> chooseCloser(vehiclePosition.latitude, vehiclePosition.longitude, previous_best, current));

        return closestVertex;
    }

    private List<Vertex> coordsToVertex(List<VehiclePosition> vehiclePositions, boolean useRememberedVehicles) {
        if (useRememberedVehicles)
            return vehiclePositions.stream().map(a -> findClosestVertex(a, rememberedVehicles)).collect(Collectors.toList());
        else
            return vehiclePositions.stream().map(a -> findClosestVertex(a, null)).collect(Collectors.toList());

    }

    public List<Edge> prepareAppearedEdge(VehiclePositionsDiff vehiclePositionsDiff) {
        List<Vertex> appearedVertex = coordsToVertex(vehiclePositionsDiff.appeared, false);
        List<Edge> appearedEdge = new LinkedList<>();
        for (Vertex vertex : appearedVertex) {
            rememberedVehicles.add(vertex);
            for (Edge edge : vertex.getOutgoing()) {
                if (edge instanceof RentCarAnywhereEdge) {
                    appearedEdge.add(edge);
                }
            }
        }
        return appearedEdge;
    }

    public List<Edge> prepareDisappearedEdge(VehiclePositionsDiff vehiclePositionsDiff) {
        List<Vertex> disappearedVertex = coordsToVertex(vehiclePositionsDiff.disappeared, true);
        List<Edge> disappeareedEdge = new LinkedList<>();
        for (Vertex vertex : disappearedVertex) {
            rememberedVehicles.remove(vertex);
            for (Edge edge : vertex.getOutgoing()) {
                if (edge instanceof RentCarAnywhereEdge) {
                    disappeareedEdge.add(edge);
                    break;
                }
            }
        }
        return disappeareedEdge;
    }

    @Override
    protected void runPolling() throws Exception {
        VehiclePositionsDiff vehiclePositionsDiff = vehiclePositionsGetter.getVehiclePositionsDiff();

        List<Edge> apppearedEdge = prepareAppearedEdge(vehiclePositionsDiff);
        List<Edge> disappeareedEdge = prepareDisappearedEdge(vehiclePositionsDiff);

        VehicleSharingGraphWriterRunnable graphWriterRunnable = new VehicleSharingGraphWriterRunnable(apppearedEdge, disappeareedEdge);

        graphUpdaterManager.execute(graphWriterRunnable);
    }
//
    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        this.graph = graph;
        this.simpleStreetSplitter = new SimpleStreetSplitter(graph);

    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.graphUpdaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) throws Exception {
        this.graph = graph;
        this.simpleStreetSplitter = new SimpleStreetSplitter(graph);

    }

    @Override
    public void teardown() {

    }
}
