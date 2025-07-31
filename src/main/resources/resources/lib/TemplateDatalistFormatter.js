{
    widgetModel : '<div id="previewBody"><style></style><div id="templateDatalistContent"></div></div>',
    renderField : function() {
        var thisObj = this;
        var editor = "#" + thisObj.editorId;
        
        var css = "#previewBody{ border: 1px solid grey; width: 100%; min-height: 50px; height: -webkit-fill-available; display: inline-block; }\n";
        css += "#previewBody #templateDatalistContent{ font-size: revert; }\n";
        css += "#fieldSelectionWrapper{ padding:10px; border: 1px solid grey; }\n"
        css += ".fieldSelection { border: 1px dotted; padding: 3px; margin: 3px; display: inline-block; cursor: pointer;}";
        
        console.log("renderField");

        var fields = "Columns Selection<br/><div id=\"fieldSelectionWrapper\">";
        
        //get current column name
        var currentColumnName = $(editor).find("div.property-editor-property[property-name='name']").find("input").val();
        
        //get columns chosen so far
        Object.keys(DatalistBuilder.chosenColumns).forEach(key => {
            var fieldID = key;
            var fieldName = DatalistBuilder.chosenColumns[key].name;
            var fieldLabel = DatalistBuilder.chosenColumns[key].label;
            
            //cannot choose own column, will hit infinite loop
            if(currentColumnName != fieldName){
                fields += "<span attr-id=\""+fieldID+"\" attr-label=\""+fieldLabel+"\" attr-name=\""+fieldName+"\" class=\"fieldSelection\">" + fieldLabel + "</span>";
            }
        });
        
        fields += "</div>";
        
        return '<style>'+ css + '</style>Preview<br/>' + thisObj.widgetModel + '<br/>' + fields;
    },
    initScripting : function() {
        var thisObj = this;
        var editor = "#" + thisObj.editorId;
        
        $(editor).find("div.property-editor-property[property-name='css']").find("textarea").keyup(thisObj.throttle(function(){
           thisObj.refreshPreview(); 
        }));
        
        $(editor).find("div.property-editor-property[property-name='template']").find("textarea").keyup(thisObj.throttle(function(){
           thisObj.refreshPreview(); 
        }));
        
        $("#" + thisObj.id + "_input").find(".fieldSelection").on("click", function(){
            thisObj.insertFieldIntoTemplate(this);
        });
        
        setTimeout(thisObj.refreshPreview(), 1000);
    },
    getData : function(useDefault) {
        return "";
    },
    addOnValidation : function(data, errors, checkEncryption) {
        var thisObj = this;
    },
    /*custom methods*/
    refreshPreview: function(){
        var thisObj = this;
        var editor = "#" + thisObj.editorId;
        
        console.log("refreshPreview" + editor);

        var cssEditorID = $(editor).find("div.property-editor-property[property-name='css']").find(".ace_editor").attr("id");
        var templateEditorID = $(editor).find("div.property-editor-property[property-name='template']").find(".ace_editor").attr("id");
        
        var css = ace.edit(cssEditorID).getValue();
        var template = ace.edit(templateEditorID).getValue();
        
        $(editor).find("#previewBody style").html(css);
        $(editor).find("#previewBody #templateDatalistContent").html(template);
    },
    insertFieldIntoTemplate: function(field){
        var thisObj = this;
        var editor = "#" + thisObj.editorId;
        var columnId = $(field).attr("attr-id");
        var columnName = $(field).attr("attr-name");
        
        var templateEditorID = $(editor).find("div.property-editor-property[property-name='template']").find(".ace_editor").attr("id");
        templateEditor = ace.edit( templateEditorID );
        templateEditor.session.insert(templateEditor.getCursorPosition(), "{" + columnId + "::" + columnName + "}");
    },
    throttle: function(f, delay){
        var timer = null;
        return function(){
            var context = this, args = arguments;
            clearTimeout(timer);
            timer = window.setTimeout(function(){
                f.apply(context, args);
            },
            delay || 500);
        };
    }
}