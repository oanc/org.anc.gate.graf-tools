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

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Resource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.Err;
import gate.util.InvalidOffsetException;

import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// import org.anc.conf.AnnotationSpaces;
import org.anc.gate.core.ANCLanguageAnalyzer;
import org.anc.util.Pair;
import org.apache.commons.io.FileUtils;
import org.xces.graf.api.IAnchor;
import org.xces.graf.api.IAnnotation;
import org.xces.graf.api.IAnnotationSpace;
import org.xces.graf.api.IEdge;
import org.xces.graf.api.IFeature;
import org.xces.graf.api.IFeatureStructure;
import org.xces.graf.api.IGraph;
import org.xces.graf.api.ILink;
import org.xces.graf.api.INode;
import org.xces.graf.api.IRegion;
import org.xces.graf.api.IStandoffHeader;
import org.xces.graf.impl.CharacterAnchor;
import org.xces.graf.io.GrafParser;
import org.xces.graf.io.dom.ResourceHeader;
import org.xces.graf.util.GraphUtils;
import org.xces.graf.util.IFunction;

/**
 * 
 * @author Keith Suderman
 * @version 1.0
 */
public class LoadGrafStandoff extends ANCLanguageAnalyzer
{
   private static final long serialVersionUID = 1L;

   public static final String STANDOFFASNAME_PARAMETER = "standoffASName";
   public static final String SOURCE_URL_PARAMETER = "sourceUrl";
   public static final String ANNOTATION_TYPE_PARAMETER = "annotationType";
   public static final String RESOURCE_HEADER_PARAMETER_NAME = "resourceHeader";

   protected String standoffASName = null;
   protected URL sourceUrl = null;
   protected String annotationType = null;
   private URL resourceHeader;

   protected transient GrafParser parser;
   protected AnnotationSet annotations;
//   protected transient GetRangeFunction getRangeFn = new GetRangeFunction();
   protected transient String content = null;
   protected transient int endOfContent = 0;

   public LoadGrafStandoff()
   {
      super();
   }

   @Override
   public Resource init() throws ResourceInstantiationException
   {
      if (resourceHeader == null)
      {
         throw new ResourceInstantiationException(
               "The resource header has not been set.");
      }
      try
      {
         super.init();
         parser = new GrafParser();
         File headerFile = FileUtils.toFile(resourceHeader);
         ResourceHeader header = new ResourceHeader(headerFile);
         for (IAnnotationSpace aspace : header.getAnnotationSpaces())
         {
            parser.addAnnotationSpace(aspace);
         }
      }
      catch (Exception ex)
      {
         throw new ResourceInstantiationException(
               "Unable to initialize the GraphParser", ex);
      }
      return this;
   }

   @Override
   public void execute() throws ExecutionException
   {
      BufferedReader r;
      //annotations is gate annotations not graf annotations
      //getAnnotations comes from the parent class ANCLanguageAnalyzer
      //standoffASName comes from the gate gui; default is 'Standoff markups'
      annotations = getAnnotations(standoffASName);
      //gets the text body of the document; document is the gate document
      content = document.getContent().toString();
      //get length of the document
      endOfContent = content.length();
      // System.out.println("standoffASName is " + standoffASName + " Content length is " + endOfContent);
      //System.out.println("There are " + annotations.size() + " GATE annotations.");

      //URL url = this.getSourceUrl();
      //get the file path for the standoff file ( ie nc, vc etc ); sourceUrl comes from the gate gui

      File file = new File(sourceUrl.getPath());
      String name = document.getName();
      if (file.isDirectory())
      {
         if (annotationType == null)
         {
            throw new ExecutionException(
                  "Source URL is a directory and no annotation type was specified.");
         }
         // TODO The annotation file should be determined from the header.
         int index = name.lastIndexOf(".txt");
         if (index > 0)
         {
            name = name.substring(0, index);
         }
         name = name + "-" + annotationType + ".xml";
         file = new File(file, name);
      }
      if (!file.exists())
      {
         File docRoot = new File(document.getSourceUrl().getPath())
               .getParentFile();
         file = new File(docRoot, name);
         if (!file.exists())
         {
            System.err.println("Unable to locate annotation file "
                  + file.getPath());
            return;
         }
      }
      if (file.length() == 0)
      {
         System.err.println("WARNING: " + file.getPath() + " is empty.");
         return;
      }

      //create empty graph to start
      IGraph graph = null;
      try
      {
         //set graph to the graph file
         // System.out.println("Loading the graph.");
         graph = parser.parse(file);

         addHeader(graph);
         //cycle through the nodes of the graph to get the annotations
         for (INode node : graph.nodes())
         {
            //node by node adds a gate annotation object ( annotations above ) with a gate
            //feature map ( which has a string of child node ids, the graf annotation setName, graf annotation labels
            //this node's id, and any feature info from this node's feature structure)
            //basically 'annotations' has all the node's stuff in it, in a gate understandable AnnotationSet
            // System.out.println("Adding annotation for node " + node.getId());
            addAnnotation(node);
         }
      }
      catch (Exception ex)
      {
         Err.prln("Error loading standoff.");
         ex.printStackTrace();
         throw new ExecutionException("Unable to load standoff.", ex);
      }
//      System.out.println("Execution complete.");
   }

   public void setResourceHeader(URL location)
   {
      resourceHeader = location;
   }

   public URL getResourceHeader()
   {
      return resourceHeader;
   }

   public String getStandoffASName()
   {
      return standoffASName;
   }

   public void setStandoffASName(String name)
   {
      standoffASName = name;
   }

   public URL getSourceUrl()
   {
      return sourceUrl;
   }

   public void setSourceUrl(URL url)
   {
      sourceUrl = url;
   }

   public String getAnnotationType()
   {
      return annotationType;
   }

   public void setAnnotationType(String type)
   {
      this.annotationType = type;
   }

   protected void addAnnotation(INode node) throws InvalidOffsetException
   {
//      getRangeFn.reset();
//      //offset object extends pair, first is start (long), second is end (long),
//      Offset offset = getRangeFn.apply(node);
//      if (offset.getEnd() < offset.getStart())
//      {
//         return;
//      }
      IRegion span = GraphUtils.getSpan(node);
      if (span == null || span.getStart() == null || span.getEnd() == null || span.getStart().compareTo(span.getEnd()) > 0)
      {
         return;
      }
      //node ids from out edges ( children node ids ) will end up as a long string
      //separated by spaces
      StringBuilder ids = new StringBuilder();
      //cycle the out edges for this node
      for (IEdge e : node.getOutEdges())
      {
         //append child node id to ids stringbuilder
         ids.append(e.getTo().getId() + " ");
      }
      for (IAnnotation a : node.annotations())
      {
         //create a gate object, FeatureMap, 
         FeatureMap newFeatures = Factory.newFeatureMap();
         //we know since this is an anc standoff graph, use Standoff Markups as the annotation setName
         String aSetName = "Standoff Markups";
         //now get the set from the annotationSet associated with the graphs node
         IAnnotationSpace as = a.getAnnotationSpace();
         //as long as the graf annotationSet is not null
         if (as != null)
         {
            //get the name of the graf annotationSet
            aSetName = as.getName();
            //now put the graf annotationSet name in the gate FeatureMap using 'graf:set' as the key
            newFeatures.put(Graf.GRAF_SET, aSetName);

         }
         //if we have any outEdges put the 'id' stringbuilder in the gate FeatureMap using 'graf:edge' as the key
         if (node.getOutEdges().size() > 0)
         {
            newFeatures.put(Graf.GRAF_EDGE, ids.toString());
         }
         //put the node id in the gate FeatureMap using 'graf:id' as the key
         newFeatures.put(Graf.GRAF_ID, node.getId());
         //get label from the graf objects annotation 
         String label = a.getLabel();
         //see the addFeatures method for how it adds the features of this annotation to the
         //gate FeatureMap using the IFeatureStructure from this graf annotation, it adds it to the
         //gate FeatureMap newFeatures, null sent in as the base feature
         addFeatures(a.getFeatures(), newFeatures, null);

         long start = 0;
         long end = 0;
         try
         {
            start = (Long) span.getStart().getOffset(); //offset.getStart();
            end = (Long) span.getEnd().getOffset(); //offset.getEnd();
            if (end > endOfContent)
            {
               System.err.println("Invalid end offset for " + label + " " + end
                     + ", end of content = " + endOfContent);

               end = endOfContent;
            }
            if (start > end)
            {
               System.err.println("Invalid start offset for " + label + " "
                     + start + ", end of content = " + endOfContent);
            }
            else
            {
               //if here, the offsets look ok, finally add the annotation to the
               //gate annotations object using the start, end, anc graf annotation name and the gate feature map
//                  Out.println(start + ", " + end + ": " + label);
               annotations.add(start, end, label, newFeatures);

            }
         }
         catch (Exception e)
         {
            System.err.println("Invalid offsets for " + label);
            System.err.println("Annotation span : " + start + " - " + end);
            throw new InvalidOffsetException("Invalid offsets for " + label
                  + " from " + start + " to " + end);
         }
      }
      //}
   }

   /**
    * Adds metadata from the graph header to the document's feature map.
    * 
    * @param graph
    */
   protected void addHeader(IGraph graph)
   {
      IStandoffHeader header = graph.getHeader();
      if (header != null)
      {
         addToMetaData("graf:annotationSpaces", header.getAnnotationSpaces());
         addToMetaData("graf:dependsOn", header.getDependsOn());
         addToMetaData("graf:roots", header.getRoots());
      }
   }

   protected void addToMetaData(String name, List<?> objects)
   {
      String value = makeString(objects.iterator());
      if (value != null)
      {
         document.getFeatures().put(name, value);
      }
   }

   /** Creates a space delimited string of all objects in a collection. */
   protected String makeString(Iterator<?> it)
   {
      StringBuilder buffer = new StringBuilder();
      if (it.hasNext())
      {
         buffer.append(it.next().toString());
      }
      while (it.hasNext())
      {
         buffer.append(' ');
         buffer.append(it.next().toString());
      }
      String result = buffer.toString();
      if (result.length() == 0)
      {
         return null;
      }
      return result;
   }

   protected void addFeatures(IFeatureStructure featStruc, FeatureMap fm,
         String base)
   {
      //graf type feature structure
      IFeatureStructure fs = featStruc;
      //if empty, get out
      if (fs == null)
      {
         return;
      }
      //loop through the features in the feature structure
      for (IFeature f : fs.features())
      {
         //if this is not a feature structure, go, otherwise recurse with child features
         if (f.isAtomic())
         {
            //if no base sent in, use the feature name as the key in the passed in feature map
            if (base == null)
            {
               //put in feature map with feature name as key, and feature value as value
               fm.put(f.getName(), f.getStringValue());
            }
            //if base is sent in append feature name to it, and use that as key instead
            else
            {
               fm.put(base + "/" + f.getName(), f.getStringValue());
            }
         }
         //wait, this is not a feature, it is a feature structure, get the child feature structure and recurse with it
         else
         {
            //get the child feature structure
            IFeatureStructure childFS = (IFeatureStructure) f.getValue();
            String childName = null;
            //if base not sent in, use feature structure name as the new base, when recursing
            if (base == null)
            {
               childName = f.getName();
            }
            //base is sent in, append feature ( or feature structure ) name to it, and use that as new base
            else
            {
               childName = base + "/" + f.getName();
            }
            //recurse with child featureStructure, featureMap, and feature name as base
            addFeatures(childFS, fm, childName);
         }
      }
   }

}

class Offset extends Pair<Long, Long>
{
   public Offset()
   {
      super(Long.MAX_VALUE, Long.MIN_VALUE);
   }

   public Offset(long start, long end)
   {
      super(start, end);
   }

   public Long getStart()
   {
      return first;
   }

   public Long getEnd()
   {
      return second;
   }

   public void setStart(long start)
   {
      setFirst(start);
   }

   public void setEnd(long end)
   {
      setSecond(end);
   }

}

/*
 * class GetRangeFunction implements IFunction<INode, Offset> { protected Offset
 * offset = new Offset(); protected Set<INode> seen = new HashSet<INode>();
 * 
 * public Offset apply(INode item) { if (seen.contains(item)) { return offset; }
 * seen.add(item); for (ILink link : item.links()) { for (IRegion region : link)
 * { getRange(region); } } for (IEdge e : item.getOutEdges()) {
 * apply(e.getTo()); } return offset; }
 * 
 * private void getRange(IRegion region) { //
 * System.out.println("Getting range for region " + region.getId()); IAnchor
 * startAnchor = region.getStart(); IAnchor endAnchor = region.getEnd(); if
 * (!(startAnchor instanceof CharacterAnchor) || !(endAnchor instanceof
 * CharacterAnchor)) { return; }
 * 
 * CharacterAnchor start = (CharacterAnchor) startAnchor; CharacterAnchor end =
 * (CharacterAnchor) endAnchor; if (start.getOffset() < offset.getStart()) {
 * offset.setStart(start.getOffset()); } if (end.getOffset() > offset.getEnd())
 * { offset.setEnd(end.getOffset()); } }
 * 
 * public void reset() { seen.clear(); offset.setStart(Long.MAX_VALUE);
 * offset.setEnd(Long.MIN_VALUE); } }
 */