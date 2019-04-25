<div class="form-cell" ${elementMetaData!}>
    <#if includeMetaData!>
        <span class="form-floating-label">NUMERIC FIELD</span>
    <#elseif element.properties.readonly! != 'true'>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/jquery.numericfield.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/terbilang-spelling.js"/></script>
        <script type="text/javascript">
            var options_calField_${element.properties.elementUniqueKey} = ${configJson!};
            $(document).ready(function(){
                $('.calFiel_${element.properties.elementUniqueKey!}').numericField(options_calField_${element.properties.elementUniqueKey});

                var val = $("#${elementParamName!}${element.properties.elementUniqueKey!}").val();

                $("#${elementParamName!}${element.properties.elementUniqueKey!}").change(function(){
                    /*if(format=="euro") {
                        amount = $(this).val();
                        amount = amount.replace(/[^0-9,]/g,'');
                    } else {
                        amount = $(this).val();
                        amount = amount.replace(/[^0-9\.]/g,'');
                    }*/
                    var amount = $(this).val();

                    <!-- ${element.properties.spellingTarget!} -->
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
                });
            });
        </script>
    </#if>

    <#if element.properties.readonly! == 'true'>
        <#assign attrDontLoadAjax = 'data-dontloadajax' >
    <#else>
        <#assign attrDontLoadAjax = '' >
    </#if>

    <#if element.properties.hidden! == 'true'>
        <#if includeMetaData!>
            <label class="label">${element.properties.id}</label>
            <input id="${elementParamName!}${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="text" value="${value!?html}" readonly style="text-align: right;" ${attrDontLoadAjax} />
        <#else>
            <input id="${elementParamName!}${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="calFiel_${element.properties.elementUniqueKey!}" ${attrDontLoadAjax} />
        </#if>
    <#else>
        <label class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
        <#if element.properties.readonlyLabel! == 'true' || element.properties.readonly! == 'true'>
            <span class="calculationField_value">${formattedValue!?html}</span>
            <input id="${elementParamName!}${element.properties.elementUniqueKey!}_mask" name="${elementParamName!}_mask" type="hidden" value="${value!?html}" class="calFiel_${element.properties.elementUniqueKey!}" ${attrDontLoadAjax} />
        <#else>
            <input id="${elementParamName!}${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="primary_value" readonly style="text-align: ${element.properties.alignment};"/>
            <input id="${elementParamName!}${element.properties.elementUniqueKey!}_mask" name="${elementParamName!}_mask" type="text" value="${formattedValue!?html}" <#if element.properties.calculationLoadBinder.className?? && element.properties.calculationLoadBinder.className != "">readonly</#if> style="text-align: ${element.properties.alignment};" class="calFiel_${element.properties.elementUniqueKey!}" ${attrDontLoadAjax} />
        </#if>
        <img id="${elementParamName!}${element.properties.elementUniqueKey!}_loading" class="loader" src="${request.contextPath}/plugin/${className}/images/spin.gif" height="24" width="24" style="vertical-align: middle; display: none;">
    </#if>
</div>