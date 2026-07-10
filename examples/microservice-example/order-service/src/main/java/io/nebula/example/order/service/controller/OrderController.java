package io.nebula.example.order.service.controller;

import io.nebula.example.order.api.dto.CreateOrderDto;
import io.nebula.example.order.api.dto.GetOrderDto;
import io.nebula.example.order.service.rpc.OrderRpcClientImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/orders", "/rpc/orders"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderRpcClientImpl orderRpcClient;

    @PostMapping
    public CreateOrderDto.Response createOrder(@Valid @RequestBody CreateOrderDto.Request request) {
        return orderRpcClient.createOrder(request);
    }

    @GetMapping("/{id}")
    public GetOrderDto.Response getOrderById(@PathVariable Long id) {
        return orderRpcClient.getOrderById(id);
    }
}
