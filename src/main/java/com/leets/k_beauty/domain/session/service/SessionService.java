package com.leets.k_beauty.domain.session.service;

import com.leets.k_beauty.domain.session.dto.SessionCreateResponse;
import com.leets.k_beauty.domain.session.dto.SessionCurrentResponse;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.global.exception.SessionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;

    @Transactional
    public SessionCreateResponse createSession() {
        Session session = Session.create();
        sessionRepository.save(session);
        return SessionCreateResponse.from(session);
    }

    public SessionCurrentResponse getCurrentSession(String sessionToken) {
        Session session = findByToken(sessionToken);
        return SessionCurrentResponse.from(session);
    }

    @Transactional
    public SessionCreateResponse restartSession(String sessionToken) {
        Session oldSession = findByToken(sessionToken);
        oldSession.markAsRestarted();

        Session newSession = Session.create();
        sessionRepository.save(newSession);
        return SessionCreateResponse.from(newSession);
    }

    private Session findByToken(String sessionToken) {
        return sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(SessionNotFoundException::new);
    }
}
