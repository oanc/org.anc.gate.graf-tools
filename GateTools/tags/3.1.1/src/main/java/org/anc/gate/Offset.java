package org.anc.gate;

import org.anc.util.Pair;

/**
 * @author Keith Suderman
 */
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
