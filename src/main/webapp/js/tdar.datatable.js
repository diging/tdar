TDAR.namespace("datatable");
TDAR.datatable = function() {
    "use strict";

    /**
     * Register a new dtatable control. By default, the datatable populates with resources editable by the user. Most of the initialization options required by
     * the $.dataTable are handled by this function. However, you can override these functions by via the options argument in the constructor
     * 
     * @param parms
     *            options object - properties in the object will override the defaults used to create the datatable. see the DataTables.net documentation for
     *            full list of option. Tdar-specific properties include: tableSelector: jquery selector which contains the table to initialize/convert into a
     *            datatable requestCallback: callback executed prior to performing ajax request. if it returns an object, those property name/value pairs are
     *            included in the ajax request, selectableRows: boolean indicating whether the rows of the data table are selectable, rowSelectionCallback:
     *            callback executed whenever user selects a row , "sAjaxSource": '/lookup/resource': url to request rowdata
     * 
     * @returns {*|jQuery|HTMLElement} reference to the datatable widget created by this element.
     */
    function _registerLookupDataTable(parms) {
        _extendSorting();
        // tableSelector, sAjaxSource, sAjaxDataProp, aoColumns, requestCallback, selectableRows
        var doNothingCallback = function() {
        };
        var options = {
            tableSelector : '#dataTable',
            requestCallback : doNothingCallback,
            selectableRows : false,
            rowSelectionCallback : doNothingCallback,
            "sAjaxSource" : '/lookup/resource',
            "sAjaxDataProp" : 'resources',
            "bJQueryUI" : false,
            "sScrollY" : "350px",
            "sScrollX" : "100%",
            fnDrawCallback : function() {
                // if all checkboxes are checked, the 'select all' box should also be checked, and unchecked in all other situations
                if ($(":checkbox:not(:checked)", $dataTable).length == 0) {
                    $('#cbCheckAllToggle').prop('checked', true);
                } else {
                    $('#cbCheckAllToggle').prop('checked', false);
                }
            }
        };

        $.extend(options, parms);
        var $dataTable = $(options.tableSelector);
        $dataTable.data("toAdd", []);
        $dataTable.data("toRemove", []);

        // here is where we will store the selected rows (if caller wants to track that stuff)
        $dataTable.data('selectedRows', {});

        var dataTableOptions = {
            "bScrollCollapse" : true,
            "bProcessing" : true,
            "bServerSide" : true,
            // "sAjaxDataProp": sAjaxDataProp,
            // "aoColumns": aoColumns

            // intercept the server request, and translate the parameters to server
            // format. similarly, take the json returned by the jserver
            // and translate to format expected by the client.
            "fnServerData" : function _fnServerData(sSource, aoData, fnCallback) {

                $.ajax({
                    traditional : true, // please don't convert my arrays to php arrays. php is dumb.
                    dataType : 'jsonp',
                    url : sSource,
                    xhrFields : {
                        withCredentials : true
                    },
                    data : _convertRequest(aoData, options.aoColumns, options.requestCallback, $dataTable ),
                    success : function(_data) {
                        var addIds = $dataTable.data("toAdd");
                        var removeIds = $dataTable.data("toRemove");
                        //assuming the above lists are mutually exclusive, the concatenation is a list of all checkboxes that should be toggled
                        var toggleIds = addIds.concat(removeIds);

                        // intercept data returned by server, translate to client format
                        var recordInfo = {
                            iTotalDisplayRecords : _data.totalRecords,
                            iTotalRecords : _data.totalRecords
                        };
                        if (typeof _data.totalRecords === "undefined") {
                            recordInfo = {
                                iTotalDisplayRecords : _data.status.totalRecords,
                                iTotalRecords : _data.status.totalRecords
                            };
                        }
                        $.extend(_data, recordInfo);
                        //update the list of resource id's that belong to the current resource collection
                        $dataTable.data("selectedResults", _data.selectedResults);

                        //similarly, add  isSelectedResult property to each result
                        if(options.selectableRows) {
                            if(!_data.selectedResults) {
                                _data.selectedResults = [];
                            }
                            $.each(_data.resources, function(idx, obj) {
                                obj.isSelectedResult = _data.selectedResults.indexOf(obj.id) > -1;
                                obj.isToggled = toggleIds.indexOf(obj.id) > -1;

                                //determining the current selected status is tricky.  We need to reconcile the value from the server setting (isSelectedResult)
                                //against any client-side changes made so far.
                                obj.isCurrentlySelected = (obj.isSelectedResult !== obj.isToggled);
                            });
                        }
                        fnCallback(_data);
                    },
                    error : function(jqXHR, textStatus, errorThrown) {
                        console.error("ajax query failed:" + errorThrown);
                    }
                });
            }
        };

        // if user wants selectable rows, render checkbox in the first column (which we assume is an ID field)
        if (options.selectableRows) {
            options.aoColumns[0].fnRender = fnRenderIdColumn;
            options.aoColumns[0].bUseRendered = false;
            dataTableOptions["fnRowCallback"] = function(nRow, obj, iDisplayIndex, iDisplayIndexFull) {
                // determine whether the user selected this item already (if so check the box)
                var $cb = $(nRow).find('input[type=checkbox]');
                var id = $cb.val();
                $cb.prop('checked', obj.isCurrentlySelected );
                return nRow;
            };

            // register datatable checkbox changes. maintain a hashtable of all of the currently selected items.
            // call the rowSelectionCallback whenever something changes
            $dataTable.on('change', 'input[type=checkbox]' , function() {
                var $elem = $(this); // here 'this' is checkbox
                var id = $elem.val();
                var objRowData = $dataTable.fnGetData($elem.parents('tr')[0]);
                if ($elem.prop('checked')) {
                    // get the json data associated w/ the selected row, put it in selectedRows
                    $dataTable.data('selectedRows')[id] = objRowData;
                    options.rowSelectionCallback(id, objRowData, true);
                } else {
                    delete $dataTable.data('selectedRows')[id]; // unchecked, so remove from the hashtable
                    options.rowSelectionCallback(id, objRowData, false);
                }

            });
        }

        // put any user-specified dataTable options that have been specified in the parms into the dataTableOptions
        $.extend(options, dataTableOptions);

        $dataTable.dataTable(options);
        _scrollOnPagination();
        return $dataTable;
    }

    /**
     * Prepare request data to be sent to tdar lookup request. This function will derive the startpage, recordsPerPage, and sortField any additional data to be
     * sent to server should be returned by requestCallback(sSearch) where sSearch is the search term entered in the datatable search box (if any).
     * 
     * @param aoData
     *            array of objects with "name" and "value" properties.
     * @param aoColumns
     * @param requestCallback
     * @param $dataTable jquery selection containing the datatable
     * @returns {{startRecord: (j.defaults.iDisplayStart|*|int), recordsPerPage: (j.defaults.iDisplayLength|*|int), sortField:
     *          (string|g_settingsMap.person.tdarSortOption|g_settingsMap.institution.tdarSortOption|g_settingsMap.keyword.tdarSortOption|tdarSortOption)}}
     * @private
     */
    function _convertRequest(aoData, aoColumns, requestCallback, $dataTable) {
        var oData = {};
        // first convert the request from array of key/val pairs to map<string,string>.
        $.each(aoData, function() {
            oData[this.name] = this.value;
        });

        // derive sort column from the field name and reversed status
        var tdarSortOption = aoColumns[oData["iSortCol_0"]].tdarSortOption;
        var sSortReversed = {
            desc : 'true'
        }[oData["sSortDir_0"]];
        if (sSortReversed) {
            tdarSortOption += '_REVERSE';
        }
        var translatedData = {
            startRecord : oData.iDisplayStart,
            recordsPerPage : oData.iDisplayLength,
            sortField : tdarSortOption
        };

        $.extend(translatedData, requestCallback(oData.sSearch));

        return translatedData;
    }

    /**
     * callback that renders the "id" column of the datatable.
     * 
     * @param oObj
     * @returns {string}
     */
    function fnRenderIdColumn(oObj) {
        // in spite of the name, aData is an object corresponding to the current row
        var id = oObj.aData.id;
        var attrId = "cbEntityId_" + id;
        var resourceType = oObj.aData.resourceType;
        // not all things are resourceTypes that are rendered like this
        if (resourceType) {
            resourceType = resourceType.toLowerCase();
        }
        // console.log("resource type:%s", resourceType);
        return ('<label class="datatable-cell-unstyled">' +
                '<input type="checkbox" class="datatable-checkbox ' + resourceType + '" id="' + attrId + '" value="' + id + '" >' + id
                + '</label>');
    }

    /**
     * datatable cell render callback: this callback specifically renders a resource title.
     * 
     * @param oObj
     *            row object
     * @returns {string} html to place insert into the cell
     */
    function fnRenderTitle(oObj) {
        // in spite of name, aData is an object containing the resource record for this row
        var objResource = oObj.aData;
        var html = '<a href="' + TDAR.uri(objResource.urlNamespace + '/' + objResource.id) + '" class=\'title\'>' + TDAR.common.htmlEncode(objResource.title) +
                '</a>';
        html += ' (ID: ' + objResource.id
        if (objResource.status != 'ACTIVE') {
            html += " " + objResource.status;
        }
        html += ')';
        return html;
    }

    /**
     * datatable cell render callback: this callback emits the title and decription.
     * 
     * @param oObj
     *            row object
     * @returns {string} html to place insert into the cell
     */
    function fnRenderTitleAndDescription(oObj) {
        var objResource = oObj.aData;
        return fnRenderTitle(oObj) + '<br /> <p>' + TDAR.common.htmlEncode(TDAR.common.elipsify(objResource.description, 80)) + '</p>';
    }

    /**
     * initialize the datatable used for the dashboard page, as well as the datatable search controls.
     * 
     * @param options
     * @private
     */
    function _setupDashboardDataTable(options) {
        var _options = $.extend({}, options);
        _extendSorting();

        // set the project selector to the last project viewed from this page
        // if not found, then select the first item
        var prevSelected = $.cookie("tdar_datatable_selected_project");
        if (prevSelected != null) {
            var elem = $('#project-selector option[value=' + prevSelected + ']');
            if (elem.length) {
                elem.attr("selected", "selected");
            } else {
                $("#project-selector").find("option :first").attr("selected", "selected");
            }

        }
        var prevSelected = $.cookie("tdar_datatable_selected_collection");
        if (prevSelected != null) {
            var elem = $('#collection-selector option[value=' + prevSelected + ']');
            if (elem.length) {
                elem.attr("selected", "selected");
            } else {
                $("#collection-selector").find("option :first").attr("selected", "selected");
            }

        }

        jQuery.fn.dataTableExt.oPagination.iFullNumbersShowPages = 3;
        $.extend($.fn.dataTableExt.oStdClasses, {
            "sWrapper" : "dataTables_wrapper form-inline"
        });
        // sDom:'<"datatabletop"ilrp>t<>', //omit the search box

        var _fnRenderTitle = _options.showDescription ? fnRenderTitleAndDescription : fnRenderTitle;

        var aoColumns_ = [ {
            "mDataProp" : "title",
            fnRender : _fnRenderTitle,
            bUseRendered : false,
            "bSortable" : false
        }, {
            "mDataProp" : "resourceTypeLabel",
            "bSortable" : false
        } ];
        // make id the first column when datatable is selectable
        if (_options.isSelectable) {
            aoColumns_.unshift({
                "mDataProp" : "id",
                tdarSortOption : "ID",
                sWidth : '5em',
                "bSortable" : false
            });
        }
        var $dataTable = $('#resource_datatable');
        _registerLookupDataTable({
            tableSelector : '#resource_datatable',
            sAjaxSource : '/lookup/resource',
            "bLengthChange" : true,
            "bFilter" : false,
            aoColumns : aoColumns_,
            // "sDom": "<'row'<'span9'l><'span6'f>r>t<'row'<'span4'i><'span5'p>>",
            "sDom" : "<'row'<'span6'l><'pull-right span3'r>>t<'row'<'span4'i><'span5'p>>", // no text filter!
            sAjaxDataProp : 'resources',
            requestCallback : function(searchBoxContents) {
                return {
                    title : searchBoxContents,
                    'resourceTypes' : $("#resourceTypes").val() == undefined ? "" : $("#resourceTypes").val(),
                    'includedStatuses' : $("#statuses").val() == undefined ? "" : $("#statuses").val(),
                    'sortField' : $("#sortBy").val(),
                    'term' : $("#query").val(),
                    'projectId' : $("#project-selector").val(),
                    'collectionId' : $("#collection-selector").val(),
                    useSubmitterContext : !_options.isAdministrator,
                    selectResourcesFromCollectionid: options.selectResourcesFromCollectionid
                }
            },
            selectableRows : _options.isSelectable,
            rowSelectionCallback : function(id, obj, isAdded) {
                if (isAdded) {
                    _rowSelected(obj, $dataTable);
                } else {
                    _rowUnselected(obj, $dataTable);
                }
            }
        });

        $("#project-selector").change(function() {
            var projId = $(this).val();
            $.cookie("tdar_datatable_selected_project", projId);
            $("#resource_datatable").dataTable().fnDraw();
        });

        $("#collection-selector").change(function() {
            var projId = $(this).val();
            $.cookie("tdar_datatable_selected_collection", projId);
            $("#resource_datatable").dataTable().fnDraw();
        });

        $("#resourceTypes").change(function() {
            $("#resource_datatable").dataTable().fnDraw();
            $.cookie($(this).attr("id"), $(this).val());
        });

        $("#statuses").change(function() {
            $("#resource_datatable").dataTable().fnDraw();
            $.cookie($(this).attr("id"), $(this).val());
        });

        $("#sortBy").change(function() {
            $("#resource_datatable").dataTable().fnDraw();
            $.cookie($(this).attr("id"), $(this).val());
        });

        $("#query").change(function() {
            $("#resource_datatable").dataTable().fnDraw();
            $.cookie($(this).attr("id"), $(this).val());
        });

        $("#query").bindWithDelay("keyup", function() {
            $("#resource_datatable").dataTable().fnDraw();
        }, 500);

        _scrollOnPagination();
    }

    //
    /**
     * populate the dataTable.data('selectedRows') from the hidden inputs in #divSelectedResources (e.g. when rendering 'edit' or 'input' form)
     * 
     * @param dataTable
     *            datatable widget
     * @param resourcesTable
     *            the table that contains the selected datatable rows.
     * @private
     */
    function _registerResourceCollectionDataTable(dataTable, resourcesTable) {
        // if user is editing existing collection, gather the hidden elements and put them in the 'seleted rows' object
        var $dataTable = $(dataTable);
        var $resourcesTable = $(resourcesTable);
        var selectedRows = {};

        $.each($('input', '#divSelectedResources'), function() {
            var elem = this;
            selectedRows[elem.value] = {
                id : elem.value,
                title : 'n/a',
                description : 'n/a'
            };
            // console.debug('adding id to preselected rows:' + elem.value);
        });
        $dataTable.data('selectedRows', selectedRows);

        // hide the selected items table if server hasn't prepopulated it
        if ($resourcesTable.find('tr').length == 1) {
            $resourcesTable.hide();
        }

    }

    /**
     * row selected callback. This callback constructs a table row for the "selected records" table
     * 
     * @param obj
     * @private
     */
    function _rowSelected(obj, $dataTable) {
        var $tableAdd = $("#tblToAdd");
        var $tableRemove = $("#tblToRemove");

        //remove tr, hidden field, id from  the 'remove' lists, if present
        _arrayRemove($dataTable.data("toRemove"), obj.id);
        $("#trmod_" + obj.id).remove();

        //if the resource was part of the collection to begin with, do nothing
        if(obj.isSelectedResult)  {
        } else {
            _addRow($dataTable, $tableAdd, "trmod_" + obj.id, obj,"toAdd");

        }

    }

    /**
     * row unselected callback: remove the row of the "selected records" table
     * 
     * @param obj
     * @private
     */
    function _rowUnselected(obj, $dataTable) {
        var $tableAdd = $("#tblToAdd");
        var $tableRemove = $("#tblToRemove");


        //remove tr, hidden field, id from  the 'add' lists, if present
        _arrayRemove($dataTable.data("toAdd"), obj.id);
        $("#trmod_" + obj.id).remove();

        //if resource wasn't part of selection to begin with, do nothing
        if(obj.isSelectedResult)  {
            // add the hidden input tag to the dom
            // next, add a new row to the 'selected items' table.
            _addRow($dataTable, $tableRemove, "trmod_" + obj.id, obj, "toRemove");
        } else {
        }
    }

    function _addRow($dataTable, $table, idattr, obj, action) {

        /**
         * Modification to use encapsulation and less dom manipulation --
         * Row contains hidden input, so removing the row, removes the element entirely
         */
        _arrayAdd($dataTable.data(action), obj.id);

        var $tr = $("<tr><td>" + obj.id 
                + '<input type="hidden" name="'+action+'" value="' + obj.id + '" id="hrid' + obj.id + '">'
                + "</td><td>"+obj.title+"</td></tr>");
        $tr.attr("id", idattr);
        $table.append($tr);
    }



    /**
     * pagination callback: this callback returns the vertical scroll position to the top of the page when the user navigates to a new page.
     * 
     * @private
     */
    function _scrollOnPagination() {
        $(".dataTables_paginate a").click(function() {
            $(".dataTables_scrollBody").animate({
                scrollTop : 0
            });
            return true;
        });
    }

    /**
     * Define sorting behavior when user clicks on datatable columns. Currency detection courtesy of Allan Jardine, Nuno Gomes
     * (http://legacy.datatables.net/plug-ins/type-detection)
     * 
     * @private
     */
    function _extendSorting() {
        // match anything that is not a currency symbol, seperator, or number (if some of these symbols appear identical then your IDE sucks)
        // if regex matches we assume it is definitly not a currency (e.g. "apple"), if false it *may* be a currency (e.g. "$3")
        var _reDetect = /[^$₠₡₢₣₤₥₦₧₨₩₪₫€₭₮₯₰₱₲₳₴₵¢₶0123456789.,-]/;

        // assuming we have detected a currency string, we use this regex to strip out symbols prior to sort
        var _rePrep = /[^-\d.]/g

        function _fnCurrencyDetect(sData) {
            var ret = null;
            if (typeof sData !== "string" || _reDetect.test(sData)) {
                ret = null;
            } else {
                ret = "tdar-currency";
            }
            return ret;
        }

        function _fnCurrencySortPrep(a) {
            a = (a === "-") ? 0 : a.replace(_rePrep, "");
            return parseFloat(a);
        }

        function _fnCurrencySortAsc(a, b) {
            return a - b;
        }

        function _fnCurrencySortDesc(a, b) {
            return b - a;
        }

        // add our custom type detector to the front of the line
        jQuery.fn.dataTableExt.aTypes.unshift(_fnCurrencyDetect);

        // register our custom sorters
        jQuery.fn.dataTableExt.oSort['tdar-currency-pre'] = _fnCurrencySortPrep;
        jQuery.fn.dataTableExt.oSort['tdar-currency-asc'] = _fnCurrencySortAsc;
        jQuery.fn.dataTableExt.oSort['tdar-currency-desc'] = _fnCurrencySortDesc;
    }

    function _fnRenderPersonId(oObj) {
        // in spite of name, aData is an object containing the resource record for this row
        var objResource = oObj.aData;
        var html = '<a href="' + TDAR.uri('browse/creators/' + objResource.id) + '" class=\'title\'>' + objResource.id + '</a>';
        return html;
    }
    function _registerUserLookupDatatable() {
        var settings = {
            tableSelector : '#dataTable',
            sAjaxSource : '/lookup/person',
            "sDom" : "<'row'<'span6'l><'span6'f>r>t<'row'<'span4'i><'span5'p>>",
            sPaginationType : "bootstrap",
            "bLengthChange" : true,
            "bFilter" : true,
            sAjaxDataProp : 'people',
            selectableRows : false,
            aoColumns : [ {
                sTitle : "id",
                bUseRendered : false,
                mDataProp : "id",
                tdarSortOption : 'ID',
                bSortable : false,
                fnRender : TDAR.datatable.renderPersonId
            }, {
                sTitle : "First",
                mDataProp : "firstName",
                tdarSortOption : 'FIRST_NAME',
                bSortable : false
            }, {
                sTitle : "Last",
                mDataProp : "lastName",
                tdarSortOption : 'LAST_NAME',
                bSortable : false
            }, {
                sTitle : "Email",
                mDataProp : "email",
                tdarSortOption : 'CREATOR_EMAIL',
                bSortable : false
            } ],
            requestCallback : function() {
                return {
                    minLookupLength : 0,
                    registered : 'true',
                    term : $("#dataTable_filter input").val()
                };
            }
        };

        TDAR.datatable.registerLookupDataTable(settings);
    }

    function _checkAllToggle() {
        var unchecked = $('#resource_datatable td input[type=checkbox]:unchecked');
        var checked = $('#resource_datatable td input[type=checkbox]:checked');
        if (unchecked.length > 0) {
            $(unchecked).click();
        } else {
            $(checked).click();
        }
    }

    function _registerChild(id, title) {
        var _windowOpener = null;
        // swallow cors exception. this can happen if window is a child but not an adhoc target
        try {
            if (window.opener) {
                _windowOpener = window.opener.TDAR.common.adhocTarget;
            }
        } catch (ex) {
            console.log("window parent not available - skipping adhoctarget check");
        }

        if (_windowOpener) {
            window.opener.TDAR.common.populateTarget({
                id : id,
                title : title
            });

            $("#datatable-child").dialog({
                resizable : false,
                modal : true,
                buttons : {
                    "Return to original page" : function() {
                        window.opener.focus();
                        window.close();
                    },
                    "Stay on this page" : function() {
                        window.opener.adhocTarget = null;
                        $(this).dialog("close");
                    }
                }
            });
        }
    }

    function _initializeCollectionAddRemove(id) {
        //dont allow submit until collection contents fully initialized.
//        $(".submitButton").prop("disabled", true);
        var $datatable = $("#resource_datatable");
        var $container = $("#divNoticeContainer");

        $datatable.on("change", ".datatable-checkbox.project", function() {
            if ($container.is(":visible")) {
                return;
            }
            if ($(this).is(":checked")) {
                $container.show();
            }
        });
    }

    function _initalizeResourceDatasetDataTable(columns, viewRowSupported, resourceId, namespace, dataTableId) {
        jQuery.fn.dataTableExt.oPagination.iFullNumbersShowPages = 3;
        $.extend($.fn.dataTableExt.oStdClasses, {
            "sWrapper" : "dataTables_wrapper form-inline"
        });

        var offset = 0;
        var browseUrl = TDAR.uri("datatable/browse?id=" + dataTableId);
        var options = {
            "sAjaxDataProp" : "results",
            "sDom" : "<'row'<'span6'l><'span3'>r>t<'row'<'span4'i><'span5'p>>",
            "bProcessing" : true,
            "bServerSide" : true,
            "bScrollInfinite" : false,
            "bScrollCollapse" : true,
            tableSelector : '#dataTable',
            sPaginationType : "bootstrap",
            sScrollX : "100%",
            "sScrollY" : "",
            "aoColumns" : [],
            "sAjaxSource" : browseUrl
        };

        if (viewRowSupported) {
            options.aoColumns.push({
                "bSortable" : false,
                "sName" : "id_row_tdar",
                "sTitle" : '<i class="icon-eye-open  icon-white"></i>',
                "fnRender" : function(obj) {
                    return '<a href="/' + namespace + '/view-row?id=' + resourceId + '&dataTableId=' + dataTableId + '&rowId=' + obj.aData[0] +
                            '" title="View row as page..."><i class="icon-list-alt"></i></a></li>';
                }
            });
            offset++;
        }
        ;
        var size = 0;
        for ( var col in columns) {
            if (columns.hasOwnProperty(col)) {
                size++;
                options.aoColumns.push({
                    "bSortable" : false,
                    "sName" : columns[col].simpleName,
                    "sTitle" : columns[col].displayName,
                    "tdarIdx" : size + offset -1,
                    "fnRender" : function(obj) {
                        var val = obj.aData[this.tdarIdx];
                        var str = TDAR.common.htmlEncode(val);
                        return str;
                    }
                });
            }
        }
        if (size > 0) {
            TDAR.datatable.registerLookupDataTable(options);
        }

    }

    /**
     * add item to array if not found in array. returns undef
     * @param arr
     * @param item
     * @private
     */
    function _arrayAdd(arr, item) {
        if(arr.indexOf(item) === -1) {
            arr.push(item);
        }
    }

    /**
     * remove item from array if found. return undef
     * @param arr
     * @param item
     * @private
     */
    function _arrayRemove(arr, item) {
        var idx = arr.indexOf(item);
        if(idx !== -1) {
            arr.splice(idx, 1);
        }
    }

    return {
        extendSorting : _extendSorting,
        registerLookupDataTable : _registerLookupDataTable,
        initUserDataTable : _registerUserLookupDatatable,
        setupDashboardDataTable : _setupDashboardDataTable,
        registerResourceCollectionDataTable : _registerResourceCollectionDataTable,
        renderPersonId : _fnRenderPersonId,
        checkAllToggle : _checkAllToggle,
        registerChild : _registerChild,
        initalizeResourceDatasetDataTable : _initalizeResourceDatasetDataTable,
        registerAddRemoveSection : _initializeCollectionAddRemove
    };
}();
