package kz.kbtu.webmidterm.service;

import kz.kbtu.webmidterm.entity.ServiceType;
import kz.kbtu.webmidterm.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceTypeService {

    private final ServiceTypeRepository repository;

    public List<ServiceType> findAll() {
        return repository.findAll();
    }
}
