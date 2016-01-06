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
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.Err;
import gate.util.InvalidOffsetException;
import gate.util.Out;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.anc.io.UTF8Reader;
import org.apache.commons.io.FileUtils;
import org.xces.graf.api.GrafException;
import org.xces.graf.api.IAnnotation;
import org.xces.graf.api.IAnnotationSpace;
import org.xces.graf.api.IEdge;
import org.xces.graf.api.IFeature;
import org.xces.graf.api.IFeatureStructure;
import org.xces.graf.api.IGraph;
import org.xces.graf.api.INode;
import org.xces.graf.api.IRegion;
import org.xces.graf.io.GrafParser;
import org.xces.graf.io.dom.DocumentHeader;
import org.xces.graf.io.dom.ResourceHeader;
import org.xces.graf.util.GraphUtils;
import org.xml.sax.SAXException;

/**
 * @author Keith Suderman
 * @version 1.0
 */
@CreoleResource(
        name = "GrAF Document",
        comment = "A single document with GrAF standoff annotations loaded."
)
public class GrafDocument extends gate.corpora.DocumentImpl implements LanguageResource
{
   private static final long serialVersionUID = 2L;

   //   protected transient GetRangeFunction getRangeFn = new GetRangeFunction();
   protected transient int endOfContent = 0;

   private Boolean loadStandoff = Boolean.TRUE;
   private String standoffASName = "Original markups";
   private List<String> standoffAnnotations = null;
   private boolean standoffLoaded = false;
   private String contentEncoding = "UTF-8";
   private URL resourceHeader;

   private Hashtable<String, String> ancAnnotations = null;
   protected AnnotationSet gateAnnotations = null;
   protected Set<String> seen = new HashSet<String>();
   AnnotationSet as;

   @CreoleParameter(comment = "The corpus resource header.",
           suffixes = ".xml"
   )
   public void setResourceHeader(URL location)
   {
      Out.prln("Setting resource header " + location.toExternalForm());
      this.resourceHeader = location;
   }
   public URL getResourceHeader()
   {
      return resourceHeader;
   }

   @Optional
   @CreoleParameter(
           comment = "If set to false no standoff annotations will be loaded.",
           defaultValue = "true"
   )
   public void setLoadStandoff(Boolean save)
   {
      loadStandoff = save;
   }
   public Boolean getLoadStandoff()
   {
      return loadStandoff;
   }

   // This not a Creole parameter. standoffLoaded will be set if any standfoff
   // annotations are loaded.
   public void setStandoffLoaded(boolean loaded)
   {
      standoffLoaded = loaded;
   }
   public boolean getStandoffLoaded()
   {
      return standoffLoaded;
   }

   @CreoleParameter(
           comment = "The AnnotationSet where new annotations will be created.",
           defaultValue = "Original markups"
   )
   public void setStandoffASName(String name)
   {
      standoffASName = name;
   }
   public String getStandoffASName()
   {
      return standoffASName;
   }

   @Optional
   @CreoleParameter(comment = "The list of annotations to be loaded. If this list is empty then all annotations will be loaded.")
   public void setStandoffAnnotations(List<String> list)
   {
      standoffAnnotations = list;
   }
   public List<String> getStandoffAnnotations()
   {
      return standoffAnnotations;
   }

   @CreoleParameter(comment = "Character encoding of the document content. Defaults to UTF-8",
   defaultValue = "UTF-8")
   public void setContentEncoding(String encoding)
   {
      contentEncoding = encoding;
   }
   public String getContentEncoding()
   {
      return contentEncoding;
   }

   @CreoleParameter(disjunction = "source",
           priority = 1,
           comment="The GrAF header file for the document to be loaded.",
           suffixes = ".hdr;.xml"
   )
   public void setSourceUrl(URL url) {
      super.setSourceUrl(url);
   }
   public URL getSourceUrl() {
      return super.getSourceUrl();
   }

   @CreoleParameter(disjunction = "source", priority = 2,
           comment = "The content of the document. This should not typically be used.")
   public void setStringContent(String stringContent) {
      super.setStringContent(stringContent);
   } // set StringContent
   public String getStringContent() {
      return super.getStringContent();
   }

   public GrafDocument()
   {
   }


   @Override
   public Resource init() throws ResourceInstantiationException
   {
      super.init();

      IGraph graph;
      File fullPath;
      //from the gate gui
      URL url = super.getSourceUrl();
      if (url != null)
      {
         fullPath = FileUtils.toFile(url);
      }
      else
      {
         Out.println("No file found: sourceUrl = " + url);
         return null;
      }

      // Load a DocumentHeader object to query the data it contains.
      DocumentHeader docHeader = null;
      try
      {
         docHeader = new DocumentHeader(fullPath);
      }
      catch (FileNotFoundException e)
      {
         throw new ResourceInstantiationException("Error loading document header.", e);
      }

      // Discard the XML from the header
      AnnotationSet originalMarkups = getAnnotations("Original markups");
      if (originalMarkups != null)
      {
         originalMarkups.clear();
      }

      // Get the text for the document
      String filename = null;
      try
      {
         filename = docHeader.getContentLocation();
      }
      catch (GrafException e)
      {
         throw new ResourceInstantiationException("Unable to locate the primary data.", e);
      }
      //file should be there...
      if (filename == null)
      {
         throw new ResourceInstantiationException("Document content not specified in the headern.");
      }

      //get the original text, see getContent below
      File txtFile = new File(fullPath.getParentFile(), filename);
      if (!txtFile.exists())
      {
         throw new ResourceInstantiationException("Primary data not found: " + txtFile.getPath());
      }
//      Out.prln("Loading text from " + txtFile.getPath());
      String theContent = null;
      try
      {
         theContent = getContent(new File(fullPath.getParentFile(), filename));
      }
      catch (IOException e)
      {
         throw new ResourceInstantiationException("Unable to load text content.", e);
      }

      endOfContent = theContent.length();
      if (endOfContent == 0)
      {
         throw new ResourceInstantiationException("Text file is empty");
      }
      // Replace the DocumentContent.  The current DocumentContent contains the
      // ANC header file, which not what we want the user to see.
      //make a new gate Document Content object with the original text
      DocumentContent docContent = new DocumentContentImpl(theContent);
      //set it to this object, and move on, nothing to see here...
      this.setContent(docContent);

      // Get a parser for the standoff annotations
      //AnnotationParser parser = new AnnotationParser();
      try
      {
         ResourceHeader header = new ResourceHeader(resourceHeader.openStream());
         GrafParser graphParser = new GrafParser(header);
         //This is a gate Annotation set made using standoffASName, that comes from the
         //gate GUI, Gate uses the setStandoffASName above to fill it, getAnnotations is a gate method, not ours
         gateAnnotations = this.getAnnotations(standoffASName);
         //get an iterator from the standoffAnnotations; ie iterate through the stand off file names
         if (standoffAnnotations == null || standoffAnnotations.size() == 0)
         {
            standoffAnnotations = docHeader.getAnnotationTypes();
         }
         for (String type : standoffAnnotations)
         {
            //ok get the file name for each standoff file type, remember we are working with a *.anc file here..
            filename = docHeader.getAnnotationLocation(type);
            Out.prln("Loading annotation " + type + " from " + filename);
            // System.out.println("filename is " + filename);
            if (filename != null)
            {
               try
               {
                  //get the file name from this iteration, call the parser for that file now...
                  //and pull out the annotations from this iterations standoff file.
                  //  newAnnotations = parser.parse(basePath + System.getProperty("file.separator") + filename);
                  graph = graphParser.parse(new File(fullPath.getParentFile(), filename));
               }
               catch (Exception e)
               {
                  Out.println("Could not load graph from " + filename);
                  throw new ResourceInstantiationException(e);
               }
               //cycle through the nodes only here, making it look easy
               for (INode node : graph.nodes())
               {
                  //this is where the magic happens...see comments in addAnnotations
                  addAnnotation(node);
               }
            }
         }
      }
      catch (SAXException e)
      {
         Out.println("Could not create GraphParser");
      }
      catch (Exception e)
      {
         Out.prln("Unable to load GrafDocument");
         e.printStackTrace();
      }
      return this;
   }

   /**
    * given the string version of the file name of a standoff file, coming from
    * the *.anc document, checks the string is in the annotations hash table and
    * returns if it exists, otherwise prints err message..actually can return
    * anything from annotations hash table if there is a key value pair
    */
   protected String _getFileForType(String type) throws ResourceInstantiationException
   {
      //returns the standoff file name tied to the key type..
      String result = ancAnnotations.get(type);
      if (result == null)
      {
//      throw new ResourceInstantiationException(
         Out.prln("Could not find the " + type + " annotation element.");
      }
      return result;
   }

   /**
    * gets the original text document
    *
    * @param file
    * @return
    * @throws ResourceInstantiationException
    */
   protected String getContent(File file) throws IOException // ResourceInstantiationException
   {
//      try
//      {
         UTF8Reader reader = new UTF8Reader(file);
         return reader.readString();
//      }
//      catch (IOException e)
//      {
//         throw new ResourceInstantiationException("Unable to load content", e);
//      }
   }

   /**
    * given gate Annotation set taken from *.anc document returns
    * hashtable<String,String> with type as key and file name as string value
    *
    * @param set
    * @return
    * @throws ResourceInstantiationException
    */
   protected Hashtable<String, String> _getAnnotationFiles(AnnotationSet set)
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

         String type = getFeature(theFeatures, "f.id");
         if (type == null)
         {
            throw new ResourceInstantiationException("f.id attribute node found.");
         }
         String location = getFeature(theFeatures, "loc");
         if (location == null)
         {
            throw new ResourceInstantiationException("loc attribute not found.");
         }
         table.put(type, location);
      }
      return table;
   }

   protected String getFeature(FeatureMap fm, String name) throws ResourceInstantiationException
   {
      Object value = fm.get(name);
      if (value == null)
      {
         throw new ResourceInstantiationException("Missing feature " + name);
      }
      return value.toString();
   }

   /**
    * addAnnotation adds annotations to the gate annoationSet, it also adds
    * features to the annotations
    *
    * @param node
    * @throws InvalidOffsetException
    */
   protected void addAnnotation(INode node) throws InvalidOffsetException
   {
      IRegion span = GraphUtils.getSpan(node);
      long start = -2;
      long end = -1;
      try
      {
         start = (Long) span.getStart().getOffset();
         end = (Long) span.getEnd().getOffset();
      }
      catch (GrafException e)
      {
         throw new InvalidOffsetException(e.getMessage());
      }

      if (end < start)
      {
         Err.prln("Span for node " + node.getId() + " is empty (" + start + "," + end + ")");
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
      //cycle through the annotations of aformented node
      for (IAnnotation a : node.annotations())
      {
         if (seen.contains(a.getId()))
         {
            continue;
         }
         seen.add(a.getId());
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

         if (end > endOfContent)
         {
            System.err.println("Invalid end offset for " + label + " " + end + ", end of content = "
                    + endOfContent);

            end = endOfContent;
         }
         if (start > end)
         {
            System.err.println("Invalid start offset for " + label + " " + start + ", end of content = "
                    + endOfContent);
         }
         else
         {
            //if here, the offsets look ok, finally add the annotation to the
            //gate annotations object using the start, end, anc graf annotation name and the gate feature map
            if (gateAnnotations == null)
            {
               throw new NullPointerException("gateAnnotations is null");
            }
            if (label == null)
            {
               throw new NullPointerException("The label is null");
            }
            if (newFeatures == null)
            {
               throw new NullPointerException("newFeatures is null");
            }
            gateAnnotations.add(start, end, label, newFeatures);
         }
      }
   }

   /**
    * adds Features to the gate annotations, will recurse into feature
    * structures if nested
    *
    * @param fs
    * @param fm
    * @param base
    */
   protected void addFeatures(IFeatureStructure fs, FeatureMap fm, String base)
   {
      if (fs == null)
      {
         return;
      }

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
