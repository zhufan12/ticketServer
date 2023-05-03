package com.metrix.services.ticket.receivers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import com.metrix.services.common.domains.MetrixException;
import com.metrix.services.ticket.repositories.TicketsRepo;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import com.metrix.services.common.domains.Tickets;
import com.metrix.services.common.domains.Events;
import com.metrix.services.common.domains.TicketsForm;
import com.metrix.services.ticket.services.EventsServices;
import java.util.function.Consumer;

import jakarta.transaction.Transactional;

@Component
public class TicketReceiver {

    @Autowired
    private TicketsRepo repo;   
    
    @Autowired
    private EventsServices eventsSvc;    
    
    @Bean("ticketreceiver")
    public Consumer<TicketsForm> createTicketMsg(){
        return ticketReceiver -> {
            createTicket(ticketReceiver);
        };
    }

    @Transactional
    private void createTicket(TicketsForm form){
        try {
            Events events = validate(form);
            for(int i=0; i < form.getNumberOfTicket(); i++){
                Tickets newTicket = new Tickets(0, form.getOperator(), generateQRcode(), false, new Date(), null, events);
                repo.save(newTicket);
            }                
        } catch (MetrixException e) {
            e.printStackTrace();
        }
    }

    private String generateQRcode(){
        //generate qr code but do while loop, you can assume it will never endless. Safe guard by only have max 100 seat
        boolean isDuplicated = true;
        String qrCode = "";
        do{
            qrCode = UUID.randomUUID().toString();
            if(!repo.getByQrCode(qrCode).isPresent()){
                isDuplicated = false;
            }
        }while(isDuplicated);
        return qrCode;
    }    

    private Events validate(TicketsForm form) throws MetrixException{
        Optional<Events> oEvent = eventsSvc.retrieve(form.getEventId());
        if(!oEvent.isPresent()){
            throw new MetrixException(-1, String.format("TicketReceiver::validate()::Event with Id [%d] not found", form.getEventId()), "");
        } else {
            Events event = oEvent.get();
            if(!event.isStartSell()){
                throw new MetrixException(-1, String.format("TicketReceiver::validate()::Event with Id [%d] not started yet", form.getEventId()), "");
            }
            int numOfSeat = event.getVenue().getNumberOfSeat();
            int ticketSold = repo.getByEventsId(event.getId()).size();
            int ticketNeeded = form.getNumberOfTicket();
            if(ticketSold + ticketNeeded > numOfSeat){
                throw new MetrixException(-1, String.format("TicketReceiver::validate()::Event with Id [%d] not started yet", form.getEventId()), "");
            }
            return event;  
        }
    }    
}
