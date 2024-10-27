package com.hackathon.bankingapp.Repositories;

import com.hackathon.bankingapp.Entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByUserId(Long userId);
    Account findByAccountNumber(String accountNumber);
}

