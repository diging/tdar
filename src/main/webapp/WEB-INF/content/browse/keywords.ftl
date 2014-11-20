<#escape _untrusted as _untrusted?html>
    <#import "/WEB-INF/macros/resource/view-macros.ftl" as view>
    <#import "/WEB-INF/macros/resource/common.ftl" as common>
    <#import "/WEB-INF/macros/resource/navigation-macros.ftl" as nav>
    <#import "/WEB-INF/macros/resource/list-macros.ftl" as list>
    <#import "/WEB-INF/macros/search/search-macros.ftl" as search>
    
<head>
    
<title>${keyword.label}</title>
    <@view.canonical keyword />
<#--    <#assign rssUrl = "/search/rss?groups[0].fieldTypes[0]=COLLECTION&groups[0].collections[0].id=${resourceCollection.id?c}&groups[0].collections[0].name=${(resourceCollection.name!'untitled')?url}">
    <@search.rssUrlTag url=rssUrl /> -->
    <@search.headerLinks includeRss=false />

</head>
<div class="glide">
    <h1>${keyword.label} <span class="xsmall red">(<@s.text name="${keywordType.localeKey}"/>)</span></h1>
    <#if keyword.synonyms?has_content>
    <p><#list keyword.synonyms![] as synonym> <#if synonym_index !=0>,</#if>${synonym.label} </#list></p>
    </#if>
    <#if keyword.parent?has_content>
    <p><b>Parent:</b><@common.searchFor keyword.parent false /></p>
    </#if>
        <@nav.keywordToolbar "view" />
    
    <p>${keyword.definition!''}</p>
</div>

    <#if ( results?? && results?size > 0) >
        <div id="divResultsSortControl">
            <div class="row">
                <div class="span4">
                    <@search.totalRecordsSection tag="h2" helper=paginationHelper itemType="Record"  />
                </div>
            </div>
        </div>
        <div class="tdarresults">
            <@list.listResources resourcelist=results  listTag="span" itemTag="span" titleTag="h3" orientation=orientation mapPosition="top" mapHeight="450"/>
        </div>
    
    </#if>

        <@search.basicPagination "Results"/>

<script type='text/javascript'>
    $(document).ready(function () {
        TDAR.common.initializeView();
    });
</script>

</#escape>