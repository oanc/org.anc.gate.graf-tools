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
import gate.util.InvalidOffsetException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import gate.util.Out;
import org.anc.gate.core.ANCLanguageAnalyzer;
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

@CreoleResource(
        name = "GrAF Load All Standoff",
        comment = "Loads a text and all GrAF standoff annotations."
)
public class LoadAllGrafStandoff extends ANCLanguageAnalyzer
{
   private static final long serialVersionUID = 2L;

   private URL resourceHeader;
   protected String standoffASName;
   private Boolean failFast;
   /**
    * If set to true stack traces will be displayed on the GATE console. Setting to
    * false (the default) results in shorter error messages.
    */
   protected Boolean printStackTrace = Boolean.FALSE;


   protected transient GrafParser parser;
   protected transient String content = null;
   protected transient int endOfContent = 0;

   public LoadAllGrafStandoff()
   {
      super();
   }

   @Override
   public Resource init() throws ResourceInstantiationException
   {
      try
      {
         super.init();

         ResourceHeader header = new ResourceHeader(resourceHeader.openStream());
         parser = new GrafParser(header);
      }
      catch (Exception ex)
      {
         throw new ResourceInstantiationException(
               "Unable to initialized the GraphParser", ex);
      }
      return this;
   }

   @Override
   public void execute() throws ExecutionException
   {
      URL url = document.getSourceUrl();
      content = document.getContent().toString();
      endOfContent = content.length();

      File file = FileUtils.toFile(url);
      if (!file.exists())
      {
         throw new ExecutionException("Could not locate original document: "
               + file.getAbsolutePath());
      }
      File parentDir = file.getParentFile();
      String filename = file.getName();
      String basename = filename.substring(0, filename.length() - 4);
      String headerName = basename + ".hdr";
      File headerFile = new File(parentDir, headerName);
      if (!headerFile.exists())
      {
         throw new ExecutionException(
               "Unable to locate document header for file " + file.getPath());
      }

      DocumentHeader docHeader = null;
      try
      {
         docHeader = new DocumentHeader(headerFile);
      }
      catch (FileNotFoundException e)
      {
         // This should not happen since we check that the file exists. So
         // this means something really bad went wrong.
         if (failFast)
         {
            throw new RuntimeException("Could not locate header file "
                    + headerFile.getPath());
         }
         else
         {
            Out.prln("Unable to load header file for " + file.getPath());
            if (printStackTrace)
            {
               e.printStackTrace();
            }
            else
            {
               Out.prln(e.getMessage());
            }
            return;
         }
      }

      try
      {
         for (String type : docHeader.getAnnotationTypes())
         {
            File soFile = new File(parentDir,
                  docHeader.getAnnotationLocation(type));
            if (soFile.exists())
            {
               Out.prln("Attempting to load " + soFile.getPath());
               IGraph graph = null;
               try
               {
                  graph = parser.parse(soFile);
                  for (INode node : graph.nodes())
                  {
                     addAnnotation(node, type);
                  }
               }
               catch (Exception ex)
               {
                  if (failFast)
                  {
                     throw new ExecutionException(ex);
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
            }
         }
      }
      catch (GrafException e)
      {
         if (failFast) {
            throw new ExecutionException(
                    "Error getting annotation types from the header.");
         }
         else if (printStackTrace)
         {
            e.printStackTrace();
         }
         else
         {
            Out.prln(e.getMessage());
         }
      }
   }

   @RunTime(false)
   @Optional(false)
   @CreoleParameter(comment ="Corpus resource header.")
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
           comment = "The AnnotationSet where new annotations will be created.",
           defaultValue = "Standoff annotations"
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
   @CreoleParameter(
           comment = "When set to true causes pipelines to halt on the first exception.",
           defaultValue = "false"
   )
   public void setFailFast(Boolean failFast) { this.failFast = failFast; }
   public Boolean getFailFast() { return failFast; }

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

   protected void addAnnotation(INode node, String type)
         throws InvalidOffsetException, ExecutionException
   {
//      getRangeFn.reset();
//      Offset offset = getRangeFn.apply(node);
//      if (offset.getEnd() < offset.getStart())
//      {
//         return;
//      }
      IRegion span = GraphUtils.getSpan(node);
      if (span == null)
      {
         return;
      }

      long start = 0;
      long end = 0;
      try
      {
         if (span.getStart() != null)
         {
            start = (Long) span.getStart().getOffset();
         }
         if (span.getEnd() != null)
         {
            end = (Long) span.getEnd().getOffset();
         }
      }
      catch (Exception e)
      {
         throw new ExecutionException("Unable to get span offsets for node " + node.getId(), e);
      }

      if (start > end)
      {
         Out.prln("Invalid offsets (" + start + "," + end + ") for node " + node.getId());
         return;
      }
      
      StringBuilder ids = new StringBuilder();
      for (IEdge e : node.getOutEdges())
      {
         ids.append(e.getTo().getId() + " ");
      }
      for (IAnnotation a : node.annotations())
      {
         FeatureMap newFeatures = Factory.newFeatureMap();
         String aSetName = "Standoff Markups";
         IAnnotationSpace as = a.getAnnotationSpace();
         if (as != null)
         {
            aSetName = as.getName();
            newFeatures.put(Graf.GRAF_SET, aSetName);
         }
         if (node.getOutEdges().size() > 0)
         {
            newFeatures.put(Graf.GRAF_EDGE, ids.toString());
         }
         newFeatures.put(Graf.GRAF_ID, node.getId());
         String label = a.getLabel();
         addFeatures(a.getFeatures(), newFeatures, null);

         AnnotationSet annotations = getAnnotations(type);
//         long start = 0; //offset.getStart();
//         long end = 0; //offset.getEnd();
         try
         {
//            start = (Long) span.getStart().getOffset();
//            end = (Long) span.getEnd().getOffset();
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
   }

   protected void addFeatures(IFeatureStructure fs, FeatureMap fm,
         String base)
   {
      if (fs == null)
      {
         return;
      }
      for (IFeature f : fs.features())
      {
         if (f.isAtomic())
         {
            if (base == null)
            {
               fm.put(f.getName(), f.getStringValue());
            }
            else
            {
               fm.put(base + "/" + f.getName(), f.getStringValue());
            }
         }
         else
         {
            IFeatureStructure childFS = (IFeatureStructure) f.getValue();
            String childName = null;
            if (base == null)
            {
               childName = f.getName();
            }
            else
            {
               childName = base + "/" + f.getName();
            }
            addFeatures(childFS, fm, childName);
         }
      }
   }

}
