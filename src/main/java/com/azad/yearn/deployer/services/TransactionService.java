package com.azad.yearn.deployer.services;

import com.azad.yearn.deployer.utils.AsyncConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionService {
    /**
     * To be used later when this code use api to query
    @Autowired
    TransactionRepository transactionRepository;

    @Async(AsyncConfiguration.TASK_EXECUTOR_SERVICE)
    public CompletableFuture<Page<Transaction>> findAll(final Pageable pageable) {
        return transactionRepository.findAllBy(pageable);
    }
    @Async(AsyncConfiguration.TASK_EXECUTOR_SERVICE)
    public CompletableFuture<Optional<Transaction>> findOneById(final String id) {
        return transactionRepository
                .findOneById(id)
                .thenApply(Optional::ofNullable);
    }
    */



}
