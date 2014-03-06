<head>
<title>Error Occurred</title>
<#import "/WEB-INF/macros/resource/common.ftl" as common><#t>
</head>

            <ul class="inline-menu hidden-desktop"><@common.loginMenu false/></ul>
<p>
</p>
<h2>An error within our system has occurred.</h2>
<p>
An unhandled error occurred and has been logged.  Please check our <a
href='${bugReportUrl}'>issue tracker</a> to see if it has already been
reported - if not, please enter the error messages listed below into our <a
href='${bugReportUrl}'>issue tracker</a> along with any relevant details of
what you were doing at the time of the error.  Thanks, and apologies for the
inconvenience.  We'll try to fix this as soon as possible.
</p>
<p>
If you have other concerns or questions or would like additional information, we are
currently working on tutorials and documentation on our <a
href='http://dev.tdar.org/confluence'>wiki</a>.  We also maintain a frequently
monitored <a href='http://lists.asu.edu/cgi-bin/wa?A0=TDAR-DEV'>mailing list</a>
where you can ask questions pertaining to the development or usage of the tDAR 
software platform.
</p>
<br />
<a href="#" id="pToggleStackTrace" onclick="$('#stackTrace').toggleClass('hidden');return false;">Show/Hide Detailed Error Information</a> (please copy and paste this when filing a bug report)
<div id="stackTrace" class='error <#if production!true>hidden</#if>' style='text-align:left'>
    <h2>Exception stack trace </h2>
    <pre>
    <@s.property value='%{exceptionStack}'/>
    </pre>
</div>


