package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import com.kevinguanchedarias.owgejava.business.AuthenticationBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserSessionService {
    private final UserStorageRepository userStorageRepository;
    private final AuthenticationBo authenticationBo;

    @Transactional
    public UserStorage findLoggedInWithDetails() {
        var tokenSimpleUser = findLoggedIn();
        if (tokenSimpleUser != null) {
            var dbFullUser = SpringRepositoryUtil.findByIdOrDie(userStorageRepository, tokenSimpleUser.getId());

            if (!tokenSimpleUser.getEmail().equals(dbFullUser.getEmail())
                    || !tokenSimpleUser.getUsername().equals(dbFullUser.getUsername())) {
                dbFullUser.setEmail(tokenSimpleUser.getEmail());
                dbFullUser.setUsername(tokenSimpleUser.getUsername());
                userStorageRepository.save(dbFullUser);
            }
            return dbFullUser;
        } else {
            return null;
        }
    }

    /**
     * Finds the logged in user information ONLY the base one, and from token<br />
     * Only id, email, and username will be returned, used
     * findLoggedInWithDetailts() for everything
     *
     * @author Kevin Guanche Darias
     */
    public UserStorage findLoggedIn() {
        var token = authenticationBo.findTokenUser();
        return token != null ? convertTokenUserToUserStorage(token) : null;
    }

    public UserStorage findLoggedInWithReference() {
        return userStorageRepository.getReferenceById(findLoggedIn().getId());
    }

    private UserStorage convertTokenUserToUserStorage(TokenUser tokenUser) {
        var user = new UserStorage();
        user.setId(tokenUser.getId().intValue());
        user.setEmail(tokenUser.getEmail());
        user.setUsername(tokenUser.getUsername());
        return user;
    }
}
