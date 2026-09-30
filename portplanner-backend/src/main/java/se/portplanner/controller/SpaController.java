package se.portplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import se.portplanner.module.ModuleService;

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

    @RequestMapping(value = { "/", "/{path:(?!admin)[^\\.]*}", "/{path:(?!admin)[^\\.]*}/**" })
    public String member() {
        if (!modules.isEnabled("member-portal")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medlemsportalen är inte aktiverad");
        }
        return "forward:/index.html";
    }
}
