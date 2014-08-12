<#escape _untrusted as _untrusted?html>
    <#import "/WEB-INF/macros/resource/edit-macros.ftl" as edit>
    <#import "/WEB-INF/macros/resource/common.ftl" as common>
    <#import "/WEB-INF/macros/resource/navigation-macros.ftl" as nav>
    <#import "/WEB-INF/macros/resource/view-macros.ftl" as view>
<head>
    <#if persistable.id == -1>
        <title>Create a Collection</title>
    <#else>
        <title>Editing: ${persistable.name}</title>
    </#if>
    <meta name="lastModifiedDate" content="$Date$"/>


</head>
<body>
    <#compress>

    <div id='subnavbar' class="subnavbar-scrollspy affix-top subnavbar resource-nav navbar-static  screen" data-offset-top="250" data-spy="affix">
        <div class="">
            <div class="container">
                <ul class="nav">
                    <li class="alwaysHidden"><a href="#top">top</a></li>
                    <li class="active"><a href="#basicInformationSection">Basic</a></li>
                    <li><a href="#divAccessRights">Rights</a></li>
                    <li><a href="#divResourcesSesction">Resources</a></li>
                </ul>
                <div id="fakeSubmitDiv" class="pull-right">
                    <button type=button class="button btn btn-primary submitButton" id="fakeSubmitButton">Save</button>
                    <img alt="progress indicator" title="progress indicator" src="<@s.url value="/images/indicator.gif"/>" class="waitingSpinner" style="display:none"/>
                </div>
            </div>
        </div>
    </div>

    <div id="sidebar-right" parse="true">
        <div id="notice">
            <h3>Introduction</h3>
            This is the editing form for a Collection.
        </div>
    </div>


    <h1><#if persistable.id == -1>Creating<#else>Editing</#if>: <span> ${persistable.name!"New Collection"}</span></h1>
        <@s.form name='metadataForm' id='metadataForm'  method='post' cssClass="form-horizontal" enctype='multipart/form-data' action='save'>
        <@s.token name='struts.csrf.token' />
        <@common.jsErrorLog />
        <h2>Basic Information</h2>

        <div class="" id="basicInformationSection" data-tiplabel="Basic Information"
             data-tooltipcontent="Enter a name and description for this collection.  You may also choose a &quot;parent
    collection&quot; which allows you to inherit all of the access permissions defined by the parent.">
            <#if resourceCollection.id?? &&  resourceCollection.id != -1>
                <@s.hidden name="id"  value="${resourceCollection.id?c}" />
            </#if>
            <@s.hidden name="startTime" value="${currentTime?c}" />
            <@s.textfield labelposition='left' label='Collection Name' name='resourceCollection.name'  cssClass="required descriptiveTitle input-xxlarge"  title="A title is required for all collections." maxlength="255" />

            <div id="parentIdContainer" class="control-group">
                <label class="control-label">Parent Collection</label>

                <div class="controls">
                    <@s.hidden name="parentId"  id="hdnParentId" cssClass=""
                    autocompleteParentElement="#parentIdContainer"  />
            <@s.textfield theme="simple" name="parentCollectionName" cssClass="input-xxlarge notValidIfIdEmpty collectionAutoComplete"  autocomplete="off"
                autocompleteIdElement="#hdnParentId" maxlength=255 autocompleteParentElement="#parentIdContainer" autocompleteName="name"
                placeholder="parent collection name" id="txtParentCollectionName"
                dynamicAttributes={"data-msg-notValidIfIdEmpty":"Invalid collection name.  Type a collection name (or partial name) in this field and choose an option from the menu that appears below."}
                />
                </div>
            </div>



            <@s.textarea rows="4" labelposition='top' label='Collection Description' name='resourceCollection.description'
            cssClass='resizable input-xxlarge' title="Please enter the description " />


            <#if administrator>
                <@s.textarea rows="4" labelposition='top' label='Collection Description (allows html)' name='resourceCollection.adminDescription'
                cssClass='resizable input-xxlarge' title="Please enter the description " />
            </#if>

        </div>

        <div id="divBrowseOptionsTips" style="display:none">
            <p>Choose whether this collection will be public or private, and how ${siteAcronym} will sort the resources when displaying this collection to other
                users. Marking a collection as "private" does not restrict access to the resources within it.</p>
            <ul>
                <li>Public collections are viewable to all ${siteAcronym} users and accessible from the &quot;Browse Collections&quot; page.</li>
                <li>Private collections are only viewable to the users specified in the <a href="#accessRights">Access Rights</a> section.</li>
            </ul>
        </div>
        <div class="glide" data-tiplabel="Browse and Display Options" data-tooltipcontent="#divBrowseOptionsTips">
            <h2>Browse and Display Options</h2>

            <div class="control-group">
                <label class="control-label">Make this collection public?</label>

                <div class="controls">
                    <label for="rdoVisibleTrue" class="radio inline"><input type="radio" id="rdoVisibleTrue" name="resourceCollection.visible"
                                                                            value="true" <@common.checkedif resourceCollection.visible true /> />Yes</label>
                    <label for="rdoVisibleFalse" class="radio inline"><input type="radio" id="rdoVisibleFalse" name="resourceCollection.visible"
                                                                             value="false" <@common.checkedif resourceCollection.visible false /> />No</label>
                </div>
            </div>

            <@s.select labelposition='top' label='When Browsing Sort Resource By' name='resourceCollection.sortBy'
            listValue='label' list='%{sortOptions}' title="Sort resource by" />

            <@s.select labelposition='top' label='Display Collection as' name='resourceCollection.orientation'
            list='%{ResultsOrientations}'  listValue='label'  title="Display as" />
        </div>

        <div id="divCollectionAccessRightsTips" style="display:none">
            <p>Determines who can edit a document or related metadata. Enter the first few letters of the person's last name.
                The form will check for matches in the ${siteAcronym} database and populate the related fields.</p>
            <em>Types of Permissions</em>
            <dl>
                <dt>View All</dt>
                <dd>User can view/download all file attachments associated with the resources in the collection.</dd>
                <dt>Modify Record
                <dt>
                <dd>User can edit the resources listed in the collection.
                <dd>
                <dt>Administer Collection
                <dt>
                <dd>User can edit resources listed in the collection, and also modify the contents of the collection.
                <dd>
            </dl>
        </div>
            <@edit.fullAccessRights tipsSelector="#divCollectionAccessRightsTips" label="Users who can View or Modify this Collection"/>

        <div class="glide" id="divResourcesSesction" data-tiplabel="Add/Remove Resources" data-tooltipcontent="Check the items in this table to add them to the collection.  Navigate the pages
                    in this list by clicking the left/right arrows at the bottom of this table.  Use the input fields above the table to limit the number
                    of results.">
            <h2>Add/Remove Resources</h2>

            <@edit.resourceDataTable false true />



            <div id="divNoticeContainer" style="display:none">
                <div id="divAddProjectToCollectionNotice" class="alert">
                    <button type="button" class="close" data-dismiss="alert" data-dismiss-cookie="divAddProjectToCollectionNotice">×</button>
                    <em>Reminder:</em> Adding projects to a collection does not include the resources within a project.
                </div>
            </div>

            <div id="divSelectedResources">
<#--                <#list resources as resource><input type="hidden" name="resources.id" value="${resource.id?c}" id="hrid${resource.id?c}"></#list> -->
            </div>
        </div>

        <div class="glide">
            <h2>Selected Resources</h2>
    <table class="table table-condensed table-hover" id="tblCollectionResources">
        <colgroup>
            <col style="width:4em">
            <col>
            <col style="width:3em">
        </colgroup>
        <thead>
        <tr>
            <th style="width: 4em">ID
            <th colspan="2">Name
        </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
        </div>


            <@edit.submit fileReminder=false />
        </@s.form>

        <@edit.resourceDataTableJavascript false true />
        <#noescape>
        <script type='text/javascript'>
            $(function () {
                'use strict';
                var form = $("#metadataForm")[0];
                TDAR.common.initEditPage(form);
                TDAR.datatable.registerResourceCollectionDataTable("#resource_datatable", "#tblCollectionResources");
                TDAR.autocomplete.applyCollectionAutocomplete($("#txtParentCollectionName"), {showCreate: false}, {permission: "ADMINISTER_GROUP"});
                TDAR.datatable.registerAddRemoveSection(${(id!-1)?c});
                        //remind users that adding a project does not also add the project's contents
        });
        </script>
        </#noescape>
        <@edit.personAutocompleteTemplate />
    </#compress>
<div style="display:none"
</body>
</#escape>
