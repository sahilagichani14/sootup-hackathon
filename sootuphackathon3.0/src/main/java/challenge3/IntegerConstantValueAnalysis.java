package challenge3;

import sootup.analysis.intraprocedural.ForwardFlowAnalysis;
import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.AbstractBinopExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.Stmt;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class IntegerConstantValueAnalysis extends ForwardFlowAnalysis<Set<IntegerConstantValueAnalysis.Binding>> {

    private Set<Binding> result = new HashSet<>();

    public String printResult() {
        return result.stream()
                .sorted(Comparator.comparing(b -> b.symbol().getName()))
                .map(b -> b.value().toString())
                .collect(Collectors.joining(""));
    }

    record Binding(Local symbol, Integer value) {
        @Override
        public String toString() {
            return "<" + symbol.toString() + "," + value.intValue() + ">";
        }
    }

    public <B extends BasicBlock<B>> IntegerConstantValueAnalysis(StmtGraph<B> graph) {
        super(graph);
    }

    @Override
    protected void execute(){
        super.execute();
    }


    @Override
    protected void flowThrough(@Nonnull Set<IntegerConstantValueAnalysis.Binding> inSet, Stmt stmt, @Nonnull Set<IntegerConstantValueAnalysis.Binding> outSet) {
        copy(inSet, outSet);

        if(stmt instanceof JAssignStmt){
            JAssignStmt assign = (JAssignStmt) stmt;
            Value rightOp = assign.getRightOp();
            Value leftOp = assign.getLeftOp();

            /**
             * Handle the case where rightOp is an IntConstant.
             * If so add a new binding with leftOp and value of the IntConstant using addBindingToOutSet()
             */
            if(rightOp instanceof IntConstant){
                IntConstant intConstant =
                        (IntConstant) rightOp;
                Integer intValue = intConstant.getValue();
                Binding binding = new Binding((Local) leftOp, intValue);
                addBindingToOutSet(outSet, binding);
            }

            /**
             * TODO 1: handle the case where rightOp is a Local.
             * Use inSetContainsBinding() to check if we know rightOp's value.
             * Use getValueOfSymbol to get the value of rightOp from the inSet
             * can you find what is next once we know the value of rightOp?
             */
            if(rightOp instanceof Local){
                Local local = (Local) rightOp;
                boolean setContainsBinding = inSetContainsBinding(inSet, local);
                if (setContainsBinding) {
                    Integer valueofSymbol = getValueofSymbol(inSet, local);
                    Binding binding = new Binding((Local) leftOp, valueofSymbol);
                    addBindingToOutSet(outSet, binding);
                }
            }

            /**
             * TODO 2: handle the case where rightOp is an AbstractBinopExpr, e.g. in the form x + y
             * Think about how to handle the cases where op1 or op2 are Locals, or IntConstants
             * Implement and use calculate() method to calculate new values for this arithmetic operation.
             */
            if(rightOp instanceof AbstractBinopExpr){
                AbstractBinopExpr abstractBinopExpr = (AbstractBinopExpr) rightOp;
                Immediate op1 = abstractBinopExpr.getOp1();
                Immediate op2 = abstractBinopExpr.getOp2();

                // Case: both op1 and op2 are IntConstants
                if (op1 instanceof IntConstant && op2 instanceof IntConstant) {
                    int calculated = calculate(((IntConstant) op1).getValue(), ((IntConstant) op2).getValue(), abstractBinopExpr.getSymbol());
                    Binding binding = new Binding((Local) leftOp, calculated);
                    addBindingToOutSet(outSet, binding);
                }

                // Case: both op1 and op2 are Locals
                if (op1 instanceof Local && op2 instanceof Local) {
                    Local l1 = (Local) op1;
                    Local l2 = (Local) op2;
                    if (inSetContainsBinding(inSet, l1) && inSetContainsBinding(inSet, l2)) {
                        int v1 = getValueofSymbol(inSet, l1);
                        int v2 = getValueofSymbol(inSet, l2);
                        int calculated = calculate(v1, v2, abstractBinopExpr.getSymbol());
                        Binding binding = new Binding((Local) leftOp, calculated);
                        addBindingToOutSet(outSet, binding);
                    }
                }

                // Case: op1 is Local, op2 is IntConstant
                if (op1 instanceof Local && op2 instanceof IntConstant) {
                    Local l1 = (Local) op1;
                    if (inSetContainsBinding(inSet, l1)) {
                        int v1 = getValueofSymbol(inSet, l1);
                        int v2 = ((IntConstant) op2).getValue();
                        int calculated = calculate(v1, v2, abstractBinopExpr.getSymbol());
                        Binding binding = new Binding((Local) leftOp, calculated);
                        addBindingToOutSet(outSet, binding);
                    }
                }

                // Case: op1 is IntConstant, op2 is Local
                if (op1 instanceof IntConstant && op2 instanceof Local) {
                    Local l2 = (Local) op2;
                    if (inSetContainsBinding(inSet, l2)) {
                        int v1 = ((IntConstant) op1).getValue();
                        int v2 = getValueofSymbol(inSet, l2);
                        int calculated = calculate(v1, v2, abstractBinopExpr.getSymbol());
                        Binding binding = new Binding((Local) leftOp, calculated);
                        addBindingToOutSet(outSet, binding);
                    }
                }
            }
        }

        if(stmt instanceof JReturnVoidStmt){
            result.addAll(outSet);
        }
    }

    static int calculate(int a, int b, String symbol) {
        return switch (symbol.trim()) {
            // TODO 3: implement the rest of the arithmetic operations
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            case "%" -> a % b;
            default -> throw new IllegalArgumentException("Unknown operator: " + symbol);
        };
    }


    @Nonnull
    @Override
    protected Set<IntegerConstantValueAnalysis.Binding> newInitialFlow() {
        return new HashSet<>();
    }

    @Override
    protected void merge(@Nonnull Set<IntegerConstantValueAnalysis.Binding> left, @Nonnull Set<IntegerConstantValueAnalysis.Binding> right, @Nonnull Set<IntegerConstantValueAnalysis.Binding> out) {
        Map<Local, Integer> resultMap = new HashMap<>();
        // Add all bindings from the first set
        for (Binding b : left) {
            resultMap.put(b.symbol, b.value);
        }

        // Merge the second set
        for (Binding b : right) {
            if(resultMap.get(b.symbol)!=null){
                // exist but different value - set value to MAX_VALUE
                if(!resultMap.get(b.symbol).equals(b.value)){
                    resultMap.put(b.symbol, Integer.MAX_VALUE);
                }
            } else {
                // does not exist add to set
                resultMap.put(b.symbol, b.value);
            }
        }

        for (Map.Entry<Local, Integer> entry : resultMap.entrySet()) {
            out.add(new Binding(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    protected void copy(@Nonnull Set<IntegerConstantValueAnalysis.Binding> in, @Nonnull Set<IntegerConstantValueAnalysis.Binding> out) {
        updateConflicts(in);
        for (IntegerConstantValueAnalysis.Binding binding : in) {
            out.add(new Binding(binding.symbol, binding.value));
        }
    }

    private void updateConflicts(Set<Binding> bindings) {
        Map<Local, Integer> symbolToValue = new HashMap<>();
        Set<Local> toUpdate = new HashSet<>();
        for (Binding b : bindings) {
            Integer existingValue = symbolToValue.get(b.symbol());
            if (existingValue != null) {
                toUpdate.add(b.symbol);
            }
            symbolToValue.putIfAbsent(b.symbol(), b.value());
        }
        for(Local symbol: toUpdate){
            bindings.removeIf(e ->e.symbol.equals(symbol));
            bindings.add(new Binding(symbol, Integer.MAX_VALUE));
        }
    }

    private void addBindingToOutSet(Set<IntegerConstantValueAnalysis.Binding> outSet, Binding binding){
        // remove previous binding of the same symbol
        outSet.removeIf(b -> b.symbol.equals(binding.symbol));
        outSet.add(binding);
    }

    private boolean inSetContainsBinding(Set<IntegerConstantValueAnalysis.Binding> inSet, Local symbol){
        return inSet.stream().filter(b -> b.symbol.equals(symbol)).findAny().isPresent();
    }

    private Integer getValueofSymbol(Set<IntegerConstantValueAnalysis.Binding> set, Local symbol){
        Optional<Binding> binding = set.stream().filter(b ->b.symbol.equals(symbol)).findFirst();
        if(binding.isPresent()){
            return binding.get().value;
        }
        return Integer.MIN_VALUE;
    }

}
