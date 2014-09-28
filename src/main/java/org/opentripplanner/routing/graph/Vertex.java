/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.graph;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.bind.annotation.XmlTransient;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A vertex in the graph. Each vertex has a longitude/latitude location, as well as a set of
 * incoming and outgoing edges.
 */
public abstract class Vertex implements Serializable, Cloneable {
    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private static final Logger LOG = LoggerFactory.getLogger(Vertex.class);

    private static int maxIndex = 0;

    private int index;
    
    /* short debugging name */
    private final String label;
    
    /* Longer human-readable name for the client */
    private String name;

    private final double x;

    private final double y;
    
    private double distanceToNearestTransitStop = 0;

    private transient Edge[] incoming = new Edge[0];

    private transient Edge[] outgoing = new Edge[0];

    
    /* PUBLIC CONSTRUCTORS */

    public Vertex(Graph g, String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.index = maxIndex  ++;
        // null graph means temporary vertex
        if (g != null)
            g.addVertex(this);
        this.name = "(no name provided)";
    }

    protected Vertex(Graph g, String label, double x, double y, String name) {
        this(g, label, x, y);
        this.name = name;
    }

    /* PUBLIC METHODS */

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(this.getLabel());
        if (this.getCoordinate() != null) {
            sb.append(" lat,lng=").append(this.getCoordinate().y);
            sb.append(",").append(this.getCoordinate().x);
        }
        sb.append(">");
        return sb.toString();
    }

    public int hashCode() {
        return index;
    }

    /* EDGE UTILITY METHODS (use arrays to eliminate copy-on-write set objects) */

    /**
     * A static helper method to avoid repeated code for outgoing and incoming lists.
     * Synchronization must be handled by the caller, to avoid passing edge array pointers that may be invalidated.
     */
    private static Edge[] addEdge(Edge[] existing, Edge e) {
        Edge[] copy = new Edge[existing.length + 1];
        int i;
        for (i = 0; i < existing.length; i++) {
            if (existing[i] == e) {
                LOG.error("repeatedly added edge {}", e);
                return existing;
            }
            copy[i] = existing[i];
        }
        copy[i] = e; // append the new edge to the copy of the existing array
        return copy;
    }

    /**
     * A static helper method to avoid repeated code for outgoing and incoming lists.
     * Synchronization must be handled by the caller, to avoid passing edge array pointers that may be invalidated.
     */
    public static Edge[] removeEdge(Edge[] existing, Edge e) {
        Edge[] copy = new Edge[existing.length - 1];
        for (int i = 0, j = 0; i < existing.length; i++) {
            if (existing[i] == e) continue;
            copy[j++] = existing[i];
            if (j == existing.length) {
                // The array did not get shorter by one -- the edge to remove must not have been present
                LOG.error("Requested removal of an edge which isn't connected to this vertex");
                return existing;
            }
        }
        // Sanity check
        for (Edge edge : copy) {
            if (edge == e) {
                LOG.error("An edge is still present after being removed (there were multiple copies?)");
                return existing;
            }
        }
        return copy;
    }

    /* FIELD ACCESSOR METHODS : READ/WRITE */

    public void addOutgoing(Edge edge) {
        synchronized (this) {
            outgoing = addEdge(outgoing, edge);
        }
    }

    /** @return whether the edge was found and removed. */
    public boolean removeOutgoing(Edge edge) {
        synchronized (this) {
            int n = outgoing.length;
            outgoing = removeEdge(outgoing, edge);
            return (outgoing.length < n);
        }
    }


    public void addIncoming(Edge edge) {
        synchronized (this) {
            incoming = addEdge(incoming, edge);
        }
    }

    /** @return whether the edge was found and removed. */
    public boolean removeIncoming(Edge edge) {
        synchronized (this) {
            int n = incoming.length;
            incoming = removeEdge(incoming, edge);
            return (incoming.length < n);
        }
    }

    /**
     * Get a collection containing all the edges leading from this vertex to other vertices.
     * There is probably some overhead to creating the wrapper ArrayList objects, but this
     * allows filtering and combining edge lists using stock Collection-based methods.
     */
    public Collection<Edge> getOutgoing() {
        return Arrays.asList(outgoing);
    }

    /** Get a collection containing all the edges leading from other vertices to this vertex. */
    public Collection<Edge> getIncoming() {
        return Arrays.asList(incoming);
    }

    @XmlTransient
    public int getDegreeOut() {
        return outgoing.length;
    }

    @XmlTransient
    public int getDegreeIn() {
        return incoming.length;
    }
    
    // TODO: this is a candidate for no-arg message-passing style
    public void setDistanceToNearestTransitStop(double distance) {
        distanceToNearestTransitStop = distance;
    }

    /** Get the distance from this vertex to the closest transit stop in meters. */
    public double getDistanceToNearestTransitStop() {
        return distanceToNearestTransitStop;
    }

    /** Get the longitude of the vertex */
    public double getX() {
        return x;
    }

    /** Get the latitude of the vertex */
    public double getY() {
        return y;
    }

    /** Get the longitude of the vertex */
    public double getLon() {
        return x;
    }

    /** Get the latitude of the vertex */
    public double getLat() {
        return y;
    }

    /** If this vertex is located on only one street, get that street's name. */
    public String getName() {
        return this.name;
    }

    public void setStreetName(String name) {
        this.name = name;
    }


    /* FIELD ACCESSOR METHODS : READ ONLY */

    /** Every vertex has a label which is globally unique. */
    public String getLabel() {
        return label;
    }

    @XmlTransient
    public Coordinate getCoordinate() {
        return new Coordinate(getX(), getY());
    }

    /** Get the bearing, in degrees, between this vertex and another coordinate. */
    public double azimuthTo(Coordinate other) {
        return DirectionUtils.getAzimuth(getCoordinate(), other);
    }

    /** Get the bearing, in degrees, between this vertex and another. */
    public double azimuthTo(Vertex other) {
        return azimuthTo(other.getCoordinate());
    }

    /** Get this vertex's unique index, that can serve as a hashcode or an index into a table */
    @XmlTransient
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public static int getMaxIndex() {
        return maxIndex;
    }


    /* SERIALIZATION METHODS */

    private void writeObject(ObjectOutputStream out) throws IOException {
        // edge lists are transient
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.incoming = new Edge[0];
        this.outgoing = new Edge[0];
        index = maxIndex++;
    }

    /* UTILITY METHODS FOR SEARCHING, GRAPH BUILDING, AND GENERATING WALKSTEPS */

    @XmlTransient
    public List<Edge> getOutgoingStreetEdges() {
        List<Edge> result = new ArrayList<Edge>();
        for (Edge out : this.getOutgoing()) {
            if (!(out instanceof StreetEdge)) {
                continue;
            }
            result.add((StreetEdge) out);
        }
        return result;
    }

    /**
     * Clear this vertex's outgoing and incoming edge lists, and remove all the edges
     * they contained from this vertex's neighbors.
     */
    public void removeAllEdges() {
        for (Edge e : outgoing) {
            Vertex target = e.getToVertex();
            if (target != null) {
                target.removeIncoming(e);
            }
        }
        for (Edge e : incoming) {
            Vertex source = e.getFromVertex();
            if (source != null) {
                source.removeOutgoing(e);
            }
        }
        incoming = new Edge[0];
        outgoing = new Edge[0];
    }


    /* GRAPH COHERENCY AND TYPE CHECKING */

    // Parameterized Class<? extends Edge) gets ugly fast here
    @SuppressWarnings("unchecked")
    private static final ValidEdgeTypes VALID_EDGE_TYPES = new ValidEdgeTypes(Edge.class);

    @XmlTransient
    public ValidEdgeTypes getValidOutgoingEdgeTypes() {
        return VALID_EDGE_TYPES;
    }

    @XmlTransient
    public ValidEdgeTypes getValidIncomingEdgeTypes() {
        return VALID_EDGE_TYPES ;
    }

    /**
     * Check that all of this Vertex's incoming and outgoing edges are of the proper types.
     * This may not be necessary if edge constructor types are strictly specified
     * and addOutgoing is protected
     */
    public boolean edgeTypesValid() {
        ValidEdgeTypes validOutgoingTypes = getValidOutgoingEdgeTypes();
        for (Edge e : getOutgoing())
            if (!validOutgoingTypes.isValid(e))
                return false;
        ValidEdgeTypes validIncomingTypes = getValidIncomingEdgeTypes();
        for (Edge e : getIncoming())
            if (!validIncomingTypes.isValid(e))
                return false;
        return true;
    }

    public static final class ValidEdgeTypes {
        private final Class<? extends Edge>[] classes;
        // varargs constructor:
        // a loophole in the law against arrays/collections of parameterized generics
        public ValidEdgeTypes (Class<? extends Edge>... classes) {
            this.classes = classes;
        }
        public boolean isValid (Edge e) {
            for (Class<? extends Edge> c : classes) {
                if (c.isInstance(e))
                    return true;
            }
            return false;
        }
    }

    /**
     * Clean up before garbage collection. Usually this method does nothing, but temporary vertices
     * must provide a method to remove their associated temporary edges from adjacent vertices'
     * edge lists, usually by simply calling detach() on them.
     * @return the number of edges affected by the cleanup.
     */
    public int removeTemporaryEdges() {
        // do nothing, signal 0 other objects affected
        return 0;
    }
}