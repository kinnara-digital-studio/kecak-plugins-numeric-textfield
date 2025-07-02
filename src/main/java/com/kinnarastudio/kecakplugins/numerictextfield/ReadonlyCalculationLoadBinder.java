package com.kinnarastudio.kecakplugins.numerictextfield;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginManager;

import java.util.ResourceBundle;

/**
 * Read-only Load Binder
 */
public class ReadonlyCalculationLoadBinder extends FormBinder implements FormLoadElementBinder {
    public final static String LABEL = "Readonly Calculation Load Binder";

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        return new FormRowSet() {{
            add(new FormRow() {{
                final String elementId = element.getPropertyString("id");
                final String value = FormUtil.getRequestParameter(element, formData);
                setProperty(elementId, value);
            }});
        }};
    }

    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getName();
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
        return "";
    }
}
