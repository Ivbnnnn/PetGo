package ru.mirea.petgo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import ru.mirea.petgo.domain.RequestPriority;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.WalkRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Аналитика: все показатели считает PostgreSQL (COUNT, AVG, MIN, MAX, SUM, GROUP BY).
 * Правила: «всего» считает все заявки; остальное — только неархивные в периоде по дате прогулки.
 */
public interface AnalyticsRepository extends Repository<WalkRequest, UUID> {

    interface StatusCount { RequestStatus getStatus(); Long getCnt(); }

    interface PriorityCount { RequestPriority getPriority(); Long getCnt(); }

    interface NameCount { String getName(); Long getCnt(); }

    interface PriceStats { Long getCnt(); Double getAvgPrice(); Double getMinPrice(); Double getMaxPrice(); }

    @Query("select count(r) from WalkRequest r")
    long countAll();

    @Query("select count(r) from WalkRequest r where r.isArchived = true")
    long countArchived();

    @Query("""
            select r.status as status, count(r) as cnt
            from WalkRequest r
            where r.isArchived = false and r.scheduledAt >= :from and r.scheduledAt < :to
            group by r.status""")
    List<StatusCount> countByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select r.priority as priority, count(r) as cnt
            from WalkRequest r
            where r.isArchived = false and r.scheduledAt >= :from and r.scheduledAt < :to
            group by r.priority""")
    List<PriorityCount> countByPriority(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select count(r) as cnt, avg(r.price) as avgPrice, min(r.price) as minPrice, max(r.price) as maxPrice
            from WalkRequest r
            where r.isArchived = false and r.scheduledAt >= :from and r.scheduledAt < :to""")
    PriceStats priceStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select sum(r.price)
            from WalkRequest r
            where r.status = ru.mirea.petgo.domain.RequestStatus.COMPLETED and r.isArchived = false
              and r.scheduledAt >= :from and r.scheduledAt < :to""")
    Double revenueCompleted(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select p.name as name, count(r) as cnt
            from WalkRequest r join r.pet p
            where r.isArchived = false and r.scheduledAt >= :from and r.scheduledAt < :to
            group by p.id, p.name
            order by count(r) desc, p.name""")
    List<NameCount> topPets(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("""
            select o.username as name, count(r) as cnt
            from WalkRequest r join r.owner o
            where r.isArchived = false and r.scheduledAt >= :from and r.scheduledAt < :to
            group by o.id, o.username
            order by count(r) desc, o.username""")
    List<NameCount> topOwners(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("""
            select w.username as name, count(r) as cnt
            from WalkRequest r join r.walker w
            where r.isArchived = false and r.scheduledAt >= :from and r.scheduledAt < :to
            group by w.id, w.username
            order by count(r) desc, w.username""")
    List<NameCount> topWalkers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("""
            select d.name as name, count(r) as cnt
            from WalkRequest r join r.district d
            where r.isArchived = false and r.scheduledAt >= :from and r.scheduledAt < :to
            group by d.id, d.name
            order by d.name""")
    List<NameCount> countByDistrict(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Нативный запрос: сущности WalkHistory пока нет. После её появления можно заменить на JPQL. */
    @Query(value = "select cast(avg(rating) as double precision) from walk_history", nativeQuery = true)
    Double averageRating();
}
