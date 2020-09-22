package com.kinnara.kecakplugins.numerictextfield;

import com.kinnara.kecakplugins.numerictextfield.utils.Declutter;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.ResourceBundleUtil;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aristo
 *
 * Numeric Validator
 *
 */
public class NumericValidator extends FormValidator implements Declutter {
    @Override
    public boolean validate(Element element, FormData formData, String[] values) {
        boolean valid = compile(getConditions(), getValue(values));
        if(!valid) {
            String id = FormUtil.getElementParameterName(element);
            formData.addFormError(id, getMessage());
        }
        return valid;
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
        return "Numeric Validator";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/numericValidator.json", null, true, "/messages/numericValidator");
    }

    @Nonnull
    private String getMessage() {
        return Optional.of("message")
                .map(this::getPropertyString)
                .filter(this::isNotEmpty)
                .map(s -> AppUtil.processHashVariable(s, null, null, null))
                .orElse(ResourceBundleUtil.getMessage("form.defaultvalidator.err.invalid"));
    }

    @Nonnull
    private List<Map<String, String>> getConditions() {
        return Optional.of("conditions")
                .map(this::getProperty)
                .map(o -> (Object[]) o)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .filter(Objects::nonNull)
                .map(o -> (Map<String, String>)o)
                .collect(Collectors.toList());
    }

    /**
     * Compile
     *
     * @param conditions
     * @param value
     * @return
     */
    private boolean compile(@Nonnull List<Map<String, String>> conditions, BigDecimal value) {
        boolean compileResult = true;
        for (Map<String, String> condition : conditions) {
            BigDecimal operand = new BigDecimal(condition.getOrDefault("value", "0"));
            String operator = condition.getOrDefault("operator", "eq");
            boolean and = condition.getOrDefault("joinType", "and").equals("and");
            boolean conditionResult = checkCondition(value, operator, operand);
            compileResult = and ? (compileResult && conditionResult) : (compileResult || conditionResult);
        }
        return compileResult;
    }


    /**
     * Get value as {@link BigDecimal}
     *
     * @param values
     * @return
     */
    private BigDecimal getValue(String[] values) {
        return Optional.ofNullable(values)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .filter(Objects::nonNull)
                .findFirst()
                .map(String::valueOf)
                .map(throwableFunction(BigDecimal::new))
                .orElse(BigDecimal.ZERO);
    }


    /**
     * Check condition
     *
     * @param operand1
     * @param operator
     * @param operand2
     * @return
     */
    private boolean checkCondition(BigDecimal operand1, String operator, BigDecimal operand2) {
        switch (operator) {
            case "lt":
                return operand1.compareTo(operand2) < 0;
            case "lte":
                return operand1.compareTo(operand2) <= 0;
            case "gt":
                return operand1.compareTo(operand2) > 0;
            case "gte":
                return operand1.compareTo(operand2) >= 0;
            case "ne":
                return !operand1.equals(operand2);
            default:
                return operand1.equals(operand2);
        }
    }

    @Override
    public String getElementDecoration() {
        return isNotEmpty(getConditions()) ? "*" : "";
    }
}
