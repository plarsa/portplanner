package se.portplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = { "/admin", "/admin/{path:[^\\.]*}", "/admin/{path:[^\\.]*}/**" })
    public String admin() {
        return "forward:/admin/index.html";
    }

    @RequestMapping(value = { "/", "/{path:(?!admin)[^\\.]*}", "/{path:(?!admin)[^\\.]*}/**" })
    public String member() {
        return "forward:/index.html";
    }
}
