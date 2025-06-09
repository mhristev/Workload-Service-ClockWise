package com.clockwise.workloadservice.service

import com.clockwise.workloadservice.dto.WorkHoursResponse
import com.clockwise.workloadservice.dto.WorkSessionResponse
import com.clockwise.workloadservice.model.WorkSession
import com.clockwise.workloadservice.model.WorkSessionStatus
import com.clockwise.workloadservice.repository.WorkSessionRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class WorkSessionService(private val workSessionRepository: WorkSessionRepository) {

    suspend fun clockIn(userId: String, shiftId: String): WorkSessionResponse {
        // Check if this shift ID already exists in the database
        if (workSessionRepository.existsByShiftId(shiftId)) {
            throw IllegalStateException("A work session for shift $shiftId already exists")
        }
        
        // Check if there's already an active session for this user and shift
        val existingSession = workSessionRepository.findActiveByUserIdAndShiftId(userId, shiftId)
        
        if (existingSession != null) {
            // Already clocked in, return the existing session
            return mapToResponse(existingSession)
        }
        
        // Create new work session
        val newSession = WorkSession(
            userId = userId,
            shiftId = shiftId,
            clockInTime = OffsetDateTime.now()
        )
        
        val savedSession = workSessionRepository.save(newSession)
        return mapToResponse(savedSession)
    }

    suspend fun clockOut(userId: String, shiftId: String): WorkSessionResponse {
        val session = workSessionRepository.findActiveByUserIdAndShiftId(userId, shiftId)
            ?: throw IllegalStateException("No active work session found for user $userId and shift $shiftId")
        
        val now = OffsetDateTime.now()
        val totalMinutes = ChronoUnit.MINUTES.between(session.clockInTime, now).toInt()
        
        val updatedSession = session.copy(
            clockOutTime = now,
            totalMinutes = totalMinutes,
            status = WorkSessionStatus.COMPLETED,
            updatedAt = now
        )
        
        val savedSession = workSessionRepository.save(updatedSession)
        return mapToResponse(savedSession)
    }

    suspend fun getEmployeeWorkHours(
        userId: String, 
        startDate: OffsetDateTime, 
        endDate: OffsetDateTime
    ): WorkHoursResponse {
        val sessions = workSessionRepository.findByUserIdAndClockInTimeBetween(userId, startDate, endDate)
            .toList()
        
        val sessionResponses = sessions.map { mapToResponse(it) }
        val totalMinutes = sessions.sumOf { it.totalMinutes ?: 0 }
        
        return WorkHoursResponse(
            userId = userId,
            totalSessions = sessions.size,
            totalMinutesWorked = totalMinutes,
            sessions = sessionResponses
        )
    }

    private fun mapToResponse(workSession: WorkSession): WorkSessionResponse {
        return WorkSessionResponse(
            id = workSession.id,
            userId = workSession.userId,
            shiftId = workSession.shiftId,
            clockInTime = workSession.clockInTime,
            clockOutTime = workSession.clockOutTime,
            totalMinutes = workSession.totalMinutes,
            status = workSession.status
        )
    }
} 