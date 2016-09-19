<#escape _untrusted as _untrusted?html>
<#import "/WEB-INF/macros/resource/list-macros.ftl" as rlist>
<#import "dashboard-common.ftl" as dash />
<#import "/WEB-INF/macros/resource/edit-macros.ftl" as edit>
<#import "/WEB-INF/macros/resource/view-macros.ftl" as view>
<#import "/WEB-INF/macros/search/search-macros.ftl" as search>
<#import "/WEB-INF/macros/resource/common.ftl" as common>
<#import "/${themeDir}/settings.ftl" as settings>

<head>
    <title>${authenticatedUser.properName}'s Dashboard</title>
    <meta name="lastModifiedDate" content="$Date$"/>
    <@edit.resourceDataTableJavascript />

</head>


<div id="titlebar" parse="true">
    <h2>My Library</h2>

</div>
<div class="row">
<div class="span2">
    <@dash.sidebar current="collections" />
</div>
<div class="span10">
        <div class="table">
            <table class="table ">
                <tbody>
            <#list allResourceCollections as collection>
                    <tr>
                        <th><a href="<@s.url value="${collection.detailUrl}"/>">${collection.name}</th>
                        <td>${collection.description}</td>
                        <td>${collection.unmanagedResources?size!0}</td>
                        <td><@moremenu collection /></td>
                    </tr>
           </#list>

                    <tr>
                        <td class="new-resources-count">
                            <span class="badge">23</span>
                        </td>
                        <th>The southwest fauna collection</th>
                        <td>Datasets, coding-sheets, and ontologies collected during the 2012 Annual Southwest Faunal Collection</td>
                        <td><@moremenu /></td>
                    </tr>

                    <tr>
                        <td class="new-resources-count"> </td>
                        <th>New Orleans cemeteries and surounding areas </th>
                        <td>Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s</td>
                        <td><@moremenu /></td>
                    </tr>

                </tbody>
            </table>
        </div>

    <div class="spacer-fold" style="height:500px; border: 1px solid #eee; border-radius: 10px">
        <p><em style="color:#eee">This space left intentionally blank</em></p>
    </div>

    <div class="well">
        <b>I want to be able to:</b>
        <ul>
            <li>View all my collections</li>
            <li>delete collections?</li>
            <li>organize collections?</li>
            <li>add resources to a collection? (maybe)?</li>
            <li>Should bookmarks be a "special" collection in your library? or a dedicated section?</li>
            <li>how to show "shared" libraries?</li>
            <li>two pane layout could work, either top/bottom or right /left where "top" or "right" was dedicated to creation, and other part was for viewing</li>

        </ul>
        <b>more ideas from jim</b>
        <ul>
            <li>present 'gmail-style' list interface</li>
            <li>filter options on left</li>
            <li>clicking inside table row shows  hierarchy tree perhaps? Accordian style?</li>
            <li>(iffy)collapsed row view shows super-terse inventory, e.g.  "12 documents, 2 datasets, 10 images"</li>


        </ul>
    </div>

</div>


</div>

    <#macro moremenu collection="">
    <div class=" pull-right">
        <div class="btn-group">
            <#if collection?has_content>
            <a href="<@s.url value="${collection.detailUrl}"/>"class="btn btn-mini">View</a>
            <a href="<@s.url value="/collection/${collection.id?c}/edit"/>"class="btn btn-mini">Edit</a>
            <#else>
            <a href="<@s.url value="/"/>"class="btn btn-mini">View</a>
            <a href="<@s.url value="/collection/0/edit"/>"class="btn btn-mini">Edit</a>
            </#if>
            </div>
    </div>

    </#macro>


    <#macro collectionsSection>

    <div class="" id="collection-section">
        <h2>My Collections</h2>
        <@common.listCollections collections=allResourceCollections>
            <li><i class="icon-star"></i> My Bookmarks
                <ul>
                    <#list bookmarkedResources as book>
                        <li><a href="${book.detailUrl}">${book.title}</a></li>
                    </#list>
                </ul></li>
            <li><a href="<@s.url value="/collection/add"/>">create one</a></li>
        </@common.listCollections>
    </div>
    <br>
        <#if sharedResourceCollections?? && !sharedResourceCollections.empty >
        <div class="">
            <h2>Collections Shared With You</h2>
            <@common.listCollections collections=sharedResourceCollections />
        </div>
        </#if>

    </#macro>

    <#macro bookmarksSection>
        <div id="bookmarks">
            <#if ( bookmarkedResources?size > 0)>
                <@rlist.listResources resourcelist=bookmarkedResources sortfield='RESOURCE_TYPE' listTag='ol' headerTag="h3" />
            <#else>
            <h3>Bookmarked resources appear in this section</h3>
            Bookmarks are a quick and useful way to access resources from your dashboard. To bookmark a resource, click on the star <i class="icon-star"></i> icon next to any resource's title.
            </#if>
        </div>
    </#macro>

<script>
    $(document).ready(function () {
        TDAR.notifications.init();
        TDAR.common.collectionTreeview();
        $("#myCarousel").carousel('cycle');
    });
</script>



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

    <#list resourcesWithErrors![]>
    <div class="alert-error alert">
        <h3><@s.text name="dashboard.archiving_heading"/></h3>

        <p><@common.localText "dashboard.archiving_errors", serviceProvider, serviceProvider /> </p>
        <ul>
            <#items as resource>
                <li>
                    <a href="<@s.url value="${resource.detailUrl}" />">${resource.title}:
                        <#list resource.filesWithProcessingErrors as file><#if file_index !=0>,</#if>${file.filename!"unknown"}</#list>
                    </a>
                </li>
            </#items>
        </ul>
    </div>
    </#list>

<#if showUserSuggestions!false>
	<#list userSuggestions>
    <div class="alert-error alert">
        <h3>Are any of these you?</h3>
		<ul class="unstyled">
			<#items as sug>
				<li><input type="checkbox" name="merge" value="${sug.id?c}-${sug.properName}">&nbsp;<@s.a value="${sug.detailUrl}">${sug.properName}</@s.a></li>
			</#items>
		</ul>
		<form>
		<!-- fixme - send email via email-controller 
			construct list of people; contruct url for -- dedup /admin/authority/select-authority?selectedDupeIds=...&entityType=PERSON -->
		<button name="yes" class="button btn">Yes</button>
		<button name="no"  class="button btn">No</button>
		</form>
    </div>
	</#list>
</#if>
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
</#escape>