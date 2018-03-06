TDAR.vuejs.collectionwidget = (function(console, $, ctx, Vue,axios) {
    "use strict";

var _getSelectizeOpts = function() {
   var opts = {
        valueField: 'id',
        labelField: 'name',
        searchField: 'name',
        create: true,
        createOnBlur:true,
        render: {
            option: function(item, escape) {
                    return '<div>' +
                        '<span class="title">' +
                            '<span class="name">' + escape(item.name) + '</span>' +
                        '</span>' +
                    '</div>';
                }
            },
       
    load: function(query, callback) {
        if (!query.length) return callback();
        $.ajax({
            url: '/api/lookup/collection',
            type: 'GET',
            data: { 
               'term':query,
               'permission':'ADMINISTER_COLLECTION'
            },
            error: function() {
                callback();
            },
            success: function(res) {
                callback(res.collections);
            }
         });
     }
     }; 
     return opts;

}

var _init = function(appId) {

//    Vue.component('collection', 
//    { 
//    props: ["id","name"],
//    template: "<option :id='id' class='extra'>{{name}}</option>",
//        computed: {
//            ellipseName: function(){
//            name
//                // this needs to have the TDAR libs in so that the name will
//				// compute.
//               // TDAR.common.htmlEncode(TDAR.ellipsify(name, 80));
//            }
//        }
//    });
//
//     Vue.component('resource-collection', 
//    { 
//    props: ["id","name"],
//    template: "<li :id='id' class='extra'>{{name}}</li>",
//    });  

    $("#addToExisting").popover({placement:'right', delay:{hide:2000}});
        
        
      var app2 = new Vue({
        el: appId,
    
    data: { 
    	//For use in the View Resource "Add to Collection" widget. 
        items:[{id:"1",name:"Sample"}], 
        selectedCollection: 0 ,
        pick:"existing", //For the radio button of existing or new collection. 
        options:[],
        unmanagedEnabled: true,
        newCollectionName:"",
        newCollectionDescription:"",
        managedCollectionsToRemove: [],
        unmanagedCollectionsToRemove: [],
        changesMade: false,
        showPermission:false,
        managedResource: true,
        resourceId: -1,
        administrator: false,
        canEdit: false,
        collections: {managed:[], unmanaged:[]},
        
        //For use in the Save Search Results to Collection widget. 
        progressStatus:0,
        lastSavedCollectionId:0
    },
    watch: {
        newCollectionName: function (value) {
            Vue.set(this,"changesMade",true);
        },
        selectedCollection: function (value) {
            Vue.set(this,"changesMade",true);
        }
    },
    mounted: function() {
        var $e = $(this.$el);
        console.log("Calling mounting functions");
        if($e.data('resourceId')!=null){
            	console.log("Mounted");
            	Vue.set(this, 'administrator',$e.data('administrator'));
            	console.log("Administrator is ",this.administrator);
            Vue.set(this, 'canEdit',$e.data('canEdit'));
            Vue.set(this, 'unmanagedEnabled',$e.data('unmanagedEnabled'));
            console.log("unmanagedEnabled",this.unmanagedEnabled);
            
            if (this.unmanagedEnabled == undefined || this.unmanagedEnabled == false) {
            	console.log("Forcing resource to be managed");
                Vue.set(this,"managedResource",true);
            }
            
        	    Vue.set(this, 'resourceId',$e.data('resourceId'));
        	    this._getCollectionsForResource();
        }
    },
    
    methods: {
    	
    	//Helper methods: maybe move to a static library?
        _arrayRemove: function(arr, item) {
            var idx = arr.indexOf(item);
            if(idx !== -1) {
                arr.splice(idx, 1);
            }
        },
        
        ellipse : function(value){
            return TDAR.common.htmlEncode(TDAR.ellipsify(value, 80))
         }, 
        
         
         //"Add to Collection" functions. 
        _resetForm: function(){
        	console.log("Resetting the 'Add to Collection' form");
            this.selectedCollection='';
            this.pick="existing";
            Vue.set(this,"changesMade",false);
            this.newCollectionName="";
            this.newCollectionDescription="";
            this.managedResource = false;
            if (this.unmanagedEnabled == undefined || this.unmanagedEnabled == false) {
                Vue.set(this,"managedResource",true);
            }

            this.managedCollectionsToRemove = [];
            this.unmanagedCollectionsToRemove = [];
            var $select = $('#collection-list').selectize();
            $select[0].selectize.clear();
        },
        
        getCollections: function(){
        	console.log("Getting list of all available collections");
            var self = this;
            var permission = "ADD_TO_COLLECTION";
            if (this.unmanagedEnabled == true) {
                permission = ADD_TO_COLLECTION;
            }
            axios.get("/api/lookup/collection?permission=" + permission).then(function(res) {
                    self.items = res.data;
                    Vue.set(this,'changesMade',true);
            });
        },
        
        _addResourceToCollection : function(collectionId){
        	console.log("Adding resource to collection "+collectionId);
            var self=this;
            var data =  {
                "resourceId":self.resourceId,
                "collectionId":collectionId,
                "addAsManagedResource":this.managedResource
            }
            
            axios.post('/api/collection/addtocollection', Qs.stringify(data)).then(function(){
                self._getCollectionsForResource();
                self._resetForm();
            }
            ).catch(function(res){
                console.log("An error occurred when adding to collection"); 
                console.log(res);
            });
            
        },
        
        _getCollectionsForResource : function(){
        	console.log("Getting all collections for the resource "+this.resourceId);
            var self = this;
            var url = "/api/collection/resourcecollections?resourceId="+this.resourceId;
            
            console.log("Calling "+url);

            var called = false;
            axios.get(url).then(function(response) {
            	console.log("response is:");
            	console.debug(response.data);
            	called = true;
            	console.log("Managed collections: "+response.data.managed.length);
            	console.log("Unmanaged collections: "+response.data.unmanaged.length);
                Vue.set(self,'collections',response.data);
            }).catch(function(error){
                console.error("An error ocurred getting a list of collections for this resource");
                console.error(error);
            });
            
            console.log("This was called? ", called);
            
            
        },
        
        _removeResourceFromCollection: function(collectionId,section){
        	console.log("removing collection id"+collectionId);
        	Vue.set(this,"changesMade",true);
        		var data =  {
                    resourceId:this.resourceId,
                    collectionId:collectionId,
                    type:section
                }
        		
                var self = this;
                axios.post('/api/collection/removefromcollection', Qs.stringify(data)).then(function(){
                 self._getCollectionsForResource();
                }).catch(function(error){
                    console.error("couldn't remove item from collection");
                    console.debug(error);
                });
        },
            
        _processCollectionRemovals : function(){
            var vapp = this;
        	$.each(this.managedCollectionsToRemove, function(idx, val){
            	console.log("Removing collection "+val+" from managed");
            	vapp._removeResourceFromCollection(val, 'MANAGED');
            });
            
            $.each(this.unmanagedCollectionsToRemove, function(idx, val){
            	console.log("Removing collection "+val+" from unmanaged");
            	vapp._removeResourceFromCollection(val, 'UNMANAGED');
            });
        },
        
        _disableSubmitButton: function(){
        	$("#modal .btn-primary").addClass("disabled");
        	$("#modal .btn-primary").prop("disabled",true);
        },
        
        _enableSubmitButton: function(){ 
        	$("#modal .btn-primary").removeClass("disabled");
        	$("#modal .btn-primary").prop("disabled",false);
        },
        
        /**
         * "save results to collection" functions
         */
        _addResultsToCollection: function(collectionId){

        	var url = $(this.$el).data("url");
        	var vapp = this;

        	var progress = $("#upload-progress");
        	var form 	 = $("#upload-form");
        	var progressBar = $("#upload-progress-status");
        	var progressMessage = $("#progress-message");
        	var title  = $("#progress-title");
        	this.lastSavedCollectionId = collectionId;
        	
        	title.show();
        	title.text("Saving Results To Collection '"+$('#collection-list').text()+"'");
        	
        	axios.post("/api/search/"+url+"&collectionId="+collectionId).then(function(res) {
            	progress.show();
            	progressMessage.show();
            	form.hide();
        		console.log("Finished adding!!");
        		vapp.updateProgressBar(collectionId);
        	});
        },
        
        _resetCollectionSelectionState : function(){
        	var progress = $("#upload-progress");
        	var progressMessage = $("#progress-message");
        	var form 	 = $("#upload-form");
            var $select = $('#collection-list').selectize();
            var title  = $("#progress-title");
            
            var vapp = this;
            
            $select[0].selectize.clear();
            vapp._enableSubmitButton();
        	progress.hide();
        	progressMessage.hide();
        	title.hide();
        	form.show();
        },
        
        
       
        /**
         * Exposed methods
         */ 
        removeResourceFromCollection: function(collection,section){
            Vue.set(this,"changesMade",true);
            	console.log("Pending collection removal from "+collection.id+" - "+section);
            	if(section=='MANAGED'){
            		this.managedCollectionsToRemove.push(collection.id);
            		this._arrayRemove(this.collections.managed, collection);
            	}
            	else {
            		this.unmanagedCollectionsToRemove.push(collection.id);
               		this._arrayRemove(this.collections.unmanaged, collection);
            	}
        },
        
        addToCollection:function(){
            var vapp = this;
            if(this.selectedCollection==""){
                $("#addToNew").popover('show');
                setTimeout(function () {
                       $('#addToNew').popover('hide');
                   }, 2000);
              }

            else if (isNaN(this.selectedCollection)) {
                	console.log("Creating a new collection");
                	console.log("Disabling submit button");
                    // post to create a new collection.
                    var data = {
                        collectionName: this.selectedCollection,
                        collectionDescription : this.newCollectionDescription
                    }
                	
                    // On success, add the resource.
                    axios.post('/api/collection/newcollection',Qs.stringify(data)).then(function(res){
                            console.log("New Collection added");
                            var id = res.data.id;
                            console.debug("new collection id is "+id);
                            vapp._addResourceToCollection(id);
                            vapp._resetForm();
                        }
                    );
                } else if(this.selectedCollection < 1){
                    // This should change the background color to red and
					// invalidate the box.
                    $("#collection-list").addClass("invalid-feedback");
                    
                    
                    console.debug("Adding popover");
                    // Change this to popovers.
                    $("#addToExisting").popover('show');
                     setTimeout(function () {
                            $('#addToExisting').popover('hide');
                        }, 2000);
                }
                else { 
                   vapp._addResourceToCollection(this.selectedCollection);
                   vapp._getCollectionsForResource();
                }
        },
        
        //Determines if the list should show the grant permissions checkbox. 
        showGrant: function(){
           var index = $('#collection-list').prop('selectedIndex');
           if(index > 0 ){
            this.showPermission = this.items[index-1].owned==true;
           }
           else {
            this.showPermission = false;
           }
        },
        
        cancelAddToCollectionChanges: function(){
        	console.log("Cancelling Add To Collection.");
        	var vapp = this;
        	vapp._resetForm();
        	vapp._getCollectionsForResource();
        },
        
        saveAddToCollectionChanges : function(){
        	var vapp = this;
        	vapp.addToCollection();
        	vapp._processCollectionRemovals();
        	vapp._resetForm();
        },
        
        
        /**
         * SAVE RESULTS TO COLLECTION FUNCTIONS
         */
        saveResultsToCollection: function(){
        	var vapp = this;
        	if(this.selectedCollection==""){
                $("#addToNew").popover('show');
                setTimeout(function () {
                       $('#addToNew').popover('hide');
                   }, 2000);
            }

            else if (isNaN(this.selectedCollection)) {
            		vapp._disableSubmitButton();
                    
            		// post to create a new collection.
                    var data = {
                        collectionName: this.selectedCollection,
                        collectionDescription : this.newCollectionDescription
                    }
                    
                    // On success, add the resource.
                    axios.post('/api/collection/newcollection',Qs.stringify(data)).then(function(res){
                            console.log("New Collection added");
                            var id = res.data.id;
                            console.debug("new collection id is "+id);
                            vapp._addResultsToCollection(id);
                        }
                    );
                }
            else { 
            	vapp._disableSubmitButton();
                vapp._addResultsToCollection(this.selectedCollection);
             }
        },
        
        
        
        updateProgressBar: function(collectionId){
            var vapp = this;
            
        	axios.get('/api/search/checkstatus?collectionId='+collectionId).
        	then(function(res){
        	    var percentComplete = parseInt(res.data.percentComplete);
        	    var message = res.data.message;
        	    
        	    var progressBar 	= $("#upload-progress-status");
        	    var progressMessage = $("#progress-message");
        	    console.log("progress is "+percentComplete+"%");
        	    
        	    if(message==null){message = "";}
        	    
        	    progressMessage.text(message)
        	    
        	    progressBar.css("width", percentComplete + "%")
        	      .attr("aria-valuenow", percentComplete)
        	      .text(percentComplete + "% Complete");
        	    
        	    if(percentComplete<100){
        	    	 setTimeout(function(){
                        vapp.updateProgressBar(collectionId);
                     }, 1000);
        	    }
        	    else {
        	    	console.log("Progress is 100%");
        	    	//display the complete message.
        	    	var collection =  $('#collection-list').text();
        	    	$("#progress-complete-text").text("Results successfully saved to "+collection);
        	    	$("#progress-complete-alert").show();
        	    	vapp._resetCollectionSelectionState();
        	    }
        	    
        	}).catch(function(res){
                console.log("An error occurred when getting the progress status"); 
                console.log(res);
                vapp._resetCollectionSelectionState();
            });
        }
    }
    });
      return app2;
    
    
}

return {
    init: _init,
    collectionSelectizeOptions : _getSelectizeOpts,
    main : function() {
        var appId = '#add-resource-form';
        if ($(appId).length  >0) {
            TDAR.vuejs.collectionwidget.init(appId);
        }
    }
}
})(console, jQuery, window, Vue,axios);
