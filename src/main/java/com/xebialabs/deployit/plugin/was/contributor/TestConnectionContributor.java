package com.xebialabs.deployit.plugin.was.contributor;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.xebialabs.deployit.plugin.api.deployment.execution.DeploymentStep;
import com.xebialabs.deployit.plugin.api.deployment.planning.Contributor;
import com.xebialabs.deployit.plugin.api.deployment.planning.DeploymentPlanningContext;
import com.xebialabs.deployit.plugin.api.deployment.specification.Delta;
import com.xebialabs.deployit.plugin.api.deployment.specification.Deltas;
import com.xebialabs.deployit.plugin.api.deployment.specification.Operation;
import com.xebialabs.deployit.plugin.api.execution.Step;
import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.Deployed;
import com.xebialabs.deployit.plugin.python.PythonDeploymentStep;
import com.xebialabs.deployit.plugin.was.deployed.ExtensibleDeployedResource;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Maps.newHashMap;

/**
 *
 */
public class TestConnectionContributor {

    public static final String TEST_CONNECTION_PROPERTY_NAME = "testConnection";

    @Contributor
    public void testConnection(Deltas deltas, DeploymentPlanningContext ctx) {
        ctx.addSteps(transform(filter(deltas.getDeltas(), CANDIDATE), TO_STEP));
    }

    private static final Function<Delta, DeploymentStep> TO_STEP = new Function<Delta, DeploymentStep>() {
        public DeploymentStep apply(Delta input) {
            ExtensibleDeployedResource deployed = (ExtensibleDeployedResource) input.getDeployed();
            return new PythonDeploymentStep(
                    deployed.<Integer>getSyntheticProperty("testConnectionOrder"),
                    deployed.getContainer().getManagingContainer(),
                    deployed.<String>getSyntheticProperty("testConnectionScript"),
                    getPythonVars(deployed),
                    "Test Connection " + deployed.getName());
        }

        private Map<String, Object> getPythonVars(Deployed deployed) {
            Map<String, Object> pythonVars = newHashMap();
            pythonVars.put("deployed", deployed);
            return pythonVars;
        }
    };

    private static final Predicate<Delta> CANDIDATE = new Predicate<Delta>() {
        public boolean apply(Delta each) {
            if (each.getOperation() == Operation.CREATE || each.getOperation() == Operation.MODIFY) {
                Deployed deployed = each.getDeployed();
                Type type = deployed.getType();
                if (!type.getPrefix().equals("was"))
                    return false;
                if (!type.getName().endsWith("Datasource"))
                    return false;

                if (!deployed.getSyntheticProperties().containsKey(TEST_CONNECTION_PROPERTY_NAME))
                    return false;
                Boolean testConnection = deployed.getSyntheticProperty(TEST_CONNECTION_PROPERTY_NAME);
                if (testConnection && deployed instanceof ExtensibleDeployedResource)
                    return true;
            }
            return false;
        }

    };
}
