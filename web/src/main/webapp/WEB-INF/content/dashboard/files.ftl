<#escape _untrusted as _untrusted?html>
<#import "/WEB-INF/macros/resource/list-macros.ftl" as rlist>
<#import "common-dashboard.ftl" as dash />
<#import "/WEB-INF/macros/resource/edit-macros.ftl" as edit>
<#import "/WEB-INF/macros/resource/view-macros.ftl" as view>
<#import "/WEB-INF/macros/search-macros.ftl" as search>
<#import "/WEB-INF/macros/resource/common-resource.ftl" as commonr>
<#import "/WEB-INF/macros/common.ftl" as common>
<#import "/${config.themeDir}/settings.ftl" as settings>

<head>
    <title>${authenticatedUser.properName}'s Dashboard</title>
    <meta name="lastModifiedDate" content="$Date$"/>

</head>


<div id="titlebar" parse="true">
    <h1>Dashboard &raquo; <span class="red">My Bookmarks</span></h1>

</div>
<div class="row">
<div class="span2">
    <@dash.sidebar current="bookmarks" />
</div>
<div class="span10">

<form class="form-horizontal">
<div class="control-group">
  <label class="control-label" for="inputInfo">Select Billing Account</label>
  <div class="controls">
    <select name="account" id="account">
        <option>Veterans Curation Program</option>
    </select>
  </div>
</div>

<table class="table">
<thead>
 <tr>
    <th>id</th>
    <th>file</th>
    <th>status</th>
    <th>action</th>
 </tr>
</thead>
</table>



    </div>
</div>

</div>


</div>



<script>
    $(document).ready(function () {
        TDAR.notifications.init();
    });
</script>



</#escape>