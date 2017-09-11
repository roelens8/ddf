/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.broker.security.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.protocol.amqp.broker.AMQPMessage;
import org.junit.Before;
import org.junit.Test;

public class SubjectInjectorPluginTest {
  Subject mockSubject;

  SecurityManager mockSecurityManager;

  ServerSession mockServerSession;

  SubjectInjectorPlugin securityServerPlugin;

  @Before
  public void setVariables() throws SecurityServiceException {
    mockSubject = mock(Subject.class);
    mockSecurityManager = mock(SecurityManager.class);
    when(mockSecurityManager.getSubject(any(Object.class))).thenReturn(mockSubject);
    mockServerSession = mock(ServerSession.class);
    when(mockServerSession.getUsername()).thenReturn("hello");
    when(mockServerSession.getPassword()).thenReturn("world");

    securityServerPlugin = new SubjectInjectorPluginTester();
    securityServerPlugin.setSecurityManager(mockSecurityManager);
  }

  @Test
  public void testGoodSubjectNonAmqp() throws SecurityServiceException {

    securityServerPlugin.clearCache();
    Message message = new CoreMessage();

    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is("Hello World"));
  }

  @Test
  public void testGoodSubjectAmqp() {

    securityServerPlugin.clearCache();
    Message message = new AMQPMessage(1);
    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is("Hello World"));
  }

  @Test
  public void testPopulatedCache() {

    Message message = new AMQPMessage(1);
    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is("Hello World"));
  }

  @Test
  public void testBadSubject() throws SecurityServiceException {
    securityServerPlugin.clearCache();
    when(mockSecurityManager.getSubject(any(Object.class)))
        .thenThrow(new SecurityServiceException());
    Message message = new CoreMessage();

    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is(nullValue()));
  }

  static class SubjectInjectorPluginTester extends SubjectInjectorPlugin {

    @Override
    String getSubjectAsString(ServerSession session) {
      return getSubjectCache().get(session.getUsername()) != null ? "Hello World" : null;
    }
  }
}
