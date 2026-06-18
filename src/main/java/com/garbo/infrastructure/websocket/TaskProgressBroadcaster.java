package com.garbo.infrastructure.websocket;

import com.garbo.api.dto.websocket.TaskProgressUpdatePayload;
import com.garbo.api.dto.websocket.WebSocketMessage;
import com.garbo.core.entity.UserTaskProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TaskProgressBroadcaster {

    @Autowired
    private WebSocketSessionManager webSocketSessionManager;

    public void broadcastTaskProgressUpdate(
            Long userId,
            Long binId,
            int totalActivityCount,
            List<UserTaskProgress> updatedTasks
    ) {
        if (userId == null || updatedTasks == null || updatedTasks.isEmpty()) {
            return;
        }

        List<TaskProgressUpdatePayload.TaskProgressItem> taskItems = new ArrayList<>();
        for (UserTaskProgress progress : updatedTasks) {
            if (progress.getTask() == null) {
                continue;
            }
            taskItems.add(new TaskProgressUpdatePayload.TaskProgressItem(
                    progress.getTask().getId(),
                    progress.getTask().getCode(),
                    progress.getTask().getTitle(),
                    progress.getTask().getDescription(),
                    progress.getTask().getBasePoints(),
                    progress.getCurrentProgress(),
                    progress.getTargetProgress(),
                    progress.isCompleted(),
                    progress.getCompletedAt() != null
                            ? progress.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null,
                    progress.getPointsEarned()
            ));
        }

        if (taskItems.isEmpty()) {
            return;
        }

        TaskProgressUpdatePayload payload = new TaskProgressUpdatePayload(
                userId,
                binId,
                totalActivityCount,
                System.currentTimeMillis(),
                taskItems
        );

        webSocketSessionManager.sendToUser(
                userId,
                new WebSocketMessage<>("TASK_PROGRESS_UPDATE", userId, payload)
        );

        log.debug(
                "Sent TASK_PROGRESS_UPDATE to userId={} with {} task item(s)",
                userId,
                taskItems.size()
        );
    }
}
