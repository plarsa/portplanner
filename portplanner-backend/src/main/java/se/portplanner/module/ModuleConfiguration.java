package se.portplanner.module;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({ModulesProperties.class, StorageProperties.class})
public class ModuleConfiguration implements WebMvcConfigurer {

    private final ModuleService moduleService;

    public ModuleConfiguration(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ModuleAccessInterceptor(moduleService)).addPathPatterns("/api/**");
    }
}
