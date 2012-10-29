<#escape _untrusted as _untrusted?html>
<#include "navigation-macros.ftl">
<#import "view-macros.ftl" as view>
<#import "common.ftl" as common>
<#assign DEFAULT_SORT = 'PROJECT' />
<#assign DEFAULT_ORIENTATION = 'LIST' />
<#-- 
Convert long string to shorter string with ellipses. If you pass it anything fancy (like 
html markup) you will probably not like the results
-->
    <#macro printTag tagName className closing>
    	<#if tagName?has_content>
    		<<#if closing>/</#if>${tagName} class="${className}" <#nested><#rt/>>
    	</#if>
    </#macro>

<#macro listResources resourcelist=resources_ sortfield=DEFAULT_SORT editable=false bookmarkable=authenticated expanded=false listTag='ul' itemTag='li' headerTag="h3" titleTag="h3" orientation=DEFAULT_ORIENTATION mapPosition="" mapHeight="">
  <#local showProject = false />
  <#local prev =""/>
  <#local first = true/>
  <#assign listTag_=listTag/>  
  <#assign itemTag_=itemTag/> 
  <#assign itemClass = ""/>
  
  <#if orientation == "GRID">
	<#assign listTag_="div"/>  
    <#assign itemClass = "span2"/>
	<#assign itemTag_="div"/> 
  <#elseif orientation == "MAP" >
	<#assign listTag_="ol"/>  
	<#assign itemTag_="li"/> 
	<div class="row">
  	<#if mapPosition=="top" || mapPosition == "right">
  	<div class="span9 google-map" <#if mapHeight?has_content>style="height:${mapHeight}px"</#if>>
  
	</div>
	</#if>	
	
		<div class="<#if mapPosition=='left' || mapPosition=="right">span3<#else>span9</#if>">


  </#if>
  <#if resourcelist??>
  <#list resourcelist as resource>
    <#local key = "" />
    <#local defaultKeyLabel="No Project"/>
    <#-- if we're a resource && are viewable -->
    <#if resource?? && (!resource.viewable?has_content || resource.viewable) >
		<#-- handle grouping/sorting  -->
        <#if sortfield?contains('RESOURCE_TYPE') || sortfield?contains('PROJECT')>
            <#if sortfield?contains('RESOURCE_TYPE')>
                <#local key = resource.resourceType.plural />
                <#local defaultKeyLabel="No Resource Type"/>  
            </#if>
            <#if sortfield?contains('PROJECT')>
                <#local defaultKeyLabel="No Project"/>
                <#if resource.project??>
                    <#local key = resource.project.titleSort />
                <#elseif resource.resourceType.project >
                    <#local key = resource.titleSort />
                </#if>
            </#if>
            <#-- print header and group/list tag -->
            <#if first || (prev != key) && key?has_content>
                <#if prev?has_content></${listTag_}></#if>
                <${headerTag}><#if key?has_content>${key}<#else>${defaultKeyLabel}</#if></${headerTag}>
                <${listTag_} class='resource-list'>
            </#if>
            <#local prev=key />
        <#elseif resource_index == 0>
            <#-- default case for group tag -->
            <@printTag listTag_ "resource-list row ${orientation}" false />
        </#if>  
		<#-- printing item tag -->
            <@printTag itemTag_ "listItem ${itemClass!''}" false>
            <#if orientation == 'MAP' && resource.firstActiveLatitudeLongitudeBox?has_content> data-lat="${resource.firstActiveLatitudeLongitudeBox.minObfuscatedLatitude?c}"
			data-long="${resource.firstActiveLatitudeLongitudeBox.minObfuscatedLongitude?c}" </#if>
			</@printTag>

            <#if itemTag_?lower_case != 'li'>
				<#if resource_index != 0>
				<#if orientation != 'GRID'>
					<hr/>
				<#elseif resource_index   % 4 = 0>
            		</div>	</div><hr /><div class="row ${orientation} resource-list"><div class="span2">
				</#if>
				</#if>
            </#if>
            <#if orientation == 'GRID'>
    			<a href="<@s.url value="/${resource.urlNamespace}/${resource.id?c}"/>" target="_top"><@view.firstThumbnail resource /></a><br/>
            </#if>
            <@searchResultTitleSection resource titleTag />
			<@printLuceneExplanation  resource />
			<@printDescription resource=resource expanded=expanded orientation=orientation length=500 showProject=showProject/>

            </${itemTag_}>
        <#local first=false/>
     </#if>
    </#list>
  </${listTag_}>
  </#if>
  <#if orientation == "MAP">
  </div>
  	<#if mapPosition=="left" || mapPosition == "bottom">
	<div class="span9 google-map" <#if mapHeight?has_content>style="height:${mapHeight}px"</#if> >
	
	</div>
	</#if>	
	</div>    
	<#-- JIM FIXME: WHERE SHOULD I GO -->
		<script>
		$(function() {
		
		  $(".google-map", '#articleBody').one("mapready", function(e, myMap) {
			var bounds = new google.maps.LatLngBounds();
			var markers = new Array();
			var infowindows = new Array();
			var i=0;
			$("ol.MAP li").each(function() {
				i++;
				var $this = $(this);
				if ($this.attr("data-lat") && $this.attr('data-long')) {
					var infowindow = new google.maps.InfoWindow({
					    content: $this.html()
					});
					var marker = new google.maps.Marker({
					    position: new google.maps.LatLng($this.attr("data-lat"),$this.attr("data-long")),
					    map: myMap,
					    icon: 'http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld='+i+'|7a1501|FFFFFF',
					    title:$("a.resourceLink", $this).text()
					});
				
					$(this).click(function() {
						myMap.panTo(marker.getPosition());
					  $(infowindows).each(function() {this.close(myMap);});
					  infowindow.open(myMap,marker);
					  return false;
					});
			
					google.maps.event.addListener(marker, 'click', function() {
					  $(infowindows).each(function() {this.close(myMap);});
					  infowindow.open(myMap,marker);
					});
				
					markers[markers.length] = marker;
					infowindows[infowindows.length] = infowindow;
					bounds.extend(marker.position);
					myMap.fitBounds(bounds);
				};
			}); 
		});
		});
		</script>	  
  </#if>
</#macro>


<#macro printDescription resource=resource expanded=false orientation=DEFAULT_ORIENTATION length=80 showProject=false>
            <#if expanded && orientation != 'GRID'>
                <div class="listItemPart">
		    <#if (resource.citationRecord && !resource.resourceType.project)>
   			<span class='cartouche' title="Citation only; this record has no attached files.">Citation</span></#if>
		    <@common.cartouch resource true><@listCreators resource/></@common.cartouch>  
                <@view.unapiLink resource  />
                <#if showProject && !resource.resourceType.project >
                <p class="project">${resource.project.title}</p>
                </#if>
                <#if resource.description?has_content && !resource.description?starts_with("The information in this record has been migrated into tDAR from the National Archaeological Database Reports Module") >
                    <p class="abstract">
                        <@common.truncate resource.description!"No description specified." length />
                    </p>
                </#if>
                <br/>
                </div>
            </#if>
</#macro>

	<#macro printLuceneExplanation resource>
            <blockquote class="luceneExplanation">
    	        <#if resource.explanation?has_content><b>explanation:</b>${resource.explanation}<br/></#if>
			</blockquote>
            <blockquote class="luceneScore">
	            <#if resource.score?has_content><b>score:</b>${resource.score}<br/></#if>
			</blockquote>
	</#macro>


<#macro searchResultTitleSection result titleTag>
    <#local titleCssClass="search-result-title-${result.status!('ACTIVE')}" />
	<!-- <h3><a href="">Casa Grande Ruins National Monument, A Centennial History of the First Prehistoric Reserve, 1892-1992</a></h3> -->
    <#if titleTag?has_content>
        <${titleTag} class="${titleCssClass}">
    </#if>
    <a class="resourceLink" href="<@s.url value="/${result.urlNamespace}/${result.id?c}"/>"><#rt>
        ${result.title!"No Title"}<#t>
        <#if (result.date?has_content && (result.date > 0 || result.date < -1) )>(${result.date?c})</#if>
    </a><#lt>
    <@bookmark result false/>
    <#if titleTag?has_content>
        </${titleTag}>
    </#if>
</#macro>


<#macro listCreators resource_>
     <#assign showSubmitter=true/>
     <#if resource_.primaryCreators?has_content>
      <span class="authors">
        <#list resource_.primaryCreators as creatr>
          <#assign showSubmitter=false/>
          ${creatr.creator.properName}<#if creatr__has_next??>,<#else>.</#if>
        </#list>
      </span>
    </#if>    

     <#if resource_.editors?has_content>
      <span class="editors">
        <#list resource_.editors as creatr>
          <#assign showSubmitter=false/>
          <#if creatr_index == 0><span class="editedBy">Edited by:</span></#if>
          ${creatr.creator.properName}<#if creatr__has_next??>,<#else>.</#if>
        </#list>
      </span>
    </#if>

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

<#macro bookmark _resource showLabel=true useListItem=false>
  <#if sessionData?? && sessionData.authenticated>
      <#if _resource.resourceType?has_content>
      <#assign status = "disabled-bookmark" />

        <#if bookmarkedResourceService.isAlreadyBookmarked(_resource, authenticatedUser)>
	       <#assign status = "un-bookmark" />
		<#else>
	       <#assign status = "bookmark" />
		</#if>
		
		<#if useListItem>
			<li class="${status}">
		</#if>

        <#if _resource.deleted?? && _resource.deleted>
            <#if !useListItem><img src='<@s.url value="/images/desaturated/bookmark.png"/>' alt='bookmark(unavailable)' title='Deleted items cannot be bookmarked.' /><#t></#if>
            <#if showLabel>
                <span class="disabled" title='Deleted items cannot be bookmarked.'>bookmark</span><#t>
            </#if>
        <#elseif bookmarkedResourceService.isAlreadyBookmarked(_resource, authenticatedUser)>
            <a href="<@s.url value='/resource/removeBookmark' resourceId='${_resource.id?c}'/>" class="bookmark" onclick='removeBookmark(${_resource.id?c}, this); return false;'>
                <#if !useListItem><img src='<@s.url value="/images/bookmark.gif"/>'/><#t></#if>
                <#if showLabel>
                    <span class="bookmark">un-bookmark</span><#t>
                </#if>
            </a><#t>
        <#else>
            <a href="<@s.url value='/resource/bookmark' resourceId='${_resource.id?c}'/>" onclick='bookmarkResource(${_resource.id?c}, this); return false;'>
                <#if !useListItem><img src='<@s.url value="/images/unbookmark.gif"/>'/><#t></#if>
                <#if showLabel>
                    <span class="bookmark"> bookmark</span><#t>
                </#if>
            </a><#t>    
        </#if>

		<#if useListItem>
			</li>
		</#if>
		
      </#if>
  </#if>
</#macro>
</#escape>
