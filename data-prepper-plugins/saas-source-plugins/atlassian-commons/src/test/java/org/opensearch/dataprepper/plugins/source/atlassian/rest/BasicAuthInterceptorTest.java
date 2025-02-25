/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */

package org.opensearch.dataprepper.plugins.source.atlassian.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.dataprepper.plugins.source.atlassian.AtlassianSourceConfig;
import org.opensearch.dataprepper.plugins.source.atlassian.configuration.AuthenticationConfig;
import org.opensearch.dataprepper.plugins.source.atlassian.configuration.BasicConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BasicAuthInterceptorTest {

    @Mock
    AuthenticationConfig authenticationConfig;
    @Mock
    BasicConfig basicConfig;
    @Mock
    private HttpRequest mockRequest;
    @Mock
    private ClientHttpRequestExecution mockExecution;
    @Mock
    private ClientHttpResponse mockResponse;
    @Mock
    private AtlassianSourceConfig mockConfig;
    @Mock
    private HttpHeaders mockHeaders;

    private BasicAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        when(mockConfig.getAuthenticationConfig()).thenReturn(authenticationConfig);
        when(authenticationConfig.getBasicConfig()).thenReturn(basicConfig);
        when(basicConfig.getUsername()).thenReturn("testUser");
        when(basicConfig.getPassword()).thenReturn("testPassword");
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
        interceptor = new BasicAuthInterceptor(mockConfig);
    }

    @Test
    void testInterceptAddsAuthorizationHeader() throws IOException {
        when(mockExecution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(mockResponse);

        ClientHttpResponse response = interceptor.intercept(mockRequest, new byte[0], mockExecution);

        verify(mockHeaders).set(eq(HttpHeaders.AUTHORIZATION), argThat(value ->
                value.startsWith("Basic ") &&
                        new String(Base64.getDecoder().decode(value.substring(6))).equals("testUser:testPassword")
        ));
        assertEquals(mockResponse, response);
    }

}
