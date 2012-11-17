<#import "/WEB-INF/macros/search/search-macros.ftl" as search>
<#import "/WEB-INF/macros/resource/edit-macros.ftl" as edit>
<#import "/WEB-INF/macros/resource/common.ftl" as common>
<#include "/WEB-INF/macros/resource/navigation-macros.ftl">



<head>
<title>Search ${siteAcronym}</title>
<style type="text/css">
</style>

</head>
<body>
<h1>Search ${siteAcronym}</h1>
<div class="usual">
<ul class="nav nav-tabs" id="myTab"> 
  <li  class="active"><a href="#resource" data-toggle="tab">Resource</a></li> 
  <li><a href="#collection" data-toggle="tab">Collection</a></li> 
  <li><a href="#institution" data-toggle="tab">Institution</a></li> 
  <li><a href="#person" data-toggle="tab">Person</a></li> 
</ul> 
<div class="tab-content">
<div id="resource" class="tab-pane active" >

<@s.form action="results" method="GET" id="searchGroups" cssClass="form-horizontal">
<div class="searchgroup" >
<h2>Choose Search Terms</h2>
<#assign currentIndex = 0 />
<#if (g?size > 0) >
 <#list g as group>
    <#assign currentIndex = currentIndex + 1 />
         
     <@searchGroup group_index group />
 </#list>
<#else>
     <@searchGroup 0 "" />
</#if>
    </div>

    <div class="glide"  id="searchFilter">
    <@search.narrowAndSort />
    </div>

    <div>
        <div id="error"></div>
        <@s.submit id="searchButton" value="Search" cssClass="btn btn-large btn-primary" /> 
    </div>
        
    </div>

</@s.form>
    <div id="collection" class="tab-pane">
        <div class="glide">
        <h3>Search For Collections By Name</h3>
        <@s.form action="collections" method="GET" id='searchForm2'>
            <@search.queryField freeTextLabel="Collection Name" showLimits=false showAdvancedLink=false />
        </@s.form>
        </div>
        <div id="collection-spacer" style="height:850px"></div>
    </div>
    <div id="institution" class="tab-pane">
        <div class="glide">
        <h3>Search For Institutions By Name</h3>
        <@s.form action="institutions" method="GET" id='searchForm3'>
            <@search.queryField freeTextLabel="Institution Name" showLimits=false showAdvancedLink=false />
        </@s.form>
        </div>
        <div id="collection-spacer" style="height:850px"></div>
    </div>

    <div id="person" class="tab-pane">
        <div class="glide">
        <h3>Search For Person By Name</h3>
        <@s.form action="people" method="GET" id='searchForm4'>
            <@search.queryField freeTextLabel="Person Name" showLimits=false showAdvancedLink=false />
        </@s.form>
        </div>
        <div id="collection-spacer" style="height:850px"></div>
    </div>
    
</div> 
</div>
</div>
<script>
$(document).ready(function(){
    //switch to the correct tab if coming from collection search

    //other view init stuff;
    if($('#large-google-map').length) {
        var mapdiv = $('#large-google-map')[0];
        TDAR.maps.initMapApi();
        TDAR.maps.setupEditMap(mapdiv, $("#latlongoptions")[0]);
        
    }

    serializeFormState();

    if ($("#autosave").val() != '') {
        $("#searchGroups").html($("#autosave").val());
    }
    initAdvancedSearch();
    

});
</script>

<form name="autosave" style="display:none;visibility:hidden" >
<textarea  id="autosave"></textarea>
</form>

<table id="template" style="display:none;visibility:hidden">
        <tr class="basicTemplate termrow">
            <td class="searchTypeCell">
                <@searchTypeSelect "{termid}" />
            </td>
            <td class="searchfor"> 
            </td>
            <td> <@removeRowButton /> </td>
        </tr>
        <tr>
            <td></td>
            <td class="searchfor">
    <#list allSearchFieldTypes as fieldType>
        <@fieldTemplate fieldType=fieldType fieldIndex="{termid}" groupid="{groupid}" />
    </#list>
    </td>
    </tr>
</table>        
</body>
<#macro fieldTemplate fieldType="NONE" fieldIndex=0 groupid=0>
    <#assign proxy_index="0"/>
    <#assign prefix="tmpl"/>
    <#if fieldType?is_hash>
        <#if fieldType="TDAR_ID">
             <div class="term retain  ${fieldType}">
                <@s.textfield type="text" name="groups[${groupid}].${fieldType.fieldName}[${fieldIndex}]" cssClass="number" />
             </div>
        <#elseif fieldType.simple>
             <div class="term retain  ${fieldType}">
                <@s.textfield type="text" name="groups[${groupid}].${fieldType.fieldName}[${fieldIndex}]" cssClass="input-xxlarge" />
                </div>
        <#elseif fieldType="COVERAGE_DATE_RADIOCARBON" || fieldType="COVERAGE_DATE_CALENDAR" >
             <div class="term ${fieldType} controls control-row">
                <#assign type="CALENDAR_DATE">
                <#if fieldType !="COVERAGE_DATE_CALENDAR">
                    <#assign type="RADIOCARBON_DATE">
                </#if>
                <@s.hidden name="groups[${groupid}].coverageDates[${fieldIndex}].dateType" value="${type}" cssClass="coverageDateType" />
    
                <@s.textfield  theme="simple" placeholder="Start Year" cssClass="coverageStartYear" name="groups[${groupid}].coverageDates[${fieldIndex}].startDate" maxlength="10" /> 
                <@s.textfield  theme="simple" placeholder="End Year" cssClass="coverageEndYear" name="groups[${groupid}].coverageDates[${fieldIndex}].endDate" maxlength="10" />
            </div>
        <#elseif fieldType="KEYWORD_INVESTIGATION">        
            <div class="term KEYWORD_INVESTIGATION">
                <table id="groups[${groupid}].investigationTypeTable[${fieldIndex}]" class="field">
                <tbody>
                    <tr><td>
                        <@s.checkboxlist name='groups[${groupid}].investigationTypeIdLists[${fieldIndex}]' list='allInvestigationTypes' listKey='id' listValue='label'  numColumns=2  cssClass="smallIndent" />
                    </td></tr>
                </tbody>
                </table>
            </div>
        <#elseif fieldType="KEYWORD_SITE">        
            <div class="term KEYWORD_SITE">
                <table id="groups[${groupid}].siteTypeKeywordTable[${fieldIndex}]" class="field">
                <tbody>
                    <tr><td><@s.checkboxlist theme="hier" id="myid" name="groups[${groupid}].approvedSiteTypeIdLists[${fieldIndex}]" keywordList="allApprovedSiteTypeKeywords" /></td></tr>
                </tbody>
                </table>
            </div>
        <#elseif fieldType="KEYWORD_MATERIAL">        
            <div class="term KEYWORD_MATERIAL">
                <table id="groups[${groupid}].materialTypeTable[${fieldIndex}]" class="field">
                <tbody>
                    <tr><td>
                        <@s.checkboxlist name='groups[${groupid}].materialKeywordIdLists[${fieldIndex}]' list='allMaterialKeywords' listKey='id' listValue='label'  numColumns=2 />
                    </td></tr>
                </tbody>
                </table>
            </div>
        <#elseif fieldType="KEYWORD_CULTURAL">        
            <div class="term KEYWORD_CULTURAL">
                <table id="groups[${groupid}].siteTypeKeywordTable[${fieldIndex}]" class="field">
                <tbody>
                    <tr><td><@s.checkboxlist theme="hier" id="myid" name="groups[${groupid}].approvedCultureKeywordIdLists[${fieldIndex}]" keywordList="allApprovedCultureKeywords" /></td></tr>
                </tbody>
                </table>
            </div>
        </div>
        <#elseif fieldType ="RESOURCE_CREATOR_PERSON">
        <div class="term RESOURCE_CREATOR_PERSON">
        <!-- FIXME: REPLACE WITH REFERENCE TO EDIT-MACROS -->
            <span class="creatorPerson "  id="group_${groupid}_row_${fieldIndex}_parent">
            <div class="control-group">
                <div class="controls controls-row">
	                <@s.hidden name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].person.id" id="group_${groupid}_${fieldIndex}_person_id" onchange="this.valid()"  autocompleteParentElement="#group_${groupid}_row_${fieldIndex}_parent"  />
	                <@s.textfield cssClass="nameAutoComplete" placeholder="Last Name"  theme="simple"  
	                     autocompleteName="lastName" autocompleteIdElement="#group_${groupid}_${fieldIndex}_person_id" autocompleteParentElement="#group_${groupid}_row_${fieldIndex}_parent"
	                    name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].person.lastName" maxlength="255" cssClass="span2" /> 
	                <@s.textfield cssClass="nameAutoComplete" placeholder="First Name" theme="simple" 
	                     autocompleteName="firstName" autocompleteIdElement="#group_${groupid}_${fieldIndex}_person_id" autocompleteParentElement="#group_${groupid}_row_${fieldIndex}_parent"
	                    name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].person.firstName" maxlength="255" cssClass="span2"  />
                <@s.select theme="simple"  name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].role" emptyOption=true listValue='label' label="Role" list=relevantPersonRoles cssClass="creator-role-select span3" />
                </div>
                <div class="controls controls-row">
                <#if authenticated>
	                <@s.textfield cssClass="nameAutoComplete" placeholder="Email (Optional)" theme="simple" 
	                     autocompleteName="email" autocompleteIdElement="#group_${groupid}_${fieldIndex}_person_id" autocompleteParentElement="#group_${groupid}_row_${fieldIndex}_parent"
	                    name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].person.email" maxlength="255" cssClass="span3" />
                </#if>
                <@s.textfield cssClass="nameAutoComplete" placeholder="Institution Name (Optional)" theme="simple" 
                     autocompleteName="institution" autocompleteIdElement="#group_${groupid}_${fieldIndex}_person_id" autocompleteParentElement="group_${groupid}_row_${fieldIndex}_parent"
                    name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].person.institution.name" maxlength="255" />
            </div>
            </div>
            </span>
        </div>
        
     <#elseif fieldType="RESOURCE_CREATOR_INSTITUTION">
    <!-- FIXME: REPLACE WITH REFERENCE TO EDIT-MACROS -->
        <div class="term retain RESOURCE_CREATOR_INSTITUTION">
            <span class="creatorInstitution" id="group_${groupid}_${fieldIndex}_institution_parent">
            <div class="control-group">
                <@s.hidden name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].institution.id" id="group_${groupid}_${fieldIndex}_institution_id"/>
            <div class="controls control-row">
                <@s.textfield theme="simple" cssClass="institutionAutoComplete institution" placeholder="Institution Name" theme="simple" 
                     autocompleteName="name" autocompleteIdElement="#group_${groupid}_${fieldIndex}_institution_id" autocompleteParentElement="#group_${groupid}_${fieldIndex}_institution_parent"
                    name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].institution.name" maxlength="255" cssClass="span4" />
                <@s.select theme="simple" name="groups[${groupid}].resourceCreatorProxies[${fieldIndex}].role" theme="simple" 
                emptyOption=true listValue='label' label="Role " list=relevantInstitutionRoles />
            </div>
            </div>
            </span>
        </div>
    
    <!-- FIXME: refactor to not repeat the same block -->
    <#elseif fieldType = 'DATE_CREATED'>
        <div class="term retain ${fieldType} controls control-row">
            <@s.textfield cssClass="placeholdered number" theme="simple" placeholder='yyyy' labelposition="left" name="groups[${groupid}].${fieldType.fieldName}[${fieldIndex}].start" label="After"/>
            <@s.textfield cssClass="placeholdered number" theme="simple" placeholder='yyyy'labelposition="left" name="groups[${groupid}].${fieldType.fieldName}[${fieldIndex}].end" label ="Before"/>
        </div>            
    
    <#elseif fieldType?starts_with("DATE_")>
        <div class="term retain ${fieldType} controls control-row">
            <@s.textfield cssClass="placeholdered datepicker" theme="simple" placeholder="m/d/yy" labelposition="left" name="groups[${groupid}].${fieldType.fieldName}[${fieldIndex}].start" label="After"/>
            <@s.textfield cssClass="placeholdered datepicker" theme="simple" placeholder="m/d/yy" labelposition="left" name="groups[${groupid}].${fieldType.fieldName}[${fieldIndex}].end" label ="Before"/>
        </div>            
    <#elseif fieldType="PROJECT">
    <!-- FIXME: refactor to not repeat the same block -->
        <@templateProject fieldIndex groupid />
    <#elseif fieldType="COLLECTION">
    <!-- FIXME: refactor to not repeat the same block -->
        <@templateCollection fieldIndex groupid />
        
    </#if>
    </#if>
</#macro>



<#macro option value="" label="" init="" disabled="" >
    <option <#if disabled?has_content>disabled="${disabled}"</#if> value="${value}" <#if (value == init)>selected=selected</#if>>${label}</option>
</#macro>

<#macro searchTypeSelect id="0" init="" groupid="0" >
    <select id="group${id}searchType" name="groups[${groupid}].fieldTypes[${id}]" class="control-label searchType" style="font-size:smaller" >
        <#assign groupName = ""/>
        <#list allSearchFieldTypes as fieldType>
            <#if !fieldType.hidden>
                <#if groupName != (fieldType.fieldGroup!"NONE") >
                <#if groupName != "">
                    </optgroup>
                </#if>
                <#assign groupName="${fieldType.fieldGroup}" />
                    <optgroup label="${fieldType.fieldGroup.label}">
                </#if>
                <@option value="${fieldType}" label="${fieldType.label}" init="${init}" />
            </#if>
        </#list>
        </optgroup>
    </select>
</#macro>



<#macro searchGroup groupid group_ >
        <div class="groupingSelectDiv" style="display:none">
            <label>Grouping</label>
            <#assign defaultOperator = "AND"/>
            <#if (group_?is_hash && group_.or ) >
                <#assign defaultOperator="OR" />
            </#if>
            
            <select name="groups[${groupid}].operator" >
                <option value="AND" <#if defaultOperator=="AND">selected</#if>>Show results that match ALL the terms below</option>
                <option value="OR" <#if defaultOperator=="OR">selected</#if>>Show results that match ANY of the terms below</option>
            </select>
        </div>
        <div id="groupTable0" class="grouptable repeatLastRow" style="width:100%" callback="setDefaultTerm" data-groupnum="0" data-add-another="add another search term">
        
            <#if group_?is_hash >
                <#list group_.fieldTypes as fieldType >
                <#if fieldType??>
                    <div id="grouptablerow_0_" class="control-group termrow repeat-row">
                        <@searchTypeSelect id="${fieldType_index}" init="${fieldType}" groupid="${groupid}" />
                        <div class="controls controls-row">
                            <div class="span8 term-container">
                                <@fieldTemplate fieldType=fieldType fieldIndex=fieldType_index groupid=groupid />
                            </div>
                            <div class="span1">
                                <@removeRowButton />
                            </div>
                        </div>
                    </div>
                </#if>
                </#list>
            <#else>
                <@blankRow />
            </#if>
        </div>
</#macro>

<#macro blankRow groupid=0 fieldType_index=0>
                <div id="grouptablerow_${groupid}_" class="control-group termrow repeat-row">
                    <@searchTypeSelect />
                    <div class="controls controls-row">
                        <div class="span8 term-container">
                            <span class="term retain  ALL_FIELDS">
                                <input type="text" name="groups[${groupid}].allFields[${fieldType_index}]" class="input-xxlarge" />
                            </span>
                        </div>
                        <div class="span1">
                            <@removeRowButton />
                        </div>
                    </div>
                </div>
</#macro>

<#macro removeRowButton>
            <button class="btn  btn-mini repeat-row-delete " type="button" tabindex="-1" onclick="deleteParentRow(this)"><i class="icon-trash"></i></button>
</#macro>


<#-- TODO: replace elseif block w/ dynamic macro calls -->
<#macro dynamic_call macroName fieldIndex="{termid}" groupid="{groupid}">
    <#local macrocall = .vars[macroName] />
    <@macrocall fieldIndex groupid />
</#macro>

<!-- FIXME: refactor to not repeat the same block -->
<#macro templateProject fieldIndex="{termid}" groupid="{groupid}">
        <div class="term PROJECT">
            <@s.hidden name="groups[${groupid}].projects[${fieldIndex}].id" id="projects_${groupid}_${fieldIndex}_id" />
            <@common.combobox cssClass="input-xxlarge projectcombo" name="groups[${groupid}].projects[${fieldIndex}].title" 
                autocompleteIdElement="#projects_${groupid}_${fieldIndex}_id" 
                target="" label="" placeholder="enter project name" value=""  bootstrapControl=false />
        </div>
</#macro>

<!-- FIXME: refactor to not repeat the same block -->
<#macro templateCollection fieldIndex="{termid}" groupid="{groupid}">
        <div class="term COLLECTION">
            <@s.hidden name="groups[${groupid}].collections[${fieldIndex}].id" id="collections_${groupid}_${fieldIndex}_id" />
            <@common.combobox name="groups[${groupid}].collections[${fieldIndex}].name" id="collections_${groupid}_${fieldIndex}_name"  
                cssClass="input-xxlarge collectioncombo" autocompleteIdElement="#collections_${groupid}_${fieldIndex}_id" 
                target="" label="" placeholder="enter collection name" value="" bootstrapControl=false/>
        </div>
</#macro>




