package challenge3;

import heros.InterproceduralCFG;
import sootup.analysis.interprocedural.icfg.JimpleBasedInterproceduralCFG;
import sootup.analysis.interprocedural.ifds.JimpleIFDSSolver;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.util.*;

public class AnalysisRunner {

    protected JavaView view;
    protected MethodSignature entryMethodSignature;
    protected SootMethod entryMethod;
    JavaIdentifierFactory identifierFactory = JavaIdentifierFactory.getInstance();

    private static JimpleIFDSSolver<?, InterproceduralCFG<Stmt, SootMethod>> solved = null;

    protected JimpleIFDSSolver<?, InterproceduralCFG<Stmt, SootMethod>> executeStaticAnalysis(String pathToJar,
            String targetTestClassName) {
        setupSoot(pathToJar, targetTestClassName);
        runAnalysis();
        if (solved == null) {
            throw new NullPointerException("Something went wrong solving the IFDS problem!");
        }
        return solved;
    }

    private void runAnalysis() {

        JimpleBasedInterproceduralCFG icfg =
                new JimpleBasedInterproceduralCFG(view, Collections.singletonList(entryMethodSignature), false, false);
        TaintAnalysisProblem problem = new TaintAnalysisProblem(icfg, entryMethod);
        JimpleIFDSSolver<?, InterproceduralCFG<Stmt, SootMethod>> solver =
                new JimpleIFDSSolver(problem);
        solver.solve(entryMethod.getDeclaringClassType().getClassName());
        solved = solver;
    }

    /*
     * This method provides the options to soot to analyse the respective
     * classes.
     */
    private void setupSoot(String pathToJar, String targetTestClassName) {
        List<AnalysisInputLocation> inputLocations = new ArrayList<>();
        inputLocations.add(
                new JavaClassPathAnalysisInputLocation(
                        pathToJar, SourceType.Application, Collections.emptyList()));

        view = new JavaView(inputLocations);


        JavaClassType mainClassSignature = identifierFactory.getClassType(targetTestClassName);
        SootClass sc = view.getClass(mainClassSignature).get();
        entryMethod =
                sc.getMethods().stream().filter(e -> e.getName().equals("main")).findFirst().get();
        entryMethodSignature = entryMethod.getSignature();
        System.out.println("starting from: " + entryMethodSignature);
    }

    public String getNumTaintedVarsInEachSink(JimpleIFDSSolver<?, InterproceduralCFG<Stmt, SootMethod>> analysis){
        StringBuilder str = new StringBuilder();
        SootMethod sink1, sink2, sink3, sink4;
        JavaClassType finalClassSig = identifierFactory.getClassType("SinkClass");
        SootClass fc = view.getClass(finalClassSig).get();
        sink1 =
                fc.getMethods().stream().filter(e -> e.getName().equals("sink1")).findFirst().get();
        str.append(numTaintedVarsAtSink(analysis, sink1)).append("-");
        sink2 =
                fc.getMethods().stream().filter(e -> e.getName().equals("sink2")).findFirst().get();
        str.append(numTaintedVarsAtSink(analysis, sink2)).append("-");
        sink3 =
                fc.getMethods().stream().filter(e -> e.getName().equals("sink3")).findFirst().get();
        str.append(numTaintedVarsAtSink(analysis, sink3)).append("-");
        sink4 =
                fc.getMethods().stream().filter(e -> e.getName().equals("sink4")).findFirst().get();
        str.append(numTaintedVarsAtSink(analysis, sink4));
        return str.toString();
    }


    public int numTaintedVarsAtSink(
            JimpleIFDSSolver<?, InterproceduralCFG<Stmt, SootMethod>> analysis, SootMethod sinkMethod) {
        if(sinkMethod==null){
            throw new RuntimeException("sinkMethod is null");
        }
        List<Stmt> stmts = sinkMethod.getBody().getStmts();
        Set<?> rawSet = analysis.ifdsResultsAt(stmts.get(stmts.size() - 1));
        Set<String> names = new HashSet<>();
        for (Object fact : rawSet) {
            if (fact instanceof Local) {
                Local l = (Local) fact;
                names.add(l.getName());
            }
            if (fact instanceof JInstanceFieldRef) {
                JInstanceFieldRef ins = (JInstanceFieldRef) fact;
                names.add(ins.getBase().getName() + "." + ins.getFieldSignature().getName());
            }
            if (fact instanceof JStaticFieldRef) {
                JStaticFieldRef stat = (JStaticFieldRef) fact;
                names.add(
                        stat.getFieldSignature().getDeclClassType() + "." + stat.getFieldSignature().getName());
            }
        }
        names.removeIf(e->e.contains("stack"));
        //System.out.println(names);
        return names.size();
    }

}
