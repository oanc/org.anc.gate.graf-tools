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

import gate.AnnotationSet;
import gate.DocumentContent;
import gate.Factory;
import gate.FeatureMap;
import gate.LanguageResource;
import gate.Resource;
import gate.corpora.DocumentContentImpl;
import gate.creole.ResourceInstantiationException;
import gate.util.DocumentFormatException;
import gate.util.InvalidOffsetException;
import gate.util.Out;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xces.graf.io.GraphParser;
import org.xces.standoff.Annotation;
import org.xces.standoff.Annotation.Feature;
import org.xces.standoff.AnnotationParser;
import org.xml.sax.SAXException;

/**
 * @author Keith Suderman
 * @version 1.0
 */
public class ANCDocument extends gate.corpora.DocumentImpl implements LanguageResource
{
   private static final long serialVersionUID = 1L;
   
   public static final String LOAD_STANDOFF_PARAMETER_NAME = "loadStandoff";
   public static final String STANDOFF_MARKUP_SET_NAME = "standoffASName";
   public static final String STANDOFF_ANNOTATIONS_PARAMETER_NAME = "standoffAnnotations";
   public static final String STANDOFF_LOADED_PARAMETER_NAME = "standoffLoaded";
   public static final String FACTORY_PROPERTY = "javax.xml.sax.SAXParserFactory";
   public static final String CONTENT_ENCODING_PARAMETER_NAME = "contentEncoding";

   private Boolean loadStandoff = Boolean.TRUE;
   private String standoffASName = "Original markups";
   private List<String> standoffAnnotations = null;
   private boolean standoffLoaded = false;
   private String contentEncoding = "UTF-16";

   private Hashtable<String, String> annotations = null;

   public void setLoadStandoff(Boolean save)
   {
      loadStandoff = save;
   }

   public Boolean getLoadStandoff()
   {
      return loadStandoff;
   }

   public void setStandoffLoaded(boolean loaded)
   {
      standoffLoaded = loaded;
   }

   public boolean getStandoffLoaded()
   {
      return standoffLoaded;
   }

   public void setStandoffASName(String name)
   {
      standoffASName = name;
   }

   public String getStandoffASName()
   {
      return standoffASName;
   }

   public void setStandoffAnnotations(List<String> list)
   {
      standoffAnnotations = list;
   }

   public List<String> getStandoffAnnotations()
   {
      return standoffAnnotations;
   }

   public void setContentEncoding(String encoding)
   {
      contentEncoding = encoding;
   }

   public String getContentEncoding()
   {
      return contentEncoding;
   }

   public ANCDocument()
   {
   }

   @Override
   public Resource init() throws ResourceInstantiationException
   {
      super.init();
      //from the gate gui
      URL url = getSourceUrl();
      //get the path of url
      File fullPath = new File(url.getPath());
      //get directory of url - minus the file name, get rid of any spaces
      String basePath = fullPath.getParent().replaceAll("%20", "\\ ");
      System.out.println("basePath is " + basePath);
      //originalMarkups is  the GATE Annotation set ( not anc )
      AnnotationSet originalMarkups = getAnnotations("Original markups");
      //if empty gate annotation set; throw ResourceInstantiationException
      if (originalMarkups == null || originalMarkups.size() == 0)
      {
         throw new ResourceInstantiationException("No elements found the in the header file "
               + fullPath.getPath());
      }
      //from originalMarkups get the Gate annotation set tied to key "annotation"
      AnnotationSet annotationSet = originalMarkups.get("annotation");
      //if annotationSet is empty, throw ResourceInstantiationException, get out
      if (annotationSet == null || annotationSet.size() == 0)
      {
         throw new ResourceInstantiationException("No annotation elements found in the header "
               + fullPath.getPath());
      }
      //System.out.println("Unpacking markup.");

      //annotations is a hashtable<String,String>, where the type is the key and string file name
      //of the stand off files are the values
      annotations = getAnnotationFiles(annotationSet);
      //clear the original markups ? why ?
      originalMarkups.clear();
      //originalMarkups.removeAll(new HashSet(annotationSet));

      // Get the text for the document
      String filename = getFileForType("content");
      //file should be there...
      if (filename == null)
      {
         throw new ResourceInstantiationException("No document content found.");
      }
      //get the original text, see getContent below
      String theContent = getContent(basePath + "/" + filename);

      // Replace the DocumentContent.  The current DocumentContent contains the
      // ANC header file, which not what we want the user to see.
      //make a new gate Document Content object with the original text
      DocumentContent docContent = new DocumentContentImpl(theContent);
      //set it to this object, and move on, nothing to see here...
      this.setContent(docContent);

/*
 * LoadStandoff loader = new LoadStandoff(); loader.init();
 * loader.setDocument(this); loader.setStandoffASName(standoffASName); Iterator
 * it = standoffAnnotations.iterator(); while (it.hasNext()) { String type =
 * (String) it.next(); filename = getFileForType(type); if (filename != null) {
 * try { URL standoff = new URL("file://" + basePath + "/" + filename);
 * System.out.println("Loading standoff from " + standoff.getPath());
 * loader.setSourceUrl(standoff); loader.execute(); } catch (Exception e) {
 * throw new ResourceInstantiationException(e); } } }
 */
      // Get a parser for the standoff annotations
      AnnotationParser parser = new AnnotationParser();
      try
      {
        GraphParser graphParser = new GraphParser();
     
      //stand-off annotations is a List coming from the gate gui, 
      //if not empty ..what happens if empty? how is it filled, if not by user ?
      System.out.println("standoffAnnotations.size() is " + standoffAnnotations.size());
      if (standoffAnnotations != null)
      {
         //This is a gate Annotation set made using standoffASName, that comes from the
         //gate GUI, Gate uses the setStandoffASName above to fill it, getAnnotations is a gate method, not ours
         AnnotationSet as = this.getAnnotations(standoffASName);
//      parser.setAnnotationSet(as);
         //get an iterator from the standoffAnnotations; ie iterate through the stand off file names
         Iterator<String> it = standoffAnnotations.iterator();
         while (it.hasNext())
//      StringTokenizer tokens = new StringTokenizer(standoffAnnotations);
//      while (tokens.hasMoreTokens())
         {
            //ok get the file name for each standoff file type, remember we are working with a *.anc file here..
            String type = it.next();
//          String type = tokens.nextToken();
            filename = getFileForType(type);
            System.out.println("filename is " + filename);
            if (filename != null)
            {
               System.out.println("Adding annotations from " + basePath + "/" + filename);
               List<Annotation> newAnnotations = null;// = new LinkedList<Annotation>();
               try
               {
                  //get the file name from this iteration, call the parser for that file now...
                  //and pull out the annotations from this iterations standoff file. 
                  newAnnotations = parser.parse(basePath + "/" + filename);
                //  IGraph graph =  graphParser.parse(basePath + "/" + filename);
               }
               //obligatory catch block
               catch (Exception e)
               {
                  throw new ResourceInstantiationException(e);
               }
               //now that we have the annotations from this stand off file, 
               //cycle through them to create a feature map for each annotation
               Out.println("newAnnotations.size is " + newAnnotations.size());
               for (Annotation a : newAnnotations)
               {
                  //get the start of the annotation, which gate will need
                  long start = a.getStart();
                  //get the end of the annotation, which gate will need
                  long end = a.getEnd();
                  //this factory is a gate factory, to make a new gate feature map
                  FeatureMap newFeatures = Factory.newFeatureMap();
                  //cycle through the features using the anc annotation.getFeatures ( not gate )
                  Out.println("a.getFeatures.size() is " + a.getFeatures().size());
                  for (Feature f : a.getFeatures())
                  {
                     //put this feature in the newFeatures Feature map, using the name as the key; and the feature as the value 
                     newFeatures.put(f.getName().getLocalName(), f.getValue());
                     System.out.println("feature name is " + f.getName().getLocalName() + " value is "
                           + f.getValue());
                  }
                  try
                  {
                     //add to gate annotation set, using start, end, name, and feature map ( uses name as key )
                     as.add(start, end, a.getType().getLocalName(), newFeatures);
                  }
                  catch (InvalidOffsetException e)
                  {
                     throw new ResourceInstantiationException(e);
                  }
               }
            }
         }
      }
      
   }
   catch (SAXException e1)
   {
      Out.println("Could not create GraphParser");
   }
     
      

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

   /**
    * given the string version of the file name of a standoff file, coming from
    * the *.anc document, checks the string is in the annotations hash table and
    * returns if it exists, otherwise prints err message..actually can return
    * anything from annotations hash table if there is a key value pair
    */
   protected String getFileForType(String type) throws ResourceInstantiationException
   {
      //returns the standoff file name tied to the key type..
      String result = annotations.get(type);
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
         throw new ResourceInstantiationException("Error reading the document content from " + path);
      }
      return sbuffer.toString();
   }

   /**
    * given gate Annotation set taken from *.anc document returns
    * hashtable<String,String> with type as key and file name as string value
    * 
    * @param set
    * @return
    * @throws ResourceInstantiationException
    */
   protected Hashtable<String, String> getAnnotationFiles(AnnotationSet set)
         throws ResourceInstantiationException
   {
      Hashtable<String, String> table = new Hashtable<String, String>();
      Iterator<gate.Annotation> it = set.iterator();
      while (it.hasNext())
      {
         gate.Annotation a = it.next();
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

   protected Hashtable<Node, Node> getAnnotationFiles(URL url) throws DocumentFormatException
   {
      Hashtable<Node, Node> table = new Hashtable<Node, Node>();
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
         throw new DocumentFormatException("Error create the XML document builder.", ex);
      }

      NodeList nodes = document.getElementsByTagName("annotation");
      if (nodes == null || nodes.getLength() == 0)
      {
         throw new DocumentFormatException("The header does not contain any annotation elements.");
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
