package com.hackathon.bankingapp.Repositories;

import com.hackathon.bankingapp.Entities.Account;
import com.hackathon.bankingapp.Entities.AccountAsset;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountAssetRepository extends JpaRepository<AccountAsset, Long> {
    List<AccountAsset> findByAccountAndAssetSymbol(Account account, String assetSymbol);

    List<AccountAsset> findByAccount(Account account);
}

