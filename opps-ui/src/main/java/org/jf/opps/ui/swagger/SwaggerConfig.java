package org.jf.opps.ui.swagger;

import com.google.gson.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;

/**
 * For auto gen documentation
 * @author JF
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .ignoredParameterTypes(JsonArray.class, JsonNull.class, JsonObject.class, JsonPrimitive.class, Number.class, JsonElement.class)
                .select()
                .apis(RequestHandlerSelectors.any())
//                .paths(PathSelectors.ant("**/api/**")) // look for anything past the /api/ URL path
                .paths(regex(".*/api/.*")) // look for anything past the /api/ URL path
                .build();
    }

    // https://stackoverflow.com/questions/47340976/swagger-ui-error-validation-when-deployed-a-spring-boot-application
    @Bean
    public UiConfiguration uiConfig() {
        return UiConfigurationBuilder.builder() //
                .displayRequestDuration( true ) //
                .validatorUrl( "" ) // Disable the validator to avoid "Error" at the bottom of the Swagger UI page
                .build();
    }
}
