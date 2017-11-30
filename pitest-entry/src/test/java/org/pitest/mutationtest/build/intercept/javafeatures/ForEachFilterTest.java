package org.pitest.mutationtest.build.intercept.javafeatures;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.engine.gregor.config.Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.NonVoidMethodCallMutator;

public class ForEachFilterTest {
  private static final String             PATH      = "foreach/{0}_{1}";
  
  ForEachLoopFilter testee = new ForEachLoopFilter();
  FilterTester verifier = new FilterTester(PATH, testee, Mutator.all());  
  
  @Test
  public void declaresTypeAsFilter() {
    assertThat(testee.type()).isEqualTo(InterceptorType.FILTER);
  }
  
  @Test
  public void filtersMutationsToForEachLoopJumps() {
    verifier = new FilterTester(PATH, testee, NegateConditionalsMutator.NEGATE_CONDITIONALS_MUTATOR);  
    verifier.assertFiltersNMutationFromSample(1, "HasForEachLoop");
  }
  
  @Test
  public void filtersMutationsToHasNextAndNext() {
    verifier = new FilterTester(PATH, testee, NonVoidMethodCallMutator.NON_VOID_METHOD_CALL_MUTATOR);  
    // can mutate calls to iterator, hasNext and next
    verifier.assertFiltersNMutationFromSample(3, "HasForEachLoop");
  }
  
  @Test
  public void filtersMutationsToForEachOverField() {
    verifier = new FilterTester(PATH, testee, NonVoidMethodCallMutator.NON_VOID_METHOD_CALL_MUTATOR);  
    // can mutate calls to iterator, hasNext and next
    verifier.assertFiltersNMutationFromClass(3, HasForEachLoopOverField.class);
  }
  
  
  @Test
  public void doesNotFilterMutationsToIndexedForLoopJumps() {
    verifier = new FilterTester("forloops/{0}_{1}", testee, NegateConditionalsMutator.NEGATE_CONDITIONALS_MUTATOR);       
    verifier.assertFiltersNMutationFromSample(0, "HasAForLoop");    
  }  
  
  @Test
  public void doesNotFilterMutationsToHandRolledIteratorLoops() {
    // additional label nodes seem to be enough to prevent triggering
    verifier.assertFiltersNMutationFromSample(0, "HandRolledIteratorLoop");    
  }  
    
  public static class HasForEachLoop {
    void foo(List<Integer> is) {
      for (int each : is) {
        System.out.println(each);
      }
    }
  }
  
  public static class HasForEachLoopOverField {
    List<Integer> is;
    void foo() {
      for (int each : is) {
        System.out.println(each);
      }
    }
  }
  
  static class HandRolledIteratorLoop {
    void foo(List<Integer> is) {
      Iterator<Integer> it = is.iterator();
      while (it.hasNext()) {
        Integer each = it.next();
        System.out.println(each);
      }
    }
  }

}
