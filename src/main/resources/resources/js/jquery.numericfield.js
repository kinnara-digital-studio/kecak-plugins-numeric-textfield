(function($){
    
    $.fn.extend({
        numericField : function(o){
            var target = this;
            if($(target)){
                var form = $(target).parents('form');
                if(!o.readonly || o.readonly != 'true'){
                    if(!(!o.variables || o.variables.length == 0)){
                        $.each(o.variables, function(index){
                            var variable = o.variables[index];
                            var fieldId = variable.fieldId;
//                            if(fieldId.indexOf(".") != -1){
//                                fieldId = fieldId.substring(0, fieldId.indexOf("."));
//                            }
//                            var field = $("[name='" + fieldId + "']");
//                            if ($(field).length == 0) {
//                                field = $("[name$='_" + fieldId + "']");
//                            }
//
//                            FormUtil.getField(fieldId);
//                            $(field).addClass("control-field");
//
//                            $(field).focusout(function(){
//                                numericCalc(target, o);
//                            });

                            var operandField = FormUtil.getField(fieldId);
                            operandField.focusout(function(){
                                numericCalc(target, o);
                            });
                            $('select[name="'+fieldId+'"]').on('change', function() {
                                numericCalc(target, o);
                            });
                            $("input[type='radio']").on('change', function () {
                                numericCalc(target, o);
                            });
                            $("input[type='checkbox']").on('change', function () {
                                numericCalc(target, o);
                            });

                        });
                    } else {
                        var elementPrimary = $(target).parent().find("input:hidden.primary_value");
                        $(target).focusout(function() {
                            elementPrimary.val($(target).val());
                            numericCalc(target, o);

                            elementPrimary.focusout();
                        });
                        $(target).focusin(function() {
                            var value = elementPrimary.val();
                            $(target).val(value);
                        });
                    }

                    if(o.currencyRefField) {
                        FormUtil.getField(o.currencyRefField).change(function() {
                            numericCalc(target, o);
                        });
                    }

                    // run on load 
                    $(document).ready(function() {
                        numericCalc(target, o);
                    });
                    // handle mobile page show
                    $(document).bind("pageshow", function() {
                        numericCalc(target, o);
                    });
                }
            }
            return target;
        }
    });
    
    function numericCalc(target, o){
//        if(o.loadBinder && o.loadBinder.calculationLoadBinder && $(target).attr('data-dontloadajax') == undefined) {
            var loadBinder = o.loadBinder;
            loadBinder.requestParams = {};
            loadBinder.fieldId = o.fieldId;
            var urlParams = "?appId=" + o.appId + "&appVersion=" + o.appVersion;
            $.each(o.variables, function(index){
                var variable = o.variables[index];
                var element = FormUtil.getField(variable.fieldId);
                var operation = variable.operation;
                var values = FormUtil.getValues(variable.fieldId);
                var valIndex = values.length;

                // Checkbox Operation Calculation
                if($("input[type='checkbox']") && operation=="sum"){
                	var sum = 0;
                	for (var i = 0; i < valIndex; i++) {
                        sum += parseInt(values[i]);
                        var value = sum;
                    }
                }
                else if($("input[type='checkbox']") && operation=="min"){
                    var min = Math.min(...values)
                    var value = min;
                }
                else if($("input[type='checkbox']") && operation=="max"){
                    var max = Math.max(...values)
                    var value = max;
                }
                else if($("input[type='checkbox']") && operation=="avg"){
                    var sum = 0;
                    for (var i = 0; i < valIndex; i++) {
                        sum += parseInt(values[i]);
                    }
                    var val = sum/valIndex;
                    var value = val.toFixed();
                }
                else if($("input[type='checkbox']") && operation=="count"){
                    var count = valIndex;
                    var value = count;
                }
                else{
                    var value = FormUtil.getValue(variable.fieldId);
                }
                loadBinder.requestParams[variable.variableName] = value;
//                urlParams += "&";
//                urlParams += variable.variableName + "=" + value;
            });
            var formatValue = $(target).parent().find("input:hidden.primary_value").val();
            if(formatValue) {
                loadBinder.formatValue = formatValue;
            }

            // buat ambil currency
            if(o.currencyRefField) {
                var fieldId = o.currencyRefField;
                var element = FormUtil.getField(fieldId);
                loadBinder.requestParams[fieldId] = FormUtil.getValue(fieldId);
            }

            var imageLoader = $(target).parent().find("img.loader");
            if(imageLoader)
                $(imageLoader).show();

            var submitButton = $(target).parents("form:first").find("input[type='submit']");
            if(submitButton) {
                $(submitButton).prop("disabled", true);
            }

            $.ajax({
                url: loadBinder.url + urlParams,
                type : 'POST',
                headers : { 'Content-Type' : 'application/json' },
                data : JSON.stringify(loadBinder)
            })
            .done(function(data) {
                var result = {};
                if(data) {
                    result.value = data.value ? data.value : 0; // get first data
                    result.mask_value = data.mask_value ? data.mask_value : 0;
                }
                apply(target, result, o);
            })
            .fail(function() {
                var result = {};
                result.value = 0;
                result.mask_value=0;
                apply(target, result, o);
            });
        /*} else {
            var equation = o.equation;
            if(o.equation == ""){
                equation = "0";
            }
            $.each(o.variables, function(){
                var variable = this;
                var value = getValue(variable.fieldId, variable.operation, o.format);
                if(o.equation == ""){
                    equation += " + " + value;
                }else if(variable.variableName != ""){
                    equation = replaceVariable(equation, variable.variableName, value);
                }
            });
            var result = eval(equation);
            apply(target, result, o);
        }*/
    };

    function apply(target, result, o) {
//        var formattedResult  = formatResult(result, o);

        $(target).val(result.mask_value);

        if ($(target).parent().find(".calculationField_value").length > 0) {
            $(target).parent().find(".calculationField_value").text(result.mask_value);
        }
        if ($(target).parent().find("input:hidden.primary_value").length > 0) {
            $(target).parent().find("input:hidden.primary_value").val(result.value);
        }

        $(target).trigger("change");

        var imageLoader = $(target).parent().find("img.loader");
        if(imageLoader)
            $(imageLoader).hide();
        var submitButton = $(target).parents("form:first").find("input[type='submit']");
        if(submitButton) {
            $(submitButton).prop("disabled", false);
        }

        if(o.spellingTarget) {
            var spellingElement = FormUtil.getField(o.spellingTarget);
            spellingElement.val(terbilang(result.value).trim());
            spellingElement.attr("readonly","true");
        }
    }
    
//    function getValue(fieldId, operation, format){
//        if(format.toUpperCase() == "EURO"){
//            $.Calculation.setDefaults({
//                reNumbers: /(-?\s?\$?\s?)(\d+(\.\d{3})*(,\d{1,})?|,\d{1,})/g,
//                cleanseNumber: function (v){
//                	v = v.replace(/\.+/g,'');
//	            	v = v.replace(/\,+/,'.');
//	            	v = v.replace(/\.00+/,'');
//                    return v.replace(/[^0-9,\-]/g, "").replace(/,/g, ".");
//                }
//            });
//        }else{
//            $.Calculation.setDefaults({
//                reNumbers: /(-?\s?\$?\s?)(\d+(,\d{3})*(\.\d{1,})?|\.\d{1,})/g,
//                cleanseNumber: function (v){
//                    return v.replace(/[^0-9.\-]/g, "");
//                }
//            });
//        }
//
//        var value = 0;
//
//        if (fieldId.indexOf(".") != -1) {
//            selector = FormUtil.getGridCells(fieldId).filter(":not(.grid-summary)");
//        } else {
//            selector = FormUtil.getField(fieldId);
//
//            //fix for IE (Grid values should not added even its have same id)
//            selector = $(selector).filter(":not(.grid-cell)");
//        }
//
//        //if radio or checkbox, count oni checked
//        var type = $(selector).attr('type');
//        if (type && (type.toLowerCase() == "radio" || type.toLowerCase() == "checkbox")) {
//           selector = $(selector).filter(":checked");
//        }
//        if(operation.toUpperCase() == "AVG"){
//            value = $(selector).avg();
//        }else if(operation.toUpperCase() == "MIN"){
//            value = $(selector).min();
//        }else if(operation.toUpperCase() == "MAX"){
//            value = $(selector).max();
//        }else if(operation.toUpperCase() == "COUNT"){
//            value = $(selector).length;
//        }else{
//            value = $(selector).sum();
//        }
//
//        if (!isFinite(value)) {
//            value = 0;
//        }
//
//        return value;
//    }
    
//    function replaceVariable(equation, variableName, value){
//        var match = new RegExp(variableName, "ig");
//        return equation.replace(match, value);
//    }
    
//    function formatResult(result, o){
//        var numOfDecimal = parseInt(o.numOfDecimal);
//        var decimalSeperator = ".";
//        var thousandSeparator = ",";
//        if(o.format.toUpperCase() == "EURO"){
//            decimalSeperator = ",";
//            thousandSeparator = ".";
//        }
//
//        var number = result;
//        var exponent = "";
//        if (!isFinite(number)) {
//            number = 0;
//        } else {
//            var numberstr = result.toString();
//            var eindex = numberstr.indexOf("e");
//            if (eindex > -1){
//                exponent = numberstr.substring(eindex);
//                number = parseFloat(numberstr.substring(0, eindex));
//            }
//
//            if (numOfDecimal != null){
//                var temp = Math.pow(10, numOfDecimal);
//                number = Math.round(number * temp) / temp;
//            }
//        }
//
//        var sign = number < 0 ? "-" : "";
//
//        var integer = (number > 0 ? Math.floor (number) : Math.abs (Math.ceil (number))).toString ();
//        var fractional = number.toString ().substring (integer.length + sign.length);
//        fractional = numOfDecimal != null && numOfDecimal > 0 || fractional.length > 1 ? (decimalSeperator + fractional.substring (1)) : "";
//        if(numOfDecimal != null && numOfDecimal > 0){
//            for (i = fractional.length - 1, z = numOfDecimal; i < z; ++i){
//                fractional += "0";
//            }
//        }
//
//        if(o.useThousandSeparator.toUpperCase() == "TRUE"){
//            for (i = integer.length - 3; i > 0; i -= 3){
//                integer = integer.substring (0 , i) + thousandSeparator + integer.substring (i);
//            }
//        }
//
//        var resultString = "";
//        if(sign != ""){
//            resultString += sign;
//        }
//        if(o.prefix != ""){
//            resultString += o.prefix + ' ';
//        }
//        resultString += integer + fractional;
//        if(exponent != ""){
//            resultString += ' ' + exponent;
//        }
//        if(o.postfix != ""){
//            resultString += ' ' + o.postfix;
//        }
//
//        return  resultString;
//    }
})(jQuery);