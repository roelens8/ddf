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

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.protocol.amqp.broker.AMQPMessage;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.codice.ddf.broker.security.api.BrokerMessageInterceptor;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.codice.ddf.security.util.SAMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SubjectInjectorPlugin implements BrokerMessageInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(SubjectInjectorPlugin.class);

  private static final Map<String, Subject> SUBJECT_CACHE = new ConcurrentHashMap<>();

  private static SecurityManager securityManager;

  private static Set<String> configuredAddresses;

  @Override
  public void handleMessage(
      ServerSession session,
      Transaction tx,
      Message message,
      boolean direct,
      boolean noAutoCreateQueue) {
    if (!configuredAddresses.contains(message.getAddress())) {
      return;
    }
    if (!SUBJECT_CACHE.containsKey(session.getUsername())) {
      UPAuthenticationToken usernamePasswordToken =
          new UPAuthenticationToken(session.getUsername(), session.getPassword());
      try {
        Subject subject = securityManager.getSubject(usernamePasswordToken);
        SUBJECT_CACHE.put(session.getUsername(), subject);
      } catch (SecurityServiceException e) {
        LOGGER.warn("Unable to retrieve the Subject from the token", e);
        return;
      }
    }
    String subjectAsString = getStringSubjectFromSession(session);
    message.putStringProperty("subject", subjectAsString);
    if (message instanceof AMQPMessage) {
      Map applicationPropertiesMap =
          ((AMQPMessage) message).getProtonMessage().getApplicationProperties().getValue();
      applicationPropertiesMap.put("subject", subjectAsString);
      ((AMQPMessage) message)
          .getProtonMessage()
          .setApplicationProperties(new ApplicationProperties(applicationPropertiesMap));
    }
  }

  @Override
  public void setConfiguredAddresses(Set<String> addresses) {
    configuredAddresses = new HashSet<>(addresses);
  }

  @Override
  public Set<String> getConfiguredAddresses() {
    return new HashSet<>(configuredAddresses);
  }

  private Element getSubjectAsElement(ServerSession session) {
    return SUBJECT_CACHE
        .get(session.getUsername())
        .getPrincipals()
        .oneByType(SecurityAssertion.class)
        .getSecurityToken()
        .getToken();
  }

  String getStringSubjectFromSession(ServerSession session) {
    return SAMLUtils.getSubjectAsStringNoSignature(getSubjectAsElement(session));
  }

  public void setSecurityManager(SecurityManager securityManager) {
    SubjectInjectorPlugin.securityManager = securityManager;
  }

  public SecurityManager getSecurityManager() {
    return securityManager;
  }

  public void clearCache() {
    SUBJECT_CACHE.clear();
  }

  static Map<String, Subject> getSubjectCache() {
    return SUBJECT_CACHE;
  }
}
