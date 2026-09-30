package se.portplanner.module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

public class ModuleAccessInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final ModuleService modules;

    public ModuleAccessInterceptor(ModuleService modules) {
        this.modules = modules;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var auto = READ_METHODS.contains(request.getMethod()) ? AccessLevel.READ : AccessLevel.WRITE;

        var onClass = AnnotatedElementUtils.findMergedAnnotation(hm.getBeanType(), RequiresModule.class);
        var onMethod = AnnotatedElementUtils.findMergedAnnotation(hm.getMethod(), RequiresModule.class);
        for (var ann : new RequiresModule[] {onClass, onMethod}) {
            if (ann != null) {
                modules.requireAccess(ann.value(), AccessLevel.max(auto, ann.level()), auth);
            }
        }
        return true;
    }
}
