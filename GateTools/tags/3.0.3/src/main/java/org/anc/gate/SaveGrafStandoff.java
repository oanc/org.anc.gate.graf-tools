/*-
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * Copyright (c) 2009 American National Corpus
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

//import org.anc.masc.MASC;
import org.anc.gate.core.ANCLanguageAnalyzer;
import org.anc.gate.core.AnnotationComparer;
import org.anc.util.IDGenerator;
import org.xces.graf.api.GrafException;
import org.xces.graf.api.IAnchor;
import org.xces.graf.api.IAnchorFactory;
import org.xces.graf.api.IAnnotation;
import org.xces.graf.api.IAnnotationSpace;
import org.xces.graf.api.IGraph;
import org.xces.graf.api.INode;
import org.xces.graf.api.IRegion;
import org.xces.graf.api.IStandoffHeader;
import org.xces.graf.impl.Factory;
//import org.xces.graf.impl.header.Media;
//import org.xces.graf.impl.header.Medium;
import org.xces.graf.io.GrafRenderer;
import org.xces.graf.io.XML;
//import org.xces.graf.util.GraphUtils;

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
//   public static final String VERSION_PARAMETER_NAME = "version";
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
//   private String version = null;
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

         //get header info from document to be placed in new graph
         Object headerObject = document.getFeatures().get(Graf.GRAF_HEADER);
         if ((headerObject instanceof IStandoffHeader) && (headerObject != null))
         {
            //sysout

            IStandoffHeader header = (IStandoffHeader) headerObject;
            graph.setHeader(header);

         }
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

      Map<String, IAnnotationSpace> grafAnnotationSetMap = new HashMap<String, IAnnotationSpace>();
      //grafASName ( default on the gui is: http://www.xces.org/schema/2003 ) and
      //grafASType (default on the gui is: xces ) 
      //come from the gate gui, use them to make a new anc graf annotationSet out of them ( now empty )
      IAnnotationSpace set = Factory.newAnnotationSpace(grafASName, grafASType);
      //add the set to the graf
      graph.addAnnotationSpace(set);
      //put the annotation set in the set map with the grafASName as key
      grafAnnotationSetMap.put(grafASName, set);

      // This is a Gate AnnotationSet object.
      AnnotationSet annotations = null;
      try
      {
         //Gate AnnotationSet object again; originals is the annotationSet based
         //on the inputASName inputed in the Gate UI ( see setinputASName below )
         //default is inputASName = standOffMarkups which is all the standoff markups we
         //know and love..nc + vc etc...
         AnnotationSet originals = this.getAnnotations(inputASName);
         if (originals != null && originals.size() > 0)
         {
            //standoffTags comes from user interface of Gate; if Gate user
            //does not add any in GUI, then stanoffTags == null or .size =0, so just get all the
            //original annotations
            if (standoffTags == null || standoffTags.size() == 0)
            {
               //go with all annotations 
               annotations = originals;
            }
            else
            {
               //if here, the user added standOff tags in the gate gui, so we just
               //want a subset of the originals based on those standOffs. so pull out only those that 
               //are listed in the standoffTags
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

      //sanity check to make sure we have some annotations
      if (annotations == null || annotations.size() == 0)
      {
         System.out.println(this.getClass().getName() + ": No standoff annotations found.");
         failed = true;
         return null; //get out !
      }

      //create a startComparer ( implements java.util.Comparator ).  
      //this compares two GATE annotations by their start offsets. If the start offsets
      //are equal the annotations will be compared by end offsets.
      Comparator<Annotation> comp = AnnotationComparer.createStartComparator();
      ArrayList<Annotation> sortedAnnotations = new ArrayList<Annotation>(annotations);
      //sort the arrayList of annotations using our comparator
      Collections.sort(sortedAnnotations, comp);
      //now that it is sorted, we need the iterator
      Iterator<Annotation> it = sortedAnnotations.iterator();
      // new File(document.getSourceUrl().getPath());

      List<Pair> pairs = new LinkedList<Pair>();

      while (it.hasNext())
      {
         //remember these are all gate annotation objects
         Annotation gateAnnotation = it.next();
         //grab feature map for each annotation in the sorted gate Annotation ArrayList
         FeatureMap fm = gateAnnotation.getFeatures();

         //get the starts and ends of the annotation
         long start = gateAnnotation.getStartNode().getOffset().longValue();
         long end = gateAnnotation.getEndNode().getOffset().longValue();

         //make new IAnchor objects using the starts and end
         IAnchor startAnchor = anchorFactory.newAnchor(start);
         IAnchor endAnchor = anchorFactory.newAnchor(end);

         //now make a new IRegion object by finding the region in the graph using the start and end anchors from above
         //or if not found ( ie not already put there ), region will be null, so we will create another
         IRegion region = graph.getRegion(startAnchor, endAnchor);
         //figure out the name of the id should be; get id from the featurestructure map that 
         //comes from the magic this object ( via a few steps ); featureMap represents the features
         //of the document loaded into gate
         String grafId = (String) fm.get(Graf.GRAF_ID);
         //make sure it will has an id, if not make sure the id starts with an 'r' for regions
         if (grafId == null)
         {
            grafId = id.generate("r");
         }
         //if region not already in our new graph object, 
         //make a new one with id starting with r from id.generate, start and end anchors
         if (region == null)
         {
            region = Factory.newRegion(id.generate("r"), startAnchor, endAnchor);
            //add it to the graph
            graph.addRegion(region);
         }

         //get all the nodes associated with this region, if region is new, there should be no nodes , right ?
         List<INode> nodes = region.getNodes();
         INode node = null;
         //see if there are multiple nodes in the region, just get the first one, why? not sure yet
         if (nodes.size() > 0)
         {
            //getting the first
            node = nodes.get(0);
         }
         //else there are no nodes in this region, lets make some
         else
         {
            //make it, mark it with an n, put it in the oven..errr graph
            node = Factory.newNode(id.generate("n"));
            //put node in graph
            graph.addNode(node);
            //tie the region to the new node
            node.addRegion(region);
         }
         //now for annotations, graf annotations that is...use the gateAnnotation type to create a new one
         IAnnotation grafAnnotation = Factory.newAnnotation(gateAnnotation.getType());
         //add it to the node, we just made
         node.addAnnotation(grafAnnotation);
         boolean addedToSet = false;
         if (fm != null && fm.size() > 0)
         {
            //Apparently fm.entrySet() returns a Set of Objects that probably could be anything we wanted...!
            Set<Map.Entry<Object, Object>> attSet = fm.entrySet();
            Iterator<Entry<Object, Object>> asIt = attSet.iterator();
            while (asIt.hasNext())
            {
               //get a single entry out of the attSet featureMap fm
               Map.Entry<Object, Object> att = asIt.next();
               //work with the annotation, unless it is the isEmptyAndSpan entry, which is ?
               if (!"isEmptyAndSpan".equals(att.getKey()))
               {
                  //lets get the key
                  String key = (String) att.getKey();

                  //if this one is edge
                  if (key == Graf.GRAF_EDGE)
                  {
                     //make this pair, the node id and the value in the feature map ( the object itself )
                     Pair nodeToChild = new Pair(node.getId(), att.getValue());
                     //add the pair to the pairs List..
                     pairs.add(nodeToChild);
                  }
                  //ok it is not an edge, if it is a graf_set ( anc annotation set )
                  else if (key == Graf.GRAF_SET)
                  {
                     //get the name of the set
                     String setName = att.getValue().toString();
                     //find it in the grafAnnotation set Map from above, ( set might be empty..no matter )
                     IAnnotationSpace aset = grafAnnotationSetMap.get(setName);

                     //if not found, print error, set the type to a default anc graf annotation set type and make a new one..
                     if (aset == null)
                     {
                        Out.prln("Undefined annotation set " + setName);
                        // grafDefaultASType comes from the gate gui; 
                        //default on the gui is: http://www.anc.org/ns/masc/1.0
                        String type = grafDefaultASType;
                        if (type.endsWith("/"))
                        {
                           type = type + setName;
                        }
                        else
                        {
                           type = type + "/" + setName;
                        }
                        //come up with a new graf annotation set using setName and type only if set was found null
                        aset = Factory.newAnnotationSpace(setName, type);
                        //put it in the map of annotation sets
                        grafAnnotationSetMap.put(setName, set);
                     }
                     //finally add the grafAnnoation we made above to this set 
                     aset.addAnnotation(grafAnnotation);
                     addedToSet = true;
                  }
                  else
                  {
                     //ok, if here we have found a feature of an annotation..right ?
                     //get the annotation from this iteration; see above
                     String value = att.getValue().toString();
                     //if found
                     if (value != null)
                     {
                        //do some funny stuff here..?
                        value = XML.encode(value);
                        //add the feature to the annotation 
                        grafAnnotation.addFeature(key, value);
                     }
                     else
                     {
                        Out.prln("Null value specified for " + key + ": " + start + "-" + end);
                     }
                  }
               }
            }
         }
         if (!addedToSet)
         {
            //if here, we must have skipped adding the annotation, and maybe just added a feature to the 
            //annotation. so add the annotation to the set of annotations
            set.addAnnotation(grafAnnotation);
         }
      }

      //now lets get back to the pairs, pairs is a list of...well...pairs
      //each pair object is node id as the first part, and a bunch of child ids concatenated
      //into a long string as the second part
      for (Pair p : pairs)
      {
         //set up a string tokenizer using the second part of the pair, which is a bunch of child ids concatenated
         //into a long string; these are the children of the node whose id is the first part of the pair
         StringTokenizer sT = new StringTokenizer((String) p.second);
         //go through the tokens of aforementioned string tokenizer
         while (sT.hasMoreTokens())
         {
            //add this edge to the graph, which is the id of a node as the fromNode and the nextToken of the 
            //second part of the pair, ( an id of a child node )
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

//   public void setVersion(String version)
//   {
//      this.version = version;
//   }
//
//   public String getVersion()
//   {
//      return version;
//   }

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

   public String getGrafASType()
   {
      return grafASType;
   }

   public void setGrafASName(String name)
   {
      grafASName = name;
   }

   public String getGrafASName()
   {
      return grafASName;
   }

   public void setGrafDefaultASType(String uri)
   {
      grafDefaultASType = uri;
   }

   public String getGrafDefaultASType()
   {
      return grafDefaultASType;
   }
}
