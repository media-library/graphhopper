/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper GmbH licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Helper;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Tests the DataFlagencoder with the SpatialRuleLookup enabled
 *
 * @author Robin Boldt
 */
public class GraphHopperDataflagEncoderSpatialRulesIT {
    private static final String DIR = "./target/north-bayreuth-gh/";

    private static final GraphHopperConfiguration config = new GraphHopperConfiguration();

    static {
        config.cmdArgs = new CmdArgs().
                put("config", "../config-example.properties").
                put("graph.flag_encoders", "generic").
                put("prepare.ch.weightings", "no").
                put("spatial_rules.location", "../core/files/spatialrules/countries.geo.json").
                put("spatial_rules.max_bbox", "11.4,11.7,49.9,50.1").
                put("datareader.file", "../core/files/north-bayreuth.osm.gz").
                put("graph.location", DIR);
    }

    @ClassRule
    public static final DropwizardAppRule<GraphHopperConfiguration> app = new DropwizardAppRule(
            GraphHopperApplication.class, config);


    @AfterClass
    public static void cleanUp() {
        Helper.removeDir(new File(DIR));
    }

    @Test
    public void testDetourToComplyWithSpatialRule() throws Exception {
        JsonNode response = app.client().target("http://localhost:8080/route?" + "point=49.995933,11.54809&point=50.004871,11.517191&vehicle=generic").request().buildGet().invoke().readEntity(JsonNode.class);
        assertFalse(response.get("info").has("errors"));
        double distance = response.get("paths").get(0).get("distance").asDouble();
        // Makes sure that SpatialRules are enforced. Without SpatialRules we take a shortcut trough the forest
        // so the route would be only 3.31km
        assertTrue("distance wasn't correct:" + distance, distance > 7000);
        assertTrue("distance wasn't correct:" + distance, distance < 7500);
    }

}