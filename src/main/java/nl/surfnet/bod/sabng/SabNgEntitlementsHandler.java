/**
 * Copyright (c) 2012, SURFnet BV
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *     disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.surfnet.bod.sabng;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Iterator;

import javax.annotation.Resource;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import nl.surfnet.bod.util.Environment;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;

@Component
public class SabNgEntitlementsHandler {

  private static final String STATUS_SUCCESS = "Success";

  private static final String REQUEST_TEMPLATE_LOCATION = "/xmlsabng/request-entitlement-template.xml";
  private static final String XPATH_STATUS_CODE = "//samlp:StatusCode";
  private static final String XPATH_ENTITLEMENTS = "//saml:Attribute[@Name='urn:oid:1.3.6.1.4.1.5923.1.1.1.7']";

  @Resource
  private Environment bodEnvironment;

  public String createRequest(String issuer, String nameId) {
    String template;
    try {
      template = IOUtils.toString(this.getClass().getResourceAsStream(REQUEST_TEMPLATE_LOCATION), "UTF-8");
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    // TODO should ID="aaf23196-1773-2113-474a-fe114412ab72" also be
    // parameterized?
    return MessageFormat.format(template, issuer, nameId);
  }

  public boolean retrieveEntitlements(InputStream responseStream) {
    boolean result = false;

    Document document = createDocument(responseStream);

    try {
      checkStatusCodeOrFail(document, getStatusToMatch());
      result = checkEntitlements(document, getRoleToMatch());
    }
    catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }

    return result;
  }

  private String getRoleToMatch() {
    return bodEnvironment.getBodAdminEntitlement();
  }

  private String getStatusToMatch() {
    return STATUS_SUCCESS;
  }

  private Document createDocument(InputStream responseStream) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);

    Document document;
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      document = builder.parse(responseStream);
    }
    catch (ParserConfigurationException | SAXException | IOException e) {
      throw new RuntimeException(e);
    }
    return document;
  }

  private void checkStatusCodeOrFail(Document document, String statusToMatch) throws XPathExpressionException {
    XPath xPath = javax.xml.xpath.XPathFactory.newInstance().newXPath();

    xPath.setNamespaceContext(new SabNgNamespaceResolver());
    XPathExpression expression = xPath.compile(XPATH_STATUS_CODE);

    String statusCode = ((NodeList) expression.evaluate(document, XPathConstants.NODESET)).item(0).getAttributes()
        .getNamedItem("Value").getNodeValue();

    Preconditions.checkState(statusCode.contains(statusToMatch), "Could not retrieve roles, statusCode:", statusCode);
  }

  private boolean checkEntitlements(Document document, String roleToMatch) throws XPathExpressionException {
    boolean result = false;

    XPath xPath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(new SabNgNamespaceResolver());
    XPathExpression expression = xPath.compile(XPATH_ENTITLEMENTS);
    NodeList nodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

    if ((nodeList != null) && (nodeList.getLength() > 0)) {
      String entitlements = nodeList.item(0).getTextContent();
      result = entitlements.contains(roleToMatch);
    }
    return result;
  }

  private class SabNgNamespaceResolver implements NamespaceContext {

    @Override
    public String getNamespaceURI(String prefix) {

      switch (prefix) {
      case "SOAP-ENV":
        return "http://schemas.xmlsoap.org/soap/envelope/";
      case "samlp":
        return "urn:oasis:names:tc:SAML:2.0:protocol";
      case "saml":
        return "urn:oasis:names:tc:SAML:2.0:assertion";
      case "xsi":
        return "http://www.w3.org/2001/XMLSchema-instance";
      case "xs":
        return "http://www.w3.org/2001/XMLSchema";
      default:
        return XMLConstants.NULL_NS_URI;
      }

    }

    @Override
    public String getPrefix(String namespaceURI) {
      return null;
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
      return null;
    }

  }
}
