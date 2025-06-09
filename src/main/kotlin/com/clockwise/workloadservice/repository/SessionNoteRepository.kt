package com.clockwise.workloadservice.repository

import com.clockwise.workloadservice.model.SessionNote
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SessionNoteRepository : CoroutineCrudRepository<SessionNote, String> {
    
    fun findByWorkSessionId(workSessionId: String): Flow<SessionNote>
} 