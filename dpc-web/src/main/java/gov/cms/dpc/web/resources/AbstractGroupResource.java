package gov.cms.dpc.web.resources;


import gov.cms.dpc.fhir.annotations.FHIR;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@FHIR
@Path("/Group")
public abstract class AbstractGroupResource {

    protected AbstractGroupResource() {
        // Not used
    }

    @Path("/{providerID}/$export")
    @GET
    public abstract Response export(@PathParam("providerID") String groupID);
}
