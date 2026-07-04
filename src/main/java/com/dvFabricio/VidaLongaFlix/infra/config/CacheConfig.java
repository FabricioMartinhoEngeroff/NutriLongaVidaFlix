package com.dvFabricio.VidaLongaFlix.infra.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String VIDEOS           = "videos";
    public static final String MOST_WATCHED     = "most-watched";
    public static final String LEAST_WATCHED    = "least-watched";
    public static final String VIEWS_BY_CATEGORY = "views-by-category";
    public static final String CATEGORIES       = "categories";
}
