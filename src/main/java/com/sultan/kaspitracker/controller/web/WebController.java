package com.sultan.kaspitracker.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/statements")
    public String statements() {
        return "statements";
    }

    @GetMapping("/statements/{id}")
    public String statementDetails(@PathVariable("id") Long id, Model model) {
        model.addAttribute("statementId", id);
        return "statement_details";
    }
}
