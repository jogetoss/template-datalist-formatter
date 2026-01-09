package org.joget.marketplace;

import java.util.Map;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;

public class TemplateDatalistClearCacheTool extends DefaultApplicationPlugin {

    private final static String MESSAGE_PATH = "messages/TemplateDatalistClearCacheTool";

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("org.joget.marketplace.TemplateDatalistClearCacheTool.pluginLabel", getClassName(), MESSAGE_PATH);
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
        return AppPluginUtil.getMessage("org.joget.marketplace.TemplateDatalistClearCacheTool.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.marketplace.TemplateDatalistClearCacheTool.pluginLabel.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/TemplateDatalistClearCacheTool.json", null, true, MESSAGE_PATH);
    }

    @Override
    public Object execute(Map map) {
        String datalistId = (String) map.get("listId");
        String recordId = (String) map.get("recordId");

        TemplateDatalistCache.clearCachedContent(datalistId + "-" + recordId);
        return null;
    }
}