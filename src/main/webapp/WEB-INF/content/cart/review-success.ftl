<#escape _untrusted as _untrusted?html>
    <#import "/WEB-INF/macros/resource/view-macros.ftl" as view>
    <#import "/WEB-INF/macros/resource/edit-macros.ftl" as edit>
    <#import "/WEB-INF/macros/resource/common.ftl" as common>
    <#import "/WEB-INF/macros/resource/list-macros.ftl" as list>
    <#import "/WEB-INF/macros/resource/navigation-macros.ftl" as nav>
    <#import "/WEB-INF/content/cart/common-invoice.ftl" as invoicecommon >

<head>
    <title>Review Billing Information</title>

</head>
<body>
<h1>Invoice <span class="small">{${invoice.transactionStatus.label}}</span></h1>
    <@s.form name='MetadataForm' id='MetadataForm'  method='post' cssClass="form-horizontal disableFormNavigate" enctype='multipart/form-data' action='/cart/process-payment-request}'>
        <#--<@s.hidden name="id" value="${invoice.id?c!-1}" />-->
        <@s.token name='struts.csrf.token' />
        <@invoicecommon.proxyNotice />

        <@invoicecommon.printInvoice />
        <#if invoice.owner??>
        <#if invoice.owner.addresses?has_content>
        <h3>Choose an existing address</h3>
            <#assign addressId = ""/>
            <#if invoice.address?has_content><#assign addressId=invoice.address.id /></#if>
            <@s.select name="invoice.address.id" listValue="addressSingleLine" listKey="id" emptyOption="true" label="Address" list="invoice.owner.addresses"  value="${addressId}"
            headerKey="-1" headerValue="(optional)" />

        </#if>
        </#if>
        <#if authenticatedUser??>
        <h3>Choose Payment Method</h3>
            <@invoicecommon.paymentMethod includePhone=false />
        <#else>
            <#assign return="/cart/finalreview">
            <a href="<@s.url value="/account/new?url=${return?url}" />" class="button" rel="nofollow">Sign Up</a>
            <@common.loginButton class="button" returnUrl=return />
            
        </#if>
    </@s.form>
</body>
</#escape>
