<#macro layout_header>

<#include "/${themeDir}/header.dec" />
<#if (authenticatedUser??) >

<p id="welcome-menu" class="welcome  screen ">
    <@s.text name="menu.welcome_back"/> <a href="">${authenticatedUser.properName}
    <i class="caret drop-down"></i>
</a>
</p>

<div class="welcome-drop  screen ">

    <p>${authenticatedUser.properName}</p>

    <ul>
        <li><a href="<@s.url value="/contribute"/>"><@s.text name="menu.create_a_resource"/></a></li>
        <li><a href="<@s.url value="/project/add"/>"><@s.text name="menu.create_a_project"/></a></li>
        <li><a href="<@s.url value="/collection/add"/>"><@s.text name="menu.create_a_collection"/></a></li>
        <li><a href="<@s.url value="/dashboard"/>"><@s.text name="menu.dashboard"/></a></li>
        <li><a href="<@s.url value="/dashboard#bookmarks"/>"><@s.text name="menu.bookmarks"/></a></li>
    </ul>

    <ul>
        <li><a href="<@s.url value='/entity/user/myprofile'/>"><@s.text name="menu.my_profile"/></a></li>
        <li><a href="${commentUrlEscaped}?subject=tDAR%20comments"><@s.text name="menu.contact"/></a></li>
        <li><a href="<@s.url value="/logout"/>"><@s.text name="menu.logout"/></a></li>
    </ul>

    <#if administrator>
        <ul>
            <li><@s.text name="menu.admin_header"/></li>
            <li><a href="<@s.url value='/admin'/>"><@s.text name="menu.admin_main"/></a></li>
            <li><a href="<@s.url value='/admin/system/activity'/>"><@s.text name="menu.admin_activity"/></a></li>
            <li><a href="<@s.url value='/admin/searchindex/build'/>"><@s.text name="menu.admin_reindex"/></a></li>
        </ul>
    </#if>

<#if !searchHeaderEnabled>
                <nav>
                    <ul class="hidden-phone-portrait">
                        <#include "/${themeDir}/nav-items.dec" />
                        <li class="button hidden-phone"><a href="<@s.url value="/search/results"/>">BROWSE</a></li>
                        <#if ((authenticatedUser.contributor)!true)>
                            <li class="button hidden-phone"><a href="<@s.url value="/contribute"/>">UPLOAD</a></li></#if>
                        <li>
                            <#if bodyid!'' != 'home'>
                                <form name="searchheader" action="<@s.url value="/search/results"/>" class="inlineform hidden-phone hidden-tablet  screen">
                                    <input type="text" name="query" class="searchbox" placeholder="Search ${siteAcronym} &hellip; ">
                                <#--<input type="hidden" name="_tdar.searchType" value="simple">-->
                                ${(page.properties["div.divSearchContext"])!""}
                                </form>
                            </#if>
                        </li>
                    </ul>
                </nav>
</#if>
</div>
</#if>

<nav>
    <ul class="hidden-phone-portrait">
    <#include "/${themeDir}/nav-items.dec" />
        <li class="button hidden-phone"><a href="<@s.url value="/search/results"/>">BROWSE</a></li>
    <#if ((authenticatedUser.contributor)!true)>
        <li class="button hidden-phone"><a href="<@s.url value="/contribute"/>">UPLOAD</a></li></#if>
    </ul>

</nav>
</#macro>

<#macro searchHeader>
<#if !searchHeaderEnabled><#return></#if>
<#assign subtitle = (resourceCollection.subtitle!(resourceCollection.institution.name)!'')>
<div class="searchheader">
    <div class="hero">
        <div class="container">
            <div class="pull-right whitelabel-login-menu" ><@common.loginMenu false/></div>
            <h2 class="color-title">${title}</h2>
            <#if subtitle?has_content>
            <p class="color-subtitle">${subtitle}</p>
            </#if>
            <form name="searchheader" action="<@s.url value="/search/results"/>" class="searchheader">
                <input type="text" name="query" placeholder="Find archaeological data..." class="searchbox input-xxlarge">
                <a href="/search/advanced?collectionId=${resourceCollection.id?c}">advanced</a>
                <input type="hidden" name="_tdar.searchType" value="simple">
                <input type="hidden" name="collectionId" value="${resourceCollection.id}">
            </form>

        </div>
    </div>
</div>
</#macro>

<#macro subnav>
<#if subnavEnabled>
<div class="subnav-section">
    <div class="container">
        <div class="row">
            <div class="span12 subnav">
                <ul class="subnav-lft">
                    <li><a href="<@s.url value="/search"/>"><@s.text name="menu.search"/></a></li>
                    <li><a href="<@s.url value="/search/results"/>"><@s.text name="menu.browse"/></a></li>
                    <li><a href="<@s.url value="/browse/explore"/>"><@s.text name="menu.explore"/></a></li>
                    <#if sessionData?? && sessionData.authenticated>
                        <li><a href="<@s.url value="/dashboard"/>"><@s.text name="menu.dashboard"/></a></li>
                        <li><a href="<@s.url value="/workspace/list"/>"><@s.text name="menu.integrate"/></a></li>
                        <#if editor>
                            <li><a href="<@s.url value="/admin"/>"><@s.text name="menu.admin"/></a></li>
                        </#if>
                    </#if>
                </ul>
                <#if actionName!='login' && actionName!='register' && actionName!='download' && actionName!='review-unauthenticated'>
                    <@common.loginMenu true />
                </#if>
            </div>
        </div>
    </div>
</div>
</#if>
</#macro>