<!--
vim:sts=2:sw=2:filetype=jsp
-->
<#import "/WEB-INF/macros/resource/navigation-macros.ftl" as nav>

<head>
    <title>Login/Register</title>
    <meta name="lastModifiedDate" content="$Date$" />
</head>
<body>
<@s.actionmessage />
 
<#if (actionErrors?size > 0)>
<div class="alert alert-error">
    <p>There were the following problems with your submission</p>
    <ul>
    <#list actionErrors as err>
        <li>${(err!"unspecified error")}</li>
    </#list>
    </ul>
</div>
</#if>

<#if sessionData.returnUrl?? && sessionData.returnUrl.contains("/filestore/") >
<div class="alert alert-warning">
    <button type="button" class="close" data-dismiss="alert">×</button>
    <strong>Note:</strong> Currently users must be logged-in to download materials.  Please login below, or signup for a free user account.
</div>
</#if>
<h1>Login to ${siteAcronym}</h1>
<@nav.loginForm/>

<#include "/${themeDir}/notice.ftl">

</body>

