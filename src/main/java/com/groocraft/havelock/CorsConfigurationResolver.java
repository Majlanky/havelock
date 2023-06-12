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

package com.groocraft.havelock;

import com.groocraft.havelock.annotation.EnableHavelock;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Map;
import java.util.Optional;

/**
 * Resolver which finds already defined {@link CorsConfigurationSource} or the one with configured ({@link EnableHavelock#corsConfigurationSource()}) name and
 * provides it as configuration for Havelock endpoints CORS protection.
 *
 * @author Majlanky
 */
@RequiredArgsConstructor
public class CorsConfigurationResolver {

    @NonNull
    private final ListableBeanFactory listableBeanFactory;
    @NonNull
    private final HavelockSetting havelockSetting;

    /**
     * @return {@link CorsConfigurationSource} which is named the same as {@link EnableHavelock#corsConfigurationSource()} configuration, the only one
     * {@link CorsConfigurationSource} in context or none. In case the no {@link EnableHavelock#corsConfigurationSource()} is configured and there is more
     * {@link CorsConfigurationSource} in context, exception is thrown.
     */
    public Optional<CorsConfigurationSource> getConfigurationSource() {
        String corsConfigurationSourceName = havelockSetting.corsConfigurationSource();
        if (StringUtils.hasText(corsConfigurationSourceName)) {
            return Optional.of(listableBeanFactory.getBean(corsConfigurationSourceName, CorsConfigurationSource.class));
        } else {
            Map<String, CorsConfigurationSource> sources = listableBeanFactory.getBeansOfType(CorsConfigurationSource.class);
            Assert.isTrue(sources.size() < 2,
                    "There is more CorsConfigurationSource but corsConfigurationSource on @EnableHavelock is not specified");
            return sources.values().stream().findAny();
        }
    }
}
