package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.TranslationBo;
import com.kevinguanchedarias.owgejava.entity.Translatable;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import jakarta.persistence.PostLoad;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Component
@Slf4j
public class TranslatableListener {
    private static final List<String> SUPPORTED_LANGUAGES = new ArrayList<>();

    static {
        SUPPORTED_LANGUAGES.add("en");
        SUPPORTED_LANGUAGES.add("es");
    }

    private HttpServletRequest httpServletRequest;
    private TranslationBo translationBo;

    @Lazy
    public TranslatableListener(HttpServletRequest httpServletRequest, TranslationBo translationBo) {
        this.httpServletRequest = httpServletRequest;
        this.translationBo = translationBo;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostLoad
    public void onLoad(Translatable translatable) {
        if (httpServletRequest != null) {
            String headerValue = httpServletRequest.getHeader("X-Owge-Lang");
            if (StringUtils.isEmpty(headerValue)) {
                throw new SgtBackendInvalidInputException("Frontend is not sending the http lang header");
            } else {
                doTranslate(translatable, headerValue);
            }
        } else {
            log.warn(
                    "Unable to handle translation, as translatable has been invoked outside of a request. Most likely from a Quartz job");
        }
    }

    private void doTranslate(Translatable translatable, String headerValue) {
        String targetLang = SUPPORTED_LANGUAGES.contains(headerValue) ? headerValue : translatable.getDefaultLangCode();
        translatable.setTranslation(translationBo.findByIdAndLangCode(translatable.getId(), targetLang));
    }

}
