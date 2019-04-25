package com.kinnara.kecakplugins;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

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
//        try {
//            boolean dotAsThousands = "euro".equalsIgnoreCase(getPropertyString("style"));
//            return Stream.of(value)
//                    .map(s -> {
//                        if(dotAsThousands)
//                            return s.replaceAll("\\.", "").replaceAll(",", ".");
//                        else
//                            return s.replaceAll(",", "");
//                    })
//                    .filter(s -> !s.isEmpty())
//                    .map(BigDecimal::new)
//                    .map(BigDecimal::doubleValue)
//                    .map(d -> String.format("%,." + getPropertyString("numOfDecimal") + "f", d))
//                    .map(s -> dotAsThousands ? s.replaceAll(",", "_").replaceAll("\\.", ",").replaceAll("_", ".") : s)
//                    .findFirst()
//                    .orElse("");
//        } catch (NumberFormatException e) {
//            LogUtil.info(getClassName(), "Number format exception for value ["+value+"]");
//            return value;
//        }
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            DataListColumnFormatDefault formatter = getFormatterPlugin(appDef.getAppId(), appDef.getVersion().toString(), this);
            return reformatValue(this.getPropertyString(FormUtil.PROPERTY_ID), value, FormUtil.findRootForm(this), formData, formatter);
        } catch (Exception e) {
            LogUtil.error(this.getClassName(), e, null);
            return value;
        }
    }

    private JSONObject generateJsonCalculationLoadBinderConf(Form form, FormData formData) {
        try {
            JSONObject requestBody = new JSONObject();
//            Map<String, Object> calculationLoadBinder = (Map<String, Object>)getProperty("calculationLoadBinder");
//            if(calculationLoadBinder != null)
//                requestBody.put("calculationLoadBinder", "true");

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
//            jsonConfig.accumulate("format", this.getPropertyString("style"));
//            jsonConfig.accumulate("numOfDecimal", this.getPropertyString("numOfDecimal"));
//            jsonConfig.accumulate("prefix", this.getPropertyString("prefix"));
//            jsonConfig.accumulate("postfix", this.getPropertyString("postfix"));
//            jsonConfig.accumulate("useThousandSeparator", this.getPropertyString("useThousandSeparator"));
            jsonConfig.accumulate("variables", this.getProperty("variables"));
            jsonConfig.put("loadBinder", generateJsonCalculationLoadBinderConf(FormUtil.findRootForm(this), formData));
            return jsonConfig.toString();
        }
        catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public FormRowSet formatData(FormData formData) {
        boolean dotAsThousands = "euro".equalsIgnoreCase(getPropertyString("style"));
        boolean storeValueUnormatted = "true".equalsIgnoreCase(getPropertyString("storeValueUnormatted"));
        FormRowSet rowSet = super.formatData(formData);

        // convert to default value
        String id = this.getPropertyString(FormUtil.PROPERTY_ID);

        for(FormRow row : rowSet) {
            String value = row.getProperty(id);
            if(!storeValueUnormatted) {
                row.setProperty(id, value);
            } else if(dotAsThousands) {
                // remove thousands delimiter and convert decimal delimiter
                row.setProperty(id, value.replaceAll("\\.", "").replaceAll(",", "."));
            } else {
                // remove thousands delimiter
                row.setProperty(id, value.replaceAll(",", ""));
            }
        }

        return rowSet;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        if("POST".equals(request.getMethod())) {
//            String action = request.getParameter("action");
//            try {
//                if (action != null && !action.isEmpty()) {
//                    Method method = this.getClass().getMethod(action, HttpServletRequest.class, HttpServletResponse.class);
//                    method.invoke(this, request, response);
//                }
//            } catch (NoSuchMethodException e) {
//                String messages = "Action cannot be null";
//                LogUtil.warn(getClassName(), messages);
//                response.sendError(HttpServletResponse.SC_BAD_REQUEST, messages);
//            } catch (IllegalAccessException e) {
//                LogUtil.error(this.getClassName(), e, null);
//                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error occurred, please check log for details");
//            } catch (InvocationTargetException e) {
//                LogUtil.error(this.getClassName(), e, null);
//                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error occurred, please check log for details");
//            }
//        } else {
//            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
//        }

        ApplicationContext appContext = AppUtil.getApplicationContext();
        try {
            JSONObject body = constructRequestBody(request);
            JSONObject data = new JSONObject();
//            Map<String, Object> loadBinderProps = (Map<String, Object>) this.getProperty("calculationLoadBinder");
//            JSONObject jsonLoadBinder = body.getJSONObject("calculationLoadBinder");
            String formDefId = body.getString("formDefId");
            String fieldId = body.getString("fieldId");
            String primaryKey = body.optString("primaryKey");

            // build form
//            FormService formService = (FormService) appContext.getBean("formService");
            Form form = generateForm(formDefId);
            Element element = FormUtil.findElement(fieldId, form, new FormData());
            Map<String, Object> loadBinderProps = (Map<String, Object>) element.getProperty("calculationLoadBinder");
            String value = "";

            if(loadBinderProps != null && loadBinderProps.get(FormUtil.PROPERTY_CLASS_NAME) != null) {
//                LogUtil.info(this.getClassName(), "=== CALC LOAD BINDER PROPS ===");
//                loadBinderProps.forEach((k, v) -> {LogUtil.info(getClassName(), k + ", " + v);});
//                LogUtil.info(this.getClassName(), "=== END PROPS ===");

                JSONObject requestParameters = body.getJSONObject("requestParams");

                PluginManager pluginManager = (PluginManager) appContext.getBean("pluginManager");
                FormBinder loadBinder = (FormBinder) pluginManager.getPlugin((String) loadBinderProps.get(FormUtil.PROPERTY_CLASS_NAME));

                if (form == null) {
                    String message = "Form cannot be defined";
                    LogUtil.warn(getClassName(), message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
                    return;
                }

                if (loadBinder == null) {
                    String message = "Load Binder cannot be defined";
                    LogUtil.warn(getClassName(), message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
                    return;
                }


                try {
                    loadBinder.setProperties((Map<String, Object>) loadBinderProps.get("properties"));
                    element.setLoadBinder((FormLoadElementBinder) loadBinder);
                } catch (Exception e) {
                    LogUtil.error(getClassName(), e, "Error configuring load binder");
                }

                value = loadFormData(element, primaryKey, requestParameters);

            }
            FormData formData = new FormData();
            formData.setPrimaryKeyValue(primaryKey);
            DataListColumnFormatDefault formatter = getFormatterPlugin(request.getParameter("appId"), request.getParameter("appVersion"), element);
            String formatValue = "";
            if(formatter!=null) {
                if (!body.isNull("formatValue") && value.isEmpty()) {
                    value = body.getString("formatValue");
                    formatValue = reformatValue(fieldId, body.getString("formatValue"), form, formData, formatter);
                } else {
                    formatValue = reformatValue(fieldId, value, form, formData, formatter);
                }
            }
            else
                formatValue = value;

            data.put("value", value);
            data.put("mask_value", formatValue);


            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(data.toString());

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private DataListColumnFormatDefault getFormatterPlugin(String appId, String appVersionStr, Element element) throws RestApiException, JSONException {


        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");

        if(appId == null || appId.isEmpty()) {
            throw new RestApiException(HttpServletResponse.SC_FORBIDDEN, "Parameter [appId] not provided");
        }

        Long appVersion = null;
        try{
            appVersion = Long.valueOf(appVersionStr);
        } catch (NumberFormatException ignored) { }

        if(appVersion == null) {
            // get published version
            appVersion = appDefinitionDao.getPublishedVersion(appId);
        }

        if(appVersion == null) {
            throw new RestApiException(HttpServletResponse.SC_NOT_FOUND, "Published version of appId ["+appId+"] cannot be defined");
        }

        AppDefinition appDefinition = appDefinitionDao.loadVersion(appId, appVersion);
        if(appDefinition == null) {
            throw new RestApiException(HttpServletResponse.SC_NOT_FOUND, "App Definition for appId ["+appId+"] appVersion ["+appVersion+"] not found");
        }

//            JSONObject body;
//            try {
//                body = constructRequestBody(request);
//            } catch (IOException | JSONException e) {
//                throw new RestApiException(HttpServletResponse.SC_NOT_ACCEPTABLE, "Error parsing HTTP body");
//            }

//            FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
//            Form form = (Form)formService.createElementFromJson(body.optString("form"));
//        if(form == null) {
//            throw new RestApiException(HttpServletResponse.SC_NOT_ACCEPTABLE, "Error generating form");
//        }
//
//            String value = body.optString("value");
//        if(value == null) {
//            throw new RestApiException(HttpServletResponse.SC_NOT_ACCEPTABLE, "Value not found");
//        }

//            JSONObject jsonFormatter = body.getJSONObject("formatter");
//            Map<String, Object> properties = FormUtil.parsePropertyFromJsonObject(jsonFormatter);
        Map<String, Object> formatterBinderProps = (Map<String, Object>) element.getProperty("formatterPlugin");
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        DataListColumnFormatDefault formatter = (DataListColumnFormatDefault) pluginManager.getPlugin((String) formatterBinderProps.get(FormUtil.PROPERTY_CLASS_NAME));
        if(formatter != null) {
            formatter.setProperties((Map<String, Object>) formatterBinderProps.get("properties"));

//        String fieldId = element.getPropertyString(FormUtil.PROPERTY_ID);
//
//        FormData formData = new FormData();
//        formData.setPrimaryKeyValue(primaryKey);
//        String reformattedValue = reformatValue(fieldId, value, form, formData, formatter);

//            response.getWriter().write(jsonResult.toString());
            return formatter;
        }
        else
            return null;

    }

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

    private String reformatValue(@Nonnull String fieldId, @Nonnull String value, Form form, @Nonnull FormData formData, DataListColumnFormatDefault formatterPlugin) {
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        FormRow formRow = null;
        if(formData.getPrimaryKeyValue() != null) {
            formRow = formDataDao.load(form, formData.getPrimaryKeyValue());
        }

        if(formRow == null) {
            formRow = new FormRow();
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
//        } else if ("true".equalsIgnoreCase(getPropertyString(FormUtil.PROPERTY_READONLY)) && "true".equalsIgnoreCase(getPropertyString(FormUtil.PROPERTY_READONLY_LABEL))) {
//            return formattedValue;
        } else {
            return formattedValue.replaceAll("<[^>]+>", "");
        }
    }

    protected Form generateForm(String formDefId) {
        // check in cache
//        if(formCache != null && formCache.containsKey(formDefId))
//            return formCache.get(formDefId);

        // proceed without cache
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        Form form = null;
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
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

    private String loadFormData(Element element, String primaryKey, JSONObject jsonRequestParameter) throws JSONException {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormService formService = (FormService) appContext.getBean("formService");

        FormData formData = new FormData();
        formData.setPrimaryKeyValue(primaryKey);

        Iterator i = jsonRequestParameter.keys();
        while (i.hasNext()) {
            String key = i.next().toString();
            try {
                String value = jsonRequestParameter.getString(key);
                formData.addRequestParameterValues(key, new String[]{value==null || value.isEmpty()? "0" : value});
            } catch (JSONException e) {
                LogUtil.warn(getClassName(), "JSON Exception for key parameter [" + key + "]");
            }
        }

        try {
            formData = formService.executeFormLoadBinders(element, formData);
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }

        FormRowSet rowSet = formData.getLoadBinderData(element);
        if(!rowSet.isEmpty()) {
            return rowSet.get(0).get(element.getPropertyString(FormUtil.PROPERTY_ID)).toString();
        }
        else
            return "";
//        return formRowSetToJson(rowSet, element.getPropertyString(FormUtil.PROPERTY_ID));
    }

//    private JSONObject formRowSetToJson(FormRowSet data, String fieldId) throws JSONException {
//        JSONObject result = new JSONObject();
//        if(data != null) {
//            for(FormRow row : data) {
//                result.put("value", row.get(fieldId));
//            }
//        }
//        return result;
//    }
}

