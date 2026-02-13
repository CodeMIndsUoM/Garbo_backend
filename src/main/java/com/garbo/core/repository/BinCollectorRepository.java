package com.garbo.core.repository;

import com.garbo.core.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BinCollectorRepository extends JpaRepository<Admin, Long> {

}
