package com.leets.k_beauty.domain.session.service;

import com.leets.k_beauty.domain.session.dto.SessionCreateResponse;
import com.leets.k_beauty.domain.session.dto.SessionCurrentResponse;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.global.exception.SessionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;

    @Transactional
    public SessionCreateResponse createSession() {
        String rawToken = UUID.randomUUID().toString();
        Session session = Session.create(rawToken);
        sessionRepository.save(session);
        return SessionCreateResponse.from(session, rawToken);
    }

    public SessionCurrentResponse getCurrentSession(String sessionToken) {
        Session session = findByToken(sessionToken);
        return SessionCurrentResponse.from(session);
    }

    @Transactional
    public SessionCreateResponse restartSession(String sessionToken) {
        Session oldSession = findByToken(sessionToken);
        oldSession.validateInProgress();
        oldSession.markAsRestarted();

        String newRawToken = UUID.randomUUID().toString();
        Session newSession = Session.create(newRawToken);
        sessionRepository.save(newSession);
        return SessionCreateResponse.from(newSession, newRawToken);
    }

    private Session findByToken(String sessionToken) {
        String hash = Session.hashToken(sessionToken);
        return sessionRepository.findBySessionTokenHash(hash)
                .orElseThrow(SessionNotFoundException::new);
    }
}
