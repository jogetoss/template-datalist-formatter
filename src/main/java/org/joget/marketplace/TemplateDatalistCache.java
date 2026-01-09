package org.joget.marketplace;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.DynamicCacheElement;
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
        boolean isDx9 = false;
        try {
            Class dx9Class = Class.forName("org.joget.commons.util.DynamicCacheElement");
            isDx9 = true;
        } catch (Exception be) {
        }

        Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
        if (cache != null) {
            clearCachedContent(recordId);

            Long duration = 600L;
            String cacheKey = getCacheKey(recordId);

            if(isDx9){
                DynamicCacheElement element = new DynamicCacheElement(content, duration);
                cache.put(cacheKey, element);
            } else {
                Element element = new Element(cacheKey, content);
                cache.put(element);
            }
            LogUtil.info(TemplateDatalistCache.class.getName(), "setCachedContent: " + cacheKey + ", duration " + duration + "s");
        }
    }

    public static String getCachedContent(String recordId) {
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
                String cacheKey = getCacheKey(recordId);
                DynamicCacheElement element = (DynamicCacheElement) cache.get(cacheKey);
                if (element != null) {
                    content = (String) element.getValue();
                }
            }
        } else {
            Cache cache = (Cache) AppUtil.getApplicationContext().getBean("userviewMenuCache");
            if (cache != null) {
                String cacheKey = getCacheKey(recordId);
                Element element = cache.get(cacheKey);
                if (element != null) {
                    content = (String) element.getObjectValue();
                }
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
