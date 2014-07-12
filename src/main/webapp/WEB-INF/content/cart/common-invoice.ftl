<#escape _untrusted as _untrusted?html>
    <#import "/WEB-INF/macros/resource/edit-macros.ftl" as edit>

    <#macro printInvoice>
    <h3>Items <#if invoice.modifiable><a href="<@s.url value="/cart/modify" />" class="small">(modify)</a></#if></h3>
    <table class="tableFormat">
        <tr>
            <th>Item</th>
            <th>Quantity</th>
            <th>Cost</th>
            <th>Files</th>
            <th>Space</th>
            <th>Resources</th>
            <th>Subtotal</th>
        </tr>
        <#list invoice.items as item>
            <tr>
                <td>${item.activity.name}</td>
                <td>${item.quantity!0}</td>
                <td><#if invoice.proxy && !billingManager>N/A<#else>$${item.activity.price}</#if></td>
                <td> ${(item.quantity!0) * (item.activity.numberOfFiles!0)} </td>
                <td> ${(item.quantity!0) * (item.activity.numberOfMb!0)} </td>
                <td> ${(item.quantity!0) * (item.activity.numberOfResources!0)}</td>
                <td><#if invoice.proxy && !billingManager>N/A<#else>$${item.subtotal}
                    <!-- for testing: ${item.activity.name}:${item.quantity!0}:$${item.activity.price}:$${item.subtotal}-->
                </#if>
                </td>
            </tr>
        </#list>
        <#if invoice.coupon?has_content>
            <tr>
                <td>Coupon ${invoice.coupon.code}</td>
                <td>1</td>
                <td></td>
                <td>${invoice.coupon.numberOfFiles}</td>
                <td>${invoice.coupon.numberOfMb}</td>
                <td></td>
                <td></td>
            </tr>
        </#if>
        <tr>
            <th colspan=6><em>Total:</em></th>
            <th><#if invoice.proxy && !billingManager>N/A<#else>$${invoice.calculatedCost!0}
                <!-- FOR testing total:$${invoice.calculatedCost!0} -->
            </#if>
            </th>
        </tr>
    </table>
    </#macro>

<#macro invoiceOwner invoice>
    ${(invoice.owner.displayName)!''}
</#macro>


    <#macro printSubtotal invoice>
    <div id="divInvoiceSubtotal" class="invoice-subtotal">
        <h3>Subtotal</h3>
        <span class="amt">$${invoice.calculatedCost}</span>
        <span class="item-desc"><@invoiceOwner invoice/></span>
        <span class="item-desc">${invoice.numberOfFiles} files / ${invoice.numberOfMb}mb</span>
        <span class="item-desc status">Status: ${invoice.transactionStatus.label}</span>
        <span class="item-desc">Payment by <@s.text name="${invoice.paymentMethod.localeKey}"/></span>
        <#if (billingManager!false)>
            <@s.a href="/cart/new?files=${invoice.numberOfFiles?c}&mb=${invoice.numberOfMb?c}&code=${((invoice.coupon.code)!'')}">Customer Link</@s.a>
        </#if>
    </div>

    </#macro>

    <#macro paymentMethod includePhone=true>
        <@s.radio list="allPaymentMethods" name="invoice.paymentMethod" label="Payment Method"
        listValue="label"    cssClass="transactionType fadeIfZeroed" emptyOption='false' />

        <#if includePhone>
        <div class="typeToggle credit_card invoice manual">
            <@s.textfield name="billingPhone" cssClass="input-xlarge phoneUS  required-visible" label="Billing Phone #" />
        </div>
        </#if>
    <div class="typeToggle invoice">
        <@s.textfield name="invoice.invoiceNumber" cssClass="input-xlarge" label="Invoice #" />
    </div>
    <div class="typeToggle manual">
        <@s.textarea name="invoice.otherReason" cssClass="input-xlarge" label="Other Reason" />
    </div>

        <@edit.submit fileReminder=false label="Next: Process Payment"/>

    <script>
        $(document).ready(function () {
            'use strict';
            TDAR.common.initEditPage($('#MetadataForm')[0]);
            $(".transactionType[type=radio]").click(function () {
                TDAR.common.switchType(this, '#MetadataForm');
            });
            if (!$(".transactionType[type=radio]:checked").length) {
                $(".transactionType[type=radio]").first().click();
            }
            TDAR.common.switchType($(".transactionType[type=radio]:checked", $('#MetadataForm')), "#MetadataForm");

            $("#MetadataForm").submit(function () {
                if ($("#MetadataForm_invoice_billingPhone").val()) {
                    $("#MetadataForm_invoice_billingPhone").val($("#MetadataForm_invoice_billingPhone").val().replace(/([^\d]+)/ig, ""));
                }
            });

            if (${(((invoice.calculatedCost)!0) <= 0)?string}) {
                $(".fadeIfZeroed").fadeTo('fast', .5);
            }

        });

    </script>

    </#macro>

    <#macro accountInfoForm>
        <@s.textfield name="account.name" cssClass="input-xlarge" label="Account Name"/>
        <@s.textarea name="account.description" cssClass="input-xlarge" label="Account Description"/>
        <@s.hidden name="invoiceId" />

        <#if billingAdmin>
        <b>allow user to change owner of account</b>
        </#if>
    <h3>Who can charge to this account </h3>
        <@edit.listMemberUsers />

        <@edit.submit fileReminder=false label="Save" />

    </#macro>

    <#macro proxyNotice>
        <#if invoice.proxy>
        <div class="alert">
            <strong>Proxy Invoice:</strong>
            You are creating this invoice on behalf of ${invoice.owner.properName}.
        </div>
        </#if>
    </#macro>

    </#escape>