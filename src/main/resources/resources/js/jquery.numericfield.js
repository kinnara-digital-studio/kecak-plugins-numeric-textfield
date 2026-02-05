(function($){
   $.fn.extend({
        numericField : function(o){
            let target = this;
            if($(target)){
                var form = $(target).parents('form');

                // not readonly
                if((!o.readonly || o.readonly != 'true')){
                    // numeric field is the result of some equation from other fields
                    // in this case, the numeric field is supposed to be readonly
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
                                field = target.getField(fieldId).filter(":not(.grid-cell)");
                            }

                            field.addClass("control-field");

                            const TIMEOUT = 100;

                            if(field.is(":checkbox, :radio, select")) {
                                $(field).on('change', function () {
                                    let $section = $(this).parents('.form-section, .subform-section');
                                    setTimeout(function() {
                                        if($section.is(":visible"))
                                            target.numericCalc(target, o);
                                    }, TIMEOUT);
                                });
                            } else if(field.is(".calculationField")) {
                                field.on('change', function(){
                                    let $section = $(this).parents('.form-section, .subform-section');
                                    setTimeout(function() {
                                        if($section.is(":visible"))
                                            target.numericCalc(target,o);
                                    }, TIMEOUT);
                                });
                            } else {
                                field.on('change', function(){
                                    let $section = $(this).parents('.form-section, .subform-section');
                                    setTimeout(function() {
                                        if($section.is(":visible"))
                                            target.numericCalc(target,o);
                                    }, TIMEOUT);
                                });
                            }
                        });
                    }

                    // numeric field is manually editable
                    else {
                        let $section = $(target).closest(".form-cell, .subform-cell");
                        let $elementMask = $($section).find("input.mask");
                        let $elementPrimary = $($section).find(".primary_value");

                        // initialize actual value and block non numeric keys
                        $elementMask.keypress(function(evt) {
                            $elementPrimary.val(Number($elementMask.val()));
                            return evt.metaKey || /[0-9\-,.\r]/.test(String.fromCharCode(evt.which));
                        });

                        $elementMask.focusout(function() {
                            $elementPrimary.val($elementMask.val());
                            target.numericCalc(target, o);
                        });

                        $elementMask.focusin(function() {
                            let value = $elementPrimary.val();
                            $elementMask.val(value == '0' ? '' : value);
                        });
                    }

                    if(o.currencyRefField) {
                        let currencyRefField = FormUtil.getField(o.currencyRefField);
                        currencyRefField.change(function() {
                            if($(currencyRefField).parents('.form-section, .subform-section').is(":visible"))
                                target.numericCalc(target, o);
                        });
                    }

                    // run on load 
                    $(document).ready(function() {
                        target.numericCalc(target, o);
                    });

                    // handle mobile page show
                    $(document).bind("pageshow", function() {
                        target.numericCalc(target, o);
                    });
                }
            }
            return target;
        },

        // copy from FormUtil.getField in formUtil.js
        getField : function(fieldId){
            var field = $("[name="+fieldId+"]").filter(':not(.grid-cell,.grid-summary)');
            if ($(field).length == 0) {
                field = $("[name$=_"+fieldId+"]").filter(':not(.grid-cell,.grid-summary)');
            }

            //filter those in hidden section
            field = $(field).filter(':parents(.section-visibility-hidden)');

            //to prevent return field with similar name, get the field with shorter name (Field in the subform)
            if ($(field).length > 1) {
                var fieldname;
                $(field).each(function(){
                    if (fieldname === undefined) {
                        fieldname = $(this).attr("name");
                    }
                    if ($(this).attr("name").length < fieldname.length) {
                        fieldname = $(this).attr("name");
                    }
                    field = $("[name="+fieldname+"]");
                });
            }

            return field;
        },

        numericCalc : function(target, o) {
            if (o.useJsEquation === true) {
                var imageLoader = $(target).closest(".form-cell, .subform-cell").find("img.loader");
                if(imageLoader)
                    $(imageLoader).show();

                let equation = o.jsEquation || "0";

                $.each(o.variables, function(index){
                    let variable = o.variables[index];
                    let fieldId = variable.variableName;
                    let value = 0;

                    let $selector;
                    if(fieldId.indexOf(".") != -1) {
                        $selector = FormUtil.getGridCells(fieldId).filter(":not(.grid-summary)");
                    } else {
                        $selector = target.getField(fieldId).filter(":not(.grid-cell)");
                    }

                    let values = [];
                    $selector.each(function () {
                        values.push(parseLocaleNumber(this.value));
                    });

                    let operation = variable.operation;
                    if (operation === "avg") {
                        value = values.length ? values.reduce((a,b)=>a+b,0) / values.length : 0;
                    } else if (operation === "min") {
                        value = values.length ? Math.min.apply(null, values) : 0;
                    } else if (operation === "max") {
                        value = values.length ? Math.max.apply(null, values) : 0;
                    } else if (operation === "count") {
                        value = values.length;
                    } else if (operation === "sum") {
                        value = values.reduce((a,b)=>a+b,0);
                    } else {
                        value = values.length ? values[0] : 0;
                    }

                    let regex = new RegExp('\\b' + variable.variableName + '\\b', 'g');
                    equation = equation.replace(regex, value);
                });

                let resultValue = 0;
                try {
                    resultValue = Function('"use strict";return (' + equation + ')')();
                } catch(e) {
                    console.error("NumericField JS Equation Error:", e);
                    resultValue = 0;
                }

                if (typeof resultValue !== "number" || Number.isNaN(resultValue) || !Number.isFinite(resultValue)) {
                    console.warn("NumericField invalid result:", resultValue, "from equation:", equation);
                    resultValue = 0;
                }

                resultValue = roundValue(resultValue, o);

                if (parseInt(o.decimalPlaces || 0, 10) === 0) {
                    resultValue = Math.trunc(resultValue);
                }

                if (o.hasFormatter) {
                    let mask = formatByFormatter(resultValue, o);
                    apply(target, {
                        value: resultValue,
                        mask_value: mask
                    }, o);
                } else {
                    apply(target, {
                        value: resultValue,
                        mask_value: resultValue
                    }, o);
                }
                return;
            } else {
                if(!$(target).closest("div").find('input[data-donttrigger*="true"]').length) {
                let loadBinder = o.loadBinder;
                loadBinder.requestParams = {};
                loadBinder.fieldId = o.fieldId;

                $.each(o.variables, function(index){
                    let variable = o.variables[index];
                    let fieldId = variable.variableName;
                    let element;

                    let value = 0;

                    let $selector;
                    if(fieldId.indexOf(".") != -1) {
                        $selector = FormUtil.getGridCells(fieldId).filter(":not(.grid-summary)");
                    } else {
                        $selector = target.getField(fieldId);

                        //fix for IE (Grid values should not added even its have same id)
                        $selector = $selector.filter(":not(.grid-cell)");
                    }

                    if($selector.length > 0 && !$selector.closest(".form-section, .subform-section").hasClass("section-visibility-hidden")) {

                        //if radio or checkbox, count only checked
                        var type = $selector.attr('type');
                        if (type != undefined && (type.toLowerCase() == "radio" || type.toLowerCase() == "checkbox")) {
                            $selector = $selector.filter(":checked");
                        }

                        let values = [];
                        $selector.each(function () {
                            values.push(parseLocaleNumber(this.value));
                        });

                        var operation = variable.operation;
                        if (operation == "avg") {
                            value = values.length ? values.reduce((a,b)=>a+b,0) / values.length : 0;
                        } else if (operation== "min") {
                            value = values.length ? Math.min.apply(null, values) : 0;
                        } else if (operation == "max") {
                            value = values.length ? Math.max.apply(null, values) : 0;
                        } else if( operation == "count") {
                            value = values.length;
                        } else if (operation == "sum") {
                            value = values.reduce((a,b)=>a+b,0);
                        } else {
                            value = values.length ? values[0] : 0;
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
              loadBinder.formatValue = formatValue ? formatValue : '';

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
      }
    });

    function roundValue(value, o) {
        if (!o.roundNumber) return value;

        let decimals = parseInt(o.decimalPlaces || 0, 10);
        let factor = Math.pow(10, decimals);

        switch (o.roundingMode) {
            case "round_up":
                return Math.ceil(value * factor) / factor;

            case "round_down":
                return Math.floor(value * factor) / factor;

            case "round_half_up":
            default:
                return Math.round(value * factor) / factor;
        }
    }

    function formatByFormatter(value, o) {
        let cls = o.formatterClass;
        let cfg = o.formatterConfig || {};

        // ===== Percentage Formatter =====
        if (cls.indexOf("PercentageFormatter") !== -1) {
            if (value === null || value === undefined) {
                return "";
            }

            let strValue = value.toString().trim();
            if (strValue === "") {
                return "";
            }

            let number = Number(strValue);
            if (isNaN(number)) {
                return strValue;
            }

            let decimals = parseInt(o.decimalPlaces || 0, 10);

            let result = number * 100;
            return result.toFixed(decimals) + "%";
        }

        // ===== Currency Formatter =====
        if (cls.indexOf("CurrencyFormatter") !== -1) {
            return formatCurrency(value, cfg);
        }

        // fallback
        return value;
    }

    function formatCurrency(value, cfg) {
        let locale = cfg.locale || 'en-US';
        let showDecimals = cfg.showDecimals === 'true';
        let showCurrency = cfg.showCurrencySymbol === 'true';

        let options = {
            minimumFractionDigits: showDecimals ? 2 : 0,
            maximumFractionDigits: showDecimals ? 2 : 0
        };

        if (showCurrency) {
            options.style = 'currency';
            options.currency =
                locale === 'in_ID' ? 'IDR' :
                locale === 'ja_JP' ? 'JPY' :
                'USD';
        }

        return new Intl.NumberFormat(locale.replace('_', '-'), options)
            .format(value);
    }

    function onKeyUp(target, o) {

    }

    function apply(target, result, o) {
        let $elementBlock = $(target).closest("div");
        let $elementMask = $elementBlock.find(".mask");
        let $elementPrimary =  $elementBlock.find(".primary_value");

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
            let spellingElement = FormUtil.getField(o.spellingTarget);
            let currencyElement = FormUtil.getField(o.currencyRefField);
            let currencyValue = FormUtil.getValue(o.currencyRefField);
            let isCurrency = currencyValue != '' ? true : false;
            let terbilangValue = terbilang(result.value).trim();

            //let value = parseFloat(result.value.toString().replace(',', '.'));
            let value = parseLocaleNumber(result.value);

            let decimalValue = decimalTerbilang(value, isCurrency).trim();

            let terbilangCurrency =
                !terbilangValue ? ''
                    : currencyValue == 'en_US' ? 'dollar'
                    : currencyValue == 'en_IE' ? 'euro'
                    : currencyValue == 'in_ID' ? 'rupiah'
                    : currencyValue == 'ja_JP' ? 'yen'
                    : '';
            let spellingValue = terbilangValue || terbilangCurrency ? (terbilangValue + ' ' + terbilangCurrency + ' ' + decimalValue) : '';


            spellingElement.val(spellingValue);
            spellingElement.attr("readonly","true");
        }
    }

    // === helper: parse number with locale support ===
    function parseLocaleNumber(val) {
        if (val === null || val === undefined) return 0;
        if (typeof val === "number") return val;

        val = val.toString().trim();
        if (!val) return 0;

        // 5.000,00 OR 5,000.00
        if (val.indexOf('.') > -1 && val.indexOf(',') > -1) {
            if (val.lastIndexOf(',') > val.lastIndexOf('.')) {
                // 5.000,00 → 5000.00
                val = val.replace(/\./g, '').replace(',', '.');
            } else {
                // 5,000.00 → 5000.00
                val = val.replace(/,/g, '');
            }
        } else if (val.indexOf(',') > -1) {
            // 5000,00 → 5000.00
            val = val.replace(',', '.');
        }

        var num = Number(val);
        return isNaN(num) ? 0 : num;
    }
})(jQuery);