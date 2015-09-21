<#escape _untrusted as _untrusted?html>

    <#import "/WEB-INF/content/${resource.urlNamespace}/view.ftl" as local_ />
    <#import "/WEB-INF/macros/resource/view-macros.ftl" as view>
    <#import "/WEB-INF/macros/resource/navigation-macros.ftl" as nav>
    <#import "/WEB-INF/macros/resource/common.ftl" as common>

<head>
    <title>${resource.title}</title>
    <meta name="lastModifiedDate" content="$Date$"/>
    <#if includeRssAndSearchLinks??>
        <#import "/WEB-INF/macros/search/search-macros.ftl" as search>
        <#assign rssUrl = "/search/rss?groups[0].fieldTypes[0]=PROJECT&groups[0].projects[0].id=${resource.id?c}&groups[0].projects[0].name=${(resource.name!'untitled')?url}">
        <@search.rssUrlTag url=rssUrl />
        <@search.headerLinks includeRss=false />
    </#if>

    <#noescape>
    ${googleScholarTags}
    </#noescape>
    
    <@view.canonical resource />

    <#if local_.head?? && local_.head?is_macro>
        <@local_.head />
    </#if>

</head>


    <@nav.toolbar "${resource.urlNamespace}" "view">
        <#if resource.resourceType.dataTableSupported && editable>
            <#assign disabled = resource.dataTables?size==0 />
            <@nav.makeLink "dataset" "columns/${persistable.id?c}" "table metadata" "columns" current true disabled "hidden-tablet hidden-phone"/>
            <@nav.makeLink "dataset" "columns/${persistable.id?c}" "metadata" "columns" current true disabled "hidden-desktop"/>
            <#if administrator && resource.project?? && resource.project.id != -1 >
            <@nav.makeLink "dataset" "resource-mapping" "res. mapping" "columns" current true disabled ""/>
            </#if>
        </#if>

        <#if local_.toolbarAdditions?? && local_.toolbarAdditions?is_macro>
            <@local_.toolbarAdditions />
        </#if>
    </@nav.toolbar>

        <#if local_.notifications?? && local_.notifications?is_macro>
            <@local_.notifications />
        </#if>

<div id="datatable-child" style="display:none">
    <p class="">
        You have successfully updated the page that opened this window. What would you like to do now?
    </p>
</div>

    <@view.pageStatusCallout />

<h1 class="view-page-title">${resource.title!"No Title"}</h1>
    <#if resource.project?? && resource.project.id?? && resource.project.id != -1>

    <div id="subtitle">
        <p>Part of the
            <#if resource.projectVisible || editable>
                <a href="<@s.url value='${resource.project.detailUrl}'/>">${resource.project.coreTitle}</a>
            <#else>
            ${resource.project.coreTitle}
            </#if>
            <#if resource.project.draft>(DRAFT)</#if> project
        </p></div>
    </#if>

    <#if editor>
    <div data-spy="affix" class="affix  screen adminbox rotate-90"><a href="<@s.url value="/resource/admin?id=${resource.id?c}"/>">ADMIN</a></div>
    </#if>

<p class="meta">
    <@view.showCreatorProxy proxyList=authorshipProxies />
    <#if resource.date?has_content && resource.date != -1 >
        <@view.kvp key="Year" val=resource.date?c />
    </#if>

    <#if copyrightMandatory && resource.copyrightHolder?? >
        <strong>Primary Copyright Holder:</strong>
        <@view.browse resource.copyrightHolder "copyrightHolder" />
    </p>
    </#if>
</p>

<p class="visible-phone"><a href="#sidebar-right">&raquo; Downloads &amp; Basic Metadata</a></p>
<hr class="dbl">

<h2>Summary</h2>
    <@common.description resource.description />

<hr/>
<@view.resourceCitationSection resource />

    <#if authenticatedUser?has_content>
     <div id="email-form"  class="modal hide fade" tabindex="-1" role="dialog"  aria-hidden="true">
     
         <form id="followup">
          <div class="modal-header">
                <h3>Send Email</h3>
           </div>
           <div class="modal-body">
                <p>Select the type of message you'd like to send to another ${siteAcronym} user.</p>
                 <br/>
                <@s.select name='type'  emptyOption='false' listValue='label' list='%{emailTypes}' label='Email Type'/>
                <#assign contactId = resource.submitter.id />
                <#if contactProxies?has_content>
                <#list contactProxies as prox>
                <#assign contactId = prox.person.id />
                <#if contactId == -1>
                    <#assign contactId = prox.institution.id />
                </#if>
                <#break/>
                </#list>
                </#if>
                <@s.hidden name="toId" value="${contactId?c}" />
                <@s.hidden name="resourceId" value="${resource.id?c}" />
                <#assign fromId = -1 />
                <#if (authenticatedUser.id)?has_content>
                    <#assign fromId = authenticatedUser.id />
                </#if>
                <@s.hidden name="fromId" value="${fromId?c}" /> 
                <@s.textarea name="messageBody" id="messageBody" rows="4" label="Message" cssClass="span5"/>
                <p><b>Note:</b> Please include sufficient information to fulfill your request (e.g. why you are requesting access to a file, or specific comments or corrections). Your contact information and a link to this resource will automatically be included in your message.</p>
                <@common.antiSpam />
            </div>
            <div class="modal-footer">
                 <button name="send" data-dismiss="modal" aria-hidden="true"  id="followup-send" class="button btn btn-primary">send</button>
                 <button name="cancel" data-dismiss="modal" aria-hidden="true"  id="followup-cancel" class="button btn btn-cancel">cancel</button>
            </div>
     </form>
    </div>
    
    
        <div id="emailStatusModal" class="modal hide fade" tabindex="-1" role="dialog"  aria-hidden="true">
          <div class="modal-header">
            <h3 class="success">Success</h3>
            <h3 class="error">Error</h3>
           </div>
           <div class="modal-body">
                <span class="success">
                    <p>Your message has been sent</p>
                </span>
                <span class="error">
                    <p>An error occurred:
                    <ul id="emailErrorContainer">
                    </ul></p>
                </span>
            </div>
            <div class="modal-footer">
                <a href="#" data-dismiss="modal" aria-hidden="true" id="email-close-button" class="btn">Close</a>
            </div>
        </div>
    </#if>
<hr/>
    <#noescape>
        <#if resource.url! != ''>
        <p><strong>URL:</strong><a href="${resource.url?html}" onclick="TDAR.common.outboundLink(this)" rel="nofollow"
                                   title="${resource.url?html}"><@common.truncate resource.url?html 80 /></a></p><br/>
        </#if>
    </#noescape>


    <#if local_.afterBasicInfo?? && local_.afterBasicInfo?is_macro>
        <@local_.afterBasicInfo />
    </#if>

    <#if ( resource.hasBrowsableImages && resource.visibleFilesWithThumbnails?size > 0)>
        <@view.imageGallery />
    <br/>
    <hr/>
    </#if>

    <#if resource.resourceType.dataTableSupported>
        <#if (resource.dataTables?has_content)>
            <#if resource.viewable && authenticated && (resource.publicallyAccessible || ableToViewConfidentialFiles)>
            <h3>Browse the Data Set</h3>

                <#if (resource.dataTables?size > 1)>
                <form>
                    <label for="table_select">Choose Table:</label>
                    <select id="table_select" name="dataTableId" onChange="window.location =  '?dataTableId=' + $(this).val()">
                        <#list resource.dataTables as dataTable_>
                            <option value="${dataTable_.id?c}" <#if dataTable_.id == dataTable.id>selected </#if>
                                    >${dataTable_.displayName}</option>
                        </#list>
                    </select>
                </form>
                </#if>

            <p><@view.embargoCheck /></p>

            <div class="row">
                <div class="span9">
                    <table id="dataTable" class="dataTable table tableFormat table-striped table-bordered"></table>
                </div>
            </div>
                <#if tdarConfiguration.xmlExportEnabled>
                <p class="faims_xml_logo"><a href="/dataset/xml?dataTableId=${dataTable.id?c}" target="_blank">XML</a></p>
                </#if>
			<#else>

	            <p><@view.embargoCheck /></p>

            </#if>

        <h3>Data Set Structure</h3>
        <div class="row">
            <div class="span3"><span class="columnSquare measurement"></span> Measurement Column</div>
            <div class="span3"><span class="columnSquare count"></span>Count Column</div>
            <div class="span3"><span class="columnSquare coded"></span>Coded Column</div>
        </div>
        <div class="row">
            <div class="span3"><span class="columnSquare mapped"></span>Mapping Column</div>
            <div class="span6"><span class="columnSquare integration"></span>Integration Column (has Ontology)</div>
        </div>

            <#list resource.dataTables as dataTable>
            <h4>Table Information: <span>${dataTable.displayName}</span></h4>
            <#if dataTable.description?has_content>
			<p>${dataTable.description}</p>
			</#if>
            <table class="tableFormat table table-bordered">
                <thead class='highlight'>
                <tr>
                    <th class="guide">Column Name</th>
                    <th>Data Type</th>
                    <th>Type</th>
                    <th>Category</th>
                    <th>Coding Sheet</th>
                    <th>Ontology</th>
                </tr>
                </thead>
                <#list dataTable.dataTableColumns?sort_by("sequenceNumber") as column>
                <#assign oddEven="oddC" />
                <#if column_index % 2 == 0>
                    <#assign oddEven="evenC" />
                </#if>
                    <tr>
                        <#assign typeLabel = ""/>
                        <#if column.measurementUnit?has_content><#assign typeLabel = "measurement"/></#if>
                        <#if column.defaultCodingSheet?has_content><#assign typeLabel = "coded"/></#if>
                        <#if (column.defaultCodingSheet.defaultOntology)?has_content><#assign typeLabel = "integration"/></#if>
                        <#if column.columnEncodingType?has_content && column.columnEncodingType.count><#assign typeLabel = "count"/></#if>
                        <#if column.mappingColumn?has_content && column.mappingColumn ><#assign typeLabel = "mapped"/></#if>
                        <#assign hasDescription = false />
                        <#if column.description?has_content >
                            <#assign hasDescription = true />
                        </#if>


                        <td class="guide" nowrap <#if hasDescription>rowspan=2</#if>><span class="columnSquare ${typeLabel}"></span><b>
                        ${column.displayName}
                        </b></td>
                        <#if hasDescription>
                            <td colspan="6" class="${oddEven} descriptionRow" >${column.description}</td></tr><tr>
                        </#if>

                        <td class="${oddEven}"><#if column.columnDataType??>${column.columnDataType.label}&nbsp;</#if></td>
                        <td class="${oddEven}"><#if column.columnEncodingType??>${column.columnEncodingType.label}</#if>
                            <#if column.measurementUnit??> (${column.measurementUnit.label})</#if> </td>
                        <td class="${oddEven}">
                            <#if column.categoryVariable??>
                                <#if column.categoryVariable.parent??>
                                ${column.categoryVariable.parent} :</#if> ${column.categoryVariable}
                            <#else>uncategorized</#if> </td>
                        <td class="${oddEven}">
                            <#if column.defaultCodingSheet??>
                                <a href="<@s.url value="/coding-sheet/${column.defaultCodingSheet.id?c}" />">
                                ${column.defaultCodingSheet.title!"no title"}</a>
                            <#else>none</#if>
                        </td>
                        <td class="${oddEven}">
                            <@_printOntology column />
                        </td>
                    </tr>
                </#list>
            </table>
            </#list>
                <#if resource.relationships?size != 0>
                <h4>Data Table Relationships:</h4>
                <table class="tableFormat table table-striped table-bordered">
                    <thead class="highlight">
                    <tr>
                        <th>Type</th>
                        <th>Local Table</th>
                        <th>Foreign Table</th>
                        <th>Column Relationships</th>
                    </tr>
                    </thead>
                    <#list resource.relationships as relationship>
                        <tr>
                            <td>${relationship.type}</td>
                            <td>${relationship.localTable.displayName}</td>
                            <td>${relationship.foreignTable.displayName}</td>
                            <td>
                                <#list relationship.columnRelationships as colRel>
                                ${colRel.localColumn.displayName} <i class="icon-arrow-right"></i> ${colRel.foreignColumn.displayName}
                                </#list>
                            </td>
                        </tr>
                    </#list>
                </table>
                </#if>

        </#if>
<hr/>    </#if>

    <#macro _printOntology column>
        <#local ont="" />
        <#if (column.defaultCodingSheet.defaultOntology)?has_content>
            <#local ont = column.defaultCodingSheet.defaultOntology />
        </#if>
        <#if ont?has_content>
        <a href="<@s.url value="/ontology/${ont.id?c}"/>">
        ${ont.title!"no title"}</a>
        <#else>
        none
		</#if>
    </#macro>




    <#if resource.resourceType.supporting >
        <@view.categoryVariables />
    </#if>
    <#if !resource.resourceType.project >
        <#if licensesEnabled?? &&  licensesEnabled>
            <@view.license />
        </#if>
    </#if>

    <span class="Z3988" title="<#noescape>${openUrl!""}</#noescape>"></span>

    <#if resource.containsActiveKeywords >
    <h2>Keywords</h2>
        <#if resource.project?has_content && resource.project.id != -1 && resource.projectVisible?? && !resource.projectVisible && resource.inheritingSomeMetadata>
        <em>Note: Inherited values from this project are not available because the project is not active</em>
        </#if>
    <div class="row">
        <#if (resource.keywordProperties?size > 1)>
        <div class="span45">
        <#elseif resource.keywordProperties?size == 1>
        <div class="span9">
        </#if>

        <#list resource.keywordProperties as prop>
        <#-- FIXME: somehow this should be folded into SearchFieldType to not have all of this if/else -->
            <#if ((resource.keywordProperties?size /2)?ceiling == prop_index)>
            </div>
            <div class="span45">
            </#if>
            <#if prop == "activeSiteNameKeywords">
                <@_keywordSection "Site Name" resource.activeSiteNameKeywords "siteNameKeywords" />
            </#if>
            <#if prop == "activeSiteTypeKeywords">
                <@_keywordSection "Site Type" resource.activeSiteTypeKeywords "uncontrolledSiteTypeKeywords" />
            </#if>
            <#if prop == "activeCultureKeywords">
                <@_keywordSection "Culture" resource.activeCultureKeywords "uncontrolledCultureKeywords" />
            </#if>
            <#if prop == "activeMaterialKeywords">
                <@_keywordSection "Material" resource.activeMaterialKeywords "query" />
            </#if>
            <#if prop == "activeInvestigationTypes">
                <@_keywordSection "Investigation Types" resource.activeInvestigationTypes "query" />
            </#if>
            <#if prop == "activeOtherKeywords">
                <@_keywordSection "General" resource.activeOtherKeywords "query" />
            </#if>
            <#if prop == "activeTemporalKeywords">
                <@_keywordSection "Temporal Keywords" resource.activeTemporalKeywords "query" />
            </#if>
            <#if prop == "activeGeographicKeywords">
                <@_keywordSection "Geographic Keywords" resource.activeGeographicKeywords "query" />
            </#if>
        </#list>
        <#if (resource.keywordProperties?size > 0)>
        </div>
        </#if>
    </div>
    <hr/>
    </#if>


    <#macro _keywordSection label keywordList searchParam>
        <#if keywordList?has_content>
        <p>
            <strong>${label}</strong><br>
            <@view.keywordSearch keywordList searchParam false />
        </p>
        </#if>
    </#macro>

    <#if resource.activeCoverageDates?has_content>
    <h2>Temporal Coverage</h2>
        <#list resource.activeCoverageDates as coverageDate>
            <#assign value>
                <#if coverageDate.startDate?has_content>${coverageDate.startDate?c}<#else>?</#if> to
                <#if coverageDate.endDate?has_content>${coverageDate.endDate?c}<#else>?</#if>
                <#if (coverageDate.description?has_content)> (${coverageDate.description})</#if>
            </#assign>
            <@view.kvp key=coverageDate.dateType.label val=value />
        </#list>
    <hr/>
    </#if>


    <#if (resource.activeLatitudeLongitudeBoxes?has_content )>
    <h2>Spatial Coverage</h2>
    <div class="title-data">
        <p>
            min long: ${resource.firstActiveLatitudeLongitudeBox.minObfuscatedLongitude}; min
            lat: ${resource.firstActiveLatitudeLongitudeBox.minObfuscatedLatitude} ;
            max long: ${resource.firstActiveLatitudeLongitudeBox.maxObfuscatedLongitude}; max
            lat: ${resource.firstActiveLatitudeLongitudeBox.maxObfuscatedLatitude} ;
            <!-- ${resource.firstActiveLatitudeLongitudeBox.scale } -->
            <!-- ${resource.managedGeographicKeywords } -->
            <#if userAbleToViewUnobfuscatedMap>
                <#if resource.firstActiveLatitudeLongitudeBox.obfuscatedObjectDifferent> [obfuscated]</#if>
            </#if>
        </p>
    </div>

    <div class="row">
        <div id='large-google-map' class="google-map span9"></div>
    </div>
    <div id="divCoordContainer" style="display:none">
        <input type="hidden" class="ne-lat" value="${resource.firstActiveLatitudeLongitudeBox.maxObfuscatedLatitude}" id="maxy"/>
        <input type="hidden" class="sw-lng" value="${resource.firstActiveLatitudeLongitudeBox.minObfuscatedLongitude}" id="minx"/>
        <input type="hidden" class="ne-lng" value="${resource.firstActiveLatitudeLongitudeBox.maxObfuscatedLongitude}" id="maxx"/>
        <input type="hidden" class="sw-lat" value="${resource.firstActiveLatitudeLongitudeBox.minObfuscatedLatitude}" id="miny"/>
    </div>
    </#if>
    <#if creditProxies?has_content >
    <h3>Individual &amp; Institutional Roles</h3>
        <@view.showCreatorProxy proxyList=creditProxies />
    <hr/>
    </#if>

    <#if (resource.activeResourceAnnotations)?has_content>
    <h3>Record Identifiers</h3>

        <#list allResourceAnnotationKeys as key>
            <#assign contents = "" />
            <#list resource.activeResourceAnnotations as ra>
                <#if key.id == ra.resourceAnnotationKey.id >
                    <#assign contents><#noescape>${contents}<#t/></#noescape><#if contents?has_content>; </#if>${ra.value}<#t/></#assign>
                </#if>
            </#list>
            <#if contents?has_content>
                <#assign keyLabel><#noescape>${key.key}</#noescape>(s)</#assign>
                <@view.kvp key=keyLabel val=contents noescape=true />
            </#if>
        </#list>
    </#if>


    <#if resource.activeResourceNotes?has_content>
    <h2>Notes</h2>
        <#list resource.activeResourceNotes.toArray()?sort_by("sequenceNumber") as resourceNote>
            <@view.kvp key=resourceNote.type.label val=resourceNote.note />
        </#list>
    <hr/>
    </#if>

    <@_relatedSimpleItem resource.activeSourceCollections "Source Collections"/>
    <@_relatedSimpleItem resource.activeRelatedComparativeCollections "Related Comparative Collections" />
    <#if resource.activeSourceCollections?has_content || resource.activeRelatedComparativeCollections?has_content>
    <hr/>
    </#if>
<#-- display linked data <-> ontology nodes -->
    <#if relatedResources?has_content>
    <h3>This ${resource.resourceType.label} is Used by the Following Datasets:</h3>
    <ol style='list-style-position:inside'>
        <#list relatedResources as related >
            <li><a href="<@s.url value="${related.detailUrl}"/>">${related.id?c} - ${rtelated.title} </a></li>
        </#list>
    </ol>
    </#if>

    <@view.unapiLink resource />
    <#if viewableResourceCollections?has_content>
    <h3>This Resource is Part of the Following Collections</h3>
    <p>
        <#list viewableResourceCollections as collection>
            <a href="<@s.url value="${collection.detailUrl}"/>">${collection.name}</a> <br/>
        </#list></p>
    <hr/>
    </#if>

<#--emit additional dataset metadata as a list of key/value pairs  -->
    <#if !resource.resourceType.project && resource.relatedDatasetData?has_content >
        <#assign map = resource.relatedDatasetData />
        <#if map?? && !map.empty>
        <h3>Additional Metadata</h3>
            <#list map?keys as key>
                <#if key?? && map.get(key)?? && key.visible?? && key.visible>
                    <@view.kvp key=key.displayName!"unknown field" val=map.get(key)!"unknown value" />
                </#if>
            </#list>
        </#if>
    </#if>
    <#if !resource.resourceType.project>
        <@view.extendedFileInfo />
    </#if>
    <#if local_.afterFileInfo?? && local_.afterFileInfo?is_macro>
        <@local_.afterFileInfo />
    </#if>
    <@view.accessRights>
    <div>
        <#if resource.embargoedFiles?? && !resource.embargoedFiles>
            The file(s) attached to this resource are <b>not</b> publicly accessible.
            They will be released to the public domain in the future</b>.
        </#if>
    </div>
    </@view.accessRights>


<div id="sidebar-right" parse="true">
    <i class="${resource.resourceType?lower_case}-bg-large"></i>
    <#if whiteLabelLogoAvailable>
        <@s.a href="/collection/${whiteLabelCollection.id?c}/${whiteLabelCollection.slug}"
            title="${whiteLabelCollection.title}"
            ><img src="${whiteLabelLogoUrl}" class="whitelabel-logo"></@s.a>
    </#if>
    <#if !resource.resourceType.project>
        <@view.uploadedFileInfo />
    </#if>
        <ul class="inline">
            <#assign txt><#if !resource.citationRecord>Request Access,</#if> Submit Correction, Comment</#assign>
            <li class="media"><i class="icon-envelope pull-left"></i>
            <div class="media-body">
            <#if (authenticatedUser.id)?has_content>
                    <a href="#" id="emailButton" class="">${txt}</a>
            <#else>
                    <a href="/login?url=${currentUrl}?showEmail">${txt} (requires login)</a>
            </#if>
            </div>
            </li>
        </ul>
    <h3>Basic Information</h3>

    <p>

    <ul class="unstyled-list">
        <#if resource.resourceProviderInstitution?? && resource.resourceProviderInstitution.id != -1>
            <li>
                <strong>Resource Provider</strong><br>
                <@view.browse creator=resource.resourceProviderInstitution />
            </li>
        </#if>

        <#if local_.sidebarDataTop?? && local_.sidebarDataTop?is_macro>
            <@local_.sidebarDataTop />
        </#if>
        <#if (((resource.publisher.name)?has_content ||  resource.publisherLocation?has_content) && !((resource.resourceType.document)!false) )>
            <li><strong>
            <#-- label -->
                <#if resource.documentType?has_content>
                ${resource.documentType.publisherName}
                <#else>
                    Publisher
                </#if></strong><br>
                <#if resource.publisher?has_content><span><@view.browse creator=resource.publisher /></span></#if>
                <#if resource.degree?has_content>${resource.degree.label}</#if>
                <#if resource.publisherLocation?has_content> (${resource.publisherLocation}) </#if>
            </li>
        </#if>
        <#if resource.doi?has_content>
            <li><strong>DOI</strong><br><a href="http://dx.doi.org/${resource.doi}">${resource.doi}</a></li>
        <#elseif resource.externalId?has_content>
            <li><strong>DOI</strong><br>${resource.externalId}</li>
        </#if>
        <#if local_.sidebarDataBottom?? && local_.sidebarDataBottom?is_macro>
            <@local_.sidebarDataBottom />
        </#if>
        <#if resource.resourceLanguage?has_content>
            <li>
                <strong>Language</strong><br>
            ${resource.resourceLanguage.label}
            </li>
        </#if>
        <#if resource.copyLocation?has_content>
            <li>
                <strong>Location</strong><br>
            ${resource.copyLocation}
            </li>
        </#if>
        <li>
            <strong>${siteAcronym} ID</strong><br>
        ${resource.id?c}
        </li>
    </ul>
</div>



    <#if local_.footer?? && local_.footer?is_macro>
        <@local_.footer />
    </#if>


<script type="text/javascript">
    $(function () {
        'use strict';
        TDAR.common.initializeView();
        <#if dataTableColumnJson?has_content && (dataTable.id)?has_content>
        <#noescape>
        var columns = ${dataTableColumnJson};
        </#noescape>
        TDAR.datatable.initalizeResourceDatasetDataTable(columns, ${viewRowSupported?string},${resource.id?c}, "${resource.urlNamespace}", ${dataTable.id?c});
        </#if>

        <#if local_.localJavascript?? && local_.localJavascript?is_macro>
            <@local_.localJavascript />
        </#if>

        TDAR.internalEmailForm.init();    
    });
</script>

<#--emit a list of related items (e.g. list of source collections or list of comparative collections -->
    <#macro _relatedSimpleItem listitems label>
        <#if listitems?has_content>
        <h3>${label}</h3>
        <table>
            <#list listitems as citation>
                <tr>
                    <td>${citation}</td>
                </tr>
            </#list>
        </table>
        </#if>
    </#macro>

    <#macro _keywordSection label keywordList searchParam>
        <#if keywordList?has_content>
        <p>
            <strong>${label}</strong><br>
            <@view.keywordSearch keywordList searchParam false />
        </p>
        </#if>
    </#macro>


</#escape>
