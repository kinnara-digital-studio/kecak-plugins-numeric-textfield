package com.kinnara.kecakplugins.numerictextfield;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

            String value = String.valueOf(e.evaluate());
            workflowManager.processVariable(workflowAssignment.getProcessId(), getResultVariable(map), value);

            updateFormData(map, value);

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

    private void updateFormData(Map properties, String value) {
        PluginManager pluginManager = (PluginManager) properties.get("pluginManager");
        FormDataDao formDataDao = (FormDataDao) pluginManager.getBean("formDataDao");

        StuffedForm stuffedForm = getForm(properties);

        String workflowVariableResult = getResultVariable(properties);
        Optional.ofNullable(stuffedForm)
                .map(s -> elementStream(s.form, s.formData))
                .orElseGet(Stream::empty)
                .filter(e -> isNotEmpty(e.getPropertyString("workflowVariable")))
                .filter(e -> workflowVariableResult.equals(e.getPropertyString("workflowVariable")))
                .forEach(e -> {
                    Objects.requireNonNull(stuffedForm);
                    final String elementId = e.getPropertyString("id");

                    FormRowSet rowSet = Optional.of(stuffedForm.formData)
                            .map(fd -> fd.getLoadBinderData(e))
                            .map(Collection::stream)
                            .orElseGet(Stream::empty)
                            .peek(r -> r.setProperty(elementId, value))
                            .collect(FormRowSet::new, FormRowSet::add,FormRowSet::addAll);

                    formDataDao.saveOrUpdate(stuffedForm.form, rowSet);
                });


    }

    /**
     * Stream element children
     *
     * @param element
     * @return
     */
    @Nonnull
    private Stream<Element> elementStream(@Nonnull Element element, FormData formData) {
        if (!element.isAuthorize(formData)) {
            return Stream.empty();
        }

        Stream<Element> stream = Stream.of(element);
        for (Element child : element.getChildren()) {
            stream = Stream.concat(stream, elementStream(child, formData));
        }
        return stream;
    }

    private String getEquation(Map properties) {
        return Optional.ofNullable("equation")
                .map(properties::get)
                .map(String::valueOf)
                .orElse("");
    }

    @Nonnull
    private String getResultVariable(Map properties) {
        return Optional.of("resultVariable")
                .map(properties::get)
                .map(String::valueOf)
                .orElse("");
    }

    @Nullable
    private StuffedForm getForm(Map properties) {
        PluginManager pluginManager = (PluginManager) properties.get("pluginManager");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        WorkflowAssignment workflowAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
        AppService appService = (AppService) pluginManager.getBean("appService");

        return Optional.of("formDefId")
                .map(properties::get)
                .map(String::valueOf)
                .map(s -> {
                    StuffedForm stuffedForm = new StuffedForm();
                    stuffedForm.formData = new FormData();
                    stuffedForm.formData.setPrimaryKeyValue(getPrimaryKey(properties));
                    if(workflowAssignment != null) {
                        stuffedForm.formData.setActivityId(workflowAssignment.getActivityId());
                        stuffedForm.formData.setProcessId(workflowAssignment.getProcessId());
                    }
                    stuffedForm.form = appService.viewDataForm(appDefinition.getAppId(), appDefinition.getVersion().toString(), s, null, null, null, stuffedForm.formData, null, null);
                    return stuffedForm;
                })
                .orElse(null);
    }

    private String getPrimaryKey(Map properties) {
        return Optional.of(properties)
                .map(m -> m.get("recordId"))
                .map(String::valueOf)
                .orElseGet(() -> {
                    PluginManager pluginManager = (PluginManager) properties.get("pluginManager");
                    WorkflowAssignment workflowAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
                    AppService appService = (AppService) pluginManager.getBean("appService");

                    return Optional.of(workflowAssignment)
                            .map(WorkflowAssignment::getProcessId)
                            .map(s -> appService.getOriginProcessId(s))
                            .orElse("");
                });
    }

    private <T> Predicate<T> not(Predicate<T> p) {
        return (T t) -> !p.test(t);
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    /**
     * Combination of form and formData
     */
    private class StuffedForm {
        Form form;
        FormData formData;
    }
}
