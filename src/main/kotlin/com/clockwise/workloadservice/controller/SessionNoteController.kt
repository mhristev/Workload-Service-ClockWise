package com.clockwise.workloadservice.controller

import com.clockwise.workloadservice.dto.SessionNoteRequest
import com.clockwise.workloadservice.dto.SessionNoteResponse
import com.clockwise.workloadservice.service.SessionNoteService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/session-notes")
class SessionNoteController(private val sessionNoteService: SessionNoteService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createNote(
        @RequestBody request: SessionNoteRequest,
        authentication: Authentication?
    ): ResponseEntity<SessionNoteResponse> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        val note = async { sessionNoteService.createNote(request) }
        ResponseEntity(note.await(), HttpStatus.CREATED)
    }

    @GetMapping("/work-session/{workSessionId}")
    suspend fun getNotesByWorkSessionId(
        @PathVariable workSessionId: String,
        authentication: Authentication?
    ): ResponseEntity<Flow<SessionNoteResponse>> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        val notes = async { sessionNoteService.getNotesByWorkSessionId(workSessionId) }
        ResponseEntity(notes.await(), HttpStatus.OK)
    }
} 