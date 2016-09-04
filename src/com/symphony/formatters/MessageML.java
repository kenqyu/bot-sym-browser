/*
 *
 *
 * Copyright 2016 Symphony Communication Services, LLC
 *
 * Licensed to Symphony Communication Services, LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package com.symphony.formatters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Written by Ryan D'souza
 *
 * Used for creating messages adhering to Symphony's MessageML specification
 * Uses an XML DocumentBuilder internally
 */

public class MessageML {

  private static final Logger LOG = LoggerFactory.getLogger(MessageML.class);

  private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
  private DocumentBuilder documentBuilder;
  private Document document;
  private Element rootElement;

  private int numChildren;

  public MessageML() {

    try {
      this.documentBuilder = builderFactory.newDocumentBuilder();
      this.document = this.documentBuilder.newDocument();

      this.rootElement = this.document.createElement("messageML");
      this.document.appendChild(this.rootElement);
      this.numChildren = 0;
    } catch (ParserConfigurationException exception) {
      throw new RuntimeException("Unable to create XML document", exception);
    }
  }

  public static void main(String[] ryan) {

    MessageML messageML = new MessageML();

    messageML.addBoldText("Bold text");
    messageML.addLineBreak();
    messageML.addItalicText("Italic text");
    messageML.addLineBreak();
    messageML.addBulletPoints("Item 1", "Item 2", "Item 3");
    messageML.addCashTag("HELLO");
    messageML.addHashTag("yolo");
    System.out.println(messageML.toString());
  }

  /**
   * Helper method to add a simple tag
   * @param parentElement
   * @param tag
   * @param text
   */
  private void addSimpleTag(Element parentElement, String tag, String text) {
    Element element = this.document.createElement(tag);
    Text elementText = this.document.createTextNode(text);

    element.appendChild(elementText);
    parentElement.appendChild(element);
    this.numChildren++;
  }

  /**
   * Helper method to add a simple tag to the root node
   * @param tag
   * @param text
   */
  private void addSimpleTag(String tag, String text) {
    this.addSimpleTag(this.rootElement, tag, text);
  }

  /**
   * Adds a paragraph - plain text
   * @param text
   */
  public void addParagraph(String text) {
    Text elementText = this.document.createTextNode(text);
    this.rootElement.appendChild(elementText);
    this.numChildren++;
    //this.addSimpleTag("p", text);
  }

  /**
   * Adds bold text
   * @param text
   */
  public void addBoldText(String text) {
    this.addSimpleTag("b", text);
  }

  /**
   * Adds italic text
   * @param text
   */
  public void addItalicText(String text) {
    this.addSimpleTag("i", text);
  }

  /**
   * Helper method to add a simple tag to the root
   * @param tag
   */
  private void addSimpleTag(String tag) {
    this.addSimpleTag(tag, this.rootElement);
  }

  /**
   * Adds a line break
   */
  public void addLineBreak() {
    this.addSimpleTag("br");
  }

  /**
   * Adds a chime
   */
  public void addChime() {
    this.addSimpleTag("chime");
  }

  /**
   * Helper method to add a simple tag
   * @param tag
   * @param root
   */
  private void addSimpleTag(String tag, Element root) {
    Element element = this.document.createElement(tag);
    root.appendChild(element);
    this.numChildren++;
  }

  /**
   * Helper method to append a tag
   * @param attributeName
   * @param tagType
   * @param text
   */
  private void appendTag(String attributeName, String tagType, String text) {
    Element cashTag = this.document.createElement(tagType);
    Attr tagAttribute = this.document.createAttribute(attributeName);
    tagAttribute.setValue(text);
    cashTag.setAttributeNode(tagAttribute);
    this.rootElement.appendChild(cashTag);
    this.numChildren++;
  }

  /**
   * Adds a cash tag for the text - similar function to a hashtag but with '$'
   * @param text
   */
  public void addCashTag(String text) {
    this.appendTag("tag", "cash", text);
  }

  /**
   * Adds a hashtag for the text
   * @param text
   */
  public void addHashTag(String text) {
    this.appendTag("tag", "hash", text);
  }

  /**
   * Adds a link with special formatting to show up in blue - no ahref text allowed
   * @param link
   */
  public void addLink(String link) {
    this.appendTag("href", "a", link);
  }

  /**
   * Adds the items as bullet points
   * @param items
   */
  public void addBulletPoints(String... items) {
    Element parentElement = this.document.createElement("ul");

    for (String item : items) {
      Element listedItem = this.document.createElement("li");
      Text text = this.document.createTextNode(item);
      listedItem.appendChild(text);
      parentElement.appendChild(listedItem);
      this.numChildren++;
    }

    this.rootElement.appendChild(parentElement);
  }

  @Override
  public String toString() {

    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      StreamResult result = new StreamResult(new StringWriter());
      DOMSource source = new DOMSource(this.document);
      transformer.transform(source, result);
      String xmlString = result.getWriter().toString();

      String resultString = xmlString.replaceAll("[\u0000-\u001f]", "");

      if (resultString.equals("<messageML/>")) {
        return "";
      }

      return resultString;
    } catch (TransformerException exception) {
      LOG.error("Error converting messageML to string: ", exception);
      return "<messageML> Error </messageML>";
      //throw new RuntimeException("Unable to convert PresentationML to string", exception);
    }
  }

  /**
   * Returns the number of children in the tree
   * @return
   */
  public int getNumChildren() {
    return this.numChildren;
  }
}