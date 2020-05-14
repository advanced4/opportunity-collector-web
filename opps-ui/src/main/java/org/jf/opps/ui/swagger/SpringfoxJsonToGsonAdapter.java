package org.jf.opps.ui.swagger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import springfox.documentation.spring.web.json.Json;

import java.lang.reflect.Type;

/**
 * Setting Spring to use GSON instead of Jackson causes a problem with Springfox.
 * Both SpringfoxJsonToGsonAdapter and WebConfigurer fix this. If GSON is removed, both of these
 * can be removed too.
 *
 * https://github.com/springfox/springfox/issues/2758
 */
public class SpringfoxJsonToGsonAdapter implements JsonSerializer<Json>{

    @Override
    public JsonElement serialize(Json json, Type type, JsonSerializationContext context) {
        final JsonParser parser = new JsonParser();
        return parser.parse(json.value());
    }

}