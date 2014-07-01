<#-- common authentication/authorization macros and functions -->
<#escape _untrusted as _untrusted?html>
<#import "/WEB-INF/macros/resource/common.ftl" as common>

<#--
 registrationForm:  render a user registration form
     verbosity:string  relative amount of detail to capture (minimal|extended|verbose)
     columns: maximum width consumed by this section, assuming 12-column grid layout
-->
<#macro registrationFormFields detail="verbose" cols=12 beanPrefix="reg" showSubmit=true>
    <@common.antiSpam />
<#local
    level = ({'verbose': 3, 'extended': 2, 'minimal': 1}[detail])!3
    showMinimal = true,
    showExtended = (2 <= level),
    showVerbose = (3 <= level)
    spanfull = "span${cols}"
    spanhalf = "span${cols/2}"
>

<div class="row">
    <div class="span8">
        <@s.textfield spellcheck="false" required=true id='username' label="Username" name="${beanPrefix}.person.username" cssClass="required username input-xlarge" />
    </div>
</div>

<div class="row">
    <div class="span4">
        <@s.textfield spellcheck="false" required=true id='firstName' label='First name'  name='${beanPrefix}.person.firstName' cssClass="required input-xlarge" />
    </div>
    <div class="span4">
        <@s.textfield spellcheck="false" required=true id='lastName' label='Last name' name='${beanPrefix}.person.lastName' cssClass="required input-xlarge" />
    </div>
</div>

<div class="row">
    <div class="span4">
        <@s.textfield spellcheck="false" required=true id='emailAddress' label="Email address" name="${beanPrefix}.person.email" cssClass="required email input-xlarge" />
    </div>
    <div class="span4">
        <@s.textfield spellcheck="false" required=true id='confirmEmail' label="Confirm email" name="${beanPrefix}.confirmEmail" cssClass="required email input-xlarge"/>
    </div>
</div>

<div class="row">
    <div class="span4">
        <@s.textfield labelposition='left' label='Organization' name='${beanPrefix}.institutionName' id='institutionName' cssClass="input-xlarge"/>
    </div>
    <div class="span4">
    <#-- listValueKey="localeKey"	       theme="tdar" -->
            <@s.select list="${beanPrefix}.affiliations" name="${beanPrefix}.affiliation" label="Affiliation / Interest" listValue="label" headerKey=""
    headerValue="Select Affiliation"   />
</div>

</div>
<div class="row">
    <div class="span4">
        <@s.password required=true label='Password' name='${beanPrefix}.password' id='password'  cssClass="required input-xlarge" autocomplete="off" />
    </div>
    <div class="span4">
        <@s.password required=true label='Confirm password' name='${beanPrefix}.confirmPassword' id='confirmPassword'  cssClass="required input-xlarge" autocomplete="off" />
    </div>
</div>

<#if (level > 1)>
<div class="row">
    <div class="span4">
        <@s.textfield label='Work phone' labelposition='left' name='${beanPrefix}.person.phone' id='phone' cssClass=" input-xlarge"/>
    </div>
</div>
</#if>

<div class="row">
    <div class="${spanfull}">
        <#if showMinimal>
            <label class="checkbox">
                <@s.checkbox theme="simple" name="${beanPrefix}.acceptTermsOfUse" id="tou-id"  />
                I have read and accept the ${siteAcronym}
                <@s.a href="tosUrl" target="_blank" title="click to open contributor agreement in another window">User Agreement</@s.a> and
                <@s.a href="contributorAgreementUrl" target="_blank" title="click to open contributor agreement in another window">Contributor Agreement</@s.a>
            </label>

        <#else>
            <div class="control-group">
                <label class="control-label">Terms of Use</label>
                <div class="controls">
                    <span class="help-block">  </span>
                    <label class="checkbox">
                        <@s.checkbox theme="simple" name="${beanPrefix}.acceptTermsOfUse" id="tou-id"  />
                        I have read and accept the ${siteAcronym}
                        <@s.a href="tosUrl" target="_blank" title="click to open contributor agreement in another window">User Agreement</@s.a>.
                    </label>
                </div>
            </div>
            <div class="control-group">
                <label class="control-label">Contributor Agreement</label>
                <div class="controls">
                <span class="help-block">Check this box if you will be contributing resources and/or resource metadata to ${siteAcronym}. You may change this setting at any time.
                </span>
                    <label class="checkbox">
                        <@s.checkbox theme="simple" name="${beanPrefix}.requestingContributorAccess" id="contributor-id"  />
                        I accept the ${siteAcronym}
                        <@s.a href="contributorAgreementUrl" target="_blank" title="click to open contributor agreement in another window">Contributor Agreement</@s.a>
                        and wish to add ${siteAcronym} content.
                    </label>
                </div>
            </div>
            <#if (level > 1)>
            <div id='contributorReasonTextArea'>
                <label class="control-label">Contributor information</label>
                <div class="control-group">
                    <div class="controls">
                    <span class="help-block">
                        Please briefly describe the geographical areas, time periods, or other subjects for which you
                        would like to contribute information
                    </span>
                        <@s.textarea theme="simple" rows=6 cssClass="input-xxlarge" name='${beanPrefix}.contributorReason' id='contributorReasonId' />
                    </div>
                </div>
            </div>
            </#if>
        </#if>

        <#if showSubmit>
        <div class="form-actions">
            <input type="submit" class='btn btn-primary  submitButton' name="submitAction" value="Register">
        </div>
        </#if>
    </div>
</div>
</#macro>


<#macro login showLegend=false beanPrefix="userLogin">
    <#if showLegend>
        <legend>Login</legend>
    </#if>
<#-- don't think this does anything
    <input type="hidden" name="url" value="${Parameters.url!''}"/> -->
    <@s.token name='struts.csrf.token' />
    <@common.antiSpam />
    <@s.textfield spellcheck="false" id='loginUsername' name="${beanPrefix}.loginUsername" label="Username" cssClass="required" autofocus="autofocus"/>
    <@s.password id='loginPassword' name="${beanPrefix}.loginPassword" label="Password" cssClass="required" />

    <@s.hidden name="returnUrl" />
    <#nested />
    <script type="text/javascript">
        $(document).ready(function () {
            TDAR.auth.initLogin();
        });
    </script>
</#macro>
</#escape>