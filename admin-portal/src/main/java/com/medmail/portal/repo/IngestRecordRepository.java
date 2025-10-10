package com.medmail.portal.repo;

import com.medmail.portal.domain.IngestRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestRecordRepository extends JpaRepository<IngestRecord, Long> {
  Page<IngestRecord> findByFile_Id(Long fileId, Pageable pageable);
}
