<#escape _untrusted as _untrusted?html>
    <#import "view-macros.ftl" as view>
    <#import "common.ftl" as common>
    <#assign DEFAULT_SORT = 'RELEVANCE' />
    <#assign DEFAULT_ORIENTATION = 'LIST_FULL' />

<#-- emit a list of resource summary information (e.g. for a a search results page, or a resource collection view page
    @param resourcelist:list<Persistable> List of Resources or Collections. Required.
    @param sortField:SortOption?  sort value.  Note, this macro does sort the provided list.  Instead, it uses sortOption as
            a hint that governs how the macro formats the list header and list items. Default: "relevance"
    @param itemsPerRow:number? how many items to show per row in "GRID" orientation
    @param listTag:string? by default wrap all resources in a (<ul/> for lists; <div/> for grid; <ol/> for map), alternately, you can change this to something else
    @param itemTag:string? by default wrap each item in a (<li/> for lists and map; <div/> for grid), alternately, specify something
    @param headerTag:string? if the search results is sorted in some manner wrap the header in a <h3/>, the tag to use for the header
    @param titleTag:string? the tag to wrap the resource title in, default <h3/>
    @param orientation:string the default orientation for the results (LIST_FULL,  LIST, GRID, MAP). Default: "LIST_FULL"
    @param mapPositon: where to show the map in relation to the result list
    @param mapHeight: how high the map should be
-->
    <#macro listResources resourcelist sortfield=DEFAULT_SORT itemsPerRow=4
    listTag='ul' itemTag='li' headerTag="h3" titleTag="h3" orientation=DEFAULT_ORIENTATION mapPosition="" mapHeight="">

        <#local showProject = false />
        <#global prev =""/>
        <#global first = true/>
        <#local listTag_=listTag/>
        <#local itemTag_=itemTag/>
        <#local itemClass = ""/>
        <#global isGridLayout = (orientation=="GRID") />
        <#global isMapLayout = (orientation=="MAP") />

        <@common.reindexingNote />

    <#-- set default ; add map wrapper -->
        <#if orientation == "GRID">
            <#local listTag_="div"/>
            <#local itemClass = "span2"/>
            <#local itemTag_="div"/>
        <#elseif orientation == "MAP" >
            <#local listTag_="ol"/>
            <#local itemTag_="li"/>
        <div class="resource-list row">
            <#if mapPosition=="top" || mapPosition == "right">
                <@_mapDiv mapPosition mapHeight />
            </#if>
        <div class="<#if mapPosition=='left' || mapPosition=="right">span3<#else>span9</#if>">
        </#if>

        <#local rowCount = -1 />

        <#if resourcelist??>
            <#list resourcelist as resource>
                <#assign key = "" />
                <#assign defaultKeyLabel="Individual Resources"/>
            <#-- if we're viewable -->
                <#if ((resource.viewable)!false) >
                    <#local rowCount= rowCount+1 />

                <#-- list headers are displayed when sorting by specific fields ResourceType and Project -->
                    <@_printListHeaders sortfield first resource headerTag orientation listTag_ />
                <#-- printing item tag start / -->
                    <${itemTag_} class="listItem ${itemClass!''}"
                    <#if orientation == 'MAP' && resource.latLongVisible >
                        <#local box = resource.firstActiveLatitudeLongitudeBox />
                        data-lat="${box.centerLatitude?c}"
                        data-long="${box.centerLongitude?c}"
                        data-lat-length="${box.absoluteLatLength?c}"
                        data-long-length="${box.absoluteLongLength?c}"
                        data-scale="${resource.firstActiveLatitudeLongitudeBox.scale?c}"
                    </#if>
                    id="resource-${resource.id?c}">

                <#-- if we're at a new row; close the above tag and re-open it (bug) -->
                    <@_printDividerBetweenResourceRows itemTag_ first rowCount itemsPerRow orientation />

                <#-- add grid thumbnail -->
                    <#if isGridLayout>
                        <a href="<@s.url value="${resource.detailUrl}"/>" target="_top"><#t>
	            <@view.firstThumbnail resource /><#t>
                        </a><br/>
                    </#if>

                <#-- add the title -->
                    <@searchResultTitleSection resource titleTag />

                <#-- if in debug add lucene description to explain relevancy -->
                    <@_printLuceneExplanation  resource />

                <#-- print resource's description -->
                    <@_printDescription resource=resource orientation=orientation length=500 showProject=showProject/>

                <#-- close item tag -->
                </${itemTag_}>
                    <#local first=false/>
                </#if>
            </#list>

        <#-- if we didn't have any results, don't close the list tag, as there was none -->
            <#if rowCount != -1>
            </${listTag_}>
            </#if>
        </#if>

        <#if orientation == "MAP">
        </div>
            <#if mapPosition=="left" || mapPosition == "bottom">
				<@_mapDiv mapPosition mapHeight />
            </#if>
        </div>
        </#if>

    </#macro>

	<#macro _mapDiv mapPosition mapHeight>
		<#local span = "span9" />
        <#if (mapPosition == 'left' || mapPosition == 'right') && ((rightSidebar!false) || (leftSidebar!false)) >
			<#local span = "span6" />
		</#if>	
		<div class="${span} leaflet-map-results" <#if mapHeight?has_content>style="height:${mapHeight}px"</#if>
        <#assign map_ = "" />
        <#if map?has_content>
            <#assign map_ = map />
        </#if>
        <#if !map_?has_content && (g[0].latitudeLongitudeBoxes[0])?has_content>
            <#assign map_ = g[0].latitudeLongitudeBoxes[0] />
        </#if>
        <#if map_?has_content && map_.valid && map_.minimumLatitude?has_content >
        data-maxy="${map_.maxObfuscatedLatitude}" 
        data-minx="${map_.minObfuscatedLongitude}"
        data-maxx="${map_.maxObfuscatedLongitude}"
        data-miny="${map_.minObfuscatedLatitude}"
        </#if> >

		</div>
	</#macro>

<#-- divider between the sections of results -->
    <#macro _printDividerBetweenResourceRows itemTag_ first rowCount itemsPerRow orientation>
        <#if itemTag_?lower_case != 'li'>
        <#-- if not first result -->
            <#if !first>
                <#if (!isGridLayout)>
                <hr/>
                <#elseif rowCount % itemsPerRow == 0>
                </div>    </div>
                <hr/>
                <div class=" ${orientation} resource-list row">
                <div class="span2">
                </#if>
            </#if>
        </#if>
    </#macro>

    <#macro _printListHeaders sortfield first resource=null headerTag="" orientation='LIST' listTag_='li'>
    <#-- handle grouping/sorting with indentation -->
    <#-- special sorting for RESOURCE_TYPE and PROJECT to group lists by these; sort key stored in "key" -->
        <#if (sortfield?contains('RESOURCE_TYPE') || sortfield?contains('PROJECT')) && resource.resourceType?has_content>
            <#if sortfield?contains('RESOURCE_TYPE')>
                <#assign key = resource.resourceType.plural />
                <#assign defaultKeyLabel="No Resource Type"/>
            </#if>
            <#if sortfield?contains('PROJECT')>
                <#if resource.project??>
                    <#assign key = resource.project.title />
                <#elseif resource.resourceType.project >
                    <#assign key = resource.title />
                </#if>
            </#if>
        <#-- print special header and group/list tag -->
            <#if first || (prev != key) && key?has_content>
                <#if prev != '' || sortField?has_content && !first && (sortField?contains("RESOURCE_TYPE") || sortField?contains("PROJECT"))></${listTag_}
                    ></#if>
                <${headerTag}><#if key?has_content>${key}<#else>${defaultKeyLabel}</#if></${headerTag}>

            <#-- if we're a grid, then reset rows -->
                <#if isGridLayout>
                <div class='resource-list row ${orientation}'>
                <#else>
                <${listTag_} class="resource-list ${orientation}">
                </#if>
            </#if>
            <#assign prev=key />
        <#elseif first>
        <#-- default case for group tag -->
            <#if isGridLayout>
            <div class='resource-list row ${orientation}'>
            <#else>
                <${listTag_} class="resource-list ${orientation}">
            </#if>
        </#if>
    </#macro>

    <#macro _printDescription resource=resource orientation=DEFAULT_ORIENTATION length=80 showProject=false>
        <#if resource?has_content>
            <#local _desc = "Description not available"/>
            <#if (resource.description)?has_content >
                <#if !resource.description?starts_with("The information in this record has been migrated into tDAR from the National Archaeological Database Reports Module")>
                    <#local _desc = resource.description />
                </#if>
            </#if>
            <#local _rid = resource.id?c >
            <#if resource.class.simpleName == 'ResourceCollection'>
                <#local _rid = "C${resource.id?c}" >
            </#if>

            <#if orientation == 'LIST_FULL'>
                <div class="listItemPart">
                    <#if (resource.citationRecord?has_content && resource.citationRecord && !resource.resourceType.project)>
                        <span class='cartouche' title="Citation only; this record has no attached files.">Citation</span>
                    </#if>
                    <@common.cartouche resource true><@_listCreators resource/></@common.cartouche>
                    <#if resource.resourceType?has_content>
                        <@view.unapiLink resource  />
                    </#if>
                    <#if showProject && !resource.resourceType.project >
                        <p class="project">${resource.project.title}</p>
                    </#if>
                    <p class="abstract">
                        <span class="pull-right small">[tDAR id: ${_rid}]</span>
                        <#-- for comparing resources - points to /resource/compare?id=??&id=... -->
                        <#-- <br><span class="compare">compare:</span><input type="checkbox" name='id' style="margin-top:-2px" /></span> -->
                        <@common.truncate _desc length />
                    </p>
                </div>
            </#if>
        </#if>
    </#macro>

<#--emit the lucene score for the specified resource for the current search -->
    <#macro _printLuceneExplanation resource>
        <#if resource.explanation?has_content>
            <blockquote class="luceneExplanation">
                <b>explanation:</b>${resource.explanation}<br/>
            </blockquote>
        </#if>
        <#if resource.score?has_content>
            <blockquote class="luceneScore">
                <b>score:</b>${resource.score}<br/>
            </blockquote>
        </#if>
    </#macro>


<#--emit the title section of the resource (as part of a list of search results), including title, status indicator,
bookmark indicator, etc..
    @param result:Peristable the resource/collection
    @param titleTag:String  name of the html tag that will wrap the actual resource title (e.g. "li", "div", "td")
 -->
    <#macro searchResultTitleSection result titleTag >
        <#local titleCssClass="search-result-title-${result.status!('ACTIVE')}" />
        <#if titleTag?has_content>
            <${titleTag} class="${titleCssClass}">
        </#if>
        <a class="resourceLink" href="<@s.url value="${result.detailUrl}"/>"><#rt>
            <#if result.title?has_content>
            ${result.title!"No Title"} <#if result.status?has_content && (editor || result.viewable) && !result.active >
                <small>[${result.status?upper_case}]</small></#if><#t>
            <#elseif result.properName?has_content>
            ${result.properName!"No Name"}<#t>
            <#else>
                No Title
            </#if>
            <#if (result.date?has_content && (result.date > 0 || result.date < -1) )>(${result.date?c})</#if>
        </a><#lt>
        <#if isMapLayout && result.latLongVisible><i class="icon-map-marker" title="click to highlight on map" alt="click to highlight on map"></i></#if>
        <@bookmark result false/>
        <#if titleTag?has_content>
        </${titleTag}>
        </#if>
    </#macro>

<#--list the author/editor/creators of a resource - part of the summary information included in a a search result item -->
    <#macro _listCreators resource_>
        <#assign showSubmitter=true/>
		<#list resource_.primaryCreators![]>
        <span class="authors">
            <#items as creatr>
                <#assign showSubmitter=false/>
            ${creatr.creator.properName}<#if creatr__has_next??>,<#else>.</#if>
            </#items>
        </span>
        </#list>

		<#list resource_.editors![]>
        <span class="editors">
            <#items  as creatr>
                <#assign showSubmitter=false/>
                <#if creatr_index == 0><span class="editedBy">Edited by:</span></#if>
            ${creatr.creator.properName}<#if creatr__has_next??>,<#else>.</#if>
            </#items>
        </span>
		</#list>

        <#if showSubmitter && resource_.submitter?has_content>
            <#assign label = "Created" />
            <#if resource_.resourceType?has_content>
                <#assign label = "Uploaded" />
            </#if>
        <span class="creators"> 
          <span class="createdBy">${label} by:</span> ${resource_.submitter.properName}
        </span>
        </#if>
    </#macro>

<#--
    Emit the bookmark indicator for a resource.

    As the name implies,  the bookmark indicator indicates whether a resource is "bookmarked". It is represented as a
    star-shaped icon and should typically be presented alongside the resource title.

    More importantly, the bookmark indicator is also a toggle button, and serves as the means for the user to add and
    remove items from the user's bookmarked items list.

    Bookmarked items will appear on the tDAR dashboard page (/dashboard). Bookmarked datasets also appear on the
    integration workspace start page (/workspace/list-tables).

    @param _resource:Resource a resource object
-->

    <#macro bookmark _resource showLabel=true useListItem=false>
        <#if sessionData?? && sessionData.authenticated>
            <#if _resource.resourceType?has_content>
                <#assign status = "disabled-bookmark" />

                <#if _resource.bookmarked>
                    <#assign status = "un-bookmark" />
                <#else>
                    <#assign status = "bookmark" />
                </#if>

                <#if useListItem>
                <li class="${status}">
                </#if>

                <#if _resource.deleted?? && _resource.deleted>
                    <#if showLabel>
                        <span class="disabled" title='Deleted items cannot be bookmarked.'>
            	<i title="disabled boomkark" class="bookmark-icon tdar-icon-bookmark-disabled"></i>
                bookmark</span><#t>
                    </#if>
                <#elseif _resource.bookmarked>
                    <a href="<@s.url value='/resource/removeBookmark' resourceId='${_resource.id?c}'/>" class="bookmark-link" resource-id="${_resource.id?c}"
                       bookmark-state="bookmarked">
                        <i title="bookmark or unbookmark" class="tdar-icon-bookmarked bookmark-icon"></i>
                        <#if showLabel>
                            <span class="bookmark-label">un-bookmark</span><#t>
                        </#if>
                    </a><#t>
                <#else>
                    <a href="<@s.url value='/resource/bookmark' resourceId='${_resource.id?c}'/>" class="bookmark-link" resource-id="${_resource.id?c}"
                       bookmark-state="bookmark">
                        <i title="bookmark or unbookmark" class="bookmark-icon tdar-icon-bookmark"></i>
                        <#if showLabel>
                            <span class="bookmark-label"> bookmark</span><#t>
                        </#if>
                    </a><#t>
                </#if>

                <#if useListItem>
                </li>
                </#if>

            </#if>
        </#if>
    </#macro>


    <#macro table data cols id="tbl${data.hashCode()?string?url}" cssClass="table tableFormat datatableSortable"  colLabels=cols>
    <table id="${id}" class="${cssClass}">
        <thead>
        <tr>
            <#list colLabels as colLabel>
                <th>${colLabel}</th>
            </#list>
        </tr>
        </thead>
        <tbody>
            <#list data as dataRow>
            <tr>
                <#nested dataRow, dataRow_index, cols, colLabels>
            </tr>
            </#list>
        </tbody>
    </table>
    </#macro>

    <#macro easytable data cols id="tblEasyTable" cssClass="table tableFormat datatableSortable" cols=data?keys >
        <@table data cols id cssClass colLabels; rowdata>
            <#list cols as key>
                <#local val = rowdata[key]!"">
                <#if val?is_date><#local val = val?datetime></#if>
            <td>${val}</td>
            </#list>
        </@table>
    </#macro>


    <#macro hashtable data id="tblNameValue" keyLabel="Key" valueLabel="Value" cssClass="table tableFormat datatableSortable">
    <table id="${id}" class="${cssClass}">
        <thead>
        <tr>
            <th>${keyLabel}</th>
            <th>${valueLabel}</th>
        </tr>
        </thead>
        <tbody>
            <#list data?keys as key>
                <#if key?has_content>
                    <#local val = data[key]!''>
                    <#if val?is_date><#local val = val?datetime></#if>
                <tr>
                    <td>${key}</td>
                    <td>${val}</td>
                </tr>
                </#if>
            </#list>
        </tbody>
    </table>
    </#macro>


</#escape>
