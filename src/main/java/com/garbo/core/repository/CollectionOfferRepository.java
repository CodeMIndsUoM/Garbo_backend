package com.garbo.core.repository;

import com.garbo.core.entity.CollectionOffer;
import com.garbo.core.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionOfferRepository extends JpaRepository<CollectionOffer, Long> {

    List<CollectionOffer> findByRequest_IdOrderByCreatedAtDesc(Long requestId);

    List<CollectionOffer> findByCollector_EmpIdOrderByCreatedAtDesc(Long collectorId);

    List<CollectionOffer> findByCollector_EmpIdAndStatusOrderByCreatedAtDesc(Long collectorId, OfferStatus status);

    List<CollectionOffer> findByCollector_EmpIdAndStatusInOrderByCreatedAtDesc(Long collectorId,
            List<OfferStatus> statuses);

    Optional<CollectionOffer> findFirstByRequest_IdAndCollector_EmpIdAndStatusIn(
            Long requestId, Long collectorId, List<OfferStatus> activeStatuses);

    List<CollectionOffer> findByRequest_IdAndStatus(Long requestId, OfferStatus status);

    List<CollectionOffer> findByStatusAndCreatedAtBefore(OfferStatus status, java.time.Instant before);

    boolean existsByRequest_IdAndCollector_EmpIdAndStatusNot(Long requestId, Long collectorId, OfferStatus status);
}
