{
    widgetModel : '<div id="previewBody"><style></style><div id="templateDatalistContent"></div></div>',
    renderField : function() {
        var thisObj = this;
        var editor = "#" + thisObj.editorId;
        
        var css = "#previewBody{ border: 1px solid grey; width: 100%; min-height: 50px; height: -webkit-fill-available; display: inline-block; }\n";
        css += "#previewBody #templateDatalistContent{ font-size: revert; }\n";
        css += "#fieldSelectionWrapper{ padding:10px; border: 1px solid grey; }\n"
        css += ".fieldSelection { border: 1px dotted; padding: 3px; margin: 3px; display: inline-block; cursor: pointer;}";

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

        // DX9
        setTimeout(() => thisObj.refreshPreview(), 1000);

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
      var editorRoot = "#" + thisObj.editorId;

      // --- helpers to read editor content (Ace, CodeMirror, or textarea) ---
      function readFieldValue(rootSel, propName) {
        var $prop = $(rootSel).find("div.property-editor-property[property-name='" + propName + "']");

        // 1) Ace (DX8)
        var aceId = $prop.find(".ace_editor").attr("id");
        if (aceId && window.ace) {
          try {
            return ace.edit(aceId).getValue();
          } catch (e) {
            console.warn("Ace read failed for", propName, e);
          }
        }

        // 2) CodeMirror (DX9)
        var cmEl = $prop.find(".CodeMirror").get(0);
        if (cmEl && cmEl.CodeMirror) {
          try {
            return cmEl.CodeMirror.getValue();
          } catch (e) {
            console.warn("CodeMirror read failed for", propName, e);
          }
        }

        // 3) Fallback: textarea (in case editor not initialized yet)
        var ta = $prop.find("textarea").get(0);
        if (ta) return ta.value;

        // Nothing found
        console.warn("No editor found for property", propName);
        return "";
      }

      var css      = readFieldValue(editorRoot, "css");
      var template = readFieldValue(editorRoot, "template");

      // Update preview
      $(editorRoot).find("#previewBody style").html(css);
      $(editorRoot).find("#previewBody #templateDatalistContent").html(template);
    },
    insertFieldIntoTemplate: function(field){
        var thisObj = this;
        var editor = "#" + thisObj.editorId;
        var columnId = $(field).attr("attr-id");
        var columnName = $(field).attr("attr-name");

        // First try Ace(DX8)
        var aceEditorID = $(editor).find("div.property-editor-property[property-name='template']").find(".ace_editor").attr("id");
        if (aceEditorID) {
            templateEditor = ace.edit( aceEditorID );
            templateEditor.session.insert(templateEditor.getCursorPosition(), "{" + columnId + "::" + columnName + "}");
            return;
        }

        // Try CodeMirror(DX9)
            // Then try CodeMirror (DX9)
            var cmElement = $(editor)
                .find("div.property-editor-property[property-name='template']")
                .find(".CodeMirror")
                .get(0);

            if (cmElement && cmElement.CodeMirror) {
                var cm = cmElement.CodeMirror;
                cm.replaceSelection("{" + columnId + "::" + columnName + "}");
                cm.focus();
                return;
            }

            console.warn("No supported editor found for template field.");
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
