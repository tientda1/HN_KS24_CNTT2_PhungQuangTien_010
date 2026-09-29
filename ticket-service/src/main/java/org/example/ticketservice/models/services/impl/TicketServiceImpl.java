package org.example.ticketservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.ticketservice.exceptions.InvalidBookingException;
import org.example.ticketservice.messaging.TicketEventPublisher;
import org.example.ticketservice.models.constants.TicketStatus;
import org.example.ticketservice.models.dto.requests.CreateTicketDetailRequest;
import org.example.ticketservice.models.dto.requests.CreateTicketRequest;
import org.example.ticketservice.models.dto.responses.TicketDetailResponse;
import org.example.ticketservice.models.dto.responses.TicketResponse;
import org.example.ticketservice.models.dto.responses.TripResponse;
import org.example.ticketservice.models.entities.Ticket;
import org.example.ticketservice.models.entities.TicketDetail;
import org.example.ticketservice.models.repositories.TicketDetailRepository;
import org.example.ticketservice.models.repositories.TicketRepository;
import org.example.ticketservice.models.services.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketDetailRepository ticketDetailRepository;
    private final TripGatewayService tripGatewayService;
    private final TicketEventPublisher ticketEventPublisher;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        List<TicketDetail> details = new ArrayList<>();
        double totalAmount = 0d;

        for (CreateTicketDetailRequest item : request.items()) {
            TripResponse trip = tripGatewayService.getTripById(item.tripId());
            validateSeatAvailability(item, trip);

            double lineTotal = trip.ticketPrice() * item.seat();
            totalAmount += lineTotal;

            details.add(TicketDetail.builder()
                    .tripId(trip.id())
                    .seats(item.seat())
                    .ticketPrice(trip.ticketPrice())
                    .lineTotal(lineTotal)
                    .build());
        }

        Ticket ticket = ticketRepository.save(Ticket.builder()
                .passengerName(request.passengerName())
                .passengerEmail(request.passengerEmail())
                .totalAmount(totalAmount)
                .status(TicketStatus.PENDING)
                .build());

        details.forEach(detail -> detail.setTicket(ticket));
        ticketDetailRepository.saveAll(details);

        ticketEventPublisher.publishTicketCreated(ticket.getPassengerEmail());

        return new TicketResponse(
                ticket.getId(),
                ticket.getPassengerName(),
                ticket.getPassengerEmail(),
                ticket.getTotalAmount(),
                ticket.getStatus(),
                details.stream().map(TicketServiceImpl::toDetailResponse).toList()
        );
    }

    private void validateSeatAvailability(CreateTicketDetailRequest item, TripResponse trip) {
        if (item.seat() == null || item.seat() <= 0) {
            throw new InvalidBookingException("Seat must be greater than 0 for trip id: " + trip.id());
        }
        if (trip.availableSeats() == null || item.seat() > trip.availableSeats()) {
            throw new InvalidBookingException("Trip id " + trip.id() + " does not have enough available seats");
        }
    }

    private static TicketDetailResponse toDetailResponse(TicketDetail detail) {
        return new TicketDetailResponse(
                detail.getId(),
                detail.getTripId(),
                detail.getSeats(),
                detail.getTicketPrice(),
                detail.getLineTotal()
        );
    }
}
