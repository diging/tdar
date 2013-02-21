<#escape _untrusted as _untrusted?html>

<#macro accountInfoForm>
    <@s.hidden name="invoiceId" />    
    
	 <#if billingManager>
   	<#if invoice?? && invoice.proxy>
   		<div class="alert-info info">
   		creating proxy account for ${invoice.owner.properName}
   		</div>
   	</#if>
    </#if>
    <h3>Who can charge to this account </h3>
    <@edit.listMemberUsers />
    
    <@edit.submit fileReminder=false />

</#macro>

</#escape>