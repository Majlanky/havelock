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

package com.groocraft.havelock.actuator;

import com.groocraft.havelock.PublicPathResolver;
import com.groocraft.havelock.annotation.EnableHavelock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Set;

/**
 * Actuator endpoint implementation which provides result of {@link PublicPathResolver} hence all paths that are considered as public.
 * Its presence can be managed by {@link EnableHavelock#publicPathsEndpoint()}
 *
 * @author Majlanky
 */
@Endpoint(id = "publicpaths")
@RequiredArgsConstructor
public class PublicPathEndpoint {

    private final PublicPathResolver publicPathResolver;

    @ReadOperation
    public Set<String> publicPaths() {
        return publicPathResolver.getPublicPaths();
    }

}
