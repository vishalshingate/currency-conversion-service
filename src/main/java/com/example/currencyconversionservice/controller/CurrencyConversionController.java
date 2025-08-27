package com.example.currencyconversionservice.controller;

import com.example.currencyconversionservice.CurrencyExchangeProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;

@RestController
public class CurrencyConversionController {

    @Autowired
    private CurrencyExchangeProxy currencyExchangeProxy;

    @GetMapping("/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversion(@PathVariable String from, @PathVariable String to, @PathVariable
    BigDecimal quantity) {

        HashMap<String, String> uirParams = new HashMap<>();
        uirParams.put("from", from);
        uirParams.put("to", to);

        // here we are using restTemplate to call the another microservice to get te data.
        ResponseEntity<CurrencyConversion> responseEntity =
            new RestTemplate().
                getForEntity("http://localhost:8200/currency-exchange/from/{from}/to/{to}",
                    CurrencyConversion.class, uirParams);

        CurrencyConversion currencyConversion = responseEntity.getBody();

        if (currencyConversion == null) {
            throw new RuntimeException("Currency Conversion Not Found");
        }
        return new CurrencyConversion(
            currencyConversion.getId(), from, to, quantity,
            currencyConversion.getConversionMultiple(), quantity.multiply(currencyConversion.getConversionMultiple()),
            currencyConversion.getEnvironment());
    }

    @GetMapping("/currency-conversion-feign/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversionFeign(@PathVariable String from, @PathVariable String to, @PathVariable
    BigDecimal quantity) {
        CurrencyConversion currencyConversion = currencyExchangeProxy.retrieveExchangeValue(from, to);

        if (currencyConversion == null) {
            throw new RuntimeException("Currency Conversion Not Found");
        }
        return new CurrencyConversion(
            currencyConversion.getId(), from, to, quantity,
            currencyConversion.getConversionMultiple(), quantity.multiply(currencyConversion.getConversionMultiple()),
            currencyConversion.getEnvironment());

    }

}

