package com.store.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class WebConfigTest {

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "uploadDir", "public/images/");
    }

    @Test
    void addResourceHandlers_registersImagesHandlerWithUploadDirLocation() {
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);
        when(registry.addResourceHandler("/images/**")).thenReturn(registration);

        webConfig.addResourceHandlers(registry);

        verify(registry).addResourceHandler("/images/**");
        verify(registration).addResourceLocations("file:public/images/");
    }
}
