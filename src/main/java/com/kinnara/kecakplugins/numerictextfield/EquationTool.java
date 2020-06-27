package com.kinnara.kecakplugins.numerictextfield;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EquationTool extends DefaultApplicationPlugin {
    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public Object execute(Map map) {
        PluginManager pluginManager = (PluginManager) map.get("pluginManager");
        WorkflowManager workflowManager = (WorkflowManager) pluginManager.getBean("workflowManager");
        WorkflowAssignment workflowAssignment = (WorkflowAssignment) map.get("workflowAssignment");
        Collection<WorkflowVariable> workflowVariables = workflowManager.getProcessVariableList(workflowAssignment.getProcessId());

        String equation = getEquation(map);

        try {
            final Expression e = new ExpressionBuilder(equation)
                    .variables(workflowVariables.stream()
                            .map(WorkflowVariable::getName)
                            .collect(Collectors.toSet()))
                    .build();

            for (WorkflowVariable variable : workflowVariables) {
                try {
                    double value = Double.parseDouble(String.valueOf(variable.getVal()));
                    e.setVariable(variable.getName(), value);
                } catch (NumberFormatException ex) {
                    LogUtil.error(getClassName(), ex, ex.getMessage());
                    e.setVariable(variable.getName(), 0);
                }
            }

            workflowManager.processVariable(workflowAssignment.getProcessId(), getResultVariable(map), String.valueOf(e.evaluate()));
        } catch (Exception e) {
        }

        return null;
    }

    @Override
    public String getLabel() {
        return "Equation Tool";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/equationTool.json", null, false, "/messages/equationTool");
    }


    private String getEquation(Map properties) {
        return Optional.ofNullable("equation")
                .map(properties::get)
                .map(String::valueOf)
                .orElse("");
    }

    private String getResultVariable(Map properties) {
        return Optional.of("resultVariable")
                .map(properties::get)
                .map(String::valueOf)
                .orElse("");
    }
}
