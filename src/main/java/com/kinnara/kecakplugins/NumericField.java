package com.kinnara.kecakplugins;

import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NumericField extends Element implements FormBuilderPaletteElement, PluginWebSupport {
    public String getName() {
        return "Numeric Field";
    }

    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    public String getLabel() {
        return getName();
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(this.getClass().getName(), "/properties/numericField.json", null, true, "messages/numericField");
    }

    public String getFormBuilderCategory() {
        return "Kecak";
    }

    public int getFormBuilderPosition() {
        return 100;
    }

    public String getFormBuilderIcon() {
        return null;
    }

    public String getFormBuilderTemplate() {
        return "<label class='label'>Numeric Field</label><input type='text' readonly/>";
    }

    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "numericField.ftl";
        String value = FormUtil.getElementPropertyValue(this, formData);
        dataModel.put("value", value);
        dataModel.put("formattedValue", formatValue(value, formData));
        dataModel.put("configJson", this.getConfigJson(formData));
        dataModel.put("className", getClassName());
        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    private String formatValue(String value, FormData formData) {
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
//            DataListColumnFormatDefault formatter = getFormatterPlugin(appDef.getAppId(), appDef.getVersion().toString(), this);
            DataListColumnFormatDefault formatter = Utilities.getPluginObject((Map<String, Object>) getProperty("formatterPlugin"), pluginManager);
            String currencyRef = getPropertyString("currencyRefField");
            Map.Entry<String, String> currency = null;
            if(currencyRef!=null && !currencyRef.isEmpty()) {
                String currencyValue = formData.getRequestParameter(currencyRef);
                if(currencyValue!=null) {
                    currency = new AbstractMap.SimpleEntry<>(currencyRef, currencyValue);
                }
            }
            return reformatValue(this.getPropertyString(FormUtil.PROPERTY_ID), value, FormUtil.findRootForm(this), formData, formatter, currency);
        } catch (Exception e) {
            LogUtil.error(this.getClassName(), e, null);
            return value;
        }
    }

    private JSONObject generateJsonCalculationLoadBinderConf(Form form, FormData formData) {
        try {
            JSONObject requestBody = new JSONObject();
            if(form != null)
                requestBody.put("formDefId", form.getPropertyString("id"));

            requestBody.put("url", WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/plugin/" + getClassName() + "/service");
            requestBody.put("baseUrl", WorkflowUtil.getHttpServletRequest().getContextPath());
            requestBody.put("primaryKey", formData.getPrimaryKeyValue());
            requestBody.put("field", "");

            return requestBody;
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error generating form json");
        }
        return null;
    }

    public String getConfigJson(FormData formData) {
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            JSONObject jsonConfig = new JSONObject();
            jsonConfig.accumulate("equation", this.getPropertyString("equation"));
            jsonConfig.accumulate("fieldId", this.getPropertyString("id"));
            jsonConfig.put("appId", appDef.getAppId());
            jsonConfig.put("appVersion", appDef.getVersion().toString());
            jsonConfig.accumulate("variables", this.getProperty("variables"));
            jsonConfig.put("currencyRefField", getPropertyString("currencyRefField"));
            jsonConfig.put("loadBinder", generateJsonCalculationLoadBinderConf(FormUtil.findRootForm(this), formData));
            return jsonConfig.toString();
        }
        catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public FormRowSet formatData(FormData formData) {
//        boolean dotAsThousands = "euro".equalsIgnoreCase(getPropertyString("style"));
        boolean storeValueUnormatted = "true".equalsIgnoreCase(getPropertyString("storeValueUnormatted"));
        FormRowSet rowSet = super.formatData(formData);

        // convert to default value
        String id = this.getPropertyString(FormUtil.PROPERTY_ID);
        // TODO "Add Validation for Calculation Field"
//        Map<String, Object> variables = (Map<String, Object>) getProperty("variables");


        return rowSet;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        PluginManager pluginManager = (PluginManager) appContext.getBean("pluginManager");
        AppService appService = (AppService) appContext.getBean("appService");

        try {
            JSONObject body = constructRequestBody(request);
            JSONObject data = new JSONObject();
            String formDefId = body.getString("formDefId");
            String fieldId = body.getString("fieldId");
            String primaryKey = body.optString("primaryKey");
            String currencyVal = body.optString("currencyValue");
            Map.Entry<String, String> currencyField = null;

            // Application Definition
            String appId = request.getParameter("appId");
            String appVersion = request.getParameter("appVersion");
            AppDefinition appDef = appService.getAppDefinition(appId, appVersion);
            if(appDef == null) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Application" + appId + "] version [" + appVersion + "] cannot be defined");
            }

            // Form
            Form form = generateForm(formDefId, appDef);
            if (form == null) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Form [" + formDefId + "] cannot be defined");
            }

            // Numeric Field Element
            Element element = FormUtil.findElement(fieldId, form, new FormData());
            if(element == null) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Field [" + fieldId + "] in form [" + formDefId + "] cannot be defined");
            }

            Map<String, Object> calculationLoadBinder = (Map<String, Object>) element.getProperty("calculationLoadBinder");
//            String value = "";

            //add Reference for currency field
            if(currencyVal != null && !currencyVal.isEmpty()) {
                String currencyKey = element.getPropertyString("currencyRefField");
                currencyField = new AbstractMap.SimpleEntry<>(currencyKey, currencyVal);
            }

            JSONObject requestParameters = body.optJSONObject("requestParams");
            Locale locale = getLocale(currencyVal);
            FormData formData = generateFormData(primaryKey, requestParameters, locale);
            BigDecimal value;

            FormLoadBinder loadBinderPlugins = Utilities.getPluginObject(calculationLoadBinder, pluginManager);
            if(loadBinderPlugins != null) {
                // value is calculated by calculation load binder
                element.setLoadBinder(loadBinderPlugins);
                value = executeCalculation(element, formData);
            } else {
                // value from user input
                value = determineNumber(body.optString("formatValue"), locale);
            }

            String formatValue = value.toString();

            Map<String, Object> formatterPlugin = (Map<String, Object>) element.getProperty("formatterPlugin");
            //            DataListColumnFormatDefault formatter = getFormatterPlugin(request.getParameter("appId"), request.getParameter("appVersion"), element);;
            DataListColumnFormatDefault formatter = Utilities.getPluginObject(formatterPlugin, pluginManager);
            if(formatter != null) {
                formatValue = reformatValue(fieldId, value.toString(), form, formData, formatter, currencyField);
            }

            data.put("value", value.doubleValue());
            data.put("mask_value", formatValue);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(data.toString());

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Determine the numeric value of input string
     * @param input input string
     * @return numeric representation
     */
    private BigDecimal determineNumber(String input, Locale locale) {

        try {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
            if (numberFormat == null) {
                LogUtil.warn(getClassName(), "Number format for locale [" + locale.getISO3Language() + "] it not found");
                return new BigDecimal("0");
            }

            try {
                Number number = numberFormat.parse(input);
                return new BigDecimal(number.doubleValue());
            } catch (ParseException e) {
                LogUtil.warn(getClassName(), e.getMessage());
            }

            try {
                String assumptionAsInternationalFormat = input.replaceAll(",", "").replaceAll("\\.", ".");
                Number number = numberFormat.parse(assumptionAsInternationalFormat);
                return new BigDecimal(number.doubleValue());
            } catch (ParseException e) {
                LogUtil.warn(getClassName(), e.getMessage());
            }

            try {
                String assumptionAsDotAsThousands = input.replaceAll("\\.", "").replaceAll(",", ".");
                Number number = numberFormat.parse(assumptionAsDotAsThousands);
                return new BigDecimal(number.doubleValue());
            } catch (ParseException e) {
                LogUtil.warn(getClassName(), e.getMessage());
            }

            LogUtil.warn(getClassName(), "Cannot determine input ["+input+"] with locale [" + locale + "] as numbers");
        } catch (NumberFormatException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }

        return new BigDecimal("0");
    }

    /**
     * Generate FormData based on request
     * @param primaryKey
     * @param requestParameters
     * @param locale
     * @return
     */
    private FormData generateFormData(String primaryKey, JSONObject requestParameters, Locale locale) {
        FormData formData = new FormData();
        formData.setPrimaryKeyValue(primaryKey);
        if(requestParameters != null) {
            Iterator<String> i = requestParameters.keys();
            while (i.hasNext()) {
                String key = i.next();
                String requestValue = requestParameters.optString(key);
                formData.addRequestParameterValues(key, new String[]{ determineNumber(requestValue, locale).toString() });
            }
        }
        return formData;
    }

    private Locale getLocale(String locale) {
        String value = locale == null || locale.isEmpty() ? "en_US" : locale;
        String[] split = value.split("_");
        return new Locale(split[0].trim(), split.length > 1 ? split[1].trim() : "");
    }

//    private DataListColumnFormatDefault getFormatterPlugin(String appId, String appVersionStr, Element element) throws RestApiException, JSONException {
//        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
//
//        if(appId == null || appId.isEmpty()) {
//            throw new RestApiException(HttpServletResponse.SC_FORBIDDEN, "Parameter [appId] not provided");
//        }
//
//        Long appVersion = null;
//        try{
//            appVersion = Long.valueOf(appVersionStr);
//        } catch (NumberFormatException ignored) { }
//
//        AppDefinition appDefinition =  appService.getAppDefinition(appId, appVersionStr);
//        if(appDefinition == null) {
//            throw new RestApiException(HttpServletResponse.SC_NOT_FOUND, "App Definition for appId ["+appId+"] appVersion ["+appVersion+"] not found");
//        }
//
//        Map<String, Object> formatterBinderProps = (Map<String, Object>) element.getProperty("formatterPlugin");
//        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
//        DataListColumnFormatDefault formatter = (DataListColumnFormatDefault) pluginManager.getPlugin((String) formatterBinderProps.get(FormUtil.PROPERTY_CLASS_NAME));
//        if(formatter != null) {
//            formatter.setProperties((Map<String, Object>) formatterBinderProps.get("properties"));
//
//            return formatter;
//        }
//        else
//            return null;
//
//    }

    private DataListColumnFormatDefault generateFormatterPlugin(Map<String, Object> prop) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        if (prop == null)
            return null;

        Map<String, Object> pluginProperties = (Map<String, Object>) prop.get("properties");
        String pluginClassName = prop.get("className").toString();

        DataListColumnFormatDefault formatterPlugin = (DataListColumnFormatDefault) pluginManager.getPlugin(pluginClassName);
        if (formatterPlugin == null)
            return null;

        formatterPlugin.setProperties(pluginProperties);

        return formatterPlugin;
    }

    private String reformatValue(@Nonnull String fieldId, @Nonnull String value, Form form, @Nonnull FormData formData, DataListColumnFormatDefault formatterPlugin, Map.Entry<String,String> currency) {
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        FormRow formRow = null;
        if(formData.getPrimaryKeyValue() != null) {
            formRow = formDataDao.load(form, formData.getPrimaryKeyValue());
        }

        if(formRow == null) {
            formRow = new FormRow();
        }

        if(currency!=null) {
            formRow.put(currency.getKey(), currency.getValue());
        }

        Map<String, String> row = formRow.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(String.valueOf(e.getKey()), String.valueOf(e.getValue())), Map::putAll);

        if(formatterPlugin == null)
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

        if(formattedValue == null) {
            return value;
        } else {
            return formattedValue.replaceAll("<[^>]+>", "");
        }
    }

    protected Form generateForm(String formDefId, AppDefinition appDef) {
        // check in cache

        // proceed without cache
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        Form form = null;
        if (appDef != null && formDefId != null && !formDefId.isEmpty()) {
            FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
            FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
            if (formDef != null) {
                String json = formDef.getJson();
                form = (Form)formService.createElementFromJson(json);

                // put in cache if possible
//                if(formCache != null)
//                    formCache.put(formDefId, form);

                return form;
            }
        }
        return null;
    }


    private JSONObject constructRequestBody(HttpServletRequest request) throws IOException, JSONException {
        try(BufferedReader bf = request.getReader()) {
            return new JSONObject(bf.lines().collect(Collectors.joining()));
        }
    }

    /**
     * Execute calculation load binder
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
            LogUtil.error(getClassName(), e, e.getMessage());
        }

        FormRowSet rowSet = formData.getLoadBinderData(element);
        return Optional.ofNullable(rowSet).map(Collection::stream).orElse(Stream.empty())
                .findFirst()
                .map(row -> row.getProperty(element.getPropertyString(FormUtil.PROPERTY_ID)))
                .map(BigDecimal::new)
                .orElse(new BigDecimal("0"));
    }
}

