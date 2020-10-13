package com.kinnara.kecakplugins.numerictextfield;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class EquationLoadBinder extends WorkflowFormBinder implements Unclutter {
    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        if(isDebug()) {
            LogUtil.info(getClassName(), "load : element ID ["+ Optional.ofNullable(element).map(e -> e.getPropertyString("id")).orElse("")+"]");
        }
        FormRowSet rowSet = new FormRowSet();
        Map<String, String[]> params = formData.getRequestParams();
        String equation = getPropertyString("equation");
        if(isDebug()) {
            LogUtil.info(getClassName(), "load : equation ["+equation+"]");
        }

        try {
            Expression e = new ExpressionBuilder(equation).variables(params.keySet()).build();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                double value = Optional.of(entry)
                        .map(Map.Entry::getValue)
                        .map(Arrays::stream)
                        .orElseGet(Stream::empty)
                        .findFirst()
                        .filter(s -> !s.isEmpty())
                        .map(throwableFunction(Double::parseDouble, (RuntimeException ex) -> {
                            if(isDebug()) {
                                LogUtil.error(getClassName(), ex, ex.getMessage());
                            }
                            return 0.0;
                        }))
                        .orElse(0.0);

                if(isDebug()) {
                    LogUtil.info(getClassName(), "load : variable ["+entry.getKey()+"] value [" + value + "]");
                }

                e.setVariable(entry.getKey(), value);
            }

            BigDecimal result = BigDecimal.valueOf(e.evaluate());

            if(isDebug()) {
                LogUtil.info(getClassName(), "load : result [" + result + "]");
            }

            FormRow row = new FormRow();
            row.put(element.getPropertyString(FormUtil.PROPERTY_ID), result.toString());
            rowSet.add(row);
        } catch (Exception e) {
        }
        return rowSet;
    }

    @Override
    public String getName() {
        return getLabel() + getVersion();
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
    public String getLabel() {
        return "Equation Load Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/equationLoadBinder.json", null, false,"/messages/equationLoadBinder");
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
