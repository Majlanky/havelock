/*
 * Copyright 2023 the original author or authors.
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

package com.groocraft.havelock.aot;

import com.groocraft.havelock.HavelockSetting;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Executable;

public class HavelockSettingAotProcessor implements BeanRegistrationAotProcessor {
    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
        if (HavelockSetting.class.equals(registeredBean.getBeanClass())) {
            return BeanRegistrationAotContribution.withCustomCodeFragments(cf ->
                    new HavelockSettingBeanRegistrationCodeFragments(cf, registeredBean));
        }
        return null;
    }

    protected static class HavelockSettingBeanRegistrationCodeFragments
            extends BeanRegistrationCodeFragmentsDecorator {

        private final RegisteredBean registeredBean;

        protected HavelockSettingBeanRegistrationCodeFragments(BeanRegistrationCodeFragments delegate, RegisteredBean registeredBean) {
            super(delegate);
            this.registeredBean = registeredBean;
        }

        @Override
        public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
                                                      BeanRegistrationCode beanRegistrationCode, Executable constructorOrFactoryMethod,
                                                      boolean allowDirectSupplierShortcut) {
            GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", method -> {
                Class<?> beanClass = this.registeredBean.getBeanClass();
                ConstructorArgumentValues constructorArgumentValues =
                        registeredBean.getBeanFactory().getBeanDefinition(registeredBean.getBeanName()).getConstructorArgumentValues();
                Boolean cors = (Boolean)constructorArgumentValues.getIndexedArgumentValue(0, Boolean.class).getValue();
                Boolean csrf = (Boolean)constructorArgumentValues.getIndexedArgumentValue(1, Boolean.class).getValue();
                String corsCustomizerName = (String)constructorArgumentValues.getIndexedArgumentValue(2, String.class).getValue();
                method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName())
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .returns(beanClass)
                        .addParameter(RegisteredBean.class, "registeredBean")
                        .addStatement("return new $T($L, $L, $S)", beanClass, cors, csrf, corsCustomizerName);
            });
            return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, beanRegistrationCode.getClassName(),
                    generatedMethod.getName());
        }


    }


}
