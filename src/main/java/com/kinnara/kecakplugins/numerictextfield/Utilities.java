package com.kinnara.kecakplugins.numerictextfield;

import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

        LogUtil.info(Utilities.class.getName(), "properties ["+properties.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue()).collect(Collectors.joining(";"))+"]");

        T  plugin = (T) pluginManager.getPlugin(className);
        if(plugin == null) {
            LogUtil.warn(Utilities.class.getName(), "Error generating plugin [" + className + "]");
            return null;
        }

        properties.forEach(plugin::setProperty);

        LogUtil.info(Utilities.class.getName(), Optional.ofNullable(plugin.getProperties()).map(Map::entrySet).orElse(new HashSet<>()).stream().map(e -> e.getKey() + "->" + e.getValue()).collect(Collectors.joining("||")));

        return plugin;
    }
}
