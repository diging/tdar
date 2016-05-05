<#escape _untrusted as _untrusted?html>
<#import "/WEB-INF/macros/resource/edit-macros.ftl" as edit>
<#import "/WEB-INF/macros/resource/navigation-macros.ftl" as nav>

<h3><@s.text name="${keywordType.localeKey}"/><span class="red">:${keyword.label}</span></h3>


<div class="row">
    <div class="span12">
        <@s.form  name='keywordForm' id='keywordForm'   cssClass="form-horizontal" method='post' enctype='multipart/form-data' action='save'>
            <@s.hidden name="id" />
            <@s.hidden name="keywordType" />
            <@s.textfield name="label" value="${keyword.label}" label="Label" cssClass="input-xxlarge"
                labelPosition="left" required=true />
            <@s.textarea name="description" value="${keyword.definition!''}" label="Definition" labelposition="top"
                cssClass="input-xxlarge"  cols="80"  rows="4" />

            <div id="mappingsTable" class="repeatLastRow" addAnother="add another mapping">
                    <#assign maps = keyword.externalMappings>
                    <#if maps?size == 0>
                        <#assign maps = [blankMapping]>
                    </#if>

                    <#list maps>
                        <h4>External Relations</h4>
                        <#items as map>
                            <div id="mapping_row_${map_index}_" class="repeat-row">
                                <div class="control-group">
                                    <label class="control-label">Relation / Type</label>
                                    <div class="controls controls-row">
                                        <div class="span6">
                                           <@s.hidden name="mappings[${map_index}].id" />
                                           <@s.textfield name="mappings[${map_index}].relation"
                                                placeholder="Relation URL e.g. www.example.com?id=12345"
                                                cssClass="url input-xxlarge"/>
                                        </div>
                                        <div class="span2">
                                          <@s.select theme="tdar" name="mappings[${map_index}].relationType"
                                            placeholder="Relation Type"
                                            list="relationTypes" listValue="term" cssClass="input-medium" emptyOption="true"
                                              />
                                        </div>
                                        <div class="span1">
                                           <@nav.clearDeleteButton id="mapping_row" />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </#items>
                    </#list>
            </div>
            <@edit.submit fileReminder=false />
        </@s.form>
    </div>
</div>
</div>
<script>
$(function(){
    TDAR.repeatrow.registerRepeatable(".repeatLastRow");
    TDAR.common.initFormValidation($('#keywordForm'))
});

</script>
</#escape>