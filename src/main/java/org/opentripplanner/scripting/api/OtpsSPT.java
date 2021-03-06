package org.opentripplanner.scripting.api;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * A shortest-path-tree, the result of a plan request on a router.
 * 
 * Example of use (python script):
 * <pre>
 *   router = otp.getRouter()
 *   req = otp.createRequest()
 *   req.setDateTime(...)
 *   spt = router.plan(req)
 * </pre>
 * 
 * @author laurent
 */
public class OtpsSPT {

    private ShortestPathTree spt;

    private SampleFactory sampleFactory;

    protected OtpsSPT(ShortestPathTree spt, SampleFactory sampleFactory) {
        this.spt = spt;
        this.sampleFactory = sampleFactory;
    }

    /**
     * Evaluate the SPT at a given point.
     * 
     * @param lat
     * @param lon
     * @return
     */
    public OtpsEvaluatedIndividual eval(double lat, double lon) {
        return eval(new OtpsIndividual(lat, lon, null, null));
    }

    /**
     * Evaluate the SPT for a single individual.
     * 
     * @param individual
     * @return The evualuated value, or NULL if no evaluation can be done (out of range, non snappable).
     */
    public OtpsEvaluatedIndividual eval(OtpsIndividual individual) {
        return individual.eval(spt, sampleFactory);
    }

    /**
     * Evaluate the SPT for a whole population.
     * 
     * @param population
     * @return The list of evualuated values; can be smaller than the population itself as
     *         non-evaluated values will not be returned in the list.
     */
    public List<OtpsEvaluatedIndividual> eval(Iterable<OtpsIndividual> population) {
        List<OtpsEvaluatedIndividual> retval = new ArrayList<>(); // Size?
        for (OtpsIndividual individual : population) {
            OtpsEvaluatedIndividual evaluated = eval(individual);
            if (evaluated != null)
                retval.add(evaluated);
        }
        return retval;
    }

    /**
     * Get the exact location of the origin of the search (from or to place, depending on the
     * arriveBy option).
     * 
     * @return
     */
    public OtpsLatLon getSnappedOrigin() {
        return new OtpsLatLon(spt.getOptions().rctx.origin.getLat(),
                spt.getOptions().rctx.origin.getLon());
    }
}
