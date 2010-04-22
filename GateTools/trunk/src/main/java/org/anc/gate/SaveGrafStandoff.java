/*-
 * Copyright (c) 2009 American National Corpus
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
import gate.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.anc.masc.MASC;
import org.anc.util.IDGenerator;
import org.xces.graf.api.GrafException;
import org.xces.graf.api.IAnchor;
import org.xces.graf.api.IAnchorFactory;
import org.xces.graf.api.IAnnotation;
import org.xces.graf.api.IAnnotationSet;
import org.xces.graf.api.IGraph;
import org.xces.graf.api.INode;
import org.xces.graf.api.IRegion;
import org.xces.graf.impl.Factory;
import org.xces.graf.io.GrafRenderer;
import org.xces.graf.io.XML;
import org.xces.graf.util.GraphUtils;

/**
 */
public class SaveGrafStandoff extends ANCLanguageAnalyzer
{
   // Parameters passed by Gate.
   /** Where the file will be written. */ 
   public static final String DESTINATION_PARAMETER_NAME = "destination";
   /** Gate AnnotationSet containing the annotations to be saved. */
   public static final String INPUTASNAME_PARAMETER_NAME = "inputASName";
   /** A white space delimited list of the annotation types (tags) to be saved. */
   public static final String STANDOFFTAGS_PARAMETER_NAME = "standoffTags";
   /** Character encoding to use. Default is UTF-8. */
   public static final String ENCODING_PARAMETER_NAME = "encoding";
   /** The type suffix that will be added to the file name. */
   public static final String ANNOTYPE_PARAMETER_NAME = "annotationType";
   public static final String VERSION_PARAMETER_NAME = "version";
   /** The name of the GrAF annotation set. */
   public static final String GRAF_AS_NAME = "grafASName";
   /** The type URI for the above GrAF annotation set name. */
   public static final String GRAF_AS_TYPE = "grafASType";
   /** Default URI to use if undeclared annotation sets are encountered. */
   public static final String GRAF_DEFAULT_AS_TYPE = "grafDefaultASType";
   
//	public static final String NAMESPACE_PARAMETER_NAME = "namespace";
//	public static final String SCHEMALOCATION_PARAMETER_NAME = "schemaLocation";

   // Properties to hold parameter values.
   private java.net.URL destination = null;
   private String inputASName = null;
   private java.util.List<String> standoffTags = null;
   private String annotationType = "standoff";
   private String version = null;
   private String encoding = null;
   private String grafASName = null;
   private String grafASType = null;
   private String grafDefaultASType = null;
   
//	private String namespace = null;
//	private String schemaLocation = null;

   public SaveGrafStandoff()
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

         IGraph graph = createGraph();
         FileOutputStream stream = new FileOutputStream(f);
         OutputStreamWriter writer = new OutputStreamWriter(stream, encoding);
         GrafRenderer graf = new GrafRenderer(writer);
         graf.render(graph);
         writer.close();
      }
      catch (RuntimeException ex)
      {
         throw ex;
      }
      catch (Exception ex)
      {
//         System.out.println(ex.getMessage());
         ex.printStackTrace();
         throw new ExecutionException(ex);
      }
   }

   public IGraph createGraph() throws IOException, GrafException
   {
//      Out.prln("GrAF Annotation set name : " + grafASName);
      IGraph graph = Factory.newGraph();
      IDGenerator id = new IDGenerator();
      IAnchorFactory anchorFactory = Factory.newCharacterAnchorFactory();

      Map<String,IAnnotationSet> grafAnnotationSetMap = new HashMap<String, IAnnotationSet>();
      IAnnotationSet set = Factory.newAnnotationSet(grafASName, grafASType);
      graph.addAnnotationSet(set);
      grafAnnotationSetMap.put(grafASName, set);
      
      // This is a Gate AnnotationSet object.
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
         return null;
      }

      if (annotations == null || annotations.size() == 0)
      {
         System.out.println(this.getClass().getName()
               + ": No standoff annotations found.");
         failed = true;
         return null;
      }

      Comparator<Annotation> comp = org.anc.gate.AnnotationComparer
            .createStartComparator();
      ArrayList<Annotation> sortedAnnotations = new ArrayList<Annotation>(
            annotations);
      Collections.sort(sortedAnnotations, comp);
      Iterator<Annotation> it = sortedAnnotations.iterator();
//      new File(document.getSourceUrl().getPath());
      
      List<Pair> pairs = new LinkedList<Pair>();
      
      while (it.hasNext())
      {
         Annotation gateAnnotation = it.next();
         FeatureMap fm = gateAnnotation.getFeatures();

         long start = gateAnnotation.getStartNode().getOffset().longValue();
         long end = gateAnnotation.getEndNode().getOffset().longValue();

         IAnchor startAnchor = anchorFactory.newAnchor(start);
         IAnchor endAnchor = anchorFactory.newAnchor(end);

         IRegion region = graph.getRegion(startAnchor, endAnchor);
         String grafId = (String) fm.get(Graf.GRAF_ID);
         if (grafId == null)
         {
            grafId = id.generate("r");
         }
         if (region == null)
         {
            region = Factory.newRegion(id.generate("r"), startAnchor, endAnchor);
            graph.addRegion(region);
         }

         List<INode> nodes = region.getNodes();
         INode node = null;
         if (nodes.size() > 0)
         {
            node = nodes.get(0);
         }
         else
         {
            node = Factory.newNode(id.generate("n"));
            graph.addNode(node);
            node.addRegion(region);
         }
         IAnnotation grafAnnotation = Factory.newAnnotation(gateAnnotation
               .getType());
         node.addAnnotation(grafAnnotation);
         boolean addedToSet = false;
         if (fm != null && fm.size() > 0)
         {
            Set<Map.Entry<Object, Object>> attSet = fm.entrySet();
            Iterator<Entry<Object, Object>> asIt = attSet.iterator();
            while (asIt.hasNext())
            {
               Map.Entry<Object, Object> att = asIt.next();
               if (!"isEmptyAndSpan".equals(att.getKey()))
               {
                  String key = (String) att.getKey();
                  
                  if(key == Graf.GRAF_EDGE)
                  {
                	  Pair nodeToChild = new Pair(node.getId(), att.getValue());
                	  pairs.add(nodeToChild);
                  }
                  else if (key == Graf.GRAF_SET)
                  {
                     String setName = att.getValue().toString();
                     IAnnotationSet aset = grafAnnotationSetMap.get(setName);
                     
                     if (aset == null)
                     {
                        Out.prln("Undefined annotation set " + setName);
                        String type = grafDefaultASType;
                        if (type.endsWith("/"))
                        {
                           type = type + setName;
                        }
                        else
                        {
                           type = type + "/" + setName;
                        }
                        aset = Factory.newAnnotationSet(setName, type);
                        grafAnnotationSetMap.put(setName, set);
                     }
                     aset.addAnnotation(grafAnnotation);
                     addedToSet = true;
                  }
                  else
                  {
                     String value = XML.encode((String) att.getValue());
                     grafAnnotation.addFeature(key, value);
                  }
               }
            }
         }
         if (!addedToSet)
         {
            set.addAnnotation(grafAnnotation);
         }
      }

      for (Pair p : pairs)
      {
         StringTokenizer sT = new StringTokenizer((String) p.second);

         while (sT.hasMoreTokens())
         {
            graph.addEdge((String) p.first, sT.nextToken());
         }
      }
      return graph;
   }

   // Property getters and setters.
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
   
   public void setGrafASType(String uri)
   {
      this.grafASType = uri;
   }
   
   public String getGrafASType() { return grafASType; }
   
   public void setGrafASName(String name)
   {
      grafASName = name;
   }
   public String getGrafASName() { return grafASName; }
      
   public void setGrafDefaultASType(String uri)
   {
      grafDefaultASType = uri;
   }
   public String getGrafDefaultASType() { return grafDefaultASType; }
}
