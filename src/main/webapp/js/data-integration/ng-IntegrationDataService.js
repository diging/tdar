(function(angular) {
    "use strict"
    var app = angular.module("integrationApp");

    /**
     * generating a simpler JavaScript object that represents an integration.
     */
    function _dumpObject(integration) {
        var out = {
            title : "untitled",
            description : "",
            columns : [],
            datasets : [],
            dataTables : [],
            ontologies : []
        };

        if (integration.description != undefined) {
            out.description = integration.description;
        }

        if (integration.title != undefined) {
            out.title = integration.title;
        }

        // we create an internal map for ontologies so we can keep track of All referenced ontologies as "view model" may remove integration.ontologies even if
        // they're used by a column
        var ontologies = {};
        // for each column
        if (integration.columns != undefined) {
            integration.columns.forEach(function(column) {
                var outputColumn = {
                    name : column.name,
                    type : column.type.toUpperCase(),
                    dataTableColumns : []
                };

                // get the data table columns in the same structure for Integration, Display, and Count columns
                var tempList = column.selectedDataTableColumns;
                if (tempList == undefined) {
                    tempList = [];
                    if (column.dataTableColumnSelections != undefined) {
                        column.dataTableColumnSelections.forEach(function(col) {
                            if (col != undefined) {
                                tempList.push(col.dataTableColumn);
                            }
                        });
                    }
                }

                // add the id/name
                tempList.forEach(function(dtc) {
                    if (dtc != undefined && dtc.id != undefined) {
                        var dtc_ = {
                            id : dtc.id,
                            name : dtc.name
                        }
                    }
                    outputColumn.dataTableColumns.push(dtc_);
                });

                // get the nodes and ontologes
                if (column.type == 'integration') {
                    var ont = {
                        id : column.ontology.id,
                        title : column.ontology.title
                    };
                    outputColumn.ontology = ont;
                    ontologies[column.ontology.id] = ont;

                    // get the selected nodes
                    if (!(column.nodeSelections == undefined)) {
                        outputColumn.nodeSelection = [];
                        column.nodeSelections.forEach(function(node) {
                            if (node.selected) {
                                var node_ = {
                                    id : node.node.id,
                                    iri : node.node.iri
                                };
                                outputColumn.nodeSelection.push(node_);
                            }
                            ;
                        });
                    }
                    ;
                }

                out.columns.push(outputColumn);
            });

        }

        // add any ontologies
        if (integration.ontologies != undefined) {
            integration.ontologies.forEach(function(ontology) {
                var ont = {
                    id : ontology.id,
                    title : ontology.title
                };
                ontologies[ontology.id] = ont;
            });
        }

        $.each(ontologies, function(ontId, ontology) {
            out.ontologies.push(ontology);
        })

        // add all data tables
        if (integration.dataTables != undefined) {
            integration.dataTables.forEach(function(dataTable) {
                var table = {
                    id : dataTable.id,
                    name : dataTable.name,
                    displayName : dataTable.displayName
                };
                out.dataTables.push(table);
            });
        }
        return out;
    }

    function _loadDocumentData() {
        var dataElements = $('[type="application/json"][id]').toArray();
        var map = {};
        dataElements.forEach(function(elem) {
            var key = elem.id;
            var val = JSON.parse(elem.innerHTML);
            map[key] = val;
        });
        return map;
    }

    function _dedupe(items) {
        var uniqueItems = [];
        items.forEach(function(item) {
            if (uniqueItems.indexOf(item) === -1) {
                uniqueItems.push(item);
            }
        })
        return uniqueItems;
    }

    function DataService($http, $cacheFactory, $q) {
        var self = this;
        var documentData = _loadDocumentData();
        var ontologyCache = $cacheFactory('ontologyCache');
        var dataTableCache = $cacheFactory('dataTableCache');
        var ontologyNodeCache = $cacheFactory('ontologyNodeCache');
        var dataTableColumnCache = $cacheFactory('dataTableColumnCache');

        // expose these caches for now, though a better solution is have find() methods that consult cache before making ajax call
        this.ontologyCache = ontologyCache;
        this.ontologyNodeCache = ontologyNodeCache;
        this.dataTableCache = dataTableCache;
        this.dataTableColumnCache = dataTableColumnCache;

        /**
         * Return a map<str, obj> of objects that were embedded in the DOM using script tags of type "application/json". for each entry in the map, the entry
         * key is the script element's ID attribute, and the key value is the parsed value of the script's JSON data.
         * 
         * @returns {*}
         */
        this.getDocumentData = function() {
            return documentData
        }

        /**
         * Create a subset of a specified array that does not contain duplicates (using object identity for equality)
         * 
         * @param items:{Array}
         * @returns {Array}
         */
        this.dedupe = _dedupe;
        this.dumpObject = _dumpObject;
        this.loadExistingIntegration = _loadExistingIntegration;
        this.saveIntegration = _saveIntegration;

        /**
         * Takes a JSON representation of the integration and saves it to the server. Server will pass back an ID and status if an ID is not already defined
         */
        function _saveIntegration(integration) {
            var futureData = $q.defer();
            var jsonData = self.dumpObject(integration);
            var path = '/api/integration/save';
            var done = false;
            // if we're doing an update
            if (parseInt(integration.id) > -1) {
                path += "/" + integration.id;
            }
            console.log("savePath:" + path + " -- " + integration.id);
            console.log(jsonData);
            var httpPromise = $http({
                method : "POST",
                url : path,
                data : $.param({
                    integration : JSON.stringify(jsonData)
                }),
                headers : {
                    'Content-Type' : 'application/x-www-form-urlencoded'
                }
            });

            // promise is not 100% used here, but in the future, perhaps needed
            httpPromise.success(function(data) {
                integration.id = data.id;
                futureData.resolve(data);
            });

            return futureData.promise;
        }

        /**
         * Based on the specified JSON representation of a data object, try and rebuild the integration
         */
        function _loadExistingIntegration(json, integration) {
            var futureData = $q.defer();

            var dataTableIds = [];
            json.dataTables.forEach(function(dataTable) {
                dataTableIds.push(dataTable.id);
            });
            integration.title = json.title;
            integration.description = json.description;
            integration.id = json.id;
            console.log("starting...");
            // Load the datasets and then use the results to build the columns out
            self.loadTableDetails(dataTableIds).then(function(dataTables) {
                self.addDataTables(integration, dataTables);
                var result = self.loadUpdatedParticipationInformation(integration);
                result.then(function(status) {
                    json.columns.forEach(function(column) {
                        var name = column.name;
                        if (name == undefined) {
                            name = "column";
                        }

                        var ids = [];
                        column.dataTableColumns.forEach(function(col) {
                            if (col != undefined && col.id != undefined) {
                                ids.push(col.id);
                            }
                        });

                        if (column.type == 'DISPLAY') {
                            integration.addDisplayColumn(name);
                            self._loadDisplayIntegrationColumns(ids, integration.columns[integration.columns.length - 1]);
                        }
                        if (column.type == 'COUNT') {
                            integration.addCountColumn(name);
                            self._loadDisplayIntegrationColumns(ids, integration.columns[integration.columns.length - 1]);
                        }
                        if (column.type == 'INTEGRATION') {
                            var ontology = undefined;
                            var ontologyIds = new Array();
                            var ontologies = new Array();
                            
                            integration.ontologies.forEach(function(ont) {
                                if (column.ontology.id == ont.id) {
                                    ontology = ont;
                                } else {
                                    ontologyIds.push(ont.id);
                                }
                            });
                            self.loadOntologyDetails(ontologyIds).then(function() {
                                if (ontology == undefined) {
                                    ontology = ontologyCache.get(column.ontology.id);
                                    integration.ontologies.push(ontology);
                                    _rebuildSharedOntologies(integration);
                                }
                                // FIXME: if not loaded, then need to go to the server for ontology details
                                integration.addIntegrationColumn(name, ontology);
                                var col = integration.columns[integration.columns.length - 1];

                                // FIXME: I'm less sure about this direct replacement -- is this okay? It appears to work, change to setter
                                col.selectedDataTableColumns = self.getCachedDataTableColumns(ids);
                                col.nodeSelections.forEach(function(node) {
                                    column.nodeSelection.forEach(function(nodeRef) {
                                        if (nodeRef.id == node.node.id) {
                                            node.selected = true;
                                        }
                                    });
                                });
                            });
                        }
                    });
                    futureData.resolve(true);
                });
            });
            return futureData.promise;
        }

        self.addDataTables = function(integration, tablesToAdd) {
            integration.addDataTables(tablesToAdd);
            _rebuildSharedOntologies(integration);
        }

        self.removeDataTables = function(integration, tablesToRemove) {
            integration.removeDataTables(tablesToRemove);
            _rebuildSharedOntologies(integration);
        }

        /**
         * Replace the current list of ontologies w/ a computed list of shared ontologies
         * 
         * @private
         */
        function _rebuildSharedOntologies(integration) {
            integration.ontologies = self.getCachedOntologies(integration.getSharedOntologyIds())

            // update integrationColumnStatus
            // fixme: this really should be a computed property on integrationColumn.
            // fixme: integrationColumn should be a proper IntegrationColumn class w/ methods and stuff.
            integration.getIntegrationColumns().forEach(function(integrationColumn) {
                integrationColumn.isValidMapping = (integration.getSharedOntologyIds().indexOf(integrationColumn.ontologyId) > -1);
            });
        }

        /**
         * internal method to initialize a Display or Count column in loading
         */
        self._loadDisplayIntegrationColumns = function(ids, col) {

            col.dataTableColumnSelections.forEach(function(selection) {
                console.log(selection);
                selection.dataTable.dataTableColumns.forEach(function(dtc) {
                    ids.forEach(function(id) {
                        if (dtc.id == id) {
                            selection.dataTableColumn = dtc;
                            ids.splice(ids.indexOf(id), 1);
                        }
                    });
                });
            });
            if (ids.length > 0) {
                console.error("did not restore all ids");
            }

        };

        /**
         * Gather any dataTableColumns that have missing participation information and tell dataService to fetch it.
         * 
         */
        this.loadUpdatedParticipationInformation = function(integration) {
            var futureData = $q.defer();

            // find dataTableColumns that are missing transientNodeParticipation, request it from dataService
            var unprocessedIds = integration.getMappedDataTableColumns()
                    .filter(function(col) {return !col.transientNodeParticipation})
                    .map(function(col){return col.id})

            if (unprocessedIds.length === 0) {
                futureData.resolve(true);
            } else {
                self.loadNodeParticipation(unprocessedIds).then(function(newColumns) {
                    futureData.resolve(true);
                });
            }
            return futureData.promise;
        };

        /**
         * Returns httpPromise of an object containing: dataTable objects corresponding to the specified array of dataTableId's
         * 
         * @param dataTableIds
         * @return HttpPromise futureDataTables:httppromise<{dataTables: Array<dataTable>, sharedOntologies: Array<ontology>}>
         * 
         */
        this.loadTableDetails = function(dataTableIds) {
            var futureData = $q.defer();

            // only load tables that aren't already in the cache
            var missingTableIds = dataTableIds.filter(function(dataTableId) {
                return !dataTableCache.get(dataTableId)
            });
            // not sure if dupe tableIds will ever occur, but dedupe anyway.
            missingTableIds = _dedupe(missingTableIds);

            if (missingTableIds.length > 0) {
                var httpPromise = $http.get('/api/integration/table-details?' + $.param({
                    dataTableIds : missingTableIds
                }, true));

                httpPromise.success(function(data) {

                    // add this new data to our caches
                    data.dataTables.forEach(function(dataTable) {
                        dataTableCache.put(dataTable.id, dataTable);
                        dataTable.dataTableColumns.forEach(function(dtc) {
                            dataTableColumnCache.put(dtc.id, dtc);
                        });
                    });

                    //cache the mapped ontologies that aren't already cached
                    data.mappedOntologies
                            .filter(function(ontology){return !ontologyCache.get(ontology.id)})
                            .forEach(function(ontology) {
                                ontologyCache.put(ontology.id, ontology);
                                ontology.nodes.forEach(function(ontologyNode) {
                                    ontologyNodeCache.put(ontologyNode.id, ontologyNode);
                                });
                    });

                    // now that we have everything in the cache, return the requested dataTables back to the caller
                    futureData.resolve(self.getCachedDataTables(dataTableIds));

                });

            } else {
                // in the event that there's nothing to load, callback immediately with the results
                futureData.resolve(self.getCachedDataTables(dataTableIds));

            }

            // TODO: create recursive 'unroll' function that emits params in struts-friendly syntax

            return futureData.promise;
        };

        /**
         * Returns httpPromise of an object containing: ontology objects corresponding to the specified array of ontologyId's
         * 
         * @param ontologyIds
         * @return HttpPromise futureOntologies:httppromise
         * 
         */
        this.loadOntologyDetails = function(ontologyIds) {
            var futureData = $q.defer();
            console.log("loading info for ontologies from server:", ontologyIds);
            // only load tables that aren't already in the cache
            var missingOntologyIds = ontologyIds.filter(function(ontologyId) {
                return !ontologyCache.get(ontologyId)
            });
            // not sure if dupe tableIds will ever occur, but dedupe anyway.
            ontologyIds = _dedupe(ontologyIds);

            if (ontologyIds.length > 0) {
                var httpPromise = $http.get('/api/integration/ontology-details?' + $.param({
                    ontologyIds : ontologyIds
                }, true));

                httpPromise.success(function(data) {
                    console.log(data);
                    // add this new data to our caches
                    data.forEach(function(proxy) {
                        proxy.ontology.nodes = [];
                        proxy.ontology.nodes.concat(proxy.nodes);
                        ontologyCache.put(proxy.ontology.id, proxy.ontology);
                        proxy.nodes.forEach(function(ontologyNode) {
                            ontologyNodeCache.put(ontologyNode.id, ontologyNode);
                        });
                    });

                    futureData.resolve(true);

                });

            } else {
                // in the event that there's nothing to load, callback immediately with the results
                futureData.resolve(true);
            }
            return futureData.promise;
        };
        

        /**
         * Returns httpPromise of an object containing: integration results
         * 
         * @param integration model
         * @return HttpPromise integrationResult:httppromise
         * 
         */
        this.performIntegration = function(integration) {
            var futureData = $q.defer();
            console.log("starting integration:");
            var integrationJson = self._dumpObject(integration);
            
            var httpPromise = $http.get('/api/integration/integrate?' + $.param({
                integration : integrationJson
            }, true));

            httpPromise.success(function(data) {
                futureData.resolve(data);

            });

            return futureData.promise;
        };

        var  _futureCancel = null;

        /**
         * send $http.get to specified url, return promise<transformedData> if transformer specified, otherwise return promise<data>;
         * 
         * @param url
         * @param searchFilter
         * @param transformer
         * @private
         */
        // fixme: move SearchFilter class to DataService.js
        function _doSearch(url, searchFilter, transformer, prefix) {
            var futureData = $q.defer();
            if(_futureCancel) {
                _futureCancel.resolve();
            }
            _futureCancel = $q.defer();

            var config = {
                params : searchFilter.toStrutsParams(),
                cache: true,
                timeout: _futureCancel.promise
            };

            $http.get(url, config).success(function(rawData_) {
                _futureCancel = null;
                var rawData = rawData_;
                if (prefix !== undefined) {
                    rawData = rawData_[prefix];
                }
                var total = rawData_.totalResults;
                var data = !!transformer ? transformer(rawData) : rawData;
                var ret = {
                        "totalRecords": total,
                        "results": data
                }
                futureData.resolve(ret);
            });
            return futureData.promise;
        }

        /**
         * Search for datasets(actually, dataTables) filtered according to specified SearchFilter. returns promise<Array<searchResult>>
         * 
         * @param searchFilter
         *            (note: searchFilter.categoryId ignored in dataset search)
         * @returns {*}
         */
        this.findDatasets = function(searchFilter) {
            console.debug(searchFilter);
            var transformer = function(data) {
                return data.map(function(item) {
                    var d = new Date(0);
                    d.setUTCSeconds(parseInt(item.dataset.dateCreated) / 1000);
                    var result = item;
                    result.title = item.dataset.title;
                    result.title += ' - ' + item.dataTable.displayName;
                    result.id = item.dataTable.id;
                    result.date_created = d;
                    result.ontologies = [];
                    item.mappedOntologies.forEach(function(ontology) {
                        result.ontologies.push(ontology.title + " (" + ontology.id + ")");
                    });
                    return result;
                })
            };
            return _doSearch("/api/integration/find-datasets", searchFilter, transformer, "dataTables");
        };

        // todo: Note that the UI for this feature is disabled, but this function ostensibly still "works"
        /**
         * Search for ontologies according to specified SearchFilter. returns promise<Array<searchResult>>
         * 
         * @param searchFilter
         * @returns {*}
         */
        this.findOntologies = function(searchFilter) {
            // FIXME: write transformer
            // NOT IMPLEMENTED
            return _doSearch("/api/integration/find-ontologies", searchFilter, null, "ontology");
        };

        /**
         * Return an array previously-loaded ontologies in the same order of the specified array of ontologyId's
         * 
         * @param ontologyIds
         */
        this.getCachedOntologies = function _getCachedOntologies(ontologyIds) {
            return ontologyIds.map(function(ontologyId) {
                return self.ontologyCache.get(ontologyId);
            });
        };

        /**
         * 
         * Return an array of previously-loaded dataTables in the same order of the specified dataTableIds. Note it is not necessary to actively consult the
         * cache before attempting to load them. DataService.loadTableDetails() will automagically pull from cached results when available.
         * 
         * @param dataTableIds
         */
        this.getCachedDataTables = function _getCachedDataTables(dataTableIds) {
            return dataTableIds.map(function(id) {
                return dataTableCache.get(id);
            });
        };

        /**
         * Return an array previously-loaded data table columns in the same order of the specified array of dataTableColumnId's
         * 
         * @param dataTableColumnIds
         */
        this.getCachedDataTableColumns = function _getDataTableColumns(dataTableColumnIds) {
            return dataTableColumnIds.map(function(dataTableColumnId) {
                return self.dataTableColumnCache.get(dataTableColumnId);
            });
        };

        //convert verbose participation info into something we can use
        var transformVerboseNodeInfo = function(data) {
            var transformedData = data.reduce(function(obj, info) {
                obj[info.dataTableColumn.id] = info.flattenedNodes.map(function(node){
                    return node.id;
                });
                return obj;
            }, {});
            return transformedData;
        };

        /**
         * Returns a promise of the ontologyNodeValues that occur in each specified dataTableColumn
         * 
         * @param dataTableColumnIds
         */
        this.loadNodeParticipation = function(dataTableColumnIds) {
            var url = '/api/integration/node-participation?' + $.param({
                dataTableColumnIds : dataTableColumnIds,
                verbose: 'true'
            }, true);
            var httpPromise = $http.get(url);
            var futureWork = $q.defer();
            var dataTableColumns = [];
            httpPromise.success(function(verboseData) {
                var nodeIdsByColumnId = transformVerboseNodeInfo(verboseData);
                Object.keys(nodeIdsByColumnId).forEach(function(dataTableColumnId) {
                    var dataTableColumn = dataTableColumnCache.get(dataTableColumnId);
                    var nodes = nodeIdsByColumnId[dataTableColumnId].map(function(ontologyNodeId){return ontologyNodeCache.get(ontologyNodeId)});
                    dataTableColumn.transientNodeParticipation = nodes;
                });
                // Note that we mutate the data directly, so there is not anything to "return". We're just notifying the caller that we are done.
                futureWork.resolve(dataTableColumns);
            });
            return futureWork.promise;
        };

        this.processIntegration = function(integration) {
            var futureData = $q.defer();

            //FIXME: refactor struts action to accept 'Content-type: application/json'. This is the angular default.
            $http({
                method: 'POST',
                url: '/api/integration/integrate',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                data: $.param({integration: JSON.stringify(_dumpObject(integration))})
            })
                    .success(function(data){
                        futureData.resolve(data);
                    })
                    .error(function(){
                        futureData.reject("Integration failed");
                    });

            return futureData.promise;
        };

    }

    app.service("DataService", [ '$http', '$cacheFactory', '$q', DataService ]);

    /* global angular */
})(angular);