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
import java.net.URL;

import org.anc.conf.AnnotationConfig;
import org.anc.conf.AnnotationSpaces;
import org.anc.conf.AnnotationType;
import org.anc.conf.MascConfig;
import org.anc.gate.core.ANCLanguageAnalyzer;
import org.apache.commons.io.FileUtils;
import org.xces.graf.api.IAnnotation;
import org.xces.graf.api.IAnnotationSpace;
import org.xces.graf.api.IEdge;
import org.xces.graf.api.IFeature;
import org.xces.graf.api.IFeatureStructure;
import org.xces.graf.api.IGraph;
import org.xces.graf.api.INode;
import org.xces.graf.io.GraphParser;
import org.xces.graf.io.dom.ResourceHeader;
import org.xml.sax.SAXException;

public class LoadAllGrafStandoff extends ANCLanguageAnalyzer
{
   private static final long serialVersionUID = 1L;

   public static final String STANDOFFASNAME_PARAMETER = "standoffASName";
   public static final String SOURCE_URL_PARAMETER = "sourceUrl";
   public static final String RESOURCE_HEADER_PARAMETER_NAME = "resourceHeader";

//   public static final String[] _TYPES =
//   {
//     "logical", "s", "ne", "vc", "nc", "ptb", "fn", "mpqa", "cb", "event", "penn" 
//   };
   
   protected String standoffASName = null;
   protected URL sourceUrl = null;
   private URL resourceHeader;

   protected transient GraphParser parser;
//   protected AnnotationSet annotations;
//   protected Map<String, AnnotationSet> annotations = new HashMap<String,AnnotationSet>();
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
         parser = new GraphParser();
         File headerFile = FileUtils.toFile(resourceHeader);
         ResourceHeader header = new ResourceHeader(headerFile);
         for (IAnnotationSpace aspace : header.getAnnotationSpaces())
         {
            parser.addAnnotationSpace(aspace);
         }         
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

      Out.prln("Loading standoff for : " + document.getName());
      File file = FileUtils.toFile(url);
      if (!file.exists())
      {
         throw new ExecutionException("Could not locate original document: " + file.getAbsolutePath());
      }
      File parentDir = file.getParentFile();
      String filename = file.getName();
      String basename = filename.substring(0, filename.length() - 4);
      AnnotationConfig config = new MascConfig();
      for (String type : config.types())
      {
         String soFilename = basename + "-" + type + ".xml";
         File soFile = new File(parentDir, soFilename);
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
//               Out.prln("Error loading standoff.");
//               ex.printStackTrace();
               throw new ExecutionException(ex);
            }
         }
      }
//      Out.println("Execution complete.");
   }

   public void setResourceHeader(URL location)
   {
      this.resourceHeader = location;
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
//      for (IAnnotationSet aSet : node.annotationSets())
//      {
//         String aSetName = aSet.getType();
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
            //for (IFeatureStructureElement fse : a.features())
            //for (IFeature fse : a.features())
            //{
              // addFeatures((IFeatureStructure) a.features(), features, null);
//             addFeatures(fse.feature)
//             System.out.println(fse.toString());
            //}
//            System.out.println("Adding annotation " + label + " from "
//                  + offset.getStart() + " to " + offset.getEnd());
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
      //}
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
//       if (e instanceof IFeature)
//       {
//       IFeature f = (IFeature) e;
//       if (type == null)
//       {
//       features.put(f.getName(), f.getValue());
//       }
//       else
//       {
//       features.put(type + "." + f.getName(), f.getValue());
//       }
//       }
//       else
//       {
//       IFeatureStructure childFS = (IFeatureStructure) e;
//       String childType = childFS.getType();
//       if (childType == null)
//       {
//       childType = type;
//       }
//       else
//       {
//       if (type != null)
//       {
//       childType = type + "." + childType;
//       }
//       }
//       addFeatures(childFS, features, childType);
//       }
      }
   }

   /*
   @Deprecated
   protected Offset getOffset(INode node)
   {
      Offset offset = (Offset) node.getUserObject();
      if (offset != null)
      {
         return offset;
      }

      long start = Long.MAX_VALUE;
      long end = Long.MIN_VALUE;
      for (IEdge e : node.getOutEdges())
      {
         INode to = e.getTo();
         offset = getOffset(to);
         if (offset != null)
         {
            if (offset.getStart() < start)
            {
               start = offset.getStart();
            }
            if (offset.getEnd() > end)
            {
               end = offset.getEnd();
            }
         }
      }
      if (end <= start)
      {
         return null;
      }

      offset = new Offset(start, end);
//    System.out.print("Creating offset for node " + node.getId());
//System.out.println(" from " + offset.getStart() + " to " +
//    offset.getEnd());
      node.setUserObject(offset);
      return offset;
   }
*/
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
//       List<String> types = new Vector<String> ();
//       types.add("ptb");
//       load.setTypes(types);
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

//class Offset extends Pair<Long, Long>
//{
//   public Offset()
//   {
//      super(Long.MAX_VALUE, Long.MIN_VALUE);
//   }
//
//   public Offset(long start, long end)
//   {
//      super(start, end);
//   }
//
//   public Long getStart()
//   {
//      return first;
//   }
//
//   public Long getEnd()
//   {
//      return second;
//   }
//
//   public void setStart(long start)
//   {
//      setFirst(start);
//   }
//
//   public void setEnd(long end)
//   {
//      setSecond(end);
//   }
//
//}
//
//class GetRangeFunction implements IFunction<INode, Offset>
//{
//   protected Offset offset = new Offset();
//
//   public Offset apply(INode item)
//   {
//      for (ILink link : item.links())
//      {
//         for (IRegion region : link)
//         {
//            getRange(region);
//         }
//      }
//      for (IEdge e : item.getOutEdges())
//      {
//         apply(e.getTo());
//      }
//      return offset;
//   }
//
//   private void getRange(IRegion region)
//   {
//      IAnchor startAnchor = region.getStart();
//      IAnchor endAnchor = region.getEnd();
//      if (!(startAnchor instanceof CharacterAnchor)
//            || !(endAnchor instanceof CharacterAnchor))
//      {
//         return;
//      }
//
//      CharacterAnchor start = (CharacterAnchor) startAnchor;
//      CharacterAnchor end = (CharacterAnchor) endAnchor;
//      if (start.getOffset() < offset.getStart())
//      {
//         offset.setStart(start.getOffset());
//      }
//      if (end.getOffset() > offset.getEnd())
//      {
//         offset.setEnd(end.getOffset());
//      }
//   }
//
//   public void reset()
//   {
//      offset.setStart(Long.MAX_VALUE);
//      offset.setEnd(Long.MIN_VALUE);
//   }
//}
//
//class StandoffFilter extends PrefixFilter
//{
//
//   public StandoffFilter(String prefix)
//   {
//      super(prefix);
//   }
//
//   @Override
//   public boolean accept(File file)
//   {
//      if (!file.getName().endsWith(".xml"))
//      {
//         return false;
//      }
//      return super.accept(file);
//   }
//}
