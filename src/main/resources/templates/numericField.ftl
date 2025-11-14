<div class="form-cell" ${elementMetaData!}>
    <#if includeMetaData!>
        <label class="label">${element.properties.label}</label>
        <span class="form-floating-label">NUMERIC FIELD</span>
        <input id="${elementParamName!}" name="${elementParamName!}" type="text" value="${value!?html}" readonly />

    <#else>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/jquery.calculation.min.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/jquery.numericfield.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/terbilang-spelling.js"/></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/decimal-terbilang-spelling.js"/></script>
        <!-- <script type="text/javascript" src="${request.contextPath}/plugin/${className}/node_modules/globalize/dist/globalize.js"></script> -->

        <#if element.properties.readonly! == 'true'>
            <#assign attrDontTrigger = 'readonly data-donttrigger="true"' >
        <#else>
            <#assign attrDontTrigger = '' >
            <script type="text/javascript">
                let options_calField_${element.properties.elementUniqueKey} = ${configJson!};
                $(document).ready(function(){
                    $('#${elementParamName!}${element.properties.elementUniqueKey!}').numericField(options_calField_${element.properties.elementUniqueKey});
                });
            </script>
        </#if>

        <input id="${elementParamName!}${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="calculationField primary_value calField_${element.properties.elementUniqueKey!}"/>

        <#if element.properties.hidden! != 'true'>
            <label class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
            <#if element.properties.readonlyLabel! == 'true' && element.properties.readonly! == 'true'>
                <#-- <input id="${elementParamName!}${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="primary_key" ${attrDontTrigger} /> -->
                <span class="calculationField_value">${formattedValue!?html}</span>
            <#else>
                <input id="${elementParamName!}${element.properties.elementUniqueKey!}_mask" name="${elementParamName!}_mask" value="${formattedValue!?html}" <#if element.properties.readonly! == 'true' || element.properties.calculationLoadBinder.className?? && element.properties.calculationLoadBinder.className != "">readonly</#if> style="text-align: right;" class="mask" autocomplete="off" ${attrDontTrigger} />
            </#if>
            <img id="${elementParamName!}${element.properties.elementUniqueKey!}_loading" class="loader" src="${request.contextPath}/plugin/${className}/images/spin.gif" height="16" width="16" style="vertical-align: middle; display: none;">
        </#if>
    </#if>
</div>