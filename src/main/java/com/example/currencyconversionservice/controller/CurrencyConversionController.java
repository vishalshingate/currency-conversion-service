package com.example.currencyconversionservice.controller;

import com.example.currencyconversionservice.CurrencyExchangeProxy;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;

@RestController
public class CurrencyConversionController {

    @Autowired
    private CurrencyExchangeProxy currencyExchangeProxy;
    private Logger logger = LoggerFactory.getLogger(CurrencyConversionController.class);

    @Autowired
    private RestTemplate restTemplate; // @LoadBalanced bean defined elsewhere

    @GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversion(@PathVariable String from, @PathVariable String to, @PathVariable
    BigDecimal quantity) {

        HashMap<String, String> uirParams = new HashMap<>();
        uirParams.put("from", from);
        uirParams.put("to", to);

        // Use Eureka service name (no port) so LoadBalancer/Eureka resolves container
        ResponseEntity<CurrencyConversion> responseEntity =
            restTemplate.getForEntity("http://currency-exchange-service/currency-exchange/from/{from}/to/{to}",
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
    @Retry(name = "sample-api", fallbackMethod = "currencyExchangeFallback")
    @Bulkhead(name="sample-api", fallbackMethod = "currencyExchangeFallback") // to configure concurrent calls
    @GetMapping("/currency-conversion-feign/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversionFeign(@PathVariable String from, @PathVariable String to, @PathVariable
    BigDecimal quantity) {
        logger.info("Sample Api call received");
        logger.info("will call exchange service to get exchange with {} {} quantity {}", from, to, quantity);
        CurrencyConversion currencyConversion = currencyExchangeProxy.retrieveExchangeValue(from, to);

        if (currencyConversion == null) {
            throw new RuntimeException("Currency Conversion Not Found");
        }
        return new CurrencyConversion(
            currencyConversion.getId(), from, to, quantity,
            currencyConversion.getConversionMultiple(), quantity.multiply(currencyConversion.getConversionMultiple()),
            currencyConversion.getEnvironment());

    }

    @CircuitBreaker(name = "default", fallbackMethod = "currencyExchangeFallback")
    @GetMapping("/currency-conversion-feign-circuit/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversionFeignCircuit(@PathVariable String from, @PathVariable String to, @PathVariable
    BigDecimal quantity) {
        logger.info("Sample Api call for circuitBreaker received");
        logger.info("will call exchange service to get exchange with {} {} quantity {}", from, to, quantity);
        CurrencyConversion currencyConversion = currencyExchangeProxy.retrieveExchangeValue(from, to);

        if (currencyConversion == null) {
            throw new RuntimeException("Currency Conversion Not Found");
        }
        return new CurrencyConversion(
            currencyConversion.getId(), from, to, quantity,
            currencyConversion.getConversionMultiple(), quantity.multiply(currencyConversion.getConversionMultiple()),
            currencyConversion.getEnvironment());

    }

    public CurrencyConversion currencyExchangeFallback(String from,
                                                       String to,
                                                       BigDecimal quantity,
                                                       Exception ex) {
        logger.warn("Fallback triggered for currency-exchange-service. Reason: {}", ex.toString());
        return new CurrencyConversion(0L, from, to, quantity,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "fallback-retry");
    }

}
