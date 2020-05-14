package org.jf.opps.ui.swagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import springfox.documentation.spring.web.json.Json;

/**
 * Setting Spring to use GSON instead of Jackson causes a problem with Springfox.
 * Both SpringfoxJsonToGsonAdapter and WebConfigurer fix this. If GSON is removed, both of these
 * can be removed too.
 *
 * https://github.com/springfox/springfox/issues/2758
 */
@Configuration
public class WebConfigurer{

    @Bean
    public GsonHttpMessageConverter gsonHttpMessageConverter() {
        GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
        converter.setGson(gson());
        return converter;
    }

    private Gson gson() {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Json.class, new SpringfoxJsonToGsonAdapter());
        return builder.create();
    }
}