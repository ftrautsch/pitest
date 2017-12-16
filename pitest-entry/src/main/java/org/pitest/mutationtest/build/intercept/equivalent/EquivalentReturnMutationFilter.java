package org.pitest.mutationtest.build.intercept.equivalent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.bytecode.analysis.MethodMatchers;
import org.pitest.bytecode.analysis.MethodTree;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.build.CompoundMutationInterceptor;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.gregor.mutators.BooleanFalseReturnValsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.BooleanTrueReturnValsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.EmptyObjectReturnValsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.NullReturnValsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.PrimitiveReturnsMutator;
import org.pitest.plugin.Feature;

/**
 * Tightly coupled to the PrimitiveReturnsMutator and EmptyObjectReturnValsMutator
 *   - removes trivially equivalent mutants generated by these.
 * operator
 *
 */
public class EquivalentReturnMutationFilter implements MutationInterceptorFactory {

  @Override
  public String description() {
    return "Trivial return vals equivalence filter";
  }

  @Override
  public Feature provides() {
    return Feature.named("FRETEQUIV")
        .withOnByDefault(true)
        .withDescription("Filters return vals mutants with bytecode equivalent to the unmutated class");

  }

  @Override
  public MutationInterceptor createInterceptor(InterceptorParameters params) {
    return new CompoundMutationInterceptor(Arrays.asList(new PrimitiveEquivalentFilter(), 
        new NullReturnsFilter(),
        new EmptyReturnsFilter(), 
        new HardCodedTrueEquivalentFilter())) {
      @Override
      public InterceptorType type() {
        return InterceptorType.FILTER;
      }
    };
  }

}

class HardCodedTrueEquivalentFilter implements MutationInterceptor {
  
  private static final Set<String> MUTATOR_IDS = new HashSet<>();
  private static final Set<Integer> TRUE_CONSTANTS = new HashSet<>();
  static {
    TRUE_CONSTANTS.add(Opcodes.ICONST_1);
    
    MUTATOR_IDS.add(BooleanTrueReturnValsMutator.BOOLEAN_TRUE_RETURN.getGloballyUniqueId());
  }
  
  private ClassTree currentClass;

  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public void begin(ClassTree clazz) {
    currentClass = clazz;
  }

  @Override
  public Collection<MutationDetails> intercept(
      Collection<MutationDetails> mutations, Mutater m) {
    return FCollection.filter(mutations, Prelude.not(isEquivalent(m)));
  }

  private F<MutationDetails, Boolean> isEquivalent(Mutater m) {
    return new F<MutationDetails, Boolean>() {
      @Override
      public Boolean apply(MutationDetails a) {
        if (!MUTATOR_IDS.contains(a.getMutator())) {
          return false;
        }
        int instruction = a.getInstructionIndex();
        MethodTree method = currentClass.methods().findFirst(MethodMatchers.forLocation(a.getId().getLocation())).value();
        return primitiveTrue(instruction, method) || boxedTrue(instruction, method);
      }

      private boolean primitiveTrue(int instruction, MethodTree method) {
        return method.instructions().get(instruction - 1).getOpcode() == Opcodes.ICONST_1;
      }

      private boolean boxedTrue(int instruction, MethodTree method) {
        return method.instructions().get(instruction - 2).getOpcode() == Opcodes.ICONST_1 && isValueOfCall(instruction - 1, method);
      }

    };
    
  }

  @Override
  public void end() {
    currentClass = null;
  }
  
  private boolean isValueOfCall(int instruction, MethodTree method) {
    AbstractInsnNode abstractInsnNode = method.instructions().get(instruction);
    if (abstractInsnNode instanceof MethodInsnNode) {
      return ((MethodInsnNode) abstractInsnNode).name.equals("valueOf");
    }
    return false;
  }
   
}


class PrimitiveEquivalentFilter implements MutationInterceptor {
  
  private static final Set<String> MUTATOR_IDS = new HashSet<>();
  private static final Set<Integer> ZERO_CONSTANTS = new HashSet<>();
  static {
    ZERO_CONSTANTS.add(Opcodes.ICONST_0);
    ZERO_CONSTANTS.add(Opcodes.LCONST_0);
    ZERO_CONSTANTS.add(Opcodes.FCONST_0);
    ZERO_CONSTANTS.add(Opcodes.DCONST_0);
    
    MUTATOR_IDS.add(PrimitiveReturnsMutator.PRIMITIVE_RETURN_VALS_MUTATOR.getGloballyUniqueId());
    MUTATOR_IDS.add(BooleanFalseReturnValsMutator.BOOLEAN_FALSE_RETURN.getGloballyUniqueId());
  }
  
  private ClassTree currentClass;

  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public void begin(ClassTree clazz) {
    currentClass = clazz;
  }

  @Override
  public Collection<MutationDetails> intercept(
      Collection<MutationDetails> mutations, Mutater m) {
    return FCollection.filter(mutations, Prelude.not(isEquivalent(m)));
  }

  private F<MutationDetails, Boolean> isEquivalent(Mutater m) {
    return new F<MutationDetails, Boolean>() {
      @Override
      public Boolean apply(MutationDetails a) {
        if (!MUTATOR_IDS.contains(a.getMutator())) {
          return false;
        }
        int intructionBeforeReturn = a.getInstructionIndex() - 1;
        MethodTree method = currentClass.methods().findFirst(MethodMatchers.forLocation(a.getId().getLocation())).value();
        return ZERO_CONSTANTS.contains(method.instructions().get(intructionBeforeReturn).getOpcode());
      }

    };
  }

  @Override
  public void end() {
    currentClass = null;
  }
  
}

class EmptyReturnsFilter implements MutationInterceptor {
  
  private static final Set<String> MUTATOR_IDS = new HashSet<>();
  
  private static final Set<Integer> ZERO_CONSTANTS = new HashSet<>();
  static {
    ZERO_CONSTANTS.add(Opcodes.ICONST_0);
    ZERO_CONSTANTS.add(Opcodes.LCONST_0);
    ZERO_CONSTANTS.add(Opcodes.FCONST_0);
    ZERO_CONSTANTS.add(Opcodes.DCONST_0);
    
    MUTATOR_IDS.add(EmptyObjectReturnValsMutator.EMPTY_RETURN_VALUES.getGloballyUniqueId());
    MUTATOR_IDS.add(BooleanFalseReturnValsMutator.BOOLEAN_FALSE_RETURN.getGloballyUniqueId());
  }
  
  private ClassTree currentClass;

  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public void begin(ClassTree clazz) {
    currentClass = clazz;
  }

  @Override
  public Collection<MutationDetails> intercept(
      Collection<MutationDetails> mutations, Mutater m) {
    return FCollection.filter(mutations, Prelude.not(isEquivalent(m)));
  }

  private F<MutationDetails, Boolean> isEquivalent(Mutater m) {
    return new F<MutationDetails, Boolean>() {
      @Override
      public Boolean apply(MutationDetails a) {
        if (!MUTATOR_IDS.contains(a.getMutator())) {
          return false;
        }

        MethodTree method = currentClass.methods().findFirst(MethodMatchers.forLocation(a.getId().getLocation())).value();
        int mutatedInstruction = a.getInstructionIndex();
        return returnsZeroValue(method, mutatedInstruction)
            || returnsEmptyString(method, mutatedInstruction) 
            || returns(method, mutatedInstruction, "java/util/Optional","empty")
            || returns(method, mutatedInstruction, "java/util/Collections","emptyList")   
            || returns(method, mutatedInstruction, "java/util/Collections","emptySet")
            || returns(method, mutatedInstruction, "java/util/List","of")
            || returns(method, mutatedInstruction, "java/util/Set","of");         
      }

      private Boolean returnsZeroValue(MethodTree method,
          int mutatedInstruction) {
        return isValueOf(method.instructions().get(mutatedInstruction - 1))
               && ZERO_CONSTANTS.contains(method.instructions().get(mutatedInstruction - 2).getOpcode());
      }

      private boolean isValueOf(AbstractInsnNode abstractInsnNode) {
        if (abstractInsnNode instanceof MethodInsnNode) {
          return ((MethodInsnNode) abstractInsnNode).name.equals("valueOf");
        }
        return false;
      }
      
      private boolean returns(MethodTree method, int mutatedInstruction, String owner, String name) {
        AbstractInsnNode node = method.instructions().get(mutatedInstruction - 1);
        if (node instanceof MethodInsnNode ) {
          MethodInsnNode call = (MethodInsnNode) node;
          return call.owner.equals(owner) && call.name.equals(name) && takesNoArguments(call.desc);
        }
        return false;
      }

      private boolean takesNoArguments(String desc) {
        return Type.getArgumentTypes(desc).length == 0;
      }
      
      private boolean returnsEmptyString(MethodTree method,
          int mutatedInstruction) {
        AbstractInsnNode node = method.instructions().get(mutatedInstruction - 1);
        if (node instanceof LdcInsnNode ) {
          LdcInsnNode ldc = (LdcInsnNode) node;
          return "".equals(ldc.cst);
        }
        return false;
      }
     
    };
  }

  @Override
  public void end() {
    currentClass = null;
  }
    
}

class NullReturnsFilter implements MutationInterceptor {
  
  private static final String MUTATOR_ID = NullReturnValsMutator.NULL_RETURN_VALUES.getGloballyUniqueId();
  
  private ClassTree currentClass;

  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public void begin(ClassTree clazz) {
    currentClass = clazz;
  }

  @Override
  public Collection<MutationDetails> intercept(
      Collection<MutationDetails> mutations, Mutater m) {
    return FCollection.filter(mutations, Prelude.not(isEquivalent(m)));
  }

  private F<MutationDetails, Boolean> isEquivalent(Mutater m) {
    return new F<MutationDetails, Boolean>() {
      @Override
      public Boolean apply(MutationDetails a) {
        if (!MUTATOR_ID.equals(a.getMutator())) {
          return false;
        }

        MethodTree method = currentClass.methods().findFirst(MethodMatchers.forLocation(a.getId().getLocation())).value();
        int mutatedInstruction = a.getInstructionIndex();
        return returnsNull(method, mutatedInstruction);
      }

      private Boolean returnsNull(MethodTree method,
          int mutatedInstruction) {
        return method.instructions().get(mutatedInstruction - 1).getOpcode() == Opcodes.ACONST_NULL;
      }     
    };
  }

  @Override
  public void end() {
    currentClass = null;
  }
    
}
