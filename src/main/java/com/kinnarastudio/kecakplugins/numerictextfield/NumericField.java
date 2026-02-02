package com.kinnarastudio.kecakplugins.numerictextfield;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import com.kinnarastudio.kecakplugins.numerictextfield.utils.Declutter;

/**
 * @author aristo
 */
public class NumericField extends Element implements Declutter, FormBuilderPaletteElement, PluginWebSupport {
    @Override
    public String getName() {
        return getLabel() + getVersion();
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
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "Numeric Field";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(this.getClass().getName(), "/properties/numericField.json", new String[]{EquationLoadBinder.class.getName()}, true, "messages/numericField").replaceAll("\"", "'");
    }

    @Override
    public String getFormBuilderCategory() {
        return "Kecak";
    }

    @Override
    public int getFormBuilderPosition() {
        return 100;
    }

    @Override
    public String getFormBuilderIcon() {
        return null;
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>Numeric Field</label><input type='text' readonly/>";
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "numericField.ftl";
        return renderTemplate(template, formData, dataModel);
    }

    protected String formatValue(String value, FormData formData) {
        try {
            LogUtil.info(getClassName(), "test value : " + value);
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
//            DataListColumnFormatDefault formatter = getFormatterPlugin(appDef.getAppId(), appDef.getVersion().toString(), this);
            DataListColumnFormatDefault formatter = Utilities.getPluginObject((Map<String, Object>) getProperty("formatterPlugin"), pluginManager);
            String currencyRef = getPropertyString("currencyRefField");
            Map.Entry<String, String> currency = null;
            if (currencyRef != null && !currencyRef.isEmpty()) {
                String currencyValue = formData.getRequestParameter(currencyRef);
                if (currencyValue != null) {
                    currency = new AbstractMap.SimpleEntry<>(currencyRef, currencyValue);
                }
            }
            return reformatValue(this.getPropertyString(FormUtil.PROPERTY_ID), value, FormUtil.findRootForm(this), formData, formatter);
        } catch (Exception e) {
            return value;
        }
    }

    protected JSONObject generateJsonCalculationLoadBinderConf(Form form, Element element, FormData formData) {
        try {
            JSONObject requestBody = new JSONObject();
            if (form != null)
                requestBody.put("formDefId", form.getPropertyString("id"));

            AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

            requestBody.put("sectionId", getSection(element).getPropertyString("id"));
            requestBody.put("url", WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/app/" + appDefinition.getAppId() + "/" + appDefinition.getVersion() + "/plugin/" + getClassName() + "/service");
            requestBody.put("baseUrl", WorkflowUtil.getHttpServletRequest().getContextPath());
            requestBody.put("primaryKey", formData.getPrimaryKeyValue());
            requestBody.put("field", "");

            return requestBody;
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }
        return null;
    }

    protected String getConfigJson(FormData formData) {
        Form rootForm = FormUtil.findRootForm(this);
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            JSONObject jsonConfig = new JSONObject();
            jsonConfig.accumulate("equation", getPropertyString("equation"));
            jsonConfig.put("fieldId", getPropertyString("id"));
            jsonConfig.put("primaryKey", formData.getPrimaryKeyValue());
            jsonConfig.put("assignmentId", formData.getActivityId());
            jsonConfig.put("appId", appDef.getAppId());
            jsonConfig.put("appVersion", appDef.getVersion().toString());

            jsonConfig.put(
                "useJsEquation", "true".equalsIgnoreCase(getPropertyString("useJsEquation"))
            );
            jsonConfig.put("jsEquation", getPropertyString("jsEquation"));

            jsonConfig.put(
                "roundNumber", "true".equalsIgnoreCase(getPropertyString("roundNumber"))
            );
            jsonConfig.put("roundingMode", getPropertyString("roundingMode"));
            jsonConfig.put("decimalPlaces", getPropertyString("decimalPlaces"));

            Object formatter = getProperty("formatterPlugin");
            boolean hasFormatter = false;

            if (formatter instanceof Map) {
                String className = ((Map<?, ?>) formatter).get("className") != null
                    ? ((Map<?, ?>) formatter).get("className").toString()
                    : "";

                hasFormatter = !className.isBlank();
            }

            jsonConfig.put("hasFormatter", hasFormatter);

            jsonConfig.accumulate("variables", getProperty("variables"));

            jsonConfig.put("currencyRefField", getPropertyString("currencyRefField"));
            jsonConfig.put("loadBinder", generateJsonCalculationLoadBinderConf(rootForm, this, formData));
            jsonConfig.put("spellingTarget", getPropertyString("spellingTarget"));

            return jsonConfig.toString();
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return "{}";
        }
    }

    @Override
    public FormRowSet formatData(FormData formData) {
        FormRowSet rowSet = super.formatData(formData);
        if (rowSet == null || rowSet.isEmpty()) {
            rowSet = new FormRowSet();
            rowSet.add(new FormRow());
        }

        String id = getPropertyString(FormUtil.PROPERTY_ID);

        String value = formData.getRequestParameter(id);

        if (value != null) {
            rowSet.get(0).put(id, value);
        }

        return rowSet;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LogUtil.info(getClass().getName(), "Executing Rest API [" + request.getRequestURI() + "] in method [" + request.getMethod() + "] contentType [" + request.getContentType() + "] as [" + WorkflowUtil.getCurrentUsername() + "]");

        ApplicationContext appContext = AppUtil.getApplicationContext();
        PluginManager pluginManager = (PluginManager) appContext.getBean("pluginManager");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        try {
            if (appDef == null) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Application definition cannot be defined");
            }

            JSONObject body;
            String formDefId;
            String sectionId;
            String fieldId;
            String primaryKey;
            String assignmentId;

            try {
                body = constructRequestBody(request);
                formDefId = body.getString("formDefId");
                sectionId = body.getString("sectionId");
                fieldId = body.getString("fieldId");
                primaryKey = body.optString("primaryKey");
                assignmentId = body.optString("assignmentId");
            } catch (JSONException e) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, e);
            }

            Form form = generateForm(formDefId, appDef);
            if (form == null) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Form [" + formDefId + "] cannot be defined");
            }

            // Numeric Field Element
            WorkflowAssignment workflowAssignment;
            if (!assignmentId.isEmpty()) {
                workflowAssignment = getAssignment(assignmentId);
                if (workflowAssignment == null) {
                    LogUtil.warn(getClassName(), "Unknown assignment [" + assignmentId + "]");
                }
            } else {
                workflowAssignment = null;
            }

            JSONObject requestParameters = body.optJSONObject("requestParams");
            FormData formData = generateFormData(workflowAssignment, primaryKey, requestParameters);

            Element element = elementStream(form, formData, true)
                    .filter(e -> fieldId.equals(e.getPropertyString("id")) && e instanceof NumericField && getSection(e).getPropertyString("id").equals(sectionId))
                    .findFirst()
                    .orElseThrow(() -> new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Unknown element [" + fieldId + "] in form [" + formDefId + "]"));

            String currencyField = element.getPropertyString("currencyRefField");
            Map<String, Object> calculationLoadBinder = (Map<String, Object>) element.getProperty("calculationLoadBinder");

            BigDecimal value;

            FormLoadBinder loadBinderPlugins = Utilities.getPluginObject(calculationLoadBinder, pluginManager);
            if (loadBinderPlugins != null) {
                // value is calculated by calculation load binder
                element.setLoadBinder(loadBinderPlugins);
                value = executeCalculation(element, formData);

            } else {
                // value from user input
                Locale locale = getLocale(requestParameters.optString(currencyField));
                value = determineNumber(body.optString("formatValue"), locale);
            }

            Map<String, Object> formatterPlugin = (Map<String, Object>) element.getProperty("formatterPlugin");
            DataListColumnFormatDefault formatter = Utilities.getPluginObject(formatterPlugin, pluginManager);
            String formattedValue = formatter == null ? value.toString()
                    : reformatValue(fieldId, value.toString(), form, formData, formatter);

            JSONObject data = new JSONObject();
            data.put("value", value.toPlainString());
            data.put("mask_value", formattedValue);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(data.toString());

        } catch (RestApiException e) {
            String appId = appDef == null ? "" : appDef.getAppId();
            long appVersion = appDef == null ? 0 : appDef.getVersion();
            LogUtil.error(getClassName(), e, "Application [" + appId + "] version [" + appVersion + "] message [" + e.getMessage() + "]");
            response.sendError(e.getErrorCode(), "Response [" + e.getErrorCode() + "] : " + e.getMessage());
        } catch (JSONException e) {
            String appId = appDef.getAppId();
            long appVersion = appDef.getVersion();
            LogUtil.error(getClassName(), e, "Application [" + appId + "] version [" + appVersion + "] message [" + e.getMessage() + "]");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Determine the numeric value of input string
     *
     * @param input input string
     * @return numeric representation
     */
    private BigDecimal determineNumber(String input, Locale locale) {
        try {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
            if (numberFormat == null) {
                return new BigDecimal("0");
            }

            try {
                Number number;
                if (locale.getLanguage().equals("in")) {
                    number = numberFormat.parse(input.replace(".", ","));
                    LogUtil.info(getClassName(), "Parse input result: " + number);
                } else {
                    number = numberFormat.parse(input);
                }

                return BigDecimal.valueOf(number.doubleValue());
            } catch (ParseException ignored) {
            }

            try {
                String assumptionAsInternationalFormat = input.replaceAll(",", "").replaceAll("\\.", ".");
                Number number = numberFormat.parse(assumptionAsInternationalFormat);
                return BigDecimal.valueOf(number.doubleValue());
            } catch (ParseException ignored) {
            }

            try {
                String assumptionAsDotAsThousands = input.replaceAll("\\.", "").replaceAll(",", ".");
                Number number = numberFormat.parse(assumptionAsDotAsThousands);
                return BigDecimal.valueOf(number.doubleValue());
            } catch (ParseException ignored) {
            }

        } catch (NumberFormatException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }

        return new BigDecimal("0");
    }

    /**
     * Generate FormData based on request
     *
     * @param primaryKey
     * @param requestParameters
     * @return
     */
    private FormData generateFormData(WorkflowAssignment workflowAssignment, String primaryKey, JSONObject requestParameters) {
        FormData formData = new FormData();
        formData.setPrimaryKeyValue(primaryKey);
        if (workflowAssignment != null) {
            formData.setProcessId(workflowAssignment.getProcessId());
            formData.setActivityId(workflowAssignment.getActivityId());
        }

        if (requestParameters != null) {
            Iterator<String> i = requestParameters.keys();
            while (i.hasNext()) {
                String key = i.next();
                String requestValue = requestParameters.optString(key);
                formData.addRequestParameterValues(key, new String[]{requestValue});
            }
        }

        return formData;
    }

    private Locale getLocale(String locale) {
        String value = locale == null || locale.isEmpty() ? "en_US" : locale;
        String[] split = value.split("_");
        return new Locale(split[0].trim(), split.length > 1 ? split[1].trim() : "");
    }

    private String reformatValue(@Nonnull String fieldId, @Nonnull String value, Form form, @Nonnull FormData formData, DataListColumnFormatDefault formatterPlugin) {
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        String primaryKey = formData.getPrimaryKeyValue();

        // get initial row from database
        Map<String, String> row = Optional.ofNullable(primaryKey)
                .map(s -> formDataDao.load(form, s))
                .orElse(new FormRow())
                .entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(String.valueOf(e.getKey()), String.valueOf(e.getValue())), Map::putAll);

        // replace some key with data passed from formData
        formData.getRequestParams().forEach((key, value1) -> row.put(key, String.join(";", value1)));

        if (formatterPlugin == null)
            return value;

        DataListColumn col = new DataListColumn();
        col.setName(getPropertyString(FormUtil.PROPERTY_ID));
        col.setLabel(getPropertyString(FormUtil.PROPERTY_LABEL));
        col.setFormats(Collections.singleton(formatterPlugin));
        DataList dataList = new DataList();
        dataList.setColumns(new DataListColumn[]{col});
        DataListCollection<Map<String, String>> rows = new DataListCollection<>();
        rows.add(row);
        dataList.setRows(rows);
        String formattedValue = formatterPlugin.format(dataList, col, row, value);

        if (formattedValue == null) {
            return value;
        } else {
            return formattedValue.replaceAll("<[^>]+>", "");
        }
    }

    protected Form generateForm(String formDefId, AppDefinition appDef) {
        // check in cache

        // proceed without cache
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        Form form;
        if (appDef != null && formDefId != null && !formDefId.isEmpty()) {
            FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
            FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
            if (formDef != null) {
                String json = formDef.getJson();
                form = (Form) formService.createElementFromJson(json);

                // put in cache if possible
//                if(formCache != null)
//                    formCache.put(formDefId, form);

                return form;
            }
        }
        return null;
    }


    private JSONObject constructRequestBody(HttpServletRequest request) throws JSONException, RestApiException {
        String jsonString = "";
        try (BufferedReader bf = request.getReader()) {
            jsonString = bf.lines().collect(Collectors.joining());
            return new JSONObject(jsonString);
        } catch (IOException | JSONException e) {
            LogUtil.warn(getClassName(), "JSON String : " + jsonString);
            throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, e);
        }
    }

    /**
     * Execute calculation load binder
     *
     * @param element
     * @param formData
     * @return
     */
    private BigDecimal executeCalculation(Element element, FormData formData) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormService formService = (FormService) appContext.getBean("formService");

        try {
            formData = formService.executeFormLoadBinders(element, formData);
        } catch (Exception e) {
        }

        FormRowSet rowSet = formData.getLoadBinderData(element);
        return Optional.ofNullable(rowSet)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .findFirst()
                .map(row -> row.getProperty(element.getPropertyString(FormUtil.PROPERTY_ID)))
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .map(BigDecimal::new)
                .orElse(new BigDecimal("0"));
    }

    private String renderTemplate(String template, FormData formData, @SuppressWarnings("rawtypes") Map dataModel) {
        String value = FormUtil.getElementPropertyValue(this, formData);
        dataModel.put("value", value);
        dataModel.put("formattedValue", formatValue(value, formData));
        dataModel.put("configJson", getConfigJson(formData));
        dataModel.put("className", getClassName());
        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }


    /**
     * Get assignment object
     *
     * @param activityId any activity ID, event the completed / aborted
     * @return
     */
    protected WorkflowAssignment getAssignment(@Nonnull String activityId) {
        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        return Optional.of(activityId)
                .map(throwableFunction(workflowManager::getAssignment))
                .orElse(null);
    }

    private boolean normalizeOnChangeEvent(Object value) {
    // default TRUE kalau null
    if (value == null) {
        return true;
    }

    // Ketika value nya String
    if (value instanceof String) {
        return "true".equalsIgnoreCase((String) value);
    }

    if (value instanceof Boolean) {
        return (Boolean) value;
    }

    return false;
}
}

