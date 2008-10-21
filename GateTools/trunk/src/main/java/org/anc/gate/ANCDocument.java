/*-
 * Copyright (c) 2008 American National Corpus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.anc.gate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import gate.Annotation;
import gate.AnnotationSet;
import gate.DocumentContent;
import gate.Factory;
import gate.FeatureMap;
import gate.GateConstants;
import gate.LanguageResource;
import gate.Resource;
import gate.corpora.DocumentContentImpl;
import gate.creole.ResourceInstantiationException;
import gate.util.DocumentFormatException;
import gate.util.InvalidOffsetException;
import gate.util.Out;

//import org.xces.gate.AnaParser;
//import org.xces.gate.CesAnaParser;
//import org.xces.gate.Token;

/**
 * @author Keith Suderman
 * @version 1.0
 */
public class ANCDocument extends gate.corpora.DocumentImpl
    implements LanguageResource
{

  public static final String LOAD_STANDOFF_PARAMETER_NAME = "loadStandoff";
  public static final String STANDOFF_MARKUP_SET_NAME = "standoffASName";
  public static final String STANDOFF_ANNOTATIONS_PARAMETER_NAME = "standoffAnnotations";
  public static final String STANDOFF_LOADED_PARAMETER_NAME = "standoffLoaded";
  public static final String FACTORY_PROPERTY = "javax.xml.sax.SAXParserFactory";
  public static final String CONTENT_ENCODING_PARAMETER_NAME = "contentEncoding";

  private Boolean loadStandoff = Boolean.TRUE;
  private String standoffASName = "Original markups";
  private List standoffAnnotations = null;
  private boolean standoffLoaded = false;
  private String contentEncoding = "UTF-16";

  private Hashtable annotations = null;

  public void setLoadStandoff(Boolean save) { loadStandoff = save; }
  public Boolean getLoadStandoff() { return loadStandoff; }

  public void setStandoffLoaded(boolean loaded) { standoffLoaded = loaded; }
  public boolean getStandoffLoaded() { return standoffLoaded; }

  public void setStandoffASName(String name) { standoffASName = name; }
  public String getStandoffASName() { return standoffASName; }

  public void setStandoffAnnotations(List list) { standoffAnnotations = list; }
  public List getStandoffAnnotations() { return standoffAnnotations; }

  public void setContentEncoding(String encoding) { contentEncoding = encoding; }
  public String getContentEncoding() { return contentEncoding; }

  public ANCDocument()
  {
//    Out.prln("XCES document constructor.");
//    this.addDocumentListener(new XCESDocumentListener(this));
  }

  public Resource init() throws ResourceInstantiationException
  {
    super.init();
//    System.out.println("Creating an XCES docuement.");
//    if (true) throw new ResourceInstantiationException("WTF");

    URL url = getSourceUrl();
    File fullPath = new File(url.getPath());
    String basePath = fullPath.getParent().replaceAll("%20", "\\ ");
    AnnotationSet originalMarkups = getAnnotations("Original markups");
    if (originalMarkups == null || originalMarkups.size() == 0)
    {
       throw new ResourceInstantiationException(
           "No elements found the in the header file " + fullPath.getPath());
    }
    AnnotationSet annotationSet = originalMarkups.get("annotation");
    if (annotationSet == null || annotationSet.size() == 0)
    {
       throw new ResourceInstantiationException(
           "No annotation elements found in the header " + fullPath.getPath());
    }
//    System.out.println("Unpacking markup.");

    annotations = getAnnotationFiles(annotationSet);
    originalMarkups.clear();
//    originalMarkups.removeAll(new HashSet(annotationSet));

    // Get the text for the document
    String filename = getFileForType("content");
    if (filename == null)
    {
       throw new ResourceInstantiationException("No document content found.");
    }
    String theContent = getContent(basePath + "/" + filename);

    // Replace the DocumentContent.  The current DocumentContent contains the
    // ANC header file, which not what we want the user to see.
    DocumentContent docContent = new DocumentContentImpl(theContent);
    this.setContent(docContent);


    LoadStandoff loader = new LoadStandoff();
    loader.init();
    loader.setDocument(this);
    loader.setStandoffASName(standoffASName);
    Iterator it = standoffAnnotations.iterator();
    while (it.hasNext())
    {
      String type = (String) it.next();
      filename = getFileForType(type);
      if (filename != null)
      {
        try
        {
         loader.setSourceUrl(new URL("file://" + basePath + "/" + filename));
        }
        catch (MalformedURLException e)
        {
           throw new ResourceInstantiationException(e);
        }
      }
    }
    
    
    /*
    // Get a parser for the standoff annotations
    CesAnaParser parser = new CesAnaParser();
    if (standoffAnnotations != null)
    {
      AnnotationSet as = this.getAnnotations(standoffASName);
      parser.setAnnotationSet(as);
      Iterator it = standoffAnnotations.iterator();
      while (it.hasNext())
//      StringTokenizer tokens = new StringTokenizer(standoffAnnotations);
//      while (tokens.hasMoreTokens())
      {
        String type = (String) it.next();
//         String type = tokens.nextToken();
        filename = getFileForType(type);
        if (filename != null)
        {
          // System.out.println("Adding annotations from " + basePath + "/" + filename);
          parser.parse(basePath + "/" + filename);
        }
      }
    }
    */

    return this;
  }

//  public void reInit() throws ResourceInstantiationException
//  {
//    Out.prln("Re-initializing XCES document");
//    this.init();
//  }

//  public String toXml()
//  {
//    Out.prln("Getting xml for XCES document");
//    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><doc/>";
//  }
//
//  public String toXml(AnnotationSet annotations)
//  {
//    return this.toXml();
//  }
//
//  public String toXml(AnnotationSet annotations, boolean includeFeatures)
//  {
//    return this.toXml();
//  }

  protected String getFileForType(String type) throws ResourceInstantiationException
  {
    String result = (String) annotations.get(type);
    if (result == null)
    {
//      throw new ResourceInstantiationException(
          Out.prln("Could not find the " + type + " annotation element.");
    }
    return result;
  }

  protected String getContent(String path) throws ResourceInstantiationException
  {
    StringBuffer sbuffer = new StringBuffer();
    InputStreamReader reader = null;
    try
    {
       //String encoding = this.getEncoding();
//       if (encoding != null)
//       {
//          reader = new InputStreamReader(new FileInputStream(path), encoding);
//       }
//       else
//       {
       System.out.println("Loading content from " + path);
       reader = new InputStreamReader(new FileInputStream(path), contentEncoding);
//       }
       char[] cbuffer = new char[8192];
       int size = reader.read(cbuffer, 0, 8192);
       while (size > 0)
       {
          sbuffer.append(cbuffer, 0, size);
          size = reader.read(cbuffer, 0, 8192);
       }
       reader.close();
    }
    catch (IOException ex)
    {
       throw new ResourceInstantiationException(
           "Error reading the document content from " + path);
    }
    return sbuffer.toString();
  }

  protected Hashtable getAnnotationFiles(AnnotationSet set) throws ResourceInstantiationException
  {
    Hashtable table = new Hashtable();
    Iterator it = set.iterator();
    while (it.hasNext())
    {
      Annotation a = (Annotation) it.next();
      FeatureMap theFeatures = a.getFeatures();
      if (theFeatures == null || theFeatures.size() == 0)
      {
        throw new ResourceInstantiationException("The annotation element does not contain any attributes");
      }

      String type = (String) theFeatures.get("type");
      String location = (String) theFeatures.get("ann.loc");
      table.put(type, location);
    }
    return table;
  }

  protected Hashtable getAnnotationFiles(URL url) throws DocumentFormatException
  {
    Hashtable table = new Hashtable();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    org.w3c.dom.Document document = null;
    try
    {
      DocumentBuilder builder = factory.newDocumentBuilder();
      document = builder.parse(url.openStream());
    }
    catch (IOException ex)
    {
      throw new DocumentFormatException("Error reading the header file.", ex);
    }
    catch (SAXException ex)
    {
      throw new DocumentFormatException("Error parsing the header file.", ex);
    }
    catch (ParserConfigurationException ex)
    {
      throw new DocumentFormatException(
          "Error create the XML document builder.", ex);
    }

    NodeList nodes = document.getElementsByTagName("annotation");
    if (nodes == null || nodes.getLength() == 0)
    {
      throw new DocumentFormatException(
          "The header does not contain any annotation elements.");
    }

    int n = nodes.getLength();
    for (int i = 0; i < n; ++i)
    {
      org.w3c.dom.Node node = nodes.item(i);
      NamedNodeMap atts = node.getAttributes();
      if (atts != null)
      {
        org.w3c.dom.Node location = atts.getNamedItem("ann.loc");
        org.w3c.dom.Node type = atts.getNamedItem("type");
        table.put(type, location);
      }
    }
    return table;
  } 
}
