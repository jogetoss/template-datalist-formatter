package org.joget.marketplace;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.DynamicCacheElement;
import org.joget.commons.util.DynamicDataSourceManager;
import org.joget.commons.util.LogUtil;

public class TemplateDatalistCache {

    public static final String CACHE_KEY_PREFIX = "TemplateDatalistFormatter";
    
    public static void clearCachedContent(String datalistId, String recordId) {
        Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
        if (cache != null) {
            cache.remove(getCacheKey(datalistId, recordId));
        }
    }

    public static void clearCachedContent(String datalistId, String recordId, String formDefId, String field) {
        clearCachedContent(datalistId, recordId, formDefId, field, false);
    }

    public static void clearCachedContent(String datalistId, String recordId, String formDefId, String field, boolean debugMode) {

        debug(debugMode, "clearCachedContent: " + datalistId + "-" + recordId + " from cache");

        clearCachedContent(datalistId, recordId);

        //optionally blank the stored form field so content regenerates
        if (formDefId != null && !formDefId.isEmpty()
                && field != null && !field.isEmpty()) {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            if (appDef != null) {
                storeContentToForm(appDef, formDefId, field, recordId, "", debugMode);
                debug(debugMode, "clearCachedContent: " + datalistId + "-" + recordId + " from form data");
            }
        }
    }

    public static void setCachedContent(String datalistId, String recordId, String content) {
        setCachedContent(datalistId, recordId, content, false);
    }

    public static void setCachedContent(String datalistId, String recordId, String content, boolean debugMode) {
        boolean isDx9 = false;
        try {
            Class dx9Class = Class.forName("org.joget.commons.util.DynamicCacheElement");
            isDx9 = true;
        } catch (Exception be) {
        }

        Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
        if (cache != null) {
            clearCachedContent(datalistId, recordId);

            Long duration = 600L;
            String cacheKey = getCacheKey(datalistId, recordId);

            if(isDx9){
                DynamicCacheElement element = new DynamicCacheElement(content, duration);
                cache.put(cacheKey, element);
            } else {
                Element element = new Element(cacheKey, content);
                cache.put(element);
            }
            debug(debugMode, "setCachedContent: " + cacheKey + ", duration " + duration + "s");
        }
    }

    public static String getCachedContent(String datalistId, String recordId) {
        return getCachedContent(datalistId, recordId, null, null);
    }

    public static String getCachedContent(String datalistId, String recordId, String formDefId, String field) {
        return getCachedContent(datalistId, recordId, formDefId, field, false);
    }

    public static String getCachedContent(String datalistId, String recordId, String formDefId, String field, boolean debugMode) {
        debug(debugMode, "getCachedContent: " + datalistId + "-" + recordId);

        boolean isDx9 = false;
        try {
            Class dx9Class = Class.forName("org.joget.commons.util.DynamicCacheElement");
            isDx9 = true;
        } catch (Exception be) {
        }

        String profile = DynamicDataSourceManager.getCurrentProfile();
        String content = null;

        if (isDx9) {
            javax.cache.Cache cache = (javax.cache.Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
            if (cache != null) {
                String cacheKey = getCacheKey(datalistId, recordId);
                DynamicCacheElement element = (DynamicCacheElement) cache.get(cacheKey);
                if (element != null) {
                    content = (String) element.getValue();
                    debug(debugMode, "getCachedContent: " + datalistId + "-" + recordId + " from cache");
                }
            }
        } else {
            Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
            if (cache != null) {
                String cacheKey = getCacheKey(datalistId, recordId);
                Element element = cache.get(cacheKey);
                if (element != null) {
                    content = (String) element.getObjectValue();
                    debug(debugMode, "getCachedContent: " + datalistId + "-" + recordId + " from cache");
                }
            }
        }

        //optional fallback: fetch from stored form data when key is no longer in cache
        if (content == null && formDefId != null && !formDefId.isEmpty()
                && field != null && !field.isEmpty()) {
            content = fetchContentFromForm(formDefId, field, recordId);
            if (content != null) {
                //repopulate the cache so subsequent reads are served from memory
                debug(debugMode, "getCachedContent: " + datalistId + "-" + recordId + " from form data");
                setCachedContent(datalistId, recordId, content, debugMode);
            }
        }

        return content;
    }

    public static String fetchContentFromForm(String formDefId, String field, String recordId) {
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            if (appDef == null) {
                return null;
            }
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            FormRowSet rows = appService.loadFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, recordId);
            if (rows != null && !rows.isEmpty()) {
                String content = rows.get(0).getProperty(field);
                if (content != null && !content.isEmpty()) {
                    return content;
                }
            }
        } catch (Exception e) {
            LogUtil.error(TemplateDatalistCache.class.getName(), e, "Failed to fetch content from form " + formDefId + " field " + field + " record " + recordId);
        }
        return null;
    }

    public static void storeContentToForm(AppDefinition appDef, String formDefId, String field, String recordId, String content, boolean debugMode) {
        if(content.length() == 0){
            debug(debugMode, "storeContentToForm: " + formDefId + "-" + field + " - " + recordId + " - clearing content");
        }else{
            debug(debugMode, "storeContentToForm: " + formDefId + "-" + field + " - " + recordId + " - storing content of length " + content.length());
        }

        try {
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            String appId = appDef.getAppId();
            String appVersion = appDef.getVersion().toString();

            FormRowSet rows = appService.loadFormData(appId, appVersion, formDefId, recordId);
            FormRow row = (rows != null && !rows.isEmpty()) ? rows.get(0) : new FormRow();
            row.setId(recordId);
            row.setProperty(field, content);

            FormRowSet toSave = new FormRowSet();
            toSave.add(row);
            appService.storeFormData(appId, appVersion, formDefId, toSave, recordId);
        } catch (Exception e) {
            LogUtil.error(TemplateDatalistCache.class.getName(), e, "Failed to store generated content to form " + formDefId + " field " + field + " record " + recordId);
        }
    }

    public static boolean hasCachedContent(String datalistId, String recordId) {
        boolean isDx9 = false;
        try {
            Class dx9Class = Class.forName("org.joget.commons.util.DynamicCacheElement");
            isDx9 = true;
        } catch (Exception be) {
        }

        String cacheKey = getCacheKey(datalistId, recordId);
        if (isDx9) {
            javax.cache.Cache cache = (javax.cache.Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
            return cache != null && cache.containsKey(cacheKey);
        } else {
            Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
            return cache != null && cache.get(cacheKey) != null;
        }
    }

    private static void debug(boolean debugMode, String message) {
        if (debugMode) {
            LogUtil.info(TemplateDatalistCache.class.getName(), message);
        }
    }

    public static String getCacheKey(String datalistId, String recordId) {
        String profile = DynamicDataSourceManager.getCurrentProfile();
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appId = (appDef != null) ? appDef.getAppId() : "";
        return CACHE_KEY_PREFIX + ":" + profile + ":" + appId + ":" + datalistId + ":" + recordId;
    }
}
