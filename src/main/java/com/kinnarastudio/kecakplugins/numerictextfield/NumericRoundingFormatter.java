package com.kinnarastudio.kecakplugins.numerictextfield;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ResourceBundle;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

public class NumericRoundingFormatter extends DataListColumnFormatDefault {
    private final static String LABEL = "Numeric Rounding Formatter";

    @Override
    public String format(DataList datalist, DataListColumn column, Object row, Object value) {
        if (value == null || value.toString().isEmpty()) {
            return "0";
        }

        String valString = value.toString().trim();
        LogUtil.info(getClassName(), "Value: [" + valString + "]");

        String mode = getPropertyString("roundingMode");
        String scaleString = getPropertyString("decimalPlaces");
        // String prefix = getPropertyString("prefix"); // e.g., "$"
        // String suffix = getPropertyString("suffix"); // e.g., " kg"

        int scale = 0;
        try {
            if (scaleString != null && !scaleString.isEmpty()) {
                scale = Integer.parseInt(scaleString);
            }
        } catch (NumberFormatException e) {
            scale = 0; 
        }

        try {
            BigDecimal number = new BigDecimal(valString);
            BigDecimal result;

            // 2. Apply Rounding with Scale
            if ("round_up".equalsIgnoreCase(mode)) {
                result = number.setScale(scale, RoundingMode.CEILING); 
            } else {
                result = number.setScale(scale, RoundingMode.FLOOR);
            }

            // 3. Convert to String
            String formattedNumber;
            if (scale == 0) {
                formattedNumber = String.valueOf(result.longValue());
            } else {
                formattedNumber = result.toPlainString();
            }

            // 4. Attach Prefix and Suffix (if they exist)
            // if (prefix != null) formattedNumber = prefix + formattedNumber;
            // if (suffix != null) formattedNumber = formattedNumber + suffix;

            return formattedNumber;

        } catch (NumberFormatException e) {
            return valString;
        }
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/NumericRoundingFormatter.json");
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
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
    
}
