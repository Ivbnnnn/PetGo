
package ru.mirea.petgo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petgo.domain.RequestNote;

import java.util.List;
import java.util.UUID;

public interface RequestNoteRepository
        extends JpaRepository<RequestNote, UUID> {

    List<RequestNote>
    findByWalkRequest_IdOrderByPinnedDescCreatedAtDesc(
            UUID requestId
    );
}
