/* ASYNC FILE UPLOAD SUPPORT */

TDAR.namespace("fileupload");

TDAR.fileupload = function() {
    'use strict';
    
    var _informationResource;
    var _informationResourceId = -1;
    var _nextRowId = 0;
    
    //main file upload registration function
    var _registerUpload  = function(options) {
        
        //combine options w/ defaults
        var _options = $.extend({formSelector: "#resourceMetadataForm"}, options);
        $(_options.formSelector).data("uploadNames", {});
        
        //pass off our options to fileupload options (to allow easy passthrough of page-specific (options e.g. acceptFileTypes)
        var $fileupload = $(_options.formSelector).fileupload($.extend({
            formData: function(){
                return [{name:"ticketId", value:$('#ticketId').val()}]
            },
            //send files in a single http request (singleFileUploads==false). See TDAR-2763 for more info      
            singleFileUploads: false,

            url: TDAR.uri('upload/upload'),
            autoUpload: true,
            maxNumberOfFiles: TDAR.maxUploadFiles,
            destroy: _destroy,
            //FIXME: don't allow dupe filenames in same upload session until TDAR-2801 fixed.
            submit: function(e, data) {
                var i;
                var errorCount = 0;
                var uploadNames = $(_options.formSelector).data("uploadNames");
                for(i = 0; i < data.files.length; i++) {
                    var file = data.files[i];
                    if(uploadNames[file.name]) {
                        errorCount++;
                        file.error = "Duplicate file name";
                        $(data.context[i]).find('.name').append("<br><span class='label label-important'>" + file.error  + "</span>");                            
                    } else {
                        uploadNames[file.name]=true;
                    }
                }
                return errorCount === 0;
            }
        }, _options));

        var $filesContainer = $fileupload.fileupload('option', 'filesContainer');
        
        $fileupload.bind("fileuploadcompleted", _updateReminder);
        //make sure the sequenceNumber field is correct after files are added (or the operation fails)
        var _updateSequenceNumbers =  function(e, data){
            //console.log("updating sequenceNumbers");
            $('tbody.files').find("tr").not(".replace-target,.deleted-file").each(function(idx, trElem){
                //console.log("updating sequencenumber::   row.id:%s   sequenceNumber:%s", trElem.id, idx+1);
                $('.fileSequenceNumber', trElem).val(idx + 1);
            });
        }
        
        //note: unlike in jquery.bind(), you cant use space-separated event names here.
        $fileupload.bind("fileuploadcompleted fileuploadfailed", _updateSequenceNumbers);
        
        //hack: disable until we have a valid ticket
        //$fileupload.fileupload("disable");
        if (!parseInt($('#ticketId').val())) {
	        $.post(TDAR.uri("upload/grab-ticket"), function(data) {
	            $('#ticketId').val(data.id);
	            //$fileupload.fileupload('enable');
	        }, 'json');
        }
        
        //populate list of peviously uploaded files,  if available.
        if(_options.informationResourceId) {
            _informationResourceId = _options.informationResourceId;
            $.ajax(
                    {
                        url: TDAR.uri("upload/list-resource-files"), 
                        data: {informationResourceId: _options.informationResourceId},
                        success: function(data){
                            //FIXME: if there's an exception in this method, it gets eaten
                            var files = _translateIrFiles(data);
                            console.log("files.length: %s", files.length);
                            // remove all of the pre-loaded proxies ahead of replacing them with their respective proxy versions
                            if (files.length) {
                                $("#fileProxyUploadBody").empty();
                            }
                            //fake out the fileupload widget to render our previously uploaded files by invoking it's 'uploadComplete' callback
                            $fileupload.fileupload('option', 'done').call($fileupload[0], null, {result: {"files":files}});
                            
                            //FIXME: the file.restriction select boxes won't have the correct state,  so as a hack we put the correct value in a data attr and reset it here
                            $filesContainer.find("select.fileProxyConfidential").each(function(){
                                var restriction = $(this).attr("datarestriction");
                                if(restriction) $(this).val(restriction);
                            });
                            
                        },
                        error: function(jqxhr, textStatus, errorThrown) {
                            console.error("textStatus:%s    error:%s", textStatus, errorThrown);
                        },
                        dataType: 'json'
                    });
        }
        
        var helper = {
                //reference back to fileupload widget's container element
                context: $fileupload[0],
                updateFileAction: _updateFileAction,
                inputSelector: _options.inputSelector,
                
                //list of existing and new files that are not deleted or serving as a file replacement
                //FIXME: needs to not include files that were uploaded but failed part way.
                validFiles: function() {
                    var $rows = $filesContainer.find('tr.template-download').not('.replace-target, .deleted-file');
                    
                    var files = $rows.map(function(){
                        var file = {};
                        $(this).find('[type=hidden]').each(function(){
                            file[$(this).attr("class")] = $(this).val();
                        });
                        file.context = $(this);
                        return file;
                    }).get();
                    
                    //translate property names and add extension
                    files = $.map(files, function(file){
                        var ext = file.fileReplaceName.substring(file.fileReplaceName.lastIndexOf(".") + 1).toLowerCase();
                        return {
                            id: parseInt(file.fileId),
                            action: file.fileAction,
                            filename: file.fileReplaceName,
                            sequence: parseInt(file.fileSequenceNumber),
                            ext:  ext,
                            base: file.fileReplaceName.substr(0, file.fileReplaceName.length - ext.length - 1),
                            context: file.context
                        }
                    });
                    return files;
                }
        };
        
        //add reference to helper object  to form and inputFile
        $(_options.formSelector).add(_options.inputSelector).data('fileuploadHelper', helper);

        _registerReplaceButton(_options.formSelector);

        return helper;
    };
    
    
    



    
    //update file proxy actionto indicate that the values have changed 
    var _updateFileAction = function(elemInRow) {
        console.log("updateFileAction(%s)", elemInRow);
        var $hdnAction = $(elemInRow).closest("tr").find(".fileAction");
        if($hdnAction.val()==="NONE") {
            $hdnAction.val("MODIFY_METADATA");
        }
    }

    //convert json returned from tDAR into something understood by upload-plugin, as well as fileproxy fields to send back to server
    var _translateIrFiles = function(fileProxies) {
        return $.map(fileProxies, function(proxy, i) {
            return $.extend({
                name: proxy.filename,
                url: TDAR.uri("filestore/" + proxy.originalFileVersionId),
                thumbnail_url: null, 
                delete_url: null,
                delete_type: "DELETE" 
            }, proxy);
        });
    };

    //mark a file as deleted/undeleted and properly set appearance and fileproxy state
    var _destroy = function(e, data){
        // data.context: download row,
        // data.url: deletion url,
        // data.type: deletion request type, e.g. "DELETE",
        // data.dataType: deletion response type, e.g. "json"
        
        var $row = data.context;
        //If pre-existing, tell server to delete the file.  If we just sent it, tell server to ignore it. 
        var newUpload = parseInt($row.find(".fileId").val()) <= 0;
        var $btnDelete = $("button.delete-button", data.context);
        var $hdnAction = $(".fileAction", data.context);
        if($btnDelete.data("type") === "DELETE") {
            $hdnAction.data("prev", $hdnAction.val());
            $hdnAction.val(newUpload ? "NONE" : "DELETE");
            $(data.context).addClass("deleted-file");
            $btnDelete.data("type", "UNDELETE");
            //TODO: make row look "deleted", disable everything except 'undelete' button
            _disableRow($row);
        } else {
            //re-enable row appearance and change label back to previous state
            $hdnAction.val($hdnAction.data("prev"));
            $(data.context).removeClass("deleted-file");
            $btnDelete.data("type", "DELETE");
            _enableRow($row);
        }
        //show the correct button label
        $("span", $btnDelete).html({
            "DELETE": "Delete",
            "UNDELETE": "Undelete"
        }[$btnDelete.data("type")] );
               
        
        console.log("destroy called. context:%s", data.context[0]);
    };

    //TODO: replace this with a custom event
    var _updateReminder = function(e, data) {
        console.log("_updateReminder")
        var $filesTable = $(data.context).closest("tbody.files");
        console.log($filesTable.length);
        if ($filesTable.length > 0) {
        	$("#reminder").hide();
        } else {
        	$("#reminder").show();
        }
    };

    //dynamically generate the replace-with dropdown list items with the the candidate replacement files
    var _buildReplaceDropdown = function(e) {
        var button = this;
        var $ul = $(button).next(); //in a button dropdown, the ul follows the button
        var $tr = $(button).parents("tr.existing-file");
        var $tbody = $(button).closest("tbody.files");
        var $newfiles = $('.new-file:not(.replace-target,.deleted-file)');
        var data = {
                jqnewfiles: $newfiles,
                //TODO: figure  out if this existing file has already chosen a replacement, if so,  render a "cancel" option.
                bReplacePending: $tr.hasClass('replacement-selected')};
        
        var $listItems = $(tmpl("template-replace-menu", data));
        $listItems.find('a').bind("click", _replacementFileItemClicked);
        $ul.empty();
        $ul.append($listItems);
    };

    //"replacement file chosen" event handler.  update the hidden replacement filename field of the replacement file row, and mark target as selected
    var _replacementFileItemClicked = function(e) {
        //FIXME: I don't think preventDefault should be necessary, but browser follows href ("#", e.g. scrolls to top of page) unless I do.
        e.preventDefault();  
        
        var $anchor = $(this);
        var $tr = $anchor.parents(".existing-file");
        var $hidden = $tr.find('.fileReplaceName');
        var $target =  $($anchor.data("target"));
        if(!$anchor.hasClass("cancel")) {
            if($target.data('jqOriginalRow')) {
                //if this replace operation overwrites a pending replace, cancel the pending replace first.
                _cancelFileReplace($target.data('jqOriginalRow'), $target);
            }
            _replaceFile($tr, $target);
        } else {
            //the 'cancel' link doesn't have a data-target attr; but we did add a reference to the target in the original
            _cancelFileReplace($tr, $tr.data("jqTargetRow"));
        }
    };
    
    var _replaceFile = function($originalRow, $targetRow) {
        var targetFilename = $targetRow.find("input.fileReplaceName").val();
        var originalFilename = $originalRow.find("input.fileReplaceName").val();
        
        
        $originalRow.find('.replacement-text').text("will be replaced by " + targetFilename + ")");
        $originalRow.find('.fileReplaceName').val(targetFilename);
        $originalRow.find('.fileReplaceName').data("original-filename", originalFilename)
        
        //effectively 'remove' the target file proxy fields from the form by removing the name attribute.
        $targetRow.find("input,select").each(function(){
            var $hidden = $(this);
            $hidden.data("original-name", $hidden.attr("name"));
            $hidden.removeAttr("name");
        });
        //have original row point to target,  in the event we need to cancel later and set everything back to normal
        $originalRow.data("jqTargetRow", $targetRow).addClass("replacement-selected");

        //implicitly cancel a pending replace if user initiates a another replace operate on the same targetRow
        $targetRow.data("jqOriginalRow", $originalRow);
        
        //Change action to "REPLACE", and store original in event of cancel 
        var $fileAction = $originalRow.find('.fileAction');
        $fileAction.data('original-fileAction', $fileAction.val());
        $fileAction.val("REPLACE");
        
        $targetRow.find('.replacement-text').text("(replacing " + originalFilename + ")");
        $targetRow.find('input, select, button.delete-button').prop("disabled", true);;
        $targetRow.addClass('replace-target');
        $targetRow.trigger("fileuploadreplaced", [$originalRow, $targetRow]);
    }
    
    //TODO: pull out redundant sections before adam has a chance to put this in a ticket for me.
    var _cancelFileReplace = function($originalRow, $targetRow) {
        $targetRow.find('.replacement-text').text("");
        $targetRow.find('input, select').prop("disabled", false).each(function() {
            $(this).attr("name", $(this).data("original-name"));
        });
        $targetRow.find('button.delete-button').prop("disabled", false);
        
        $originalRow.find('.replacement-text').text('');
        $originalRow.removeClass("replacement-selected");
        
        var $fileAction = $originalRow.find('.fileAction');
        $fileAction.val($fileAction.data('original-fileAction'));
        
        var $filename = $originalRow.find('.fileReplaceName');
        $filename.val($filename.data('original-filename'));
        $.removeData($originalRow[0], "jqTargetRow");
        $targetRow.removeClass('replace-target');
    }
    
    
    var _enableRow = function(row) {
        $('button:not(.delete-button), select', row).prop('disabled', false);
        $('.delete-button', row).removeClass('btn-warning').addClass('btn-danger');
    };
    
    var _disableRow = function(row) {
        $('button:not(.delete-button), select', row).prop('disabled', true);
        $('.delete-button', row).addClass('btn-warning').removeClass('btn-danger');
    };
    
    //public: kludge for dealing w/ fileupload's template system, which doesn't have a good way to pass the row number of the table the template will be rendered to
    var _getRowId = function() {
        return _nextRowId++;
    }

    var _registerReplaceButton = function(formSelector) {
        console.log("registering replace button")

        //invoke the fileupload widgets "add" method
        $(formSelector).on('change', '.fileupload-replace' , function (e) {
            console.log("triggering file upload");
            $(formSelector).fileupload('add', {
                files: e.target.files || [{name: this.value}],
                fileInput: $(this),
                $replaceTarget: $(this).closest(".existing-file")
            });
        });

        //when the upload is complete&succesful, update file proxy fields to indicate incoming file is replacement
        $(formSelector).bind("fileuploadcompleted", function(e, data) {
            if(!data.$replaceTarget) return;
            var file = data.files[0];
            var $originalRow = data.$replaceTarget;
            $originalRow.find(".fileReplaceName").val(file.name);
            $originalRow.find(".fileAction").val("REPLACE");
            $originalRow.find("td span.replacement-text").html(
                    "<br><em>Will be replaced by:</em><br>"+
                    file.name
            );

            //remove the new row
            //TODO: the latest version of this fileupload allows deleting files with null data.url, use that instead
            //FIXME: need to account for maxNumberOfFiles
            $(data.context).remove();
        });

    }
    
    //expose public elements
    return {
        "registerUpload": _registerUpload,
        //FIXME: we can remove the need for this if we include it as closure to instanced version of _destroy.
        "updateFileAction": _updateFileAction,
        "getRowId": _getRowId
        
    };
}();