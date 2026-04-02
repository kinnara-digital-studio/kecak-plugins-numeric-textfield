<div class="form-cell" ${elementMetaData!}>
    <#if includeMetaData!>
        <label class="label">${element.properties.label}</label>
        <span class="form-floating-label">NUMERIC FIELD</span>
        <input id="${elementParamName!}" name="${elementParamName!}" type="text" value="${value!?html}" readonly />

    <#else>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/jquery.calculation.min.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/jquery.numericfield.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/math.js"></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/terbilang-spelling.js"/></script>
        <script type="text/javascript" src="${request.contextPath}/plugin/${className}/js/decimal-terbilang-spelling.js"/></script>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
        <style>
            input.mask[readonly] {
                background-color: #f4f4f4;
                color: #666;
                border-color: #ddd;
            }
            .calculationField_value {
                color: #666;
            }
            .nf-calc-btn-${element.properties.elementUniqueKey!} {
                position: absolute;
                right: 5px;
                top: 14.5px;
                transform: translateY(-50%);
                display: inline-flex;
                align-items: center;
                justify-content: center;
                width: 25px;
                height: 23px;
                padding: 0;
                border: 1px solid transparent;
                border-radius: 4px;
                background: #FFFFFF;
                cursor: pointer;
                font-size: 20px;
                line-height: 1;
                color: #6b7280;
                z-index: 5;
                transition: background-color 0.15s, color 0.15s;
            }
            .nf-calc-btn-${element.properties.elementUniqueKey!}:hover,
            .nf-calc-btn-${element.properties.elementUniqueKey!}:focus,
            .nf-calc-btn-${element.properties.elementUniqueKey!}:active {
                background-color: #FFFFFF !important;
                color: #6b7280 !important;
                outline: none !important;
                box-shadow: none !important;
            }
            #nf-calc-modal-${element.properties.elementUniqueKey!} {
                display: none;
                position: absolute;
                top: calc(100% + 4px);
                right: 0;
                z-index: 99999;
            }
            #nf-calc-modal-${element.properties.elementUniqueKey!}.nf-calc-open {
                display: block;
            }
            .nf-calc-card-${element.properties.elementUniqueKey!} {
                background: #ffffff;
                border: 1px solid #d1d5db;
                border-radius: 6px;
                box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);
                width: 220px;
                padding: 12px;
                font-family: inherit;
                user-select: none;
            }
            .nf-calc-display-${element.properties.elementUniqueKey!} {
                background: #f9fafb;
                border: 1px solid #e5e7eb;
                border-radius: 4px;
                padding: 8px 12px;
                margin-bottom: 12px;
                text-align: right;
            }
            .nf-calc-expr-${element.properties.elementUniqueKey!} {
                font-size: 11px;
                color: #6b7280;
                min-height: 14px;
                word-break: break-all;
            }
            .nf-calc-result-${element.properties.elementUniqueKey!} {
                font-size: 20px;
                font-weight: 600;
                color: #111827;
                word-break: break-all;
                line-height: 1.2;
                margin-top: 2px;
            }
            .nf-calc-grid-${element.properties.elementUniqueKey!} {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 6px;
            }
            .nf-calc-grid-${element.properties.elementUniqueKey!} button {
                background: #ffffff;
                color: #374151;
                border: 1px solid #d1d5db;
                border-radius: 4px;
                font-size: 14px;
                font-weight: 500;
                height: 36px;
                cursor: pointer;
                transition: background-color 0.1s, border-color 0.1s;
                box-shadow: 0 1px 2px 0 rgba(0,0,0,0.05);
            }
            .nf-calc-grid-${element.properties.elementUniqueKey!} button:active {
                background-color: #f3f4f6 !important;
                box-shadow: none;
                transform: translateY(1px);
            }
            .nf-calc-grid-${element.properties.elementUniqueKey!} button:hover,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button:focus {
                background-color: #ffffff !important;
                border-color: #d1d5db !important;
                color: #374151 !important;
                outline: none !important;
            }
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-op {
                background: #f3f4f6 !important;
                color: #1f2937 !important;
                font-weight: 600;
            }
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-op:hover,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-op:focus,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-op:active {
                background: #f3f4f6 !important;
                color: #1f2937 !important;
                border-color: #d1d5db !important;
                outline: none !important;
                box-shadow: none !important;
            }
            .nf-calc-btn-eq {
                grid-column: span 4;
                background: #2563eb !important;
                color: #ffffff !important;
                border-color: #1d4ed8 !important;
                font-weight: 700;
                font-size: 15px !important;
            }
            .nf-calc-btn-eq:hover,
            .nf-calc-btn-eq:focus,
            .nf-calc-btn-eq:active {
                background: #2563eb !important;
                color: #ffffff !important;
                border-color: #1d4ed8 !important;
                outline: none !important;
            }
            .nf-calc-btn-cancel {
                grid-column: span 4;
                background: #ffffff !important;
                color: #6b7280 !important;
            }
            .nf-calc-btn-cancel:hover,
            .nf-calc-btn-cancel:focus,
            .nf-calc-btn-cancel:active {
                background: #ffffff !important;
                color: #6b7280 !important;
                border-color: #d1d5db !important;
                outline: none !important;
            }
            .nf-calc-btn-clr, .nf-calc-btn-bsp {
                color: #dc2626 !important;
                background: #fef2f2 !important;
                border-color: #fecaca !important;
            }
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-clr,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-bsp {
                color: #dc2626 !important;
                background: #fef2f2 !important;
                border-color: #fecaca !important;
            }
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-clr:hover,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-clr:focus,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-clr:active,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-bsp:hover,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-bsp:focus,
            .nf-calc-grid-${element.properties.elementUniqueKey!} button.nf-calc-btn-bsp:active {
                background: #fef2f2 !important;
                border-color: #fecaca !important;
                color: #dc2626 !important;
                outline: none !important;
                box-shadow: none !important;
            }
        </style>

        <#if element.properties.readonly! == 'true'>
            <#assign attrDontTrigger = 'readonly="readonly" data-donttrigger="true"' >
        <#else>
            <#assign attrDontTrigger = '' >
        </#if>
        <script type="text/javascript">
            let options_calField_${element.properties.elementUniqueKey} = ${configJson!};
            $(document).ready(function(){
                $('#${elementParamName!}${element.properties.elementUniqueKey!}').numericField(options_calField_${element.properties.elementUniqueKey});
            });
        </script>

        <input id="${elementParamName!}${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="calculationField primary_value calField_${element.properties.elementUniqueKey!}"/>

        <#if element.properties.hidden! != 'true'>
            <label class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
            <#if element.properties.readonlyLabel! == 'true' && element.properties.readonly! == 'true'>
                <span class="calculationField_value">${formattedValue!?html}</span>
            <#else>
                <div style="position: relative; width: 100%; display: block;">
                    <input id="${elementParamName!}${element.properties.elementUniqueKey!}_mask" name="${elementParamName!}_mask" value="${formattedValue!?html}" <#if element.properties.readonly! == 'true' || element.properties.calculationLoadBinder.className?? && element.properties.calculationLoadBinder.className != "">readonly="readonly"</#if> style="text-align: right; width: 100%; box-sizing: border-box; <#if calculatorCheckBox! == 'true'>padding-right: 36px;</#if>" class="mask" autocomplete="off" ${attrDontTrigger} />
                    <#if calculatorCheckBox! == 'true'>
                        <button type="button"
                             class="nf-calc-btn-${element.properties.elementUniqueKey!}"
                             id="${elementParamName!}${element.properties.elementUniqueKey!}_calcBtn"
                             title="Open Calculator">
                             <i class="fa-solid fa-calculator"></i></button>

                        <#if element.properties.readonly! != 'true'>
                        <div id="nf-calc-modal-${element.properties.elementUniqueKey!}" role="dialog" aria-modal="true" aria-label="Calculator">
                            <div class="nf-calc-card-${element.properties.elementUniqueKey!}">
                                <div class="nf-calc-display-${element.properties.elementUniqueKey!}">
                                    <div class="nf-calc-expr-${element.properties.elementUniqueKey!}" id="nf-calc-expr-${element.properties.elementUniqueKey!}"></div>
                                    <div class="nf-calc-result-${element.properties.elementUniqueKey!}" id="nf-calc-result-${element.properties.elementUniqueKey!}">0</div>
                                </div>
                                <div class="nf-calc-grid-${element.properties.elementUniqueKey!}">
                                    <!-- Row 1 -->
                                    <button type="button" class="nf-calc-btn-clr" data-calc-action="clear">C</button>
                                    <button type="button" class="nf-calc-btn-bsp" data-calc-action="backspace">⌫</button>
                                    <button type="button" class="nf-calc-btn-op"  data-calc-action="paren" data-value="(">(</button>
                                    <button type="button" class="nf-calc-btn-op"  data-calc-action="paren" data-value=")">)</button>
                                    <!-- Row 2 -->
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="7">7</button>
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="8">8</button>
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="9">9</button>
                                    <button type="button" class="nf-calc-btn-op"  data-calc-action="operator" data-value="/">÷</button>
                                    <!-- Row 3 -->
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="4">4</button>
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="5">5</button>
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="6">6</button>
                                    <button type="button" class="nf-calc-btn-op"  data-calc-action="operator" data-value="*">×</button>
                                    <!-- Row 4 -->
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="1">1</button>
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="2">2</button>
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="3">3</button>
                                    <button type="button" class="nf-calc-btn-op"  data-calc-action="operator" data-value="-">−</button>
                                    <!-- Row 5 -->
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value="0">0</button>
                                    <button type="button" class="nf-calc-btn-num" data-calc-action="digit" data-value=".">.</button>
                                    <button type="button" class="nf-calc-btn-op"  data-calc-action="percent" data-value="%">%</button>
                                    <button type="button" class="nf-calc-btn-op"  data-calc-action="operator" data-value="+">+</button>
                                    <!-- Row 6: = full width, hitung + simpan + tutup -->
                                    <button type="button" class="nf-calc-btn-eq"  data-calc-action="equals">=</button>
                                    <!-- Row 7: Cancel full width -->
                                    <button type="button" class="nf-calc-btn-cancel" data-calc-action="cancel">Cancel</button>
                                </div>
                            </div>
                        </div>

                        <script type="text/javascript">
                        (function(){
                            var KEY      = '${element.properties.elementUniqueKey!}';
                            var MODAL    = document.getElementById('nf-calc-modal-' + KEY);
                            var exprEl   = document.getElementById('nf-calc-expr-'   + KEY);
                            var resultEl = document.getElementById('nf-calc-result-' + KEY);
                            var openBtn  = document.getElementById('${elementParamName!}' + KEY + '_calcBtn');

                            var state = { expr: '', current: '0', hasResult: false };

                            function updateDisplay() {
                                exprEl.textContent   = state.expr;
                                resultEl.textContent = state.current;
                            }

                            function safeEval(expr) {
                                try {
                                    var e = expr
                                        .replace(/\u00F7/g, '/')   /* ÷ */
                                        .replace(/\u00D7/g, '*')   /* × */
                                        .replace(/\u2212/g, '-')   /* − */
                                        .replace(/[+\-*/]\s*$/, '');
                                    /* Handle % as /100, e.g. 1 * 10% → 1 * (10/100) */
                                    e = e.replace(/(\d+\.?\d*)\s*%/g, '($1/100)');
                                    if (!/^[0-9+\-*/.%\s()]+$/.test(e)) return NaN;
                                    var r = math.evaluate(e);
                                    return isFinite(r) ? r : NaN;
                                } catch(ex) { return NaN; }
                            }
<#--  
                            function safeEval(expr) {
                                try {
                                    var e = expr
                                        .replace(/\u00F7/g, '/')
                                        .replace(/\u00D7/g, '*')
                                        .replace(/\u2212/g, '-')
                                        .replace(/[+\-*/]\s*$/, '');
                                    e = e.replace(/(\d+\.?\d*)\s*%/g, '($1/100)');

                                    var openCount  = (e.match(/\(/g) || []).length;
                                    var closeCount = (e.match(/\)/g) || []).length;
                                    var missing    = openCount - closeCount;
                                    if (missing > 0) e = e + ')'.repeat(missing);

                                    if (!/^[0-9+\-*/.%\s()]+$/.test(e)) return NaN;
                                    var r = math.evaluate(e);
                                    return isFinite(r) ? r : NaN;
                                } catch(ex) { return NaN; }
                            }  -->

                            function commitAndClose(numStr) {
                                var num = parseFloat(numStr);
                                if (isNaN(num)) return;
                                var hiddenInput = document.getElementById('${elementParamName!}' + KEY);
                                var maskInput   = document.getElementById('${elementParamName!}' + KEY + '_mask');
                                if (hiddenInput) hiddenInput.value = num;
                                if (maskInput)   maskInput.value   = num;
                                if (hiddenInput) $(hiddenInput).val(num).trigger('change');
                                closeModal();
                            }

                            function handle(action, value) {
                                if (state.current === "Error" && action !== "clear") return;
                                switch(action) {

                                    case 'percent':
                                        var val = parseFloat(state.current);
                                        if (!isNaN(val)) {
                                            state.current = String(val / 100);
                                        }
                                        break;

                                    case 'digit':
                                        if (value === '.' && state.current.indexOf('.') !== -1) break;
                                        if (state.hasResult) { state.expr = ''; state.hasResult = false; }
                                        state.current = (state.current === '0' && value !== '.') ? value : state.current + value;
                                        break;

                                    case 'paren':
                                        if (state.hasResult) { state.expr = ''; state.hasResult = false; }
                                        if (value === '(') {
                                            state.expr   += (state.current !== '0') ? state.current + ' * ( ' : '( ';
                                            state.current = '0';
                                        } else {
                                            state.expr   += (state.current !== '0' || state.expr.trim().slice(-1) !== ')') ? state.current + ' ) ' : ') ';
                                            state.current = '0';
                                        }
                                        break;

                                    case 'operator':
                                        if (state.expr.trim().slice(-1) === ')' && state.current === '0') {
                                            state.expr = state.expr + ' ' + value + ' ';
                                        } else {
                                            state.expr = state.expr + state.current + ' ' + value + ' ';
                                        }
                                        state.current   = '0';
                                        state.hasResult = false;
                                        break;

                                    case 'equals':
                                        /* Build full expression from expr + current */
                                        var full = state.expr;
                                        if (!(state.expr.trim().slice(-1) === ')' && state.current === '0')) {
                                            full += state.current;
                                        }
                                        var res = safeEval(full);
                                        if (!isNaN(res)) {
                                            var resultStr = String(parseFloat(res.toFixed(10)));
                                            /* Update display first so user sees the result */
                                            state.expr      = full + ' =';
                                            state.current   = resultStr;
                                            state.hasResult = true;
                                            updateDisplay();
                                            /* Then immediately save & close */
                                            commitAndClose(resultStr);
                                        } else {
                                            state.current = 'Error';
                                            updateDisplay();
                                        }
                                        return; /* skip bottom updateDisplay() */

                                    case 'clear':
                                        state.expr = ''; state.current = '0'; state.hasResult = false;
                                        break;

                                    case 'backspace':
                                        if (state.hasResult) { state.expr=''; state.current='0'; state.hasResult=false; break; }
                                        state.current = state.current.length > 1 ? state.current.slice(0,-1) : '0';
                                        break;

                                    case 'cancel':
                                        closeModal();
                                        return;
                                }
                                updateDisplay();
                            }

                            function openModal() {
                                state.expr=''; state.current='0'; state.hasResult=false;
                                updateDisplay();
                                MODAL.classList.add('nf-calc-open');
                                MODAL.focus();
                            }
                            function closeModal() {
                                MODAL.classList.remove('nf-calc-open');
                            }

                            MODAL.addEventListener('click', function(e) {
                                var btn = e.target.closest('[data-calc-action]');
                                if (!btn) return;
                                handle(btn.dataset.calcAction, btn.dataset.value || '');
                            });

                            document.addEventListener('click', function(e) {
                                if (!MODAL.classList.contains('nf-calc-open')) return;
                                var inside = MODAL.contains(e.target) || (openBtn && openBtn.contains(e.target));
                                if (!inside) closeModal();
                            });

                            if (openBtn) openBtn.addEventListener('click', openModal);

                            document.addEventListener('keydown', function(e) {
                                if (!MODAL.classList.contains('nf-calc-open')) return;
                                if (e.key === 'Escape')    { e.preventDefault(); closeModal(); return; }
                                if (e.key === 'Enter')     { e.preventDefault(); handle('equals'); return; }
                                if (e.key === 'Backspace') { e.preventDefault(); handle('backspace'); return; }
                                if (/^[0-9.]$/.test(e.key)) { handle('digit', e.key); return; }
                                if (e.key === '%')          { handle('percent'); return; }
                                if (e.key === '(' || e.key === ')') { handle('paren', e.key); return; }
                                if (['+','-','*','/'].indexOf(e.key) !== -1) { handle('operator', e.key); }
                            });
                        })();
                        </script>
                        </#if>
                    </#if>
                </div>
            </#if>
            <img id="${elementParamName!}${element.properties.elementUniqueKey!}_loading" class="loader" src="${request.contextPath}/plugin/${className}/images/spin.gif" height="16" width="16" style="vertical-align: middle; display: none;">
        </#if>
    </#if>

</div>
