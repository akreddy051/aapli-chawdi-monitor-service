package com.example.aapliChawdi.service;

import com.example.aapliChawdi.entity.UserSession;
import com.example.aapliChawdi.enums.SessionState;
import com.example.aapliChawdi.repository.UserSessionRepositoryInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepositoryInterface userSessionRepo;

    public UserSession getSession(Long chatId) {
        return userSessionRepo.findById(chatId)
                .orElseGet(() -> {
                    UserSession session = new UserSession();
                    session.setChatId(chatId);
                    session.setState(SessionState.IDLE); // ← no more .name()
                    return userSessionRepo.save(session);
                });
    }

    public void reset(UserSession session) {
        session.setState(SessionState.IDLE); // ← no more .name()
        session.setDistrict(null);
        session.setTaluka(null);
        userSessionRepo.save(session);
    }

    public void save(UserSession session) {
        userSessionRepo.save(session);
    }
}