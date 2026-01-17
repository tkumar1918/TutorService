package dev.tushar.tutorservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/{name}")
    public String greetUser(@PathVariable String name) {
        return "<p>Hello %s!</p>".formatted(name);
    }
}
