package org.joget.marketplace;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.datalist.service.DataListService;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.datalist.model.DataListColumnFormat;
import org.joget.plugin.base.PluginWebSupport;

public class TemplateDatalistFormatter extends DataListColumnFormatDefault implements PluginWebSupport {

    //Support i18n
    private final static String MESSAGE_PATH = "messages/TemplateDatalistFormatter";
    
    @Override
    public String getName() {
        return "Template Datalist Formatter";
    }

    @Override
    public String getVersion() {
        return "7.0.1";
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
        
        String content = "";
        
        /* Add required stylesheet */
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        if (request != null && request.getAttribute(getClassName()) == null) {
            //content += "<link rel=\"stylesheet\" href=\"" + request.getContextPath() + "/plugin/"+getClassName()+"/lib/w3-v4.css\" />\n";
            content += customHeader;
            content += "<style>" + css + "</style>";
            content += "<script>" + javascript + "</script>";
            request.setAttribute(getClassName(), true);
            
            //cant manipulate datalist to hide other columns, commenting out code
//            //set all other columns to hidden
//            DataListColumn[] cols = dataList.getColumns();
//            List<DataListColumn> colsNew = new ArrayList<DataListColumn>();
//
//            for(DataListColumn col : cols){
//                if(!column.getProperty("id").toString().equalsIgnoreCase(col.getProperty("id").toString())){
//                    col.setHidden(true);
//                    col.setProperty("hidden", "true");
//                    colsNew.add(col);
//                }else{
//                    colsNew.add(col);
//                }
//            }
//            dataList.setColumns((DataListColumn[]) colsNew.toArray(new DataListColumn[colsNew.size()]));

        }
        
        
        if( cacheEnabled){
            String cachedContent = TemplateDatalistCache.getCachedContent(datalistId + "-" + recordId);
            if(cachedContent != null){
                return content + cachedContent;
            }
        }
        
        //render template
        //template = StringUtil.stripHtmlRelaxed(template);
        if (/*template == null &&*/ getPropertyString("template") != null && !getPropertyString("template").isEmpty()) {
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
        
        //content += template;
        
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
            TemplateDatalistCache.setCachedContent(datalistId + "-" + recordId, content);
        }
        
        return content;
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
    
}
