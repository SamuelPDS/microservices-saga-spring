package br.com.microservices.orchestrated.orchestratorservice.core.service;

import org.springframework.stereotype.Service;

@Service
public class TesteService {
    public int simpleSum(int num1,  int num2) {
        return num1 + num2;
    }
}
