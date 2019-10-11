<div class="form-cell" ${elementMetaData!}>
    <#if includeMetaData!>
        <label class="control-label">${element.properties.label}</label>
        <span class="form-floating-label">NUMERIC FIELD</span>
        <input id="${elementParamName!}" name="${elementParamName!}" type="text" value="${value!?html}" readonly />

    <#else>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/jquery.calculation.min.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/jquery.numericfield.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/terbilang-spelling.js"/></script>
        <!-- <script type="text/javascript" src="${request.contextPath}/plugin/${className}/bower_components/globalize/dist/globalize.js"></script> -->

        <#if element.properties.readonly! == 'true'>
            <#assign attrDontTrigger = 'readonly data-donttrigger="true"' >
        <#else>
            <#assign attrDontTrigger = '' >
            <script type="text/javascript">
                var options_calField_${element.properties.elementUniqueKey} = ${configJson!};
                $(document).ready(function(){
                    $('.calFiel_${element.properties.elementUniqueKey!}').numericField(options_calField_${element.properties.elementUniqueKey});

                    var val = $("#${elementParamName!}${element.properties.elementUniqueKey!}").val();

                    $("#${elementParamName!}${element.properties.elementUniqueKey!}").change(function(){
                        var amount = $(this).val();

                        <!-- ${element.properties.spellingTarget!} -->
                        <!--
                        <#if (element.properties.spellingTarget! != '') >
                            amount = amount.replace(/\.+/g,'');
                            amount = amount.replace(/\,+/,'.');
                            amount = amount.replace(/\.00+/,'');

                            var spelled = terbilang(amount).trim();
                            var fieldSpellingTarget = FormUtil.getField("${element.properties.spellingTarget!}");
                            if(fieldSpellingTarget && spelled){
                                $(fieldSpellingTarget).val(spelled);
                            }
                            $("#${element.properties.spellingTarget!}").attr("readonly","true");
                        </#if>
                        -->
                    });
                });
            </script>
        </#if>

        <input id="${elementParamName!}${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="primary_value"/>

        <#if element.properties.hidden! != 'true'>
            <label class="control-label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
            <#if element.properties.readonlyLabel! == 'true' && element.properties.readonly! == 'true'>
                <input id="${elementParamName!}${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="primary_key" ${attrDontTrigger} />
                <span class="calculationField_value">${formattedValue!?html}</span>
            <#else>
                <input id="${elementParamName!}${element.properties.elementUniqueKey!}_mask" name="${elementParamName!}_mask"                 value="${formattedValue!?html}" <#if element.properties.readonly! == 'true' || element.properties.calculationLoadBinder.className?? && element.properties.calculationLoadBinder.className != "">readonly</#if> style="text-align: right;" class="calFiel_${element.properties.elementUniqueKey!} form-control" ${attrDontTrigger} />
            </#if>
            <img id="${elementParamName!}${element.properties.elementUniqueKey!}_loading" class="loader" src="${request.contextPath}/plugin/${className}/images/spin.gif" height="16" width="16" style="vertical-align: middle; display: none;">
        </#if>
    </#if>
</div>