package com.tinkerpop.rexster;

import com.sun.jersey.api.client.ClientResponse;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EdgeResourceIntegrationTest extends AbstractGraphResourceIntegrationTest {
    public EdgeResourceIntegrationTest() throws Exception {
        super();
    }

    @Test
    public void getEdgeDoesNotExistStatusNotFound() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            if (testGraph.getFeatures().supportsEdgeRetrieval) {
                ClientResponse graphResponse = doGraphGet(testGraph, "edges/123doesnotexist");

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.NOT_FOUND, graphResponse.getClientResponseStatus());
            }
        }
    }

    @Test
    public void getEdgeFoundStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            if (testGraph.getFeatures().supportsEdgeRetrieval) {
                String id = testGraph.getEdgeIdSet().values().iterator().next();
                ClientResponse graphResponse = doGraphGet(testGraph, "edges/" + encode(id));

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

                JSONObject edgeJson = graphResponse.getEntity(JSONObject.class);
                Assert.assertNotNull(edgeJson);

                JSONObject results = edgeJson.optJSONObject(Tokens.RESULTS);
                Assert.assertEquals(id, results.optString(Tokens._ID));
            }
        }
    }

    @Test
    public void getEdgesAllFoundStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            if (testGraph.getFeatures().supportsEdgeIteration) {
                ClientResponse graphResponse = doGraphGet(testGraph, "edges");

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

                JSONObject edgeJson = graphResponse.getEntity(JSONObject.class);
                Assert.assertNotNull(edgeJson);

                Assert.assertEquals(6, edgeJson.optJSONArray(Tokens.RESULTS).length());
            }
        }
    }

    @Test
    public void getEdgesPagingStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            if (testGraph.getFeatures().supportsEdgeIteration) {
                ArrayList<String> uniqueIds = new ArrayList<String>();

                // get the first two elements
                ClientResponse graphResponse = doGraphGet(testGraph, "edges", "rexster.offset.start=0&rexster.offset.end=2");

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

                JSONObject edgeJson = graphResponse.getEntity(JSONObject.class);
                Assert.assertNotNull(edgeJson);

                JSONArray results = edgeJson.optJSONArray(Tokens.RESULTS);
                Assert.assertEquals(2, results.length());

                uniqueIds.add(results.optJSONObject(0).optString(Tokens._ID));

                Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
                uniqueIds.add(results.optJSONObject(1).optString(Tokens._ID));

                // get the next two elements
                graphResponse = doGraphGet(testGraph, "edges", "rexster.offset.start=2&rexster.offset.end=4");

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

                edgeJson = graphResponse.getEntity(JSONObject.class);
                Assert.assertNotNull(edgeJson);

                results = edgeJson.optJSONArray(Tokens.RESULTS);
                Assert.assertEquals(2, results.length());

                Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
                uniqueIds.add(results.optJSONObject(0).optString(Tokens._ID));

                Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
                uniqueIds.add(results.optJSONObject(1).optString(Tokens._ID));

                // get the final two elements
                graphResponse = doGraphGet(testGraph, "edges", "rexster.offset.start=4&rexster.offset.end=6");

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

                edgeJson = graphResponse.getEntity(JSONObject.class);
                Assert.assertNotNull(edgeJson);

                results = edgeJson.optJSONArray(Tokens.RESULTS);
                Assert.assertEquals(2, results.length());

                Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
                uniqueIds.add(results.optJSONObject(0).optString(Tokens._ID));

                Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
                uniqueIds.add(results.optJSONObject(1).optString(Tokens._ID));

                // get the final two elements without specifying the end parameter
                graphResponse = doGraphGet(testGraph, "edges", "rexster.offset.start=4");

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

                edgeJson = graphResponse.getEntity(JSONObject.class);
                Assert.assertNotNull(edgeJson);

                results = edgeJson.optJSONArray(Tokens.RESULTS);
                Assert.assertEquals(2, results.length());

                Assert.assertEquals(uniqueIds.get(4), results.optJSONObject(0).optString(Tokens._ID));
                Assert.assertEquals(uniqueIds.get(5), results.optJSONObject(1).optString(Tokens._ID));

                // get the first two elements without specifying the start parameter
                graphResponse = doGraphGet(testGraph, "edges", "rexster.offset.end=2");

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

                edgeJson = graphResponse.getEntity(JSONObject.class);
                Assert.assertNotNull(edgeJson);

                results = edgeJson.optJSONArray(Tokens.RESULTS);
                Assert.assertEquals(2, results.length());

                Assert.assertEquals(uniqueIds.get(0), results.optJSONObject(0).optString(Tokens._ID));
                Assert.assertEquals(uniqueIds.get(1), results.optJSONObject(1).optString(Tokens._ID));
            }
        }
    }

    @Test
    public void postEdgeEdgeExistingWithNoEdgePropertiesStatusConflict() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            // POST edge does a getEdge to determine if the edge was already posted with that ID
            // and to allow new properties to be POSTed to the edge
            if (testGraph.getFeatures().supportsEdgeRetrieval) {
                String id = testGraph.getEdgeIdSet().values().iterator().next();
                ClientResponse response = this.doGraphPost(testGraph, "edges/" + encode(id));

                Assert.assertNotNull(response);
                Assert.assertEquals(ClientResponse.Status.CONFLICT, response.getClientResponseStatus());
            }
        }
    }

    @Test
    public void postEdgeNewEdgeVerticesDoNotExistStatusConflict() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            // POST edge does a getEdge to determine if the edge was already posted with that ID
            // and to allow new properties to be POSTed to the edge
            if (testGraph.getFeatures().supportsEdgeRetrieval) {
                String id = testGraph.getEdgeIdSet().values().iterator().next();
                ClientResponse response = this.doGraphPost(testGraph, "edges/" + encode(id), "_outV=102notreal&_inV=123notreal");

                Assert.assertNotNull(response);
                Assert.assertEquals(ClientResponse.Status.CONFLICT, response.getClientResponseStatus());
            }
        }
    }

    @Test
    public void postEdgeSimpleStatusOk() throws JSONException {
        for (GraphTestHolder testGraph : this.testGraphs) {
            Iterator<String> itty = testGraph.getVertexIdSet().values().iterator();
            String vertexIdIn = itty.next();
            String vertexIdOut = itty.next();

            // post as URI
            String edgeProperty = "propertya=(i,123)&propertyb=(d,321.5)&propertyc=test";
            ClientResponse response = this.doGraphPost(testGraph, "edges", "_outV=" + encode(vertexIdOut) + "&_inV=" + encode(vertexIdIn) + "&_label=uriPost&" + edgeProperty);
            assertPostedEdge(vertexIdIn, vertexIdOut, response, true);

            // post as JSON
            Map<String, Object> jsonEdgeData = new HashMap<String, Object>();
            jsonEdgeData.put(Tokens._OUT_V, vertexIdOut);
            jsonEdgeData.put(Tokens._IN_V, vertexIdIn);
            jsonEdgeData.put(Tokens._LABEL, "jsonPost");
            jsonEdgeData.put("propertya", 123);
            jsonEdgeData.put("propertyb", 321.5);
            jsonEdgeData.put("propertyc", "test");

            JSONObject jsonEdgeToPost = new JSONObject(jsonEdgeData);

            response = this.doGraphPostOfJson(testGraph, "edges", jsonEdgeToPost);
            assertPostedEdge(vertexIdIn, vertexIdOut, response, true);
        }
    }

    @Test
    public void postEdgeComplexStatusOk() throws JSONException {
        for (GraphTestHolder testGraph : this.testGraphs) {
            if (testGraph.getFeatures().supportsMapProperty){
                Iterator<String> itty = testGraph.getVertexIdSet().values().iterator();
                String vertexIdIn = itty.next();
                String vertexIdOut = itty.next();
    
                // post as URI
                String complexValue = "(map,(propertya=(i,123),propertyb=(d,321.5),propertyc=(list,(x,y,z)),propertyd=(map,(x=xyz))))";
                String complexKeyValueUri = "&complex=" + complexValue;
                ClientResponse response = this.doGraphPost(testGraph, "edges", "_outV=" + encode(vertexIdOut) + "&_inV=" + encode(vertexIdIn) + "&_label=uriPost" + complexKeyValueUri);
                assertPostedEdge(vertexIdIn, vertexIdOut, response, false);
    
                // post as JSON
                String complexKeyValueJson = "{\"propertya\":123,\"propertyb\":321.5,\"propertyc\":[\"x\",\"y\",\"z\"],\"propertyd\":{\"x\":\"xyz\"}}";
                JSONTokener tokener = new JSONTokener(complexKeyValueJson);
                JSONObject complexJsonObject = new JSONObject(tokener);
    
                Map<String, Object> jsonEdgeData = new HashMap<String, Object>();
                jsonEdgeData.put(Tokens._OUT_V, vertexIdOut);
                jsonEdgeData.put(Tokens._IN_V, vertexIdIn);
                jsonEdgeData.put(Tokens._LABEL, "jsonPost");
                jsonEdgeData.put("complex", complexJsonObject);
    
                JSONObject jsonEdgeToPost = new JSONObject(jsonEdgeData);
    
                response = this.doGraphPostOfJson(testGraph, "edges", jsonEdgeToPost);
                assertPostedEdge(vertexIdIn, vertexIdOut, response, false);
            }
        }
    }

    @Test
    public void putEdgeStatusNotFound() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            // A PUT has to get the edge before it can update it
            if (testGraph.getFeatures().supportsEdgeRetrieval) {
                String keyValueThatWillNeverUpdate = "&k1=v1";
                ClientResponse response = this.doGraphPut(testGraph, "edges/1000notreal", keyValueThatWillNeverUpdate);

                Assert.assertEquals(ClientResponse.Status.NOT_FOUND, response.getClientResponseStatus());
            }
        }
    }

    @Test
    public void putEdgeSimpleStatusOk() throws JSONException {
        for (GraphTestHolder testGraph : this.testGraphs) {
            // A PUT has to get the edge before it can update it
            if (testGraph.getFeatures().supportsEdgeRetrieval) {
                Iterator<String> itty = testGraph.getEdgeIdSet().values().iterator();
                String firstEdgeId = itty.next();
                String secondEdgeId = itty.next();

                // put as URI
                String edgeProperty = "propertya=(i,123)&propertyb=(d,321.5)&propertyc=test";
                ClientResponse response = this.doGraphPut(testGraph, "edges/" + encode(firstEdgeId), edgeProperty);
                assertPuttedEdge(firstEdgeId, response, true);

                // put as JSON
                Map<String, Object> jsonEdgeData = new HashMap<String, Object>();
                jsonEdgeData.put("propertya", 123);
                jsonEdgeData.put("propertyb", 321.5);
                jsonEdgeData.put("propertyc", "test");

                JSONObject jsonEdgeToPut = new JSONObject(jsonEdgeData);
                response = this.doGraphPutOfJson(testGraph, "edges/" + encode(secondEdgeId), jsonEdgeToPut);
                assertPuttedEdge(secondEdgeId, response, true);
            }
        }
    }

    @Test
    public void putEdgeComplexStatusOk() throws JSONException {
        for (GraphTestHolder testGraph : this.testGraphs) {
            // A PUT has to get the edge before it can update it
            if (testGraph.getFeatures().supportsEdgeRetrieval && testGraph.getFeatures().supportsMapProperty) {
                Iterator<String> itty = testGraph.getEdgeIdSet().values().iterator();
                String firstEdgeId = itty.next();
                String secondEdgeId = itty.next();

                // put as URI
                String complexValue = "(map,(propertya=(i,123),propertyb=(d,321.5),propertyc=(list,(x,y,z)),propertyd=(map,(x=xyz))))";
                String complexKeyValueUri = "complex=" + complexValue;
                ClientResponse response = this.doGraphPut(testGraph, "edges/" + encode(firstEdgeId), complexKeyValueUri);
                assertPuttedEdge(firstEdgeId, response, false);

                // put as JSON
                String complexKeyValueJson = "{\"propertya\":123,\"propertyb\":321.5,\"propertyc\":[\"x\",\"y\",\"z\"],\"propertyd\":{\"x\":\"xyz\"}}";
                JSONTokener tokener = new JSONTokener(complexKeyValueJson);
                JSONObject complexJsonObject = new JSONObject(tokener);
                Map<String, Object> jsonEdgeData = new HashMap<String, Object>();
                jsonEdgeData.put("complex", complexJsonObject);

                JSONObject jsonEdgeToPost = new JSONObject(jsonEdgeData);
                response = this.doGraphPutOfJson(testGraph, "edges/" + encode(secondEdgeId), jsonEdgeToPost);
                assertPuttedEdge(secondEdgeId, response, false);
            }
        }
    }

    @Test
    public void deleteEdgeStatusNotFound() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            // A DELETE has to get the edge before it can remove it
            if (testGraph.getFeatures().supportsEdgeRetrieval) {
                ClientResponse response = this.doGraphDelete(testGraph, "edges/1000notreal");

                Assert.assertEquals(ClientResponse.Status.NOT_FOUND, response.getClientResponseStatus());
            }
        }
    }

    @Test
    public void deleteEdgeStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            // maybe make a test that doesn't do property deletes first so that this test doesn't
            // need edge retrieval
            if (testGraph.getFeatures().supportsEdgeRetrieval) {
                Iterator<String> itty = testGraph.getEdgeIdSet().values().iterator();
                String edgeToDelete = itty.next();

                ClientResponse responseGetEdge = this.doGraphGet(testGraph, "edges/" + encode(edgeToDelete));
                Assert.assertEquals(ClientResponse.Status.OK, responseGetEdge.getClientResponseStatus());
                JSONObject edgeJson = responseGetEdge.getEntity(JSONObject.class);

                List<String> keysToRemove = new ArrayList<String>();
                Iterator<String> propertyItty = edgeJson.optJSONObject(Tokens.RESULTS).keys();
                String keysToDeleteQueryString = "";
                while (propertyItty.hasNext()) {
                    String key = propertyItty.next();
                    if (!key.startsWith(Tokens.UNDERSCORE)) {
                        keysToRemove.add(key);
                        keysToDeleteQueryString = keysToDeleteQueryString + "&" + key;
                    }
                }

                // delete the properties first
                ClientResponse responsePropertyDelete = this.doGraphDelete(testGraph, "edges/" + encode(edgeToDelete), keysToDeleteQueryString);
                Assert.assertEquals(ClientResponse.Status.OK, responsePropertyDelete.getClientResponseStatus());

                responseGetEdge = this.doGraphGet(testGraph, "edges/" + encode(edgeToDelete));
                Assert.assertEquals(ClientResponse.Status.OK, responseGetEdge.getClientResponseStatus());
                edgeJson = responseGetEdge.getEntity(JSONObject.class).optJSONObject(Tokens.RESULTS);

                for (String key : keysToRemove) {
                    Assert.assertFalse(edgeJson.has(key));
                }

                // delete the edge itself
                responsePropertyDelete = this.doGraphDelete(testGraph, "edges/" + encode(edgeToDelete));
                Assert.assertEquals(ClientResponse.Status.OK, responsePropertyDelete.getClientResponseStatus());

                responseGetEdge = this.doGraphGet(testGraph, "edges/" + encode(edgeToDelete));
                Assert.assertEquals(ClientResponse.Status.NOT_FOUND, responseGetEdge.getClientResponseStatus());
            }
        }
    }

    private void assertPostedEdge(String vertexIdIn, String vertexIdOut, ClientResponse response, boolean simpleProperties) {
        Assert.assertNotNull(response);
        Assert.assertEquals(ClientResponse.Status.OK, response.getClientResponseStatus());

        JSONObject createdEdgeJson = response.getEntity(JSONObject.class).optJSONObject(Tokens.RESULTS);
        Assert.assertEquals(vertexIdIn, createdEdgeJson.optString(Tokens._IN_V));
        Assert.assertEquals(vertexIdOut, createdEdgeJson.optString(Tokens._OUT_V));

        if (simpleProperties) {
            assertPostedEdgeSimpleProperties(createdEdgeJson);    
        } else {
            assertPostedEdgeComplexProperties(createdEdgeJson);
        }
    }

    private void assertPuttedEdge(String edgeId, ClientResponse response, boolean simpleProperties) {
        Assert.assertNotNull(response);
        Assert.assertEquals(ClientResponse.Status.OK, response.getClientResponseStatus());

        JSONObject createdEdgeJson = response.getEntity(JSONObject.class).optJSONObject(Tokens.RESULTS);
        Assert.assertEquals(edgeId, createdEdgeJson.optString(Tokens._ID));

        if (simpleProperties) {
            assertPostedEdgeSimpleProperties(createdEdgeJson);
        } else {
            assertPostedEdgeComplexProperties(createdEdgeJson);
        }

        int countProperties = 0;
        Iterator<String> itty = createdEdgeJson.keys();
        while (itty.hasNext()) {
            String key = itty.next();
            if (!key.startsWith(Tokens.UNDERSCORE)) {
                countProperties++;
            }
        }

        if (simpleProperties) {
            Assert.assertEquals(3, countProperties);
        } else {
            Assert.assertEquals(1, countProperties);
        }
    }

    private void assertPostedEdgeComplexProperties(JSONObject createdEdgeJson) {
        JSONObject mapRootProperty = createdEdgeJson.optJSONObject("complex");
        Assert.assertEquals(123, mapRootProperty.optInt("propertya"));
        Assert.assertEquals(321.5, mapRootProperty.optDouble("propertyb"));

        JSONArray listInMapProperty = mapRootProperty.optJSONArray("propertyc");
        Assert.assertEquals(3, listInMapProperty.length());

        JSONObject mapInMapProperty = mapRootProperty.optJSONObject("propertyd");
        Assert.assertEquals("xyz", mapInMapProperty.optString("x"));
    }
    
    private void assertPostedEdgeSimpleProperties(JSONObject createdEdgeJson) {
        Assert.assertEquals(123, createdEdgeJson.optInt("propertya"));
        Assert.assertEquals(321.5, createdEdgeJson.optDouble("propertyb"));
        Assert.assertEquals("test", createdEdgeJson.optString("propertyc"));
    }
}
