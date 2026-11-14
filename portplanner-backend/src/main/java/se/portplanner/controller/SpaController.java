package se.portplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import se.portplanner.module.ModuleService;

/**
 * Forwards deep links into the two single-page applications so that the browser
 * history routes keep working on a page reload. The landing page at "/" is a plain
 * static resource and is served by Spring's welcome-page handling.
 */
@Controller
public class SpaController {

    private final ModuleService modules;

    public SpaController(ModuleService modules) {
        this.modules = modules;
    }

    @RequestMapping(value = { "/admin", "/admin/{path:[^\\.]*}", "/admin/{path:[^\\.]*}/**" })
    public String admin() {
        return "forward:/admin/index.html";
    }

    @RequestMapping(value = { "/member", "/member/{path:[^\\.]*}", "/member/{path:[^\\.]*}/**" })
    public String member() {
        if (!modules.isEnabled("member-portal")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The member portal is not enabled");
        }
        return "forward:/member/index.html";
    }
}
