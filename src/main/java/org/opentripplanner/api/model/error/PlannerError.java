package org.opentripplanner.api.model.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.LocationNotAccessible;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This API response element represents an error in trip planning.
 */
@Getter
public class PlannerError {

    private static final Logger LOG = LoggerFactory.getLogger(PlannerError.class);
    private static Map<Class<? extends Exception>, Message> messages;

    static {
        messages = new HashMap<Class<? extends Exception>, Message>();
        messages.put(VertexNotFoundException.class, Message.OUTSIDE_BOUNDS);
        messages.put(PathNotFoundException.class, Message.PATH_NOT_FOUND);
        messages.put(LocationNotAccessible.class, Message.LOCATION_NOT_ACCESSIBLE);
        messages.put(TransitTimesException.class, Message.NO_TRANSIT_TIMES);
        messages.put(TrivialPathException.class, Message.TOO_CLOSE);
        messages.put(GraphNotFoundException.class, Message.GRAPH_UNAVAILABLE);
        messages.put(IllegalArgumentException.class, Message.BOGUS_PARAMETER);
    }

    private int id;
    private String msg;
    private Message message;

    /**
     * the list of point names which cannot be found (from, to, intermediate.n)
     */
    private List<String> missing = null;

    /**
     * whether no path has been found
     */
    private boolean noPath = false;

    /**
     * An error where no path has been found, but no points are missing
     */
    public PlannerError() {
        noPath = true;
    }

    public PlannerError(Exception e) {
        this();
        message = messages.get(e.getClass());
        if (message == null) {
            LOG.error("exception planning trip: ", e);
            message = Message.SYSTEM_ERROR;
        }
        this.setMsg(message);
        if (e instanceof VertexNotFoundException)
            this.setMissing(((VertexNotFoundException) e).getMissing());
    }

    public void setMsg(Message msg) {
        this.msg = msg.get();
        this.id = msg.getId();
    }

    /**
     * @param missing the list of point names which cannot be found (from, to, intermediate.n)
     */
    public void setMissing(List<String> missing) {
        this.missing = missing;
    }

    /**
     * @param noPath whether no path has been found
     */
    public void setNoPath(boolean noPath) {
        this.noPath = noPath;
    }

    public static boolean isPlanningError(Class<?> clazz) {
        return messages.containsKey(clazz);
    }
}
