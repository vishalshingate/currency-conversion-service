package com.example.currencyconversionservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;

@RestController
public class CurrencyConversionController {


    @GetMapping("/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversion(@PathVariable String from, @PathVariable String to, @PathVariable
    BigDecimal quantity ) {

        HashMap<String, String> uirParams = new HashMap<>();
        uirParams.put("from", from);
        uirParams.put("to", to);

       // here we are using restTemplate to call the another microservice to get te data.
        ResponseEntity<CurrencyConversion> responseEntity =
        new RestTemplate().
            getForEntity("http://localhost:8200/currency-exchange/from/{from}/to/{to}",
                CurrencyConversion.class, uirParams);

        CurrencyConversion currencyConversion = responseEntity.getBody();

        return new CurrencyConversion(
            currencyConversion.getId(), from, to, quantity,
            currencyConversion.getConversionMultiple(), quantity.multiply(currencyConversion.getConversionMultiple()), currencyConversion.getEnvironment());
    }
}
