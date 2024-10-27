package com.hackathon.bankingapp.Services;

import com.hackathon.bankingapp.Entities.Account;
import com.hackathon.bankingapp.Entities.AccountAsset;
import com.hackathon.bankingapp.Entities.Transaction;
import com.hackathon.bankingapp.Entities.User;
import com.hackathon.bankingapp.Repositories.AccountAssetRepository;
import com.hackathon.bankingapp.Repositories.AccountRepository;
import com.hackathon.bankingapp.Repositories.TransactionRepository;
import com.hackathon.bankingapp.Repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountAssetRepository accountAssetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RestTemplate restTemplate;

    private final String MARKET_PRICES_URL = "https://faas-lon1-917a94a7.doserverless.co/api/v1/web/fn-e0f31110-7521-4cb9-86a2-645f66eefb63/default/market-prices-simulator";


    public String createPin(String email, String pin, String rawPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getHashedPassword())) {
            return "Incorrect pasword";
        }

        Account account = accountRepository.findByUserId(user.getId());

        account.setPin(passwordEncoder.encode(pin));
        accountRepository.save(account);

        return "PIN created successfully";
    }

    public String updatePin(String email, String oldPin, String newPin, String rawPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getHashedPassword())) {
            return "Incorrect pasword";
        }

        Account account = accountRepository.findByUserId(user.getId());

        if (!passwordEncoder.matches(oldPin, account.getPin())) {
            return "Old PIN is icorrect";
        }

        account.setPin(passwordEncoder.encode(newPin));
        accountRepository.save(account);

        return "PIN updated successfully";
    }

    public String deposit(String email, String pin, BigDecimal amount) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Account account = accountRepository.findByUserId(user.getId());

        if (!passwordEncoder.matches(pin, account.getPin())) {
            return "Invalid PIN";
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(amount);
        transaction.setTransactionDate(System.currentTimeMillis());
        transaction.setTransactionType("CASH_DEPOSIT");
        transaction.setSourceAccountNumber(account.getAccountNumber());
        transactionRepository.save(transaction);

        return "Cash deposited successfully";
    }

    public String withdraw(String email, String pin, BigDecimal amount) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Account account = accountRepository.findByUserId(user.getId());

        if (!passwordEncoder.matches(pin, account.getPin())) {
            return "Invalid PIN";
        }

        if (account.getBalance().compareTo(amount) < 0) {
            return "Insufficient balance";
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(amount);
        transaction.setTransactionDate(System.currentTimeMillis());
        transaction.setTransactionType("CASH_WITHDRAWAL");
        transaction.setSourceAccountNumber(account.getAccountNumber());
        transactionRepository.save(transaction);

        return "Cash withdrawn successfully";
    }

    public String transferFunds(String sourceAccountEmail, String pin, BigDecimal amount, String targetAccountNumber) {
        User sourceUser = userRepository.findByEmail(sourceAccountEmail)
            .orElseThrow(() -> new IllegalArgumentException("Source user not found"));

        Account account = accountRepository.findByUserId(sourceUser.getId());

        if (!passwordEncoder.matches(pin, account.getPin())) {
            return "Invalid PIN";
        }

        if (account.getBalance().compareTo(amount) < 0) {
            return "Insufficient balance";
        }

        Account targetAccount = accountRepository.findByAccountNumber(targetAccountNumber);
        if (targetAccount == null) {
            return "Target account not found";
        }

        targetAccount.setBalance(targetAccount.getBalance().add(amount));
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(targetAccount);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(amount);
        transaction.setTransactionDate(System.currentTimeMillis());
        transaction.setTransactionType("CASH_TRANSFER");
        transaction.setSourceAccountNumber(account.getAccountNumber());
        transaction.setTargetAccountNumber(targetAccount.getAccountNumber());
        transactionRepository.save(transaction);

        return "Fund transferred successfully";
    }

    public List<Transaction> getTransactionHistory(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Account account = accountRepository.findByUserId(user.getId());
        if (account == null) {
            return null;
        }

        return transactionRepository.findByAccountId(account.getAccountId());
    }

    public String buyAsset(String email, String assetSymbol, BigDecimal amount, String pin) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Account account = accountRepository.findByUserId(user.getId());
    
        if (!passwordEncoder.matches(pin, account.getPin())) {
            return "Invalid PIN";
        }
    
        if (account.getBalance().compareTo(amount) < 0) {
            return "Insufficient balance";
        }
    
        ResponseEntity<Map<String, Double>> response = restTemplate.getForEntity(MARKET_PRICES_URL, (Class<Map<String, Double>>) (Class<?>) Map.class);
        
        Map<String, Double> apiResponse = response.getBody();
        
        if (apiResponse == null) {
            return "No se pudo obtener los precios del mercado";
        }
    
        if (!apiResponse.containsKey(assetSymbol)) {
            return "Asset not found in market prices";
        }
    
        BigDecimal assetPrice = BigDecimal.valueOf(apiResponse.get(assetSymbol));
        BigDecimal quantity = amount.divide(assetPrice, 2, RoundingMode.UP);
    
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        
        AccountAsset newAsset = new AccountAsset();
        newAsset.setAccount(account);
        newAsset.setAssetSymbol(assetSymbol);
        newAsset.setQuantity(quantity);
        newAsset.setpurchasePrice(assetPrice.multiply(quantity));
        accountAssetRepository.save(newAsset);
        
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(amount);
        transaction.setTransactionDate(System.currentTimeMillis());
        transaction.setTransactionType("ASSET_PURCHASE");
        transaction.setSourceAccountNumber(account.getAccountNumber());
        transactionRepository.save(transaction);
    
        BigDecimal totalAssetsValue = calculateTotalAssetsValue(account, apiResponse);
        BigDecimal netWorth = account.getBalance().add(totalAssetsValue);
    
        String emailContent = String.format(
            "Dear %s,\n\n" +
            "You have successfully purchased %.2f units of %s for a total amount of $%.2f.\n\n" +
            "Current holdings of %s: %.2f units\n\n" +
            "Summary of current assets:\n%s" +
            "Account Balance: $%.2f\n" +
            "Net Worth: $%.2f\n\n" +
            "Thank you for using our investment services.\n\n" +
            "Best Regards,\n" +
            "Investment Management Team",
            user.getName(), quantity, assetSymbol, amount, assetSymbol, quantity,
            getAssetsSummary(account, apiResponse), account.getBalance(), netWorth
        );
    
        emailService.sendEmail(email, emailContent);
        return "Asset purchase successful.";
    }
    
    private BigDecimal calculateTotalAssetsValue(Account account, Map<String, Double> apiResponse) {
        List<AccountAsset> assets = accountAssetRepository.findByAccount(account);
        BigDecimal totalValue = BigDecimal.ZERO;
    
        for (AccountAsset asset : assets) {
            String symbol = asset.getAssetSymbol();
            BigDecimal price = BigDecimal.valueOf(apiResponse.get(symbol));
            totalValue = totalValue.add(price.multiply(asset.getQuantity()));
        }
    
        return totalValue;
    }
    
    private String getAssetsSummary(Account account, Map<String, Double> apiResponse) {
        List<AccountAsset> assets = accountAssetRepository.findByAccount(account);
        StringBuilder summary = new StringBuilder();
    
        for (AccountAsset asset : assets) {
            String symbol = asset.getAssetSymbol();
            BigDecimal quantity = asset.getQuantity();
            BigDecimal price = BigDecimal.valueOf(apiResponse.get(symbol));
            summary.append(String.format("- %s: %.2f units purchased at $%.2f\n", symbol, quantity, price));
        }
    
        return summary.toString();
    }
    

    public String sellAsset(String email, String assetSymbol, BigDecimal amount, String pin) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Account account = accountRepository.findByUserId(user.getId());
    
        if (!passwordEncoder.matches(pin, account.getPin())) {
            return "Invalid PIN";
        }
    
        ResponseEntity<Map<String, Double>> response = restTemplate.getForEntity(MARKET_PRICES_URL, (Class<Map<String, Double>>) (Class<?>) Map.class);
        
        Map<String, Double> apiResponse = response.getBody();
        
        if (apiResponse == null) {
            return "No se pudo obtener los precios del mercado";
        }
    
        if (!apiResponse.containsKey(assetSymbol)) {
            return "Asset not found in market prices";
        }
    
        List<AccountAsset> userAssets = accountAssetRepository.findByAccountAndAssetSymbol(account, assetSymbol);
        if (userAssets.isEmpty() || userAssets.get(0).getQuantity().compareTo(amount) < 0) {
            return "Insufficient asset quantity";
        }
    
        BigDecimal assetPrice = BigDecimal.valueOf(apiResponse.get(assetSymbol));
        BigDecimal totalReceived = amount.multiply(assetPrice);
    
        BigDecimal saleAmount = assetPrice.multiply(amount);
        account.setBalance(account.getBalance().add(totalReceived));
        accountRepository.save(account);
        
        AccountAsset accountAsset = userAssets.get(0);
        accountAsset.setQuantity(accountAsset.getQuantity().subtract(amount));
        
        if (accountAsset.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            accountAssetRepository.delete(accountAsset);
        } else {
            accountAssetRepository.save(accountAsset);
        }
        
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(totalReceived);
        transaction.setTransactionDate(System.currentTimeMillis());
        transaction.setTransactionType("ASSET_SALE");
        transaction.setSourceAccountNumber(account.getAccountNumber());
        transactionRepository.save(transaction);
    
        BigDecimal totalAssetsValue = calculateTotalAssetsValue(account, apiResponse);
        BigDecimal netWorth = account.getBalance().add(totalAssetsValue);
    
        BigDecimal totalGainLoss = saleAmount.subtract(accountAsset.getpurchasePrice().multiply(amount));

        String emailContent = String.format(
            "Dear %s,\n\n" +
            "You have successfully sold %.2f units of %s.\n\n" +
            "Total Gain/Loss: $%.2f\n\n" +
            "Remaining holdings of %s: %.2f units\n\n" +
            "Summary of current assets:\n%s" +
            "Account Balance: $%.2f\n" +
            "Net Worth: $%.2f\n\n" +
            "Thank you for using our investment services.\n\n" +
            "Best Regards,\n" +
            "Investment Management Team",
            user.getName(), amount, assetSymbol, totalGainLoss, assetSymbol, accountAsset.getQuantity(),
            getAssetsSummary(account, apiResponse), account.getBalance(), netWorth
        );
    
        emailService.sendEmail(email, emailContent);
        return "Asset sale successful.";
    }

    public BigDecimal calculateNetWorth(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Account account = accountRepository.findByUserId(user.getId());

        BigDecimal cashBalance = account.getBalance();

        Map<String, Double> assetPrices = fetchCurrentAssetPrices();

        BigDecimal totalAssetsValue = calculateTotalAssetsValue(account, assetPrices);

        BigDecimal netWorth = cashBalance.add(totalAssetsValue);
        return netWorth.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Double> fetchCurrentAssetPrices() {
        ResponseEntity<Map<String, Double>> response = restTemplate.getForEntity(MARKET_PRICES_URL, (Class<Map<String, Double>>) (Class<?>) Map.class);
        return response.getBody();
    }

    public Map<String, BigDecimal> fetchAllMarketPrices() {
        Map<String, Double> response = restTemplate.getForObject(MARKET_PRICES_URL, Map.class);
        if (response == null) {
            return new HashMap<>();
        }

        return response.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BigDecimal.valueOf(entry.getValue())
                ));
    }

    public BigDecimal fetchMarketPrice(String assetSymbol) {
        Map<String, BigDecimal> marketPrices = fetchAllMarketPrices();
        return marketPrices.get(assetSymbol.toUpperCase());
    }
    
}
