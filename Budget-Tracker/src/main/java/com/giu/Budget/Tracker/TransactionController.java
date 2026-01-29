package com.giu.Budget.Tracker;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private TransactionRepository transactionRepository;
    private UserRepository userRepository;

    public TransactionController(TransactionRepository transactionRepository,UserRepository userRepository) {
        this.transactionRepository=transactionRepository;
        this.userRepository=userRepository;
    }

    @GetMapping
    public List<Transaction> getAllTransations(){
        return transactionRepository.findAll();
    }
    @PostMapping("/{userId}")
    public Transaction createTransaction(@RequestBody Transaction transaction,@PathVariable Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        transaction.setUser(user);
        // 1. Auto-set date if missing
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDate.now());
        }

        // 2. CHECK: Is this an EXPENSE? (Amount is less than 0)
        // BigDecimal.compareTo returning -1 means "less than"
        if (transaction.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {

            // 3. Calculate current balance (Re-using logic from getBalance)
            List<Transaction> allTransactions = transactionRepository.findAll();
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (Transaction t : allTransactions) {
                total = total.add(t.getAmount());
            }

            // 4. PREDICT: What will the balance be if we allow this?
            java.math.BigDecimal futureBalance = total.add(transaction.getAmount());

            // 5. IF future balance is negative -> STOP!
            if (futureBalance.compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Insufficient funds! You are broke.");
            }
        }

        // 6. If we survived the check, save it.
        return transactionRepository.save(transaction);
    }
    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable Long id){
        transactionRepository.deleteById(id);
    }
    @GetMapping("/balance")
    public java.math.BigDecimal getBalance(){
        List<Transaction> allTransactions=transactionRepository.findAll();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;

        for (Transaction t : allTransactions) {
            total = total.add(t.getAmount());
        }

        return total;
    }
    // GET transactions for a specific user
    @GetMapping("/user/{userId}")
    public List<Transaction> getUserTransactions(@PathVariable Long userId){
        return transactionRepository.findByUserId(userId);
    }

    // GET balance for a specific user
    @GetMapping("/balance/{userId}")
    public java.math.BigDecimal getBalance(@PathVariable Long userId){
        List<Transaction> userTransactions = transactionRepository.findByUserId(userId);
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;

        for (Transaction t : userTransactions) {
            total = total.add(t.getAmount());
        }
        return total;
    }

}
