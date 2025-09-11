package com.project2.To_do_list.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/list")
    public String showListPage() {
        return "list"; // this will render list.html from templates/
    }
}
