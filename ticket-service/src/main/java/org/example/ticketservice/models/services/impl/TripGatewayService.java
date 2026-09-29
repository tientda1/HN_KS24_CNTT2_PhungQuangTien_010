package org.example.ticketservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.ticketservice.clients.TripClient;
import org.example.ticketservice.exceptions.TripNotFoundException;
import org.example.ticketservice.exceptions.TripServiceException;
import org.example.ticketservice.models.dto.responses.TripResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripGatewayService {

    private static final String CIRCUIT_BREAKER_NAME = "tripService";
    private static final String FALLBACK_METHOD = "getTripByIdFallback";

    private final TripClient tripClient;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = FALLBACK_METHOD)
    public TripResponse getTripById(Long tripId) {
        TripResponse trip = tripClient.getTripById(tripId);
        if (trip == null || trip.id() == null) {
            throw new TripServiceException("Trip service returned an invalid response for trip id: " + tripId);
        }
        return trip;
    }

    private TripResponse getTripByIdFallback(Long tripId, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            throw new TripNotFoundException(tripId);
        }
        throw new TripServiceException("Trip service is unavailable for trip id: " + tripId, throwable);
    }
}
