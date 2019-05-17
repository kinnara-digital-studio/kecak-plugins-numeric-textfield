package com.kinnara.kecakplugins;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;

import java.math.BigDecimal;
import java.util.Map;

public class EquationLoadBinder extends WorkflowFormBinder {
    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet rowSet = new FormRowSet();
        Map<String, String[]> params = formData.getRequestParams();
        String equation = getPropertyString("equation");
        LogUtil.info(this.getClassName(), "Equation : " + equation);
        try {
            Expression e = new ExpressionBuilder(equation).variables(params.keySet()).build();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String value = entry.getValue()[0];
                e.setVariable(entry.getKey(), (value==null || value.isEmpty()) ? (0) : (Double.valueOf(value)));
            }
            Double res = e.evaluate();
            BigDecimal result = BigDecimal.valueOf(res);
            FormRow row = new FormRow();
            row.put(element.getPropertyString(FormUtil.PROPERTY_ID), result);
            rowSet.add(row);
        } catch (Exception e) {
            LogUtil.error(this.getClassName(), e, null);
        }
        return rowSet;
    }

    @Override
    public String getName() {
        return "Equation Load Binder";
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
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/equationLoadBinder.json", null, false,"/messages/equationLoadBinder");
    }

}
