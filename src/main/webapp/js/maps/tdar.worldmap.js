TDAR.worldmap = (function(console, $, ctx) {
    "use strict";

    var statesData;
    var hlayer;
    var geodata = {};
    var map;
	var $mapDiv;
    var OUTLINE = "#777777";
    // note no # (leaflet doesn't use jquery selectors)
    var mapId = "worldmap";

    var myStyle = {
        "color": OUTLINE,
        "weight": 1,
        "fillOpacity": 1
    }

    /**
     * Look for embedded json in the specified container  and return the parsed result.  Embedded json should be
     * tagged with a boolean attribute named 'data-mapdata'.  If no embedded json found, method returns null.
     *
     * @param containerElem
     * @private
     */
    function _getMapdata(containerElem) {
        var json = $(containerElem).find('script[data-mapdata]').text() || 'null';
        return JSON.parse(json);
    }

    function _initWorldMap(mapId_) {
        if (mapId_ != undefined) {
            mapId = mapId_;
        }
        if (document.getElementById(mapId) == undefined) {
            return;
        }
		$mapDiv = $("#"+mapId);
        var mapdata = _getMapdata($mapDiv.parent());

        map = L.map(mapId,{
        // config for leaflet.sleep
        sleep: false,
        // time(ms) for the map to fall asleep upon mouseout
        sleepTime: 750,
        // time(ms) until map wakes on mouseover
        wakeTime: 750,
        // defines whether or not the user is prompted oh how to wake map
        sleepNote: false,
        // should hovering wake the map? (clicking always will)
        hoverToWake: true
        });
        _resetView();
        for (var i = 0; i < mapdata.length; i++) {
            if (mapdata[i].resourceType == undefined) {
                geodata[mapdata[i].code] = mapdata[i].count;
            }
        }

        // load map data
        //FIXME: consider embedding data for faster rendering
        $.getJSON("/js/maps/world.json", function(data) {
            hlayer = new L.GeoJSON(data, {
                style: myStyle,
                onEachFeature: function(feature, layer_) {
                    layer_.on({
                        click: _clickWorldMapLayer,
                        mouseover: _highlightFeature,
                        mouseout: _resetHighlight
                    });
                }
            }).addTo(map);

            hlayer.eachLayer(function(layer) {
                var color = _getColor(geodata[layer.feature.id]);
                if (typeof layer._path != 'undefined') {
                    layer._path.id = layer.feature.id;
                    _redrawLayer(layer, color,1);
                } else {
                    layer.eachLayer(function(layer2) {
                        layer2._path.id = layer.feature.id;
                        _redrawLayer(layer2, color,1);
                    });
                }

            });
        });
        map.on('click', _resetView);
        
        var legend = L.control({position: 'bottomright'});

        legend.onAdd = function (map) {

            var div = L.DomUtil.create('div', 'info legend');
            $(div).append("<div id='data'></div>");

            var grades = [0, 1, 2, 5, 10,  20, 100, 1000];

            // loop through our density intervals and generate a label with a colored square for each interval
            for (var i = 0; i < grades.length; i++) {
                div.innerHTML +=
                '<i style="width:10px;height:10px;display:inline-block;background:' + _getColor(grades[i] + 1) + '">&nbsp;</i> ';
            }
            return div;
        };

        legend.addTo(map);
        return map;
    }
    
    function _redrawLayer(layer, fillColor, opacity, id) {
        layer.options.fill = fillColor;
        layer.options.opacity = 1;
        layer.options.color = OUTLINE;
        layer._path.style.fill = fillColor;
        layer._path.style.opacity = 1;
        layer.redraw();
    }

    var stateLayer = undefined;
    var overlay = false;

    function _clickWorldMapLayer(event) {
        var ly = event.target.feature.geometry.coordinates[0];
        if (stateLayer != undefined) {
            map.removeLayer(stateLayer);
        }
        
        if (event.target.feature.id == 'USA') {
            var usStyle = {
                "strokeColor": "#ff7800",
                "weight": .5,
                "fillOpacity": 0
            };
            stateLayer = new L.GeoJSON(statesData, {
                style: usStyle
            }).addTo(map);
            hlayer.setStyle({
                "fillOpacity": .1
            });
        }
		var $div = $("#mapgraphdata");
		if ($div.length == 0) {
			$div = $("<div id='mapgraphdata' style='left:"+$mapDiv.width()+"px;height:"+$mapDiv.height()+"px'></div>");
			$mapDiv.parent().append($div);
		}
		$div.html("<h5>" + event.target.feature.properties.name + "</h5><div id='mapgraphpie'></div>");
        var mapdata = _getMapdata($mapDiv.parent());
		var filter = mapdata.filter(function(d) {return d.code == event.target.feature.id});
		var data = [];
		filter.forEach(function(row){
			if (parseInt(row.count) && row.count > 0 && row.resourceType != undefined) {
				data.push([row.label, row.count]);				
			}
		});

		$(".mapcontainer").mouseleave(function(){
			$("#mapgraphdata").remove();
		});
		var obj = {
			bindto: '#mapgraphpie',
		    data: {
		        columns: data,
		        type : 'pie',
		    }			
		};
		var c3colors = $("#c3colors");
		if (c3colors.length > 0) {
			var c3colorsobj = JSON.parse(c3colors.html());
			obj.color = {};
			obj.color.pattern = c3colorsobj;
		}
		
		setTimeout(100,  c3.generate(obj));
        if (event.target.feature.id != 'RUS') {
            map.fitBounds(event.target.getBounds());
            overlay = true;
        }
    }

    function _resetView() {
        map.setView([44.505, -0.09], 1);
        overlay = false;
        if (hlayer != undefined) {
            hlayer.setStyle({
            "fillOpacity": 1
        });
        }
        map.eachLayer(function(l) {
            if (typeof l.redraw === "function") {
                l.redraw();
            }
        });
        if (stateLayer != undefined) {
            map.removeLayer(stateLayer);
        }
    }

    function _highlightFeature(e) {
        var layer = e.target;

        var cnt = 0;
        if (layer.feature.id != undefined) {
            if (geodata[layer.feature.id] != undefined) {
                cnt = geodata[layer.feature.id];
            }
        }

        $("#data").html(layer.feature.properties.name + ": " + cnt);
        if (overlay === true) {
            return false;
        }
        layer.setStyle({
            weight: 1,
            color: '#666',
            dashArray: '',
            fillOpacity: 0.7
        });
    }

    function _resetHighlight(e) {
        if (overlay === true) {
            return false;
        }

        var layer = e.target;
        hlayer.resetStyle(layer);
        $("#data").html("");
    }

    function _getColor(d) {
        d = parseInt(d);
        return d > 1000 ? '#800026' : d > 100 ? '#BD0026' : d > 20 ? '#E31A1C' : d > 10 ? '#FC4E2A' : d > 5 ? '#FD8D3C' : d > 2 ? '#FEB24C' : d > 1 ? '#FED976'
            : '#FFF';
    }

    return {
        initWorldMap: _initWorldMap
    }
})(console, jQuery, window);
$(function() {
    TDAR.worldmap.initWorldMap();
});