package com.example.mahout.documentation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@Configuration
@EnableSwagger2
public class SwaggerConfig {
    private static final String TITLE = "Requirement Classifier API";
    private static final String DESCRIPTION = "REST API that provides a requirements classifier. " +
            "The classifier is based on a Naive Bayes implemented using Apache Mahout (https://mahout.apache.org/)" +
            "\n\n" +
            "This service implements two different classifiers:\n\n" +
            "**· Multiclass classifier:** given a property P for the *requirement_type* property which identifies a model, the multiclass classifier builds a " +
            "multiclass model for each possible value {V} of the property P in the dataset. Each requirement is related to a " +
            "single value V.\n\n" +
            "**· Multilabel classifier:** given the domain of property values {V}, the multilabel classifier is used for " +
            "properties that can have more than one value V for each item.\n\n";

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(TITLE)
                .description(DESCRIPTION)
                .build();
    }

    @Bean
    public Docket classifierApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .pathMapping("/")
                .select()
                /* Anything after upc will be included into my Swagger configuration */
                .paths(PathSelectors.regex("/upc.*"))
                .build();
    }
}
