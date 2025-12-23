package com.kinnarastudio.kecakplugins.numerictextfield;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class ConditionalValueLoadBinder extends WorkflowFormBinder {

    private static final String LABEL = "Conditional Value Load Binder";

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        if (isDebug()) {
            LogUtil.info(getClassName(), "load : element ID [" + Optional.ofNullable(element).map(e -> e.getPropertyString("id")).orElse("") + "]");
        }
        Map<String, String[]> params = formData.getRequestParams();

        params.forEach((k, v) -> {
            LogUtil.info(getClassName(), "[" + k + "]: [" + Arrays.toString(v) + "]");
        });

        Map<String, String>[] variables = getPropertyGrid("variables");

        for (Map<String, String> variable : variables) {
            for (Map.Entry<String, String> entry : variable.entrySet()) {
                LogUtil.info(getClassName(), "Var Key: [" + entry.getKey() + "], Val: [" + entry.getValue() + "]");
            }
        }

        String selectedOperation = null;

        for (Map<String, String> variable : variables) {
            String condition = variable.get("variableName");
            String operation = variable.get("operation");

            if (evaluateCondition(condition, params)) {
                selectedOperation = operation;
                break;
            }
        }

        if (selectedOperation == null) {
            return null; // atau default value
        }

        try {
            Expression e = new ExpressionBuilder(selectedOperation)
                    .variables(params.keySet())
                    .build();

            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                if (selectedOperation.contains(entry.getKey())) {
                    double value = Optional.of(entry)
                            .map(Map.Entry::getValue)
                            .map(v -> v[0])
                            .filter(s -> !s.isEmpty())
                            .map(Double::parseDouble)
                            .orElse(0.0);

                    e.setVariable(entry.getKey(), value);
                }
            }

            BigDecimal result = BigDecimal.valueOf(e.evaluate());

            if (isDebug()) {
                LogUtil.info(getClassName(), "load : result [" + result.toPlainString() + "]");
            }

            final String finalResult = result.toPlainString();

            return new FormRowSet() {
                {
                    add(new FormRow() {
                        {
                            put(element.getPropertyString(FormUtil.PROPERTY_ID), finalResult);
                        }
                    });
                }
            };
        } catch (Exception e) {
        }
        return null;
    }

    private boolean evaluateCondition(String condition, Map<String, String[]> params) {
        Pattern p = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*(<=|>=|<|>|==)\\s*(\\d+(\\.\\d+)?)");
        Matcher m = p.matcher(condition);

        if (!m.matches()) {
            return false;
        }

        String field = m.group(1);
        String operator = m.group(2);
        double compareValue = Double.parseDouble(m.group(3));

        double fieldValue = Optional.ofNullable(params.get(field))
                .map(v -> v[0])
                .filter(s -> !s.isEmpty())
                .map(Double::parseDouble)
                .orElse(0.0);

        switch (operator) {
            case "<":
                return fieldValue < compareValue;
            case "<=":
                return fieldValue <= compareValue;
            case ">":
                return fieldValue > compareValue;
            case ">=":
                return fieldValue >= compareValue;
            case "==":
                return fieldValue == compareValue;
            default:
                return false;
        }
    }

    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        return resourceBundle.getString("buildNumber");
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/ConditionalValueLoadBinder.json");
    }

    /**
     * Get property "debug"
     *
     * @return
     */
    private boolean isDebug() {
        return "true".equalsIgnoreCase(getPropertyString("debug"));
    }
}
