package com.garagedesk.repo;

import com.garagedesk.model.JobCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface JobCardRepository extends JpaRepository<JobCard, Long> {
    List<JobCard> findByStatusIn(Collection<JobCard.Status> statuses);
    boolean existsByVehicleNumberIgnoreCaseAndStatusIn(String vehicleNumber, Collection<JobCard.Status> statuses);
    List<JobCard> findByVehicleNumberIgnoreCaseOrderByCreatedAtDesc(String vehicleNumber);
    boolean existsByBay_Id(Long bayId);
}
