/*
   Copyright 2017 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.ericsson.ei.config;

import com.ericsson.ei.controller.model.ParseInstanceInfoEI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;

@Configuration
public class SwaggerConfig {

    private static final String CONTACT_NAME = "Eiffel Intelligence Maintainers";
    private static final String CONTACT_URL = "https://github.com/eiffel-community/eiffel-intelligence";
    private static final String CONTACT_EMAIL = "eiffel-community@googlegroups.com";

    @Autowired
    ParseInstanceInfoEI parseInstanceInfoEI;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.ericsson.ei.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(metaData());
    }

    private ApiInfo metaData() {
        ApiInfo apiInfo = new ApiInfo(
                "Eiffel Intelligence REST API",
                "A real time data aggregation and analysis solution for Eiffel events.",
                parseInstanceInfoEI.getVersion(),
                "Terms of service",
                new Contact(CONTACT_NAME, CONTACT_URL, CONTACT_EMAIL),
               "Apache License Version 2.0",
               "https://www.apache.org/licenses/LICENSE-2.0",
               new ArrayList<>());
        return apiInfo;
    }
}