package org.example.tripservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.tripservice.exceptions.TripNotFoundException;
import org.example.tripservice.models.entities.Trip;
import org.example.tripservice.models.repositories.TripRepository;
import org.example.tripservice.models.services.TripService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private static final String TRIP_CACHE = "trips";

    private final TripRepository tripRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = TRIP_CACHE, key = "#id", sync = true)
    public Trip getTripById(Long id) {
        return tripRepository.findById(id).orElseThrow(() -> new TripNotFoundException(id));
    }
}
