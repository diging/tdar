<#import "/WEB-INF/content/admin/admin-common.ftl" as admin>
<head>
    <title>Build ${siteAcronym} Index</title>
    <script type="text/javascript">
        var $buildStatus, $buildLog, $progressbar;
        var production = ${production?string('true', 'false')};
        $(document).ready(function () {
            $buildStatus = $('#buildStatus');
            $buildLog = $('#buildLog');
            $progressbar = $('#progressbar');

            $progressbar.progressbar({value: 0});
            $("#idxBtn").click(function () {
                        var confirmed = true;
                        if (production) {
                            confirmed = confirm("You are in production.  are you sure you want to do this?");
                        }

                        if (confirmed) {
                            this.disabled = true;
                            $buildStatus.empty().append("Building Index...");
                            setTimeout(startIndex, 6000);
                        }
                    });
        });

        function startIndex() {

            var url = "<@s.url value="buildIndex"/>?userId=${authenticatedUser.id?c}&";
            var indx = 0;
            $('input[type=checkbox]:checked').each(function () {
                url += "&indexesToRebuild[" + indx + "]=" + $(this).val();
                indx++;
            });
            $.ajax(url,{async:true}).done(function (data) {
                _checkStatus(data);
            });
        }

        function _checkStatus(data) {
            if (data.percentDone == -1) {
                $("#idxBtn").removeAttr('disabled');
            } else if (data.percentDone != 100) {
                var timeString = (new Date()).toLocaleTimeString();
                $progressbar.progressbar("option", "value", data.percentDone);
                document.title = "(" + data.percentDone + "%) Build ${siteAcronym} Index";
                if (data.errorHtml) {
                    $('#errors').show();
                    $('#errors').html(data.errorHtml)
                }
                if ($buildStatus.text() != data.phase) {
                    $buildLog.prepend("<br>[" + timeString + "] " + $buildStatus.text().replace("Current Status: ", ""));
                    $buildStatus.empty().append(data.phase);
                }
            } else {
                $progressbar.progressbar("option", "value", 100);
                document.title = "Indexing complete.";
                $buildStatus.empty().append("<span id='spanDone'>Done.</span>");
                $("#idxBtn").removeAttr('disabled');
            }
            setTimeout(updateProgress, 200);
        }        
        function updateProgress() {
        console.log("updateProgress");
            var url = "<@s.url value="checkstatus"/>?userId=${authenticatedUser.id?c}&";
            $.post(url, function (data) {
                _checkStatus(data);
            });
        }

    </script>
</head>
<body>
<@admin.header/>
<h1>Rebuild Search Indexes</h1>
<@s.checkboxlist id="sources" name='indexesToRebuild' list='allSources'  label="what to reindex"/>
<div>
    <div id="progressbar"></div>
    <br/>
    <#if reindexing!false>
        <div class="alert">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
            <strong>REINDEX IN PROGRESS!</strong> You are already reindexing on this server.
            <script>var disable = true;</script>
        </div>
    </#if>
        <script>
        $(document).ready(function () {
            if (typeof disable !== 'undefined' && disable) {
                $("#idxBtn").attr('disabled','true');
            }
            updateProgress();
        });
        </script>
<#if production>
    <div class="alert">
        <button type="button" class="close" data-dismiss="alert">&times;</button>
        <strong>Hey!</strong> You are on the production server. Please do be careful.
    </div>
    <input type='button' value='Build Index on core.tdar.org' id='idxBtn' class="btn btn-danger"/>
<#else>
    <input type='button' value='Build Index' id='idxBtn' class="btn"/>
</#if>
    <br/>
    <br/>
    <span id="buildStatus"></span>

    <div id="buildLog" style="height:20em; border: 0px dotted #495251; overflow:auto; font-family:sans-serif"></div>
</div>
</body>
