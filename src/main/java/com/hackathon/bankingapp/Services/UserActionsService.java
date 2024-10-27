package com.hackathon.bankingapp.Services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hackathon.bankingapp.Entities.Account;
import com.hackathon.bankingapp.Entities.AccountAsset;
import com.hackathon.bankingapp.Entities.Transaction;
import com.hackathon.bankingapp.Entities.User;
import com.hackathon.bankingapp.Repositories.AccountAssetRepository;
import com.hackathon.bankingapp.Repositories.AccountRepository;
import com.hackathon.bankingapp.Repositories.TransactionRepository;
import com.hackathon.bankingapp.Repositories.UserRepository;

import jakarta.annotation.PreDestroy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@EnableScheduling
public class UserActionsService {

    // He de decir que no estoy muy orgulloso del codigo que tengo aqui, no tengo mucho tiempo y es lo que he podido hacer 
    // y bueno ire explicandolo

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountAssetRepository accountAssetRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RestTemplate restTemplate;

    private final String MARKET_PRICES_URL = "https://faas-lon1-917a94a7.doserverless.co/api/v1/web/fn-e0f31110-7521-4cb9-86a2-645f66eefb63/default/market-prices-simulator";

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentSubscriptionTask;

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private ScheduledExecutorService botScheduler;

    public String createSubscription(String email, String pin, BigDecimal amount, int intervalSeconds) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Account account = accountRepository.findByUserId(user.getId());

        if (!encoder.matches(pin, account.getPin())) {
            return "Invalid PIN";
        }

        if (account.getBalance().compareTo(amount) < 0) {
            return "Insufficient balance";
        }

        if (currentSubscriptionTask != null && !currentSubscriptionTask.isCancelled()) {
            currentSubscriptionTask.cancel(true);
        }

        currentSubscriptionTask = scheduler.scheduleAtFixedRate(() -> processSubscription(email, amount), 
                                                               0, intervalSeconds, TimeUnit.SECONDS);

        return "Subscription created successfully.";
    }

    // Esto es tan solo un metodo de prueba de una suscripcion que de forma random dice de sumar o restar el dinero que hayas metido
    private void processSubscription(String email, BigDecimal amount) {
        Account account = accountRepository.findByUserId(userRepository.findByEmail(email).get().getId());
        Random random = new Random();
    
        BigDecimal randomAmount = amount.multiply(BigDecimal.valueOf(random.nextDouble() * 2)); 
        boolean shouldAdd = random.nextBoolean(); 
    
        if (shouldAdd) {
            account.setBalance(account.getBalance().add(randomAmount));
        } else {
            account.setBalance(account.getBalance().subtract(randomAmount));
        }
    
        accountRepository.save(account);
    
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(shouldAdd ? randomAmount : randomAmount.negate());
        transaction.setTransactionDate(System.currentTimeMillis());
        transaction.setTransactionType("SUBSCRIPTION");
        transaction.setSourceAccountNumber(account.getAccountNumber());
        transactionRepository.save(transaction);
    
        if (account.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            currentSubscriptionTask.cancel(true);
        }
    }

    public String enableAutoInvest(String email, String pin) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Account account = accountRepository.findByUserId(user.getId());

        if (!encoder.matches(pin, account.getPin())) {
            return "Invalid PIN";
        }

        startAutoInvestBot(email, pin);

        return "Automatic investment enabled successfully.";
    }

    private void startAutoInvestBot(String email, String pin) {
        botScheduler = Executors.newSingleThreadScheduledExecutor();
        botScheduler.scheduleAtFixedRate(() -> checkMarketAndTrade(email, pin), 0, 30, TimeUnit.SECONDS);        

    }

    // En este metodo se mete e itera sobre todos los assets del usuario viendo si merecen la pena ser vendidos o comprados segun el porcentaje
    // de gain/loss que hayan tenido con el precio que lo compraron, funciona a lo mejor no de la manera mas optima pero mas o menos

    private void checkMarketAndTrade(String email, String pin) {
        try {
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Account account = accountRepository.findByUserId(user.getId());
    
            List<AccountAsset> userAssets = accountAssetRepository.findByAccount(account);
    
            Map<String, BigDecimal> currentPrices = fetchAllMarketPrices();
    
            for (AccountAsset asset : userAssets) {
                String assetSymbol = asset.getAssetSymbol();
                BigDecimal currentPrice = currentPrices.get(assetSymbol);
    
                if (currentPrice != null) {
                        BigDecimal previousPrice = asset.getpurchasePrice();
                        BigDecimal changePercentage = currentPrice.subtract(previousPrice)
                            .divide(previousPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

    
                        if (changePercentage.compareTo(BigDecimal.valueOf(-3)) <= 0) {
                            BigDecimal amountToBuy = new BigDecimal("0.1");
    
                            buyAsset(email, assetSymbol, amountToBuy, pin);
                        }
                        else if (changePercentage.compareTo(BigDecimal.valueOf(3)) >= 0) {
                            BigDecimal amountToSell = new BigDecimal("0.1");
    
                            sellAsset(email, assetSymbol, amountToSell, pin);
                        }
                    }
                }
        } catch (Exception e) {
            System.err.println("Error during market check: " + e.getMessage());
        }
    }
    
    private void buyAsset(String email, String assetSymbol, BigDecimal amount, String pin) {
        accountService.buyAsset(email, assetSymbol, amount, pin);
    }
    
    private String sellAsset(String email, String assetSymbol, BigDecimal quantity, String pin) {
        return accountService.sellAsset(email, assetSymbol, quantity, pin);
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
    
}