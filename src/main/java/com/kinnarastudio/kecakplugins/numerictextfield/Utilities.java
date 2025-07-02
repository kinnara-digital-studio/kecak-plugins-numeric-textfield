package com.kinnarastudio.kecakplugins.numerictextfield;

import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;

import java.util.Map;

public class Utilities {
    /**
     * Generate plugins
     * @param elementSelect
     * @param <T>
     * @return
     */
    public static <T extends PropertyEditable> T getPluginObject(Map<String, Object> elementSelect, PluginManager pluginManager) {
        if(elementSelect == null)
            return null;

        String className = (String)elementSelect.get("className");
        Map<String, Object> properties = (Map<String, Object>)elementSelect.get("properties");

        T  plugin = (T) pluginManager.getPlugin(className);
        if(plugin == null) {
            return null;
        }

        properties.forEach(plugin::setProperty);

        return plugin;
    }
}
