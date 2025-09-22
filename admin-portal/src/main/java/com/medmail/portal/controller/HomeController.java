package com.medmail.portal.controller; // or com.medmail.portal.web — keep consistent with your app

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/") public String index(){ return "index"; }
}
