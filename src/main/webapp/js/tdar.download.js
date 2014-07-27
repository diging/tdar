/**
 * email-specific functionality
 */
(function(TDAR, $) {
    "use strict";

    var _register = function (url, versionId) {
        TDAR.common.registerDownload(url, versionId);
    };

    var _autoDownload = function (url, versionId) {
        _register(url, versionId);
        document.location = url;
    };

    /**
     * Scan for auto-download links and, if found, queue download to start in a few seconds.
     * @private
     */
    var _setup = function() {
        //look for link data-auto-download boolean attribute.  Terminate if none found.
        var $link = $("a[data-auto-download]").first();
        if($link.length === 0) return;

        //grap url, irf.id from link, set a timer to call _autodownload
        var url = $link[0].href;
        var versionId = $link.data("versionId");
        var DOWNLOAD_WAIT_SECONDS = 4;
        var id = setTimeout(function() {_autoDownload(url, versionId)}, DOWNLOAD_WAIT_SECONDS * 1000);
    
        //cancel auto-download if user clicks on link before timeout fires
        $link.click(function () {
            clearTimeout(id);
            _register(url, versionId);
            return true;
        });
    }

    TDAR.download = {
        setup : _setup
    };

})(TDAR, jQuery);
