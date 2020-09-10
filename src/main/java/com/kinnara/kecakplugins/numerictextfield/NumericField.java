package com.kinnara.kecakplugins.numerictextfield;

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
import javax.annotation.Nullable;
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

public class NumericField extends Element implements FormBuilderPaletteElement, PluginWebSupport, AceFormElement, AdminLteFormElement {
    public String getName() {
        return getLabel() + getVersion();
    }

    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    public String getLabel() {
        return "Numeric Field";
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(this.getClass().getName(), "/properties/numericField.json", new String[] {EquationLoadBinder.class.getName()}, true, "messages/numericField");
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
        return renderTemplate(template, formData, dataModel);
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
            return reformatValue(this.getPropertyString(FormUtil.PROPERTY_ID), value, FormUtil.findRootForm(this), formData, formatter);
        } catch (Exception e) {
            return value;
        }
    }

    private JSONObject generateJsonCalculationLoadBinderConf(Form form, FormData formData) {
        try {
            JSONObject requestBody = new JSONObject();
            if(form != null)
                requestBody.put("formDefId", form.getPropertyString("id"));


            AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

            requestBody.put("url", WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/app/" + appDefinition.getAppId() + "/" +appDefinition.getVersion() + "/plugin/" + getClassName() + "/service");
            requestBody.put("baseUrl", WorkflowUtil.getHttpServletRequest().getContextPath());
            requestBody.put("primaryKey", formData.getPrimaryKeyValue());
            requestBody.put("field", "");

            return requestBody;
        } catch (Exception e) {
        }
        return null;
    }

    public String getConfigJson(FormData formData) {
        Form rootForm = FormUtil.findRootForm(this);
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            JSONObject jsonConfig = new JSONObject();
            jsonConfig.accumulate("equation", getPropertyString("equation"));
//            jsonConfig.accumulate("fieldId", getPropertyString("id"));
            jsonConfig.accumulate("fieldId", getFullFieldPath(rootForm, this));
            jsonConfig.put("appId", appDef.getAppId());
            jsonConfig.put("appVersion", appDef.getVersion().toString());
            jsonConfig.accumulate("variables", getProperty("variables"));
            jsonConfig.put("currencyRefField", getPropertyString("currencyRefField"));
            jsonConfig.put("loadBinder", generateJsonCalculationLoadBinderConf(rootForm, formData));
            jsonConfig.put("spellingTarget", getPropertyString("spellingTarget"));
            return jsonConfig.toString();
        }
        catch (Exception e) {
            return "{}";
        }
    }

    private String getFullFieldPath(@Nonnull Form form, @Nonnull Element element) {
        return getFullFieldPathRec(form, element, "");
    }

    private String getFullFieldPathRec(@Nonnull Form form, Element element, @Nonnull String trailingPath) {
        if(element == null || form == element) {
            return trailingPath;
        }

        trailingPath = Stream.concat(
            Stream.of(element)
                    .map(e -> e.getPropertyString(FormUtil.PROPERTY_ID))
                    .filter(Objects::nonNull)
                    .filter(s -> !s.isEmpty()),

            Stream.of(trailingPath).filter(s -> !s.isEmpty())
        ).collect(Collectors.joining("."));
        return getFullFieldPathRec(form, element.getParent(), trailingPath);
    }

    private Element getElement(@Nonnull Form form, String fullPath) {
        return getElementRec(form, fullPath, "");
    }

    @Nullable
    private Element getElementRec(Element element, @Nonnull final String fullPath, @Nonnull final String path) {
        if(element == null) {
            return null;
        }

        if(fullPath.equalsIgnoreCase(path)) {
            return element;
        }

        return Optional.of(element)
                .map(Element::getChildren)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(child -> {
                    final String currentPath = Stream.concat(
                        Stream.of(path).filter(s -> !s.isEmpty()),
                        Stream.of(child).map(e -> e.getPropertyString(FormUtil.PROPERTY_ID)).filter(Objects::nonNull).filter(s -> !s.isEmpty())
                    ).collect(Collectors.joining("."));
                    return getElementRec(child, fullPath, currentPath);
                })
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    @Override
    public FormRowSet formatData(FormData formData) {
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

        try {
            JSONObject body;
            String formDefId;
            String fieldId;
            String primaryKey;

            try {
                body = constructRequestBody(request);
                formDefId = body.getString("formDefId");
                fieldId = body.getString("fieldId");
                primaryKey = body.optString("primaryKey");
            } catch (JSONException e) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, e);
            }

            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            if(appDef == null) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Application definition cannot be defined");
            }

            // Form
            Form form = generateForm(formDefId, appDef);
            if (form == null) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Form [" + formDefId + "] cannot be defined");
            }

            // Numeric Field Element
//            Element element = FormUtil.findElement(fieldId, form, new FormData());
            Element element = getElement(form, fieldId);
            if(element == null) {
                throw new RestApiException(HttpServletResponse.SC_BAD_REQUEST, "Field [" + fieldId + "] in form [" + formDefId + "] cannot be defined");
            }

            String currencyField = element.getPropertyString("currencyRefField");

            Map<String, Object> calculationLoadBinder = (Map<String, Object>) element.getProperty("calculationLoadBinder");
            JSONObject requestParameters = body.optJSONObject("requestParams");
            Locale locale = getLocale(requestParameters.optString(currencyField));
            FormData formData = generateFormData(element, primaryKey, requestParameters, locale);

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

            Map<String, Object> formatterPlugin = (Map<String, Object>) element.getProperty("formatterPlugin");
            DataListColumnFormatDefault formatter = Utilities.getPluginObject(formatterPlugin, pluginManager);
            String formattedValue = formatter == null ? value.toString()
                    : reformatValue(fieldId, value.toString(), form, formData, formatter);

            JSONObject data = new JSONObject();
            data.put("value", value.doubleValue());
            data.put("mask_value", formattedValue);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(data.toString());

        } catch (RestApiException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            response.sendError(e.getErrorCode(), e.getMessage());
        } catch (JSONException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
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
                return new BigDecimal("0");
            }

            try {
                Number number = numberFormat.parse(input);
                return new BigDecimal(number.doubleValue());
            } catch (ParseException e) {
            }

            try {
                String assumptionAsInternationalFormat = input.replaceAll(",", "").replaceAll("\\.", ".");
                Number number = numberFormat.parse(assumptionAsInternationalFormat);
                return new BigDecimal(number.doubleValue());
            } catch (ParseException e) {
            }

            try {
                String assumptionAsDotAsThousands = input.replaceAll("\\.", "").replaceAll(",", ".");
                Number number = numberFormat.parse(assumptionAsDotAsThousands);
                return new BigDecimal(number.doubleValue());
            } catch (ParseException e) {
            }

        } catch (NumberFormatException e) {
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
    private FormData generateFormData(Element element, String primaryKey, JSONObject requestParameters, Locale locale) {
        String currencyField = element.getPropertyString("currencyRefField");
        FormData formData = new FormData();
        formData.setPrimaryKeyValue(primaryKey);
        if(requestParameters != null) {
            Iterator<String> i = requestParameters.keys();
            while (i.hasNext()) {
                String key = i.next();
                String requestValue = requestParameters.optString(key);
                formData.addRequestParameterValues(key, new String[]{ key.equals(currencyField) ? requestValue : requestValue });
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

    private String renderTemplate(String template, FormData formData, @SuppressWarnings("rawtypes") Map dataModel){
        String value = FormUtil.getElementPropertyValue(this, formData);
        dataModel.put("value", value);
        dataModel.put("formattedValue", formatValue(value, formData));
        dataModel.put("configJson", getConfigJson(formData));
        dataModel.put("className", getClassName());
        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    @Override
    public String renderAceTemplate(FormData formData, Map map) {
        String template = "AceNumericField.ftl";
        return renderTemplate(template, formData, map);
    }

    @Override
    public String renderAdminLteTemplate(FormData formData, Map map) {
        String template = "AdminLteNumericField.ftl";
        return renderTemplate(template, formData, map);
    }
}

