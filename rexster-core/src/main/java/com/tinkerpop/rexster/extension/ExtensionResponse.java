package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Wraps the Jersey response object with some simple response builder methods.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ExtensionResponse {

    private final Response jerseyResponse;
    private final boolean errorResponse;

    /**
     * Create a non-error ExtensionResponse object.
     */
    public ExtensionResponse(final Response response) {
        this(response, false);
    }

    /**
     * Create an ExtensionResponse object.
     */
    public ExtensionResponse(final Response response, final boolean errorResponse) {
        this.jerseyResponse = response;
        this.errorResponse = errorResponse;
    }

    /**
     * Override the builder and literally construct the Jersey response.
     * <p/>
     * Rexster will add its standard headers and override any provided in the response.  It is recommended
     * to use the @see error methods as opposed to override if the intention is to return an error on
     * the response.  The override methods will not throw a WebApplicationException or do any standard
     * Rexster server side logging.
     */
    public static ExtensionResponse override(final Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }

        return new ExtensionResponse(response);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     */
    public static ExtensionResponse error(final String message) {
        return error(message, (Exception) null);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     */
    public static ExtensionResponse error(final String message, final String appendKey, final JSONObject appendJson) {
        return error(message, null, appendKey, appendJson);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     *
     * @param appendJson Additional JSON to push into the response. The root of the key values from this object
     *                   will be merged into the root of the resulting JSON.
     */
    public static ExtensionResponse error(final String message, final JSONObject appendJson) {
        return error(message, null, null, appendJson);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     */
    public static ExtensionResponse error(final Exception source) {
        return error("", source);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     */
    public static ExtensionResponse error(final Exception source, final String appendKey, final JSONObject appendJson) {
        return error("", source, appendKey, appendJson);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     *
     * @param appendJson Additional JSON to push into the response. The root of the key values from this object
     *                   will be merged into the root of the resulting JSON.
     */
    public static ExtensionResponse error(final Exception source, final JSONObject appendJson) {
        return error("", source, null, appendJson);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     */
    public static ExtensionResponse error(final String message, final Exception source) {
        return error(message, source, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     *
     * @param appendKey  This parameter is only relevant if the appendJson parameter is passed.  If this value
     *                   is not null or non-empty the value of appendJson will be assigned to this key value in
     *                   the response object.  If the key is null or empty the appendJson parameter will be
     *                   written at the root of the response object.
     * @param appendJson Additional JSON to push into the response.
     */
    public static ExtensionResponse error(final String message, final Exception source, final String appendKey, final JSONObject appendJson) {
        return error(message, source, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), appendKey, appendJson);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     *
     * @param appendJson Additional JSON to push into the response. The root of the key values from this object
     *                   will be merged into the root of the resulting JSON.
     */
    public static ExtensionResponse error(final String message, final Exception source, final JSONObject appendJson) {
        return error(message, source, null, appendJson);
    }

    /**
     * Generates standard Rexster JSON error with a specified server error response code.
     * <p/>
     * The status code is not validated, so throw the right code.
     */
    public static ExtensionResponse error(final String message, final Exception source, final int statusCode) {
        return error(message, source, statusCode, null, null);
    }

    /**
     * Generates standard Rexster JSON error with a specified server error response code.
     * <p/>
     * The status code is not validated, so throw the right code.
     *
     * @param appendKey  This parameter is only relevant if the appendJson parameter is passed.  If this value
     *                   is not null or non-empty the value of appendJson will be assigned to this key value in
     *                   the response object.  If the key is null or empty the appendJson parameter will be
     *                   written at the root of the response object.
     * @param appendJson Additional JSON to push into the response.
     */
    public static ExtensionResponse error(final String message, final Exception source, final int statusCode,
                                          final String appendKey, final JSONObject appendJson) {
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put(Tokens.MESSAGE, message);

        if (source != null) {
            m.put("error", source.getMessage());
        }

        if (appendJson != null) {
            if (appendKey != null && !appendKey.isEmpty()) {
                m.put(appendKey, appendJson);
            } else {
                final Iterator keys = appendJson.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    m.put(key, appendJson.opt(key));
                }
            }
        }

        // use a hashmap with the constructor so that a JSONException
        // will not be thrown
        return new ExtensionResponse(Response.status(statusCode).entity(new JSONObject(m)).build(), true);
    }

    /**
     * Generates a response with no content and matching status code.
     */
    public static ExtensionResponse noContent() {
        return new ExtensionResponse(Response.noContent().build());
    }

    /**
     * Generates a response with an OK status code.  Accepts a HashMap as the response value.
     * It is converted to JSON.
     */
    public static ExtensionResponse ok(final Map result) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }

        return ok(new JSONObject(result));
    }

    /**
     * Generate a response with an OK status code for an Element.  Serializes all properties in the graph Element
     * with GraphSONMode.NORMAL.
     */
    public static ExtensionResponse ok(final Element result) {
        return ok(result, null, GraphSONMode.NORMAL);
    }

    /**
     * Generate a response with an OK status code for an Element.
     */
    public static ExtensionResponse ok(final Element result, final Set<String> propertyKeys, final GraphSONMode mode) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }

        try {
            return ok(GraphSONUtility.jsonFromElement(result, propertyKeys, mode));
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
    }

    /**
     * Generate a response with an OK status code for a iterator of graph Elements.  Serializes all properties in
     * the graph elemetn with GraphSONMode.NORMAL.
     */
    public static ExtensionResponse ok(final Iterator<Element> result) {
        return ok(result, null, GraphSONMode.NORMAL);
    }

    /**
     * Generate a response with an OK status code for a iterator of graph Elements.
     */
    public static ExtensionResponse ok(final Iterator<Element> result,
                                       final Set<String> propertyKeys, final GraphSONMode mode) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }

        final JSONObject json = new JSONObject();
        final JSONArray elementArray = new JSONArray();
        try {
            while(result.hasNext()) {
                elementArray.put(GraphSONUtility.jsonFromElement(result.next(), propertyKeys, mode));
            }

            json.put(Tokens.RESULTS, elementArray);
        } catch (JSONException jsone) {
            throw new RuntimeException(jsone);
        }

        return ok(json);
    }

    public static ExtensionResponse availableOptions(final String... methods) {
        return new ExtensionResponse(Response.noContent()
                .header("Access-Control-Allow-Methods", StringUtils.join(methods, ",")).build());
    }

    /**
     * Generates an response with an OK status code.
     */
    public static ExtensionResponse ok(final JSONObject result) {
        return new ExtensionResponse(Response.ok(result).build());
    }

    public static ExtensionResponse badRequest(final String message, final JSONObject appendJson) {
        return error(message, null,  Response.Status.BAD_REQUEST.getStatusCode(), null, appendJson);
    }

    public static ExtensionResponse badRequest() {
        return new ExtensionResponse(Response.status(Response.Status.BAD_REQUEST).build());
    }

    public Response getJerseyResponse() {
        return this.jerseyResponse;
    }

    public boolean isErrorResponse() {
        return this.errorResponse;
    }
}
