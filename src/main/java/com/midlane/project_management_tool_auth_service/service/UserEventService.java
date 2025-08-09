package com.midlane.project_management_tool_auth_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midlane.project_management_tool_auth_service.dto.UserEventDto;
import com.midlane.project_management_tool_auth_service.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventService {

    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.user.added:user.added}")
    private String userAddedTopic;

    public void publishUserEvent(User user, String eventType) {
        try {
            UserEventDto userEvent = new UserEventDto(
                user.getUserId(),
                user.getEmail(),
                eventType
            );

            String message = objectMapper.writeValueAsString(userEvent);
            kafkaProducerService.sendMessageWithKey(
                userAddedTopic,
                user.getUserId().toString(),
                message
            );

            log.info("Published user event: {} for user: {}", eventType, user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish user event: {} for user: {}", eventType, user.getEmail(), e);
        }
    }
}
