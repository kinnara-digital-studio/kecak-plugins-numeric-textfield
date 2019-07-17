(function($){
    $.fn.extend({
        numericField : function(o){
            var target = this;
            if($(target)){
                var form = $(target).parents('form');
                if(!o.readonly || o.readonly != 'true'){
                    if(o.variables && o.variables.length != 0){
                        $.each(o.variables, function(index){
                            var variable = o.variables[index];
                            var fieldId = variable.fieldId;
                            if(fieldId.indexOf(".") != -1){
                                fieldId = fieldId.substring(0, fieldId.indexOf("."));
                            }

                            var field;
                            if (fieldId.indexOf(".") != -1) {
                                field = FormUtil.getGridCells(fieldId).filter(":not(.grid-summary)");
                            } else {
                                field = FormUtil.getField(fieldId);

                                //fix for IE (Grid values should not added even its have same id)
                                field = field.filter(":not(.grid-cell)");
                            }

                            field.addClass("control-field");

                            if(field.is("checkbox, radio, select")) {
                                $(field).on('change', function () {
                                    numericCalc(target, o);
                                });
                            } else if(field.is(".primary_value")) {
                                field.on('change', function(){
                                    numericCalc(target,o);
                                });
                            } else {
                                field.change(function(){
                                    numericCalc(target,o);
                                });
                            }
                        });
                    } else {
                        var elementPrimary = $(target).closest(".form-cell").find(".primary_value");
                        $(target).focusout(function() {
                            elementPrimary.val($(target).val());
                            numericCalc(target, o);

//                            elementPrimary.focusout();
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
        if(!$(target).closest("div").find('input[data-donttrigger*="true"]').length) {
            var loadBinder = o.loadBinder;
            loadBinder.requestParams = {};
            loadBinder.fieldId = o.fieldId;
            var urlParams = "?appId=" + o.appId + "&appVersion=" + o.appVersion;
            $.each(o.variables, function(index){
                var variable = o.variables[index];
                var fieldId = variable.fieldId;
                var element;

                var value = 0;

                if(fieldId.indexOf(".") != -1) {
                    selector = FormUtil.getGridCells(fieldId).filter(":not(.grid-summary)");
                } else {
                    selector = FormUtil.getField(fieldId);

                    //fix for IE (Grid values should not added even its have same id)
                    selector = $(selector).filter(":not(.grid-cell)");
                }

                //if radio or checkbox, count oni checked
                var type = $(selector).attr('type');
                if (type != undefined && (type.toLowerCase() == "radio" || type.toLowerCase() == "checkbox")) {
                   selector = $(selector).filter(":checked");
                }
                var operation = variable.operation;
                if (operation == "avg") {
                    value = $(selector).avg();
                } else if (operation== "min") {
                    value = $(selector).min();
                } else if (operation == "max") {
                    value = $(selector).max();
                } else if( operation == "count") {
                    value = $(selector).length;
                } else if (operation == "sum") {
                    value = $(selector).sum();
                } else {
                    value = "";
                    $(selector).each((i, e) => value += e.value);
                }

                if ($.isNumeric(value) && !isFinite(value)) {
                    value = 0;
                }

                loadBinder.requestParams[variable.variableName] = value;
            });

            var formatValue = $(target).closest(".form-cell").find(".primary_value").val();
            if(formatValue) {
                loadBinder.formatValue = formatValue;
            }

            // buat ambil currency
            if(o.currencyRefField) {
                var fieldId = o.currencyRefField;
                var element = FormUtil.getField(fieldId);
                loadBinder.requestParams[fieldId] = FormUtil.getValue(fieldId);
            }

            var imageLoader = $(target).closest(".form-cell").find("img.loader");
            if(imageLoader)
                $(imageLoader).show();

            var submitButton = $(target).closest("form:first").find("input[type='submit']");
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
                var result = {
                    value: 0,
                    mask_value: 0
                };
                apply(target, result, o);
            });
        }
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
        var elementBlock = $(target).closest("div");
        $(target).val(result.mask_value);

        if ($(target).parent().find(".calculationField_value").length > 0) {
            $(target).parent().find(".calculationField_value").text(result.mask_value);
        }

        elementBlock.find(".primary_value").val(result.value);
        elementBlock.find(".primary_value").trigger("change");

        var imageLoader = elementBlock.find("img.loader");
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
})(jQuery);