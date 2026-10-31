package kz.kbtu.webmidterm.controller;

import kz.kbtu.webmidterm.entity.ServiceType;
import kz.kbtu.webmidterm.service.ServiceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    @GetMapping
    public List<ServiceType> findAll() {
        return serviceTypeService.findAll();
    }
}
