package com.springguard.repo;

import com.springguard.model.entity.ScanRecord;
import com.springguard.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScanRecordRepository extends JpaRepository<ScanRecord, Long> {
    List<ScanRecord> findByUserOrderByCreatedAtDesc(User user);
}
