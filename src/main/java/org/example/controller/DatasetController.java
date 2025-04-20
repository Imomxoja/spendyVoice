package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.DatasetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dataset")
@RequiredArgsConstructor
public class DatasetController {
    private final DatasetService service;

    @GetMapping("/get-count")
    public Long getCount() {
        return service.getCount();
    }

    @GetMapping("/delete")
    public void clear() {
        service.deleteAll();
    }
}
