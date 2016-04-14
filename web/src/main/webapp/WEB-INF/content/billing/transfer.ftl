<#escape _untrusted as _untrusted?html>
    <#import "/WEB-INF/macros/resource/view-macros.ftl" as view>
    <#import "/WEB-INF/macros/resource/common.ftl" as common>
    <#import "/WEB-INF/macros/resource/list-macros.ftl" as list>
    <#import "/WEB-INF/macros/resource/navigation-macros.ftl" as nav>
    <#import "/WEB-INF/macros/resource/common.ftl" as common>

<head>
    <title>${account.name!"Your Account"}</title>
    <meta name="lastModifiedDate" content="$Date$"/>

</head>
<body>
    <@nav.billingToolbar "${account.urlNamespace}" "transfer"/>

	<h1>${account.name!"Your account"} <#if accountGroup?has_content><span>${accountGroup.name}</span></#if></h1>

    <#if account.description?has_content>
    <p>${account.description!""}</p>
    </#if>

<h3>Transfer the balance of extra files (${account.availableNumberOfFiles}) into annother account:</h3>

<@s.form action="${id?c}/transfer" method="POST">
    <@s.select name="toAccountId" list="%{accounts}" label="Select destination account" title="Select an account to transfer to" listValue="name" listKey="id"
         emptyOption="false" required=true cssClass="required"/>

    <@s.submit />
</@s.form>
</body>
</#escape>
