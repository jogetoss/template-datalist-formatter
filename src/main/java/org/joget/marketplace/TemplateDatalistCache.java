package org.joget.marketplace;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.DynamicDataSourceManager;
import org.joget.commons.util.LogUtil;

public class TemplateDatalistCache {

    public static final String CACHE_KEY_PREFIX = "TemplateDatalistFormatter";
    
    public static void clearCachedContent(String recordId) {
        Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
        if (cache != null) {
            cache.remove(getCacheKey(recordId));
        }
    }

    public static void setCachedContent(String recordId, String content) {
        Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
        if (cache != null) {    
            clearCachedContent(recordId);

            Integer duration = 600;
            String cacheKey = getCacheKey(recordId);
            
            Element element = new Element(cacheKey, content);
            if (duration != null && duration > 0) {
                element.setTimeToIdle(duration);
                element.setTimeToLive(duration);
            }
            cache.put(element);
            
            //if (LogUtil.isDebugEnabled(UserviewCache.class.getName())) {
            LogUtil.info(TemplateDatalistCache.class.getName(), "setCachedContent: " + cacheKey + ", duration " + duration + "s");
            //}
        }
    }

    public static String getCachedContent(String recordId) {
        String profile = DynamicDataSourceManager.getCurrentProfile();
        String content = null;
        Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
        if (cache != null) {
            String cacheKey = getCacheKey(recordId);
            Element element = cache.get(cacheKey);
            if (element != null) {
                content = (String) element.getObjectValue();
//                if (LogUtil.isDebugEnabled(TemplateDatalistCache.class.getName())) {
//                    LogUtil.info(TemplateDatalistCache.class.getName(), "getCachedContent: " + cacheKey);
//                    LogUtil.debug(TemplateDatalistCache.class.getName(), "getCachedContent: " + cacheKey);
//                }
            }
        }
        
        LogUtil.info(TemplateDatalistCache.class.getName(), "getCachedContent: " + recordId);
        
        return content;
    }

    public static String getCacheKey(String recordId) {
        String profile = DynamicDataSourceManager.getCurrentProfile();
        return CACHE_KEY_PREFIX + ":" + profile + ":" + recordId;
    }    
}
