/**
 * Copyright 2018 Connexta, LLC
 *
 * <p>Unlimited Government Rights (FAR Subpart 27.4) Government right to use, disclose, reproduce,
 * prepare derivative works, distribute copies to the public, and perform and display publicly, in
 * any manner and for any purpose, and to have or permit others to do so.
 */
package org.codice.ddf.security.util;

import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.platform.util.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SAMLUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(SAMLUtils.class);

  private static final Pattern SAML_PREFIX = Pattern.compile("<(?<prefix>\\w+?):Assertion\\s.*");

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private static final String EVIDENCE =
      "<%1$s:Evidence xmlns:%1$s=\"urn:oasis:names:tc:SAML:2.0:assertion\">%2$s</%1$s:Evidence>";

  private static SecurityManager securityManager;

  public static Subject getSubjectFromSAML(SecurityToken securityToken) {

    Subject returnSubject = null;
    try {
      returnSubject = securityManager.getSubject(securityToken);
    } catch (SecurityServiceException e) {
      LOGGER.warn("Could not convert SAML Auth Token into a Subject. Caught exception: ", e);
    }
    return returnSubject;
  }

  public static SecurityToken getSecurityTokenFromSAMLAssertion(String samlAssertion) {
    SecurityToken securityToken = new SecurityToken();
    Element thisToken;

    try {
      thisToken = StaxUtils.read(new StringReader(samlAssertion)).getDocumentElement();
    } catch (XMLStreamException e) {
      LOGGER.info(
              "Unexpected error converting XML string to element - proceeding without SAML token.", e);
      thisToken = parseAssertionWithoutNamespace(samlAssertion);
    }

    securityToken.setToken(thisToken);
    return securityToken;
  }

  public static String getSubjectAsString(Element subject) {
    return DOM2Writer.nodeToString(subject);
  }

  public static String getSubjectAsStringNoSignature(Element subject) {
    subject.normalize();
    Node signatureElement = subject.getElementsByTagNameNS("*", "Signature").item(0);
    subject.removeChild(signatureElement);
    return DOM2Writer.nodeToString(subject);

  }

  public static Element parseAssertionWithoutNamespace(String assertion) {
    Element result = null;

    Matcher prefix = SAML_PREFIX.matcher(assertion);
    if (prefix.find()) {

      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();
      thread.setContextClassLoader(SAMLUtils.class.getClassLoader());

      try {
        DocumentBuilderFactory dbf = XML_UTILS.getSecureDocumentBuilderFactory();
        dbf.setNamespaceAware(true);

        String evidence = String.format(EVIDENCE, prefix.group("prefix"), assertion);

        Element root =
            dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(evidence.getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement();

        result = ((Element) root.getChildNodes().item(0));
      } catch (ParserConfigurationException | SAXException | IOException ex) {
        LOGGER.info("Unable to parse SAML assertion", ex);
      } finally {
        thread.setContextClassLoader(loader);
      }
    }

    return result;
  }

  public void setSecurityManager(SecurityManager securityManager) {
    SAMLUtils.securityManager = securityManager;
  }
}
