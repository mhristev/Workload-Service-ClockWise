package com.clockwise.workloadservice.service

import com.clockwise.workloadservice.dto.SessionNoteRequest
import com.clockwise.workloadservice.dto.SessionNoteResponse
import com.clockwise.workloadservice.model.SessionNote
import com.clockwise.workloadservice.repository.SessionNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service

@Service
class SessionNoteService(private val sessionNoteRepository: SessionNoteRepository) {

    suspend fun createNote(request: SessionNoteRequest): SessionNoteResponse {
        val note = SessionNote(
            workSessionId = request.workSessionId,
            content = request.content
        )
        
        val savedNote = sessionNoteRepository.save(note)
        return mapToResponse(savedNote)
    }

    fun getNotesByWorkSessionId(workSessionId: String): Flow<SessionNoteResponse> {
        return sessionNoteRepository.findByWorkSessionId(workSessionId)
            .map { mapToResponse(it) }
    }

    private fun mapToResponse(sessionNote: SessionNote): SessionNoteResponse {
        return SessionNoteResponse(
            id = sessionNote.id,
            workSessionId = sessionNote.workSessionId,
            content = sessionNote.content,
            createdAt = sessionNote.createdAt
        )
    }
} 