ALTER TABLE pets ADD CONSTRAINT uq_pets_id_owner UNIQUE (id, owner_id);

ALTER TABLE walk_requests
    ADD CONSTRAINT fk_walk_request_pet_owner FOREIGN KEY (pet_id, owner_id) REFERENCES pets (id, owner_id);
