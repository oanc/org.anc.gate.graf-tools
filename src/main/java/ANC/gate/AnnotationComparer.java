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

package ANC.gate;

import java.util.Comparator;

import gate.Annotation;

/** Compares two GATE annotations by their end offsets.  If the end offsets
 *  are equal the annotations will be compared by start offsets.
 * 
 * @author Keith Suderman
 * @version 1.0
 */
class EndComparer implements Comparator<Annotation>
{

   @Override
   public int compare(Annotation left, Annotation right)
   {
      long loffset = left.getEndNode().getOffset().longValue();
      long roffset = right.getEndNode().getOffset().longValue();

      if (loffset == roffset)
      {
         // if the annotations end at the same location sort by start offset.
         // the one with the smallest start offset should come first
         loffset = left.getStartNode().getOffset().longValue();
         roffset = right.getStartNode().getOffset().longValue();
         return (int) (roffset - loffset);
      }
      return (int) (loffset - roffset);
   }
}

/** Compares two GATE annotations by their start offsets.  If the start offsets
 *  are equal the annotations will be compared by end offsets.
 * 
 * @author Keith Suderman
 * @version 1.0
 */
class StartComparer implements Comparator<Annotation>
{
   public int compare(Annotation left, Annotation right)
   {
      long loffset = left.getStartNode().getOffset().longValue();
      long roffset = right.getStartNode().getOffset().longValue();
      if (loffset == roffset)
      {
         // if the start offsets are the same compare end offsets.
         // the largest offset should come first
         loffset = left.getEndNode().getOffset().longValue();
         roffset = right.getEndNode().getOffset().longValue();
         return (int) (roffset - loffset);
      }
      return (int) (loffset - roffset);
   }

   protected boolean ofInterest(String left, String right)
   {
      if ("s".equals(left) && "head".equals(right))
      {
         return true;
      }
      if ("s".equals(right) && "head".equals(left))
      {
         return true;
      }
      return false;
   }
}

public class AnnotationComparer // implements Comparator
{
   private AnnotationComparer()
   {
   }

   public static final int START = 0;
   public static final int END = 1;

   public static Comparator<Annotation> create()
   {
      return create(START);
   }

   public static Comparator<Annotation> create(int type)
   {
      if (START == type)
         return new StartComparer();
      return new EndComparer();
   }

   public static Comparator<Annotation> createStartComparator()
   {
      return new StartComparer();
   }
   
   public static Comparator<Annotation> createEndComparator()
   {
      return new EndComparer();
   }
   
   protected long getStart(Annotation a)
   {
      return a.getStartNode().getOffset().longValue();
   }

   protected long getEnd(Annotation a)
   {
      return a.getEndNode().getOffset().longValue();
   }
}
