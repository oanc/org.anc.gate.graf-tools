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

import gate.Annotation;
import gate.AnnotationSet;
import gate.FeatureMap;
import gate.Resource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.Out;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.anc.Sys;
import org.xces.XCES;
import org.xces.graf.util.GraphUtils;

/**
 */
public class SaveStandoff extends ANCLanguageAnalyzer
//	implements org.xces.util.XcesAna
{
   // Parameters passed by Gate.
//   public static final String DOCUMENT_PARAMETER_NAME = "document";
   public static final String DESTINATION_PARAMETER_NAME = "destination";
   public static final String INPUTASNAME_PARAMETER_NAME = "inputASName";
   public static final String STANDOFFTAGS_PARAMETER_NAME = "standoffTags";
   public static final String ENCODING_PARAMETER_NAME = "encoding";
   public static final String ANNOTYPE_PARAMETER_NAME = "annotationType";
   public static final String VERSION_PARAMETER_NAME = "version";
   public static final String NAMESPACE_PARAMETER_NAME = "namespace";
   public static final String SCHEMALOCATION_PARAMETER_NAME = "schemaLocation";

   // Properties to hold parameter values.
//   private gate.Document document = null;
//   private Corpus corpus = null;
   private java.net.URL destination = null;
   private String inputASName = null;
   private java.util.List<String> standoffTags = null;
   private String annotationType = "standoff";
   private String version = null;
   private String encoding = null;
   private String namespace = null;
   private String schemaLocation = null;

   public SaveStandoff()
   {
   }

   @Override
   public Resource init() throws ResourceInstantiationException
   {
      super.init();
      return this;
   }

   @Override
   public void reInit() throws ResourceInstantiationException
   {
      this.init();
   }

   @Override
   public void execute() throws ExecutionException
   {
      if (null == document)
      {
         throw new ExecutionException("Parameter document has not been set.");
      }

      if (null == destination)
      {
         throw new ExecutionException("Parameter destination has not been set.");
      }

      if (null == encoding)
      {
         encoding = "UTF-8";
      }

      try
      {
         File f = new File(destination.getPath());
         if (f.isDirectory())
         {
            f = new File(f, document.getName() + "-" + annotationType + ".xml");
         }
         FileOutputStream stream = new FileOutputStream(f);
         OutputStreamWriter writer = new OutputStreamWriter(stream, encoding);
         writeXml(writer);
         writer.close();
      }
      catch (IOException ex)
      {
         System.out.println(ex.getMessage());
         ex.printStackTrace();
         throw new ExecutionException(ex);
      }
   }

   public void writeXml(Writer writer) throws IOException
   {
      AnnotationSet annotations = null;
      try
      {
         AnnotationSet originals = this.getAnnotations(inputASName);
         if (originals != null && originals.size() > 0)
         {
            if (standoffTags == null || standoffTags.size() == 0)
            {
               annotations = originals;
            }
            else
            {
               annotations = originals.get(new HashSet<String>(standoffTags));
            }
         }
      }
      catch (Exception ex)
      {
         Out.prln(ex.getMessage());
         failed = true;
         return;
      }

      if (annotations == null || annotations.size() == 0)
      {
         System.out
               .println("org.anc.gate.SaveStandoff : No standoff annotations found.");
         failed = true;
         return;
      }
//     Out.prln("dumping head annotations");
//     dump(annotations, "head");

      // Sort the sentences by offset so they are added in order.

      Comparator<Annotation> comp = org.anc.gate.AnnotationComparer
            .createStartComparator();
      ArrayList<Annotation> sortedAnnotations = new ArrayList<Annotation>(
            annotations);
      Collections.sort(sortedAnnotations, comp);
      Iterator<Annotation> it = sortedAnnotations.iterator();
      new File(document.getSourceUrl().getPath());

//     StringBuffer buffer = new StringBuffer();
      writer.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>"
            + Sys.EOL + "<cesAna xmlns=\"" + namespace + "\" version=\""
            + version + "\"");
      if (defined(schemaLocation))
      {
         writer
               .write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
         writer.write(" xsi:schemaLocation=\"" + namespace + " "
               + schemaLocation + "\"");
      }
      writer.write(">" + Sys.EOL);

      String indent = "   ";
      String indentMore = indent + indent;

      while (it.hasNext())
      {
         Annotation a = it.next();
         long start = a.getStartNode().getOffset().longValue();
         long end = a.getEndNode().getOffset().longValue();
         FeatureMap fm = a.getFeatures();
         writer.write(indent + "<" + XCES.ANNO_TAG + " " + XCES.ANNO_TYPE
               + "=\"" + a.getType() + "\" " + XCES.ANNO_FROM + "=\"" + start
               + "\" " + XCES.ANNO_TO + "=\"" + end + "\"");
         if (fm == null || fm.size() == 0)
         {
            writer.write("/>" + Sys.EOL);
         }
         else
         {
            writer.write(">" + Sys.EOL);
            Set<Map.Entry<Object, Object>> attSet = fm.entrySet();
            Iterator<Map.Entry<Object, Object>> asIt = attSet.iterator();
            while (asIt.hasNext())
            {
               Map.Entry<Object, Object> att = asIt.next();
               if (!"isEmptyAndSpan".equals(att.getKey()))
               {
//             if ("id".equals(att.getKey()))
//             {
//               Out.prln("Saving with id " + att.getValue());
//             }
                  writer.write(indentMore + "<" + XCES.ATT_TAG + " "
                        + XCES.ATT_NAME + "=\"" + att.getKey() + "\" "
                        + XCES.ATT_VALUE + "=\"" + fix(att.getValue()) + "\"/>"
                        + Sys.EOL);
               }
            }
            writer.write(indent + "</" + XCES.ANNO_TAG + ">" + Sys.EOL);
         }

      }

      writer.write("</cesAna>");
//     return buffer.toString();
   }

   protected Object fix(Object value)
   {
      if (!(value instanceof String))
      {
         return value;
      }

      return GraphUtils.encode((String) value);
   }

   // Property getters and setters.
//   public void setDocument(gate.Document document) { this.document = document; }
//   public gate.Document getDocument() { return document; }

   public void setDestination(java.net.URL destination)
   {
      this.destination = destination;
   }

   public java.net.URL getDestination()
   {
      return destination;
   }

   public void setInputASName(String inputASName)
   {
      this.inputASName = inputASName;
   }

   public String getInputASName()
   {
      return inputASName;
   }

   public void setStandoffTags(java.util.List<String> standoffTags)
   {
      this.standoffTags = standoffTags;
   }

   public java.util.List<String> getStandoffTags()
   {
      return standoffTags;
   }

   public void setEncoding(String encoding)
   {
      this.encoding = encoding;
   }

   public String getEncoding()
   {
      return encoding;
   }

   public void setNamespace(String namespace)
   {
      this.namespace = namespace;
   }

   public String getNamespace()
   {
      return namespace;
   }

   public void setSchemaLocation(String schemaLocation)
   {
      this.schemaLocation = schemaLocation;
   }

   public String getSchemaLocation()
   {
      return schemaLocation;
   }

   public void setVersion(String version)
   {
      this.version = version;
   }

   public String getVersion()
   {
      return version;
   }

   public void setAnnotationType(String type)
   {
      this.annotationType = type;
   }

   public String getAnnotationType()
   {
      return annotationType;
   }
}
