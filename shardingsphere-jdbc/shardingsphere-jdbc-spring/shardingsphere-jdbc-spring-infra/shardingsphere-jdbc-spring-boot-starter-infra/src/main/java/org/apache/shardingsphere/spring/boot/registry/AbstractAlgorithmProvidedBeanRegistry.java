/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.spring.boot.registry;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmPostProcessor;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.infra.spi.type.TypedSPI;
import org.apache.shardingsphere.infra.spi.type.TypedSPIRegistry;
import org.apache.shardingsphere.infra.yaml.config.algorithm.YamlShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.spring.boot.util.PropertyUtil;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract algorithm provided bean registry.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractAlgorithmProvidedBeanRegistry implements BeanDefinitionRegistryPostProcessor, BeanPostProcessor {
    
    private final Environment environment;
    
    @SuppressWarnings("all")
    protected void registerBean(final String preFix, final Class clazz, final BeanDefinitionRegistry registry) {
        Map<String, Object> paramMap = PropertyUtil.handle(environment, preFix, Map.class);
        Set<String> keySet = paramMap.keySet().stream().map(key -> key.substring(0, key.indexOf("."))).collect(Collectors.toSet());
        Map<String, YamlShardingSphereAlgorithmConfiguration> shardingAlgorithmMap = new LinkedHashMap<>();
        keySet.forEach(key -> {
            String type = environment.getProperty(preFix + key + ".type");
            Map<String, Object> propsMap = PropertyUtil.handle(environment, preFix + key + ".props", Map.class);
            YamlShardingSphereAlgorithmConfiguration configuration = new YamlShardingSphereAlgorithmConfiguration();
            configuration.setType(type);
            configuration.getProps().putAll(propsMap);
            shardingAlgorithmMap.put(key, configuration);
        });
        ShardingSphereServiceLoader.register(clazz);
        shardingAlgorithmMap.forEach((k, v) -> {
            TypedSPI typedSPI = TypedSPIRegistry.getRegisteredService(clazz, v.getType(), v.getProps());
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(typedSPI.getClass());
            builder.addPropertyValue("props", v.getProps());
            registry.registerBeanDefinition(k, builder.getBeanDefinition());
        });
    }
    
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory configurableListableBeanFactory) {
    }
    
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        if (bean instanceof ShardingSphereAlgorithmPostProcessor) {
            ((ShardingSphereAlgorithmPostProcessor) bean).init();
        }
        return bean;
    }
}
