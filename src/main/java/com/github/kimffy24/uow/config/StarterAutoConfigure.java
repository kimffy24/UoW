package com.github.kimffy24.uow.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kimffy24.uow.CommittingService;
import com.github.kimffy24.uow.ExecutingContextFactory;
import com.github.kimffy24.uow.SpringUoWMapperBinder;
import com.github.kimffy24.uow.repos.RepositoryHub;

@Configuration
@ConditionalOnProperty(prefix = "com.github.kimffy24.uow", value = "enabled", havingValue = "true")
@EnableConfigurationProperties(StarterServiceProperties.class)
public class StarterAutoConfigure {

    @Autowired
    private StarterServiceProperties properties;
    
    @AutoConfigureOrder(1)
    @Bean
    @ConditionalOnMissingBean
    CommittingService provideCommittingService(BeanFactory beanFactory){
		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory)beanFactory;
		return listableBeanFactory.createBean(CommittingService.class);
    }

    @AutoConfigureOrder(3)
    @Bean
    @ConditionalOnMissingBean
    SpringUoWMapperBinder provideSpringUoWMapperBinder(BeanFactory beanFactory){
		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory)beanFactory;
		return listableBeanFactory.createBean(SpringUoWMapperBinder.class);
    }

    @AutoConfigureOrder(4)
    @Bean
    @ConditionalOnMissingBean
    RepositoryHub provideRepositoryHub(BeanFactory beanFactory){
		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory)beanFactory;
		return listableBeanFactory.createBean(RepositoryHub.class);
    }

    @AutoConfigureOrder(5)
    @Bean
    @ConditionalOnMissingBean
    ExecutingContextFactory provideExecutingContextFactory(BeanFactory beanFactory){
		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory)beanFactory;
		return listableBeanFactory.createBean(ExecutingContextFactory.class);
    }
}
