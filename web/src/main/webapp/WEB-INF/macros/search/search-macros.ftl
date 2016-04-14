<#escape _untrusted as _untrusted?html>
    <#macro queryField freeTextLabel="Search" showAdvancedLink=true showLimits=false submitLabel="Search">

        <@s.textfield placeholder="${freeTextLabel}" id='queryField' name='query' size='81' value="${query!}" cssClass="input-xxlarge"/>
        <#if showAdvancedLink><span class="help-inline"><a style="display:inline" href="<@s.url value="/search/advanced"/>">advanced search</a></span></#if>
        <@s.submit value="${submitLabel}" cssClass="btn btn-primary" />
        <#nested>
        <#if showLimits>
        <br/>
            <@narrowAndSort />
        </#if>
    <br/>
    </#macro>

    <#macro narrowAndSort>
    <h2>Narrow Your Search</h2>

        <@s.checkboxlist id="includedResourceTypes" numColumns=4 spanClass="span2" name='resourceTypes' list='allResourceTypes'  listValue='label' label="Resource Type"/>

        <#if authenticated>
        <#--FIXME: there seems to be a bug in numColumns when the value is 'too high' (not sure what that number is yet) -->
        <#--FIXME: also,  we need a good,efficient way to emit bootstrap's version of an inline checkboxlist -->
            <@s.checkboxlist theme="bootstrap" id="myincludedstatuses" name='includedStatuses' list='allStatuses'  listValue='label' label="Status" />
        </#if>

    <h4>Limit by geographic region:</h4>
    <div id="latlongoptions">
        <div id='large-map' class='leaflet-map-editable' data-search="true">
            <span class="latlong-fields">
                <@s.hidden name="groups[0].latitudeLongitudeBoxes[0].maximumLongitude" id="maxx" cssClass="ne-lng latLongInput maxx" />
                <@s.hidden name="groups[0].latitudeLongitudeBoxes[0].minimumLatitude"  id="miny" cssClass="sw-lat latLongInput miny" />
                <@s.hidden name="groups[0].latitudeLongitudeBoxes[0].minimumLongitude" id="minx" cssClass="sw-lng latLongInput minx" />
                <@s.hidden name="groups[0].latitudeLongitudeBoxes[0].maximumLatitude"  id="maxy" cssClass="ne-lat latLongInput maxy" />
            </span>
            <div class="mapdiv"></div>
        </div>
    </div>


    <h2>Sorting Options and Submit</h2>
        <@sortFields />
    </#macro>

    <#macro typeSelected type>
        <#if !resourceTypes??>
            <#if type == 'DOCUMENT' || type == 'DATASET'>checked="checked"</#if>
        <#else>
            <#if resourceTypes.contains(type)>checked="yes"</#if>
        </#if>
    </#macro>

    <#macro resourceTypeLimits>
    <div class="field col3 resourceTypeLimits">
        <input type="checkbox" name="resourceTypes"  <@typeSelected "PROJECT" /> value="PROJECT" id="resourceTypes_Project"/>
        <label for="resourceTypes_Project">Projects</label>
        <input type="checkbox" name="resourceTypes" <@typeSelected "DOCUMENT" /> value="DOCUMENT" id="resourceTypes_Document"/>
        <label for="resourceTypes_Document">Documents</label>
        <input type="checkbox" name="resourceTypes" <@typeSelected "DATASET" /> value="DATASET" id="resourceTypes_Dataset"/>
        <label for="resourceTypes_Dataset">Datasets</label>
        <br/>
        <input type="checkbox" name="resourceTypes" <@typeSelected "IMAGE" /> value="IMAGE" id="resourceTypes_Image"/>
        <label for="resourceTypes_Image">Images</label>
        <input type="checkbox" name="resourceTypes" <@typeSelected "SENSORY_DATA" /> value="SENSORY_DATA" id="resourceTypes_Sensory_Data"/>
        <label for="resourceTypes_Sensory_Data">Sensory Data</label>
        <input type="checkbox" name="resourceTypes" <@typeSelected "CODING_SHEET" /> value="CODING_SHEET" id="resourceTypes_Coding_Sheet"/>
        <label for="resourceTypes_Coding_Sheet">Coding Sheets</label>
        <br/>
        <input type="checkbox" name="resourceTypes" <@typeSelected "ONTOLOGY" /> value="ONTOLOGY" id="resourceTypes_Ontology"/>
        <label for="resourceTypes_Ontology">Ontologies</label>
        <br/>
    </div>
    </#macro>

    <#macro sortFields label="Sort By">
    <label>${label}
        <@s.select value="sortField" name='sortField' cssClass="input-large" theme="simple"
        emptyOption='false' listValue='label' list='%{sortOptions}'/>
    <#--FIXME: move this block to tdar.common.js, bind if select has 'autoreload' class -->
    </label>
    </#macro>

    <#macro rssUrlTag url>
    <link rel="alternate" type="application/atom+xml" title="Atom 1.0" href="<@s.url value="${url}" />"/>
    </#macro>


    <#macro headerLinks includeRss=false>
    <meta name="totalResults" content="${totalRecords}"/>
    <meta name="startIndex" content="${startRecord}"/>
    <meta name="itemsPerPage" content="${recordsPerPage}"/>
        <#if includeRss>
            <@rssUrlTag url=rssUrl />
        </#if>
        <#if (nextPageStartRecord < totalRecords) >
        <link rel="next" href="<@searchUrl ""><@s.param name="startRecord" value="${nextPageStartRecord?c}"/></@searchUrl>"/>
        </#if>
        <#if  paginationHelper.hasPrevious() >
        <link rel="previous"
              href="<@searchUrl "" ><#if prevPageStartRecord !=0><@s.param name="startRecord" value="${prevPageStartRecord?c}" /><#else><@s.param name="startRecord" value="" /></#if></@searchUrl>"/>
        </#if>
    </#macro>


    <#macro searchLink path linkText>
    <a href="<@searchUrl path><#nested></@searchUrl>">${linkText}</a>
    </#macro>

    <#macro searchUrl path><@s.url includeParams="all" value="${path}"><#if path?? && path!="results"><@s.param name="id" value=""/><@s.param name="keywordType" value=""/><@s.param name="slug" value=""/></#if><#nested></@s.url></#macro>

    <#macro refineUrl actionName=actionName>
        <#local _actionmap = {"results": "advanced", "people": "person", "collections": "collection", "institutions":"institution"}><#t>
        <#local _path = _actionmap[actionName]><#t>
        <@searchUrl _path/><#t>
    </#macro>

    <#macro paginationLink startRecord path linkText>
    <span class="paginationLink">
        <@searchLink path linkText>
    	<#if startRecord != 0>
            <@s.param name="startRecord" value="${startRecord?c}" />
        <#else>
            <@s.param name="startRecord" value="" />
        </#if>
        <@s.param name="recordsPerPage" value="${recordsPerPage?c}" />
    </@searchLink>
    </span>
    </#macro>

    <#macro join sequence delimiter=",">
        <#list sequence as item>
        ${item}<#if item_has_next>${delimiter}</#if><#t>
        </#list>
    </#macro>

    <#macro pagination path="results" helper=paginationHelper>
    <div id="divPaginationSection" class="">
        <#if (helper.totalNumberOfItems >0)>
            <table class="pagin">
                <tr>
                    <#if helper.hasPrevious()>
                        <td class="prev">
                            <@paginationLink startRecord=helper.previousPageStartRecord path=path linkText="Previous" />
                        </td>
                    </#if>
                    <td class="page">
                        <ul>
                            <#if (0 < helper.minimumPageNumber) >
                                <li>
                                    <@paginationLink startRecord=0 path=path linkText="First" />
                                </li>
                                <li>...</li>
                            </#if>
                            <#list helper.minimumPageNumber..helper.maximumPageNumber as i>
                                <li>
                                    <#if i == helper.currentPage>
                                        <span class="currentResultPage">${i + 1}</span>
                                    <#else>
                                        <@paginationLink startRecord=(i * helper.itemsPerPage) path=path linkText=(i + 1) />
                                    </#if>
                                </li>
                            </#list>
                            <#if (helper.maximumPageNumber < (helper.pageCount - 1))>
                                <li>...</li>
                                <li>
                                    <@paginationLink startRecord=helper.lastPage path=path linkText="Last" />
                                </li>
                            </#if>
                        </ul>
                    </td>
                    <#if (helper.hasNext()) >
                        <td class="next">
                            <@paginationLink startRecord=helper.nextPageStartRecord path=path linkText="Next" />
                        </td>
                    </#if>
                </tr>
            </table>
        </div>
        </#if>
    </#macro>

    <#macro bcad _year>
        <#if (_year < 0)>BC<#else>AD</#if><#t/>
    </#macro>


    <#macro basicPagination label="Records" showIfOnePage=false helper=paginationHelper>
        <#if (helper.pageCount > 1)>
        <div class="glide">
            <@pagination ""/>
        </div>
        </#if>
    </#macro>

    <#macro totalRecordsSection tag="h2" helper=paginationHelper itemType="Resource">
    <${tag} class="totalRecords">
    ${helper.startRecord}-${helper.endRecord} (${helper.totalNumberOfItems} ${itemType}<#if (helper.totalNumberOfItems != 1)>s</#if>)
    </${tag}>

    </#macro>


    <#macro facetBy facetlist=[] currentValues=[] label="Facet Label" facetParam="" ulClass="media-list tools" liCssClass="media" action=actionName link=true icon=true pictoralIcon=false>
        <#if (facetlist?? && !facetlist.empty)>
            <#if label != ''>
            <h4>${label}:</h4>
            </#if>
        <ul class="${ulClass}">
            <#list facetlist as facet>
                <li class="${liCssClass}">
                    <#if (facetlist?size > 1)>

                        <span class="media-body">
                <#if link><#t>
                <a rel="noindex" href="<#compress><@s.url action=action includeParams="all">
                    <@s.param name="${facetParam}">${facet.raw}</@s.param>
                    <@s.param name="startRecord" value="0"/>
                    <#if (documentType!'') == '' && facetParam != 'documentType'>
                        <@s.param name="documentType" value=""/>
                    </#if>
                    <#if (fileAccess!'') == '' && facetParam != 'fileAccess'>
                        <@s.param name="fileAccess" value=""/>
                    </#if>
                    <#if facetParam != "integratableOptions">
                        <@s.param name="integratableOptions" value=""/>
                    </#if>
                    <#nested>
                </@s.url></#compress>"></#if>
<#compress>
                    <#if icon || pictoralIcon><#if pictoralIcon && facetParam=='selectedResourceTypes'>
                        <svg class="svgicon red"><use xlink:href="/images/svg/symbol-defs.svg#svg-icons_${facet.raw?lower_case}"></use></svg>
                    <#else>
                        <#if currentValues?size == 1>
                            <svg class="svgicon grey"><use xlink:href="/images/svg/symbol-defs.svg#svg-icons_selected"></use></svg>
                        <#else>
                            <svg class="svgicon grey"><use xlink:href="/images/svg/symbol-defs.svg#svg-icons_deselected"></use></svg>
                        </#if>
                    </#if></#if>
                <@s.text name="${facet.label}"/>
				<#if link></a></#if>
				 <span>(${facet.count})</span></span></#compress>
                    <#elseif (currentValues?size > 0) >
                        <@removeFacet facetlist=currentValues facetParam=facetParam />
                    <#else>
                        <span class="media-body"> <@s.text name="${facet.label}"/> <span>(${facet.count})</span></span>
                    </#if>
                </li>
            </#list>
        </ul>
        </#if>

    </#macro>

    <#macro removeFacet facetlist="" label="Facet Label" facetParam="">
        <#if facetlist?has_content>
            <#if (facetlist?is_collection)>
                <#if facetlist?size == 1>
                    <#assign facet= facetlist.get(0) />
                </#if>
            <#elseif (facetlist?is_string) >
                <#assign facet= facetlist />
            </#if>
            <#if facet?has_content>
                <#assign facetText=facet/>
                <#if facet.plural?has_content><#assign facetText=facet.plural/>
                <#elseif facet.label?has_content><#assign facetText=facet.label/>
                </#if>
        
        <ul class="media-list tools">
                <li class="media">
                    <span class="media-body">
            <a rel="noindex" href="<@s.url includeParams="all">
            <@s.param name="${facetParam}"value="" />
            <@s.param name="startRecord" value="0"/>
            <#if facetParam != "documentType">
                <@s.param name="documentType" value=""/>
            </#if>
            <#if facetParam != "integratableOptions">
                <@s.param name="integratableOptions" value=""/>
            </#if>
            <#nested>
        </@s.url>">
                        <svg class=" svgicon grey"><use xlink:href="/images/svg/symbol-defs.svg#svg-icons_selected"></use></svg>
                        ${facetText}</a>
                    </span>
                </li>
        </ul>
            </#if>
        </#if>
    </#macro>


<#macro personInstitutionSearch>

    <#if (totalRecords > 0)>
        <#if !hideFacetsAndSort>
        <div id="sidebar-left" parse="true" class="options hidden-phone">

            <h2 class="totalRecords">Search Options</h2>
            <ul class="tools media-list">
                <li class="media"><a href="<@refineUrl/>" rel="noindex"><i class="search-magnify-icon-red"></i> Refine your search &raquo;</a></li>

                <#if (contextualSearch!false)>
                    <#if projectId??>
                        <li class="media"><@s.a href="/project/${projectId?c}"><i class="icon-project icon-red"></i> Return to project page &raquo;</@s.a></li>
                    <#else>
                        <li class="media"><@s.a href="/collection/${collectionId?c}"><i class="icon-collection icon-red"></i> Return To collection
                            page &raquo;</@s.a></li>
                    </#if>
                </#if>

                <!--        <li>Subscribe via &raquo;
	            <a class="subscribe"  href="${rssUrl}">RSS</a>
	        </li> -->
            </ul>

        </div>
        <div class="visible-phone">
            <a href="<@refineUrl />">Refine your search &raquo;</a>
        </div>
        </#if>

    <div id="divResultsSortControl">
        <div class="row">
            <div class="span3">
                <@totalRecordsSection tag="h2" helper=paginationHelper itemType="Result" />
            </div>
            <div class="span6 form-inline">
                <div class="pull-right">
                    <div class="control-group"></div>
                    <label>Records Per Page
                        <@s.select  theme="simple" id="recordsPerPage" cssClass="input-small" name="recordsPerPage"
                        list={"10":"10", "25":"25", "50":"50"} listKey="key" listValue="value" />
                    </label>
                    <#if !hideFacetsAndSort>
                        <@sortFields />
                    </#if>
                </div>
            </div>
        </div>
    </div>

    <div class="tdarresults">
        <#assign indx = 0/>
        <#list results as result>
            <#if result?has_content>
                <#if indx != 0>
                    <hr/></#if>
                <#assign indx = indx + 1/>
                <div class="listItemPart">
                    <h3 class="search-result-title-${result.status}">
                        <a class="resourceLink" href="${result.detailUrl}">${result.properName}</a>
                    </h3>
                    <#if result.institution?has_content><p>${result.institution.name}</p></#if>
                    <blockquote class="luceneExplanation">${result.explanation!""}</blockquote>
                    <blockquote class="luceneScore">
                    <b>score:</b>${result.score!""}<br> </blockquote>
                </div>
            </#if>
        </#list>
    </div>
        <@pagination ""/>

    <#else>
    <h2>No records match the query.</h2>
    </#if>

</#macro>

<#function activeWhen _actionNames>
    <#local _active = false>
    <#list _actionNames?split(",") as _actionName>
        <#local _active = _active || (_actionName?trim == actionName)>
    </#list>
    <#return _active?string("active", "") />
</#function>

<#macro toolbar>
    <ul class="nav nav-tabs" id="myTab">
        <li class="${activeWhen('basic,advanced,results')}"><a href="advanced">Resource</a></li>
        <li class="${activeWhen('collection,collections')}"><a href="/search/collection">Collection</a></li>
        <li class="${activeWhen('institution,institutions')}"><a href="/search/institution">Institution</a></li>
        <li class="${activeWhen('person,people')}"><a href="/search/person">Person</a></li>
    </ul>
</#macro>

</#escape>