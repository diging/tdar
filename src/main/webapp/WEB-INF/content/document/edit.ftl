<#escape _untrusted as _untrusted?html>
<#import "/WEB-INF/macros/resource/edit-macros.ftl" as edit>

<head>
<@edit.title />

<meta name="lastModifiedDate" content="$Date$"/>

</head>
<body>
<div id='subnavbar' class="affix-top navbar navbar-static"  data-offset-top="250" data-offset-bottom="250" data-spy="affix">
  <div class="navbar-inner">
    <div class="container" style="width: auto;">
	<ul class="nav">
		<li><a href="#basicInformationSection">Basic</a></li>
		<li><a href="#authorshipSection">Authors</a></li>
		<li><a href="#divFileUpload">Upload</a></li>
		<li><a href="#organizeSection">Project</a></li>
		<li><a href="#spatialSection">Where</a></li>
		<li><a href="#temporalSection">When</a></li>
		<li><a href="#investigationSection">What</a></li>
		<li><a href="#siteSection">Site</a></li>
		<li><a href="#resourceNoteSectionGlide">Notes</a></li>
		<li><a href="#divAccessRights">Permissions</a></li>
	</ul>
	<span class="brand">
	
	      <#-- @edit.submitButton "save" / -->
	</span>
	</div>
  </div>
</div>
<div>
<@edit.toolbar "${resource.urlNamespace}" "edit" />
<@s.form id='resourceMetadataForm' method='post' enctype='multipart/form-data' action='save'  cssClass="form-horizontal">

<@edit.basicInformation 'document' 'document' >
    <#if linkedInformationResource??>
    <div class='help-block'>
    This will be linked as a <b>${linkType}</b> citation for: <b>${linkedInformationResource.title}</b>
    <@s.hidden name='linkType' value='${linkType}'/>
    <@s.hidden name='linkedResourceId' value='${linkedInformationResource.id?c}'/>
    </div>
    </#if>

    <span tiplabel="Document Type"  tooltipcontent="Select the document type. Appropriate citation fields will be displayed below."></span>
    <@s.radio name='document.documentType' id="documentType" emptyOption='false' listValue="label"  
        list='%{documentTypes}' label="Document Type" theme="bootstrap" />
            
    
    <span tiplabel="Additional Title" tooltipcontent="Enter the title of the book, report, or journal this document is part of"></span>
    <div class="journal_article ">
            <@s.textfield label="Journal Title" id='journalName' 
            title="A journal title is required"
            name='document.journalName' cssClass="requiredIfVisible tdartext longfield input-xxlarge" />
    </div>
    
    <span tooltipcontent="Enter the title of the book, report, or journal this document is part of." tiplabel="Additional Title"></span>
    
    <div class="book_section">
        <@s.textfield label="Book Title" id='bookTitle' title="A book title is required" name='document.bookTitle' cssClass="requiredIfVisible tdartext input-xxlarge" />
    </div>


</@edit.basicInformation>

<@edit.allCreators "Authors / Editors" authorshipProxies 'authorship' false />
<@edit.citationInfo "document">


    <span tiplabel="Language" tooltipcontent="Select the language in which the document is written."></span>
    <@s.select label='Language'  emptyOption='false' name='resourceLanguage'  listValue="label" list='%{languages}' cssClass="right-shortfield "/>

    <div class="journal_article other">
        <@s.textfield id='volume' label='Volume' name='document.volume' cssClass="shortfield"  />
        <@s.textfield id='journalNumber' label='Issue Number' name='document.journalNumber' cssClass="right-shortfield"  />
    </div>
    
    <div class='book_section book other'>
        <@s.textfield id='seriesName' label='Series Title' name='document.seriesName' cssClass="" />
        <@s.textfield id='seriesNumber' label='Series #' name='document.seriesNumber' cssClass="span1" />
        <@s.textfield id='edition' label='Edition' name='document.edition' cssClass="span1" />
    </div>
    
    <div class="book_section journal_article other">
        <div class="control-group">
            <label class="control-label">Start/End Page</label>
            <div class="controls controls-row">
                <@s.textfield theme="tdar" id='startPage' placeholder="Start#" name='document.startPage' cssClass="span1" />
                <@s.textfield theme="tdar" id='endPage'  placeholder="End#" name='document.endPage' cssClass="span1" />
            </div>
        </div>    
    </div>

    <div class="thesis">
        <@s.radio name='document.degree' label="Degree" id="degreeType" emptyOption='false' listValue="label"  list='%{degrees}' />
    </div> 
          
    <div tooltipcontent="Actual physical location of a copy of the document, e.g. an agency, repository, or library." tiplabel="Copy Location"></div>
        <@s.textfield id='copyLocation' label='Copy Location' name='document.copyLocation' cssClass=""/>

    <span id="t-doi" tiplabel="DOI" tooltipcontent="Digital Object Identifier."></span>
    <@s.textfield labelposition='left' id='doi' label='DOI' name='document.doi' cssClass="shortfield" />
    
    <span id="t-isbn" tiplabel="ISBN" tooltipcontent="International Standard Book Number."></span>
    <@s.textfield labelposition='left' id='isbn' title="please add a valid ISBN" label='ISBN' name='document.isbn' cssClass="isbn book_section book other" />
    
    <span id="t-issn" tiplabel="ISSN" tooltipcontent="International Standard Serial Number, an eight-digit number assigned to many serial publications."></span>
    <@s.textfield labelposition='left' id='issn' title="please add a valid ISSN" label='ISSN' name='document.issn' cssClass="issn journal_article" />


</@edit.citationInfo>

<@edit.asyncFileUpload "Attach Document Files" true />

<@edit.sharedFormComponents />
</@s.form>

</div>

<@edit.asyncUploadTemplates />
<@edit.resourceJavascript includeAsync=true />
</body>
</#escape>
