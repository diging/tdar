<#escape _untrusted as _untrusted?html>
<#import "/WEB-INF/macros/resource/common.ftl" as common>


<h1>Send a Message</h1>
<h3>Resource: <span class="red">${resource.title} (${resource.id?c})</span></h3>
<br/>
    <@s.form name='metadataForm' id='metadataForm'   cssClass="form-horizontal tdarvalidate" method='post' enctype='multipart/form-data' action='deliver'>
        <@s.token name='struts.csrf.token' />
              <p>Select the type of message you'd like to send to another ${siteAcronym} user.</p>
                 <br/>
                <@s.radio name='type'  emptyOption='false' listValue='label' list='%{emailTypes}' label='Email Type' cssClass="required" />
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
                <@s.hidden name="id" value="${resource.id?c}" />
                <#assign fromId = -1 />
                <#if (authenticatedUser.id)?has_content>
                    <#assign fromId = authenticatedUser.id />
                </#if>
                <@s.hidden name="fromId" value="${authenticatedUser.id?c}" /> 
                <@s.textarea name="messageBody" id="messageBody" rows="4" label="Message" cssClass="span5 required" cols="80" />

                <p><b>Note:</b> Your message will be sent to the owner (designated contact?) of this digital resource in tDAR. 
                Please use "<i>suggest correction</i>" if you would like to recommend changes to the metadata.  
                <#if emailTypes?seq_contains("SAA") >Please use "<i>SAA Abstract project</i>" if you are an author of this resource and would like to upload a copy of your presentation, paper, poster, or supporting data.</#if>  
                <#if emailTypes?seq_contains("REQUEST_ACCESS")>Please use "<i>request access</i>" to request access to confidential or embargoed materials.</#if> 
                
                 Please include sufficient information to fulfill your request (e.g. why you are requesting access to a file, or specific comments or corrections). Your contact information and a link to this resource will automatically be included in your message.</p>
                <@common.antiSpam />
        
    <@s.submit name="submit" cssClass="button btn btn-primary tdar-button" />
</@s.form>
</#escape>