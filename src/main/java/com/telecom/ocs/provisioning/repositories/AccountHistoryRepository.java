package com.telecom.ocs.provisioning.repositories;

import com.telecom.ocs.provisioning.models.AccountHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountHistoryRepository extends JpaRepository<AccountHistory, String> {
}
