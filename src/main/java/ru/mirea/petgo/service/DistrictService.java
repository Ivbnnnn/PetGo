package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.District;
import ru.mirea.petgo.dto.DistrictDto;
import ru.mirea.petgo.error.ConflictException;
import ru.mirea.petgo.error.ResourceNotFoundException;
import ru.mirea.petgo.repository.DistrictRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DistrictService {
    private static final Sort BY_NAME = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));
    private final DistrictRepository districts;

    @Transactional(readOnly = true)
    public List<DistrictDto> list(Boolean enabled) {
        List<District> results = enabled == null ? districts.findAll(BY_NAME)
                : districts.findAllByEnabled(enabled, BY_NAME);
        return results.stream().map(DistrictDto::from).toList();
    }

    @Transactional
    public DistrictDto create(String name) {
        String cleanName = name.strip();
        if (districts.existsByNameIgnoreCase(cleanName)) {
            throw duplicateName();
        }
        District district = new District();
        district.setName(cleanName);
        return DistrictDto.from(districts.save(district));
    }

    @Transactional
    public DistrictDto update(UUID id, String name) {
        District district = find(id);
        String cleanName = name.strip();
        if (districts.existsByNameIgnoreCaseAndIdNot(cleanName, id)) {
            throw duplicateName();
        }
        district.setName(cleanName);
        return DistrictDto.from(district);
    }

    @Transactional
    public DistrictDto setEnabled(UUID id, boolean enabled) {
        District district = find(id);
        district.setEnabled(enabled);
        return DistrictDto.from(district);
    }

    @Transactional(readOnly = true)
    public District assignable(UUID id) {
        District district = find(id);
        if (!district.isEnabled()) {
            throw new ConflictException("Район отключён");
        }
        return district;
    }

    private District find(UUID id) {
        return districts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Район не найден"));
    }

    private ConflictException duplicateName() {
        return new ConflictException("Район с таким названием уже существует");
    }
}
