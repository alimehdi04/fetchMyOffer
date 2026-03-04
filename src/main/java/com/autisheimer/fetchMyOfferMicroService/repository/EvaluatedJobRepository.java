package com.autisheimer.fetchMyOfferMicroService.repository;

import com.autisheimer.fetchMyOfferMicroService.entity.EvaluatedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluatedJobRepository extends JpaRepository<EvaluatedJob, Long> {

    // Spring magically writes the SQL query for this based on the method name!
    boolean existsByJobUrl(String jobUrl);
}