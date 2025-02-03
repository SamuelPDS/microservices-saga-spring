package br.com.microservices.orchestrated.orchestratorservice.core.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.FactoryBasedNavigableListAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class TesteServiceTest {
    @Mock
    TesteService testeService;

   @BeforeEach
    void setUp() {
       MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("")
    public void shouldReturnTheCorrectNumber() {
       var num1 = 4;
       var num2 = 5;
       var sum = num1 + num2;

        when(testeService.simpleSum(num1,num2)).thenReturn(9);

        assertEquals(testeService.simpleSum(num1,num2), sum);
    }

    @Test
    @DisplayName("")
    public void shouldNotReturnTheCorrectNumber() {
        var num1 = 4;
        var num2 = 5;
        var sum = num1 + num2;

        when(testeService.simpleSum(num1,num2)).thenReturn(5);

        assertNotEquals(testeService.simpleSum(num1,num2), sum);
        //       when()
    }
}