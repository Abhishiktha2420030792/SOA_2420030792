package com.carebridge.notification.service;

import com.carebridge.notification.dto.NotificationRequest;
import com.carebridge.notification.model.Notification;
import com.carebridge.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * For the hackathon demo, actual delivery is mocked via logging (clearly
     * visible in the console during a live demo). Swap the body of
     * dispatch() for a real JavaMailSender / Twilio call for production use
     * without touching the controller or any caller.
     */
    public Notification send(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setRecipientId(request.getRecipientId());
        notification.setType(request.getType());
        notification.setChannel(request.getChannel() == null ? "EMAIL" : request.getChannel());
        notification.setMessage(request.getMessage());

        boolean delivered = dispatch(notification);
        notification.setStatus(delivered ? "SENT" : "FAILED");

        return notificationRepository.save(notification);
    }

    private boolean dispatch(Notification notification) {
        try {
            log.info("=== NOTIFICATION [{}] via {} -> recipient {} ===\n{}",
                    notification.getType(), notification.getChannel(),
                    notification.getRecipientId(), notification.getMessage());
            // TODO: replace with real JavaMailSender / Twilio integration
            return true;
        } catch (Exception e) {
            log.error("Failed to dispatch notification", e);
            return false;
        }
    }

    public List<Notification> getByRecipient(String recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }
}
