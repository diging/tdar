<#escape _untrusted as _untrusted?html>
<#macro sidebar current="dashboard">
    <ul class="nav nav-list nav-stacked dashboard-nav">
      <li <@activeIf current "dashboard" />><a href="/dashboard"> My Resources</a></li>
      <li class="nav-header">My Shares</li>
      <li  <@activeIf current "share" />><a href="/manage">Share</a></li>
      <li><a href="/manage">Manage</a></li>
      <li><a href="/rights">Resources</a></li>
      <li class="nav-header">My Library</li>
      <li  <@activeIf current "collections" />> <a href="/organize">Collections</a></li>
      <li  <@activeIf current "bookmarks" />> <a href="/bookmarks"> Bookmarks</a></li>
      <li class="nav-header">My Account</li>
      <li><a href="/entity/user/myprofile">Profile</a></li>
      <#if contributor>
          <li <@activeIf current "billing" />><a href="/billing">Billing Accounts</a></li>
          <li <@activeIf current "export" />><a href="/export/request">Export</a></li>
      </#if>
    </ul>
</#macro>

<#macro activeIf current test>
<#if current == test>class="active"</#if>
</#macro>


<#macro headerNotifications>
    <#list currentNotifications as notification>
        <div class="${notification.messageType} alert" id="note_${notification.id?c}">
        <button type="button" id="close_note_${notification.id?c}" class="close" data-dismiss="alert" data-dismiss-id="${notification.id?c}" >&times;</button>
        <#if notification.messageDisplayType.normal>
        <@s.text name="${notification.messageKey}"/> [${notification.dateCreated?date?string.short}]
        <#else>
            <#local file = "../notifications/${notification.messageKey}.ftl" />
            <#if !notification.messageKey?string?contains("..") >
                <#attempt>
                    <#include file />
                <#recover>
                    Could not load notification.
                </#attempt>
            </#if>
        </#if>
        </div>
    </#list>


    <#list overdrawnAccounts![]>
    <div class="alert-error alert">
        <h3><@s.text name="dashboard.overdrawn_title"/></h3>

        <p><@s.text name="dashboard.overdrawn_description" />
            <a href="<@s.url value="/cart/add"/>"><@s.text name="dashboard.overdrawn_purchase_link_text" /></a>
        </p>
        <ul>
            <#items as account>
                <li>
                    <a href="<@s.url value="/billing/${account.id?c}" />">${account.name!"unamed"}</a>
                </li>
            </#items>
        </ul>
    </div>
    </#list>

</#macro>

<#macro collectionLegend collection>
        <ul class="inline">
            <li title="# of resources"><i class="icon-file"></i> ${((collection.unmanagedResources?size)!0 + collection.resources?size!0)}</li>
            <#local children= (collection.transientChildren?size)!0/>
            <#local users= (collection.authorizedUsers?size)!0/>
            
            <#local folder = "icon-folder-close" />
            <#if (children > 0)>
                <#local folder="icon-folder-open" />
            </#if>
            <li title="# of collections"><i class="${folder}"></i> ${children}</li>
            <li title="# of users"><i class="icon-user"></i> ${users}</li>
            <#if (children + users > 0)><li><i class="icon-circle-arrow-right" data-toggle="collapse" data-target="#details${collection.id?c}"></i> </li></#if>
        </ul>
        <div id="details${collection.id?c}" class="collapse">
          <ul class="">
            <#list collection.transientChildren><li><i class="${folder}"></i> <#items as child>${child.name}<#sep>&bull;</#sep></#items></li></#list>
            <#list collection.authorizedUsers><li><i class="icon-user"></i> <#items as user>${user.user.properName} (${user.generalPermission})<#sep>&bull;</#sep></#items></li></#list>
        </ul>
       </div>
</#macro>

</#escape>