package com.kinnarastudio.kecakplugins.numerictextfield;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

public class ConditionalValueLoadBinder extends WorkflowFormBinder {
    private static final String LABEL = "Conditional Value Load Binder";

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        if (isDebug()) {
            LogUtil.info(getClassName(), "load : element ID [" + Optional.ofNullable(element).map(e -> e.getPropertyString("id")).orElse("") + "]");
        }
        Map<String, String[]> params = formData.getRequestParams();
        String equation = getPropertyString("equation");
        if (isDebug()) {
            LogUtil.info(getClassName(), "load : equation [" + equation + "]");
        }

        params.forEach((k, v) -> {
            LogUtil.info(getClassName(), "[" + k + "]: [" + Arrays.toString(v) + "]");
        });

        try {
            return null;

            // if (isDebug()) {
            //     LogUtil.info(getClassName(), "load : result [" + result.toPlainString() + "]");
            // }

            // final String finalResult = result.toPlainString();

            // return new FormRowSet() {
            //     {
            //         add(new FormRow() {
            //             {
            //                 put(element.getPropertyString(FormUtil.PROPERTY_ID), finalResult);
            //             }
            //         });
            //     }
            // };
        } catch (Exception e) {
        }
        return null;
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
        return AppUtil.readPluginResource(getClassName(), "/properties/equationLoadBinder.json");
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
