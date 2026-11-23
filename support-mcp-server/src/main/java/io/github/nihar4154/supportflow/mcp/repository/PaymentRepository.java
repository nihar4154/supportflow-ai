package io.github.nihar4154.supportflow.mcp.repository;

import io.github.nihar4154.supportflow.mcp.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderOrderNumberOrderByChargedAtAsc(String orderNumber);

    Optional<Payment> findByTransactionRef(String transactionRef);
}
