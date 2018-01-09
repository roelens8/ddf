/**
 * Copyright (c) Codice Foundation
 *
 * <p>
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.protocol.amqp.broker.AMQPMessage;
import org.codice.ddf.platform.util.XMLUtils;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class SubjectInjectorPluginTest {
  Subject mockSubject;

  SecurityManager mockSecurityManager;

  ServerSession mockServerSession;

  SubjectInjectorPlugin securityServerPlugin;

  String SAML_ASSERTION =
      "<saml2:Assertion ID=\"_30a958fd-9283-4169-a65c-f79a33cec296\" IssueInstant=\"2017-12-21T22:41:17.713Z\" Version=\"2.0\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"saml2:AssertionType\"><saml2:Issuer>localhost</saml2:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">super secret signature don't tell anyone</ds:Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\" NameQualifier=\"http://cxf.apache.org/sts\">admin</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"/></saml2:Subject><saml2:Conditions NotBefore=\"2017-12-21T22:41:17.714Z\" NotOnOrAfter=\"2017-12-21T23:11:17.714Z\"><saml2:AudienceRestriction><saml2:Audience>https://localhost:8993/services/SecurityTokenService?wsdl</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AuthnStatement AuthnInstant=\"2017-12-21T22:41:17.713Z\" SessionIndex=\"135416475\"><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement><saml2:AttributeStatement><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">admin@localhost.local</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">group</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">admin</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">manager</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">viewer</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">system-admin</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">systembundles</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">admin</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

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
    securityServerPlugin.setConfiguredAddresses(
        new HashSet<>(Collections.singletonList("test.address")));
  }

  @Test
  public void testGoodSubjectNonAmqp() throws SecurityServiceException {

    securityServerPlugin.clearCache();
    Message message = new CoreMessage();
    message.setAddress("test.address");

    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is("Hello World"));
  }

  @Test
  public void testGoodSubjectAmqp() {

    securityServerPlugin.clearCache();
    Message message = new AMQPMessage(1);
    message.setAddress("test.address");
    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is("Hello World"));
  }

  @Test
  public void testPopulatedCache() {

    Message message = new AMQPMessage(1);

    message.setAddress("test.address");
    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is("Hello World"));
  }

  @Test
  public void testBadSubject() throws SecurityServiceException {
    securityServerPlugin.clearCache();
    when(mockSecurityManager.getSubject(any(Object.class)))
        .thenThrow(new SecurityServiceException());
    Message message = new CoreMessage();

    message.setAddress("test.address");

    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is(nullValue()));
  }

  @Test
  public void testNotApplicableAddress() throws SecurityServiceException {

    securityServerPlugin.setConfiguredAddresses(new HashSet<>());

    securityServerPlugin.clearCache();
    Message message = new CoreMessage();
    message.setAddress("test.address");

    securityServerPlugin.handleMessage(mockServerSession, null, message, false, false);
    assertThat(message.getStringProperty("subject"), is(nullValue()));
  }

  @Test
  public void testRemoveSignature() throws IOException, SAXException, ParserConfigurationException {
    DocumentBuilderFactory dbf = XMLUtils.getInstance().getSecureDocumentBuilderFactory();
    dbf.setNamespaceAware(true);
    DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
    Document doc = documentBuilder.parse(new ByteArrayInputStream(SAML_ASSERTION.getBytes()));
    String returnSubject =
        securityServerPlugin.getSubjectAsStringNoSignature(doc.getDocumentElement());
    assertThat(returnSubject, is(returnSubject));
  }

  static class SubjectInjectorPluginTester extends SubjectInjectorPlugin {

    @Override
    String getSubjectAsString(ServerSession session) {
      return getSubjectCache().get(session.getUsername()) != null ? "Hello World" : null;
    }
  }
}
