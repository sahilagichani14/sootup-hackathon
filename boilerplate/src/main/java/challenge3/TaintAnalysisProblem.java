package challenge3;

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Gen;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;
import sootup.analysis.interprocedural.ifds.DefaultJimpleIFDSTabulationProblem;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.SootMethod;
import sootup.core.types.NullType;

import java.util.*;

public class TaintAnalysisProblem extends DefaultJimpleIFDSTabulationProblem<Value, InterproceduralCFG<Stmt, SootMethod>> {

    private SootMethod entryMethod;

    protected InterproceduralCFG<Stmt, SootMethod> icfg;

    public TaintAnalysisProblem(
            InterproceduralCFG<Stmt, SootMethod> icfg, SootMethod entryMethod) {
        super(icfg);
        this.icfg = icfg;
        this.entryMethod = entryMethod;
    }

    @Override
    public Map<Stmt, Set<Value>> initialSeeds() {
        return DefaultSeeds.make(
                Collections.singleton(entryMethod.getBody().getStmtGraph().getStartingStmt()), zeroValue());
    }

    @Override
    protected FlowFunctions<Stmt, Value, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Stmt, Value, SootMethod>() {

            @Override
            public FlowFunction<Value> getNormalFlowFunction(Stmt curr, Stmt succ) {
                return getNormalFlow(curr, succ);
            }

            @Override
            public FlowFunction<Value> getCallFlowFunction(Stmt callStmt, SootMethod destinationMethod) {
                return getCallFlow(callStmt, destinationMethod);
            }

            @Override
            public FlowFunction<Value> getReturnFlowFunction(
                    Stmt callSite, SootMethod calleeMethod, Stmt exitStmt, Stmt returnSite) {
                return getReturnFlow(callSite, calleeMethod, exitStmt, returnSite);
            }

            @Override
            public FlowFunction<Value> getCallToReturnFlowFunction(Stmt callSite, Stmt returnSite) {
                return getCallToReturnFlow(callSite, returnSite);
            }
        };
    }

    @Override
    protected Value createZeroValue() {
        return new Local("<<zero>>", NullType.getInstance());
    }

    FlowFunction<Value> getNormalFlow(Stmt curr, Stmt succ) {
        // Challenge 3
        // think about possible assignment cases where current statement (curr) is in the form: leftOp = rightOp
        // generate taint only at leftOp = "SECRET"
        // Hint: to generate a new taint: return new Gen<>(leftOp, zeroValue()); (a built in FlowFunction)
        // Hint: to propagate an existing taint to other variables, implement your custom FlowFunction
        // Hint: what happens if a tainted variable leftOp, is assigned with something else leftOp = "..."?
        // Implement your solution here:

        if (curr instanceof JAssignStmt) {
            JAssignStmt assign = (JAssignStmt) curr;
            Value leftOp = assign.getLeftOp();
            Value rightOp = assign.getRightOp();
            if ((rightOp instanceof StringConstant) && (((StringConstant) rightOp).getValue().equals("SECRET"))) {
                return new Gen<>(leftOp, zeroValue());
            }
            if (rightOp instanceof JFieldRef) {
                return new Gen<>(leftOp, zeroValue());
            }

            return new FlowFunction<Value>() {
                @Override
                public Set<Value> computeTargets(Value value) {
                    if (value.equals(zeroValue())) {
                        return Collections.singleton(value);
                    }
                    if (value == leftOp) {
                        return Collections.emptySet();
                    }
                    Set<Value> res = new HashSet<>();
                    res.add(value);
                    if (rightOp.equals(value)) {
                        res.add(leftOp);
                    }

                    return res;
                }
            };
        }

        return Identity.v(); // do not modify this line
    }

    FlowFunction<Value> getCallFlow(Stmt callStmt, final SootMethod destinationMethod) {
        if ("<clinit>".equals(destinationMethod.getName())) {
            return KillAll.v();
        }

        Optional<AbstractInvokeExpr> ie = callStmt.asInvokableStmt().getInvokeExpr();

        final List<Immediate> callArgs = ie.get().getArgs();
        final List<Value> paramLocals = new ArrayList<Value>();
        for (int i = 0; i < destinationMethod.getParameterCount(); i++) {
            paramLocals.add(destinationMethod.getBody().getParameterLocal(i));
        }

        return new FlowFunction<Value>() {
            @Override
            public Set<Value> computeTargets(Value source) {
                Set<Value> ret = new HashSet<>();
                if (source instanceof JStaticFieldRef) {
                    ret.add(source);
                }
                // Tainted func parameters
                for (int i = 0; i < callArgs.size(); i++) {
                    if (callArgs.get(i).equivTo(source) && i < paramLocals.size()) {
                        ret.add(paramLocals.get(i));
                    }
                }
                return ret;
            }
        };
    }

    FlowFunction<Value> getReturnFlow(
            final Stmt callSite, final SootMethod calleeMethod, Stmt exitStmt, Stmt returnSite) {

        Optional<AbstractInvokeExpr> ieo = callSite.asInvokableStmt().getInvokeExpr();
        AbstractInvokeExpr ie = ieo.get();

        Value base = null;
        if (ie instanceof JVirtualInvokeExpr) {
            JVirtualInvokeExpr vie = (JVirtualInvokeExpr) ie;
            base = vie.getBase();
        } else if (ie instanceof JInterfaceInvokeExpr) {
            JInterfaceInvokeExpr iie = (JInterfaceInvokeExpr) ie;
            base = iie.getBase();
        } else if (ie instanceof JSpecialInvokeExpr) {
            JSpecialInvokeExpr iie = (JSpecialInvokeExpr) ie;
            base = iie.getBase();
        }
        final Value baseF = base;

        if (exitStmt instanceof JReturnStmt) {
            JReturnStmt returnStmt = (JReturnStmt) exitStmt;
            final Value retOp = returnStmt.getOp();
            if (retOp instanceof StringConstant) {
                StringConstant str = (StringConstant) retOp;
                if (str.getValue().equals("SECRET")) {
                    if (callSite instanceof JAssignStmt) {
                        JAssignStmt assign = (JAssignStmt) callSite;
                        final Value leftOp = assign.getLeftOp();
                        return new Gen<>(leftOp, zeroValue());
                    }
                }
            }
            return new FlowFunction<Value>() {
                @Override
                public Set<Value> computeTargets(Value source) {
                    Set<Value> ret = new HashSet<>();
                    if (source instanceof JStaticFieldRef) {
                        ret.add(source);
                    }
                    if (callSite instanceof AbstractDefinitionStmt && source == retOp) {
                        AbstractDefinitionStmt defnStmt = (AbstractDefinitionStmt) callSite;
                        ret.add(defnStmt.getLeftOp());
                    }
                    if (baseF != null && source.equals(calleeMethod.getBody().getThisLocal())) {
                        ret.add(baseF);
                    }
                    return ret;
                }
            };
        }
        if (exitStmt instanceof JReturnVoidStmt) {
            return new FlowFunction<Value>() {
                @Override
                public Set<Value> computeTargets(Value source) {
                    Set<Value> ret = new HashSet<Value>();
                    if (source instanceof JStaticFieldRef) {
                        ret.add(source);
                    }
                    if (baseF != null && source.equals(calleeMethod.getBody().getThisLocal())) {
                        ret.add(baseF);
                    }
                    return ret;
                }
            };
        }
        return KillAll.v();
    }

    FlowFunction<Value> getCallToReturnFlow(final Stmt callSite, Stmt returnSite) {
        Optional<AbstractInvokeExpr> ieo = callSite.asInvokableStmt().getInvokeExpr();
        AbstractInvokeExpr ie = ieo.get();
        final List<Immediate> callArgs = ie.getArgs();

        Value base = null;
        Value leftOp = null;

        if (ie instanceof JVirtualInvokeExpr) {
            JVirtualInvokeExpr vie = (JVirtualInvokeExpr) ie;
            base = vie.getBase();
        } else if (ie instanceof JInterfaceInvokeExpr) {
            JInterfaceInvokeExpr iie = (JInterfaceInvokeExpr) ie;
            base = iie.getBase();
        } else if (ie instanceof JSpecialInvokeExpr) {
            JSpecialInvokeExpr iie = (JSpecialInvokeExpr) ie;
            base = iie.getBase();
        }

        if (callSite instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt defnStmt = (AbstractDefinitionStmt) callSite;
            leftOp = defnStmt.getLeftOp();
        }

        final Value baseF = base;
        final Value leftOpF = leftOp;

        // use assumption if no callees to analyze
        if (icfg.getCalleesOfCallAt(callSite).isEmpty()) {
            return new FlowFunction<Value>() {
                @Override
                public Set<Value> computeTargets(Value source) {
                    Set<Value> ret = new HashSet<Value>();
                    ret.add(source);
                    // taint leftOp if base is tainted
                    if (baseF != null && leftOpF != null && source == baseF) {
                        ret.add(leftOpF);
                    }
                    // taint leftOp if one of the args is tainted
                    if (leftOpF != null && callArgs.contains(source)) {
                        ret.add(leftOpF);
                    }
                    // taint base if one of the args is tainted and has no callee in known methods
                    if (baseF != null && callArgs.contains(source)) {
                        ret.add(baseF);
                    }
                    return ret;
                }
            };
        }
        return Identity.v();
    }
}