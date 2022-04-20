/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groocraft.havelock.annotation;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Target({METHOD, TYPE, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)

/**
 * Annotation of Havelock that makes the annotated method/endpoint public (permitted for all in security). CORS and CSRF protection depends on
 * configuration. The annotation is also causing removal of any security item from swagger documentation for the particular operation/method. It
 * is handy when you want to configure global security item and exclude them for public endpoints.
 *
 * @author Majlanky
 * @see EnableHavelock
 */
@Inherited
@SecurityRequirements
public @interface Public {
}
