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
    private static final String SWAGGER_API_VERSION = "0.1";
    private static final String LICENCE_TEXT = "License";
    private static final String title = "Requirement Classifier API";
    private static final String description = "REST API that provides a requirements classifier. " +
            "The classifier is based on a Naive Bayes implemented using Apache Mahout (https://mahout.apache.org/)" +
            "\n\n" +
            "This service implements two different classifiers:\n\n" +
            "**· Binary classifier:** given a property value V for the *requirement_type* property, the binary classifier builds a " +
            "classifier to evaluate if a requirement is a member of the property value class (*requirement_type = 'V'*) or if it is not " +
            "(*requirement_type = 'Prose'*).\n\n" +
            "**· Multilabel classifier:** given the domain of property values {V}, the multilabel classifier is used for " +
            "properties that can have more than one value for each item.\n\n" +
            "**DATASET REQUIREMENTS**\n\nIn order to guarantee a minimum accuracy in the classification process, this classifier" +
            " requires a minimum number of requirements per each tag(i.e., per each requirement-type value). " +
            "Based on an analytical evaluation with real datasets, this value can be estimated with the following formula:\n\n" +
            " **|reqs-per-tag|** = (N - 1) x 40,\n\n" +
            "where N = nº of tags (i.e., classes or" +
            " requirement types) in the dataset.\n\n" +
            "*Note: this analytical evaluation has been tested with values N=2,3,4 and a minim accuracy=75%*";

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(title)
                .description(description)
                //.license(LICENCE_TEXT)
                //.license(SWAGGER_API_VERSION)
                .build();
    }

    @Bean
    public Docket classifier_api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .pathMapping("/")
                .select()
                /* Anything after upc will be included into my Swagger configuration */
                .paths(PathSelectors.regex("/upc.*"))
                .build();
    }
}
