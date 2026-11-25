package com.mendel.transactions.web;

import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.service.SaveResult;
import com.mendel.transactions.service.TransactionService;
import com.mendel.transactions.web.dto.TransactionRequest;
import com.mendel.transactions.web.dto.TransactionStatusResponse;
import com.mendel.transactions.web.dto.TransactionSumResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionStatusResponse> save(
            @PathVariable long id,
            @Valid @RequestBody TransactionRequest request) {
        Transaction transaction = new Transaction(id, request.amount(), request.type(), request.parentId());
        SaveResult result = service.save(transaction);
        HttpStatus status = result == SaveResult.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(new TransactionStatusResponse("ok"));
    }

    @GetMapping("/types/{type}")
    public List<Long> findIdsByType(@PathVariable String type) {
        return service.findIdsByType(type);
    }

    @GetMapping("/sum/{id}")
    public TransactionSumResponse sum(@PathVariable long id) {
        return new TransactionSumResponse(service.sumWithDescendants(id));
    }
}
