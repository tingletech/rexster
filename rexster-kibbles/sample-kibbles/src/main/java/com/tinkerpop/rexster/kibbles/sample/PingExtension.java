package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple extension that just echoes back the string parameter passed in.
 */
@ExtensionNaming(namespace = AbstractSampleExtension.EXTENSION_NAMESPACE, name = PingExtension.EXTENSION_NAME)
public class PingExtension extends AbstractSampleExtension {

    public static final String EXTENSION_NAME = "ping";

    /**
     * Exposes the ping extension from the root of the graph name accepting a single parameter
     * called "reply" which will be echoed back in the a JSON response.
     * <p/>
     * The "reply" parameter is the only parameter that matters for purpose of the ping extension.
     * The remaining parameters ("context" and "graph") are present only for example purposes.  In
     * a more realistic example, the extension would make use of these types of objects to perform
     * the extensions function.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor(description = "a simple ping extension.")
    public ExtensionResponse evaluatePing(@RexsterContext RexsterResourceContext context,
                                          @RexsterContext Graph graph,
                                          @ExtensionRequestParameter(name = "reply", defaultValue = "pong (default)", description = "a value to reply with") String reply) {
        if (reply == null || reply.isEmpty()) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the reply parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("ping", reply);
        return ExtensionResponse.ok(map);
    }
}
