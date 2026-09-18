package org.joget.marketplace;

import java.util.Map;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormat;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.datalist.service.DataListService;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

public class TemplateDatalistFormatterUsageWrapper extends DataListColumnFormatDefault {

    //Support i18n
    private final static String MESSAGE_PATH = "messages/TemplateDatalistFormatterUsageWrapper";

    @Override
    public String getName() {
        return "Template Datalist Formatter Usage Wrapper";
    }

    @Override
    public String getVersion() {
        return Activator.VERSION;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.marketplace.TemplateDatalistFormatterUsageWrapper.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.marketplace.TemplateDatalistFormatterUsageWrapper.pluginLabel.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/TemplateDatalistFormatterUsageWrapper.json", null, true, MESSAGE_PATH);
    }

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        String recordId = (String) DataListService.evaluateColumnValueFromRow(row, "id");
        String datalistId = dataList.getId();

        //cache exists → template formatter will serve cached content; skip wrapped formatter
        if (recordId != null && !recordId.isEmpty()
                && TemplateDatalistCache.hasCachedContent(datalistId, recordId)) {
            return "";
        }

        //cache miss → execute the actual formatter and return its content (no cache write)
        String result = (value != null) ? value.toString() : "";
        Object formatterObj = getProperty("formatter");
        if (formatterObj instanceof Map) {
            Map fvMap = (Map) formatterObj;
            if (fvMap.get("className") != null && !fvMap.get("className").toString().isEmpty()) {
                try {
                    PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
                    DataListColumnFormat wrapped = (DataListColumnFormat) pluginManager.getPlugin(fvMap.get("className").toString());
                    if (wrapped != null) {
                        wrapped.setProperties((Map) fvMap.get("properties"));
                        result = wrapped.format(dataList, column, row, value);
                    }
                } catch (Exception e) {
                    LogUtil.error(getClassName(), e, "Error executing wrapped formatter");
                }
            }
        }
        return result;
    }
}
