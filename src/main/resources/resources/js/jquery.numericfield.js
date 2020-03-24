(function($){
   $.fn.extend({
        numericField : function(o){
            let target = this;
            if($(target)){
                var form = $(target).parents('form');

                // not readonly
                if((!o.readonly || o.readonly != 'true')){
                    if(o.variables && o.variables.length != 0){
                        $.each(o.variables, function(index){
                            let variable = o.variables[index];
                            let fieldId = variable.variableName;
                            if(fieldId.indexOf(".") != -1){
                                fieldId = fieldId.substring(0, fieldId.indexOf("."));
                            }

                            let field;
                            if (fieldId.indexOf(".") != -1) {
                                field = FormUtil.getGridCells(fieldId).filter(":not(.grid-summary)");
                            } else {
                                field = FormUtil.getField(fieldId).filter(":not(.grid-cell)");
                            }

                            field.addClass("control-field");

                            const TIMEOUT = 100;

                            if(field.is(":checkbox, :radio, select")) {
                                $(field).on('change', function () {
                                    let $section = $(this).parents('.form-section, .subform-section');
                                    setTimeout(function() {
                                        if($section.is(":visible"))
                                            numericCalc(target, o);
                                    }, TIMEOUT);
                                });
                            } else if(field.is(".calculationField")) {
                                field.on('change', function(){
                                    let $section = $(this).parents('.form-section, .subform-section');
                                    setTimeout(function() {
                                        if($section.is(":visible"))
                                            numericCalc(target,o);
                                    }, TIMEOUT);
                                });
                            } else {
                                field.on('change', function(){
                                    let $section = $(this).parents('.form-section, .subform-section');
                                    setTimeout(function() {
                                        if($section.is(":visible"))
                                            numericCalc(target,o);
                                    }, TIMEOUT);
                                });
                            }

//                            var observer = new MutationObserver(function(mutations) {
//                              mutations.forEach(function(mutation) {
//                                if (mutation.attributeName === "class") {
//                                  var attributeValue = $(mutation.target).prop(mutation.attributeName);
//                                  if($(mutation.target).is(":visible"))
//                                    numericCalc(target,o);
//                                }
//                              });
//                            });
//
//                            $section.each(function(i) {
//                                observer.observe($section[i], {
//                                  attributes: true
//                                });
//                            });
                        });
                    } else {
                        let $section = $(target).closest(".form-cell, .subform-cell");
                        let elementMask = $($section).find("input.mask");
                        let elementPrimary = $($section).find(".primary_value");
                        $(elementMask).focusout(function() {
                            elementPrimary.val($(elementMask).val());
                            numericCalc(target, o);

//                            elementPrimary.focusout();
                        });

                        $(elementMask).focusin(function() {
                            let value = elementPrimary.val();
                            $(elementMask).val(value);
                        });
                    }

                    if(o.currencyRefField) {
                        let currencyRefField = FormUtil.getField(o.currencyRefField);
                        currencyRefField.change(function() {
                            if($(currencyRefField).parents('.form-section, .subform-section').is(":visible"))
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
            let loadBinder = o.loadBinder;
            loadBinder.requestParams = {};
            loadBinder.fieldId = o.fieldId;

            $.each(o.variables, function(index){
                let variable = o.variables[index];
                let fieldId = variable.fieldId;
                let element;

                let value = 0;

                let $selector;
                if(fieldId.indexOf(".") != -1) {
                    $selector = FormUtil.getGridCells(fieldId).filter(":not(.grid-summary)");
                } else {
                    $selector = FormUtil.getField(fieldId);

                    //fix for IE (Grid values should not added even its have same id)
                    $selector = $selector.filter(":not(.grid-cell)");
                }

                if($selector.length > 0 && !$selector.closest(".form-section, .subform-section").hasClass("section-visibility-hidden")) {

                    //if radio or checkbox, count only checked
                    var type = $selector.attr('type');
                    if (type != undefined && (type.toLowerCase() == "radio" || type.toLowerCase() == "checkbox")) {
                       $selector = $selector.filter(":checked");
                    }
                    var operation = variable.operation;
                    if (operation == "avg") {
                        value = $selector.avg();
                    } else if (operation== "min") {
                        value = $selector.min();
                    } else if (operation == "max") {
                        value = $selector.max();
                    } else if( operation == "count") {
                        value = $selector.length;
                    } else if (operation == "sum") {
                        value = $selector.sum();
                    } else {
                        $selector.each((i, e) => value = e.value);
                    }

                    if ($.isNumeric(value) && !isFinite(value)) {
                        value = 0;
                    }

                    loadBinder.requestParams[variable.variableName] = value;
                } else {
                    loadBinder.requestParams[variable.variableName] = 0;
                }
            });

            var formatValue = $(target).closest(".form-cell, .subform-cell").find(".primary_value").val();
            if(formatValue) {
                loadBinder.formatValue = formatValue;
            }

            // buat ambil currency
            if(o.currencyRefField) {
                var fieldId = o.currencyRefField;
                var element = FormUtil.getField(fieldId);
                loadBinder.requestParams[fieldId] = FormUtil.getValue(fieldId);
            }

            var imageLoader = $(target).closest(".form-cell, .subform-cell").find("img.loader");
            if(imageLoader)
                $(imageLoader).show();

            var submitButton = $(target).closest("form:first").find("input[type='submit']");
            if(submitButton) {
                $(submitButton).prop("disabled", true);
            }

            $.ajax({
                url: loadBinder.url,
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
        let $elementBlock = $(target).closest("div");
        let $elementMask = $elementBlock.find(".mask");
        let $elementPrimary =  $elementBlock.find(".primary_value");

//        if ($(target).parent().find(".calculationField_value").length > 0) {
//            $(target).parent().find(".calculationField_value").text(result.mask_value);
//        }

        $elementMask.val(result.mask_value);
        $elementPrimary.val(result.value);
        $elementPrimary.trigger("change");

        let imageLoader = $elementBlock.find("img.loader");
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