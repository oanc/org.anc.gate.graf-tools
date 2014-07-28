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
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
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
import gate.util.Out;
import org.anc.gate.core.ANCLanguageAnalyzer;
import org.anc.util.Pair;
import org.apache.commons.io.FileUtils;
import org.xces.graf.api.*;
import org.xces.graf.impl.CharacterAnchor;
import org.xces.graf.io.GrafParser;
import org.xces.graf.io.dom.ResourceHeader;
import org.xces.graf.util.GraphUtils;
import org.xces.graf.util.IFunction;
import org.xml.sax.SAXException;

/**
 * 
 * @author Keith Suderman
 * @version 1.0
 */
@CreoleResource(
        name = "GrAF Load Standoff",
        comment = "Loads GrAF standoff annotations"
)
public class LoadGrafStandoff extends ANCLanguageAnalyzer
{
   private static final long serialVersionUID = 1L;

   /** The name of the GATE annotationSet where the annotations will be created. */
   protected String standoffASName = null;

   /** Allows the user to specify the annotation file to load. If set sourceUrl
    *  overrides the default algorithm used to calculate the path to the standoff
    *  annotation file.
    */
   protected URL sourceUrl = null;

   /** The annotation type (as specified in the resource header) to be loaded. The path
    *  to the standoff annotation type can be fetched from the document header using the
    *  annotationType or the path can be derived from the document URL and annotationType.
    */
   protected String annotationType = null;

   /** If failFast is set to true then an Exception will be thrown causing Pipelines
    *  to halt. Otherwise exceptions are caught, an error message is printed, and the
    *  resource exits cleanly.  This allows CorpusPipelines to continue processing when
    *  a few documents cause exceptions to be thrown.
    */
   protected Boolean failFast = Boolean.FALSE;

   /**
    * If set to true stack traces will be displayed on the GATE console. Setting to
    * false (the default) results in shorter error messages.
    */
   protected Boolean printStackTrace = Boolean.FALSE;

   /** The URL to the corpus resource header. */
   private URL resourceHeader;

   private ResourceHeader header;

   /** Parser used to load the standoff annotation file. */
//   protected transient GrafParser parser;

   /** The GATE AnnotationSet where new annotations will be created. */
   protected AnnotationSet annotations;

//   protected transient GetRangeFunction getRangeFn = new GetRangeFunction();

   /** Text content for the document being processed. */
   protected transient String content = null;

   /** The length of the content. */
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
//         parser = new GrafParser();
         File headerFile = FileUtils.toFile(resourceHeader);
         header = new ResourceHeader(headerFile);
//         for (IAnnotationSpace aspace : header.getAnnotationSpaces())
//         {
//            parser.addAnnotationSpace(aspace);
//         }
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
//      BufferedReader r;
		GrafParser parser = null;
		try
		{
			parser = new GrafParser(header);
		}
		catch (SAXException e)
		{
			throw new ExecutionException(e);
		}
		catch (GrafException e)
		{
			throw new ExecutionException(e);
		}

		// Get the GATE annotations from the document being processed.
      annotations = super.getAnnotations(standoffASName);

      // Get the text from the document.
      content = document.getContent().toString();
      endOfContent = content.length();

      File file;
      if (sourceUrl == null) {
         // If the sourceUrl is null then the path to the standoff file should be derived
         // from the path to the document and the annotationType.
         if (annotationType == null)
         {
            // This is something we can not recover from so the failFast
            // parameter is ignored.
            throw new ExecutionException(
                    "Source URL is null and no annotation type was specified.");
         }
         // TODO The annotation file should be retrieved from the document header.
         File docFile = new File(document.getSourceUrl().getPath());
         File parent = docFile.getParentFile();

         String filename = docFile.getName();
         int index = filename.lastIndexOf(".txt");
         if (index > 0)
         {
            filename = filename.substring(0, index);
         }
         filename = filename + "-" + annotationType + ".xml";
         file = new File(parent, filename);
      }
      else
      {
         // The sourceUrl was specified so load the standoff annotations from there.
         file = new File(sourceUrl.getPath());
      }

      if (!file.exists())
      {
         String message = "Unable to locate annotation file " + file.getPath();
         if (failFast) {
            throw new ExecutionException(message);
         }
         Out.prln(message);
         return;
      }
      if (file.length() == 0)
      {
         String message = "WARNING: " + file.getPath() + " is empty.";
         if (failFast)
         {
            throw new ExecutionException(message);
         }
         Out.prln(message);
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
         Err.prln("Error loading standoff from " + file.getPath());
         if (failFast) {
            throw new ExecutionException("Unable to load standoff.", ex);
         }
         else if (printStackTrace)
         {
            ex.printStackTrace();
         }
         else
         {
            Out.prln(ex.getMessage());
         }
      }
//      System.out.println("Execution complete.");
   }

   @RunTime(false)
   @Optional(false)
   @CreoleParameter(comment = "Corpus resource header.")
   public void setResourceHeader(URL location)
   {
      resourceHeader = location;
   }
   public URL getResourceHeader()
   {
      return resourceHeader;
   }

   @RunTime
   @Optional
   @CreoleParameter(
           comment = "Determines whether stack traces will be displayed if an exception is encountered.",
           defaultValue = "false"
   )
   public void setPrintStackTrace(Boolean printStackTrace) {
      this.printStackTrace = printStackTrace;
   }
   public Boolean getPrintStackTrace()
   {
      return printStackTrace;
   }

   @RunTime
   @Optional
   @CreoleParameter(
           comment = "Setting failFast to true causes pipelines to halt on the first exception.",
           defaultValue = "false"
   )
   public void setFailFast(Boolean failFast) { this.failFast = failFast; }
   public Boolean getFailFast() { return failFast; }

   @RunTime
   @Optional
   @CreoleParameter(
           comment = "New annotations will be added to this GATE annotation set.",
           defaultValue = "Standoff markups"
   )
   public void setStandoffASName(String name)
   {
      standoffASName = name;
   }
   public String getStandoffASName()
   {
      return standoffASName;
   }


   @RunTime
   @Optional
   @CreoleParameter(comment = "Standoff annotations will be loaded from this URL.")
   public void setSourceUrl(URL url)
   {
      sourceUrl = url;
   }
   public URL getSourceUrl()
   {
      return sourceUrl;
   }


   @RunTime
   @Optional
   @CreoleParameter(comment = "Annotation type to be loaded. Only used if the sourceUrl is not specified.")
   public void setAnnotationType(String type)
   {
      this.annotationType = type;
   }
   public String getAnnotationType()
   {
      return annotationType;
   }


   protected void addAnnotation(INode node) throws InvalidOffsetException
   {
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