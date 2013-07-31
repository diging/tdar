<#include "/${parameters.templateDir}/simple/controlheader.ftl" />
<#assign hasFieldErrors = parameters.name?? && fieldErrors?? && fieldErrors[parameters.name]??/>
<#if parameters.label?exists || parameters.groupLabel?exists >
    <label <#t/>
<#if parameters.id?exists>
        for="${parameters.id?html}" <#t/>
</#if>
class="<#t/>
<#if hasFieldErrors>errorLabel </#if><#t/>
<#if parameters.labelposition?exists && parameters.labelposition.equalsIgnoreCase('top')>toplabel</#if>"><#t/>
<#if parameters.required?default(false) && parameters.requiredposition?default("right") != 'right'>
        <span class="required">*</span><#t/>
</#if>
<#if parameters.label?exists>
 ${parameters.label?html}<#t/>
</#if>
<#if parameters.groupLabel?exists>
${parameters.groupLabel?html}<#t/>
</#if>
<#if parameters.required?default(false) && parameters.requiredposition?default("right") == 'right'>
 <span class="required">*</span><#t/>
</#if>
${parameters.labelseparator?default("")?html}<#t/>
<#include "/${parameters.templateDir}/xhtml/tooltip.ftl" /> 
</label><#t/>
</#if>
<#if parameters.labelposition?exists && parameters.labelposition.equalsIgnoreCase('top')>
  <br/>
</#if>
<#lt/>
