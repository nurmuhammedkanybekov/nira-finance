package com.nira.finance.controller;

import com.nira.finance.model.Account;
import com.nira.finance.repository.AccountRepository;
import com.nira.finance.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<Account> list() {
        return accountRepository.findByUserId(CurrentUser.id());
    }

    @PostMapping
    public Account create(@Valid @RequestBody Account account) {
        account.setUserId(CurrentUser.id());
        account.setId(null);
        return accountRepository.save(account);
    }
}
