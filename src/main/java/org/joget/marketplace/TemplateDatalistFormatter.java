package org.joget.marketplace;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.datalist.service.DataListService;
import org.joget.plugin.base.PluginManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.joget.apps.datalist.model.DataListColumnFormat;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;

public class TemplateDatalistFormatter extends DataListColumnFormatDefault implements PluginWebSupport{

    //Support i18n
    private final static String MESSAGE_PATH = "messages/TemplateDatalistFormatter";
    
    @Override
    public String getName() {
        return "Template Datalist Formatter";
    }

    @Override
    public String getVersion() {
        return Activator.VERSION;
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.marketplace.TemplateDatalistFormatter.pluginLabel.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        
        String template = (String) getPropertyString("template");
        String customHeader = (String) getPropertyString("customHeader");
        String css = (String) getPropertyString("css");
        String javascript = (String) getPropertyString("javascript");
        String recordId = (String) DataListService.evaluateColumnValueFromRow(row, "id");
        String datalistId = dataList.getId();
        
        boolean cacheEnabled = false;
        if(getPropertyString("cacheEnabled") != null && getPropertyString("cacheEnabled").equals("true")){
            cacheEnabled = true;
        }
        
        boolean richContent = false;
        if(getPropertyString("richContent") != null && getPropertyString("richContent").equals("true")){
            richContent = true;
        }
        
        //backward compatibilty, default to true
        if(getPropertyString("richContent") == null){
            richContent = true;
        }
        
        String header = "";
        
        /* Add required stylesheet, header, js*/
        String uniqueColumnIdentifier = datalistId + column.getProperty("id") + getClassName(); //to support using multple same datalists in the same page, and load the style/script/header only once.
         HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
         if (request != null && request.getAttribute(uniqueColumnIdentifier) == null) {
            header += customHeader;
            header += "<style>" + css + "</style>";
            header += "<script>" + javascript + "</script>";
            request.setAttribute(uniqueColumnIdentifier, true);
        }
         
        if(cacheEnabled){
            String cachedContent = TemplateDatalistCache.getCachedContent(datalistId + "-" + recordId);
            if(cachedContent != null){
                return header + cachedContent;
            }
        }
        
        //render template
        if ( getPropertyString("template") != null && !getPropertyString("template").isEmpty()) {
            template = AppUtil.processHashVariable(getPropertyString("template"), null, null, null);
            
            //support exact column matching
            //use column id
            Pattern pattern = Pattern.compile("\\{([a-zA-Z0-9_-]+)::([a-zA-Z0-9_-]+)\\}");
            Matcher descMatcher = pattern.matcher(template);
            while (descMatcher.find()) {
                String columnId = descMatcher.group(1);
                String columnName = descMatcher.group(2);
                //if(!columnId.isEmpty()){
                    //use column id
                    String temp[] = columnId.split("::");
                    columnId = temp[0];
                    template = template.replace("{" + columnId + "::" + columnName + "}", getBinderFormattedValue(dataList, row, columnId, columnName));
            }
            
            //for backward compatibility
            //use column name
            Pattern pattern2 = Pattern.compile("\\{([a-zA-Z0-9_-]+)\\}");
            Matcher descMatcher2 = pattern2.matcher(template);
            while (descMatcher2.find()) {
                String columnName = descMatcher2.group(1);
                    //use column name
                    //limitation, it will pick up first column with same column name, if there are multiple columns with same column name, it will not handle properly
                    template = template.replace("{" + columnName + "}", getBinderFormattedValue(dataList, row, null, columnName));
            }
        }
        
        String content = "";
        if(richContent){
            /* Generate the card body*/
            PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
            Map model = new HashMap();
            model.put("element", this);
            model.put("recordId", recordId);
            model.put("template", template);
            
            content += pluginManager.getPluginFreeMarkerTemplate(model, getClass().getName(), "/templates/TemplateDatalistFormatter.ftl", null);
        }else{
            content += template;
        }

        if(cacheEnabled){
            //add UTC timestamp to content
            content += "<!-- Cached at " + java.time.Instant.now() + " -->";
            TemplateDatalistCache.setCachedContent(datalistId + "-" + recordId, content);
        }
        
        return header + content;
    }
    
    protected String getBinderFormattedValue(DataList dataList, Object row, String columnId, String columnName){
        //when loaded fresh from list builder, return empty string
        if(dataList.getColumns() == null){
            return "";
        }
        
        String value = DataListService.evaluateColumnValueFromRow(row, columnName).toString();
        
        DataListColumn[] columns = dataList.getColumns();
        if(columnId != null){
            for (DataListColumn c : columns) {
                if(c.getProperty("id").equals(columnId)){
                    try{

                        Collection<DataListColumnFormat> formats = c.getFormats();
                        if (formats != null) {
                            for (DataListColumnFormat f : formats) {
                                if (f != null) {
                                    value = f.format(dataList, c, row, value);
                                    return value;
                                }else{
                                    return value;
                                }
                            }
                        }else{
                            return value;
                        }
                    }catch(Exception ex){

                    }
                }
            }
        }else{
            for (DataListColumn c : columns) {
                if(c.getName().equals(columnName)){
                    try{

                        Collection<DataListColumnFormat> formats = c.getFormats();
                        if (formats != null) {
                            for (DataListColumnFormat f : formats) {
                                if (f != null) {
                                    value = f.format(dataList, c, row, value);
                                    return value;
                                }else{
                                    return value;
                                }
                            }
                        }else{
                            return value;
                        }
                    }catch(Exception ex){

                    }
                }
            }
        }
        return "";        
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.marketplace.TemplateDatalistFormatter.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/TemplateDatalistFormatter.json", null, true, MESSAGE_PATH);
    }
    
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PluginManager pluginManager = (PluginManager)AppUtil.getApplicationContext().getBean("pluginManager");
        String script = AppUtil.readPluginResource(getClass().getName(), "/resources/lib/TemplateDatalistFormatter.js", null, false, MESSAGE_PATH);
        response.getWriter().write(script);
    }

    public void webService(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        PluginManager pluginManager = (PluginManager)AppUtil.getApplicationContext().getBean("pluginManager");
        String script = AppUtil.readPluginResource(getClass().getName(), "/resources/lib/TemplateDatalistFormatter.js", null, false, MESSAGE_PATH);
        response.getWriter().write(script);
    }
    
}
