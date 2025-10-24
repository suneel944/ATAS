package com.atas.framework.repository;

import com.atas.framework.model.TestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TestAttachment} entities. Attachments include screenshots, videos and log
 * files and can be queried directly or via their parent result.
 */
@Repository
public interface TestAttachmentRepository extends JpaRepository<TestAttachment, Long> {}
