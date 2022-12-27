package com.kevinguanchedarias.owgejava.fake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@Service
@Primary
public class NonPostConstructSocketIoService extends SocketIoService {
    private static final ObjectMapper OBJECT_MAPPER_MOCK = mock(ObjectMapper.class);

    static {
        given(OBJECT_MAPPER_MOCK.copy()).willReturn(mock(ObjectMapper.class));
    }

    public NonPostConstructSocketIoService() {
        super(OBJECT_MAPPER_MOCK);
    }

    @Override
    public void init() {
        // Don't invoke init
    }

    @Override
    public void onContextReady() {
        // Don't invoke onContextReady
    }

    public void realInit() {
        super.init();
    }

    public void realContextReady() {
        super.onContextReady();
    }

    @Override
    public void destroy() {
        // Don't invoke
    }

    public void realDestroy() {
        super.destroy();
    }
}
