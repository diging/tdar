Dear ${requestor.properName},
  Your request for access to ${resource.title} (${resource.id?c}) has been decliend by ${authorizedUser.properName}

<#if message?has_content>
${message}
</#if>
