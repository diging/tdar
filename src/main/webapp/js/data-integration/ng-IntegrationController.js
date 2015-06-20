(function($, angular, console){
    "use strict";
    console.debug("IntegrationController::");
    
    var app = angular.module('integrationApp');

    // top-level controller for the integration viewmodel
    app.controller('IntegrationController', ['$rootScope', '$scope',  'IntegrationService', 'DataService', 'ValidationService',
        function($rootScope, $scope, integration, dataService, validationService){
        var self = this,
            _openModal,
            _processAddedIntegrationColumns;

        // controller public fields
        self.integration = integration;
        self.tab = 0;
        self.sharedOntologies = [];
        _openModal = function(options) {
            $rootScope.$broadcast("openTdarModal", options);
        };
        
        //status messages
        $scope.alert = {
            kind: "default",
            message: ""
        };
        
        

        // controller public methods
        self.setTab  = function(idx) {
            this.tab = idx;
        }

        self.isTabSet = function(idx) {
            return this.tab === idx;
        }

        self.closeTab = function(idx) {
            integration.removeOutputColumn(idx);
        }

        self.saveClicked = function() {
            self.updateStatus("Saving...");
            var savePromise = dataService.saveIntegration(self.integration);
            self.promiseStatus(savePromise, "Saving", "Saved");
            savePromise.then(function(status) {
                window.location.hash = self.integration.id;
            });
        };

        self.saveAsClicked = function() {
            console.log("Saving.");
            self.integration.id = -1;
            self.updateStatus("Saving...");

            dataService.saveIntegration(self.integration).then(
                    function(status) {
                        self.updateStatus("Save: " + status.status);
                    },
                    function(err){
                        self.updateStatus("Unable to save. Please review your seledtions and try again. If problem persists, please contact an administrator.");
                        console.error(err);
                    }
            );
        };

        /**
         * shows the user a status message
         * @param msg  the message to display
         * @param kind the kind of message ('info', 'success', or error'error')
         */
        self.updateStatus = function(msg, kind) {
            var _kind = kind ? kind : "success";
            $scope.statusMessage = msg;
            $scope.alert.message = msg;
            $scope.alert.kind = _kind;
        };

        /**
         * Show user status messages across the span of a promise lifecycle.
         *
         * @param promise
         * @param msgStart message to immediately display
         * @param msgDone message to display when promise resolves
         * @param [msgFail] message to display when promise rejects (default: "${msgStart} : failed")
         */
        self.promiseStatus = function(promise, msgStart, msgDone, msgFail) {
            var _msgDone = msgDone ? msgDone : (msgStart + ": " + "complete.")
            var _msgFail = msgFail ? msgFail : (msgStart + ": " + "failed.");

            self.updateStatus(msgStart, "default");
            promise.then(
                    function(){self.updateStatus(_msgDone, "success");},
                    function(){self.updateStatus(_msgFail, "error");}
            );
            return promise;
        }

        self.loadJSON = function() {
            var jsonData = dataService.getDocumentData("jsondata");
            if(!jsonData) return;

            //validate the embedded json first
            validationService.validateJson(jsonData);

            var result = dataService.loadIntegration(jsonData , self.integration);
            self.promiseStatus(result,
                "Loading integration...", 
                "Loading complete.",
                "Load failed. Please try again - if problem continues please contact a system administrator."
            );
            result.then(
                function(){
                    validationService.validateIntegration(jsonData, self.integration);
                    if(validationService.errors.length) {
                        self.updateStatus("Loading complete. However, the underlying resources used in this integration may have changed. " +
                        " Please review your selections and ensure they are up-to-date.");
                        console.warn(validationService.errors);
                        //in the event errors, rebuild cached/transient data
                        self.integration.reset();
                    }
                },
                function(err) {
                    console.log("load failed - error information follows");
                    console.error(err);
                });
         };
        
        /**
         * Called after user selects list of dataset id's from 'add datasets' modal.
         * 
         * @param dataTableIds
         */
        self.addDatasets = function(dataTableIds) {
            if(dataTableIds.length === 0) return;
            
            var tableDetailsPromise =  dataService.loadTableDetails(dataTableIds);
            self.promiseStatus(tableDetailsPromise, "Loading table details");
            
            tableDetailsPromise.then(function(dataTables) {
                dataService.addDataTables(integration, dataTables);
                var participationPromise = dataService.loadUpdatedParticipationInformation(integration);
                self.promiseStatus(participationPromise, 
                    "Loadng participation data",
                    "Loading complete",
                    "Error: unable to load ontology info.  Please try again - if the problem continues please contact a system administrator");
            });
        };

        // add and initialize an integration column associated with with the specified ontology ID
        _processAddedIntegrationColumns = function(ontologies) {
            console.debug("_processAddedIntegrationColumns ::");
            ontologies.forEach(function(ontology){
                self.integration.addIntegrationColumn('intcol' + ontology.id, ontology);
            });
        };

        self.addDatasetsClicked = function(arg) {
            console.debug('Add Datasets clicked');
            _openModal({
                title: "Add Datasets",
                searchType: "dataset",
                close: function(data) {
                    self.addDatasets(data);
                }

            });
        };

        self.addIntegrationColumnsClicked = function(arg) {
            console.debug('Add Integration Columns Clicked');
            _openModal({
                title: "Add Ontologies",
                searchType: "ontology",
                categoryFilter: true,
                close: function(data) {
                    self.addIntegrationColumns(data);
                }
            });
        };

        self.addDisplayColumnClicked = function(arg) {
            integration.addDisplayColumn("display column");
            self.setTab(integration.columns.length -1);
        };

        self.addCountColumnClicked = function(arg) {
            integration.addCountColumn("count column");
            self.setTab(integration.columns.length -1);
        };

        self.removeSelectedDatasetClicked = function() {
            dataService.removeDataTables(integration, $scope.selectedDataTables);
        };

        self.addIntegrationColumnsMenuItemClicked = function(ontology) {
            self.integration.addIntegrationColumn(ontology.title, ontology);
            self.setTab(integration.columns.length -1);
        }

        self.isCountColumnDisabled = function() {
            return !self.integration.isCountColumnEnabled();
        };

        /**
         * Output column filter: show only 'count' columns
         * 
         * @param col
         * @returns {boolean}
         */
        $scope.filterCount = function(col) {
            return col.columnEncodingType === 'COUNT';
        };

        $scope.lookupCompatibleColumns = function(id) {
            return self.integration.mappedDataTables[id];
            // FIXME: angular doesn't like my getter functions - apparently they cause endless loop of detected changes (or something?)
            // https://docs.angularjs.org/error/$rootScope/infdig
            // return (self.integration.getMappedDataTables()[id]);
        };

        // FIXME: proper validation required
        $scope.isValid = function() {
            // do we have title or enough to "save"
            if (!$scope.isMinimallyValid()) {
                return false;
            }
            
            // do we have a mapped dataset
            if (integration.dataTables.length < 1) {
                return false;
            }

            // do we have an integration column
            if (integration.getIntegrationColumns().length == 0) {
                return false;
            }

            
            var validMappedIntegrationColumns = integration.getIntegrationColumns().every(function(col) {
                var allColumnsMapped = col.selectedDataTableColumns.length === integration.dataTables.length;
                if(!allColumnsMapped) {
                    console.info("column has fewer selected dataTableColumns than selected dataTables: %s", col.name);
                }
                return allColumnsMapped;
            });
            
            return validMappedIntegrationColumns;
        }

        $scope.isMinimallyValid = function() {
            if (self.integration.title == undefined || $.trim(self.integration.title) == "") {
                return false;
            }
            return true;
        }

        /**
         * convenience method: does the current ontologyNodeValue occur in the current dataTableColumn.
         * 
         * @param dataTableColumnId
         */
        $scope.ontologyValuePresent = function(dataTableColumn, ontology) {
            return self.integration.isNodePresent(dataTableColumn, ontology);
        }

        /**
         * 
         * 
         * @param criteria
         *            "some" or "every"
         */
        $scope.selectMatchingNodes = function(criteria) {
            var integrationColumn = self.integration.columns[self.tab];
            var nodeSelections = integrationColumn.nodeSelections;
            nodeSelections.forEach(function(selectionInfo){
                var ontologyNode = selectionInfo.node;
                var mappedColumns = self.integration.getMappedDataTableColumns(integrationColumn.ontologyId)
                selectionInfo.selected = selectionInfo.selected ||  mappedColumns[criteria](function(dtc){
                    return self.integration.isNodePresent(dtc, ontologyNode)
                });
            });

        };


        $scope.downloadReady = false;

        $scope.download = null;

        /**
         * Hide the 'download info' notification and dispose results.
         */
        $scope.dismissDownload = function() {
            $scope.download = false;
            $scope.download = null;
        }

        /**
         * Send the integration to the server for processing.  If successful,  show notification.
         */
        self.submitIntegration  = function() {
            $scope.downloadReady = false
            var results = dataService.processIntegration(integration);
            self.promiseStatus(results, "Processing integration, please wait...", "Integration Results ready!");
            results.then(function(data){
                $scope.downloadReady = true;
                $('#divResultContainer').modal({show:true});
                $scope.download = {
                    previewData: {
                        //fixme: where are the column names? generate 'col1, col2, ..., coln' for now
                        columns: data.previewColumnLabels,
                        rows: data.previewData
                    },
                    pivotData: {
                        columns: data.pivotColumnLabels,
                        rows: data.pivotData
                    },
                    ticketId: data.ticket.id
                };
                console.debug(data);
            }, function(err) {
                console.debug("submitIntegration:: failed:%s", err);
                //todo: toast explaining what went wrong
            });

        };
    }]);

    /* global jQuery, angular */
})(jQuery, angular, console);
