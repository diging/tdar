<#escape _untrusted as _untrusted?html>
    <#import "/WEB-INF/macros/resource/view-macros.ftl" as view>
    <#import "/WEB-INF/macros/resource/common.ftl" as common>
    <#import "/WEB-INF/macros/resource/list-macros.ftl" as list>
    <#import "/WEB-INF/macros/resource/navigation-macros.ftl" as nav>
    <#import "/WEB-INF/macros/search/search-macros.ftl" as search>

<head>
<#-- @search.initResultPagination/ -->
    <@search.headerLinks includeRss=false />

    <title><#if creator?? && creator.properName??>${creator.properName}<#else>No title</#if></title>


    <#if creator??&& ( keywords?has_content || collaborators?has_content)>
        <link rel="meta" type="application/rdf+xml" title="FOAF" href="/browse/creators/${creator.id?c}/rdf"/>
    </#if>
</head>


    <@nav.creatorToolbar "view" />



    <@view.pageStatusCallout />

    <#if creator?? >
        <#if  keywords?has_content || collaborators?has_content >
        <div id="sidebar-right" parse="true">
            <div class="sidebar-spacer">
                <#if collaborators?has_content>
                    <div id="related-creators">
                        <h3>Related Creators</h3>
                        <ul>
                            <#list collaborators as collab>
                                <li><a href="<@s.url value="/browse/creators/${collab.@id}"/>">${collab.@name}</a>
                                </li>
                            </#list>
                        </ul>
                    </div>
                </#if>

                <#if keywords?has_content>
                    <div id="related-keywords">
                        <h3>Related Keywords</h3>
                        <ul>
                            <#list keywords as keyword>
                                <#if keyword.@name?has_content >
                                    <#assign tst = keyword.@simpleClassName!"" />
                                    <#if keywordTypeBySimpleName[tst]?? >
                                        <#assign keywordType = keywordTypeBySimpleName[tst] />
                                        <li>
                                            <#assign term = keyword.@id />
                                            <#if !keywordType.fieldName?contains("IdList")>
                                                <#assign term = keyword.@name?url />
                                            </#if>
                                            <a href="<@s.url value="/search/results?groups%5B0%5D.operator=AND&groups%5B0%5D.${keywordType.fieldName}%5B0%5D=${term}&groups%5B0%5D.fieldTypes%5B0%5D=${keywordType}"/>">${keyword.@name}</a>
                                        </li>
                                    </#if>
                                </#if>
                            </#list>
                        </ul>
                    </div>
                </#if>
                <div>
                    <small>Related Keywords and Creators are determined by looking at all of the Creators and Keywords
                        associated with a Creator and highlighting the most commonly used.
                    </small>
                </div>
            </div>
        </div>
        </#if>

    <h1><#if creator.properName??>${creator.properName}</#if></h1>
        <#assign scope="http://schema.org/Person"/>
        <#if creator.creatorType.institution >
            <#assign scope="http://schema.org/Organization"/>
        </#if>

    <div itemscope itemtype="${scope}">
        <meta itemprop="name" content="${creator.properName}"/>
        <#if creator.url?has_content>
            <a href="${creator.url?html}" onclick="TDAR.common.outboundLink(this);">${creator.url?html}</a>
        </#if>

        <#if creator.institution??>

            <a itemprop="affiliation" href="<@s.url value="/browse/creators/${creator.institution.id?c}"/>">${creator.institution}</a>
        </#if>

        <@common.description creator.description />

        <#if creator.synonyms?has_content>
            <p class="small"><b>Alternate Names:</b> <#list creator.synonyms as syn> <#if syn_index !=0>,</#if>${syn.properName}</#list>
            </p>
        </#if>
        <br/>
        <#if creator.creatorType.person>
            <#if creator.url?has_content || creator.orcidId?has_content>
                <table class='tableFormat table'>
                    <tr>
                        <td><b>URL:</b> <#if creator.url?has_content><a href="${creator.url}">${creator.url}</a></#if></td>
                        <td><b>ORCID Identifier:</b> <#if creator.orcidId?has_content><a href="http://orcid.org/${creator.orcidId}">${creator.orcidId}</a></#if>
                        </td>
                    </tr>
                </table>
            </#if>
            <#if showBasicInfo >
                <table class='tableFormat table'>
                    <#assign registered = false />
                    <#if (creator.registered)?has_content>
                        <#assign registered = creator.registered>
                    </#if>
                    <#if showAdminInfo>
                        <tr>
                            <td>
                                <B>Registered</b>: ${registered?string}
                            </td>
                            <td>
                                <B>Username</b>: ${creator.username!"N/A"}
                            </td>
                        </tr>
                    </#if>
                    <tr>
                        <td <#if !showAdminInfo>colspan=2</#if>>
                            <B>Registered Professional Archaeologist</B>:${creator.rpaNumber!"no"}
                        </td>
                        <td>
                            <#if showAdminInfo>
                            <#if (creator.lastLogin)?has_content>
                                <@_datefield "Last Login"  creator.lastLogin />
                            <#else>
                                <@_textfield "Last Login"  "No record" />
                            </#if>
                        <#else>
                                <@_boolean "Registered User" registered />
                            </#if>
                        </td>
                    </tr>
                    <tr>
                        <#if creator.emailPublic || (editor || id == authenticatedUser.id) >
                            <td itemprop="email">
                                <@_textfield "Email" creator.email />
                            </td>
                        <#else>
                            <td>
                                <@_textfield "Email" "Not Shown" />
                            </td>
                        </#if>
                        <#if creator.phonePublic || (editor || id == authenticatedUser.id)>
                            <td itemprop="telephone">
                                <@_textfield "Phone" creator.phone true />
                            </td>
                        <#else>
                            <td>
                                <@_textfield "Phone" "Not Shown" />
                            </td>
                        </#if>
                    </tr>
                    <tr>
                        <td colspan=2>
                            <#escape x as x?html>
                    <@_textfield "Contributor Reason" creator.contributorReason true />
                    </#escape>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <#escape x as x?html>
                    <@_textfield "TOS Version" creator.tosVersion true />
                    </#escape>
                        </td>
                        <td>
                            <#escape x as x?html>
                                <@_textfield "Agreement Version" creator.contributorAgreementVersion true />
                                <br/>
                                <@_textfield "Affiliation" creator.affilliation />

                            </#escape>
                        </td>
                    </tr>
                </table>
                <#if (editor || id == authenticatedUser.id) >
                    <h3>Future Contact Information</h3>
                    <p>The institution or person that should be contacted in the future about draft, or confidential materials you have uploaded if you are un-reachable <i>(edit profile to add or modify)</i><p>
                    <#if creator.proxyInstitution?has_content>
                        <a href="<@s.url value="/browse/creators/${creator.proxyInstitution.id?c}"/>">${creator.proxyInstitution}</a>
                    <#else>
                        None Specified
                    </#if>
                    <p>${creator.proxyNote!""}</p>
                </#if>

                <@common.resourceUsageInfo />
                <#if (editor || id == authenticatedUser.id) >

                    <#if creator.registered?? >
                        <div class="row">
                            <div class="span6">
                                <@common.billingAccountList accounts />
                            </div>
                            <div class="span6">
                                <h2>Group Membership</h2>
                                <ul>
                                    <#list groups as group>
                                        <li>${group}</li>
                                    </#list>
                                </ul>
                            </div>
                        </div>
                    </#if>
                    <#-- Person addresses should only be viewable/editable by tdar-editor and above -->
                    <#if creator.addresses?has_content >
                        <h3>Addresses</h3>

                        <div class="row">
                            <#list creator.addresses  as address>
                                <div class="span3">
                                    <@common.printAddress  address=address creatorType=creator.creatorType?lower_case creatorId=creator.id />
                                </div>
                            </#list>
                        </div>
                    </#if>
                </#if>
            </#if>
        <#else>
            <#-- Institution addresses can be shown to anybody (but can only be edited by tdar-editor and above) -->
            <#if creator.addresses?has_content >
                <h3>Addresses</h3>

                <div class="row">
                    <#list creator.addresses  as address>
                        <div class="span3">
                            <@common.printAddress  address=address creatorType=creator.creatorType?lower_case creatorId=creator.id />
                        </div>
                    </#list>
                </div>
            </#if>
        </#if>
    </div>
    </#if>

    <#if ( results?? && results?size > 0) >
    <div id="divResultsSortControl">
        <div class="row">
            <div class="span4">
                <@search.totalRecordsSection tag="h2" helper=paginationHelper itemType="Record"/>
            </div>
            <div class="span5">
                <#if !hideFacetsAndSort>
                    <div class="form-horizontal pull-right">
                        <@search.sortFields true/>
                    </div>
                </#if>
            </div>
        </div>
    </div>

        <#if editor && creatorXml?has_content>
        ${creatorXml?html}
        </#if>

    <div class="tdarresults">
        <@list.listResources resourcelist=results sortfield="RESOURCE_TYPE" titleTag="h5" orientation="LIST"/>
    </div>
        <@search.basicPagination "Results"/>
    <#else>
        <#if (creator.properName)?has_content>
        No Resources associated with ${creator.properName}
        </#if>
    </#if>
    <#if editor>
    <p><b>This Creator Page was Viewed:</b>${viewCount} times</p>
    </#if>

    <#macro _datefield _label _val="" _alwaysShow=true>
        <#if _alwaysShow || _val?is_date>
        <b>${_label}</b>
            <#if _val?is_date>
                <@view.shortDate _val true/>
            </#if>
        </#if>
    </#macro>

    <#macro _textfield _label _val="" _alwaysShow=true>
        <#if _alwaysShow || _val?has_content >
        <b>${_label}:</b> ${_val}
        </#if>
    </#macro>

<#-- FIXME: jim: this is the worst thing you've ever written.  -->
    <#macro _boolean _label _val _show=true trueString="Yes" falseString="No">
        <#if _show>
        <b>${_label}:</b>
            <#if _val>${trueString}<#else>${falseString}</#if>
        </#if>
    </#macro>
</#escape>