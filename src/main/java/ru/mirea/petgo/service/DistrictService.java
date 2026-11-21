package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
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
    private final AuditService audit;

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
        districts.save(district);
        audit.record("DISTRICT_CREATED", EntityType.DISTRICT, district.getId(), "Создан район выгула");
        return DistrictDto.from(district);
    }

    @Transactional
    public DistrictDto update(UUID id, String name) {
        District district = find(id);
        String cleanName = name.strip();
        if (districts.existsByNameIgnoreCaseAndIdNot(cleanName, id)) {
            throw duplicateName();
        }
        district.setName(cleanName);
        audit.record("DISTRICT_UPDATED", EntityType.DISTRICT, id, "Изменено название района");
        return DistrictDto.from(district);
    }

    @Transactional
    public DistrictDto setEnabled(UUID id, boolean enabled) {
        District district = find(id);
        if (district.isEnabled() != enabled) {
            district.setEnabled(enabled);
            audit.record(enabled ? "DISTRICT_ENABLED" : "DISTRICT_DISABLED", EntityType.DISTRICT, id,
                    enabled ? "Район включён" : "Район отключён");
        }
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
