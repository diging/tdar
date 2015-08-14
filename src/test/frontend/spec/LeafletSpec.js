/* global describe, it, expect */
describe("LeafletSpec", function() {

    it("should work when we call initLeafletMaps", function() {
        var options = null;
        var expectedVal = null;

        //var result = TDAR.leaflet.adminUsageStats(options);
        expect(TDAR.leaflet).toExist(); //fixme: implement this test
    });

    it("initLeafletMaps:validSetup", function() {
        loadFixtures("leaflet/leaflet-view.html");
        //$(".leaflet-map").data("maxy","");
        TDAR.leaflet.initLeafletMaps();
        var options = null;
        var expectedVal = null;
        //var result = TDAR.leaflet.adminUsageStats(options);
        console.log(TDAR.leaflet.initialized());
        //https://www.npmjs.com/package/jasmine-jquery-matchers
        expect(TDAR.leaflet.initialized()).toBeGreaterThan(-1); //fixme: implement this test
        expect($(".leaflet-container")).toHaveLength(1);

    });


    it("initLeafletMaps:invalidSetup", function() {
        loadFixtures("leaflet/leaflet-view.html");
        $(".leaflet-map").data("maxy", "");
        TDAR.leaflet.initLeafletMaps();
        var options = null;
        var expectedVal = null;
        //https://www.npmjs.com/package/jasmine-jquery-matchers
        expect(TDAR.leaflet.initialized()).toBeLessThan(-1); //fixme: implement this test

        // assert that the map was properly initialized and has the rectangle
        // assert that the map was not proeprly initialized because it was missing a parameter
        // assert that an "edit" was initialzied
        // assert that a change to the map, updated hidden inputs and visa-versa (may not be possible)
        // assert that the degree translation on the hidden vs. the visible inputs updated properly
        // assert that a result map has items
    });

    it("initLeafletMaps:validResultsSetup", function() {
        loadFixtures("leaflet/leaflet-results.html");
        TDAR.leaflet.initResultsMaps();
        var options = null;
        var expectedVal = null;
        //var result = TDAR.leaflet.adminUsageStats(options);
        console.log(TDAR.leaflet.initialized());
        //https://www.npmjs.com/package/jasmine-jquery-matchers
        expect(TDAR.leaflet.initialized()).toBeGreaterThan(-1); //fixme: implement this test
        expect($(".leaflet-container")).toHaveLength(1);
        // 1 rectangle and 4 place-holders
        expect($(".leaflet-clickable")).toHaveLength(4);
        expect($("div.marker-cluster")).toHaveLength(2);
        expect($("img.leaflet-marker-icon")).toHaveLength(1);
        // $("img.leaflet-marker-icon").click();
        // var popup = $(".leaflet-popup-content");
        // expect(popup).toHaveLength(1);
        // console.log(popup);
    });

    it("initLeafletMaps:validEdit", function() {
        loadFixtures("leaflet/leaflet-edit.html");
        TDAR.leaflet.initEditableLeafletMaps();
        var options = null;
        var expectedVal = null;
        //var result = TDAR.leaflet.adminUsageStats(options);
        console.log(TDAR.leaflet.initialized());
        //https://www.npmjs.com/package/jasmine-jquery-matchers
        expect(TDAR.leaflet.initialized()).toBeGreaterThan(-1); //fixme: implement this test
        expect($(".leaflet-container")).toHaveLength(1);
        // 1 rectangle and 4 place-holders
        expect($(".leaflet-clickable")).toHaveLength(1);

    });
	

	it("edit:fireCreate",function(){
	        loadFixtures("leaflet/leaflet-edit.html");
	        TDAR.leaflet.initEditableLeafletMaps();

		var bounds = [[53.912257, 27.581640], [53.902257, 27.561640]];
		var $el = $("#large-map");
		var map = $(".mapdiv",$el).data('map');
		var rect = L.rectangle(bounds, {color: 'blue', weight: 1});
		var event = $.Event("draw:created");
		event.type = "draw:created";
		event.layer = rect;
		map.fireEvent(event.type,event);
	    expect(parseFloat($(".minx", $el).val())).toEqual(bounds[1][1]);
	    expect(parseFloat($(".miny", $el).val())).toEqual(bounds[1][0]);
	    expect(parseFloat($(".maxx", $el).val())).toEqual(bounds[0][1]);
	    expect(parseFloat($(".maxy", $el).val())).toEqual(bounds[0][0]);
	});


	// it("edit:fireDelete",function(){
	//         loadFixtures("leaflet/leaflet-edit.html");
	//
	// 	var $el = $("#large-map");
	// 	var bounds = [[53.912257, 27.581640], [53.902257, 27.561640]];
	//     $(".minx", $el).val(bounds[1][1]);
	//     $(".miny", $el).val(bounds[1][0]);
	//     $(".maxx", $el).val(bounds[0][1]);
	//     $(".maxy", $el).val(bounds[0][0]);
	//         TDAR.leaflet.initEditableLeafletMaps();
	// 	var map = $(".mapdiv",$el).data('map');
	// 	console.log("!--- " + map);
	// 	// var rect = L.rectangle(bounds, {color: 'blue', weight: 1});
	// 	map.eachLayer(function (layer) {
	// 		if (layer instanceof L.rectangle) {
	//  			    console.log(layer);
	// 		}
	// 	});
	// 	var event = $.Event("draw:deleted");
	// 	event.type = "draw:deleted";
	// 	// event.layer = rect;
	// 	map.fireEvent(event.type,event);
	// });
});
