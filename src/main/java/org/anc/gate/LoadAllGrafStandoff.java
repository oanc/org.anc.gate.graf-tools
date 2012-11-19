package org.anc.gate;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Resource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import gate.util.Out;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

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
import org.xces.graf.io.GrafParser;
import org.xces.graf.io.dom.DocumentHeader;
import org.xces.graf.io.dom.ResourceHeader;

public class LoadAllGrafStandoff extends ANCLanguageAnalyzer
{
   private static final long serialVersionUID = 1L;

   public static final String STANDOFFASNAME_PARAMETER = "standoffASName";
   public static final String SOURCE_URL_PARAMETER = "sourceUrl";
   public static final String RESOURCE_HEADER_PARAMETER_NAME = "resourceHeader";

   protected String standoffASName = null;
   protected URL sourceUrl = null;
   private URL resourceHeader;

   protected transient GrafParser parser;
   protected transient GetRangeFunction getRangeFn = new GetRangeFunction();
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
         throw new ResourceInstantiationException("Unable to initialized the GraphParser", ex);
      }
      return this;
   }

   @Override
   public void execute() throws ExecutionException
   {
      URL url = document.getSourceUrl();
      content = document.getContent().toString();
      endOfContent = content.length();

//      Out.prln("Loading standoff for : " + document.getName());
      File file = FileUtils.toFile(url);
      if (!file.exists())
      {
         throw new ExecutionException("Could not locate original document: " + file.getAbsolutePath());
      }
      File parentDir = file.getParentFile();
      String filename = file.getName();
      String basename = filename.substring(0, filename.length() - 4);
      String headerName = basename + ".hdr";
      File headerFile = new File(parentDir, headerName);
      if (!headerFile.exists())
      {
         throw new ExecutionException("Unable to locate document header for file " + file.getPath());
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
         throw new RuntimeException("Could not locate header file " + headerFile.getPath());
      }
      
      try
      {
         for (String type : docHeader.getAnnotationTypes())
         {
            File soFile = new File(parentDir, docHeader.getAnnotationLocation(type));
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
                  throw new ExecutionException(ex);
               }
            }
         }
      }
      catch (GrafException e)
      {
         throw new ExecutionException("Error getting annotation types from the header.");
      }
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

   protected void addAnnotation(INode node, String type) throws InvalidOffsetException, ExecutionException
   {
      getRangeFn.reset();
      Offset offset = getRangeFn.apply(node);
      if (offset.getEnd() < offset.getStart())
      {
         return;
      }

      StringBuilder ids = new StringBuilder();
      for (IEdge e :node.getOutEdges())
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
            if(node.getOutEdges().size() > 0)
            {
               newFeatures.put(Graf.GRAF_EDGE, ids.toString());
            }
            newFeatures.put(Graf.GRAF_ID, node.getId());
            String label = a.getLabel();
            addFeatures(a.getFeatures(), newFeatures, null);
            
            AnnotationSet annotations = getAnnotations(type);
            long start = offset.getStart();
            long end = offset.getEnd();
            try
            {
               if (end > endOfContent)
               {
                  System.err.println("Invalid end offset for " + label + " "
                        + end + ", end of content = " + endOfContent);

                  end = endOfContent;
               }
               if (start > end )
               {
                  System.err.println("Invalid start offset for " + label + " "
                        + start + ", end of content = " + endOfContent);
               }
               else
               {
                  annotations.add(start, end, label, newFeatures);
               }
            }
            catch (InvalidOffsetException e)
            {
               System.err.println("Invalid offsets for " + label);
               System.err.println("Annotation span : " + start + " - " + end);
               throw new InvalidOffsetException("Invalid offsets for " + label
                     + " from " + offset.getStart() + " to " + offset.getEnd());
            }
         }
   }

   protected void addFeatures(IFeatureStructure featStruc, FeatureMap fm,
         String base)
   {
      IFeatureStructure fs = featStruc;
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


   protected void test()
   {
      try
      {
         System.setProperty("gate.home", "d:/Applications/Gate-5.0");
         Gate.init();
         Document doc = Factory.newDocument(new URL(
               "file:/D:/corpora/masc/ptb/graf/110cyl067-ptb.xml"));
         System.out.println("Document loaded");
         Resource res = Factory.createResource("org.anc.gate.LoadGrafStandoff");
         LoadGrafStandoff load = (LoadGrafStandoff) res;
         System.out.println("Resource created.");
         load.setDocument(doc);
         load.execute();
         System.out.println("Done");
      }
      catch (Exception ex)
      {
         System.out.println(ex);
         ex.printStackTrace();
      }
   }

   public static void main(String[] args)
   {
      LoadGrafStandoff app = new LoadGrafStandoff();
      app.test();
   }
}

